package stock.stock;

import org.json.JSONObject;
import org.json.JSONException;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class StockService {

    private static final String API_URL_TEMPLATE =
        "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=%s&apikey=%s&outputsize=full";
    private static final String API_KEY = "7VQJ6ZGTYZYY7A17";

    /**
     * Fetch stock data from API within the given date range.
     *
     * @param symbol    Stock symbol.
     * @param startDate Start date in "yyyy-MM-dd" format.
     * @param endDate   End date in "yyyy-MM-dd" format.
     * @return A map of dates to closing prices within the specified range.
     * @throws IOException   If an I/O error occurs.
     * @throws JSONException If the API response cannot be parsed.
     */
    public Map<String, Double> fetchStockData(String symbol, String startDate, String endDate) throws IOException, JSONException {
        String apiUrl = String.format(API_URL_TEMPLATE, symbol, API_KEY);
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != 200) {
            throw new IOException("Failed to connect to API, HTTP response code: " + conn.getResponseCode());
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            JSONObject json = new JSONObject(content.toString());

            if (!json.has("Time Series (Daily)")) {
                throw new JSONException("Missing 'Time Series (Daily)' data in API response.");
            }

            JSONObject timeSeries = json.getJSONObject("Time Series (Daily)");
            Map<String, Double> closingPrices = new TreeMap<>(); // Keep dates sorted
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            Date start = dateFormat.parse(startDate);
            Date end = dateFormat.parse(endDate);

            for (String dateStr : timeSeries.keySet()) {
                Date date = dateFormat.parse(dateStr);
                if (!date.before(start) && !date.after(end)) {
                    double closePrice = timeSeries.getJSONObject(dateStr).getDouble("4. close");
                    closingPrices.put(dateStr, closePrice);
                }
            }
            return closingPrices;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error fetching stock data", e);
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Perform backtest based on the selected strategy.
     *
     * @param closingPrices The stock data (date to closing price).
     * @param strategy      The chosen strategy (e.g., MA, RSI, MACD).
     * @return A list of portfolio values mapped with dates for visualization.
     */
    public List<Map<String, Object>> performBacktest(Map<String, Double> closingPrices, String strategy) {
        switch (strategy.toUpperCase()) {
            case "MA": // Moving Average Strategy
                return backtestMA(closingPrices);
            case "RSI": // Relative Strength Index Strategy
                return backtestRSI(closingPrices);
            case "MACD": // MACD Strategy
                return backtestMACD(closingPrices);
            default:
                throw new IllegalArgumentException("Invalid strategy selected.");
        }
    }

    // Backtest Moving Average (MA) Strategy
    private List<Map<String, Object>> backtestMA(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        double cash = 100000, shares = 0;
        List<Double> portfolioValues = new ArrayList<>();
        List<String> dates = new ArrayList<>(closingPrices.keySet());

        for (int i = 10; i < prices.size(); i++) {
            double ma5 = calculateMA(prices, i, 5);
            double ma10 = calculateMA(prices, i, 10);

            if (ma5 > ma10 && shares == 0) {
                shares = cash / prices.get(i);
                cash = 0;
            } else if (ma5 < ma10 && shares > 0) {
                cash = shares * prices.get(i);
                shares = 0;
            }

            portfolioValues.add(cash + shares * prices.get(i));
        }

        return generatePortfolioData(portfolioValues, dates);
    }

    // Backtest Relative Strength Index (RSI) Strategy
    private List<Map<String, Object>> backtestRSI(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        double cash = 100000, shares = 0;
        List<Double> portfolioValues = new ArrayList<>();
        List<String> dates = new ArrayList<>(closingPrices.keySet());
        int rsiPeriod = 14;

        for (int i = rsiPeriod; i < prices.size(); i++) {
            double rsi = calculateRSI(prices, i, rsiPeriod);

            if (rsi > 70 && shares > 0) {
                cash = shares * prices.get(i);
                shares = 0;
            } else if (rsi < 30 && shares == 0) {
                shares = cash / prices.get(i);
                cash = 0;
            }

            portfolioValues.add(cash + shares * prices.get(i));
        }

        return generatePortfolioData(portfolioValues, dates);
    }

    // Backtest MACD Strategy
    private List<Map<String, Object>> backtestMACD(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        double cash = 100000, shares = 0;
        List<Double> portfolioValues = new ArrayList<>();
        List<String> dates = new ArrayList<>(closingPrices.keySet());

        for (int i = 26; i < prices.size(); i++) {
            double macd = calculateMACD(prices, i);
            double signal = calculateSignal(prices, i);

            if (macd > signal && shares == 0) {
                shares = cash / prices.get(i);
                cash = 0;
            } else if (macd < signal && shares > 0) {
                cash = shares * prices.get(i);
                shares = 0;
            }

            portfolioValues.add(cash + shares * prices.get(i));
        }

        return generatePortfolioData(portfolioValues, dates);
    }

    // Calculate Moving Average (MA)
    private double calculateMA(List<Double> prices, int currentIndex, int period) {
        if (currentIndex < period) return 0;
        double sum = 0;
        for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }

    // Calculate Relative Strength Index (RSI)
    private double calculateRSI(List<Double> prices, int currentIndex, int period) {
        double gain = 0, loss = 0;
        for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change > 0) {
                gain += change;
            } else {
                loss -= change;
            }
        }

        double averageGain = gain / period;
        double averageLoss = loss / period;
        double rs = averageLoss == 0 ? 100 : averageGain / averageLoss;
        return 100 - (100 / (1 + rs));
    }

    // Calculate MACD
    private double calculateMACD(List<Double> prices, int currentIndex) {
        double ema12 = calculateEMA(prices, currentIndex, 12);
        double ema26 = calculateEMA(prices, currentIndex, 26);
        return ema12 - ema26;
    }

    // Calculate MACD Signal Line
    private double calculateSignal(List<Double> prices, int currentIndex) {
        double macd = calculateMACD(prices, currentIndex);
        return calculateEMA(Arrays.asList(macd), currentIndex, 9);
    }

    // Calculate Exponential Moving Average (EMA)
    private double calculateEMA(List<Double> prices, int currentIndex, int period) {
        double multiplier = 2.0 / (period + 1);
        double ema = prices.get(currentIndex);
        for (int i = currentIndex - 1; i >= 0 && i >= currentIndex - period; i--) {
            ema = (prices.get(i) - ema) * multiplier + ema;
        }
        return ema;
    }

    // Generate portfolio data for front-end visualization
    private List<Map<String, Object>> generatePortfolioData(List<Double> portfolioValues, List<String> dates) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (int i = 0; i < portfolioValues.size(); i++) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("date", dates.get(i));
            entry.put("value", portfolioValues.get(i));
            result.add(entry);
        }
        return result;
    }
}



/*package stock.stock;

import org.json.JSONObject;
import org.json.JSONException;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Service
public class StockService {

    private static final String API_URL_TEMPLATE =
        "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=%s&apikey=%s&outputsize=full";
    private static final String API_KEY = "7VQJ6ZGTYZYY7A17";

    // Fetch stock data from API
    public Map<String, Double> fetchStockData(String symbol) throws IOException, JSONException {
        String apiUrl = String.format(API_URL_TEMPLATE, symbol, API_KEY);
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != 200) {
            throw new IOException("Failed to connect to API, HTTP response code: " + conn.getResponseCode());
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            JSONObject json = new JSONObject(content.toString());

            if (!json.has("Time Series (Daily)")) {
                throw new JSONException("Missing 'Time Series (Daily)' data in API response.");
            }

            JSONObject timeSeries = json.getJSONObject("Time Series (Daily)");
            Map<String, Double> closingPrices = new TreeMap<>(); // Keep dates sorted

            for (String date : timeSeries.keySet()) {
                double closePrice = timeSeries.getJSONObject(date).getDouble("4. close");
                closingPrices.put(date, closePrice);
            }
            return closingPrices;
        } finally {
            conn.disconnect();
        }
    }

    // Perform backtest based on selected strategy
    public List<Map<String, Object>> performBacktest(Map<String, Double> closingPrices, String strategy) {
        switch (strategy.toUpperCase()) {
            case "MA": // Moving Average Strategy
                return backtestMA(closingPrices);
            case "RSI": // Relative Strength Index Strategy
                return backtestRSI(closingPrices);
            case "MACD": // MACD Strategy
                return backtestMACD(closingPrices);
            default:
                throw new IllegalArgumentException("Invalid strategy selected.");
        }
    }

    // Backtest Moving Average (MA) Strategy
    private List<Map<String, Object>> backtestMA(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        double cash = 100000, shares = 0;
        List<Double> portfolioValues = new ArrayList<>();
        List<String> dates = new ArrayList<>(closingPrices.keySet());

        for (int i = 10; i < prices.size(); i++) {
            double ma5 = calculateMA(prices, i, 5);
            double ma10 = calculateMA(prices, i, 10);

            if (ma5 > ma10 && shares == 0) {
                shares = cash / prices.get(i);
                cash = 0;
            } else if (ma5 < ma10 && shares > 0) {
                cash = shares * prices.get(i);
                shares = 0;
            }

            portfolioValues.add(cash + shares * prices.get(i));
        }

        return generatePortfolioData(portfolioValues, dates);
    }

    // Backtest Relative Strength Index (RSI) Strategy
    private List<Map<String, Object>> backtestRSI(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        double cash = 100000, shares = 0;
        List<Double> portfolioValues = new ArrayList<>();
        List<String> dates = new ArrayList<>(closingPrices.keySet());
        int rsiPeriod = 14;

        for (int i = rsiPeriod; i < prices.size(); i++) {
            double rsi = calculateRSI(prices, i, rsiPeriod);

            if (rsi > 70 && shares > 0) {
                cash = shares * prices.get(i);
                shares = 0;
            } else if (rsi < 30 && shares == 0) {
                shares = cash / prices.get(i);
                cash = 0;
            }

            portfolioValues.add(cash + shares * prices.get(i));
        }

        return generatePortfolioData(portfolioValues, dates);
    }

    // Backtest MACD Strategy
    private List<Map<String, Object>> backtestMACD(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        double cash = 100000, shares = 0;
        List<Double> portfolioValues = new ArrayList<>();
        List<String> dates = new ArrayList<>(closingPrices.keySet());

        for (int i = 26; i < prices.size(); i++) {
            double macd = calculateMACD(prices, i);
            double signal = calculateSignal(prices, i);

            if (macd > signal && shares == 0) {
                shares = cash / prices.get(i);
                cash = 0;
            } else if (macd < signal && shares > 0) {
                cash = shares * prices.get(i);
                shares = 0;
            }

            portfolioValues.add(cash + shares * prices.get(i));
        }

        return generatePortfolioData(portfolioValues, dates);
    }

    // Calculate Moving Average (MA)
    private double calculateMA(List<Double> prices, int currentIndex, int period) {
        if (currentIndex < period) return 0;
        double sum = 0;
        for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }

    // Calculate Relative Strength Index (RSI)
    private double calculateRSI(List<Double> prices, int currentIndex, int period) {
        double gain = 0, loss = 0;
        for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change > 0) {
                gain += change;
            } else {
                loss -= change;
            }
        }

        double averageGain = gain / period;
        double averageLoss = loss / period;
        double rs = averageLoss == 0 ? 100 : averageGain / averageLoss;
        return 100 - (100 / (1 + rs));
    }

    // Calculate MACD
    private double calculateMACD(List<Double> prices, int currentIndex) {
        double ema12 = calculateEMA(prices, currentIndex, 12);
        double ema26 = calculateEMA(prices, currentIndex, 26);
        return ema12 - ema26;
    }

    // Calculate MACD Signal Line
    private double calculateSignal(List<Double> prices, int currentIndex) {
        double macd = calculateMACD(prices, currentIndex);
        return calculateEMA(Arrays.asList(macd), currentIndex, 9);
    }

    // Calculate Exponential Moving Average (EMA)
    private double calculateEMA(List<Double> prices, int currentIndex, int period) {
        double multiplier = 2.0 / (period + 1);
        double ema = prices.get(currentIndex);
        for (int i = currentIndex - 1; i >= 0 && i >= currentIndex - period; i--) {
            ema = (prices.get(i) - ema) * multiplier + ema;
        }
        return ema;
    }

    // Generate portfolio data for front-end visualization
    private List<Map<String, Object>> generatePortfolioData(List<Double> portfolioValues, List<String> dates) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (int i = 0; i < portfolioValues.size(); i++) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("date", dates.get(i));
            entry.put("value", portfolioValues.get(i));
            result.add(entry);
        }
        return result;
    }
}
*/



/*package stock.stock;

import org.json.JSONObject;
import org.json.JSONException;
import org.springframework.stereotype.Service;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Service
public class StockService {

    private static final String API_URL_TEMPLATE =
        "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=%s&apikey=%s&outputsize=full";
    private static final String API_KEY = "7VQJ6ZGTYZYY7A17";

    // 根據股票代碼獲取股票數據
    public Map<String, Double> fetchStockData(String symbol) throws IOException, JSONException {
        String apiUrl = String.format(API_URL_TEMPLATE, symbol, API_KEY);
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != 200) {
            throw new IOException("無法連接到 API，HTTP 回應碼：" + conn.getResponseCode());
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            JSONObject json = new JSONObject(content.toString());

            if (!json.has("Time Series (Daily)")) {
                throw new JSONException("API 回應中未找到 'Time Series (Daily)' 資料。");
            }

            JSONObject timeSeries = json.getJSONObject("Time Series (Daily)");
            Map<String, Double> closingPrices = new TreeMap<>(Collections.reverseOrder()); // 按日期降序排列

            for (String date : timeSeries.keySet()) {
                double closePrice = timeSeries.getJSONObject(date).getDouble("4. close");
                closingPrices.put(date, closePrice);
            }
            return closingPrices;
        } finally {
            conn.disconnect();
        }
    }

    // 根據策略進行回測
    public String performBacktest(Map<String, Double> closingPrices, String strategy) {
        switch (strategy.toUpperCase()) {
            case "MA": // 移動平均策略
                return backtestMA(closingPrices);
            case "RSI": // 相對強弱指數策略
                return backtestRSI(closingPrices);
            case "MACD": // MACD 策略
                return backtestMACD(closingPrices);
            default:
                return "選擇的策略無效。";
        }
    }

    // 回測移動平均（MA）策略
    private String backtestMA(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        double cash = 100000, shares = 0;
        List<Double> portfolioValues = new ArrayList<>();
        List<String> dates = new ArrayList<>(closingPrices.keySet());

        for (int i = 10; i < prices.size(); i++) {
            double ma5 = calculateMA(prices, i, 5);
            double ma10 = calculateMA(prices, i, 10);

            if (ma5 > ma10 && shares == 0) {
                shares = cash / prices.get(i);
                cash = 0;
            } else if (ma5 < ma10 && shares > 0) {
                cash = shares * prices.get(i);
                shares = 0;
            }

            // 保存每一天的資產值
            portfolioValues.add(cash + shares * prices.get(i));
        }

        // 生成並返回 Base64 編碼的圖表
        return generatePortfolioChart(portfolioValues, dates);
    }

    // 回測相對強弱指數（RSI）策略
    private String backtestRSI(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        double cash = 100000, shares = 0;
        List<Double> portfolioValues = new ArrayList<>();
        List<String> dates = new ArrayList<>(closingPrices.keySet());
        int rsiPeriod = 14;

        for (int i = rsiPeriod; i < prices.size(); i++) {
            double rsi = calculateRSI(prices, i, rsiPeriod);

            if (rsi > 70 && shares > 0) {
                cash = shares * prices.get(i);
                shares = 0;
            } else if (rsi < 30 && shares == 0) {
                shares = cash / prices.get(i);
                cash = 0;
            }

            // 保存每一天的資產值
            portfolioValues.add(cash + shares * prices.get(i));
        }

        // 生成並返回 Base64 編碼的圖表
        return generatePortfolioChart(portfolioValues, dates);
    }

    // 回測 MACD 策略
    private String backtestMACD(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        double cash = 100000, shares = 0;
        List<Double> portfolioValues = new ArrayList<>();
        List<String> dates = new ArrayList<>(closingPrices.keySet());

        for (int i = 26; i < prices.size(); i++) {
            double macd = calculateMACD(prices, i);
            double signal = calculateSignal(prices, i);

            if (macd > signal && shares == 0) {
                shares = cash / prices.get(i);
                cash = 0;
            } else if (macd < signal && shares > 0) {
                cash = shares * prices.get(i);
                shares = 0;
            }

            // 保存每一天的資產值
            portfolioValues.add(cash + shares * prices.get(i));
        }

        // 生成並返回 Base64 編碼的圖表
        return generatePortfolioChart(portfolioValues, dates);
    }

    // 計算移動平均（MA）
    private double calculateMA(List<Double> prices, int currentIndex, int period) {
        if (currentIndex < period) return 0;
        double sum = 0;
        for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }

    // 計算相對強弱指數（RSI）
    private double calculateRSI(List<Double> prices, int currentIndex, int period) {
        double gain = 0, loss = 0;
        for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change > 0) {
                gain += change;
            } else {
                loss -= change;
            }
        }

        double averageGain = gain / period;
        double averageLoss = loss / period;
        double rs = averageLoss == 0 ? 100 : averageGain / averageLoss;
        return 100 - (100 / (1 + rs));
    }

    // 計算 MACD
    private double calculateMACD(List<Double> prices, int currentIndex) {
        double ema12 = calculateEMA(prices, currentIndex, 12);
        double ema26 = calculateEMA(prices, currentIndex, 26);
        return ema12 - ema26;
    }

    // 計算 MACD 的信號線
    private double calculateSignal(List<Double> prices, int currentIndex) {
        double macd = calculateMACD(prices, currentIndex);
        return calculateEMA(Arrays.asList(macd), currentIndex, 9);
    }

    // 計算指數移動平均（EMA）
    private double calculateEMA(List<Double> prices, int currentIndex, int period) {
        double multiplier = 2.0 / (period + 1);
        double ema = prices.get(currentIndex);
        for (int i = currentIndex - 1; i >= 0 && i >= currentIndex - period; i--) {
            ema = (prices.get(i) - ema) * multiplier + ema;
        }
        return ema;
    }

    // 生成並返回資產變化的 Base64 圖表
    public String generatePortfolioChart(List<Double> portfolioValues, List<String> dates) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // 根據回測的結果生成圖表
        for (int i = 0; i < portfolioValues.size(); i++) {
            String date = dates.get(i);
            dataset.addValue(portfolioValues.get(i), "Portfolio Value", date);
        }

        JFreeChart lineChart = ChartFactory.createLineChart(
                "Portfolio Value Over Time", // 圖表標題
                "Date",                      // x 軸標籤
                "Portfolio Value",           // y 軸標籤
                dataset,                     // 資料集
                PlotOrientation.VERTICAL,    // 顯示垂直的折線圖
                true,                         // 顯示圖例
                true,                         // 顯示提示框
                false                         // 顯示工具欄
        );

        // 生成圖表的 BufferedImage
        try {
            BufferedImage image = lineChart.createBufferedImage(800, 600);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            baos.flush();
            byte[] imageInByte = baos.toByteArray();
            baos.close();

            // 返回 Base64 編碼的圖表
            return Base64.getEncoder().encodeToString(imageInByte);
        } catch (IOException e) {
            e.printStackTrace();
            return "Error generating chart image.";
        }
    }
}
*/



/*package stock.stock;

import org.json.JSONObject;
import org.json.JSONException;
import org.springframework.stereotype.Service;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.Base64;

@Service
public class StockService {

    private static final String API_URL_TEMPLATE =
        "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=%s&apikey=%s&outputsize=full";
    private static final String API_KEY = "7VQJ6ZGTYZYY7A17";

    // 根據股票代碼獲取股票數據
    public Map<String, Double> fetchStockData(String symbol) throws IOException, JSONException {
        String apiUrl = String.format(API_URL_TEMPLATE, symbol, API_KEY);
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != 200) {
            throw new IOException("無法連接到 API，HTTP 回應碼：" + conn.getResponseCode());
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            JSONObject json = new JSONObject(content.toString());

            if (!json.has("Time Series (Daily)")) {
                throw new JSONException("API 回應中未找到 'Time Series (Daily)' 資料。");
            }

            JSONObject timeSeries = json.getJSONObject("Time Series (Daily)");
            Map<String, Double> closingPrices = new TreeMap<>(Collections.reverseOrder()); // 按日期降序排列

            for (String date : timeSeries.keySet()) {
                double closePrice = timeSeries.getJSONObject(date).getDouble("4. close");
                closingPrices.put(date, closePrice);
            }
            return closingPrices;
        } finally {
            conn.disconnect();
        }
    }

    // 根據策略進行回測
    public String performBacktest(Map<String, Double> closingPrices, String strategy) {
        switch (strategy.toUpperCase()) {
            case "MA": // 移動平均策略
                return backtestMA(closingPrices);
            case "RSI": // 相對強弱指數策略
                return backtestRSI(closingPrices);
            case "MACD": // MACD 策略
                return backtestMACD(closingPrices);
            default:
                return "選擇的策略無效。";
        }
    }

    // 回測移動平均（MA）策略
    private String backtestMA(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        double cash = 100000, shares = 0;
        List<Double> portfolioValues = new ArrayList<>();
        List<String> dates = new ArrayList<>(closingPrices.keySet());

        for (int i = 10; i < prices.size(); i++) {
            double ma5 = calculateMA(prices, i, 5);
            double ma10 = calculateMA(prices, i, 10);

            if (ma5 > ma10 && shares == 0) {
                shares = cash / prices.get(i);
                cash = 0;
            } else if (ma5 < ma10 && shares > 0) {
                cash = shares * prices.get(i);
                shares = 0;
            }

            // 保存每一天的資產值
            portfolioValues.add(cash + shares * prices.get(i));
        }

        // 生成並返回 Base64 編碼的圖表
        return generatePortfolioChart(portfolioValues, dates);
    }

    // 回測相對強弱指數（RSI）策略
    private String backtestRSI(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        double cash = 100000, shares = 0;
        List<Double> portfolioValues = new ArrayList<>();
        List<String> dates = new ArrayList<>(closingPrices.keySet());
        int rsiPeriod = 14;

        for (int i = rsiPeriod; i < prices.size(); i++) {
            double rsi = calculateRSI(prices, i, rsiPeriod);

            if (rsi > 70 && shares > 0) {
                cash = shares * prices.get(i);
                shares = 0;
            } else if (rsi < 30 && shares == 0) {
                shares = cash / prices.get(i);
                cash = 0;
            }

            // 保存每一天的資產值
            portfolioValues.add(cash + shares * prices.get(i));
        }

        // 生成並返回 Base64 編碼的圖表
        return generatePortfolioChart(portfolioValues, dates);
    }

    // 回測 MACD 策略
    private String backtestMACD(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        double cash = 100000, shares = 0;
        List<Double> portfolioValues = new ArrayList<>();
        List<String> dates = new ArrayList<>(closingPrices.keySet());

        for (int i = 26; i < prices.size(); i++) {
            double macd = calculateMACD(prices, i);
            double signal = calculateSignal(prices, i);

            if (macd > signal && shares == 0) {
                shares = cash / prices.get(i);
                cash = 0;
            } else if (macd < signal && shares > 0) {
                cash = shares * prices.get(i);
                shares = 0;
            }

            // 保存每一天的資產值
            portfolioValues.add(cash + shares * prices.get(i));
        }

        // 生成並返回 Base64 編碼的圖表
        return generatePortfolioChart(portfolioValues, dates);
    }

    // 計算移動平均（MA）
    private double calculateMA(List<Double> prices, int currentIndex, int period) {
        if (currentIndex < period) return 0;
        double sum = 0;
        for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }

    // 計算相對強弱指數（RSI）
    private double calculateRSI(List<Double> prices, int currentIndex, int period) {
        double gain = 0, loss = 0;
        for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change > 0) {
                gain += change;
            } else {
                loss -= change;
            }
        }

        double averageGain = gain / period;
        double averageLoss = loss / period;
        double rs = averageLoss == 0 ? 100 : averageGain / averageLoss;
        return 100 - (100 / (1 + rs));
    }

    // 計算 MACD
    private double calculateMACD(List<Double> prices, int currentIndex) {
        double ema12 = calculateEMA(prices, currentIndex, 12);
        double ema26 = calculateEMA(prices, currentIndex, 26);
        return ema12 - ema26;
    }

    // 計算 MACD 的信號線
    private double calculateSignal(List<Double> prices, int currentIndex) {
        double macd = calculateMACD(prices, currentIndex);
        return calculateEMA(Arrays.asList(macd), currentIndex, 9);
    }

    // 計算指數移動平均（EMA）
    private double calculateEMA(List<Double> prices, int currentIndex, int period) {
        double multiplier = 2.0 / (period + 1);
        double ema = prices.get(currentIndex);
        for (int i = currentIndex - 1; i >= 0 && i >= currentIndex - period; i--) {
            ema = (prices.get(i) - ema) * multiplier + ema;
        }
        return ema;
    }

    // 生成並返回資產變化的 Base64 圖表
    public String generatePortfolioChart(List<Double> portfolioValues, List<String> dates) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (int i = 0; i < portfolioValues.size(); i++) {
            String date = dates.get(i);
            dataset.addValue(portfolioValues.get(i), "Portfolio Value", date);
        }

        JFreeChart lineChart = ChartFactory.createLineChart(
                "Portfolio Value Over Time",
                "Date",
                "Portfolio Value",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        // 生成圖表的 BufferedImage
        try {
            BufferedImage image = lineChart.createBufferedImage(800, 600);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            baos.flush();
            byte[] imageInByte = baos.toByteArray();
            baos.close();

            // 返回 Base64 編碼的圖表
            return Base64.getEncoder().encodeToString(imageInByte);
        } catch (IOException e) {
            e.printStackTrace();
            return "Error generating chart image.";
        }
    }
}
*/



/*package stock.stock;

import org.json.JSONObject;
import org.json.JSONException;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Service
public class StockService {

    private static final String API_URL_TEMPLATE =
        "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=%s&apikey=%s&outputsize=full";

    private static final String API_KEY = "7VQJ6ZGTYZYY7A17";

    
     * Fetch stock data for a given symbol.
     *
     * @param symbol 股票代碼
     * @return Map 日期 -> 收盘价
     * @throws IOException   如果网络请求失败
     * @throws JSONException 如果 JSON 解析失败
     
    public Map<String, Double> fetchStockData(String symbol) throws IOException, JSONException {
        String apiUrl = String.format(API_URL_TEMPLATE, symbol, API_KEY);
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != 200) {
            throw new IOException("Failed to connect to API. HTTP response code: " + conn.getResponseCode());
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            JSONObject json = new JSONObject(content.toString());

            if (!json.has("Time Series (Daily)")) {
                throw new JSONException("Time Series (Daily) not found in API response.");
            }

            JSONObject timeSeries = json.getJSONObject("Time Series (Daily)");
            Map<String, Double> closingPrices = new TreeMap<>(Collections.reverseOrder());

            for (String date : timeSeries.keySet()) {
                double closePrice = timeSeries.getJSONObject(date).getDouble("4. close");
                closingPrices.put(date, closePrice);
            }
            return closingPrices;
        } finally {
            conn.disconnect();
        }
    }

    
     * Perform backtesting with a specific strategy.
     *
     * @param closingPrices 收盘价数据
     * @param strategy 策略类型（如 "MA", "RSI", "MACD"）
     * @return 回测结果的字符串描述
     
    public String performBacktest(Map<String, Double> closingPrices, String strategy) {
        switch (strategy.toUpperCase()) {
            case "MA":
                return backtestMA(closingPrices);
            case "RSI":
                return backtestRSI(closingPrices);
            case "MACD":
                return backtestMACD(closingPrices);
            default:
                return "Invalid strategy selected.";
        }
    }

    private String backtestMA(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        double cash = 100000, shares = 0;
        boolean tradeCompleted = false; // 用來標記是否有完成交易

        for (int i = 10; i < prices.size(); i++) {
            double ma5 = calculateMA(prices, i, 5);
            double ma10 = calculateMA(prices, i, 10);

            if (ma5 > ma10 && shares == 0) {
                shares = cash / prices.get(i);
                cash = 0;
                tradeCompleted = true; // 有進行買入
            } else if (ma5 < ma10 && shares > 0) {
                cash = shares * prices.get(i);
                shares = 0;
                tradeCompleted = true; // 有進行賣出
            }
        }

        // 如果沒有完成任何交易，返回提示信息
        if (!tradeCompleted) {
            return "No completed trades in MA strategy.";
        }

        double finalValue = cash + shares * prices.get(prices.size() - 1);
        return "Final portfolio value with MA strategy: " + finalValue;
    }

    private String backtestRSI(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        double cash = 100000, shares = 0;
        int rsiPeriod = 14;
        boolean tradeCompleted = false; // 用來標記是否有完成交易

        for (int i = rsiPeriod; i < prices.size(); i++) {
            double rsi = calculateRSI(prices, i, rsiPeriod);

            if (rsi > 70 && shares > 0) {
                cash = shares * prices.get(i);
                shares = 0;
                tradeCompleted = true;
            } else if (rsi < 30 && shares == 0) {
                shares = cash / prices.get(i);
                cash = 0;
                tradeCompleted = true;
            }
        }

        // 如果沒有完成任何交易，返回提示信息
        if (!tradeCompleted) {
            return "No completed trades in RSI strategy.";
        }

        double finalValue = cash + shares * prices.get(prices.size() - 1);
        return "Final portfolio value with RSI strategy: " + finalValue;
    }

    private String backtestMACD(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        double cash = 100000, shares = 0;
        boolean tradeCompleted = false; // 用來標記是否有完成交易

        for (int i = 26; i < prices.size(); i++) {
            double macd = calculateMACD(prices, i);
            double signal = calculateSignal(prices, i);

            if (macd > signal && shares == 0) {
                shares = cash / prices.get(i);
                cash = 0;
                tradeCompleted = true;
            } else if (macd < signal && shares > 0) {
                cash = shares * prices.get(i);
                shares = 0;
                tradeCompleted = true;
            }
        }

        // 如果沒有完成任何交易，返回提示信息
        if (!tradeCompleted) {
            return "No completed trades in MACD strategy.";
        }

        double finalValue = cash + shares * prices.get(prices.size() - 1);
        return "Final portfolio value with MACD strategy: " + finalValue;
    }

    private double calculateMA(List<Double> prices, int currentIndex, int period) {
        if (currentIndex < period) return 0;
        double sum = 0;
        for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }

    private double calculateRSI(List<Double> prices, int currentIndex, int period) {
        double gain = 0, loss = 0;
        for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change > 0) {
                gain += change;
            } else {
                loss -= change;
            }
        }

        double averageGain = gain / period;
        double averageLoss = loss / period;
        double rs = averageLoss == 0 ? 100 : averageGain / averageLoss;
        return 100 - (100 / (1 + rs));
    }

    private double calculateMACD(List<Double> prices, int currentIndex) {
        double ema12 = calculateEMA(prices, currentIndex, 12);
        double ema26 = calculateEMA(prices, currentIndex, 26);
        return ema12 - ema26;
    }

    private double calculateSignal(List<Double> prices, int currentIndex) {
        double macd = calculateMACD(prices, currentIndex);
        return calculateEMA(Arrays.asList(macd), currentIndex, 9);
    }

    private double calculateEMA(List<Double> prices, int currentIndex, int period) {
        double multiplier = 2.0 / (period + 1);
        double ema = prices.get(currentIndex);
        for (int i = currentIndex - 1; i >= 0 && i >= currentIndex - period; i--) {
            ema = (prices.get(i) - ema) * multiplier + ema;
        }
        return ema;
    }
}
*/


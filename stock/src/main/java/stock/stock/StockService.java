package stock.stock;

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

    /**
     * Fetch stock data for a given symbol.
     *
     * @param symbol 股票代碼
     * @return Map 日期 -> 收盘价
     * @throws IOException   如果网络请求失败
     * @throws JSONException 如果 JSON 解析失败
     */
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

    /**
     * Perform backtesting with a specific strategy.
     *
     * @param closingPrices 收盘价数据
     * @param strategy 策略类型（如 "MA", "RSI", "MACD"）
     * @return 回测结果的字符串描述
     */
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
        }

        double finalValue = cash + shares * prices.get(prices.size() - 1);
        return "Final portfolio value with MA strategy: " + finalValue;
    }

    private String backtestRSI(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        double cash = 100000, shares = 0;
        int rsiPeriod = 14;

        // 计算 RSI 策略
        for (int i = rsiPeriod; i < prices.size(); i++) {
            double rsi = calculateRSI(prices, i, rsiPeriod);

            if (rsi > 70 && shares > 0) {
                cash = shares * prices.get(i);
                shares = 0;
            } else if (rsi < 30 && shares == 0) {
                shares = cash / prices.get(i);
                cash = 0;
            }
        }

        double finalValue = cash + shares * prices.get(prices.size() - 1);
        return "Final portfolio value with RSI strategy: " + finalValue;
    }

    private String backtestMACD(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        double cash = 100000, shares = 0;

        // 计算 MACD 策略
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
        }

        double finalValue = cash + shares * prices.get(prices.size() - 1);
        return "Final portfolio value with MA strategy: " + finalValue;
    }

    private String backtestRSI(Map<String, Double> closingPrices) {
        // RSI 策略实现（与 MA 类似）
        return "RSI strategy not yet implemented.";
    }

    private String backtestMACD(Map<String, Double> closingPrices) {
        // MACD 策略实现（与 MA 类似）
        return "MACD strategy not yet implemented.";
    }

    private double calculateMA(List<Double> prices, int currentIndex, int period) {
        if (currentIndex < period) return 0;
        double sum = 0;
        for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }
}*/




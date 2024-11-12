package stocks;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import javax.swing.*;

public class stock {
    private static final String API_KEY = "9SA14RV53IUBTFHN";
    private static final String SYMBOL = "TSM";  // 例：使用蘋果股票
    private static final String API_URL = 
        "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol="
        + SYMBOL + "&apikey=" + API_KEY + "&outputsize=full";

    public static void main(String[] args) {
        try {
            // 1. 獲取股價數據
            Map<String, Double> closingPrices = fetchStockData();

            // 2. 計算移動平均線並回測策略
            performBacktest(closingPrices);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Double> fetchStockData() throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder content = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        conn.disconnect();

        // 解析 JSON 並獲取收盤價
        JSONObject json = new JSONObject(content.toString());
        JSONObject timeSeries = json.getJSONObject("Time Series (Daily)");
        Map<String, Double> closingPrices = new TreeMap<>(Collections.reverseOrder());

        for (String date : timeSeries.keySet()) {
            double closePrice = timeSeries.getJSONObject(date).getDouble("4. close");
            closingPrices.put(date, closePrice);
        }
        return closingPrices;
    }

    private static void performBacktest(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        double cash = 100000;  // 初始資金
        double shares = 0;     // 持有股票數量

        // 用於記錄每次交易的資本變化
        List<Double> portfolioValues = new ArrayList<>();

        for (int i = 10; i < prices.size(); i++) {
            double ma5 = calculateMA(prices, i, 5);
            double ma10 = calculateMA(prices, i, 10);

            // 黃金交叉：MA5 上穿 MA10，買入
            if (ma5 > ma10 && shares == 0) {
                shares = cash / prices.get(i);
                cash = 0;
                System.out.println("黃金交叉，買入於價格：" + prices.get(i));
            }
            // 死亡交叉：MA5 下穿 MA10，賣出
            else if (ma5 < ma10 && shares > 0) {
                cash = shares * prices.get(i);
                shares = 0;
                System.out.println("死亡交叉，賣出於價格：" + prices.get(i));
            }

            // 計算目前投資組合的總價值
            double currentValue = cash + shares * prices.get(i);
            portfolioValues.add(currentValue);
        }

        // 繪製收益圖
        plotPortfolioValues(portfolioValues);
    }

    private static double calculateMA(List<Double> prices, int currentIndex, int period) {
        double sum = 0;
        for (int i = currentIndex - period; i < currentIndex; i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }

    private static void plotPortfolioValues(List<Double> portfolioValues) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        // 添加数据
        for (int i = 0; i < portfolioValues.size(); i++) {
            dataset.addValue(portfolioValues.get(i), "Portfolio Value", Integer.toString(i));
        }

        // 创建图表
        JFreeChart lineChart = ChartFactory.createLineChart(
                "Portfolio Value Over Time",
                "Time",
                "Portfolio Value",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        // 显示图表
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(lineChart));
        frame.pack();
        frame.setVisible(true);
    }
}


/*import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class stock {
    private static final String API_KEY = "9SA14RV53IUBTFHN";
    private static final String SYMBOL = "AAPL";
    private static final String API_URL = 
    "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol="
        + SYMBOL + "&apikey=" + API_KEY + "&outputsize=full"; 

    public static void main(String[] args) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            conn.disconnect();

            System.out.println("API 回應: " + content.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 



@SuppressWarnings("unused")
public class stock {
    private static final String API_KEY = "9SA14RV53IUBTFHN";
    private static final String SYMBOL = "AAPL";  // 例：使用蘋果股票
    private static final String API_URL = 
        "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol="
        + SYMBOL + "&apikey=" + API_KEY + "&outputsize=full";

    public static void main(String[] args) {
        try {
            // 1. 獲取股價數據
            Map<String, Double> closingPrices = fetchStockData();

            // 2. 計算移動平均線並回測策略
            performBacktest(closingPrices);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Double> fetchStockData() throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder content = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        conn.disconnect();

        // 解析 JSON 並獲取收盤價
        JSONObject json = new JSONObject(content.toString());
        JSONObject timeSeries = json.getJSONObject("Time Series (Daily)");
        Map<String, Double> closingPrices = new TreeMap<>(Collections.reverseOrder());

        for (String date : timeSeries.keySet()) {
            double closePrice = timeSeries.getJSONObject(date).getDouble("4. close");
            closingPrices.put(date, closePrice);
        }
        return closingPrices;
    }

    private static void performBacktest(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        double cash = 100000;  // 初始資金
        double shares = 0;     // 持有股票數量

        // 用於記錄每次交易的資本變化
        List<Double> portfolioValues = new ArrayList<>();

        for (int i = 10; i < prices.size(); i++) {
            double ma5 = calculateMA(prices, i, 5);
            double ma10 = calculateMA(prices, i, 10);

            // 黃金交叉：MA5 上穿 MA10，買入
            if (ma5 > ma10 && shares == 0) {
                shares = cash / prices.get(i);
                cash = 0;
                System.out.println("黃金交叉，買入於價格：" + prices.get(i));
            }
            // 死亡交叉：MA5 下穿 MA10，賣出
            else if (ma5 < ma10 && shares > 0) {
                cash = shares * prices.get(i);
                shares = 0;
                System.out.println("死亡交叉，賣出於價格：" + prices.get(i));
            }

            // 計算目前投資組合的總價值
            double currentValue = cash + shares * prices.get(i);
            portfolioValues.add(currentValue);
        }

        // 繪製收益圖
        plotPortfolioValues(portfolioValues);
    }

    private static double calculateMA(List<Double> prices, int currentIndex, int period) {
        double sum = 0;
        for (int i = currentIndex - period; i < currentIndex; i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }

    private static void plotPortfolioValues(List<Double> portfolioValues) {
        // 使用 Java 繪圖庫（如 JFreeChart）來繪製圖表
        // 此處省略具體實現，可參考 JFreeChart 的使用說明
    }
}*/


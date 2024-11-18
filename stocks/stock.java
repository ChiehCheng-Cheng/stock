package stocks;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
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
    // 定義 Alpha Vantage API 密鑰和股票代碼（此範例為台積電 TSM）
    private static final String API_KEY = "7VQJ6ZGTYZYY7A17";
    private static final String SYMBOL = "TSM";  
    private static final String API_URL = 
        "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol="
        + SYMBOL + "&apikey=" + API_KEY + "&outputsize=full";

    public static void main(String[] args) {
        try {
            // 1. 獲取股價數據
            Map<String, Double> closingPrices = fetchStockData();
            // 2. 執行回測策略
            performBacktest(closingPrices);
        } catch (Exception e) {
            // 處理錯誤並打印錯誤訊息
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 從 Alpha Vantage API 獲取股價數據
    private static Map<String, Double> fetchStockData() throws IOException, JSONException {
        URL url = new URL(API_URL);  // 創建 API 請求的 URL
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");  // 設定 HTTP 請求方法為 GET

        // 檢查是否成功連接，回應狀態碼不是 200 就拋出錯誤
        if (conn.getResponseCode() != 200) {
            throw new IOException("Failed to connect to API. HTTP response code: " + conn.getResponseCode());
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String inputLine;
            // 讀取 API 返回的數據
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            // 解析返回的 JSON 數據
            JSONObject json = new JSONObject(content.toString());
            JSONObject timeSeries = json.getJSONObject("Time Series (Daily)");

            Map<String, Double> closingPrices = new TreeMap<>(Collections.reverseOrder());  // 使用 TreeMap 反轉日期順序

            // 提取每一天的收盤價並存入 closingPrices 地圖中
            for (String date : timeSeries.keySet()) {
                double closePrice = timeSeries.getJSONObject(date).getDouble("4. close");
                closingPrices.put(date, closePrice);
            }
            return closingPrices;
        } finally {
            // 確保連線釋放
            conn.disconnect();
        }
    }

    // 執行回測策略，根據移動平均線進行買賣決策
    private static void performBacktest(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());  // 轉換收盤價為 List 方便處理
        double cash = 100000;  // 初始資金
        double shares = 0;     // 初始股票數量
        List<Double> portfolioValues = new ArrayList<>();  // 用於記錄每個時間點的投資組合價值
        List<String> transactionLog = new ArrayList<>();  // 用於記錄每次交易的日誌

        // 從第 10 天開始進行回測，避免移動平均線無法計算
        for (int i = 10; i < prices.size(); i++) {
            double ma5 = calculateMA(prices, i, 5);  // 計算 5 日移動平均線
            double ma10 = calculateMA(prices, i, 10);  // 計算 10 日移動平均線

            // 黃金交叉：MA5 上穿 MA10，買入
            if (ma5 > ma10 && shares == 0) {
                shares = cash / prices.get(i);  // 使用現金買入股票
                cash = 0;  // 資金清空
                transactionLog.add("Golden Cross - Bought at: " + prices.get(i));  // 記錄買入操作
            } 
            // 死亡交叉：MA5 下穿 MA10，賣出
            else if (ma5 < ma10 && shares > 0) {
                cash = shares * prices.get(i);  // 賣出所有持有的股票，將現金更新為賣出所得
                shares = 0;  // 股票數量清空
                transactionLog.add("Death Cross - Sold at: " + prices.get(i));  // 記錄賣出操作
            }

            // 計算目前投資組合的總價值（現金 + 股票的市值）
            double currentValue = cash + shares * prices.get(i);
            portfolioValues.add(currentValue);  // 記錄每個時間點的投資組合價值
        }

        // 打印交易日誌
        transactionLog.forEach(System.out::println);
        // 繪製投資組合價值隨時間變化的圖表
        plotPortfolioValues(portfolioValues);
    }

    // 計算移動平均線
    private static double calculateMA(List<Double> prices, int currentIndex, int period) {
        if (currentIndex < period) return 0;  // 如果當前索引小於移動平均期數，返回 0
        double sum = 0;
        // 計算從 currentIndex - period 到 currentIndex 的平均數
        for (int i = currentIndex - period; i < currentIndex; i++) {
            sum += prices.get(i);
        }
        return sum / period;  // 返回移動平均值
    }

    // 繪製投資組合價值隨時間變化的圖表
    private static void plotPortfolioValues(List<Double> portfolioValues) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // 將每一個時間點的投資組合價值添加到圖表數據集
        for (int i = 0; i < portfolioValues.size(); i++) {
            dataset.addValue(portfolioValues.get(i), "Portfolio Value", Integer.toString(i));
        }

        // 使用 JFreeChart 繪製折線圖
        JFreeChart lineChart = ChartFactory.createLineChart(
                "Portfolio Value Over Time",  // 圖表標題
                "Time",  // x 軸標題
                "Portfolio Value",  // y 軸標題
                dataset,  // 數據集
                PlotOrientation.VERTICAL,  // 圖表方向
                true, true, false);  // 是否顯示圖例、工具提示等

        // 顯示圖表
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(lineChart));
        frame.pack();
        frame.setVisible(true);
    }
}

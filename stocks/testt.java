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
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import javax.swing.*;

public class testt {
    private static final String API_KEY = "7VQJ6ZGTYZYY7A17";
    private static final String SYMBOL = "TSM";
    private static final String API_URL =
        "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol="
        + SYMBOL + "&apikey=" + API_KEY + "&outputsize=full";

    public static void main(String[] args) {
        try {
            // 1. 获取股价数据
            Map<String, Double> closingPrices = fetchStockData();
            // 2. 执行回测策略
            performBacktest(closingPrices);
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Map<String, Double> fetchStockData() throws IOException, JSONException {
        URL url = new URL(API_URL);
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

    private static void performBacktest(Map<String, Double> closingPrices) {
        List<Double> prices = new ArrayList<>(closingPrices.values());
        List<String> dates = new ArrayList<>(closingPrices.keySet());
        double cash = 100000;
        double shares = 0;
        List<Double> portfolioValues = new ArrayList<>();
        List<String> transactionLog = new ArrayList<>();

        int months = 6;
        int tradingDaysInMonth = 21;
        int totalTradingDays = months * tradingDaysInMonth;
        
        //ma策略 ma5>ma10buy ma10<ma5sell
        /*for (int i = 10; i < Math.min(prices.size(), totalTradingDays); i++) {
            double ma5 = calculateMA(prices, i, 5);
            double ma10 = calculateMA(prices, i, 10);

            if (ma5 > ma10 && shares == 0) {
                shares = cash / prices.get(i);
                cash = 0;
                transactionLog.add("Golden Cross - Bought at: " + prices.get(i));
            } else if (ma5 < ma10 && shares > 0) {
                cash = shares * prices.get(i);
                shares = 0;
                transactionLog.add("Death Cross - Sold at: " + prices.get(i));
            }

            double currentValue = cash + shares * prices.get(i);
            portfolioValues.add(currentValue);
        }

        transactionLog.forEach(System.out::println);

        double finalPortfolioValue = cash + shares * prices.get(Math.min(prices.size(), totalTradingDays) - 1);
        System.out.println("Final portfolio value: " + finalPortfolioValue);

        plotPortfolioValues(portfolioValues, dates);
    }

    private static double calculateMA(List<Double> prices, int currentIndex, int period) {
        if (currentIndex < period) return 0;
        double sum = 0;
        for (int i = currentIndex - period; i < currentIndex; i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }*/
    //ma策略

        // RSI策略 rsi>70sell rsi<30buy
        /*for (int i = 10; i < Math.min(prices.size(), totalTradingDays); i++) {
        double rsi = calculateRSI(prices, i, 14); // 14日RSI

        // 基於 RSI 策略的買賣邏輯
        if (rsi < 30 && shares == 0) {
            shares = cash / prices.get(i);
            cash = 0;
            transactionLog.add("RSI Strategy - Bought at: " + prices.get(i) + " (RSI: " + rsi + ")");
        } else if (rsi > 70 && shares > 0) {
            cash = shares * prices.get(i);
            shares = 0;
            transactionLog.add("RSI Strategy - Sold at: " + prices.get(i) + " (RSI: " + rsi + ")");
        }

        // 計算當前的投資組合價值
        double currentValue = cash + shares * prices.get(i);
        portfolioValues.add(currentValue);
    }

    transactionLog.forEach(System.out::println);

    double finalPortfolioValue = cash + shares * prices.get(Math.min(prices.size(), totalTradingDays) - 1);
    System.out.println("Final portfolio value: " + finalPortfolioValue);

    plotPortfolioValues(portfolioValues, dates);
}

private static double calculateRSI(List<Double> prices, int currentIndex, int period) {
    if (currentIndex < period) return 0;
    
    double gain = 0, loss = 0;
    
    for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
        double change = prices.get(i) - prices.get(i - 1);
        if (change > 0) gain += change;
        else loss -= change; // loss is positive in magnitude
    }
    
    double avgGain = gain / period;
    double avgLoss = loss / period;
    
    if (avgLoss == 0) return 100; // Avoid division by zero
    
    double rs = avgGain / avgLoss;
    return 100 - (100 / (1 + rs));
}*/
//rsi策略

  // macd策略  DIF>MACD9買入 DIF<MACD9賣出
  //計算macd線
  List<Double> macdLine = new ArrayList<>();
  List<Double> signalLine = new ArrayList<>();
  List<Double> histogram = new ArrayList<>();

  for (int i = 26; i < prices.size(); i++) { // 从26天开始计算MACD
      double ema12 = calculateEMA(prices, i, 12);
      double ema26 = calculateEMA(prices, i, 26);
      double macd = ema12 - ema26;
      macdLine.add(macd);
      if (macdLine.size() >= 9) {
          double signal = calculateEMA(macdLine, macdLine.size() - 1, 9);
          signalLine.add(signal);
          histogram.add(macd - signal);
      }
  }

  for (int i = 10; i < Math.min(prices.size(), totalTradingDays); i++) {
      double ma5 = (i >= 4) ? calculateMA(prices, i, 5) : 0;  // 确保 i >= 4 时才计算 5 日均线
      double ma10 = (i >= 9) ? calculateMA(prices, i, 10) : 0;  // 确保 i >= 9 时才计算 10 日均线

      // MA 策略判断
      if (ma5 > ma10 && shares == 0) {
          shares = cash / prices.get(i);
          cash = 0;
          transactionLog.add("Golden Cross - Bought at: " + prices.get(i));
      } else if (ma5 < ma10 && shares > 0) {
          cash = shares * prices.get(i);
          shares = 0;
          transactionLog.add("Death Cross - Sold at: " + prices.get(i));
      }

      // macd 策略判断
      if (macdLine.size() > i - 10 && signalLine.size() > i - 10) {
          double macdValue = macdLine.get(i - 10);
          double signalValue = signalLine.get(i - 10);

          if (macdValue > signalValue && shares == 0) {
              shares = cash / prices.get(i);
              cash = 0;
              transactionLog.add("MACD Buy - Bought at: " + prices.get(i));
          } else if (macdValue < signalValue && shares > 0) {
              cash = shares * prices.get(i);
              shares = 0;
              transactionLog.add("MACD Sell - Sold at: " + prices.get(i));
          }
      }

      double currentValue = cash + shares * prices.get(i);
      portfolioValues.add(currentValue);
  }

  transactionLog.forEach(System.out::println);

  double finalPortfolioValue = cash + shares * prices.get(Math.min(prices.size(), totalTradingDays) - 1);
  System.out.println("Final portfolio value: " + finalPortfolioValue);

  plotPortfolioValues(portfolioValues, dates);
}

private static double calculateEMA(List<Double> prices, int currentIndex, int period) {
  if (currentIndex < period) return 0;
  double multiplier = 2.0 / (period + 1);
  double ema = prices.get(currentIndex - period);
  for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
      ema = (prices.get(i) - ema) * multiplier + ema;
  }
  return ema;
}

private static double calculateMA(List<Double> prices, int currentIndex, int period) {
  if (currentIndex < period) return 0; // 确保索引大于等于期数-1
  double sum = 0;
  for (int i = currentIndex - period + 1; i <= currentIndex; i++) { // 计算从 i-period+1 到 i 的平均值
      sum += prices.get(i);
  }
  return sum / period;
}
//macd策略

    
    private static void plotPortfolioValues(List<Double> portfolioValues, List<String> dates) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // 確保索引不會越界
        for (int i = 0; i < portfolioValues.size(); i++) {
            // 使用 i 來確保不會超過日期列表長度
            String date = dates.get(i + 10 < dates.size() ? i + 10 : i);
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

        // 设置 Y 轴范围和刻度
        if (lineChart.getCategoryPlot().getRangeAxis() instanceof NumberAxis) {
            NumberAxis yAxis = (NumberAxis) lineChart.getCategoryPlot().getRangeAxis();
            yAxis.setRange(0, 200000); // 设置范围
            yAxis.setTickUnit(new NumberTickUnit(5000)); // 设置步长为 5000
        } else {
            System.err.println("Range axis is not a NumberAxis, unable to set tick unit.");
        }

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(lineChart));
        frame.pack();
        frame.setVisible(true);
    }
}



/*package stocks;

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
import java.text.SimpleDateFormat;
import java.util.Date;

public class testt {
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

            // 打印原始返回的 JSON 結果
            System.out.println(content.toString()); // 打印返回的 JSON 結果

            // 解析返回的 JSON 數據
            JSONObject json = new JSONObject(content.toString());

            // 檢查是否包含 "Time Series (Daily)"
            if (!json.has("Time Series (Daily)")) {
                throw new JSONException("Time Series (Daily) not found in API response.");
            }

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
        List<String> dates = new ArrayList<>(closingPrices.keySet());  // 提取日期
        double cash = 100000;  // 初始資金
        double shares = 0;     // 初始股票數量
        List<Double> portfolioValues = new ArrayList<>();  // 用於記錄每個時間點的投資組合價值
        List<String> transactionLog = new ArrayList<>();  // 用於記錄每次交易的日誌

        // 只回測半年，即回測 6 個月（約 126 個交易日）
        int months = 6;
        int tradingDaysInMonth = 21;  // 平均每月有 21 個交易日
        int totalTradingDays = months * tradingDaysInMonth;
        for (int i = 10; i < Math.min(prices.size(), totalTradingDays); i++) {
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

        // 打印最終資金狀況
        double finalPortfolioValue = cash + shares * prices.get(Math.min(prices.size(), totalTradingDays) - 1);
        System.out.println("Final portfolio value: " + finalPortfolioValue);

        // 繪製投資組合價值隨時間變化的圖表
        plotPortfolioValues(portfolioValues, dates);
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
    private static void plotPortfolioValues(List<Double> portfolioValues, List<String> dates) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // 使用 SimpleDateFormat 格式化年份
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");

        // 將每一個時間點的投資組合價值添加到圖表數據集
        for (int i = 0; i < portfolioValues.size(); i++) {
            String date = dates.get(i + 10);  // 由於回測從第 10 天開始，將日期從這裡開始
            try {
                // 格式化日期，僅提取年份
                Date formattedDate = sdf.parse(date);
                // 將投資組合價值添加到數據集，x 軸顯示年份，y 軸顯示投資組合價值
                dataset.addValue(portfolioValues.get(i), "Portfolio Value", sdf.format(formattedDate));
            } catch (Exception e) {
                e.printStackTrace();  // 捕捉日期解析錯誤
            }
        }

        // 創建圖表
        JFreeChart lineChart = ChartFactory.createLineChart(
                "Portfolio Value Over Time",  // 圖表標題
                "Year",  // x 軸標題
                "Portfolio Value",  // y 軸標題
                dataset,  // 數據集
                PlotOrientation.VERTICAL,  // 圖表方向
                true, true, false);  // 顯示圖例和提示

        // 顯示圖表
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(lineChart));
        frame.pack();
        frame.setVisible(true);
    }
}*/






package stock.stock.Controller;

import stock.stock.StockService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/stock") // 统一路径前缀
public class StockController {

    private final StockService stockService;

    // 构造器注入 StockService
    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    /**
     * 回测股票数据并返回结果页面
     *
     * @param symbol   股票代码
     * @param strategy 回测策略
     * @param model    模型数据
     * @return 视图名称
     */
    @GetMapping("/backtest")
    public String runBacktest(
            @RequestParam String symbol,
            @RequestParam String strategy,
            Model model) {
        try {
            // 获取股票数据
            Map<String, Double> closingPrices = stockService.fetchStockData(symbol);

            // 根据策略运行回测
            String backtestResult = stockService.performBacktest(closingPrices, strategy);

            // 将结果添加到模型
            model.addAttribute("closingPrices", closingPrices);
            model.addAttribute("symbol", symbol);
            model.addAttribute("strategy", strategy);
            model.addAttribute("message", backtestResult);

            // 返回结果页面
            return "result";
        } catch (Exception e) {
            // 设置错误消息并返回错误页面
            model.addAttribute("error", "Error occurred: " + e.getMessage());
            e.printStackTrace();
            return "error";
        }
    }
}





/*package stock.stock.Controller;

import stock.stock.StockService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.json.JSONException;

import java.io.IOException;
import java.util.Map;

@Controller
public class StockController {

    private final StockService stockService;

    // 使用建構子注入 StockService
    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/backtest")
    public String runBacktest(Model model) {
        try {
            // 從 StockService 獲取股票數據
            Map<String, Double> closingPrices = stockService.fetchStockData();
            // 返回顯示結果的頁面
            model.addAttribute("closingPrices", closingPrices);
            return "result";  // 返回 result.html 頁面
        } catch (IOException | JSONException e) {
            // 捕獲錯誤並顯示錯誤頁面
            model.addAttribute("error", "Failed to fetch stock data: " + e.getMessage());
            e.printStackTrace(); // 記錄錯誤堆棧
            return "error";  // 返回 error.html 頁面
        }
    }
}*/

package stock.stock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/**
 * 股票回測應用的主入口類。
 * 這個應用程式提供股票數據檢索與回測功能，
 * 支援多種策略，包括移動平均線（MA）、相對強弱指標（RSI）和 MACD。
 */
@SpringBootApplication
public class StockApplication {

    public static void main(String[] args) {
        // 啟動 Spring Boot 應用程式
        SpringApplication.run(StockApplication.class, args);
        System.out.println("股票回測應用程式已成功啟動！");
    }
}

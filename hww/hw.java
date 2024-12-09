package hww;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class hw {
    public static void main(String[] args) {
        // 設定檔案名稱，如果命令列有傳入參數則使用第一個參數作為檔名，否則使用預設檔名
        String filename = "";
        if (args.length > 0)
            filename = args[0];  // 使用命令列的檔案名稱
        else
            filename = "C:\\Users\\USER\\Desktop\\program\\java\\java vs\\hww\\iris.txt";  // 預設檔案名稱

        FileReader fr;  // 用來讀取檔案
        String[] varName = null;  // 儲存欄位名稱
        int i = 0;  // 記錄讀取的行數
        int j = 0;  // 記錄讀取的欄位數
        List<String[]> dataList = new ArrayList<String[]>();  // 儲存資料
        int row = 0;  // 記錄資料行數
        int col = 0;  // 記錄資料欄數

        try {
            // 開啟檔案讀取器
            fr = new FileReader(filename);
            // 使用 BufferedReader 來逐行讀取檔案
            try (BufferedReader br = new BufferedReader(fr)) {
                String line;  // 用來儲存每次讀取的行
                // 逐行讀取資料
                while ((line = br.readLine()) != null) {
                    // 第一行是欄位名稱
                    if (i == 0) {
                        varName = line.split("\t");  // 將第一行按制表符分割，獲得欄位名稱
                    } else {
                        // 其餘行是資料
                        String[] tempData = line.split("\t");  // 以制表符分割每行資料
                        String[] data = new String[4];  // 假設每行資料有4個值
                        for (j = 0; j < 4; j++) {
                            data[j] = tempData[j + 2];  // 跳過前兩個欄位，只處理數值部分
                        }
                        dataList.add(data);  // 將資料加入列表
                    }
                    i++;  // 行數遞增
                }

                col = varName.length - 2;  // 計算資料列數（排除前兩個欄位）
                row = dataList.size();  // 計算資料行數
                System.out.println("Row: " + row + ", Col: " + col);  // 輸出行列數

                // 用來存儲 Pearson 相關係數的臨時變數
                double[] tempVar1 = new double[col];
                double[] tempVar2 = new double[col];
                // 用來存儲最終的相關係數矩陣
                double[][] correlationMatrix = new double[row][row];

                // 計算每兩行資料的 Pearson 相關係數
                for (i = 0; i < row; i++) {
                    for (int k = 0; k < row; k++) {
                        for (j = 0; j < col; j++) {
                            // 將資料轉換為 double 類型數字
                            tempVar1[j] = Double.valueOf(dataList.get(i)[j]);
                            tempVar2[j] = Double.valueOf(dataList.get(k)[j]);
                        }
                        // 計算 Pearson 相關係數
                        double correlation = helloworld.calculatePearsonCorrelation(tempVar1, tempVar2);
                        // 將計算出的相關係數存入矩陣
                        correlationMatrix[i][k] = correlation;
                    }
                }

                // 設定輸出檔案名稱
                String outputFilename = "C:\\Users\\USER\\Desktop\\program\\java\\java vs\\hww\\correlation_matrix.csv";
                File file = new File(outputFilename);
                
                // 使用 BufferedWriter 來寫入 CSV 檔案
                try (BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(file, false), "utf8"))) {

                    // 寫入相關係數矩陣到檔案
                    for (i = 0; i < row; i++) {
                        for (j = 0; j < row; j++) {
                            // 寫入每個相關係數，並將其格式化為 6 位小數
                            writer.write(String.format("%.6f", correlationMatrix[i][j]));
                            if (j < row - 1)
                                writer.write(",");  // 行內值之間以逗號分隔
                        }
                        writer.newLine();  // 寫入新的一行
                    }
                }

                // 不再需要顯式地關閉 BufferedReader，因為它已經在 try-with-resources 中被自動關閉
                br.close();
            } catch (NumberFormatException e) {
                // 處理數字轉換過程中的異常
                e.printStackTrace();
            }

            // 不再需要顯式關閉 FileReader，因為它已經在 try-with-resources 中被自動關閉
            fr.close();
            System.out.println("Finished writing correlation matrix!");  // 輸出處理完成訊息

        } catch (FileNotFoundException e) {
            // 處理檔案未找到的異常
            e.printStackTrace();
        } catch (IOException e) {
            // 處理其他 IO 異常
            e.printStackTrace();
        }
    }
}

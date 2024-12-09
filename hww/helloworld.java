package hww;

public class helloworld {
    // 計算 Pearson 相關係數的靜態方法
    public static double calculatePearsonCorrelation(double[] tempVar1, double[] tempVar2) {
        int n = tempVar1.length;  // 取得數據長度 (假設兩個數組長度相同)
        
        // 初始化變數來儲存計算過程中的中間值
        double sumX = 0.0, sumY = 0.0, sumXY = 0.0, sumX2 = 0.0, sumY2 = 0.0;

        // 遍歷數組進行逐步累加
        for (int i = 0; i < n; i++) {
            sumX += tempVar1[i];  // 累加 tempVar1 中的所有元素 (X 的總和)
            sumY += tempVar2[i];  // 累加 tempVar2 中的所有元素 (Y 的總和)
            sumXY += tempVar1[i] * tempVar2[i];  // 累加 X 和 Y 的乘積總和
            sumX2 += tempVar1[i] * tempVar1[i];  // 累加 X 的平方和
            sumY2 += tempVar2[i] * tempVar2[i];  // 累加 Y 的平方和
        }

        // 計算 Pearson 相關係數的分子 (n * sum(XY) - sum(X) * sum(Y))
        double numerator = n * sumXY - sumX * sumY;
        
        // 計算 Pearson 相關係數的分母 (sqrt((n * sum(X^2) - sum(X)^2) * (n * sum(Y^2) - sum(Y)^2)))
        double denominator = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));

        // 如果分母為零，則返回零，避免除以零的情況；否則返回計算出來的 Pearson 相關係數
        return (denominator == 0) ? 0 : numerator / denominator;
    }
}

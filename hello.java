import java.util.Scanner;
public class hello{
    public static void main(String[] args) {
         /*int x=9;
        System.out.println(x*10);
        System.out.println("hi");
        System.out.println(3.666);
        System.out.println("哈囉");*/
        Scanner s = new Scanner(System.in);
        System.out.println("請輸入數字:");
        int x=s.nextInt();
        System.out.println(x*2);
        s.close();
    }
}



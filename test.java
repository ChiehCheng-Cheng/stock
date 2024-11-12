public class test {
    public static void main(String[] args) {
        System.out.println("你好");
            for(int i=0; i<10; i++)
        {   System.out.println("i: "+i);
            if(i>5)
             break;
             int j = i*2;
             System.out.println("j:"+j);
        }
        
            /*int j = 0;
            while (j<0) 
            {
                System.out.println(j);
                j++;
                
            }*/
        {
            int k=0;
            int [] arrayTest  = new int[5];
            arrayTest[0]=10;
            for(k=0; k<arrayTest.length; k++)
            System.out.println(arrayTest[k]);

        }
        

       }
}

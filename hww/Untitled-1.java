                // 使用 BufferedWriter 將結果寫入檔案
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream("elephant.txt")))) {
                    
                    // 象的耳朵
                    writer.write("    _______________\n");
                    writer.write("   /               \\\n");
                    writer.write("  /                 \\\n");
                    
                    // 象的頭和眼睛
                    writer.write(" /   @           @   \\\n");
                    writer.write("|                     |\n");
                    writer.write("|     \\         /     |\n");
                    
                    // 象的鼻子
                    writer.write(" \\     \\_______/     /\n");
                    writer.write("  \\                 /\n");
                    writer.write("   \\_______________/\n");
                    writer.write("          ||  \n");
                    writer.write("          ||  \n");
                    
                    // 象的身體和腿
                    writer.write("    ____/||\\____\n");
                    writer.write("   |    ||||    |\n");
                    writer.write("   |____||||____|\n");
                    writer.write("       ||||  \n");
                    writer.write("      //||\\\\ \n");
                    writer.write("     // || \\\\ \n");
                    
                } catch (IOException e) {
                    System.out.println("寫入檔案時發生錯誤：" + e.getMessage());
                }

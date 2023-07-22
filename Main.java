import syntaxtree.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import myPackage.*;


public class Main {
    public static void main(String[] args) throws Exception {

        if(args.length < 1){
            System.err.println("Usage: java Main <inputFile>");
            System.exit(1);
        }

        
        for (String file : args){
            System.out.println();
            System.out.println("Checking file: " + file);

            FileInputStream fis = null;
            try{
                
                fis = new FileInputStream(file);
                MiniJavaParser parser = new MiniJavaParser(fis);

                Goal root = parser.Goal();

                System.err.println("Program parsed successfully.");
                LookUpTable symbolTable = new LookUpTable();
                SymbolTableVisitor symbolTableVisitor = new SymbolTableVisitor(symbolTable);
                TypeCheckVisitor typeCheckVisitor = new TypeCheckVisitor(symbolTable);
                IRGenVisitor IRVisitor = new IRGenVisitor(symbolTable);

                root.accept(symbolTableVisitor, null);
                root.accept(typeCheckVisitor, null);
                System.out.println("Semantic Check Passed");
                System.out.println("-----------------------");
                symbolTable.printOffsets();
                System.out.println("-----------------------");
                symbolTable.genVtables();
                root.accept(IRVisitor, null);

                try {
                    String fileName = file.replace(".java", ".ll");
                    FileWriter llFile = new FileWriter(fileName, false);
                    llFile.write(symbolTable.getBuffer());
                    llFile.close();
                    System.out.println(fileName + " produced successfully");
                }
                catch(IOException ex){
                    System.err.println(ex.getMessage());
                }

            }
            catch(ParseException ex){
                System.out.println(ex.getMessage());
            }
            catch(FileNotFoundException ex){
                System.err.println(ex.getMessage());
            }
            catch (TypeCheckError ex) {
                System.err.println(ex.getMessage());
            }
            finally{
                try{
                    if(fis != null) fis.close();
                }
                catch(IOException ex){
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}



package myPackage;

import java.util.HashMap;
import java.util.Map;

public class Register {
    int number;
    String regString;
    Map<String, Integer> regMap;

    public Register(){
        this.number = 0;
        this.regMap = new HashMap<String, Integer>();
    }

    public void reset(){
        this.number = 0;
        this.regMap.clear();
    }

    public String generate(String name){
        this.number++;

        if (this.regMap.get(name) != null){
            this.regMap.replace(name, number - 1);
        }
        else{
            this.regMap.put(name, number - 1);
        }
        
        return "%_" + (number - 1);
    }

    public String generate() {
        this.number++;

        return "%_" + (number - 1);
    }

    public String getRegister(String name){
        Integer reg = this.regMap.get(name);

        if (reg == null){
            return null;
        }

        return "%_" + reg;
    }

}

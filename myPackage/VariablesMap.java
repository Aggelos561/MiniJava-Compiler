package myPackage;

import java.util.LinkedHashMap;
import java.util.Map;

public class VariablesMap {
    
    // variable name --> type
    private Map<String, String> varsMap;

    public VariablesMap(){
        this.varsMap = new LinkedHashMap<String, String>();
    }

    public void insert(String name, String type) throws TypeCheckError {
        if ((varsMap.putIfAbsent(name, type)) != null){
            System.out.println("Duplicate local variable " + name);
            throw new TypeCheckError();
        }
    }

    public Map<String, String> getVarsMap(){
        return this.varsMap;
    }

    public String getVartype(String varName){
        return varsMap.get(varName);
    }


    public void print(){
        System.out.println("Variables Map: " + varsMap);
    }

}

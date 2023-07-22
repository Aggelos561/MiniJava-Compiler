package myPackage;

import java.util.LinkedList;
import java.util.List;

public class method {

    private String type;
    private String name;
    private String className;
    private int offset;

    private VariablesMap varMap;
    private List<String> argsList;
    
    public method(String type, String name, String className) {
        this.type = type;
        this.name = name;
        this.className = className;

        this.varMap = new VariablesMap();
        this.argsList = new LinkedList<String>();

        this.offset = 0;
    }

    public String getMethodType(){
        return this.type;
    }

    public String getMethodName(){
        return this.name;
    }

    public String getMethodClass(){
        return this.className;
    }

    public int getOffset() {
        return this.offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }


    public void insertArg(String arg, String type) throws TypeCheckError {
        varMap.insert(arg, type);
        argsList.add(type);
    }

    public VariablesMap getMethodArgs(){
        return this.varMap;
    }
    
    public List<String> getMethodArgsTypes() {
        return this.argsList;
    }

    public boolean samePrototype(method checkMethod){

        if (this.name.equals(checkMethod.getMethodName()) && !this.className.equals(checkMethod.getMethodClass())){
            if (!this.type.equals(checkMethod.getMethodType()) || !this.argsList.equals(checkMethod.getMethodArgsTypes())){
                return false;
            }
            else{
                return true;
            }
        }
        else{
            return true;
        }

    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (obj == null || obj.getClass() != this.getClass())
            return false;

        method new_method = (method) obj;

        return new_method.name.equals(this.name) && new_method.className.equals(this.className);
    }

    @Override
    public int hashCode() {
        String toHash = this.name + " " + this.className;
        return toHash.hashCode();
    }

    @Override
    public String toString() {
        return this.name;        
    }

}

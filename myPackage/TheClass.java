package myPackage;

import java.util.LinkedHashMap;
import java.util.Map;

public class TheClass  {
    
    private String name;
    private String parentName;
    private Map<String, Integer> fieldOffsetsMap;
    private int currOffset;

    public TheClass(String name, String parentName){
        this.name = name;
        this.parentName = parentName;

        this.fieldOffsetsMap = new LinkedHashMap<String, Integer>();
        this.currOffset = 0;
    }

    public void inserFieldOffset(String className, int offset){
        fieldOffsetsMap.putIfAbsent(className, offset);
    }

    public Integer getFieldOffset(String fieldName){
        return fieldOffsetsMap.get(fieldName);
    }

    public Map<String, Integer> getFieldOffsetMap(){
        return this.fieldOffsetsMap;
    }

    public Integer getLastField() {
        String lastKey = " ";
        for (String key : this.fieldOffsetsMap.keySet()){
            lastKey = key;
        }

        if (this.fieldOffsetsMap.get(lastKey) == null)
            return 0;
        
        else return this.fieldOffsetsMap.get(lastKey);
        
    }

    public int getCurrOffset(){
        return this.currOffset;
    }

    public void setCurrOffset(int offset){
        this.currOffset = offset;
    }

    public String getClassName(){
        return name;
    }

    public String getParentName() {
        return parentName;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (obj == null || obj.getClass() != this.getClass())
            return false;

        TheClass new_class = (TheClass) obj;

        return new_class.name.equals(this.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        if (parentName == null)
            return this.name;
        else
            return this.name + " extends " + this.parentName;
    }

}

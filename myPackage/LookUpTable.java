package myPackage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class LookUpTable {

    private Map<TheClass, VariablesMap> classTable;
    private Map<String, TheClass> findClassMap;
    private Map<method, VariablesMap> methodTable;
    private String IRString;

    public final String[] primitiveTypes = {"int", "int[]", "boolean", "boolean[]"};

    public LookUpTable(){
        this.classTable = new LinkedHashMap<TheClass, VariablesMap>();
        this.findClassMap = new LinkedHashMap<String, TheClass>();
        this.methodTable = new LinkedHashMap<method, VariablesMap>();
        this.IRString = "";
    }

    public void printOffsets(){

        boolean skipMain = false;

        for (TheClass thisClass : classTable.keySet()){
            
            if (!skipMain){
                skipMain = true;
                continue;
            }
                
            int fValue = 0;
            int mValue = 0;

            String className = thisClass.getClassName();
            String parentName = getParentString(className);

            if (parentName != null){

                TheClass parentClass = getParentClass(parentName);
                fValue = parentClass.getCurrOffset();
                
                method checkMethod = null;
                for (method key : methodTable.keySet()){
                    //It must not be main method
                    if (key.getMethodClass().equals(parentName) && !key.getMethodType().equals("void")){
                        checkMethod = key;
                    }
                }
                if (checkMethod != null){
                    mValue = checkMethod.getOffset() + 8;
                }

            }

            VariablesMap vMap = classTable.get(thisClass);
            Map<String, String> fieldsMap = vMap.getVarsMap();

            for (String fieldName : fieldsMap.keySet()){

                String type = fieldsMap.get(fieldName);

                thisClass.inserFieldOffset(fieldName, fValue);

                if (type.equals("boolean"))
                        fValue += 1;
                    else if (type.equals("int"))
                        fValue += 4;
                    else
                        fValue += 8;
            
                thisClass.setCurrOffset(fValue);
                System.out.println(className + "." + fieldName + " : " + thisClass.getFieldOffset(fieldName));
                    
            }

            for (method thisMethod : methodTable.keySet()) {

                if (thisMethod.getMethodClass().equals(className) && !checkIfOverriden(thisMethod)) {
                    thisMethod.setOffset(mValue);
                    mValue += 8;
                }

            }

            for (method thisMethod : methodTable.keySet()) {

                if (thisMethod.getMethodClass().equals(className) && checkIfOverriden(thisMethod) == false) {
                    System.out.println(className + "." + thisMethod.getMethodName() + " : " + thisMethod.getOffset());
                }

            }

        }

    }

    public String getParentString(String className){
        TheClass parent = this.findClassMap.get(className);

        if (parent != null){
            return parent.getParentName();
        }
        return null;
    }

    public TheClass getParentClass(String parentName) {
        return this.findClassMap.get(parentName);
    }

    public boolean checkIfOverriden(method checkMethod){

        String parent;
        String className = checkMethod.getMethodClass();

        do{

            parent = getParentString(className);

            if (parent != null){
                method thisMethod = new method(null, checkMethod.getMethodName(), parent);
                className = parent;
                if (methodTable.get(thisMethod) != null){
                    return true;
                }
            }

        } while(parent != null);

        return false;
    }

    
    public void insertClass(TheClass newClass, String identifier, String type) throws TypeCheckError {
        
        VariablesMap varMap;
        String parent;

        if ((parent = newClass.getParentName()) != null){
            TheClass findParent = new TheClass(parent, null);

            if (classTable.get(findParent) == null){
               System.out.println(findParent + " cannot be resolved to a type");
               throw new TypeCheckError();
            }
        }

        if (identifier == null && type == null){
            varMap = new VariablesMap();
            if (classTable.putIfAbsent(newClass, varMap) != null) {
                System.out.println("Class " + newClass.getClassName() + " is already defined");
                throw new TypeCheckError();
            }

            if (findClassMap.putIfAbsent(newClass.getClassName(), newClass) != null) {
                System.out.println("Class " + newClass.getClassName() + " is already be defined");
                throw new TypeCheckError();
            }


        }
        else if ((varMap = classTable.get(newClass)) != null){
            varMap.insert(identifier, type);
        }
        else{
            varMap = new VariablesMap();
            varMap.insert(identifier, type);
            if (classTable.putIfAbsent(newClass, varMap) != null) {
                System.out.println("Class " + newClass.getClassName() + " already be defined");
                throw new TypeCheckError();
            }
        }

    }

    public void insertMethod(method newMethod, String identifier, String type) throws TypeCheckError {

        VariablesMap varMap;

        if (identifier == null && type == null) {

            varMap = new VariablesMap();
            if (methodTable.putIfAbsent(newMethod, varMap) != null) {
                System.out.println("Duplicate method " + newMethod.getMethodName() + " in Class " + newMethod.getMethodClass());
                throw new TypeCheckError();
            }
            
        }
        else if ((varMap = methodTable.get(newMethod)) != null) {
            if ((newMethod.getMethodArgs().getVarsMap().get(identifier)) != null) {
                System.out.println("Duplicate local variable " + identifier + " in method " + newMethod.getMethodName() + " in Class " + newMethod.getMethodClass());
                throw new TypeCheckError();
            }
            varMap.insert(identifier, type);
        } 
        else {
            if ((newMethod.getMethodArgs().getVarsMap().get(identifier)) != null) {
                System.out.println("Duplicate local variable " + identifier + " in method " + newMethod.getMethodName() + " in Class " + newMethod.getMethodClass());
                throw new TypeCheckError();
            }
            varMap = new VariablesMap();
            varMap.insert(identifier, type);
            if (methodTable.putIfAbsent(newMethod, varMap) != null) {
                System.out.println("Duplicate method " + newMethod.getMethodName() + " in Class " + newMethod.getMethodClass());
                throw new TypeCheckError();
            }
        }

    }

    public boolean checkOverloading(method newMethod) throws TypeCheckError {

        String parent;
        method checkMethod = newMethod;

        do {
            
            parent = getParentString(checkMethod.getMethodClass());
            if (parent != null) {
                checkMethod = new method(newMethod.getMethodType(), newMethod.getMethodName(), parent);
                method temp = checkMethod;
            
                for (method keyMethod : methodTable.keySet()) {
                    if (keyMethod.equals(checkMethod)){
                        checkMethod = keyMethod;
                        break;
                    }

                }
                
                if (checkMethod != temp){
                    if (!newMethod.samePrototype(checkMethod)) {
                        System.out.println("Method " + newMethod.getMethodName() + " in Class " + newMethod.getMethodClass() + " overloaded in Class " + checkMethod.getMethodClass());
                        throw new TypeCheckError();
                    }
                }

                
            }

        } while (parent != null);


        return false;

    }

    public void insertArg(method inMethod, String identifier, String type) throws TypeCheckError {
  
        if (methodTable.get(inMethod) != null){
            for (method keyMethod : methodTable.keySet()){
                if (keyMethod.equals(inMethod)){
                    inMethod = keyMethod;
                    break;
                }
            }
            inMethod.insertArg(identifier, type);
        }
        else {
            System.out.println("Method " + inMethod.getMethodName() + " in Class " + inMethod.getMethodClass() + " has not been declared");
            throw new TypeCheckError();
        }
        
    }

    public boolean findType(String type){

        for (String primitive : primitiveTypes){
            if (type.equals(primitive)){
                return true;
            }
        }

        if ((findClassMap.get(type)) != null){
            return true;
        }

        return false;
    }

    public List<String> getMethodArgsAsList(String scope, String methodName) throws TypeCheckError {

        boolean flag = false;
        String parent = scope;

        do {

            scope = parent;
            method findMethod = new method(null, methodName, scope);
            for (method keyMethod : methodTable.keySet()){
                if (keyMethod.equals(findMethod)){
                    findMethod = keyMethod;
                    flag = true;
                    break;
                }
            }
            if(flag){
                if (findMethod.getMethodArgsTypes().size() == 0){
                    return new LinkedList<String>() {{ add(""); }};
                }

                return findMethod.getMethodArgsTypes();
            }
           
            parent = getParentString(parent);

        } while(parent != null);

        throw new TypeCheckError();

    }

    public int getFieldOffset(String className, String methodName, String identifier){

        String parentName;
        TheClass checkClass;
        Integer offset;

        checkClass = this.findClassMap.get(className);
        if ((offset = checkClass.getFieldOffset(identifier)) != null) {
            return offset;
        }

        do {
            if ((parentName = getParentString(className)) != null) {
                checkClass = this.findClassMap.get(parentName);
                className = parentName;

                if ((offset = checkClass.getFieldOffset(identifier)) != null){
                    return offset;
                }
            }
        } while (parentName != null);

        return -1;

    }


    public int getMethodOffset(String className, String methodName) {

        method findMethod = null;

        for (method thisMethod : this.methodTable.keySet()){
            if (thisMethod.getMethodClass().equals(className)){
                if (thisMethod.getMethodName().equals(methodName)) {
                    findMethod = thisMethod;
                    break;
                }
            }
            
        }

        String parent;
        do {

            parent = getParentString(className);
           
            if (parent != null) {
                className = parent;
                for (method checkMethod : this.methodTable.keySet()) {
                    if (checkMethod.getMethodClass().equals(className)) {
                        if (checkMethod.getMethodName().equals(methodName)) {
                            findMethod = checkMethod;
                            break;
                        }
                    }

                }
            }

        } while (parent != null);
    
        return findMethod.getOffset()/8;

    }

    public int totalSizeClass(String className){

        int size = 0;

        TheClass thisClass = this.findClassMap.get(className);

        Map<String, Integer> vMap = thisClass.getFieldOffsetMap();

        int mapSize = vMap.size();
        int counter = 1;

        for (String field : vMap.keySet()){
            if (counter == mapSize){
                size = vMap.get(field);
            }
            counter++;
        }

        return size;
    }


    public String checkType(String scope, String methodName, String identifier) {
        
        method checkMethod = new method(null, methodName, scope);
        TheClass checkClass = new TheClass(scope, null);

        VariablesMap mMap = methodTable.get(checkMethod);
        VariablesMap cMap = classTable.get(checkClass);
        String type;

        VariablesMap argsMap = null;
        for (method keyMethod : methodTable.keySet()){
            if (keyMethod.equals(checkMethod)){
                argsMap = keyMethod.getMethodArgs();
                break;
            }
        }

        if (argsMap != null){
            if ((type = argsMap.getVartype(identifier)) != null){
                return type;
            }
        }

        if ((type = mMap.getVartype(identifier)) != null) {
            return type;

        } 
        else if ((type = cMap.getVartype(identifier)) != null) {
            return type;
        } 
        else {
            String parentName;

            do {
                if ((parentName = getParentString(scope)) != null) {
                    checkClass = new TheClass(parentName, null);
                    scope = parentName;

                    if ((cMap = classTable.get(checkClass)) != null) {
                        if ((type = cMap.getVartype(identifier)) != null) {
                            return type;
                        }
                    }
                }
            } while (parentName != null);
        }

        return null;
    }


    public boolean isField(String scope, String methodName, String identifier) {

        method checkMethod = new method(null, methodName, scope);
        TheClass checkClass = new TheClass(scope, null);

        VariablesMap mMap = methodTable.get(checkMethod);
        VariablesMap cMap = classTable.get(checkClass);


        VariablesMap argsMap = null;
        for (method keyMethod : methodTable.keySet()) {
            if (keyMethod.equals(checkMethod)) {
                argsMap = keyMethod.getMethodArgs();
                break;
            }
        }

        if (argsMap != null) {
            if ((argsMap.getVartype(identifier)) != null) {
                return false;
            }
        }

        if ((mMap.getVartype(identifier)) != null) {
            return false;

        } else if ((cMap.getVartype(identifier)) != null) {
            return true;
        } else {
            String parentName;

            do {
                if ((parentName = getParentString(scope)) != null) {
                    checkClass = new TheClass(parentName, null);
                    scope = parentName;

                    if ((cMap = classTable.get(checkClass)) != null) {
                        if ((cMap.getVartype(identifier)) != null) {
                            return true;
                        }
                    }
                }
            } while (parentName != null);
        }

        return false;
    }


    public String findMethodType(String scope, String methodName){

        method checkMethod = new method(null, methodName, scope);
        String parent = scope;

        do {
            for (method keyMethod : methodTable.keySet()) {
                if (keyMethod.equals(checkMethod)) {
                    return keyMethod.getMethodType();
                }
            }
            
            parent = getParentString(scope);
            scope = parent;
            checkMethod = new method(null, methodName, parent);
           
        } while(parent != null);

        return null;
    }


    public String subType(String typeName, String parentClass){

        String parentName;

        do{
            if ((parentName = getParentString(typeName)) != null){
                typeName = parentName;
                if (parentName.equals(parentClass)){
                    return parentClass;
                }
            }
        } while(parentName != null);    

        return null;
    }

    public void writeBuffer(String buffer){
        this.IRString += buffer;
    }

    public String getBuffer(){
        return this.IRString;
    }

    public String getIRType(String type){
        if (type.equals("int")){
            return "i32";
        }
        else if(type.equals("boolean")){
            return "i1";
        }
        else if (type.equals("boolean[]") || type.equals("int[]")) {
            return "i32*";
        }
        else{
            return "i8*";
        }
    }

    public int countMethods(String className){
        int counter = 0;
        List<String> methodsList = new LinkedList<String>();

        for (method thisMethod : methodTable.keySet()){
            if (thisMethod.getMethodClass().equals(className) && !checkIfOverriden(thisMethod) && !thisMethod.getMethodType().equals("void")){
                methodsList.add(thisMethod.getMethodName());
                counter++;
            }
        }

        String parentName = null;
        do{
            parentName = getParentString(className);
            if (parentName != null){
                for (method thisMethod : methodTable.keySet()) {
                    if (thisMethod.getMethodClass().equals(parentName) && !checkIfOverriden(thisMethod) && !methodsList.contains(thisMethod.getMethodName()) && !thisMethod.getMethodType().equals("void")) {
                        methodsList.add(thisMethod.getMethodName());
                        counter++;
                    }
                }
                className = parentName;
            }
            
        } while(parentName != null);


        return counter;
    }

    public String getMethodType(String className, String methd){

        for (method thisMethod : methodTable.keySet()) {
            if (thisMethod.getMethodClass().equals(className)) {
                if (thisMethod.getMethodName().equals(methd))
                    return thisMethod.getMethodType();
            }
        }

        String parent;
        do {

            parent = getParentString(className);
            if (parent != null) {
                className = parent;
                for (method checkMethod : this.methodTable.keySet()) {
                    if (checkMethod.getMethodClass().equals(className)) {
                        if (checkMethod.getMethodName().equals(methd)) {
                            return checkMethod.getMethodType(); 
                        }
                    }

                }
            }

        } while (parent != null);

        return null;
    }

    public void genVtables() throws TypeCheckError{

        boolean skipMain = false;

        for (TheClass thisClass : classTable.keySet()) {

            if (skipMain == false) {
                skipMain = true;
                continue;
            }

            String IRvtable = "@" + thisClass.getClassName() + "_vtable = global [" + countMethods(thisClass.getClassName()) + " x i8*] [\n";

            if (thisClass.getParentName() == null) {

                int counter = 0;
                for (method thisMethod: methodTable.keySet()){
                    
                    if (thisMethod.getMethodClass().equals(thisClass.getClassName()) && !thisMethod.getMethodType().equals("void")){
                        List<String> argsList = getMethodArgsAsList(thisClass.getClassName(), thisMethod.getMethodName());
                        String type = thisMethod.getMethodType();
                        
                        if (counter == 0){
                            IRvtable += "\ti8* bitcast ( " + getIRType(type) + "(i8*";      
                        }
                        else{
                            IRvtable += ",\n\ti8* bitcast ( " + getIRType(type) + "(i8*";
                        }

                        for (String param : argsList) {
                            if (!param.equals(""))
                                IRvtable += ", " + getIRType(param);
                        }

                        IRvtable += ")* @" + thisClass.getClassName() + "_" + thisMethod.getMethodName() + " to i8*)";
                        counter++;
                    }

                }
                
                String writeString = IRvtable + "\n]\n\n";
                writeBuffer(writeString);
               
            } 
            else {

                String className = thisClass.getClassName();
                List<method> methodsList = new ArrayList<method>();
                List<String> classesList = new ArrayList<String>();

                classesList.add(className);
                String parentName;
                 
                do{
                    parentName = getParentString(className);

                    if (parentName != null){
                        classesList.add(0, parentName);
                    }

                    className = parentName;

                } while(parentName != null);
                
                for (String extendClass : classesList) {
                    for (method thisMethod : methodTable.keySet()){
                        if (thisMethod.getMethodClass().equals(extendClass) && !thisMethod.getMethodType().equals("void")){
                            methodsList.add(thisMethod);
                        }
                    }
                }

                List<method> RmDuplicatesList = new ArrayList<method>();

                for (method dupMethod : methodsList) {
                    boolean found = false;

                    for (method lastOccurance : methodsList) {
                        if (lastOccurance.getMethodName().equals(dupMethod.getMethodName()) && !lastOccurance.getMethodType().equals("void")) {
                            dupMethod = lastOccurance;
                        }
                    }

                    for (method dupMethod2 : RmDuplicatesList) {
                        
                        if (dupMethod2.getMethodName().equals(dupMethod.getMethodName()) && !dupMethod2.getMethodType().equals("void")){
                            found = true;
                            break;
                        }
                    }

                    if (found == false){
                        RmDuplicatesList.add(dupMethod);
                    }

                }

                
                int counter = 0;
                for (method thisMethod : RmDuplicatesList) {
                    List<String> argsList = getMethodArgsAsList(thisMethod.getMethodClass(), thisMethod.getMethodName());
                    String type = thisMethod.getMethodType();

                    if (counter == 0) {
                        IRvtable += "\ti8* bitcast ( " + getIRType(type) + "(i8*";
                    } 
                    else {
                        IRvtable += ",\n\ti8* bitcast ( " + getIRType(type) + "(i8*";
                    }
                   
                    for (String param : argsList) {
                        if (!param.equals(""))
                            IRvtable += ", " + getIRType(param);
                    }
                    
                    IRvtable += ")* @" + thisMethod.getMethodClass() + "_" + thisMethod.getMethodName() + " to i8*)";
                    
                    counter++;

                }
                String writeString = IRvtable + "\n]\n\n";
                writeBuffer(writeString);
            }

            

        }

        writeBuffer("declare i8* @calloc(i32, i32)\ndeclare i32 @printf(i8*, ...)\ndeclare void @exit(i32)\n\n");

        String dec_routines = "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n"
                            + "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n\n";

        String def_print = "define void @print_int(i32 %i) {\n"
                        + "\t%_str = bitcast [4 x i8]* @_cint to i8*\n"
                        + "\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n"
                        + "\tret void\n}\n\n";

        String def_oob = "define void @throw_oob() {\n"
                        + "\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n"
                        + "\tcall i32 (i8*, ...) @printf(i8* %_str)\n"
                        + "\tcall void @exit(i32 1)\n"
                        + "\tret void\n}\n\n";


        writeBuffer(dec_routines);
        writeBuffer(def_print);
        writeBuffer(def_oob);

    }
       
}


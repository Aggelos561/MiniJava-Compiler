import syntaxtree.*;
import visitor.*;

import java.util.LinkedList;
import java.util.List;

import myPackage.*;


class TypeCheckVisitor extends GJDepthFirst<String, String> {

    private LookUpTable symbolTable;
    private String currClass;
    private String currMethod;

    public TypeCheckVisitor(LookUpTable lookUpTable){
        this.symbolTable = lookUpTable;
        this.currClass = null;
        this.currMethod = null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */

    @Override
    public String visit(MainClass n, String argu) throws Exception {
        
        this.currClass = n.f1.accept(this, null);
        this.currMethod = "main";

        for (Node node : n.f14.nodes) {
            node.accept(this, null);
        }

        n.f15.accept(this, this.currClass);

        return null;
    }


    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */

    @Override
    public String visit(ClassDeclaration n, String argu) throws Exception {

        this.currClass = n.f1.accept(this, null);

        for (Node node : n.f3.nodes) {
            node.accept(this, null);
        }

        for (Node node : n.f4.nodes) {
            node.accept(this, this.currClass);
        }

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */

    @Override
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception {

        this.currClass = n.f1.accept(this, null);

        for (Node node : n.f5.nodes) {
            node.accept(this, null);
        }

        for (Node node : n.f6.nodes) {
            node.accept(this, this.currClass);
        }

        return null;
    }


    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */

    @Override
    public String visit(VarDeclaration n, String argu) throws Exception, TypeCheckError {

        String typename = n.f0.accept(this, null);
        String identifierName = n.f1.accept(this, null);

        if (!symbolTable.findType(typename)) {
            System.out.println(typename + " cannot be resolved as a type");
            throw new TypeCheckError();
        }

        return (typename + " " + identifierName);

    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */

    @Override
    public String visit(MethodDeclaration n, String argu) throws Exception, TypeCheckError {

        String type = n.f1.accept(this, null);
        this.currMethod = n.f2.accept(this, null);

        for (Node node : n.f7.nodes) {
            node.accept(this, null);
        }

        n.f8.accept(this, this.currClass);

        String returnType = n.f10.accept(this, "type");

        if (!type.equals(returnType)){
            if (symbolTable.subType(returnType, type) == null){
                System.out.println("Type mismatch: cannot convert from " + returnType + " to " + type);
                throw new TypeCheckError();
            }
        }
        
        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */

    @Override
    public String visit(FormalParameterList n, String argu) throws Exception, TypeCheckError {

        String ret = n.f0.accept(this, null);
        String type[] = ret.split(" ");

        if (!symbolTable.findType(type[0])) {
            System.out.println(type[0] + " cannot be resolved as a type");
            throw new TypeCheckError();
        }

        if (n.f1 != null) {
            ret = n.f1.accept(this, null);
        }

        return null;
    }

    @Override
    public String visit(FormalParameter n, String argu) throws Exception {
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);
        return type + " " + name;
    }

    @Override
    public String visit(FormalParameterTerm n, String argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    @Override
    public String visit(FormalParameterTail n, String argu) throws Exception, TypeCheckError {
        
        String ret;
        for (Node node : n.f0.nodes) {
            
            ret =  node.accept(this, null);
            String[] type = ret.split(" ");
            
            if (!symbolTable.findType(type[0])) {
                System.out.println(type[0] + " cannot be resolved as a type");
                throw new TypeCheckError();
            }

        }

        return null;
    }

    @Override
    public String visit(BooleanArrayType n, String argu) {
        return "boolean[]";
    }

    @Override
    public String visit(IntegerArrayType n, String argu) {
        return "int[]";
    }

    @Override
    public String visit(BooleanType n, String argu) {
        return "boolean";
    }

    @Override
    public String visit(IntegerType n, String argu) {
        return "int";
    }

    @Override
    public String visit(Block n, String argu) throws Exception {    
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public String visit(AssignmentStatement n, String argu) throws Exception, TypeCheckError {
        
        String type1 = n.f0.accept(this, "type");  
        String type2 = n.f2.accept(this, "type");

        if (!type1.equals(type2)){
            if ((symbolTable.subType(type2, type1)) == null){
                System.out.println("Type mismatch: cannot convert from " + type2 + " to " + type1);
                throw new TypeCheckError();
            }
        }
        
        return null;
    }


    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    @Override
    public String visit(ArrayAssignmentStatement n, String argu) throws Exception, TypeCheckError {
        
        String identType = n.f0.accept(this, "type");

        String expr1 = n.f2.accept(this, "type");

        if (!expr1.equals("int")){
            System.out.println("Type mismatch: cannot convert from " + expr1 + " to int");
            throw new TypeCheckError();
        }
        
        String expr2 = n.f5.accept(this, "type");

        if (!identType.replace("[]", "").equals(expr2)){
            System.out.println("Type mismatch: cannot convert from " + expr2 + " to " + identType.replace("[]", ""));
            throw new TypeCheckError();
        }

        return null;
    }



    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    @Override
    public String visit(MessageSend n, String argu) throws Exception, TypeCheckError {
        
        String methodClass = this.currClass;
        String objType = n.f0.accept(this, "type");
        
        this.currClass = objType;
        String methodType = n.f2.accept(this, null);

        String type;
        if ((type = symbolTable.findMethodType(this.currClass, methodType)) == null) {
            System.out.println("The method " + methodType + " is undefined for " + objType);
            throw new TypeCheckError();
        }

        this.currClass = methodClass;
        String argumentList = n.f4.present() ? n.f4.accept(this, "type") : "";
        
        List<String> checkTypes = new LinkedList<String>();
        String[] params = argumentList.split(", ");

        for (String parameter : params){
            checkTypes.add(parameter);
        }
        
        List<String> argsList = symbolTable.getMethodArgsAsList(objType, methodType);

        if (argsList == null){
            System.out.println("The method " + methodType + " is not applicable for the arguments");
            throw new TypeCheckError();
        }
        if (checkTypes.size() == argsList.size()){
            for (int i = 0; i < checkTypes.size();  i++){
                for (int j = 0; j < argsList.size(); j++){

                    if (i == j){
                        if (!checkTypes.get(i).equals(argsList.get(j))){
                            if (symbolTable.subType(checkTypes.get(i), argsList.get(j)) == null){
                                System.out.println("The method " + methodType + " is not applicable for the arguments");
                                throw new TypeCheckError();
                            }
                        }
                    }

                }
            }
        
        }
        else{
            System.out.println("The method " + methodType + " is not applicable for the arguments");
            throw new TypeCheckError();
        }

        return type;
    }

    @Override
    public String visit(Expression n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    @Override
    public String visit(ExpressionList n, String argu) throws Exception {
        
        String ret = n.f0.accept(this, "type");

        if (n.f1 != null) {
            ret += n.f1.accept(this, "type");
        }

        return ret;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    @Override
    public String visit(ExpressionTail n, String argu) throws Exception {

        String ret = "";
        for (Node node : n.f0.nodes) {
            ret += ", " + node.accept(this, "type");
        }

        return ret;
    }


    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    @Override
    public String visit(ExpressionTerm n, String argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(PlusExpression n, String argu) throws Exception, TypeCheckError {
        String leftExpr =  n.f0.accept(this, "type");
        String rightExpr = n.f2.accept(this, "type");
        
        if (!leftExpr.equals("int") || !rightExpr.equals("int")){
            System.out.println("Operator + is undefined for the argument types (" + leftExpr + ", " + rightExpr + ")");
            throw new TypeCheckError();
        }

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(MinusExpression n, String argu) throws Exception, TypeCheckError {
        String leftExpr = n.f0.accept(this, "type");
        String rightExpr = n.f2.accept(this, "type");

        if (!leftExpr.equals("int") || !rightExpr.equals("int")) {
            System.out.println("Operator - is undefined for the argument types (" + leftExpr + ", " + rightExpr + ")");
            throw new TypeCheckError();
        }

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(TimesExpression n, String argu) throws Exception, TypeCheckError {
        String leftExpr = n.f0.accept(this, "type");
        String rightExpr = n.f2.accept(this, "type");

        if (!leftExpr.equals("int") || !rightExpr.equals("int")) {
            System.out.println("Operator * is undefined for the argument types (" + leftExpr + ", " + rightExpr + ")");
            throw new TypeCheckError();
        }

        return "int";
    }


    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(CompareExpression n, String argu) throws Exception, TypeCheckError {
        String leftExpr = n.f0.accept(this, "type");
        String rightExpr = n.f2.accept(this, "type");

        if (!leftExpr.equals("int") || !rightExpr.equals("int")) {
            System.out.println("Operator < is undefined for the argument types (" + leftExpr + ", " + rightExpr + ")");
            throw new TypeCheckError();
        }

        return "boolean";
    }


    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    @Override
    public String visit(AndExpression n, String argu) throws Exception, TypeCheckError {
        String clause1 = n.f0.accept(this, "type");
        String clause2 = n.f2.accept(this, "type");
        
        if (!clause1.equals("boolean") || !clause2.equals("boolean")){
            System.out.println("Operator && is undefined for the argument types (" + clause1 + ", " + clause2 + ")");
            throw new TypeCheckError();
        }
        
        return "boolean";
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    @Override
    public String visit(NotExpression n, String argu) throws Exception, TypeCheckError {
        String clause = n.f1.accept(this, "type");

        if (!clause.equals("boolean")){
            System.out.println("Operator ! is undefined for the argument types (" + clause + ")");
            throw new TypeCheckError();
        }

        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    @Override
    public String visit(ArrayLength n, String argu) throws Exception, TypeCheckError {
        
        String array = n.f0.accept(this, "type");

        if (!array.equals("boolean[]") && !array.equals("int[]")){
            System.out.println("Type mismatch: cannot convert from " + array + " to boolean[] or int[]");
            throw new TypeCheckError();
        }

        return "int";
    }

       /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    @Override
    public String visit(ArrayLookup n, String argu) throws Exception, TypeCheckError {

        String type =  n.f0.accept(this, "type");

        if (type.equals("int[]") || type.equals("boolean[]")){
            type = type.replace("[]", "");
        }
        else{
            System.out.println("Type mismatch: cannot convert from " + type + " to " + "boolean[] or int[]");
            throw new TypeCheckError();
        }
        
        String index = n.f2.accept(this, "type");
        
        if (!index.equals("int")){
            System.out.println("Type mismatch: cannot convert from " + index + " to int");
            throw new TypeCheckError();
        }
        
        return type;
    }

    @Override
    public String visit(Identifier n, String argu) throws Exception, TypeCheckError {  

        String identifier = n.f0.toString();

        if (argu == null){
            return identifier;
        }
        else if (argu.equals("type")){
            String type;
            if ((type = symbolTable.checkType(this.currClass, this.currMethod, identifier)) == null) {
                System.out.println(identifier + " cannot be resolved to a variable");
                throw new TypeCheckError();
            }
            return type;
        }

        return identifier;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public String visit(PrintStatement n, String argu) throws Exception, TypeCheckError { 
        
        String printExprType =  n.f2.accept(this, "type");

        if (!printExprType.equals("int") && !printExprType.equals("boolean")){
            System.out.println("Type mismatch: cannot convert from " + printExprType + " to int or boolean");
            throw new TypeCheckError();
        }
        
        return printExprType;
    }


    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public String visit(WhileStatement n, String argu) throws Exception, TypeCheckError {
        
        String expr = n.f2.accept(this, "type");
        n.f4.accept(this, argu);

        if (expr.equals("boolean")){
            return "boolean";
        }
        else {
            System.out.println("Type mismatch: cannot convert from " + expr + " to boolean");
            throw new TypeCheckError();
        }

    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    public String visit(IfStatement n, String argu) throws Exception, TypeCheckError {

        String expr = n.f2.accept(this, "type");
        n.f4.accept(this, argu);
        n.f6.accept(this, argu);

        if (expr.equals("boolean")) {
            return "boolean";
        } 
        else {
            System.out.println("Type mismatch: cannot convert from " + expr + " to boolean");
            throw new TypeCheckError();
        }
    }


    /**
     * f0 -> "this"
     */
    @Override
    public String visit(ThisExpression n, String argu) throws Exception {
        n.f0.accept(this, argu);
        return this.currClass;
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    @Override
    public String visit(BracketExpression n, String argu) throws Exception {     
        return n.f1.accept(this, "type");
    }

    @Override
    public String visit(IntegerLiteral n, String argu) throws Exception {
        n.f0.accept(this, argu);
        return "int";
    }

    @Override
    public String visit(TrueLiteral n, String argu) throws Exception {
        n.f0.accept(this, argu);
        return "boolean";
    }

    @Override
    public String visit(FalseLiteral n, String argu) throws Exception {
        n.f0.accept(this, argu);
        return "boolean";
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    @Override
    public String visit(AllocationExpression n, String argu) throws Exception {
        return n.f1.accept(this, null);
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    @Override
    public String visit(IntegerArrayAllocationExpression n, String argu) throws Exception, TypeCheckError {      
        String expr = n.f3.accept(this, "type");
        
        if (!expr.equals("int")) {
            System.out.println("Type mismatch: cannot convert from " + expr + " to int");
            throw new TypeCheckError();
        }
        return "int[]";
        
    }

    @Override
    public String visit(ArrayAllocationExpression n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "new"
     * f1 -> "boolean"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    @Override
    public String visit(BooleanArrayAllocationExpression n, String argu) throws Exception, TypeCheckError{
        
        String expr = n.f3.accept(this, "type");

        if(expr.equals("int")){
            return "boolean[]";
        }

        else {
            System.out.println("Type mismatch: cannot convert from " + expr + " to int");
            throw new TypeCheckError();
        }
        
    }

}

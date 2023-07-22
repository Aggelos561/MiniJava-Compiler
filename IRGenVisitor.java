import syntaxtree.*;
import visitor.*;

import java.util.List;

import myPackage.*;


public class IRGenVisitor extends GJDepthFirst<String, String>{
    
    private LookUpTable symbolTable;
    private String currClass;
    private String currMethod;
    private String currType;
    private String currArray;
    private Register register;
    
    private int ifNumber, whileNumber, andNumber, arrAllocNumber;

    public IRGenVisitor(LookUpTable lookUpTable){
        this.symbolTable = lookUpTable;
        this.currClass = null;
        this.currMethod = null;
        this.currType = null;
        this.currArray = null;
        this.register = new Register();
        this.ifNumber = 0;
        this.whileNumber = 0;
        this.andNumber = 0;
        this.arrAllocNumber = 0;
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
        
        String IRString = "define i32 @main(){\nEntry:\n";
        symbolTable.writeBuffer(IRString);

        for (Node node : n.f14.nodes) {
            node.accept(this, "method");
        }

        n.f15.accept(this, this.currClass);

        symbolTable.writeBuffer("\tret i32 0\n}\n\n");
        
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
            node.accept(this, "class");
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
            node.accept(this, "class");
        }

        for (Node node : n.f6.nodes) {
            node.accept(this, this.currClass);
        }

        return null;
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
    public String visit(MethodDeclaration n, String argu) throws Exception {

        register.reset();

        this.currMethod = n.f2.accept(this, null);
        String methodType = symbolTable.getMethodType(this.currClass, this.currMethod);
        
        String IRString = "define " + symbolTable.getIRType(methodType) + " @" +this.currClass + "_" + this.currMethod;
        
        IRString += "(i8* %this";

        String parameters = n.f4.present() ? n.f4.accept(this, null) : "";
        String[] params = null;

        if (!parameters.equals("")){
            params = parameters.split(", ");
            for (String param : params){
                String[] splitted = param.split(" ");
                IRString += ", " + symbolTable.getIRType(splitted[0]) + " " + register.generate(splitted[1]);
            }
        }
        IRString += "){\n";
        symbolTable.writeBuffer(IRString);
        
        IRString = "";

        if (!parameters.equals("")){
            for (String param : params) {
                String[] splitted = param.split(" ");
                String prevReg = register.getRegister(splitted[1]);
                String paramReg = register.generate(splitted[1]);
                IRString += "\t"+paramReg + " = alloca "+ symbolTable.getIRType(splitted[0])+"\n\t" + "store " 
                + symbolTable.getIRType(splitted[0]) + " " + prevReg + ", " + symbolTable.getIRType(splitted[0]) + "* " + paramReg + "\n"; 
            }
        }

        symbolTable.writeBuffer(IRString);

        for (Node node : n.f7.nodes) {
            node.accept(this, "method");
        }

        n.f8.accept(this, null);

        String type = n.f1.accept(this, null);
        String returnVal = n.f10.accept(this, null);
        String loadRetVal = load(returnVal, type, true);
        
        symbolTable.writeBuffer("\tret " + symbolTable.getIRType(type) + " " + loadRetVal + "\n}\n\n");

        return null;
    }


    @Override
    public String visit(VarDeclaration n, String argu) throws Exception {

        String type = n.f0.accept(this, null);
        String identifier = n.f1.accept(this, null);

        if (argu != null && argu.equals("method")){
            String IRString = "\t" + register.generate(identifier) + " = alloca " + symbolTable.getIRType(type) + "\n";
            symbolTable.writeBuffer(IRString);
        }
        
        return null;

    }


    @Override
    public String visit(AssignmentStatement n, String argu) throws Exception {

        String identifier = n.f0.accept(this, null);
        String identType  = symbolTable.checkType(this.currClass, this.currMethod, identifier);

        String identReg = register.getRegister(identifier);

        if (identReg == null){
            identReg = load(identifier, identType, false);
        }
        
        String expr = n.f2.accept(this, null);
        
        String regExpr = load(expr, identType, true);
        
        String IRString = "\tstore "+ symbolTable.getIRType(identType) + " " + regExpr + ", " 
                        + symbolTable.getIRType(identType) + "* " + identReg + "\n";

        symbolTable.writeBuffer(IRString);

        return null;
    }


    /**
     * f0 -> AndExpression()
     * | CompareExpression()
     * | PlusExpression()
     * | MinusExpression()
     * | TimesExpression()
     * | ArrayLookup()
     * | ArrayLength()
     * | MessageSend()
     * | Clause()
     */

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

        String ret = n.f0.accept(this, null);

        if (n.f1 != null) {
            ret += n.f1.accept(this, null);
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
            ret += ", " + node.accept(this, null);
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
    public String visit(PlusExpression n, String argu) throws Exception {
        String regExpr1 = n.f0.accept(this, null);
        String regExpr2 = n.f2.accept(this, null);
        String reg1 = load(regExpr1, "int", true);
        String reg2 = load(regExpr2, "int", true);
        
        String reg = register.generate();

        String IRString = "\t" + reg + " = add i32 " + reg1 + ", " + reg2 + "\n";
        
        symbolTable.writeBuffer(IRString);

        return reg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(MinusExpression n, String argu) throws Exception {
        String regExpr1 = n.f0.accept(this, null);
        String regExpr2 = n.f2.accept(this, null);
        
        String reg1 = load(regExpr1, "int", true);
        String reg2 = load(regExpr2, "int", true);
        
        String reg = register.generate();

        String IRString = "\t" + reg + " = sub i32 " + reg1 + ", " + reg2 + "\n";
        
        symbolTable.writeBuffer(IRString);

        return reg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(TimesExpression n, String argu) throws Exception {
        String regExpr1 = n.f0.accept(this, null);
        String regExpr2 = n.f2.accept(this, null);
        
        String reg1 = load(regExpr1, "int", true);
        String reg2 = load(regExpr2, "int", true);
        
        String reg = register.generate();

        String IRString = "\t" + reg + " = mul i32 " + reg1 + ", " + reg2 + "\n";
        
        symbolTable.writeBuffer(IRString);

        return reg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(CompareExpression n, String argu) throws Exception {
        String regExpr1 = n.f0.accept(this, null);
        String regExpr2 = n.f2.accept(this, null);

        String reg1 = load(regExpr1, "int", true);
        String reg2 = load(regExpr2, "int", true);
        
        String reg = register.generate();

        String IRString = "\t" + reg + " = icmp slt i32 " + reg1 + ", " + reg2 + "\n";
        
        symbolTable.writeBuffer(IRString);

        return reg;
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    @Override
    public String visit(AndExpression n, String argu) throws Exception {

        String clause1 = n.f0.accept(this, null);
        String clause2 = n.f2.accept(this, null);

        String regClause1 = load(clause1, "boolean", true);
       
        String andLabel1 = "LabelAnd" + this.andNumber;
        this.andNumber++;
        String andLabel2 = "LabelAnd" + this.andNumber;
        this.andNumber++;
        String andLabel3 = "LabelAnd" + this.andNumber;
        this.andNumber++;
       
        String andRegFinal = register.generate();

        symbolTable.writeBuffer("\tbr i1 " + regClause1 + ", label %" + andLabel2 + ", label %" + andLabel1 + "\n");
        String IRString;

        IRString = andLabel2 + ":\n";
        symbolTable.writeBuffer(IRString);
        String regClause2 = load(clause2, "boolean", true);

        if (clause2.equals("1")) {
            regClause2 = "true";
        } 
        else if (clause2.equals("0")){
            regClause2 = "false";
        }

        IRString = "\n\tbr label %" + andLabel3 + "\n" +
                andLabel1 + ":\n\t" + "br label %" + andLabel3 + "\n" +
                andLabel3 + ":\n\t" +
                andRegFinal + " = phi i1 [ " + false + ", %" + andLabel1 + " ], [ " + regClause2 + ", %" + andLabel2 + " ]\n";
    

        symbolTable.writeBuffer(IRString);

        return andRegFinal;

    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    @Override
    public String visit(NotExpression n, String argu) throws Exception {
        String clause = n.f1.accept(this, null);

        String reg1 = load(clause, "boolean", true);
        String reg = register.generate();

        String IRString = "\t" + reg + " = xor i1 " + 1 + ", " + reg1 + "\n";
        symbolTable.writeBuffer(IRString);

        return reg;
    }


    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public String visit(PrintStatement n, String argu) throws Exception {
        String regExpr = n.f2.accept(this, null);

        String reg = load(regExpr, "int", true);

        String IRString = "\tcall void (i32) @print_int(i32 " + reg + ")\n";
        symbolTable.writeBuffer(IRString);

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
    public String visit(MessageSend n, String argu) throws Exception {
        
        String argumentList = n.f4.present() ? n.f4.accept(this, null) : "";
        String objType = n.f0.accept(this, "currType");
        String objTypeReg = load(objType, "class", true);

        String methodName = n.f2.accept(this, null);
        String methodType = symbolTable.getMethodType(this.currType, methodName);
    

        String bitCastReg = register.generate();
        String loadReg1 = register.generate();
        String getElemReg = register.generate();
        String loadReg2 = register.generate();
        String bitCastReg2 = register.generate();

        
        List<String> paramsList = symbolTable.getMethodArgsAsList(this.currType, methodName);


        String IRparams = "";
        int counter = 1;

        for (String param : paramsList) {
            if (!param.equals("")){
                IRparams += ", " + symbolTable.getIRType(param);
            }
            counter++;
        }

        String IRString = "\t" + bitCastReg + " = bitcast i8* " + objTypeReg + " to i8***\n\t" +
                        loadReg1 + " = load i8**, i8*** " + bitCastReg + "\n\t"
                        + getElemReg + " = getelementptr i8*, i8** " + loadReg1 + ", i32 " + 
                        symbolTable.getMethodOffset(this.currType, methodName) + "\n\t" +
                        loadReg2 + " = load i8*, i8** " + getElemReg + "\n\t" +
                        bitCastReg2 + " = bitcast i8* " + loadReg2 + " to " + 
                        symbolTable.getIRType(methodType) + "(i8*"+ IRparams + ")*\n";


        String[] argsSplit = argumentList.split(", ");
        IRparams = "";
        counter = 1;

        for (String param : paramsList) {
            if (!param.equals("")){
                IRparams += ", " + symbolTable.getIRType(param) + " " + load(argsSplit[counter - 1], param, true);
            }
            counter++;
        }

        String callReg = register.generate();
        IRString += "\t" + callReg + " = call " + symbolTable.getIRType(methodType) + " " + bitCastReg2 + "(i8* " + objTypeReg +
                    IRparams + ")\n";

        symbolTable.writeBuffer(IRString);

        return callReg;
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    @Override
    public String visit(AllocationExpression n, String argu) throws Exception {
        String className = n.f1.accept(this, "currType");
        
        int classSize = symbolTable.totalSizeClass(className) + 8;

        String callocReg = register.generate();
        String bitcastReg = register.generate();
        String getElemReg = register.generate();

        int methodsCounter = symbolTable.countMethods(className);
        
        String IRString = "\t" + callocReg + " = call i8* @calloc(i32 1, i32 "+ classSize +")\n\t" +
                            bitcastReg + " = bitcast i8* " + callocReg + " to i8***\n\t" +
                            getElemReg + " = getelementptr [" + methodsCounter + " x i8*], [" + methodsCounter + " x i8*]* @" +
                            className + "_vtable, i32 0, i32 0\n\t" +
                            "store i8** " + getElemReg + ", i8*** " + bitcastReg + "\n";

        symbolTable.writeBuffer(IRString);
        return callocReg;
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
    public String visit(IfStatement n, String argu) throws Exception {

        String reg = n.f2.accept(this, null);
        String ifReg = load(reg, "boolean", true);

        String ifLabael = "then" + this.ifNumber;
        this.ifNumber++;
        String elseLabel = "els" + this.ifNumber;
        this.ifNumber++;

        int continueLabel = this.ifNumber;
        this.ifNumber++;
        
        String IRSting = "\tbr i1 " + ifReg + ", label %" + ifLabael + ", label %" + elseLabel + "\n";

        IRSting += ifLabael + ":\n";
        symbolTable.writeBuffer(IRSting);

        n.f4.accept(this, argu);
        symbolTable.writeBuffer("\tbr label %continue" + continueLabel + "\n");
        
        IRSting = elseLabel + ":\n";
        symbolTable.writeBuffer(IRSting);
        
        n.f6.accept(this, argu);
        symbolTable.writeBuffer("\tbr label %continue" + continueLabel + "\n");
        
        symbolTable.writeBuffer("continue" + continueLabel + ":\n");
        
        return ifReg;
    }



    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public String visit(WhileStatement n, String argu) throws Exception {

        String whileLabel1 = "LabelWhile" + this.whileNumber;
        this.whileNumber++;

        String whileLabel2 = "StartWhile" + this.whileNumber;
        this.whileNumber++;

        String whileLabel3 = "ExitWhile" + this.whileNumber;
        this.whileNumber++;

        String IRString = "\tbr label %" + whileLabel1 + "\n" + whileLabel1 + ":\n";
        symbolTable.writeBuffer(IRString);

        String regExpr = n.f2.accept(this, null);
        String regExprReg = load(regExpr, "boolean", true);
    
        IRString = "\tbr i1 " + regExprReg + ", label %" + whileLabel2 + ", " +
                    "label %" + whileLabel3 + "\n" + whileLabel2 + ":\n";
        symbolTable.writeBuffer(IRString);
        
        n.f4.accept(this, argu);

        IRString = "\tbr label %" + whileLabel1 + "\n" + whileLabel3 + ":\n";
        symbolTable.writeBuffer(IRString);

        return null;

    }


    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */

    @Override
    public String visit(FormalParameterList n, String argu) throws Exception {

        String ret = n.f0.accept(this, null);
        
        if (n.f1 != null) {
            ret += n.f1.accept(this, null);
        }
    
        return ret;
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
    public String visit(FormalParameterTail n, String argu) throws Exception {

        String ret = "";

        for (Node node : n.f0.nodes) {

            ret += ", " + node.accept(this, null);

        }

        return ret;
    }


    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    @Override
    public String visit(IntegerArrayAllocationExpression n, String argu) throws Exception {
        String expr = n.f3.accept(this, null);

        String regExpr = load(expr, "int", true);

        String arrayErrorLabel = "arrayErrorLabel" + this.arrAllocNumber;
        this.arrAllocNumber++;
        
        String arrayPassLabel = "arrayPassLabel" + this.arrAllocNumber;
        this.arrAllocNumber++;

        
        String checkNegative = register.generate();
        String sizeReg = register.generate();
        String callocReg = register.generate();
        String bitCastReg = register.generate();

        String IRString = "\t" + checkNegative + " = icmp slt i32 " + regExpr + ", " + 0 + "\n" +
                        "\tbr i1 " + checkNegative + ", label %" + arrayErrorLabel + ", label %" + arrayPassLabel + "\n" +
                        arrayErrorLabel +":\n\tcall void @throw_oob()\n\tbr label %" + arrayPassLabel + "\n" + 
                        arrayPassLabel + ":\n\t" + sizeReg + " = add i32 " + regExpr + ", 1\n\t" +
                        callocReg + " = call i8* @calloc(i32 " + sizeReg + ", i32 4)\n\t" +
                        bitCastReg + " = bitcast i8* " + callocReg + " to i32*\n" + 
                        "\tstore i32 " + regExpr + ", i32* " + bitCastReg + "\n";
        
        symbolTable.writeBuffer(IRString);

        return bitCastReg;

    }



    /**
     * f0 -> "new"
     * f1 -> "boolean"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    @Override
    public String visit(BooleanArrayAllocationExpression n, String argu) throws Exception {

        String expr = n.f3.accept(this, null);

        String regExpr = load(expr, "int", true);

        String arrayErrorLabel = "arrayErrorLabel" + this.arrAllocNumber;
        this.arrAllocNumber++;

        String arrayPassLabel = "arrayPassLabel" + this.arrAllocNumber;
        this.arrAllocNumber++;

        String checkNegative = register.generate();
        String sizeReg = register.generate();
        String callocReg = register.generate();
        String bitCastReg = register.generate();

        String IRString = "\t" + checkNegative + " = icmp slt i32 " + regExpr + ", " + 0 + "\n" +
                "\tbr i1 " + checkNegative + ", label %" + arrayErrorLabel + ", label %" + arrayPassLabel + "\n" +
                arrayErrorLabel + ":\n\tcall void @throw_oob()\n\tbr label %" + arrayPassLabel + "\n" +
                arrayPassLabel + ":\n\t" + sizeReg + " = add i32 " + regExpr + ", 1\n\t" +
                callocReg + " = call i8* @calloc(i32 " + sizeReg + ", i32 1)\n\t" +
                bitCastReg + " = bitcast i8* " + callocReg + " to i32*\n" +
                "\tstore i32 " + regExpr + ", i32* " + bitCastReg + "\n";

        symbolTable.writeBuffer(IRString);

        return bitCastReg;
        
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
    public String visit(ArrayAssignmentStatement n, String argu) throws Exception {

        String identifier = n.f0.accept(this, "currArray");
        
        
        String expr1 = n.f2.accept(this, null);
        String expr2 = n.f5.accept(this, null);

        String identReg = load(identifier, "int[]", true);
        
        String indexReg = load(expr1, "int", true);
        String assignReg = load(expr2, this.currArray.replace("[]", ""), true);

        String sizeReg = register.generate();
        String icmpUltReg = register.generate();
        String findIndexReg = register.generate();
        String getPtr = register.generate();

        String arrLabel1 = "oob" + this.arrAllocNumber;
        this.arrAllocNumber++;
        String arrLabel2 = "oob" + this.arrAllocNumber;
        this.arrAllocNumber++;
        String arrLabel3 = "oob" + this.arrAllocNumber;
        this.arrAllocNumber++;
        String IRString;

        if (this.currArray.equals("int[]")){
            IRString = "\t" + sizeReg + " = load i32, i32* " + identReg + "\n" +
                    "\t" + icmpUltReg + " = icmp ult i32 " + indexReg + ", " + sizeReg + "\n" +
                    "\tbr i1 " + icmpUltReg + ", label %" + arrLabel1 + ", label %" + arrLabel2 + "\n" +
                    arrLabel1 + ":\n\t" + findIndexReg + " = add i32 " + indexReg + ", " + 1 + "\n\t" +
                    getPtr + " = getelementptr i32, i32* " + identReg + ", i32 " + findIndexReg + "\n" +
                    "\tstore i32 " + assignReg + ", i32* " + getPtr + "\n\tbr label %" + arrLabel3 + "\n" +
                    arrLabel2 + ":\n\t" + "call void @throw_oob()\n" + "\tbr label %" + arrLabel3 + "\n" + 
                    arrLabel3 + ":\n";
        }
        else{
            String bitCastReg = register.generate();

            IRString = "\t" + sizeReg + " = load i32, i32* " + identReg + "\n" +
                    "\t" + icmpUltReg + " = icmp ult i32 " + indexReg + ", " + sizeReg + "\n" +
                    "\tbr i1 " + icmpUltReg + ", label %" + arrLabel1 + ", label %" + arrLabel2 + "\n" +
                    arrLabel1 + ":\n\t" + findIndexReg + " = add i32 " + indexReg + ", " + 1 + "\n\t" +
                    getPtr + " = getelementptr i32, i32* " + identReg + ", i32 " + findIndexReg + "\n\t" +
                    bitCastReg + " = bitcast i32* " + getPtr + " to i1*\n" +
                    "\tstore i1 " + assignReg + ", i1* " + bitCastReg + "\n\tbr label %" + arrLabel3 + "\n" +
                    arrLabel2 + ":\n\t" + "call void @throw_oob()\n" + "\tbr label %" + arrLabel3 + "\n" +
                    arrLabel3 + ":\n";
        }

        symbolTable.writeBuffer(IRString);

        return null;
    }



    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    @Override
    public String visit(ArrayLookup n, String argu) throws Exception, TypeCheckError {

        String expr = n.f0.accept(this, "currArray");
        String index = n.f2.accept(this, null);        
        
        String arrayReg = load(expr, "int[]", true);
        String indexReg = load(index, "int", true);
        String IRString;

        String lookupLabel1 = "lookupLabel" + this.arrAllocNumber;
        this.arrAllocNumber++;
        String lookupLabel2 = "lookupLabel" + this.arrAllocNumber;
        this.arrAllocNumber++;
        String lookupLabel3 = "lookupLabel" + this.arrAllocNumber;
        this.arrAllocNumber++;

        String sizeReg = register.generate();
        String loadValReg = register.generate();
        String icmpUltReg = register.generate();
        String findIndexReg = register.generate();
        String getPtr = register.generate();
        String valueReg = register.generate();

        if (this.currArray.equals("int[]")){

            IRString = "\t" + sizeReg + " = getelementptr i32, i32* " + arrayReg + ", i32 " + 0 + "\n\t"+
                    loadValReg + " = load i32, i32* " + sizeReg +
                    "\n\t" + icmpUltReg + " = icmp ult i32 " + indexReg + ", " + loadValReg + "\n" +
                    "\tbr i1 " + icmpUltReg + ", label %" + lookupLabel1 + ", label %" + lookupLabel2 + "\n" +
                    lookupLabel1 + ":\n\t" + findIndexReg + " = add i32 " + indexReg + ", " + 1 + "\n\t" +
                    getPtr + " = getelementptr i32, i32* " + arrayReg + ", i32 " + findIndexReg + "\n\t" +
                    valueReg + " = load i32, i32* " + getPtr +
                    "\n\tbr label %" + lookupLabel3 + "\n" +
                    lookupLabel2 + ":\n\t" + "call void @throw_oob()\n" + "\tbr label %" + lookupLabel3 + "\n" +
                    lookupLabel3 + ":\n";

                    symbolTable.writeBuffer(IRString);
                    return valueReg;
            
        }
        else{

            String boolPtr = register.generate();

            IRString = "\t" + sizeReg + " = getelementptr i32, i32* " + arrayReg + ", i32 " + 0 + "\n\t" +
                    loadValReg + " = load i32, i32* " + sizeReg +
                    "\n\t" + icmpUltReg + " = icmp ult i32 " + indexReg + ", " + loadValReg + "\n" +
                    "\tbr i1 " + icmpUltReg + ", label %" + lookupLabel1 + ", label %" + lookupLabel2 + "\n" +
                    lookupLabel1 + ":\n\t" + findIndexReg + " = add i32 " + indexReg + ", " + 1 + "\n\t" +
                    getPtr + " = getelementptr i32, i32* " + arrayReg + ", i32 " + findIndexReg + "\n\t" +
                    valueReg + " = load i32, i32* " + getPtr + "\n\t" +
                    boolPtr + " = trunc i32 " + valueReg + " to i1\n" +
                    "\n\tbr label %" + lookupLabel3 + "\n" +
                    lookupLabel2 + ":\n\t" + "call void @throw_oob()\n" + "\tbr label %" + lookupLabel3 + "\n" +
                    lookupLabel3 + ":\n";

            symbolTable.writeBuffer(IRString);
            return boolPtr;

        }

    }


    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    @Override
    public String visit(ArrayLength n, String argu) throws Exception, TypeCheckError {

        String array = n.f0.accept(this, null);
        String arrayReg = load(array, "int[]", true);

        String lengthReg = register.generate();

        String IRString = "\t" + lengthReg + " = load i32, i32* " + arrayReg + "\n";

        symbolTable.writeBuffer(IRString);

        return lengthReg;
    }

    @Override
    public String visit(ThisExpression n, String argu) throws Exception {
        this.currType = this.currClass;
        n.f0.accept(this, argu);
        return "%this";
    }


    @Override
    public String visit(Identifier n, String argu) throws Exception {

        String identifier = n.f0.toString();

        if (this.currClass != null && this.currMethod != null && argu != null && argu.equals("currType")){
            
            this.currType = symbolTable.checkType(this.currClass, this.currMethod, identifier);
            if (this.currType == null){
                this.currType = identifier;
            }
        
        }

        if (this.currClass != null && this.currMethod != null && argu != null && argu.equals("currArray")) {

            this.currArray = symbolTable.checkType(this.currClass, this.currMethod, identifier);
            if (this.currArray == null) {
                this.currArray = identifier;
            }
           

        }
        
        return identifier;
    }
    


    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    @Override
    public String visit(BracketExpression n, String argu) throws Exception {
        return n.f1.accept(this, null);
    }

    @Override
    public String visit(IntegerLiteral n, String argu) throws Exception {
        return n.f0.toString();
    }

    @Override
    public String visit(TrueLiteral n, String argu) throws Exception {
        n.f0.accept(this, argu);
        return "1";
    }

    @Override
    public String visit(FalseLiteral n, String argu) throws Exception {
        n.f0.accept(this, argu);
        return "0";
    }

    @Override
    public String visit(BooleanArrayType n, String argu) throws Exception {
        return "boolean[]";
    }

    @Override
    public String visit(IntegerArrayType n, String argu) throws Exception {
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


    public String load(String expr, String type, boolean loadit){
        String retReg = register.getRegister(expr);

        if (retReg != null) {
            String reg = register.generate();
            
            type = symbolTable.getIRType(type);
            symbolTable.writeBuffer("\t" + reg + " = load " + type + ", " + type + "* " + retReg + "\n");
            return reg;
        }
        else{

            if (symbolTable.isField(this.currClass, this.currMethod, expr)) {

                String getelemReg = register.generate();
                String bitCastReg = register.generate();
               
                int offset = symbolTable.getFieldOffset(this.currClass, this.currMethod, expr);
                String IRString = "\t" + getelemReg + " = getelementptr i8, i8* %this, i32 " + (8 + offset) + "\n\t" +
                                bitCastReg + " = bitcast i8* " + getelemReg + " to " + symbolTable.getIRType(type) + "*\n";
                
                if (loadit){
                    String loadReg = register.generate();
                    IRString += "\t" + loadReg + " = load " + symbolTable.getIRType(type) + ", " + symbolTable.getIRType(type) + "* " + bitCastReg + "\n";
                    symbolTable.writeBuffer(IRString);
                    return loadReg;
                }

                symbolTable.writeBuffer(IRString);
                return bitCastReg;
            }


            return expr;
        }
        
    }

}

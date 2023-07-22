import myPackage.*;
import visitor.*;
import syntaxtree.*;

class SymbolTableVisitor extends GJDepthFirst<String, String> {

    private LookUpTable symbolTable;

    public SymbolTableVisitor(LookUpTable symbolTable) {
        this.symbolTable = symbolTable;
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
    public String visit(MainClass n, String argu) throws Exception, TypeCheckError {

        String classname = n.f1.accept(this, null);
        String arg = n.f11.accept(this, null);

        TheClass newClass = new TheClass(classname, null);
        symbolTable.insertClass(newClass, null, null);

        method newMethod = new method("void", "main", classname);
        symbolTable.insertMethod(newMethod, arg, "String[]");

        String vardecl;
        for (Node node : n.f14.nodes) {
            vardecl = node.accept(this, null);
            String[] splitted = vardecl.split(" ");
            symbolTable.insertMethod(newMethod, splitted[1], splitted[0]);
        }

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
    public String visit(ClassDeclaration n, String argu) throws Exception, TypeCheckError {

        String classname = n.f1.accept(this, null);
        TheClass thisClass = new TheClass(classname, null);
        symbolTable.insertClass(thisClass, null, null);

        String vardecl;
        for (Node node : n.f3.nodes) {
            vardecl = node.accept(this, null);
            String[] splitted = vardecl.split(" ");
            
            symbolTable.insertClass(thisClass, splitted[1], splitted[0]);
        }

        for (Node node : n.f4.nodes) {
            node.accept(this, classname);
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
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception, TypeCheckError {
        
        String classname = n.f1.accept(this, null);
        String extended = n.f3.accept(this, null);

        TheClass thisClass = new TheClass(classname, extended);
        symbolTable.insertClass(thisClass, null, null);

        String vardecl;
        for (Node node : n.f5.nodes) {
            vardecl = node.accept(this, null);
            String[] splitted = vardecl.split(" ");
            
            symbolTable.insertClass(thisClass, splitted[1], splitted[0]);
        }

        for (Node node : n.f6.nodes) {
            node.accept(this, classname);
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

        String classname = argu;
        String type = n.f1.accept(this, null);
        String methodName = n.f2.accept(this, null);
       
        method newMethod = new method(type, methodName, classname);
        symbolTable.insertMethod(newMethod, null, null);

        String argumentList = n.f4.present() ? n.f4.accept(this, null) : "";
        String[] arguments = argumentList.split(", ");

        for (String arg : arguments) {
            if (!arg.equals("")) {
                String[] splitted = arg.split(" ");
                symbolTable.insertArg(newMethod, splitted[1], splitted[0]);
            }
        }

        String vardecl;
        for (Node node : n.f7.nodes) {
            vardecl = node.accept(this, null);
            String[] splitted = vardecl.split(" ");
            symbolTable.insertMethod(newMethod, splitted[1], splitted[0]);
        }

        symbolTable.checkOverloading(newMethod);
        return null;

    }

    @Override
    public String visit(FormalParameter n, String argu) throws Exception {
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);
        return type + " " + name;
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
    public String visit(FormalParameterTerm n, String argu) throws Exception{
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

    @Override
    public String visit(Type n, String argu) throws Exception {

        String typename = n.f0.accept(this, argu);

        return typename;
    }

    @Override
    public String visit(Identifier n, String argu) throws Exception {
        return n.f0.toString();
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

}

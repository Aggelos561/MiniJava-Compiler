all: compile

compile:
	java -jar jtb132di.jar -te minijava.jj
	java -jar javacc5.jar minijava-jtb.jj
	javac ./myPackage/TypeCheckError.java
	javac ./myPackage/Register.java
	javac ./myPackage/VariablesMap.java
	javac ./myPackage/method.java
	javac ./myPackage/TheClass.java
	javac ./myPackage/LookUpTable.java
	javac SymbolTableVisitor.java
	javac TypeCheckVisitor.java
	javac IRGenVisitor.java
	javac Main.java
	
clean:
	rm -f *.class *~

#Change or add any .java files 
execute:
	@if [ -n "$(FILE)" ]; then \
		java Main $(FILE); \
	elif [ -n "$(DIR)" ]; then \
		find $(DIR) -name "*.java" -exec java Main {} \;; \
	else \
		echo "Please provide either FILE=[file1.java] or DIR=[directory_path]"; \
	fi

# MiniJava Compiler

This is a compiler for MiniJava, a subset of the Java programming language. MiniJava supports basic object-oriented features like classes, fields, methods, and single inheritance. It also includes arrays, logical operators, control flow statements, and simple expressions using integers.

## Language Overview

Here's a brief overview of what MiniJava supports:

- **Class Declaration**: Define classes with fields and methods. Fields are protected, and methods are inherently polymorphic (virtual).

- **Inheritance**: Classes can extend other classes, inheriting their fields and methods. Method overriding is allowed.

- **Basic Types**: MiniJava supports `int`, `boolean`, `int[]` (arrays of integers), and `boolean[]` (arrays of booleans).

- **Method Calls**: Call methods using the dot notation (`object.method()`), passing parameters as arguments.

- **Arrays**: Perform array operations, including array access (`array[index]`) and getting the size of an array using the `length` attribute.

- **Control Flow**: MiniJava has `if-else` statements and `while` loops for control flow.

- **Logical Operators**: Supports `&&` (logical AND) and `!` (logical NOT) operators.

- **Constructor and Destructor**: No explicit constructors or destructors; objects are created with the `new` operator.

- **Scoping and Shadowing**: Local variables are defined at the beginning of a method and can shadow class fields.

- **Static Methods and Fields**: MiniJava does not support static methods or fields.

- **Special Method `main`**: The `main` method serves as the entry point for the MiniJava program.

## Compiler Features

The MiniJava compiler is implemented using JavaCC and JTB tools. It consists of several visitors that perform different tasks:

1. **Parser**: The MiniJava source code is parsed using the BNF grammar (`minijava.jj`) to construct an Abstract Syntax Tree (AST).

2. **Semantic Analysis**: The AST is traversed by the TypeCheckVisitor to perform static semantic analysis, ensuring that the program adheres to the MiniJava language rules.

3. **Intermediate Representation (IR) Generation**: The IRGenVisitor generates LLVM intermediate representation (IR) code for further compilation.

## How to Compile and Execute

To compile the MiniJava compiler and execute the examples, follow these steps:

1. Use the provided Makefile to compile the MiniJava compiler:

   ```bash
   make
   ```

2. Run the MiniJava compiler on a specific file or a folder containing MiniJava files:

   ```bash
   make execute FILE=path/to/your_file.java
   ```
   or
   ```bash
   make execute FOLDER=path/to/your_folder
   ```

   The compiled LLVM IR files for each MiniJava source file will be generated in the same folder with `.ll` extensions.

3. You can then use Clang or another LLVM-based compiler to compile the generated IR code into an executable:

   ```bash
   clang -o output_file out1.ll
   ```

   Finally, execute the output file:

   ```bash
   ./output_file
   ```

## MiniJava Examples

The `minijava-examples/passed` folder contains MiniJava example files that should compile successfully. The `minijava-examples/errors` folder contains files that are expected to throw errors during compilation due to semantic issues.


Feel free to contribute, report issues, or suggest improvements.

Please note that this is a simplified version of the MiniJava compiler, and it may not cover all edge cases and features of the full Java language.
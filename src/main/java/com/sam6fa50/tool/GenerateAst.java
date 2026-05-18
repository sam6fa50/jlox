package com.sam6fa50.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    private static final List<String> Exprs = Arrays.asList(
            "Assign   : Token name, Expr value",
            "Binary   : Token operator, Expr left, Expr right",
            "Ternary  : Expr condition, Expr left, Expr right",
            "Grouping : Expr expression",
            "Literal  : Object value",
            "Logical  : Token operator, Expr left, Expr right",
            "Unary    : Token operator, Expr right",
            "Variable : Token name"
    );

    private static final List<String> Stmts = Arrays.asList(
            "Block      : List<Stmt> statements",
            "Expression : Expr expression",
            "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
            "Print      : Expr expression",
            "Var        : Token name, Expr initializer",
            "While      : Expr condition, Stmt body"
    );

    static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        defineAst(outputDir, "Stmt", Stmts);
    }

    private static void defineAst
            (String outputDir, String baseName, List<String> types)
            throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8);

        writer.println("package com.sam6fa50.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");

        // accept() method
        writer.println("    abstract <R> R accept(Visitor<R> visitor);");
        writer.println();

        defineVisitor(writer, baseName, types);
        writer.println();

        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
            writer.println();
        }

        writer.println("}");
        writer.close();
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        writer.println("    static class " + className + " extends " + baseName + " {");

        // Extract fields
        String[] fields = fieldList.split(", ");

        // Fields
        for (String field : fields) {
            writer.println("        final " + field + ";");
        }

        writer.println();

        // Constructor
        writer.println("        " + className + "(" + fieldList + ") {");

        // Store parameters in fields
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("            this." + name + " = " + name + ";");
        }

        writer.println("        }");

        // Visitor pattern
        writer.println();
        writer.println("        @Override");
        writer.println("        <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" + className + baseName + "(this);");
        writer.println("        }");

        writer.println("    }");
    }

    // The visitor pattern's entire point is to decouple data from functionality. It says that hey, since we're in a
    // OOP-only language, we do need to have functionality in the data to trigger its usage, and that's what 'accept' is
    // for, but the actual implementation of the usage should be governed by the user of the data. So a parser would
    // use the data differently than an interpreter, and so on so forth. It evaluates as follows:
    // (Expected return type) visited.accept(Visitor<Expected return type>: visitor); // Which returns
    // (Expected return type) visitor.visitVisited(visited Type: this);
    // visitor then goes and consumes it as needed, example print function would do something like:
    // String visitVisited { System.out.println(visited.name); }
    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("    interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("        R visit" + typeName + baseName + "(" +
                    typeName + " " + baseName.toLowerCase() + ");");
            writer.println();
        }

        writer.println("    }");
    }
}

package com.sam6fa50.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    public Environment() {
        this.enclosing = null;
    }

    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    // Notice, getting a variable value (evaluating) throws a runtime error because named identifiers can be referred to
    // without being used. Example: recursive function names. Therefore, Lox delegates evaluation to runtime
    Object get(Token name) {
        if (values.containsKey(name.lexeme())) {
            if (!(values.get(name.lexeme()) instanceof Interpreter.UndefinedIdentifier)) {
                return values.get(name.lexeme());
            }

            throw new RuntimeError(name, "Variable " + name.lexeme() + " has not been assigned a value.");
        }

        // Handle recursion, check if the identifier is defined in the super-scope
        if (enclosing != null) return enclosing.get(name);

        throw new RuntimeError(name, "Undefined variable: '" + name.lexeme() + "'.");
    }

    // Assigning a variable itself throws a runtime error because assignment is a usage and not referral, so it's evaluation
    // is delegated to runtime.
    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme())) {
            values.put(name.lexeme(), value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable: '" + name.lexeme() + "'.");
    }

    // Definition is a
    void define(String name, Object value) {
        values.put(name, value);
    }
}

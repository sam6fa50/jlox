package com.sam6fa50.lox;

import java.util.List;

import static com.sam6fa50.lox.TokenType.*;

public class Parser {

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    // We follow precedence rules, so the last way we want to treat an expression is as an equality, and check if we need
    // to upgrade it to beyond.
    private Expr expression() {
        return equality();
    }

    // Check if the current token is of the expected type, then go to the next token
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    // Check if the current token is of the expected type, then go to the next token if so. This function EXPECTS
    // the token to match or else throws an error as that implicates unfinishedness of an expression
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    // Check token type helper
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type() == type;
    }

    // Move on to the next token and return the token that the parser was just at.
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    // synchronize keeps advancing tokens without use until the interpreter can once again analyze code without failure.
    // This occurs at the beginning of a class, function, variable, for loop, if branch, while loop, print, and return statements.
    private void synchronize() {
        advance(); // Definitely consume the erroneous token

        while (!isAtEnd()) {
            if (previous().type() == SEMICOLON) return;

            switch (peek().type()) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }

    // Following precedence rules, if we have an expression that is currently marked as an equality, we check if
    // it can be upgraded to a comparison on either end
    private Expr equality() {
        // The left side token, anything that precedes an equality, so comparison upwards:
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous(); // The previous token that was the operator
            Expr right = comparison(); // Logic is, consume the next tokens recursively as the right side after the operator
            expr = new Expr.Binary(expr, operator, right);
        }

        // If there is no right side, just return this expression up as it's the processed result of the recursion down
        // below
        return expr;
    }

    // If we have a comparison expression, we check if it can be upgraded to a term on either end
    private Expr comparison() {
        // The left side token, anything that precedes an equality, so comparison upwards:
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        // If there is no right side, just return this expression up as it's the processed result of the recursion down
        // below
        return expr;
    }

    // If we have a term expression, we check if it can be upgraded to a factor on either end
    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // If we have a factor expression, we check if it can be upgraded to a unary on either end
    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // If we have a unary expression, we check if it can be upgraded to a primary on the right terminal
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    // If we have a primary expression, we check if it becomes a literal, or if it can be upgraded to a nested expression
    // on the right end
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal()); // Literal with Literal Value
        }

        // Nested expression handling
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            // Expect a right parenthesis to enclose the statement
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        // If we literally find no expression at the end, we throw an error.
        throw error(peek(), "Expect expression.");
    }

    // Purpose of ParseError class is to represent parser errors as objects that we can use to get the parser back on
    // track rather than actually throwing a Java error that breaks our program loop. Based on its contents, we decide
    // whether we want to actually synchronize.
    private static class ParseError extends RuntimeException {

    }
}

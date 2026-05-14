package com.sam6fa50.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sam6fa50.lox.TokenType.*;

class Scanner {
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("fun", FUN);
        keywords.put("for", FOR);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
    }

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int line = 1;
    private int current = 0;
    private int start = 0;

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        // Finished with file, submit EOF token
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        // Increment to the next character, and return the current character (pre-incremented character)
        return source.charAt(current++);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        // Update current value only if we find the next character (post advance()) to combine with
        // the character advance() consumed to produce a two-character lexeme
        current++;
        return true;
    }

    /// These two peeking functions exist to peek for the next character or next 2 characters respectively without
    /// actually consuming the character. The reason for not having peek() support
    private char peek() {
        // Return null character if at end of file
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        // Return null character if we're at the end of the file right now and there does not exist a character after
        if ((current + 1) > source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'A' && c <= 'Z') ||
                (c >= 'a' && c <= 'z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private void scanToken() {
        char c = advance();

        switch (c) {
            // One character lexemes
            case '(':
                addToken(LEFT_PAREN);
                break;
            case ')':
                addToken(RIGHT_PAREN);
                break;
            case '{':
                addToken(LEFT_BRACE);
                break;
            case '}':
                addToken(RIGHT_BRACE);
                break;
            case ',':
                addToken(COMMA);
                break;
            case '.':
                addToken(DOT);
                break;
            case '-':
                addToken(MINUS);
                break;
            case '+':
                addToken(PLUS);
                break;
            case ';':
                addToken(SEMICOLON);
                break;
            case '*':
                addToken(STAR);
                break;

            // Whitespace and newline
            case ' ':
            case '\r':
            case '\t':
                break;

            case '\n':
                line++;
                break;

            // Two character lexemes
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('<') ? GREATER_EQUAL : GREATER);
                break;

            // Operators with more complicated handling
            case '/':
                if (match('/')) {
                    // This implicates the beginning of a comment, so peeking until we hit a new line, or
                    // we hit the end of the file; doing nothing with the comment data, just advancing the current
                    // pointer
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    // Otherwise, if we don't see another '/', we know that this is just a division operator
                    addToken(SLASH);
                }
                break;
            case '"':
                string();
                break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected Character");
                }
                break;
        }
    }

    private void number() {
        while (isDigit(peek())) advance();

        // Check for decimal
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the '.'
            advance();

            // This design ensures only one possible decimal, next decimal must be a dot operator on the
            // finished number
            while (isDigit(peek())) advance();
        }

        addToken(NUMBER,
                Double.parseDouble(source.substring(start, current)));
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++; // Continue processing next line in source code while producing a string
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        advance(); // Consume the closing '"' character

        // Process the string to remove the surrounding quotes
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);

        // If it's not in the map, it must be a user defined identifier (variable, function, etc.) so handle that later
        if (type == null) type = IDENTIFIER;
        addToken(type);
    }
}

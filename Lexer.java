/**
 * Lexer.java
 * --------------------------------------------------------------------------
 * Phase 1 of the compiler: the Lexical Analyzer.
 *
 * Responsibility:
 *   Take a raw String of C-subset source code and convert it into a flat
 *   List<Token>. The Lexer works character by character, grouping input
 *   characters into the smallest meaningful units (lexemes) and tagging
 *   each one with a TokenType (see TokenType.java).
 *
 * Design choices:
 *   - Pure single-pass scanner: no regex engine is used (per spec, only
 *     the constructs defined in Section 2 are recognised -- this keeps
 *     the implementation transparent and educational).
 *   - Tracks a 1-based line number so that error messages later in the
 *     pipeline can be precise.
 *   - Whitespace, tabs and newlines are skipped silently EXCEPT that
 *     newlines bump the line counter (Section 3 - Phase 1).
 *   - For any unrecognised character a LexerException is thrown -- the
 *     GUI will display it instead of crashing.
 * --------------------------------------------------------------------------
 */
import java.util.ArrayList;
import java.util.List;

public class Lexer {

    /** The full source text being scanned. */
    private final String source;

    /** Current position within {@link #source} (0-based index). */
    private int pos;

    /** Current 1-based line number -- for error reporting. */
    private int line;

    /**
     * Constructs a Lexer ready to scan the given source.
     *
     * @param source the raw C-subset source code
     */
    public Lexer(String source) {
        this.source = source;
        this.pos = 0;
        this.line = 1;
    }

    /**
     * Entry point: scans the entire source string and returns every token
     * it found, terminated by a single EOF token.
     *
     * @return immutable list of tokens (including the trailing EOF)
     * @throws LexerException if an unrecognised character is encountered
     */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        // Loop until the scanner reaches end-of-input.
        while (pos < source.length()) {
            char c = source.charAt(pos);

            // ----- Skip insignificant whitespace -----
            if (c == ' ' || c == '\t' || c == '\r') {
                pos++;
                continue;
            }
            // Newline: skip the char but bump the line counter.
            if (c == '\n') {
                line++;
                pos++;
                continue;
            }

            // ----- Identifiers & Keywords -----
            // Identifiers start with a letter or underscore, then letters/digits/underscores.
            if (Character.isLetter(c) || c == '_') {
                tokens.add(readIdentifierOrKeyword());
                continue;
            }

            // ----- Numeric Literals (int or float) -----
            if (Character.isDigit(c)) {
                tokens.add(readNumber());
                continue;
            }

            // ----- Operators / Punctuation -----
            // For multi-character operators (==, !=, <=, >=, &&, ||)
            // we peek at the next char as well.
            tokens.add(readOperatorOrPunctuation());
        }
        // Always append an EOF sentinel so the parser knows when to stop.
        tokens.add(new Token(TokenType.EOF, "", line));
        return tokens;
    }

    /**
     * Reads an identifier or reserved keyword starting at the current
     * position. After collecting the full lexeme, the method checks
     * whether it matches a reserved keyword; if so it returns the
     * corresponding keyword TokenType, otherwise IDENTIFIER.
     */
    private Token readIdentifierOrKeyword() {
        int start = pos;
        // Consume first char (guaranteed letter or '_').
        pos++;
        // Consume subsequent letters, digits, or underscores.
        while (pos < source.length()
                && (Character.isLetterOrDigit(source.charAt(pos))
                    || source.charAt(pos) == '_')) {
            pos++;
        }
        String lexeme = source.substring(start, pos);

        // Map reserved words to their dedicated keyword token-types.
        switch (lexeme) {
            case "int":    return new Token(TokenType.INT,    lexeme, line);
            case "float":  return new Token(TokenType.FLOAT,  lexeme, line);
            case "void":   return new Token(TokenType.VOID,   lexeme, line);
            case "if":     return new Token(TokenType.IF,     lexeme, line);
            case "else":   return new Token(TokenType.ELSE,   lexeme, line);
            case "while":  return new Token(TokenType.WHILE,  lexeme, line);
            case "for":    return new Token(TokenType.FOR,    lexeme, line);
            case "return": return new Token(TokenType.RETURN, lexeme, line);
            // Not a keyword -> generic identifier.
            default:       return new Token(TokenType.IDENTIFIER, lexeme, line);
        }
    }

    /**
     * Reads an integer or float literal. According to the spec a float
     * is "Digits with a single decimal point" -- so we look for an
     * optional '.' followed by more digits. If we never see a '.', the
     * literal is INT_LITERAL; otherwise FLOAT_LITERAL.
     */
    private Token readNumber() {
        int start = pos;
        // Consume leading digits.
        while (pos < source.length() && Character.isDigit(source.charAt(pos))) {
            pos++;
        }
        // If a decimal point follows, consume it and any trailing digits.
        if (pos < source.length() && source.charAt(pos) == '.'
                && pos + 1 < source.length()
                && Character.isDigit(source.charAt(pos + 1))) {
            pos++; // consume the '.'
            while (pos < source.length() && Character.isDigit(source.charAt(pos))) {
                pos++;
            }
            String lexeme = source.substring(start, pos);
            return new Token(TokenType.FLOAT_LITERAL, lexeme, line);
        }
        // No decimal point -> integer literal.
        String lexeme = source.substring(start, pos);
        return new Token(TokenType.INT_LITERAL, lexeme, line);
    }

    /**
     * Reads a single- or double-character operator, or a punctuation
     * symbol. Throws LexerException if the character at the current
     * position is not a recognised symbol.
     */
    private Token readOperatorOrPunctuation() {
        char c = source.charAt(pos);
        // For 2-char operators we need to peek at the next char.
        char next = (pos + 1 < source.length()) ? source.charAt(pos + 1) : '\0';
        int startLine = line;

        switch (c) {
            // ----- 2-character operators -----
            case '=':
                if (next == '=') { pos += 2; return new Token(TokenType.EQUAL, "==", startLine); }
                pos++;            return new Token(TokenType.ASSIGN, "=", startLine);
            case '!':
                if (next == '=') { pos += 2; return new Token(TokenType.NOT_EQUAL, "!=", startLine); }
                throw new LexerException("Unexpected '!' (did you mean '!='?) at line " + line);
            case '<':
                if (next == '=') { pos += 2; return new Token(TokenType.LESS_EQUAL, "<=", startLine); }
                pos++;            return new Token(TokenType.LESS, "<", startLine);
            case '>':
                if (next == '=') { pos += 2; return new Token(TokenType.GREATER_EQUAL, ">=", startLine); }
                pos++;            return new Token(TokenType.GREATER, ">", startLine);
            case '&':
                if (next == '&') { pos += 2; return new Token(TokenType.AND, "&&", startLine); }
                throw new LexerException("Unexpected '&' (did you mean '&&'?) at line " + line);
            case '|':
                if (next == '|') { pos += 2; return new Token(TokenType.OR, "||", startLine); }
                throw new LexerException("Unexpected '|' (did you mean '||'?) at line " + line);

            // ----- 1-character arithmetic operators -----
            case '+': pos++; return new Token(TokenType.PLUS,   "+", startLine);
            case '-': pos++; return new Token(TokenType.MINUS,  "-", startLine);
            case '*': pos++; return new Token(TokenType.STAR,   "*", startLine);
            case '/': pos++; return new Token(TokenType.SLASH,  "/", startLine);

            // ----- Punctuation -----
            case '{': pos++; return new Token(TokenType.LBRACE,    "{", startLine);
            case '}': pos++; return new Token(TokenType.RBRACE,    "}", startLine);
            case '(': pos++; return new Token(TokenType.LPAREN,    "(", startLine);
            case ')': pos++; return new Token(TokenType.RPAREN,    ")", startLine);
            case ';': pos++; return new Token(TokenType.SEMICOLON, ";", startLine);
            case ',': pos++; return new Token(TokenType.COMMA,     ",", startLine);

            // Anything else is illegal in our C-subset.
            default:
                throw new LexerException(
                        "Unrecognised character '" + c + "' at line " + line);
        }
    }

    /**
     * Custom checked exception so the GUI can display lexical errors
     * distinctly from syntax errors if desired.
     */
    public static class LexerException extends RuntimeException {
        public LexerException(String message) { super(message); }
    }
}

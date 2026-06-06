/**
 * Token.java
 * --------------------------------------------------------------------------
 * A plain immutable data-class representing a single token produced by the
 * Lexer. According to the spec (Section 3 - Phase 1) every token MUST carry:
 *   - TokenType : the category (from the TokenType enum)
 *   - Lexeme    : the exact substring of source code that formed this token
 *   - LineNumber: the source-line on which the token appeared (for error
 *                 reporting)
 *
 * The class is intentionally simple; it is a "value object" that travels
 * through the rest of the compilation pipeline (Parser, Symbol Table, GUI).
 * --------------------------------------------------------------------------
 */
public class Token {

    /** The category of this token (e.g. INT, PLUS, IDENTIFIER). */
    private final TokenType type;

    /** The raw text from the source code that produced this token. */
    private final String lexeme;

    /** The 1-based line number where this token was found. */
    private final int lineNumber;

    /**
     * Constructs a new immutable Token.
     *
     * @param type       the token's category
     * @param lexeme     the source text
     * @param lineNumber the line on which it was found (1-based)
     */
    public Token(TokenType type, String lexeme, int lineNumber) {
        this.type = type;
        this.lexeme = lexeme;
        this.lineNumber = lineNumber;
    }

    /** @return the token's category. */
    public TokenType getType() {
        return type;
    }

    /** @return the raw source text. */
    public String getLexeme() {
        return lexeme;
    }

    /** @return the 1-based source line number. */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Human-readable string form, useful for the GUI's token table and
     * for debugging via println().
     */
    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", lexeme='" + lexeme + '\'' +
                ", line=" + lineNumber +
                '}';
    }
}

/**
 * TokenType.java
 * --------------------------------------------------------------------------
 * Enumerates every possible category of token that the Lexical Analyzer
 * (Lexer) can emit for the C-subset language defined in the project
 * specification (see instructions.md, Section 2.A).
 *
 * Having a strongly-typed enum prevents "magic string" bugs (e.g. someone
 * passing the literal "INT" somewhere instead of the proper constant) and
 * makes the Parser's switch/if-chains self-documenting.
 *
 * NOTE: This enum is intentionally minimal. We do NOT add tokens for
 * features the language does not support (e.g. no CHAR, no DOUBLE, no
 * SWITCH, no POINTERS, no #include) -- per the "NO SCOPE CREEP" rule.
 * --------------------------------------------------------------------------
 */
public enum TokenType {
    // ---------- Data Types ----------
    INT,            // the keyword "int"
    FLOAT,          // the keyword "float"
    VOID,           // the keyword "void"

    // ---------- Control-Flow Keywords ----------
    IF,             // the keyword "if"
    ELSE,           // the keyword "else"
    WHILE,          // the keyword "while"
    FOR,            // the keyword "for"
    RETURN,         // the keyword "return"

    // ---------- Literals ----------
    INT_LITERAL,    // e.g. 42
    FLOAT_LITERAL,  // e.g. 3.14

    // ---------- Identifier (variable/function names) ----------
    IDENTIFIER,     // matches regex: [a-zA-Z_][a-zA-Z0-9_]*

    // ---------- Arithmetic Operators ----------
    PLUS,           // +
    MINUS,          // -
    STAR,           // *
    SLASH,          // /

    // ---------- Relational Operators ----------
    EQUAL,          // ==
    NOT_EQUAL,      // !=
    LESS,           // <
    GREATER,        // >
    LESS_EQUAL,     // <=
    GREATER_EQUAL,  // >=

    // ---------- Logical Operators ----------
    AND,            // &&
    OR,             // ||

    // ---------- Assignment Operator ----------
    ASSIGN,         // =

    // ---------- Punctuation / Delimiters ----------
    LBRACE,         // {
    RBRACE,         // }
    LPAREN,         // (
    RPAREN,         // )
    SEMICOLON,      // ;
    COMMA,          // ,

    // ---------- Sentinel ----------
    EOF             // End-Of-File marker emitted by the Lexer
}

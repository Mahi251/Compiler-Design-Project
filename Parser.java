/**
 * Parser.java
 * --------------------------------------------------------------------------
 * Phase 2 of the compiler: the Syntax Analyzer.
 *
 * Uses the classic RECURSIVE-DESCENT approach: one Java method per
 * grammar rule. Each rule-method consumes the tokens it expects from
 * the input stream and, on success, returns a ParseTreeNode that
 * represents the subtree for the construct just parsed.
 *
 * Grammar (informal, only the C-subset from the spec is supported):
 *
 *   program        -> (functionDefinition | declaration | statement)*
 *   functionDef    -> type IDENTIFIER '(' parameters? ')' block
 *   parameters     -> parameter (',' parameter)*
 *   parameter      -> type IDENTIFIER
 *   block          -> '{' statement* '}'
 *   statement      -> declaration
 *                  |  assignment
 *                  |  functionCallStmt
 *                  |  returnStmt
 *                  |  ifStmt
 *                  |  whileStmt
 *                  |  forStmt
 *                  |  block
 *   declaration    -> type IDENTIFIER ('=' expression)? ';'
 *   assignment     -> IDENTIFIER '=' expression ';'
 *   functionCallStmt-> IDENTIFIER '(' arguments? ')' ';'
 *   returnStmt     -> RETURN expression? ';'
 *   ifStmt         -> IF '(' condition ')' block (ELSE block)?
 *   whileStmt      -> WHILE '(' condition ')' block
 *   forStmt        -> FOR '(' (declaration|assignment|';')
 *                              condition? ';'
 *                              expression? ')' block
 *   condition      -> expression
 *   arguments      -> expression (',' expression)*
 *   expression     -> logicalOr        (lowest precedence)
 *   logicalOr      -> logicalAnd ('||' logicalAnd)*
 *   logicalAnd     -> equality   ('&&' equality)*
 *   equality       -> relational (('=='|'!=') relational)*
 *   relational     -> additive   (('<'|'>'|'<='|'>=') additive)*
 *   additive       -> multiplicative (('+'|'-') multiplicative)*
 *   multiplicative -> primary (('*'|'/') primary)*
 *   primary        -> INT_LITERAL | FLOAT_LITERAL
 *                  |  IDENTIFIER ('(' arguments? ')')?
 *                  |  '(' expression ')'
 * --------------------------------------------------------------------------
 */
import java.util.List;

public class Parser {

    /** The flat token list coming from the Lexer. */
    private final List<Token> tokens;

    /** Index of the "current" token the parser is looking at. */
    private int current;

    /** Symbol table that gets populated as we parse. */
    private final SymbolTable symbolTable;

    /** The root of the parse tree (a "Program" node). */
    private ParseTreeNode root;

    /**
     * Constructs a Parser over the given token list.
     *
     * @param tokens token list from the Lexer (must end with EOF)
     */
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.current = 0;
        this.symbolTable = new SymbolTable();
    }

    /** @return the symbol table populated during parsing. */
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    /** @return the root of the parse tree. */
    public ParseTreeNode getParseTree() {
        return root;
    }

    // ===================================================================
    //  ENTRY POINT
    // ===================================================================

    /**
     * Parses an entire program.
     *
     * @return the root ParseTreeNode ("Program")
     * @throws SyntaxException if the source does not match the grammar
     */
    public ParseTreeNode parseProgram() {
        root = new ParseTreeNode("Program");
        // A program is a sequence of top-level items: function
        // definitions, declarations, or plain statements. We stop when
        // we hit EOF.
        while (!isAtEnd()) {
            // Peek to decide which top-level rule to invoke.
            if (isFunctionDefinitionStart()) {
                root.addChild(parseFunctionDefinition());
            } else {
                root.addChild(parseStatement());
            }
        }
        return root;
    }

    // ===================================================================
    //  HELPER / LOOK-AHEAD METHODS
    // ===================================================================

    /** @return true if the current token is the EOF sentinel. */
    private boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }

    /** @return the current token without consuming it. */
    private Token peek() {
        return tokens.get(current);
    }

    /** @return the previously consumed token (useful for error messages). */
    private Token previous() {
        return tokens.get(current - 1);
    }

    /**
     * Consumes the current token if its type matches {@code type},
     * otherwise throws a SyntaxException. Returns the consumed token
     * on success.
     */
    private Token consume(TokenType type, String errorMessage) {
        if (check(type)) {
            return advance();
        }
        throw new SyntaxException(
                "Syntax Error at line " + peek().getLineNumber() + ": "
                        + errorMessage + " (got '" + peek().getLexeme() + "')");
    }

    /** @return true if the current token is of the given type. */
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().getType() == type;
    }

    /**
     * Consumes the current token unconditionally and returns it,
     * advancing the cursor by one position.
     */
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    /**
     * Detects the start of a function definition WITHOUT consuming the
     * lookahead token. A function definition looks like:
     *   <type> IDENTIFIER '(' ...
     * If we see that pattern, the next production is a function
     * definition; otherwise it must be a declaration or statement.
     */
    private boolean isFunctionDefinitionStart() {
        // Need at least 3 tokens ahead to even consider: type id (
        if (current + 2 >= tokens.size()) return false;
        TokenType t0 = tokens.get(current).getType();
        TokenType t1 = tokens.get(current + 1).getType();
        TokenType t2 = tokens.get(current + 2).getType();
        boolean isType = (t0 == TokenType.INT || t0 == TokenType.FLOAT || t0 == TokenType.VOID);
        return isType && t1 == TokenType.IDENTIFIER && t2 == TokenType.LPAREN;
    }

    // ===================================================================
    //  GRAMMAR RULE METHODS
    // ===================================================================

    /**
     * Parses a function definition:
     *   type IDENTIFIER ( parameters? ) block
     * Also adds a FUNCTION entry to the symbol table.
     */
    private ParseTreeNode parseFunctionDefinition() {
        ParseTreeNode node = new ParseTreeNode("FunctionDefinition");

        // ----- Return type -----
        Token returnType = advance(); // INT | FLOAT | VOID
        node.addChild(new ParseTreeNode("ReturnType: " + returnType.getLexeme()));

        // ----- Function name -----
        Token name = consume(TokenType.IDENTIFIER, "Expected function name");
        node.addChild(new ParseTreeNode("FunctionName: " + name.getLexeme()));

        // Record the function in the symbol table.
        symbolTable.declare(name.getLexeme(), returnType.getLexeme(),
                            Symbol.SymbolType.FUNCTION);

        // ----- Parameter list -----
        consume(TokenType.LPAREN, "Expected '(' after function name");
        ParseTreeNode params = new ParseTreeNode("Parameters");
        if (!check(TokenType.RPAREN)) {
            // First (required) parameter.
            params.addChild(parseParameter());
            // Zero or more additional parameters separated by commas.
            while (check(TokenType.COMMA)) {
                advance(); // consume ','
                params.addChild(parseParameter());
            }
        }
        node.addChild(params);
        consume(TokenType.RPAREN, "Expected ')' after parameters");

        // ----- Function body (block) -----
        node.addChild(parseBlock());
        return node;
    }

    /**
     * Parses a single parameter: type IDENTIFIER
     * Also declares the parameter as a VARIABLE in the symbol table.
     */
    private ParseTreeNode parseParameter() {
        ParseTreeNode node = new ParseTreeNode("Parameter");
        Token type = advance(); // INT or FLOAT
        node.addChild(new ParseTreeNode("Type: " + type.getLexeme()));
        Token id = consume(TokenType.IDENTIFIER, "Expected parameter name");
        node.addChild(new ParseTreeNode("Name: " + id.getLexeme()));
        // Add parameter to the symbol table as a variable.
        symbolTable.declare(id.getLexeme(), type.getLexeme(),
                            Symbol.SymbolType.VARIABLE);
        return node;
    }

    /**
     * Parses a block: '{' statement* '}'
     * The block is a "scope" in C, but for simplicity we keep a single
     * flat symbol table (per the spec's "no scope creep" rule).
     */
    private ParseTreeNode parseBlock() {
        ParseTreeNode node = new ParseTreeNode("Block");
        consume(TokenType.LBRACE, "Expected '{' to start block");
        // Zero or more statements until the matching '}'.
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            node.addChild(parseStatement());
        }
        consume(TokenType.RBRACE, "Expected '}' to close block");
        return node;
    }

    /**
     * Parses a single statement. This is a "dispatcher" -- it peeks at
     * the first token to decide which specific rule applies.
     */
    private ParseTreeNode parseStatement() {
        Token first = peek();

        // Declarations start with a type keyword.
        if (first.getType() == TokenType.INT
                || first.getType() == TokenType.FLOAT
                || first.getType() == TokenType.VOID) {
            return parseDeclaration();
        }
        // Return statement.
        if (first.getType() == TokenType.RETURN) {
            return parseReturnStatement();
        }
        // Control flow.
        if (first.getType() == TokenType.IF)    return parseIfStatement();
        if (first.getType() == TokenType.WHILE) return parseWhileStatement();
        if (first.getType() == TokenType.FOR)   return parseForStatement();
        // A '{' opens a nested block.
        if (first.getType() == TokenType.LBRACE) {
            return parseBlock();
        }
        // Anything else must start with an identifier: it could be
        // either an assignment or a function call. We disambiguate by
        // looking further ahead for the '(' or '='.
        if (first.getType() == TokenType.IDENTIFIER) {
            return parseAssignmentOrCall();
        }

        throw new SyntaxException(
                "Syntax Error at line " + first.getLineNumber()
                        + ": Unexpected token '" + first.getLexeme()
                        + "' at start of statement");
    }

    /**
     * Parses a declaration: type IDENTIFIER ( '=' expression )? ';'
     * Also inserts a VARIABLE entry into the symbol table.
     */
    private ParseTreeNode parseDeclaration() {
        ParseTreeNode node = new ParseTreeNode("Declaration");

        // ----- Type -----
        Token type = advance(); // INT | FLOAT | VOID
        node.addChild(new ParseTreeNode("Type: " + type.getLexeme()));

        // ----- Identifier -----
        Token id = consume(TokenType.IDENTIFIER, "Expected variable name");
        node.addChild(new ParseTreeNode("Name: " + id.getLexeme()));

        // Register the variable in the symbol table.
        symbolTable.declare(id.getLexeme(), type.getLexeme(),
                            Symbol.SymbolType.VARIABLE);

        // ----- Optional initializer -----
        if (check(TokenType.ASSIGN)) {
            advance(); // consume '='
            node.addChild(new ParseTreeNode("="));
            node.addChild(parseExpression());
        }
        consume(TokenType.SEMICOLON, "Expected ';' after declaration");
        return node;
    }

    /**
     * Decides between an assignment (IDENTIFIER '=' ...) and a function
     * call statement (IDENTIFIER '(' ... ')' ';'). The parser looks
     * ahead two tokens to tell them apart.
     */
    private ParseTreeNode parseAssignmentOrCall() {
        // Look at the token AFTER the identifier.
        if (current + 1 >= tokens.size()) {
            throw new SyntaxException(
                    "Syntax Error at line " + peek().getLineNumber()
                            + ": Unexpected end of input after identifier");
        }
        TokenType after = tokens.get(current + 1).getType();

        if (after == TokenType.ASSIGN) {
            return parseAssignment();
        }
        if (after == TokenType.LPAREN) {
            return parseFunctionCallStatement();
        }
        throw new SyntaxException(
                "Syntax Error at line " + peek().getLineNumber()
                        + ": Expected '=' or '(' after identifier '"
                        + peek().getLexeme() + "'");
    }

    /**
     * Parses an assignment: IDENTIFIER '=' expression ';'
     */
    private ParseTreeNode parseAssignment() {
        ParseTreeNode node = new ParseTreeNode("Assignment");
        Token id = advance(); // IDENTIFIER
        node.addChild(new ParseTreeNode("Target: " + id.getLexeme()));
        consume(TokenType.ASSIGN, "Expected '=' in assignment");
        node.addChild(new ParseTreeNode("="));
        node.addChild(parseExpression());
        consume(TokenType.SEMICOLON, "Expected ';' after assignment");
        return node;
    }

    /**
     * Parses a function-call statement: IDENTIFIER '(' args? ')' ';'
     */
    private ParseTreeNode parseFunctionCallStatement() {
        ParseTreeNode node = new ParseTreeNode("FunctionCallStmt");
        Token name = advance(); // IDENTIFIER
        node.addChild(new ParseTreeNode("Call: " + name.getLexeme()));
        consume(TokenType.LPAREN, "Expected '(' in function call");
        ParseTreeNode args = new ParseTreeNode("Arguments");
        if (!check(TokenType.RPAREN)) {
            args.addChild(parseExpression());
            while (check(TokenType.COMMA)) {
                advance(); // consume ','
                args.addChild(parseExpression());
            }
        }
        node.addChild(args);
        consume(TokenType.RPAREN, "Expected ')' to close function call");
        consume(TokenType.SEMICOLON, "Expected ';' after function call");
        return node;
    }

    /**
     * Parses a return statement: RETURN expression? ';'
     */
    private ParseTreeNode parseReturnStatement() {
        ParseTreeNode node = new ParseTreeNode("ReturnStatement");
        advance(); // consume 'return'
        // Optional return value.
        if (!check(TokenType.SEMICOLON)) {
            node.addChild(parseExpression());
        }
        consume(TokenType.SEMICOLON, "Expected ';' after return statement");
        return node;
    }

    /**
     * Parses an if-statement:
     *   IF '(' condition ')' block ( ELSE block )?
     */
    private ParseTreeNode parseIfStatement() {
        ParseTreeNode node = new ParseTreeNode("IfStatement");
        advance(); // consume 'if'
        consume(TokenType.LPAREN, "Expected '(' after 'if'");
        node.addChild(new ParseTreeNode("Condition"));
        node.addChild(parseExpression());
        consume(TokenType.RPAREN, "Expected ')' after if condition");
        node.addChild(parseBlock());
        // Optional else branch.
        if (check(TokenType.ELSE)) {
            advance(); // consume 'else'
            node.addChild(new ParseTreeNode("Else"));
            node.addChild(parseBlock());
        }
        return node;
    }

    /**
     * Parses a while-statement: WHILE '(' condition ')' block
     */
    private ParseTreeNode parseWhileStatement() {
        ParseTreeNode node = new ParseTreeNode("WhileStatement");
        advance(); // consume 'while'
        consume(TokenType.LPAREN, "Expected '(' after 'while'");
        node.addChild(new ParseTreeNode("Condition"));
        node.addChild(parseExpression());
        consume(TokenType.RPAREN, "Expected ')' after while condition");
        node.addChild(parseBlock());
        return node;
    }

    /**
     * Parses a for-statement:
     *   FOR '(' (declaration|assignment|';')  condition? ';'  update? ')' block
     */
    private ParseTreeNode parseForStatement() {
        ParseTreeNode node = new ParseTreeNode("ForStatement");
        advance(); // consume 'for'
        consume(TokenType.LPAREN, "Expected '(' after 'for'");

        // ---- Init part: declaration, assignment, or empty ----
        node.addChild(new ParseTreeNode("Init"));
        if (check(TokenType.SEMICOLON)) {
            advance(); // empty init
        } else if (check(TokenType.INT) || check(TokenType.FLOAT) || check(TokenType.VOID)) {
            node.addChild(parseDeclaration());
        } else if (check(TokenType.IDENTIFIER)) {
            // Either an assignment (id = expr) or a function call (id(...);)
            if (current + 1 < tokens.size()
                    && tokens.get(current + 1).getType() == TokenType.ASSIGN) {
                node.addChild(parseAssignment());
            } else {
                node.addChild(parseFunctionCallStatement());
            }
        } else {
            throw new SyntaxException(
                    "Syntax Error at line " + peek().getLineNumber()
                            + ": Invalid for-loop initializer");
        }

        // ---- Condition part: optional expression ending in ';' ----
        node.addChild(new ParseTreeNode("Condition"));
        if (!check(TokenType.SEMICOLON)) {
            node.addChild(parseExpression());
        }
        consume(TokenType.SEMICOLON, "Expected ';' after for-loop condition");

        // ---- Update part: optional expression OR assignment ending in ')' ----
        // Per C convention the update is typically an assignment
        // (e.g. "i = i + 1") or a function call, but we also allow a
        // bare expression for flexibility.
        node.addChild(new ParseTreeNode("Update"));
        if (!check(TokenType.RPAREN)) {
            if (check(TokenType.IDENTIFIER) && current + 1 < tokens.size()
                    && tokens.get(current + 1).getType() == TokenType.ASSIGN) {
                // It's an assignment (without the trailing ';').
                ParseTreeNode assign = new ParseTreeNode("Assignment");
                Token id = advance();
                assign.addChild(new ParseTreeNode("Target: " + id.getLexeme()));
                advance(); // consume '='
                assign.addChild(new ParseTreeNode("="));
                assign.addChild(parseExpression());
                node.addChild(assign);
            } else {
                node.addChild(parseExpression());
            }
        }
        consume(TokenType.RPAREN, "Expected ')' to close for-loop header");

        // ---- Body ----
        node.addChild(parseBlock());
        return node;
    }

    // -------------------------------------------------------------------
    //  EXPRESSION GRAMMAR (precedence-climbed, lowest to highest)
    //   expression     -> logicalOr
    //   logicalOr      -> logicalAnd ('||' logicalAnd)*
    //   logicalAnd     -> equality   ('&&' equality)*
    //   equality       -> relational (('=='|'!=') relational)*
    //   relational     -> additive   (('<'|'>'|'<='|'>=') additive)*
    //   additive       -> multiplicative (('+'|'-') multiplicative)*
    //   multiplicative -> primary (('*'|'/') primary)*
    //   primary        -> INT_LITERAL | FLOAT_LITERAL
    //                  |  IDENTIFIER ('(' arguments? ')')?
    //                  |  '(' expression ')'
    // -------------------------------------------------------------------

    /**
     * Top-level "expression" entry point used by all callers.
     * Implemented as an alias for the LOWEST-precedence rule (logicalOr)
     * so that any operator in the language can appear inside a condition.
     */
    private ParseTreeNode parseExpression() {
        return parseLogicalOr();
    }

    /** Logical OR: left-associative '||'. */
    private ParseTreeNode parseLogicalOr() {
        ParseTreeNode node = parseLogicalAnd();
        while (check(TokenType.OR)) {
            Token op = advance();
            node.addChild(new ParseTreeNode("Operator: " + op.getLexeme()));
            node.addChild(parseLogicalAnd());
        }
        return node;
    }

    /** Logical AND: left-associative '&&'. */
    private ParseTreeNode parseLogicalAnd() {
        ParseTreeNode node = parseEquality();
        while (check(TokenType.AND)) {
            Token op = advance();
            node.addChild(new ParseTreeNode("Operator: " + op.getLexeme()));
            node.addChild(parseEquality());
        }
        return node;
    }

    /** Equality: left-associative '==' and '!='. */
    private ParseTreeNode parseEquality() {
        ParseTreeNode node = parseRelational();
        while (check(TokenType.EQUAL) || check(TokenType.NOT_EQUAL)) {
            Token op = advance();
            node.addChild(new ParseTreeNode("Operator: " + op.getLexeme()));
            node.addChild(parseRelational());
        }
        return node;
    }

    /** Relational: left-associative '<', '>', '<=', '>='. */
    private ParseTreeNode parseRelational() {
        ParseTreeNode node = parseAdditive();
        while (check(TokenType.LESS) || check(TokenType.GREATER)
                || check(TokenType.LESS_EQUAL) || check(TokenType.GREATER_EQUAL)) {
            Token op = advance();
            node.addChild(new ParseTreeNode("Operator: " + op.getLexeme()));
            node.addChild(parseAdditive());
        }
        return node;
    }

    /** Additive: left-associative '+' and '-'. */
    private ParseTreeNode parseAdditive() {
        ParseTreeNode node = parseMultiplicative();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            Token op = advance();
            node.addChild(new ParseTreeNode("Operator: " + op.getLexeme()));
            node.addChild(parseMultiplicative());
        }
        return node;
    }

    /** Multiplicative: left-associative '*' and '/'. */
    private ParseTreeNode parseMultiplicative() {
        ParseTreeNode node = parsePrimary();
        while (check(TokenType.STAR) || check(TokenType.SLASH)) {
            Token op = advance();
            node.addChild(new ParseTreeNode("Operator: " + op.getLexeme()));
            node.addChild(parsePrimary());
        }
        return node;
    }

    /**
     * Highest precedence: a primary value.
     *   - integer literal
     *   - float literal
     *   - identifier, possibly followed by '( args )' for a function call
     *   - parenthesised sub-expression
     */
    private ParseTreeNode parsePrimary() {
        Token t = peek();

        if (check(TokenType.INT_LITERAL)) {
            advance();
            return new ParseTreeNode("Literal: " + t.getLexeme());
        }
        if (check(TokenType.FLOAT_LITERAL)) {
            advance();
            return new ParseTreeNode("Literal: " + t.getLexeme());
        }
        if (check(TokenType.IDENTIFIER)) {
            advance();
            // If the identifier is followed by '(', this is a function call.
            if (check(TokenType.LPAREN)) {
                ParseTreeNode call = new ParseTreeNode("FunctionCall: " + t.getLexeme());
                advance(); // consume '('
                if (!check(TokenType.RPAREN)) {
                    call.addChild(parseExpression());
                    while (check(TokenType.COMMA)) {
                        advance();
                        call.addChild(parseExpression());
                    }
                }
                consume(TokenType.RPAREN, "Expected ')' to close function call");
                return call;
            }
            // Plain identifier reference.
            return new ParseTreeNode("Identifier: " + t.getLexeme());
        }
        if (check(TokenType.LPAREN)) {
            advance(); // consume '('
            ParseTreeNode inner = parseExpression();
            consume(TokenType.RPAREN, "Expected ')' to close parenthesised expression");
            return inner;
        }
        // Nothing matched -- bad input.
        throw new SyntaxException(
                "Syntax Error at line " + t.getLineNumber()
                        + ": Expected expression, got '" + t.getLexeme() + "'");
    }

    // ===================================================================
    //  CUSTOM EXCEPTION
    // ===================================================================

    /**
     * Thrown whenever the source code fails to match the expected
     * grammar. Carries enough information for the GUI to display a
     * useful diagnostic.
     */
    public static class SyntaxException extends RuntimeException {
        public SyntaxException(String message) { super(message); }
    }
}

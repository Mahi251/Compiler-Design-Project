/**
 * Symbol.java
 * --------------------------------------------------------------------------
 * Represents a single entry in the Symbol Table (see instructions.md,
 * Phase 3). A Symbol stores the metadata the compiler knows about a
 * variable or function:
 *
 *   - Name     : the identifier (lexeme) as it appears in the source
 *   - DataType : one of {int, float, void} -- the declared type
 *   - Type     : VARIABLE or FUNCTION -- distinguishes the kind of
 *                declaration so later phases can act appropriately
 *                (e.g. type-checking arguments vs. assignments).
 *
 * It is a simple POJO/record-style class; the symbol table itself is
 * responsible for storage and lookup.
 * --------------------------------------------------------------------------
 */
public class Symbol {

    /** Allowed symbol kinds. */
    public enum SymbolType {
        VARIABLE,
        FUNCTION
    }

    /** The identifier as written in source code (e.g. "myVar"). */
    private final String name;

    /**
     * The declared data-type. Stored as a String (rather than TokenType)
     * to keep this class decoupled from the lexer; legal values are
     * "int", "float" or "void".
     */
    private final String dataType;

    /** Whether this symbol is a variable or a function. */
    private final SymbolType type;

    /**
     * Constructs a new Symbol entry.
     *
     * @param name     identifier name
     * @param dataType declared data type ("int", "float", or "void")
     * @param type     VARIABLE or FUNCTION
     */
    public Symbol(String name, String dataType, SymbolType type) {
        this.name = name;
        this.dataType = dataType;
        this.type = type;
    }

    public String getName()     { return name; }
    public String getDataType() { return dataType; }
    public SymbolType getType() { return type; }

    /** Useful for the GUI's symbol-table view. */
    @Override
    public String toString() {
        return name + " : " + dataType + " (" + type + ")";
    }
}

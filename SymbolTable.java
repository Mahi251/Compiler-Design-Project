/**
 * SymbolTable.java
 * --------------------------------------------------------------------------
 * Phase 3 of the compiler.
 *
 * Stores identifier metadata (name, data type, kind) for every
 * variable and function encountered during parsing. Backed by a
 * HashMap<String, Symbol> as required by the spec.
 *
 * The Parser will:
 *   - call {@link #declare(String, String, Symbol.SymbolType)} when it
 *     successfully matches a declaration or a function definition.
 *   - call {@link #lookup(String)} later (e.g. while type-checking
 *     assignments or function-call arguments).
 *
 * The GUI reads the table via {@link #getAll()} to populate its
 * "Symbol Table" JTable.
 * --------------------------------------------------------------------------
 */
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {

    /**
     * The underlying storage: identifier name -> Symbol entry.
     * HashMap gives O(1) average insert and lookup.
     */
    private final Map<String, Symbol> table;

    /** Creates an empty symbol table. */
    public SymbolTable() {
        this.table = new HashMap<>();
    }

    /**
     * Adds a new symbol to the table. If a symbol with the same name
     * already exists, the new entry overwrites the old one -- this is
     * a deliberate simplification for an educational compiler. A
     * production compiler would flag the redeclaration as an error.
     *
     * @param name     identifier lexeme
     * @param dataType "int", "float" or "void"
     * @param type     VARIABLE or FUNCTION
     */
    public void declare(String name, String dataType, Symbol.SymbolType type) {
        table.put(name, new Symbol(name, dataType, type));
    }

    /**
     * Looks up an identifier in the table.
     *
     * @param name identifier lexeme
     * @return the matching Symbol, or {@code null} if not found
     */
    public Symbol lookup(String name) {
        return table.get(name);
    }

    /**
     * Returns every symbol currently stored. The order is not
     * guaranteed (HashMap iteration); the GUI just needs a snapshot
     * to fill its table.
     *
     * @return collection of all stored symbols
     */
    public Collection<Symbol> getAll() {
        return table.values();
    }

    /**
     * Convenience helper for the GUI / debugging -- returns a sorted
     * list (by identifier name) so the display is stable.
     *
     * @return sorted list of symbols
     */
    public List<Symbol> getAllSorted() {
        List<Symbol> list = new ArrayList<>(table.values());
        list.sort((a, b) -> a.getName().compareTo(b.getName()));
        return list;
    }

    /** @return the number of symbols currently in the table. */
    public int size() {
        return table.size();
    }
}

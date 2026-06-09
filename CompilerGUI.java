/**
 * CompilerGUI.java
 * --------------------------------------------------------------------------
 * Phase 5 (final phase) of the compiler project: the Swing GUI.
 *
 * Wires the three lower-level components together:
 *   - Lexer     -> fills the "Tokens" JTable
 *   - Parser    -> fills the "Parse Tree" JTree and the "Symbol Table"
 *                  JTable
 *
 * The window is laid out as follows (top to bottom):
 *
 *   +---------------------------------------------------+
 *   |   Source Code (JTextArea)            [Compile]    |  <- north
 *   +---------------------------------------------------+
 *   |  Tokens   |  Symbol Table                         |  <- center
 *   |  (JTable) |  (JTable)                              |
 *   +---------------------------------------------------+
 *   |  Parse Tree (JTree)                               |  <- center (lower)
 *   +---------------------------------------------------+
 *   |  Console / Error Output (JTextArea)               |  <- south
 *   +---------------------------------------------------+
 *
 * Swing is used exclusively (no JavaFX, no external libs) so the
 * project compiles with a stock JDK and can be launched with:
 *   java CompilerGUI
 * --------------------------------------------------------------------------
 */
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.List;

public class CompilerGUI extends JFrame {

    // ===============================================================
    //  GUI WIDGET FIELDS
    // ===============================================================

    /** Multi-line input box for the user's C-subset source code. */
    private final JTextArea codeArea;

    /** Button that triggers the Lexer + Parser pipeline. */
    private final JButton compileButton;

    /** Table model backing the "Tokens" JTable. */
    private final DefaultTableModel tokenTableModel;

    /** Table model backing the "Symbol Table" JTable. */
    private final DefaultTableModel symbolTableModel;

    /** Read-only area that prints status and error messages. */
    private final JTextArea consoleArea;

    /**
     * The JTree used to display the parse tree. Held as a field so the
     * action-listener can swap its model on every compile (see update.txt).
     */
    private final JTree parseTree;

    /** Tree model backing the parse-tree JTree; rebuilt on every compile. */
    private DefaultTreeModel parseTreeModel;

    // ===============================================================
    //  CONSTRUCTOR
    // ===============================================================

    /**
     * Builds the entire UI: menu-less JFrame with nested split panes
     * and tabs.
     */
    public CompilerGUI() {
        // ---- Window basics ----
        setTitle("C-Subset Compiler  -  Lexical & Syntax Analyzer");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // centre on screen
        setLayout(new BorderLayout(5, 5));

        // ===========================================================
        //  NORTH: source-code input + Compile button
        // ===========================================================
        JPanel northPanel = new JPanel(new BorderLayout(5, 5));
        northPanel.setBorder(BorderFactory.createTitledBorder("Source Code (C-subset)"));

        codeArea = new JTextArea(8, 60);
        codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        // A tiny demo program so the user can click "Compile" right away.
        codeArea.setText(
                "int add(int a, int b) {\n" +
                "    return a + b;\n" +
                "}\n" +
                "\n" +
                "int main() {\n" +
                "    int x;\n" +
                "    float y = 3.14;\n" +
                "    x = 10;\n" +
                "    if (x > 0) {\n" +
                "        y = y + 1.0;\n" +
                "    } else {\n" +
                "        y = 0.0;\n" +
                "    }\n" +
                "    while (x > 0) {\n" +
                "        x = x - 1;\n" +
                "    }\n" +
                "    for (int i = 0; i < 5; i = i + 1) {\n" +
                "        add(i, 2);\n" +
                "    }\n" +
                "    return x;\n" +
                "}\n");
        northPanel.add(new JScrollPane(codeArea), BorderLayout.CENTER);

        compileButton = new JButton("Compile / Analyze");
        compileButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        compileButton.addActionListener(e -> runAnalysis());
        northPanel.add(compileButton, BorderLayout.EAST);

        add(northPanel, BorderLayout.NORTH);

        // ===========================================================
        //  CENTER: tabs for Tokens, Symbol Table, Parse Tree
        // ===========================================================
        JTabbedPane centerTabs = new JTabbedPane();

        // --- Tokens tab: JTable with columns [Line, Type, Lexeme] ---
        tokenTableModel = new DefaultTableModel(
                new Object[]{"Line", "Token Type", "Lexeme"}, 0) {
            // Make cells non-editable so the user cannot edit the output.
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tokenTable = new JTable(tokenTableModel);
        tokenTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        tokenTable.setRowHeight(18);
        centerTabs.addTab("Tokens", new JScrollPane(tokenTable));

        // --- Symbol Table tab: JTable [Name, DataType, SymbolType] ---
        symbolTableModel = new DefaultTableModel(
                new Object[]{"Identifier Name", "Data Type", "Symbol Type"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable symbolTable = new JTable(symbolTableModel);
        symbolTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        symbolTable.setRowHeight(18);
        centerTabs.addTab("Symbol Table", new JScrollPane(symbolTable));

        // --- Parse Tree tab: JTree inside a scroll pane ---
        // Start with a single "No Tree Generated" root so the panel is
        // never blank. A proper DefaultTreeModel is used (rather than the
        // implicit model that JTree builds from a node) because we need
        // to call model.reload() to refresh the view after every compile.
        DefaultMutableTreeNode initialRoot = new DefaultMutableTreeNode("No Tree Generated");
        parseTreeModel = new DefaultTreeModel(initialRoot);
        parseTree = new JTree(parseTreeModel);
        parseTree.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        centerTabs.addTab("Parse Tree", new JScrollPane(parseTree));

        add(centerTabs, BorderLayout.CENTER);

        // ===========================================================
        //  SOUTH: console / error output
        // ===========================================================
        consoleArea = new JTextArea(6, 60);
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        consoleArea.setBackground(new Color(245, 245, 245));
        consoleArea.setText("Ready. Click 'Compile / Analyze' to begin.\n");
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setBorder(BorderFactory.createTitledBorder("Console / Output"));
        southPanel.add(new JScrollPane(consoleArea), BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
    }

    // ===============================================================
    //  ANALYSIS PIPELINE  (called when the button is clicked)
    // ===============================================================

    /**
     * Orchestrates the three phases:
     *   1. Read the source from the JTextArea.
     *   2. Run the Lexer -> populate the Tokens JTable.
     *   3. Run the Parser -> populate the Symbol Table and Parse Tree.
     *   4. Report success or detailed error info in the console area.
     *
     * Any thrown exception is caught here so the GUI never dies.
     */
    private void runAnalysis() {
        // ============================================================
        //  UI RESET (per update.txt) -- runs BEFORE the Lexer/Parser
        //  are invoked, so stale data from a prior run is never mixed
        //  with the new run's output.
        // ============================================================

        // 1. Clear the console / error-output area.
        consoleArea.setText("");

        // 2. Clear the Token JTable's underlying model (0 rows).
        tokenTableModel.setRowCount(0);

        // 3. Clear the Symbol Table JTable's underlying model (0 rows).
        symbolTableModel.setRowCount(0);

        // 4. Reset the parse-tree JTree to a single "No Tree Generated"
        //    root. We rebuild the DefaultTreeModel rather than just
        //    mutating the existing node, so the JTree is guaranteed to
        //    repaint (DefaultTreeModel.reload() is called).
        DefaultMutableTreeNode emptyRoot = new DefaultMutableTreeNode("No Tree Generated");
        parseTreeModel = new DefaultTreeModel(emptyRoot);
        parseTree.setModel(parseTreeModel);

        String source = codeArea.getText();
        if (source == null || source.trim().isEmpty()) {
            consoleArea.setText("Error: Source code is empty.\n");
            return;
        }

        try {
            // ---- Phase 1: Lexical Analysis ----
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            // Fill the Tokens table.
            for (Token t : tokens) {
                // Hide the trailing EOF row for a cleaner display,
                // but still keep it in the list for the parser.
                if (t.getType() == TokenType.EOF) continue;
                tokenTableModel.addRow(new Object[]{
                        t.getLineNumber(),
                        t.getType(),
                        t.getLexeme()
                });
            }
            consoleArea.append("Lexical analysis complete: " + tokens.size()
                    + " tokens generated.\n");

            // ---- Phase 2: Syntax Analysis ----
            Parser parser = new Parser(tokens);
            ParseTreeNode root = parser.parseProgram();

            // Rebuild the parse-tree JTree from the new root.
            DefaultMutableTreeNode newRoot = new DefaultMutableTreeNode(root.getLabel());
            for (ParseTreeNode child : root.getChildren()) {
                newRoot.add(child.toTreeNode());
            }
            parseTreeModel = new DefaultTreeModel(newRoot);
            parseTree.setModel(parseTreeModel);

            // ---- Phase 4: Semantic Analysis ----
            // This runs immediately after a successful parse so semantic
            // errors stop the pipeline before the GUI reports success.
            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
            semanticAnalyzer.analyze(root, parser.getSymbolTable());

            // Refresh the Symbol Table JTable.
            for (Symbol s : parser.getSymbolTable().getAllSorted()) {
                symbolTableModel.addRow(new Object[]{
                        s.getName(),
                        s.getDataType(),
                        s.getType()
                });
            }

            consoleArea.append("Syntax analysis complete: "
                    + parser.getSymbolTable().size() + " symbols stored.\n");
            consoleArea.append("Build Successful.\n");

        } catch (Lexer.LexerException le) {
            // Lexical error -- show the line and the offending char.
            consoleArea.append("LEXICAL ERROR: " + le.getMessage() + "\n");
            resetParseTreeToEmpty();
        } catch (Parser.SyntaxException se) {
            // Syntax error -- the message already contains the line number.
            consoleArea.append("SYNTAX ERROR: " + se.getMessage() + "\n");
            resetParseTreeToEmpty();
        } catch (SemanticAnalyzer.SemanticException sme) {
            // Semantic error -- halt immediately on the first violation.
            consoleArea.append(sme.getMessage() + "\n");
            resetParseTreeToEmpty();
        } catch (Exception ex) {
            // Anything else -- log the stack trace for the developer.
            consoleArea.append("UNEXPECTED ERROR: " + ex.getMessage() + "\n");
            ex.printStackTrace();
            resetParseTreeToEmpty();
        }
    }

    /**
     * Helper used by every failure path in {@link #runAnalysis()}.
     * Swaps the JTree's model to a single "No Tree Generated" root
     * so the parse-tree tab never displays a stale tree from a
     * previous successful run.
     */
    private void resetParseTreeToEmpty() {
        DefaultMutableTreeNode emptyRoot = new DefaultMutableTreeNode("No Tree Generated");
        parseTreeModel = new DefaultTreeModel(emptyRoot);
        parseTree.setModel(parseTreeModel);
    }

    // ===============================================================
    //  APPLICATION ENTRY POINT
    // ===============================================================

    /**
     * Standard Java main: schedule the GUI creation on the
     * Event-Dispatch Thread to comply with Swing's single-thread rule.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CompilerGUI().setVisible(true));
    }
}

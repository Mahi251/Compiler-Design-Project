/**
 * ParseTreeNode.java
 * --------------------------------------------------------------------------
 * A single node in the syntax/parse tree (see instructions.md, Phase 4).
 *
 * Structure (per the spec):
 *   - label    : a String describing what this node represents
 *               (e.g. "Program", "IfStatement", "Assignment", an operator
 *                like "+", or -- for leaves -- a Token's lexeme).
 *   - children : an ordered list of child nodes, built left-to-right.
 *
 * The tree is built bottom-up by the recursive-descent Parser: each
 * grammar-rule method returns a ParseTreeNode representing the subtree
 * it just matched. The GUI later renders this tree in a Swing JTree
 * via the {@link #toTreeNode()} helper.
 * --------------------------------------------------------------------------
 */
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;

public class ParseTreeNode {

    /** Human-readable label for this node (grammar symbol or token). */
    private final String label;

    /** Ordered list of child nodes (empty for leaves). */
    private final List<ParseTreeNode> children;

    /**
     * Creates a new node with the given label and no children.
     *
     * @param label the node's display label
     */
    public ParseTreeNode(String label) {
        this.label = label;
        this.children = new ArrayList<>();
    }

    /**
     * Appends a child to this node and returns the child so calls can
     * be chained, e.g.  parent.addChild(new ParseTreeNode("X"));
     *
     * @param child the child node to attach
     * @return the same child (for convenience)
     */
    public ParseTreeNode addChild(ParseTreeNode child) {
        this.children.add(child);
        return child;
    }

    public String getLabel()                  { return label; }
    public List<ParseTreeNode> getChildren()  { return children; }

    /**
     * Recursively converts this node (and its subtree) into a Swing
     * DefaultMutableTreeNode so it can be plugged straight into a JTree.
     *
     * @return a Swing tree-node mirroring this parse-tree node
     */
    public DefaultMutableTreeNode toTreeNode() {
        DefaultMutableTreeNode swingNode = new DefaultMutableTreeNode(label);
        // Recursively convert every child subtree.
        for (ParseTreeNode child : children) {
            swingNode.add(child.toTreeNode());
        }
        return swingNode;
    }

    /** Debug helper: prints the tree in a simple indented form. */
    public void printTree(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) sb.append("  ");
        sb.append(label);
        System.out.println(sb.toString());
        for (ParseTreeNode child : children) {
            child.printTree(depth + 1);
        }
    }
}

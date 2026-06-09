/*
 * SemanticAnalyzer.java
 * --------------------------------------------------------------------------
 * Phase 4 of the compiler: semantic analysis and type checking.
 *
 * This class walks the parse tree produced by Parser.java and applies the
 * project-specific semantic rules:
 *   - flat scope only (no nested scopes)
 *   - halt on the first semantic error
 *   - no void variables or void parameters
 *   - no redeclaration in the flat scope
 *   - identifiers must be declared before use
 *   - function calls must match declared argument count and types
 *   - int -> float widening is allowed
 *   - float -> int narrowing is rejected
 *
 * The analyzer intentionally does not mutate the Parser's SymbolTable. It
 * keeps its own declaration-order state so it can enforce "declared before
 * use" without changing the existing parser design.
 * --------------------------------------------------------------------------
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SemanticAnalyzer {

    /** Thrown as soon as the first semantic error is found. */
    public static class SemanticException extends RuntimeException {
        public SemanticException(String message) {
            super(message);
        }
    }

    /** Small in-memory representation of a function signature. */
    private static class FunctionSignature {
        private final String returnType;
        private final List<String> parameterTypes;

        FunctionSignature(String returnType, List<String> parameterTypes) {
            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
        }
    }

    /** Identifiers that have been declared so far, in source order. */
    private final Set<String> declaredNames = new HashSet<>();

    /** Map from identifier name to declared type. */
    private final Map<String, String> declaredTypes = new HashMap<>();

    /** Map from function name to its signature. */
    private final Map<String, FunctionSignature> functions = new HashMap<>();

    /** Current function return type while analyzing a function body. */
    private String currentFunctionReturnType;

    /**
     * Entry point used by the GUI.
     *
     * @param root parse tree root returned by Parser.parseProgram()
     * @param symbolTable parser-built symbol table (read only here)
     */
    public void analyze(ParseTreeNode root, SymbolTable symbolTable) {
        declaredNames.clear();
        declaredTypes.clear();
        functions.clear();
        currentFunctionReturnType = null;

        if (root == null) {
            throw new SemanticException("Internal error: parse tree is null");
        }
        if (!"Program".equals(root.getLabel())) {
            throw new SemanticException("Internal error: expected Program root node");
        }

        // Walk top-level items in the exact order they appeared in the source.
        for (ParseTreeNode child : root.getChildren()) {
            analyzeTopLevelNode(child);
        }
    }

    /**
     * Handles the program's immediate children.
     * Top-level items are function definitions, declarations, or statements.
     */
    private void analyzeTopLevelNode(ParseTreeNode node) {
        String label = node.getLabel();

        if ("FunctionDefinition".equals(label)) {
            analyzeFunctionDefinition(node);
            return;
        }

        if ("Declaration".equals(label)) {
            analyzeDeclaration(node);
            return;
        }

        analyzeStatement(node);
    }

    /**
     * Function definitions are processed in two stages:
     *   1. register the function name and parameter names immediately so
     *      later body statements can call it
     *   2. analyze the function body with the current return type set
     */
    private void analyzeFunctionDefinition(ParseTreeNode node) {
        // Parser.java emits children in this order:
        //   0 -> ReturnType: <lexeme>
        //   1 -> FunctionName: <lexeme>
        //   2 -> Parameters
        //   3 -> Block
        String returnType = readPrefixedLabel(node.getChildren().get(0), "ReturnType: ");
        String functionName = readPrefixedLabel(node.getChildren().get(1), "FunctionName: ");

        // A name may appear only once in the flat scope.
        declareName(functionName, returnType, "function");

        // Collect parameter types now so function calls can be checked later.
        ParseTreeNode parametersNode = node.getChildren().get(2);
        List<String> parameterTypes = new ArrayList<>();

        for (ParseTreeNode parameterNode : parametersNode.getChildren()) {
            // Parameter nodes have two children:
            //   0 -> Type: <lexeme>
            //   1 -> Name: <lexeme>
            String parameterType = readPrefixedLabel(parameterNode.getChildren().get(0), "Type: ");
            String parameterName = readPrefixedLabel(parameterNode.getChildren().get(1), "Name: ");

            if ("void".equals(parameterType)) {
                throw semanticError("Parameter '" + parameterName + "' cannot have type 'void'");
            }

            // Parameters share the same flat scope as everything else.
            declareName(parameterName, parameterType, "parameter");
            parameterTypes.add(parameterType);
        }

        functions.put(functionName, new FunctionSignature(returnType, parameterTypes));

        // Analyze the function body with the return type in context.
        currentFunctionReturnType = returnType;
        analyzeBlock(node.getChildren().get(3));
        currentFunctionReturnType = null;
    }

    /**
     * Variable declarations are checked for:
     *   - duplicate names in the flat scope
     *   - illegal 'void' variable type
     *   - initializer compatibility, if present
     */
    private void analyzeDeclaration(ParseTreeNode node) {
        String type = readPrefixedLabel(node.getChildren().get(0), "Type: ");
        String name = readPrefixedLabel(node.getChildren().get(1), "Name: ");

        if ("void".equals(type)) {
            throw semanticError("Variable '" + name + "' cannot have type 'void'");
        }

        declareName(name, type, "variable");

        // The parser only adds an initializer when '=' is present.
        for (int i = 2; i < node.getChildren().size(); i++) {
            ParseTreeNode child = node.getChildren().get(i);
            if ("=".equals(child.getLabel())) {
                if (i + 1 >= node.getChildren().size()) {
                    throw semanticError("Malformed declaration for '" + name + "'");
                }
                String initializerType = inferExpressionType(node.getChildren().get(i + 1));
                ensureAssignable(type, initializerType, "variable '" + name + "'");
                break;
            }
        }
    }

    /**
     * Statements are handled by label.
     * Container nodes such as Block/IfStatement/WhileStatement/ForStatement
     * are traversed explicitly so the source order is preserved.
     */
    private void analyzeStatement(ParseTreeNode node) {
        String label = node.getLabel();

        if ("Block".equals(label)) {
            analyzeBlock(node);
            return;
        }

        if ("Declaration".equals(label)) {
            analyzeDeclaration(node);
            return;
        }

        if ("Assignment".equals(label)) {
            analyzeAssignment(node);
            return;
        }

        if ("FunctionCallStmt".equals(label)) {
            analyzeFunctionCallStatement(node);
            return;
        }

        if ("ReturnStatement".equals(label)) {
            analyzeReturnStatement(node);
            return;
        }

        if ("IfStatement".equals(label)) {
            analyzeIfStatement(node);
            return;
        }

        if ("WhileStatement".equals(label)) {
            analyzeWhileStatement(node);
            return;
        }

        if ("ForStatement".equals(label)) {
            analyzeForStatement(node);
            return;
        }

        // If we arrive here, the node is most likely an expression root.
        inferExpressionType(node);
    }

    /**
     * Blocks are analyzed in order, because the project intentionally uses a
     * single flat symbol table and therefore source order matters.
     */
    private void analyzeBlock(ParseTreeNode node) {
        for (ParseTreeNode child : node.getChildren()) {
            analyzeStatement(child);
        }
    }

    /**
     * If-statement structure from Parser.java:
     *   0 -> Condition
     *   1 -> condition expression
     *   2 -> Block
     *   3 -> Else (optional)
     *   4 -> Block (optional)
     */
    private void analyzeIfStatement(ParseTreeNode node) {
        if (node.getChildren().size() < 3) {
            throw semanticError("Malformed if-statement");
        }

        String conditionType = inferExpressionType(node.getChildren().get(1));
        requireConditionType(conditionType, "if-statement");

        analyzeStatement(node.getChildren().get(2));

        if (node.getChildren().size() > 3) {
            analyzeStatement(node.getChildren().get(4));
        }
    }

    /**
     * While-statement structure from Parser.java:
     *   0 -> Condition
     *   1 -> condition expression
     *   2 -> Block
     */
    private void analyzeWhileStatement(ParseTreeNode node) {
        if (node.getChildren().size() < 3) {
            throw semanticError("Malformed while-statement");
        }

        String conditionType = inferExpressionType(node.getChildren().get(1));
        requireConditionType(conditionType, "while-statement");

        analyzeStatement(node.getChildren().get(2));
    }

    /**
     * For-statement structure from Parser.java is slightly more flexible:
     *   0 -> Init
     *   1 -> optional initializer statement (declaration / assignment / call)
     *   2 -> Condition
     *   3 -> optional condition expression
     *   4 -> Update
     *   5 -> optional update expression / statement
     *   6 -> Block
     */
    private void analyzeForStatement(ParseTreeNode node) {
        List<ParseTreeNode> children = node.getChildren();
        if (children.size() < 4) {
            throw semanticError("Malformed for-statement");
        }

        int index = 1;

        // Optional initializer.
        if (index < children.size() && !"Condition".equals(children.get(index).getLabel())) {
            analyzeStatement(children.get(index));
            index++;
        }

        if (index >= children.size() || !"Condition".equals(children.get(index).getLabel())) {
            throw semanticError("Malformed for-statement: missing condition marker");
        }
        index++;

        // Optional condition expression. If the next child is the Update
        // marker, the condition was intentionally omitted.
        if (index < children.size() && !"Update".equals(children.get(index).getLabel())) {
            String conditionType = inferExpressionType(children.get(index));
            requireConditionType(conditionType, "for-loop condition");
            index++;
        }

        if (index >= children.size() || !"Update".equals(children.get(index).getLabel())) {
            throw semanticError("Malformed for-statement: missing update marker");
        }
        index++;

        // Optional update expression or statement.
        if (index < children.size() && !"Block".equals(children.get(index).getLabel())) {
            analyzeStatement(children.get(index));
            index++;
        }

        if (index >= children.size() || !"Block".equals(children.get(index).getLabel())) {
            throw semanticError("Malformed for-statement: missing body block");
        }
        analyzeStatement(children.get(index));
    }

    /**
     * Assignment structure from Parser.java:
     *   0 -> Target: <name>
     *   1 -> "="
     *   2 -> expression
     */
    private void analyzeAssignment(ParseTreeNode node) {
        if (node.getChildren().size() < 3) {
            throw semanticError("Malformed assignment");
        }

        String targetLabel = node.getChildren().get(0).getLabel();
        if (!targetLabel.startsWith("Target: ")) {
            throw semanticError("Malformed assignment target");
        }

        String targetName = targetLabel.substring("Target: ".length());
        String targetType = resolveIdentifierType(targetName);

        String expressionType = inferExpressionType(node.getChildren().get(2));
        ensureAssignable(targetType, expressionType, "variable '" + targetName + "'");
    }

    /**
     * Stand-alone function-call statements use the same call checking rules
     * as function calls inside expressions.
     */
    private void analyzeFunctionCallStatement(ParseTreeNode node) {
        if (node.getChildren().isEmpty()) {
            throw semanticError("Malformed function call statement");
        }

        String callLabel = node.getChildren().get(0).getLabel();
        if (!callLabel.startsWith("Call: ")) {
            throw semanticError("Malformed function call statement");
        }

        String functionName = callLabel.substring("Call: ".length());
        FunctionSignature signature = resolveFunctionSignature(functionName);

        ParseTreeNode argumentsNode = node.getChildren().size() > 1 ? node.getChildren().get(1) : null;
        List<String> argumentTypes = new ArrayList<>();

        if (argumentsNode != null) {
            for (ParseTreeNode argumentNode : argumentsNode.getChildren()) {
                argumentTypes.add(inferExpressionType(argumentNode));
            }
        }

        ensureCallMatchesSignature(functionName, signature, argumentTypes);
    }

    /**
     * Return-statement checks depend on the current function return type.
     */
    private void analyzeReturnStatement(ParseTreeNode node) {
        if (currentFunctionReturnType == null) {
            throw semanticError("Return statement appears outside of a function");
        }

        // Bare "return;" is only legal in void functions.
        if (node.getChildren().isEmpty()) {
            if (!"void".equals(currentFunctionReturnType)) {
                throw semanticError("Function must return '" + currentFunctionReturnType + "' but return statement has no value");
            }
            return;
        }

        String returnedType = inferExpressionType(node.getChildren().get(0));
        ensureAssignable(currentFunctionReturnType, returnedType, "return value");
    }

    /**
     * Infers the type of an expression and enforces type rules while doing so.
     *
     * The parser emits expressions as a left-associative tree:
     *   firstOperand, Operator:+, nextOperand, Operator:*, nextOperand, ...
     *
     * This method reads the base operand type, then walks the trailing
     * operator/operand pairs left-to-right.
     */
    private String inferExpressionType(ParseTreeNode node) {
        String currentType = inferPrimaryType(node);

        List<ParseTreeNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            ParseTreeNode operatorNode = children.get(i);
            String operatorLabel = operatorNode.getLabel();

            if (!operatorLabel.startsWith("Operator: ")) {
                continue;
            }

            if (i + 1 >= children.size()) {
                throw semanticError("Malformed expression near operator '" + operatorLabel + "'");
            }

            String operator = operatorLabel.substring("Operator: ".length()).trim();
            String rightType = inferExpressionType(children.get(i + 1));
            currentType = applyBinaryOperator(operator, currentType, rightType);

            // Skip the right operand we just consumed.
            i++;
        }

        return currentType;
    }

    /**
     * Infers the type of the node's base value before any trailing operators.
     */
    private String inferPrimaryType(ParseTreeNode node) {
        String label = node.getLabel();

        if (label.startsWith("Literal: ")) {
            String lexeme = label.substring("Literal: ".length());
            return lexeme.contains(".") ? "float" : "int";
        }

        if (label.startsWith("Identifier: ")) {
            String identifierName = label.substring("Identifier: ".length());
            return resolveIdentifierType(identifierName);
        }

        if (label.startsWith("FunctionCall: ")) {
            String functionName = label.substring("FunctionCall: ".length());
            FunctionSignature signature = resolveFunctionSignature(functionName);

            List<String> argumentTypes = new ArrayList<>();
            for (ParseTreeNode argumentNode : node.getChildren()) {
                argumentTypes.add(inferExpressionType(argumentNode));
            }

            ensureCallMatchesSignature(functionName, signature, argumentTypes);
            return signature.returnType;
        }

        // Parenthesized expressions are returned directly by the parser, so
        // they are represented by the inner expression node itself.
        if (!node.getChildren().isEmpty()) {
            // If the node already contains operator children, inferExpressionType
            // will process them after this base-type step. If the node is just a
            // wrapper-like container with one child, recurse into that child.
            if (node.getChildren().size() == 1 && !label.startsWith("Operator: ")) {
                return inferExpressionType(node.getChildren().get(0));
            }
        }

        throw semanticError("Expected an expression, got '" + label + "'");
    }

    /**
     * Applies one binary operator with the requested type rules.
     */
    private String applyBinaryOperator(String operator, String leftType, String rightType) {
        if ("+".equals(operator) || "-".equals(operator) || "*".equals(operator) || "/".equals(operator)) {
            ensureNumericOperand(leftType, operator);
            ensureNumericOperand(rightType, operator);
            return ("float".equals(leftType) || "float".equals(rightType)) ? "float" : "int";
        }

        if ("<".equals(operator) || ">".equals(operator) || "<=".equals(operator)
                || ">=".equals(operator) || "==".equals(operator) || "!=".equals(operator)) {
            ensureNumericOperand(leftType, operator);
            ensureNumericOperand(rightType, operator);
            return "int";
        }

        if ("&&".equals(operator) || "||".equals(operator)) {
            ensureNumericOperand(leftType, operator);
            ensureNumericOperand(rightType, operator);
            return "int";
        }

        throw semanticError("Unsupported operator '" + operator + "'");
    }

    /**
     * Numeric operands are required for every operator in this project.
     * The language has no separate boolean type, so numeric results are used
     * for conditions as well.
     */
    private void ensureNumericOperand(String type, String operator) {
        if (!"int".equals(type) && !"float".equals(type)) {
            throw semanticError("Operator '" + operator + "' requires numeric operands, but found '" + type + "'");
        }
    }

    /**
     * Conditions are treated as numeric expressions in this project.
     */
    private void requireConditionType(String type, String context) {
        if (!"int".equals(type) && !"float".equals(type)) {
            throw semanticError("" + context + " requires a numeric condition, but found '" + type + "'");
        }
    }

    /**
     * Assignment compatibility rules:
     *   - int -> float allowed
     *   - float -> int rejected
     *   - exact matches allowed
     */
    private void ensureAssignable(String targetType, String sourceType, String targetDescription) {
        if (targetType == null || sourceType == null) {
            throw semanticError("Internal error while checking assignment to " + targetDescription);
        }

        if (targetType.equals(sourceType)) {
            return;
        }

        if ("float".equals(targetType) && "int".equals(sourceType)) {
            return;
        }

        throw semanticError("Cannot assign '" + sourceType + "' to '" + targetType + "' in " + targetDescription);
    }

    /**
     * Resolves a declared identifier and returns its type.
     * The check is order-sensitive because the analyzer walks the tree in
     * source order and only records declarations when they are reached.
     */
    private String resolveIdentifierType(String name) {
        if (!declaredNames.contains(name)) {
            throw semanticError("Identifier '" + name + "' used before declaration");
        }

        String type = declaredTypes.get(name);
        if (type == null) {
            throw semanticError("Internal error: no type recorded for identifier '" + name + "'");
        }

        return type;
    }

    /**
     * Resolves a declared function signature.
     */
    private FunctionSignature resolveFunctionSignature(String name) {
        if (!declaredNames.contains(name)) {
            throw semanticError("Function '" + name + "' used before declaration");
        }

        FunctionSignature signature = functions.get(name);
        if (signature == null) {
            throw semanticError("Identifier '" + name + "' is not a function");
        }

        return signature;
    }

    /**
     * Shared function-call validation for both statements and expressions.
     */
    private void ensureCallMatchesSignature(String functionName, FunctionSignature signature, List<String> argumentTypes) {
        if (argumentTypes.size() != signature.parameterTypes.size()) {
            throw semanticError("Function '" + functionName + "' expects " + signature.parameterTypes.size()
                    + " arguments but received " + argumentTypes.size());
        }

        for (int i = 0; i < argumentTypes.size(); i++) {
            String expectedType = signature.parameterTypes.get(i);
            String actualType = argumentTypes.get(i);
            ensureAssignable(expectedType, actualType, "argument #" + (i + 1) + " of function '" + functionName + "'");
        }
    }

    /**
     * Declares a name in the flat scope. Redeclaration is a semantic error.
     */
    private void declareName(String name, String type, String kind) {
        if (declaredNames.contains(name)) {
            throw semanticError("Redeclaration of " + kind + " '" + name + "'");
        }

        declaredNames.add(name);
        declaredTypes.put(name, type);
    }

    /**
     * Helper for labels such as 'Name: foo' or 'Type: int'.
     */
    private String readPrefixedLabel(ParseTreeNode node, String prefix) {
        String label = node.getLabel();
        if (!label.startsWith(prefix)) {
            throw semanticError("Expected label starting with '" + prefix + "' but found '" + label + "'");
        }
        return label.substring(prefix.length());
    }

    /**
     * Creates the single semantic exception type used by the analyzer.
     */
    private SemanticException semanticError(String message) {
        return new SemanticException("Semantic Error: " + message);
    }
}

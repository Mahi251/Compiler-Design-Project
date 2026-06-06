# C-Subset Compiler — Lexical & Syntax Analysis Phases

**Course:** Compiler Design
**Deliverable Type:** Front-end Compiler (Phases 1 & 2) with Interactive GUI
**Language:** Java (JDK 8+)
**GUI Framework:** Java Swing (no external libraries)

---

## 1. Title & Introduction

### 1.1 Project Aim

The aim of this project is to engineer the two front-end phases of a compiler for a deliberately constrained subset of the C programming language. Specifically, the system implements:

1. **Phase 1 — Lexical Analysis:** converting a raw source-code string into a flat, ordered list of lexical tokens.
2. **Phase 2 — Syntax Analysis:** validating that the token stream conforms to a formal context-free grammar via a top-down recursive-descent parser, and constructing:
   * A populated **Symbol Table** recording every declared identifier and function, and
   * A hierarchical **Parse Tree** that mirrors the grammatical structure of the input program.

The system is delivered as a single-window Java Swing application. The user pastes or types source code into a code area, presses a *Compile / Analyze* button, and receives — in three synchronised panels — the token stream, the symbol table, and the parse tree. Lexical or syntactic errors are reported in a console region with the exact line number and offending token, and the parse-tree panel is reset to a neutral state so no stale output from a prior run persists on screen.

### 1.2 Scope Discipline ("No Scope Creep")

To keep the project tractable, the language is intentionally limited to the constructs enumerated in §2. The following C features are explicitly **out of scope** and will produce errors if encountered: `char`, `double`, `long`, `short`, `signed`, `unsigned`, arrays, pointers, `struct`, `union`, `enum`, `switch`/`case`/`default`, `do…while`, `break`, `continue`, `goto`, preprocessor directives (`#include`, `#define`), string literals, character literals, multi-dimensional constructs, and global-scope rules. This strict scope guarantees that the implemented grammar is closed, deterministic, and easy to defend.

### 1.3 High-Level Pipeline

```
   Source String
        │
        ▼
   ┌─────────┐    List<Token>     ┌─────────┐    ParseTreeNode  +  SymbolTable
   │  Lexer  │ ─────────────────► │ Parser  │ ──────────────────────────────► GUI
   └─────────┘                    └─────────┘
   Phase 1                         Phase 2 + 3 + 4
   (character-by-character)        (recursive descent)
```

---

## 2. Language Specification Grammar (BNF / EBNF)

The grammar below is the formal contract between the Lexer and the Parser. Every non-terminal has a direct one-to-one correspondence with a `parse…` method in `Parser.java`. Terminals appear in **monospace** and are produced by the Lexer as `Token` values. `ε` denotes the empty production.

### 2.1 Top-Level Structure

```bnf
program           → ( functionDefinition | statement )*  EOF
functionDefinition→ type  IDENTIFIER  "("  parameters?  ")"  block
parameters        → parameter  ( ","  parameter )*
parameter         → type  IDENTIFIER
type              → "int" | "float" | "void"
```

### 2.2 Statements

```bnf
statement         → declaration
                   | assignment
                   | functionCallStmt
                   | returnStmt
                   | ifStmt
                   | whileStmt
                   | forStmt
                   | block

declaration       → type  IDENTIFIER  ( "="  expression )?  ";"
assignment        → IDENTIFIER  "="  expression  ";"
functionCallStmt  → IDENTIFIER  "("  arguments?  ")"  ";"
returnStmt        → "return"  expression?  ";"

ifStmt            → "if"  "("  condition  ")"  block  ( "else"  block )?
whileStmt         → "while"  "("  condition  ")"  block
forStmt           → "for"  "("  ( declaration | assignment | ";" )
                              condition?  ";"
                              update?  ")"  block
block             → "{"  statement*  "}"
```

### 2.3 Conditions, Arguments, Expressions

```bnf
condition         → expression
arguments         → expression  ( ","  expression )*
```

Expressions are parsed with the classic **precedence-climbing** method (lowest to highest):

```bnf
expression        → logicalOr
logicalOr         → logicalAnd   ( "||"  logicalAnd )*
logicalAnd        → equality      ( "&&"  equality )*
equality          → relational    ( ( "==" | "!=" )  relational )*
relational        → additive      ( ( "<" | ">" | "<=" | ">=" )  additive )*
additive          → multiplicative( ( "+" | "-" )  multiplicative )*
multiplicative    → primary       ( ( "*" | "/" )  primary )*
primary           → INT_LITERAL
                   | FLOAT_LITERAL
                   | IDENTIFIER  ( "("  arguments?  ")" )?
                   | "("  expression  ")"
```

### 2.4 Operator Precedence Table (Highest → Lowest)

| Precedence | Operators                         | Associativity | Parser Method            |
|------------|-----------------------------------|---------------|--------------------------|
| 7 (high)   | `()` (function call / grouping)   | n/a           | `parsePrimary`           |
| 6          | `*`, `/`                          | left          | `parseMultiplicative`    |
| 5          | `+`, `-`                          | left          | `parseAdditive`          |
| 4          | `<`, `>`, `<=`, `>=`              | left          | `parseRelational`        |
| 3          | `==`, `!=`                        | left          | `parseEquality`          |
| 2          | `&&`                              | left          | `parseLogicalAnd`        |
| 1 (low)    | `\|\|`                            | left          | `parseLogicalOr`         |
| 0          | `=` (assignment, not inside expr) | right (statement context) | `parseAssignment` |

### 2.5 Lexical Conventions (Driven by the Lexer)

| Category       | Pattern                                | Token Types Emitted                                       |
|----------------|----------------------------------------|-----------------------------------------------------------|
| Identifier     | `[A-Za-z_][A-Za-z0-9_]*`               | `IDENTIFIER` or keyword-specific type if lexeme matches  |
| Integer        | `[0-9]+`                               | `INT_LITERAL`                                             |
| Float          | `[0-9]+\.[0-9]+`                       | `FLOAT_LITERAL`                                           |
| Arithmetic     | `+ - * /`                              | `PLUS`, `MINUS`, `STAR`, `SLASH`                          |
| Relational     | `<  >  <=  >=`                         | `LESS`, `GREATER`, `LESS_EQUAL`, `GREATER_EQUAL`          |
| Equality       | `==  !=`                               | `EQUAL`, `NOT_EQUAL`                                      |
| Logical        | `&&  \|\|`                             | `AND`, `OR`                                               |
| Assignment     | `=`                                    | `ASSIGN`                                                  |
| Punctuation    | `{  }  (  )  ;  ,`                     | `LBRACE`, `RBRACE`, `LPAREN`, `RPAREN`, `SEMICOLON`, `COMMA` |
| Whitespace     | space, tab, carriage-return            | discarded                                                 |
| Line break     | `\n`                                   | discarded; line counter increments                        |
| Sentinel       | end of input                           | `EOF`                                                     |

---

## 3. Architectural Component Breakdown

The system is partitioned into seven classes, each owning a single concern. The dependency direction is strictly downward: the GUI depends on the Parser, which depends on the Lexer, which depends on `Token` / `TokenType`.

### 3.1 `TokenType.java` — Token Category Enumeration

**Role:** Enumerates every category of token the Lexer can emit and that the Parser must recognise. Stored as a strongly-typed `enum` to eliminate "magic string" bugs (e.g. accidentally comparing against the literal `"INT"` instead of `TokenType.INT`).

The enum is partitioned into five semantic groups: data types (`INT`, `FLOAT`, `VOID`), control-flow keywords (`IF`, `ELSE`, `WHILE`, `FOR`, `RETURN`), literals (`INT_LITERAL`, `FLOAT_LITERAL`), the generic `IDENTIFIER`, operator groups (arithmetic, relational, logical, assignment), punctuation delimiters, and the `EOF` sentinel. New token categories are intentionally **not** added when new language features are considered — this enforces the "no scope creep" rule at the type level.

### 3.2 `Token.java` — Immutable Token Value Object

**Role:** A plain immutable data record that travels through every subsequent phase of the pipeline. Each instance carries three fields:

* `type` — the `TokenType` category, exposed via `getType()`.
* `lexeme` — the exact substring of source code that produced the token, exposed via `getLexeme()`.
* `lineNumber` — the 1-based line on which the token was found, exposed via `getLineNumber()`.

All three fields are `final` and assigned only in the constructor. The class overrides `toString()` to a `Token{type=…, lexeme='…', line=…}` format that is used by the GUI's token table and is invaluable during debugging.

### 3.3 `Symbol.java` — Symbol-Table Entry

**Role:** A small POJO representing a single declared identifier in the program. Each `Symbol` holds:

* `name` — the identifier lexeme, exposed via `getName()`.
* `dataType` — the declared type as a String, one of `"int"`, `"float"`, `"void"`, exposed via `getDataType()`. Storing it as a String (rather than a `TokenType`) keeps this class decoupled from the Lexer.
* `type` — a nested `SymbolType` enum with two values: `VARIABLE` and `FUNCTION`. This distinction lets later phases (out of scope here, but designed for) differentiate "is this identifier a value I can assign to?" from "is it something I can call?".

### 3.4 `SymbolTable.java` — Identifier Repository

**Role:** Stores every `Symbol` produced during parsing, keyed by identifier name. The backing data structure is `HashMap<String, Symbol>`, providing O(1) average-case insertion (`declare`) and lookup (`lookup`).

Public API:

| Method                              | Purpose                                                                 |
|-------------------------------------|-------------------------------------------------------------------------|
| `declare(name, dataType, type)`     | Insert or overwrite a symbol — overwriting is a deliberate educational simplification; a production compiler would flag a redeclaration as a semantic error. |
| `lookup(name)`                      | Return the matching `Symbol` or `null` if not present.                  |
| `getAll()`                          | Return an unordered `Collection<Symbol>` (HashMap iteration order).     |
| `getAllSorted()`                    | Return a `List<Symbol>` sorted alphabetically by name — the GUI uses this to give the user a stable, predictable display. |
| `size()`                            | Return the entry count, used by the GUI's success message.              |

The Parser calls `declare` at exactly two points: when a `parseDeclaration` successfully matches a variable declaration, and when `parseFunctionDefinition` matches the function header. The GUI never writes to the table directly; it is a read-only consumer.

### 3.5 `Lexer.java` — Character-by-Character Scanner

**Role:** Convert a raw `String` of source code into a `List<Token>`. The Lexer is a single-pass scanner; it does not use Java's regex engine, which keeps the implementation transparent and educational.

**Internal state:**

* `source` — the immutable input string.
* `pos` — 0-based index of the next character to consume.
* `line` — 1-based current line number, used for error reporting.

**`tokenize()` algorithm:**

1. Loop while `pos < source.length()`.
2. If the current character is space / tab / carriage-return, advance and continue.
3. If it is a newline (`\n`), increment `line` and advance.
4. If it is a letter or underscore, hand off to `readIdentifierOrKeyword()`, which consumes an identifier, then checks the lexeme against the reserved-word set (`int`, `float`, `void`, `if`, `else`, `while`, `for`, `return`); a match returns the corresponding keyword token, otherwise an `IDENTIFIER` token.
5. If it is a digit, hand off to `readNumber()`, which consumes digits, and if a `.` followed by another digit is encountered, continues into `FLOAT_LITERAL`; otherwise it returns `INT_LITERAL`.
6. Otherwise, hand off to `readOperatorOrPunctuation()`, which uses a `switch` on the current character with one-character look-ahead to disambiguate multi-character operators (`==`, `!=`, `<=`, `>=`, `&&`, `||`) from their single-character siblings (`=`, `!`, `<`, `>`, `&`, `|`). The single-character variants of `!`, `&`, and `|` are *not* part of the language and trigger a `LexerException` with a helpful "did you mean …?" hint.
7. Any unrecognised character (e.g. `@`, `#`, `'`, `"`, `[`, `]`) triggers `LexerException("Unrecognised character 'X' at line N")`.
8. After the loop, append a single `EOF` token so the Parser can detect end-of-input without an extra flag.

**Error handling:** The Lexer defines its own unchecked `LexerException` (a `RuntimeException` subclass) so the GUI can display lexical errors distinctly from syntax errors, though in the current GUI both flow through the same console region.

### 3.6 `Parser.java` — Recursive-Descent Syntax Analyzer

**Role:** Consume the `List<Token>` produced by the Lexer, verify it against the grammar in §2, and as a side-effect populate the `SymbolTable` and build a `ParseTreeNode` hierarchy.

**Internal state:**

* `tokens` — the immutable token list.
* `current` — index of the "current" token under examination.
* `symbolTable` — owned by the Parser (created in the constructor), exposed via `getSymbolTable()`.
* `root` — the topmost `ParseTreeNode` (a `"Program"` node), exposed via `getParseTree()`.

**`parseProgram()`** is the entry point. It loops until `EOF`, dispatching to `parseFunctionDefinition` (when the three-token look-ahead pattern `<type> IDENTIFIER (` is seen) or to `parseStatement` otherwise. The use of look-ahead without consumption is the standard way to disambiguate top-level productions in recursive-descent parsers.

**Helper primitives** form the standard parsing toolkit:

* `peek()` — current token, no advance.
* `previous()` — most recently consumed token (used in error messages).
* `advance()` — consume and return the current token.
* `check(type)` — does the current token have the given type?
* `consume(type, msg)` — atomic "must be this token" operation; throws `SyntaxException` with line, expected, and got information if the check fails.
* `isAtEnd()` — convenience wrapper around `peek().getType() == EOF`.

**Each grammar rule** has a dedicated private method named `parse…`. The methods return `ParseTreeNode` objects so the tree is constructed bottom-up; the parent caller attaches the returned subtree to its own node via `addChild`. For example, `parseAdditive` calls `parseMultiplicative` for the left operand, then in a `while` loop consumes any number of `+`/`-` operators, each time appending an `"Operator: +"` (or `-`) label node and a recursive subtree for the right operand.

**Symbol-table side effects** are localised to two call sites:

* `parseDeclaration` → `symbolTable.declare(id, type, VARIABLE)`.
* `parseFunctionDefinition` → `symbolTable.declare(name, returnType, FUNCTION)`.
* `parseParameter` → `symbolTable.declare(id, type, VARIABLE)`.

**Error handling:** The Parser defines a custom unchecked `SyntaxException`. Every `consume` call embeds the current line number, the expected token description, and the lexeme actually encountered. The message format is:

```
Syntax Error at line <N>: <reason> (got '<lexeme>')
```

A `SyntaxException` halts the parse immediately; no error recovery is attempted. This is consistent with the project specification: the GUI must show *where* the error is, not silently try to continue.

### 3.7 `ParseTreeNode.java` — Parse-Tree Node

**Role:** A single node in the hierarchical syntax tree. Each node has:

* `label` — a `String` describing what the node represents (e.g. `"Program"`, `"IfStatement"`, `"Assignment"`, `"Literal: 42"`, `"Operator: +"`).
* `children` — an ordered `List<ParseTreeNode>` of subtrees, built left-to-right.

The public API consists of `addChild` (which returns the child for call chaining), `getLabel`, `getChildren`, and two presentation helpers: `toTreeNode()` recursively converts the node into a `javax.swing.tree.DefaultMutableTreeNode` (the data type that Swing's `JTree` consumes), and `printTree(depth)` emits a depth-indented textual rendering for command-line debugging.

### 3.8 `CompilerGUI.java` — Swing Front-End

**Role:** The single user-facing class, extending `JFrame`. It owns all GUI widgets, wires the action listener, and orchestrates the three-phase pipeline.

**Layout** (top-to-bottom, using `BorderLayout`):

* **North** — `JTextArea` for source-code entry (pre-populated with a demo program) plus a bold `JButton` labelled *Compile / Analyze* on the east side.
* **Centre** — `JTabbedPane` with three tabs: *Tokens* (a `JTable` showing `Line | Token Type | Lexeme`), *Symbol Table* (`JTable` showing `Identifier Name | Data Type | Symbol Type`), and *Parse Tree* (a `JTree` rooted at the program's label, fully expandable).
* **South** — A read-only `JTextArea` serving as the console / error output panel.

**Action handler `runAnalysis()`** executes four ordered phases on every button click:

1. **UI reset** (per `update.txt`): clear the console, clear both table models to zero rows, and replace the `JTree` model with a single `"No Tree Generated"` root. This guarantees that no stale data from a prior compilation is visible.
2. **Lexical analysis**: instantiate `Lexer(source)`, call `tokenize()`, and append every token (except the trailing `EOF`) as a row in the Tokens table.
3. **Syntax / symbol / tree analysis**: instantiate `Parser(tokens)`, call `parseProgram()`. On success, build a fresh `DefaultTreeModel` from `root.toTreeNode()` and assign it to the `JTree`; iterate `parser.getSymbolTable().getAllSorted()` to populate the Symbol Table view.
4. **Reporting**: append two status lines to the console ("Syntax analysis complete: N symbols stored.", "Build Successful.").

All `LexerException` and `SyntaxException` throws are caught at the top level. On any failure, the console receives a prefixed message (`LEXICAL ERROR:` or `SYNTAX ERROR:`), the parse-tree panel is reset to the neutral state via `resetParseTreeToEmpty()`, and the GUI remains interactive so the user can correct the input and re-run.

**Application entry point:** `public static void main(String[] args)` schedules GUI creation on the Event-Dispatch Thread via `SwingUtilities.invokeLater`, complying with Swing's single-thread rule.

---

## 4. Test Cases & Quality Assurance

The system was validated against three representative scenarios. Each test exercises a different boundary of the implementation: the success path, syntax-error recovery, and lexical-error recovery.

### 4.1 Test Case 1 — Valid Program (Success Path)

**Objective:** Confirm that a complete, syntactically valid C-subset program is accepted end-to-end, that the Lexer produces a plausible token sequence, that the Parser populates the Symbol Table, and that the Parse Tree is non-trivial and expandable in the `JTree`.

**Input C-subset program:**

```c
int add(int a, int b) {
    return a + b;
}

int main() {
    int x;
    float y = 3.14;
    x = 10;
    if (x > 0) {
        y = y + 1.0;
    } else {
        y = 0.0;
    }
    while (x > 0) {
        x = x - 1;
    }
    for (int i = 0; i < 5; i = i + 1) {
        add(i, 2);
    }
    return x;
}
```

**Expected behavior:**

1. **Token panel** — populates with 60+ rows (excluding EOF) covering every token type: keywords (`int`, `float`, `return`, `if`, `else`, `while`, `for`), identifiers (`add`, `a`, `b`, `main`, `x`, `y`, `i`), literals (`3.14`, `10`, `0`, `5`, `1.0`), operators (`+`, `-`, `>`, `=`, `<`), and punctuation (`{`, `}`, `(`, `)`, `;`, `,`).
2. **Symbol Table panel** — shows the following rows (alphabetical):
   * `a : int (VARIABLE)`
   * `add : int (FUNCTION)`
   * `b : int (VARIABLE)`
   * `i : int (VARIABLE)`
   * `main : int (FUNCTION)`
   * `x : int (VARIABLE)`
   * `y : float (VARIABLE)`
3. **Parse Tree panel** — root labelled `Program`, with two top-level children (`FunctionDefinition` for `add` and `FunctionDefinition` for `main`). Expanding the `main` subtree reveals `Parameters` (empty), `Block`, then inside the block: `Declaration` (for `x`), `Declaration` (for `y` with initializer subtree), `Assignment`, `IfStatement` (with `Condition`, `Block`, `Else`, `Block`), `WhileStatement` (with `Condition` and `Block`), `ForStatement` (with `Init`, `Condition`, `Update`, `Block`), and `ReturnStatement`.
4. **Console** — `Lexical analysis complete: <N> tokens generated.` followed by `Syntax analysis complete: 7 symbols stored.` and `Build Successful.`

This test confirms the full pipeline and the GUI's success-path rendering for all three data panels.

### 4.2 Test Case 2 — Syntax Error: Missing Semicolon

**Objective:** Verify that a syntactic violation is caught at the correct line, that the GUI surfaces a useful diagnostic, and that the parse-tree panel is reset so no stale tree is left visible.

**Input C-subset snippet (note: line 2 is missing its terminating semicolon):**

```c
int main() {
    int x = 5
    float y = 2.0;
    return 0;
}
```

**Expected behavior:**

1. **Token panel** — fully populated; the missing semicolon is a *syntactic* problem, not a lexical one, so all tokens are generated normally.
2. **Symbol Table panel** — remains **empty**. Because the Parser throws before it can finish the `parseDeclaration` for `x`, the symbol table is never populated for this run.
3. **Parse Tree panel** — shows the neutral `No Tree Generated` placeholder. The `runAnalysis()` failure path calls `resetParseTreeToEmpty()`, so any tree from a previous successful run is wiped.
4. **Console** — displays:
   ```
   Lexical analysis complete: <N> tokens generated.
   SYNTAX ERROR: Syntax Error at line 2: Expected ';' after declaration (got 'float')
   ```
   The line number (2), the expected token (`';'`), and the offending lexeme (`float`) are all present, giving the user enough information to localise and correct the bug.

This test confirms the `SyntaxException` throw site inside `parseDeclaration` (the `consume(TokenType.SEMICOLON, …)` call), the `catch` block in `runAnalysis()`, and the explicit UI-reset behaviour required by `update.txt`.

### 4.3 Test Case 3 — Lexical Error: Out-of-Subset Character

**Objective:** Verify that a character outside the language's lexical alphabet is rejected at the earliest possible stage, that the error message identifies the offending character and line, and that downstream panels are not left in a half-populated state.

**Input C-subset snippet (line 2 contains a character literal `'a'`, which is *not* in the supported subset):**

```c
int main() {
    char ch = 'a';
    return 0;
}
```

**Expected behavior:**

1. **Token panel** — contains the tokens generated *up to* the offending character on line 2: `int`, `main`, `(`, `)`, `{`, `int`, `ch`, `=`. The Lexer halts at the `'` character.
2. **Symbol Table panel** — **empty**. The Parser is never invoked, so nothing is declared.
3. **Parse Tree panel** — `No Tree Generated` (the neutral placeholder).
4. **Console** — displays:
   ```
   LEXICAL ERROR: Unrecognised character ''' at line 2
   ```
   The error is caught by the `LexerException` branch of the GUI's top-level `try` block, and `resetParseTreeToEmpty()` is invoked.

This test confirms that the Lexer's `default` branch in `readOperatorOrPunctuation()` is the gatekeeper for all unsupported characters (`'`, `"`, `@`, `#`, `[`, `]`, etc.), and that the GUI's two-tier catch (`LexerException` and `SyntaxException`) lets the user distinguish the two error classes at a glance.

### 4.4 Quality Assurance Summary

| Dimension                        | Verification Mechanism                                            | Result |
|----------------------------------|-------------------------------------------------------------------|--------|
| Lexical correctness              | Test Case 1 token stream manually inspected against the input    | Pass   |
| Lexical robustness               | Test Case 3 — out-of-subset char rejected with line number        | Pass   |
| Syntactic correctness            | Test Case 1 — entire program parses to completion                 | Pass   |
| Syntactic robustness             | Test Case 2 — missing semicolon caught with precise line + token  | Pass   |
| Symbol-table accuracy            | Test Case 1 — 7 expected symbols, sorted, with correct kinds      | Pass   |
| Parse-tree structure             | Test Case 1 — `Program` → `FunctionDefinition` × 2 → full bodies  | Pass   |
| GUI error-path hygiene           | Test Cases 2 & 3 — no stale tree or table rows after failure      | Pass   |
| GUI reset between runs           | Compile button always clears prior state before analysis          | Pass   |
| No-scope-creep compliance        | `TokenType` enum contains no entries beyond the specification     | Pass   |

---

## 5. Build & Run

The project compiles and runs with a stock JDK (8 or later) — no external dependencies, no build tool required.

```bash
# from the project root
javac *.java
java CompilerGUI
```

The Swing window opens, pre-loaded with the Test Case 1 program. Pressing *Compile / Analyze* populates all three panels and prints a success message in the console.

---

## 6. Conclusion

This project demonstrates the canonical two-phase front-end of a compiler — lexical analysis followed by syntax analysis — implemented from first principles in Java with a Swing GUI. The Lexer's character-by-character design is transparent and easy to defend; the Parser's one-method-per-rule structure mirrors the grammar exactly, making the relationship between code and specification visually obvious; the HashMap-backed Symbol Table and JTree-rendered Parse Tree turn internal compiler state into inspectable, user-visible artifacts; and the careful error-handling discipline — `LexerException` and `SyntaxException` carrying the line number, expected token, and offending lexeme — guarantees that the GUI degrades gracefully on bad input rather than crashing.

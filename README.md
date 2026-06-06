# C-Subset Compiler — Lexical & Syntax Analyzer

A self-contained Java Swing application that implements the **Lexical Analysis**
and **Syntax Analysis** phases of a compiler for a small subset of the C
language. The program is driven by a friendly graphical interface: paste
source code, click **Compile / Analyze**, and inspect the resulting token
stream, symbol table, and parse tree.

---

## Table of Contents
1. [Features](#features)
2. [Supported Language Subset](#supported-language-subset)
3. [Project Structure](#project-structure)
4. [Architecture & Pipeline](#architecture--pipeline)
5. [Getting Started](#getting-started)
6. [Usage](#usage)
7. [Error Handling](#error-handling)
8. [Limitations](#limitations)
9. [Author](#author)

---

## Features
- **Single-pass Lexer** that converts source text into a typed token stream.
- **Recursive-descent Parser** with full operator-precedence support.
- **Symbol Table** populated automatically as declarations and function
  definitions are matched.
- **Visual Parse Tree** rendered in an expandable Swing `JTree`.
- **Tabbed Swing GUI** with separate panels for tokens, symbols, parse
  tree, and a console for status / error output.
- **Resilient error reporting** — lexical and syntax errors are caught,
  described, and displayed without crashing the application.
- **No external dependencies** — pure JDK 8+ standard library only.

---

## Supported Language Subset

### Data Types
`int`, `float`, `void`

### Keywords
`if`, `else`, `while`, `for`, `return`

### Literals
- Integer literal: `42`
- Float literal: `3.14`

### Operators
| Category       | Symbols                       |
|----------------|-------------------------------|
| Arithmetic     | `+  -  *  /`                  |
| Relational     | `==  !=  <  >  <=  >=`        |
| Logical        | `&&  ||`                      |
| Assignment     | `=`                           |

### Statements
- Declaration: `int x;` or `float y = 5.5;`
- Assignment: `x = 10;` or `x = y + 5;`
- Function call: `myFunction(x, y);`
- Return: `return x;` or `return;`
- Conditional: `if (cond) { ... } else { ... }`
- Loops: `while (cond) { ... }` and `for (init; cond; update) { ... }`
- Function definition: `int myFunc(int a) { ... }`

---

## Project Structure

| File                  | Responsibility                                            |
|-----------------------|-----------------------------------------------------------|
| `TokenType.java`      | Enum of every legal token category                        |
| `Token.java`          | Immutable token (type, lexeme, line number)               |
| `Symbol.java`         | Symbol-table entry (name, data type, kind)                |
| `ParseTreeNode.java`  | Generic tree node with helper to convert to `JTree` model |
| `Lexer.java`          | Phase 1 — character-by-character scanner                  |
| `SymbolTable.java`    | Phase 3 — `HashMap`-backed table of declared symbols     |
| `Parser.java`         | Phase 2 — recursive-descent syntax analyzer               |
| `CompilerGUI.java`    | Phase 5 — Swing front-end (entry point with `main`)       |
| `instructions.md`     | Original specification document                           |
| `update.txt`          | Subsequent UI reset patch                                 |

---

## Architecture & Pipeline

```
   +-----------+      +---------+      +---------+      +---------+
   | Source    | ---> | Lexer   | ---> | Parser  | ---> | Symbol  |
   | Code (UI) |      | (Phase1)|      | (Phase2)|      | Table   |
   +-----------+      +---------+      +---------+      +---------+
                                              |
                                              v
                                       +-------------+
                                       | Parse Tree  |
                                       +-------------+
```

1. **Lexical Analysis** (`Lexer.tokenize()`) — reads source character by
   character, groups characters into lexemes, and produces a flat
   `List<Token>`.
2. **Syntax Analysis** (`Parser.parseProgram()`) — consumes the token
   list using a recursive-descent strategy with one method per grammar
   rule. On success it returns a `ParseTreeNode` root and populates the
   `SymbolTable`.
3. **GUI Update** (`CompilerGUI.runAnalysis()`) — the Swing layer
   receives the lexer output, parser output, symbol table, and parse
   tree, and re-renders the four output panels.

---

## Getting Started

### Prerequisites
- **Java JDK 8 or later** (tested on OpenJDK 17).
- No third-party libraries are required.

### Build & Run (Windows / macOS / Linux)

```bash
# 1. Move into the project folder
cd path/to/CD-project

# 2. Compile every .java file in the folder
javac *.java

# 3. Launch the GUI
java CompilerGUI
```

A window titled **"C-Subset Compiler - Lexical & Syntax Analyzer"**
will appear with a pre-loaded sample program already in the editor.

---

## Usage

1. **Type or paste** your C-subset source code in the top text area.
2. Click the green **Compile / Analyze** button on the right.
3. Switch between the tabs at the bottom of the window to inspect:
   - **Tokens** — every lexeme with its line number and category.
   - **Symbol Table** — every declared variable and function.
   - **Parse Tree** — the full grammar tree, expandable / collapsible.
4. Read the **Console / Output** panel at the bottom for `Build
   Successful` or detailed error messages.

The editor starts with a demo program that exercises most supported
constructs (functions, declarations with initializers, `if/else`,
`while`, `for`, function calls, return) so you can verify the
pipeline immediately.

---

## Error Handling

The compiler never crashes on bad input. Any failure is caught and
displayed in the console panel:

| Failure type        | Example                                | Message format |
|---------------------|----------------------------------------|----------------|
| Unknown character   | `int x = 5 @;`                         | `Unrecognised character '@' at line N` |
| Bad keyword syntax  | `int main() { int x return x; }`       | `Syntax Error at line N: Expected ';' after declaration (got 'return')` |
| Unmatched brace     | `int main() { int x = 5;`              | `Syntax Error at line N: Expected '}' to close block (got '')` |
| Empty source        | _(blank editor)_                       | `Error: Source code is empty.` |

On any failure the parse tree is reset to a single `No Tree Generated`
root, so no stale output from a prior successful run lingers on
screen.

---

## Limitations

This is an **educational** compiler. The following are intentionally
**not** supported (per the "no scope creep" rule in the spec):

- No `char`, `double`, `struct`, `union`, `enum`
- No `switch` / `case` / `do-while`
- No pointers (`*p`, `&x`)
- No preprocessor (`#include`, `#define`)
- No arrays, no multi-dimensional declarations
- No `++` / `--` / `+=` / `-=` shortcut operators
- Single shared symbol table (no nested scoping)
- No semantic analysis or code generation — analysis halts at syntax

---

## Author

Built as a Compiler Design coursework project demonstrating the
front-end phases (lexical + syntax analysis) of a C-subset compiler
with a Swing-based graphical user interface.

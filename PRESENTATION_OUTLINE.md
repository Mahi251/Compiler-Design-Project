# C-Subset Compiler — Group Presentation Outline

**Format:** 7-slide defense, 5-member team
**Recommended Time Budget:** 12–15 minutes presentation + 5 minutes Q&A
**Total Slides:** 7 (one per content area, with presenter assignments and a scripted narrative for each)

---

## Team Roles & Slide Assignments

| Member | Role                              | Owns Slides              |
|--------|-----------------------------------|--------------------------|
| M1     | Project Lead / Opening            | 1, 7 (intro + conclusion) |
| M2     | Language & Lexical Specialist     | 2, 3                      |
| M3     | Syntax & Grammar Specialist       | 4                         |
| M4     | Data-Structures Specialist        | 5                         |
| M5     | Demo Operator / Closing Backup    | 6 (live demo)             |

**Handoff protocol:** Each presenter speaks for ~90 seconds. Use a verbal cue ("…and that's the lexical pipeline. Over to M3.") to keep transitions clean.

---

## Slide 1 — Title Slide

**Visual layout (16:9, dark blue background, white text):**

```
+----------------------------------------------------------+
|                                                          |
|              C-Subset Compiler                            |
|        Lexical & Syntax Analyzer with GUI                 |
|                                                          |
|              Compiler Design — Final Project             |
|                                                          |
|        Team Members:                                     |
|           M1 — Project Lead (Lexer integration)          |
|           M2 — Lexical Analyzer design                   |
|           M3 — Parser / Grammar                          |
|           M4 — Symbol Table & Parse Tree                 |
|           M5 — GUI & Demo                                |
|                                                          |
|        Instructor: [Name]                                |
|        Date: [Date]                                      |
+----------------------------------------------------------+
```

**Script — M1 (90 seconds):**

> "Good [morning/afternoon]. We're presenting our final project for Compiler Design: a front-end compiler for a deliberately constrained subset of the C programming language, implemented in Java with a Swing-based graphical user interface.
>
> The project is organised into four logical phases — a Lexical Analyzer, a Syntax Analyzer, a Symbol Table, and a Parse Tree — all wired together by a single-window GUI. The system reads C-subset source code, breaks it into tokens, validates the token stream against a formal grammar, populates a symbol table, and renders the resulting parse tree visually.
>
> Over the next seven slides we'll walk through the language subset we chose to support, the architecture of each component, the data structures that hold compiler state, and a live demonstration. We'll close with a summary of what we learned and the kinds of questions we expect to face during the defense. The full source code, documentation, and this slide deck are included in the submission package."

---

## Slide 2 — Language Subset Definition

**Visual layout (white background, three-column infographic):**

```
+----------------------------------------------------------------+
|  WHAT OUR COMPILER ACCEPTS                                     |
|                                                                |
|  DATA TYPES       CONTROL FLOW       OPERATORS                 |
|  ----------       ------------       ---------                 |
|   int              if / else          +  -  *  /               |
|   float            while              ==  !=                   |
|   void             for                <  >  <=  >=             |
|                    return             &&  ||                   |
|                                        =  (assign)             |
|                                                                |
|  STATEMENTS                    EXAMPLES                         |
|  ----------                    --------                         |
|   int x;                       int add(int a, int b) {         |
|   x = 10;                         return a + b;                |
|   if (x > 0) { ... }           }                                |
|   while (x > 0) { ... }                                         |
|   for (int i = 0; i < n; i=i+1)                                 |
+----------------------------------------------------------------+
```

**Script — M2 (90 seconds):**

> "Before writing any code, we froze the language specification. This is a teaching compiler, so we deliberately limited scope to avoid feature creep.
>
> **Data types:** We support `int`, `float`, and `void` — that's it. No `char`, no `double`, no pointers, no structs.
>
> **Control flow:** Conditional branching with `if` and optional `else`, iteration with `while` and `for`, and function exit with `return`. We did not implement `do…while`, `switch`, `break`, or `continue` — they were out of scope.
>
> **Operators:** The full arithmetic set `+`, `-`, `*`, `/`. Relational `<`, `>`, `<=`, `>=`. Equality `==`, `!=`. Logical `&&`, `||`. And the single assignment operator `=`. Notably absent: bitwise operators, the ternary `? :`, and pre/post-increment.
>
> **Statements:** Variable declarations, assignments, function calls, return statements, the control-flow statements above, and blocks delimited by braces. A complete program is a sequence of top-level function definitions and statements.
>
> Keeping the language small gave us three benefits: a grammar that fits on one slide, a parser we can implement in roughly 600 lines of Java, and a system that is easy to defend because every construct has exactly one path through the code."

**Likely Q&A (be ready):**
* "Why no `char`?" → Out of scope; would require string-literal tokenisation rules and escape-sequence handling.
* "Why no `switch`?" → Adds non-trivial grammar (case lists, fall-through, default branch) for marginal educational value.
* "Why a subset at all?" → A real C compiler is millions of lines; a subset is the only tractable scope for a semester project while still demonstrating every front-end phase.

---

## Slide 3 — Lexical Analyzer Architecture

**Visual layout (pipeline diagram, top to bottom):**

```
   Source string
        │
        │ character-by-character
        ▼
   ┌─────────────────────────────┐
   │  LEXER.tokenize()           │
   │                             │
   │  • skip whitespace          │
   │  • readIdentifierOrKeyword  │──► reserved? → INT/FLOAT/VOID/IF/...
   │  • readNumber               │──► INT_LITERAL or FLOAT_LITERAL
   │  • readOperator/Punct       │──► 1- or 2-char operators
   │  • throw on unknown char    │──► LexerException(line, char)
   └─────────────────────────────┘
        │
        ▼
   List<Token>  +  trailing EOF
        │
        ▼
   "Tokens" JTable
   [Line | Type | Lexeme]
```

**Script — M2 (90 seconds):**

> "The Lexer is phase one. It receives the raw source string from the code area and produces a flat, ordered `List<Token>`. Each token carries three fields: the `TokenType` enum, the lexeme — the exact source substring that produced it — and the 1-based line number, which is critical for error reporting.
>
> Implementation-wise, the Lexer is a pure single-pass scanner. No regex engine — we wanted the logic to be inspectable. The state is just a position index and a line counter. The main loop dispatches on the current character: whitespace and tabs are skipped, newlines bump the line counter, letters or underscores start an identifier (or a keyword, which we detect with a `switch` over the lexeme), digits start a number (we look one character ahead to decide between `INT_LITERAL` and `FLOAT_LITERAL`), and anything else goes through a `switch` on punctuation and operators.
>
> Multi-character operators are handled with a one-character look-ahead: when we see `=`, we peek at the next char; if it's another `=`, we emit `EQUAL`; otherwise we emit `ASSIGN`. The same trick handles `!=`, `<=`, `>=`, `&&`, `||`. Importantly, the *single-character* forms of `!`, `&`, and `|` are rejected explicitly — they are not in our language, and we throw a `LexerException` with a 'did you mean `&&`?' style hint.
>
> Finally, the Lexer always appends a single `EOF` sentinel token so the Parser has a definitive end-of-input marker. Unrecognised characters like `@`, `#`, `'`, or `"` trip the `default` branch and throw a `LexerException` with the line number and the offending character — that's exactly what our Test Case 3 exercises."

**Likely Q&A (be ready):**
* "Why not use `java.util.regex`?" → Spec required strict, auditable behaviour for a known alphabet; regex would hide edge cases (e.g. a stray `=` inside an identifier) and complicate the educational walkthrough.
* "How do you handle a `1.2.3` literal?" → The number reader stops at the second `.`; the next iteration of the main loop sees `.` and throws, because `.` is not a valid punctuation token in our language.
* "What about comments?" → Out of scope; would be a `//` skip-to-end-of-line pass before the main loop, or a `/* … */` state machine.

---

## Slide 4 — Syntax Analyzer & Grammar Rules

**Visual layout (left: grammar snippet; right: parser-method correspondence):**

```
   GRAMMAR (excerpt)              PARSER METHODS
   ----------------------         ------------------------
   program → stmts EOF            parseProgram()
   stmt    → decl | assign | ...  parseStatement()  [dispatcher]
   decl    → type id (= expr)?;   parseDeclaration()
   ifStmt  → if (cond) block      parseIfStatement()
              (else block)?       parseBlock()
   expr    → logicalOr            parseExpression()
   logicalOr → logicalAnd         parseLogicalOr()
              (|| logicalAnd)*    parseLogicalAnd() ...
   primary → literal | id | (...) parsePrimary()
```

**Script — M3 (90 seconds):**

> "The Parser is phase two. It implements the grammar we showed in slide two using recursive-descent parsing: one Java method per grammar rule, and the method's structure mirrors the production's structure one-to-one. This makes the relationship between the spec and the code visually obvious — a major reason we chose recursive descent over a table-driven parser like LALR.
>
> The entry point is `parseProgram`. It loops until EOF, and at each iteration uses a non-consuming three-token look-ahead to decide whether the next item is a *function definition* — pattern `<type> IDENTIFIER (` — or a *statement*. Function definitions go to `parseFunctionDefinition`; everything else goes to `parseStatement`.
>
> `parseStatement` is itself a dispatcher: it peeks at the first token and routes to `parseDeclaration` (when it sees a type keyword), `parseReturnStatement` (when it sees `return`), `parseIfStatement`, `parseWhileStatement`, `parseForStatement`, `parseBlock` (for nested braces), or `parseAssignmentOrCall` (when it sees an identifier). The `parseAssignmentOrCall` helper does its own one-token look-ahead to choose between an assignment and a function call.
>
> Expressions are handled with a precedence-climbing cascade: `parseExpression` delegates to `parseLogicalOr`, which calls `parseLogicalAnd`, which calls `parseEquality`, and so on down to `parsePrimary`. Each level uses a `while` loop to absorb any number of operators at its own precedence, building left-associative subtrees along the way.
>
> **Error handling** is the most important part. Every place we *must* see a specific token uses a helper called `consume(expectedType, message)`. If the check fails, it throws a `SyntaxException` with the current line number, the expected token, and the lexeme we actually got. The GUI catches this in a `try/catch` and displays the message verbatim. We deliberately do not attempt error recovery — when a parse fails, the GUI clears the parse-tree panel and the symbol table so no stale state survives, and the user can correct the input and recompile.
>
> The output of a successful parse is two side-effect artifacts: a populated `SymbolTable` (built up incrementally inside `parseDeclaration` and `parseFunctionDefinition`) and a `ParseTreeNode` root, which we render into the `JTree` via `toTreeNode()`."

**Likely Q&A (be ready):**
* "Why recursive descent and not LALR(1)?" → For a subset of C with no left-recursive or ambiguous productions, recursive descent is shorter, more readable, and the call stack literally is the parse tree.
* "Is your grammar LL(1)?" → Effectively yes; we only need up to two tokens of look-ahead to disambiguate function-def-vs-declaration and assignment-vs-call.
* "Why halt on first error?" → A teaching parser prioritises a precise, single error message over a flood of cascaded errors. The spec called for halting behaviour.
* "What about left-recursion?" → The expression grammar is intentionally right-recursive in BNF form (`A → A α | β`) and converted to left-iteration in the parser (`while`), so there is no infinite recursion.

---

## Slide 5 — Data Structures (Symbol Table & Parse Tree)

**Visual layout (two side-by-side panels with diagrams):**

```
   SYMBOL TABLE                       PARSE TREE
   ----------------                   --------------------
   HashMap<String, Symbol>            ParseTreeNode
   ┌────────────────────────┐         { label, children: List }
   │ "a"     → Symbol{...}  │         
   │ "add"   → Symbol{...}  │              [Program]
   │ "b"     → Symbol{...}  │              /        \
   │ "i"     → Symbol{...}  │        [FuncDef]    [FuncDef]
   │ "main"  → Symbol{...}  │          /               \
   │ "x"     → Symbol{...}  │     [Type][id][Params]  [Block]
   │ "y"     → Symbol{...}  │                           ...
   └────────────────────────┘
   O(1) declare & lookup              Recursive:
                                      toTreeNode() → DefaultMutableTreeNode
                                      printTree(depth) → indented text
```

**Script — M4 (90 seconds):**

> "Two data structures hold all of the compiler's persistent state after a successful parse: the Symbol Table and the Parse Tree.
>
> **The Symbol Table** is implemented in `SymbolTable.java` as a `HashMap<String, Symbol>`. We chose `HashMap` for O(1) average-case insertion and lookup — critical because the Parser calls `declare` once per declaration and would, in a larger compiler, call `lookup` for every identifier reference. Each `Symbol` is a small immutable record with three fields: the name, the data type — stored as the string `'int'`, `'float'`, or `'void'` to keep the class decoupled from the Lexer — and a `SymbolType` enum with two values, `VARIABLE` and `FUNCTION`. That last distinction is what lets us later differentiate 'something I can assign to' from 'something I can call'.
>
> The Parser populates the table at exactly two points: `parseDeclaration` calls `declare` with `VARIABLE`, and `parseFunctionDefinition` calls it with `FUNCTION`. The GUI never writes to the table; it just reads `getAllSorted()` — a sorted snapshot — to populate the Symbol Table `JTable`. We deliberately do *not* flag redeclarations as errors in this version: the `HashMap.put` overwrites silently. That is a documented simplification, appropriate for an educational compiler, and it would be the first thing we'd change in a production system.
>
> **The Parse Tree** is implemented in `ParseTreeNode.java` as a recursive data type: a node has a `String label` and a `List<ParseTreeNode> children`. The Parser constructs the tree bottom-up — every `parse…` method returns a node, and parents call `addChild` to attach subtrees. The root is a `Program` node; leaves are labelled with their token text (e.g. `Literal: 42`, `Identifier: x`, `Operator: +`).
>
> Two presentation helpers make the tree useful: `toTreeNode()` recursively converts a `ParseTreeNode` into a Swing `DefaultMutableTreeNode`, which is the data type that `JTree` consumes. This keeps the compiler-side data model decoupled from the GUI toolkit. And `printTree(depth)` emits a depth-indented textual rendering, which we used extensively while debugging the parser from the command line."

**Likely Q&A (be ready):**
* "Why a flat `HashMap` and not a scoped symbol table with a stack of scopes?" → The C-subset doesn't have nested scopes that shadow names in a way that would matter for this project; the spec explicitly forbade "scope creep". A real compiler would push a new `HashMap` on block entry and pop it on exit.
* "What is `getAllSorted()` for?" → Deterministic GUI display; without sorting, the rows would shift on every recompile because `HashMap` iteration order is unspecified.
* "Could the parse tree be reused for later phases?" → Yes — an interpreter or code generator would walk this exact tree. We kept the node structure generic for that reason.

---

## Slide 6 — Live Application Demo Guide

**Visual layout (annotated screenshot of the GUI with callouts):**

```
   +---------------------------------------------------+
   |  Source Code (C-subset)        [Compile / Analyze]|
   |  +-----------------------------------------+       |
   |  |  int main() {                          |       |
   |  |      int x = 5;                        |       |
   |  |      ...                               |       |
   |  +-----------------------------------------+       |
   +---------------------------------------------------+
   | [Tokens] [Symbol Table] [Parse Tree]              |
   |  +-----------------------------------------+       |
   |  |  Line | Type       | Lexeme            |       |
   |  |  1    | INT        | int               |       |
   |  |  1    | IDENTIFIER | main              |       |
   |  |  ...                                        |       |
   |  +-----------------------------------------+       |
   +---------------------------------------------------+
   |  Console / Output                                  |
   |  Lexical analysis complete: 73 tokens generated.   |
   |  Syntax analysis complete: 7 symbols stored.       |
   |  Build Successful.                                 |
   +---------------------------------------------------+
```

**Script — M5 (90 seconds for the live demo; have the app already running):**

> "Now let me show you the system in action. The application is already open — you can see the source code in the top panel. This is the demo program that ships with the GUI; it defines a function `add` that takes two integers and returns their sum, then a `main` that exercises every control-flow construct we support.
>
> **[CLICK: Compile / Analyze]**
>
> The compile button does four things in order: it clears the console, clears the Tokens table, clears the Symbol Table, and resets the Parse Tree to a neutral state. Then it runs the Lexer.
>
> **[TAB: Tokens]** — here are 73 tokens: every keyword, identifier, literal, and operator. The line numbers are visible in column one. The trailing EOF token is hidden from this view but kept internally for the parser.
>
> **[TAB: Symbol Table]** — seven entries, sorted alphabetically: `a`, `add`, `b`, `i`, `main`, `x`, `y`. Notice the `Symbol Type` column distinguishes the two functions (`add`, `main`) from the five variables. The data type column shows the declared type.
>
> **[TAB: Parse Tree]** — the root is `Program`. Expanding it shows two function definitions. I'll expand the `main` function. You can see the parameters node, the block, and inside the block every statement type: declaration, assignment, if-statement, while-statement, for-statement, return. The tree structure mirrors the grammar exactly.
>
> **[Console]** — three lines: lexical analysis complete with token count, syntax analysis complete with symbol count, and `Build Successful.`
>
> Now let me show you what happens when the input is broken. I'll replace line 2 with one that's missing its semicolon.
>
> **[REPLACE: line 2: `int x = 5` with no semicolon, CLICK Compile]**
>
> The Tokens table still populates — the error is syntactic, not lexical. The Symbol Table stays empty because the parser threw before completing. The Parse Tree is reset to `No Tree Generated`. And the console shows the precise diagnostic: line 2, expected `;`, got `float`. That's Test Case 2.
>
> For Test Case 3, let me introduce a character that's outside our language — a `char` declaration with a single-quoted literal.
>
> **[REPLACE: line 2: `char ch = 'a';`, CLICK Compile]**
>
> The Lexer halts at the `'`, the parse tree is reset, and the console reports the lexical error with the line number and the offending character. That confirms the error-handling contract: every failure path clears stale GUI state."

**Demo contingency plan:** If the projector fails, the same three test cases are written into the documentation (Section 4) and can be narrated verbally with screenshots.

---

## Slide 7 — Conclusion & Q&A Preparation

**Visual layout (two columns — left: takeaways, right: anticipated questions):**

```
   KEY LEARNING OUTCOMES              ANTICIPATED QUESTIONS
   ------------------------            --------------------------------
   • Front-end compiler               "Why no semantic analysis?"
     workflow: lex → parse →          "Why halt on first error?"
     symbol table → parse tree.       "How would you add arrays?"
   • Recursive-descent design         "How would you extend to
     mirrors the grammar 1-to-1.        a real C subset?"
   • Side-effect ordering matters     "Why HashMap and not a tree?"
     (parseDeclaration must come      "Could this scale to a real
     before parseAssignment).            compiler?"
   • Error messages must carry        "What is the time complexity
     line, expected, got.                of the parser?"
   • GUI state must be reset on       "How do you test a parser?"
     every run, not just on success.
   • Strict scope discipline
     enables a tractable, defensible
     implementation.
```

**Script — M1 (90 seconds, then open the floor):**

> "To wrap up, here are the key takeaways from this project.
>
> First, the front-end of a compiler is a *pipeline*: raw text becomes a token stream, the token stream becomes a parse tree, and side artifacts — the symbol table, in our case — are accumulated along the way. Each phase has a single responsibility, and a clean boundary between phases.
>
> Second, recursive-descent parsing gives an almost one-to-one mapping from grammar rule to Java method. That mapping is the single most important design decision we made, because it makes the code auditable: a reviewer can read the grammar on one page and the parser on the facing page and verify they match.
>
> Third, error messages have to be *informative*. Every `consume` call we wrote embeds the line number, what we expected, and what we got. Without that information, debugging a parse error is guesswork. And every error path resets the GUI so no stale state survives — that was the lesson from `update.txt`.
>
> Fourth, scope discipline is everything. We resisted adding `char`, `double`, pointers, `switch`, and preprocessor directives. That resistance is what made the project completable, testable, and defensible in 15 minutes.
>
> Finally, the data structures we chose — `HashMap` for the symbol table and a recursive `ParseTreeNode` for the tree — were deliberately chosen to be both performant and pedagogically transparent.
>
> We're now ready for questions. We expect you might ask about semantic analysis, why we halt on first error, how we'd extend this to a larger language, the time complexity of the parser, our testing strategy, or what changes we'd make for a production compiler. We're happy to take any of those, or any other questions."

**Handoff back to team:** "If I don't know the answer, I'll defer to M2/M3/M4/M5 as appropriate."

---

## Pre-Presentation Checklist

* [ ] All five team members have rehearsed their slides at least twice.
* [ ] The GUI is open on the demo machine with the pre-populated Test Case 1 program.
* [ ] The three test cases from §4 of the documentation are ready to paste into the code area in order.
* [ ] The `DOCUMENTATION.md` file is open in a browser tab as backup for any Q&A reference.
* [ ] The build command (`javac *.java && java CompilerGUI`) is in everyone's notes in case the demo needs to be restarted.
* [ ] One team member is designated to advance slides; another to drive the demo; they should not be the same person.
* [ ] Time budget: 90 seconds per slide × 7 = ~10.5 minutes; leave 2–4 minutes for transitions and 5 minutes for Q&A.

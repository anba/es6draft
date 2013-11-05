/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import static com.github.anba.es6draft.semantics.StaticSemantics.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.*;

import com.github.anba.es6draft.ast.AbruptNode.Abrupt;
import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.MethodDefinition.MethodType;
import com.github.anba.es6draft.parser.ParserException.ExceptionType;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.SmallArrayList;
import com.github.anba.es6draft.runtime.objects.FunctionPrototype;

/**
 * Parser for ECMAScript6 source code
 * <ul>
 * <li>12 ECMAScript Language: Expressions
 * <li>13 ECMAScript Language: Statements and Declarations
 * <li>14 ECMAScript Language: Functions and Classes
 * <li>15 ECMAScript Language: Scripts and Modules
 * </ul>
 */
public class Parser {
    private static final boolean MODULES_ENABLED = false;
    private static final boolean DEBUG = false;

    private static final int MAX_ARGUMENTS = FunctionPrototype.getMaxArguments();
    private static final List<Binding> NO_INHERITED_BINDING = Collections.emptyList();
    private static final Set<String> EMPTY_LABEL_SET = Collections.emptySet();

    private final String sourceFile;
    private final int sourceLine;
    private final EnumSet<Option> options;
    private TokenStream ts;
    private ParseContext context;

    private enum StrictMode {
        Unknown, Strict, NonStrict
    }

    private enum StatementType {
        Iteration, Breakable, Statement
    }

    private enum ContextKind {
        Script, Module, Function, Generator, ArrowFunction, Method
    }

    private static class ParseContext {
        final ParseContext parent;
        final ContextKind kind;

        boolean superReference = false;
        boolean yieldAllowed = false;
        boolean returnAllowed = false;

        StrictMode strictMode = StrictMode.Unknown;
        boolean explicitStrict = false;
        ParserException strictError = null;
        List<FunctionNode> deferred = null;
        ArrayDeque<ObjectLiteral> objectLiterals = null;

        Map<String, LabelContext> labelSet = null;
        LabelContext labels = null;

        ScopeContext scopeContext;
        final FunctionContext funContext;

        ParseContext() {
            this.parent = null;
            this.kind = null;
            this.funContext = null;
        }

        ParseContext(ParseContext parent, ContextKind kind) {
            this.parent = parent;
            this.kind = kind;
            this.funContext = new FunctionContext(this);
            this.scopeContext = funContext;
            this.returnAllowed = isFunction();
            if (parent.strictMode == StrictMode.Strict) {
                this.strictMode = parent.strictMode;
            }
        }

        ParseContext findSuperContext() {
            ParseContext cx = this;
            while (cx.kind == ContextKind.ArrowFunction) {
                cx = cx.parent;
            }
            return cx;
        }

        void setReferencesSuper() {
            superReference = true;
        }

        boolean hasSuperReference() {
            return superReference;
        }

        final boolean isFunction() {
            switch (kind) {
            case ArrowFunction:
            case Function:
            case Generator:
            case Method:
                return true;
            case Module:
            case Script:
            default:
                return false;
            }
        }

        int countLiterals() {
            return (objectLiterals != null ? objectLiterals.size() : 0);
        }

        void addLiteral(ObjectLiteral object) {
            if (objectLiterals == null) {
                objectLiterals = new ArrayDeque<>(4);
            }
            objectLiterals.push(object);
        }

        void removeLiteral(ObjectLiteral object) {
            objectLiterals.removeFirstOccurrence(object);
        }
    }

    // TODO: rename - not used exclusively for functions, also used for scripts and modules
    private static class FunctionContext extends ScopeContext implements FunctionScope {
        final ScopeContext enclosing;
        HashSet<String> parameterNames = null;
        boolean directEval = false;

        FunctionContext(ParseContext context) {
            super(null);
            this.enclosing = context.parent.scopeContext;
        }

        private boolean isStrict() {
            if (node instanceof FunctionNode) {
                return IsStrict((FunctionNode) node);
            } else {
                assert node instanceof Script;
                return IsStrict((Script) node);
            }
        }

        @Override
        public ScopeContext getEnclosingScope() {
            return enclosing;
        }

        @Override
        public boolean isDynamic() {
            return directEval && !isStrict();
        }

        @Override
        public Set<String> parameterNames() {
            return parameterNames;
        }

        @Override
        public Set<String> lexicallyDeclaredNames() {
            return lexDeclaredNames;
        }

        @Override
        public List<Declaration> lexicallyScopedDeclarations() {
            return lexScopedDeclarations;
        }

        @Override
        public Set<String> varDeclaredNames() {
            return varDeclaredNames;
        }

        @Override
        public List<StatementListItem> varScopedDeclarations() {
            return varScopedDeclarations;
        }
    }

    private static class BlockContext extends ScopeContext implements BlockScope {
        final boolean dynamic;

        BlockContext(ScopeContext parent, boolean dynamic) {
            super(parent);
            this.dynamic = dynamic;
        }

        @Override
        public Set<String> lexicallyDeclaredNames() {
            return lexDeclaredNames;
        }

        @Override
        public List<Declaration> lexicallyScopedDeclarations() {
            return lexScopedDeclarations;
        }

        @Override
        public boolean isDynamic() {
            return dynamic;
        }
    }

    private abstract static class ScopeContext implements Scope {
        final ScopeContext parent;
        ScopedNode node = null;

        HashSet<String> varDeclaredNames = null;
        HashSet<String> lexDeclaredNames = null;
        List<StatementListItem> varScopedDeclarations = null;
        List<Declaration> lexScopedDeclarations = null;

        ScopeContext(ScopeContext parent) {
            this.parent = parent;
        }

        @Override
        public Scope getParent() {
            return parent;
        }

        @Override
        public ScopedNode getNode() {
            return node;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("var: ").append(varDeclaredNames != null ? varDeclaredNames : "<null>");
            sb.append("\t");
            sb.append("lex: ").append(lexDeclaredNames != null ? lexDeclaredNames : "<null>");
            return sb.toString();
        }

        boolean isTopLevel() {
            return (parent == null);
        }

        boolean addVarDeclaredName(String name) {
            if (varDeclaredNames == null) {
                varDeclaredNames = new HashSet<>();
            }
            varDeclaredNames.add(name);
            return (lexDeclaredNames == null || !lexDeclaredNames.contains(name));
        }

        boolean addLexDeclaredName(String name) {
            if (lexDeclaredNames == null) {
                lexDeclaredNames = new HashSet<>();
            }
            return lexDeclaredNames.add(name)
                    && (varDeclaredNames == null || !varDeclaredNames.contains(name));
        }

        void addVarScopedDeclaration(StatementListItem decl) {
            if (varScopedDeclarations == null) {
                varScopedDeclarations = newSmallList();
            }
            varScopedDeclarations.add(decl);
        }

        void addLexScopedDeclaration(Declaration decl) {
            if (lexScopedDeclarations == null) {
                lexScopedDeclarations = newSmallList();
            }
            lexScopedDeclarations.add(decl);
        }
    }

    private static class LabelContext {
        final LabelContext parent;
        final StatementType type;
        final Set<String> labelSet;
        final EnumSet<Abrupt> abrupts = EnumSet.noneOf(Abrupt.class);

        LabelContext(LabelContext parent, StatementType type, Set<String> labelSet) {
            this.parent = parent;
            this.type = type;
            this.labelSet = labelSet;
        }

        void mark(Abrupt abrupt) {
            abrupts.add(abrupt);
        }
    }

    @SuppressWarnings("serial")
    private static class RetryGenerator extends RuntimeException {
    }

    public enum Option {
        Strict, FunctionCode, LocalScope, DirectEval, EvalScript, EnclosedByWithStatement,

        /** B.1.1 Numeric Literals */
        LegacyOctalIntegerLiteral,

        /** B.1.2 String Literals */
        OctalEscapeSequence,

        /** B.1.3 HTML-like Comments */
        HTMLComments,

        /** Moz-Extension: for-each statement */
        ForEachStatement,

        /** Moz-Extension: guarded catch */
        GuardedCatch,

        /** Moz-Extension: expression closure */
        ExpressionClosure,

        /** Moz-Extension: let statement */
        LetStatement,

        /** Moz-Extension: let expression */
        LetExpression,

        /** Moz-Extension: legacy (star-less) generators */
        LegacyGenerator,

        /** Moz-Extension: legacy comprehension forms */
        LegacyComprehension;

        public static EnumSet<Option> from(Set<CompatibilityOption> compatOptions) {
            EnumSet<Option> options = EnumSet.noneOf(Option.class);
            if (compatOptions.contains(CompatibilityOption.LegacyOctalIntegerLiteral)) {
                options.add(Option.LegacyOctalIntegerLiteral);
            }
            if (compatOptions.contains(CompatibilityOption.OctalEscapeSequence)) {
                options.add(Option.OctalEscapeSequence);
            }
            if (compatOptions.contains(CompatibilityOption.HTMLComments)) {
                options.add(Option.HTMLComments);
            }
            if (compatOptions.contains(CompatibilityOption.ForEachStatement)) {
                options.add(Option.ForEachStatement);
            }
            if (compatOptions.contains(CompatibilityOption.GuardedCatch)) {
                options.add(Option.GuardedCatch);
            }
            if (compatOptions.contains(CompatibilityOption.ExpressionClosure)) {
                options.add(Option.ExpressionClosure);
            }
            if (compatOptions.contains(CompatibilityOption.LetStatement)) {
                options.add(Option.LetStatement);
            }
            if (compatOptions.contains(CompatibilityOption.LetExpression)) {
                options.add(Option.LetExpression);
            }
            if (compatOptions.contains(CompatibilityOption.LegacyGenerator)) {
                options.add(Option.LegacyGenerator);
            }
            if (compatOptions.contains(CompatibilityOption.LegacyComprehension)) {
                options.add(Option.LegacyComprehension);
            }
            return options;
        }
    }

    public Parser(String sourceFile, int sourceLine, Set<Option> options) {
        this.sourceFile = sourceFile;
        this.sourceLine = sourceLine;
        this.options = EnumSet.copyOf(options);
        context = new ParseContext();
        context.strictMode = this.options.contains(Option.Strict) ? StrictMode.Strict
                : StrictMode.NonStrict;
    }

    String getSourceFile() {
        return sourceFile;
    }

    int getSourceLine() {
        return sourceLine;
    }

    boolean isEnabled(Option option) {
        return options.contains(option);
    }

    private ParseContext newContext(ContextKind kind) {
        return context = new ParseContext(context, kind);
    }

    private ParseContext restoreContext() {
        if (context.parent.strictError == null) {
            context.parent.strictError = context.strictError;
        }
        return context = context.parent;
    }

    private BlockContext enterWithContext() {
        BlockContext cx = new BlockContext(context.scopeContext, true);
        context.scopeContext = cx;
        return cx;
    }

    private ScopeContext exitWithContext() {
        return exitScopeContext();
    }

    private BlockContext enterBlockContext() {
        BlockContext cx = new BlockContext(context.scopeContext, false);
        context.scopeContext = cx;
        return cx;
    }

    private BlockContext reenterBlockContext(BlockContext cx) {
        context.scopeContext = cx;
        return cx;
    }

    private ScopeContext exitBlockContext() {
        return exitScopeContext();
    }

    private ScopeContext exitScopeContext() {
        ScopeContext scope = context.scopeContext;
        ScopeContext parent = scope.parent;
        assert parent != null : "exitScopeContext() on top-level";
        HashSet<String> varDeclaredNames = scope.varDeclaredNames;
        if (varDeclaredNames != null) {
            scope.varDeclaredNames = null;
            for (String name : varDeclaredNames) {
                addVarDeclaredName(parent, name);
            }
        }
        return context.scopeContext = parent;
    }

    private void addFunctionDeclaration(FunctionDeclaration decl) {
        String name = BoundName(decl.getIdentifier());
        ScopeContext parentScope = context.parent.scopeContext;
        if (parentScope.isTopLevel()) {
            // top-level function declaration
            parentScope.addVarScopedDeclaration(decl);
            if (!parentScope.addVarDeclaredName(name)) {
                reportSyntaxError(decl, Messages.Key.VariableRedeclaration, name);
            }
        } else {
            // block-scoped function declaration
            parentScope.addLexScopedDeclaration(decl);
            if (!parentScope.addLexDeclaredName(name)) {
                reportSyntaxError(decl, Messages.Key.VariableRedeclaration, name);
            }
        }
    }

    private void addGeneratorDeclaration(GeneratorDeclaration decl) {
        String name = BoundName(decl.getIdentifier());
        ScopeContext parentScope = context.parent.scopeContext;
        parentScope.addLexScopedDeclaration(decl);
        if (!parentScope.addLexDeclaredName(name)) {
            reportSyntaxError(decl, Messages.Key.VariableRedeclaration, name);
        }
    }

    private void addLexScopedDeclaration(Declaration decl) {
        context.scopeContext.addLexScopedDeclaration(decl);
    }

    private void addVarScopedDeclaration(VariableStatement decl) {
        context.funContext.addVarScopedDeclaration(decl);
    }

    private void addVarDeclaredName(ScopeContext scope, String name) {
        if (!scope.addVarDeclaredName(name)) {
            // FIXME: provide correct line/source information
            reportSyntaxError(Messages.Key.VariableRedeclaration, name);
        }
    }

    private void addVarDeclaredName(ScopeContext scope, Binding binding, String name) {
        if (!scope.addVarDeclaredName(name)) {
            reportSyntaxError(binding, Messages.Key.VariableRedeclaration, name);
        }
    }

    private void addLexDeclaredName(ScopeContext scope, Binding binding, String name) {
        if (!scope.addLexDeclaredName(name)) {
            reportSyntaxError(binding, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * <strong>[13.1] Block</strong>
     * <p>
     * Static Semantics: Early Errors<br>
     * <ul>
     * <li>It is a Syntax Error if any element of the LexicallyDeclaredNames of StatementList also
     * occurs in the VarDeclaredNames of StatementList.
     * </ul>
     */
    @SuppressWarnings("unused")
    private void addVarDeclaredName(Binding binding) {
        if (binding instanceof BindingIdentifier) {
            addVarDeclaredName((BindingIdentifier) binding);
        } else {
            assert binding instanceof BindingPattern;
            addVarDeclaredName((BindingPattern) binding);
        }
    }

    private void addVarDeclaredName(BindingIdentifier bindingIdentifier) {
        String name = BoundName(bindingIdentifier);
        addVarDeclaredName(context.scopeContext, bindingIdentifier, name);
    }

    private void addVarDeclaredName(BindingPattern bindingPattern) {
        for (String name : BoundNames(bindingPattern)) {
            addVarDeclaredName(context.scopeContext, bindingPattern, name);
        }
    }

    /**
     * <strong>[13.1] Block</strong>
     * <p>
     * Static Semantics: Early Errors<br>
     * <ul>
     * <li>It is a Syntax Error if the LexicallyDeclaredNames of StatementList contains any
     * duplicate entries.
     * <li>It is a Syntax Error if any element of the LexicallyDeclaredNames of StatementList also
     * occurs in the VarDeclaredNames of StatementList.
     * </ul>
     */
    private void addLexDeclaredName(Binding binding) {
        if (binding instanceof BindingIdentifier) {
            addLexDeclaredName((BindingIdentifier) binding);
        } else {
            assert binding instanceof BindingPattern;
            addLexDeclaredName((BindingPattern) binding);
        }
    }

    private void addLexDeclaredName(BindingIdentifier bindingIdentifier) {
        String name = BoundName(bindingIdentifier);
        addLexDeclaredName(context.scopeContext, bindingIdentifier, name);
    }

    private void addLexDeclaredName(BindingPattern bindingPattern) {
        for (String name : BoundNames(bindingPattern)) {
            addLexDeclaredName(context.scopeContext, bindingPattern, name);
        }
    }

    private void addLexDeclaredNames(List<Binding> bindings) {
        for (Binding binding : bindings) {
            addLexDeclaredName(binding);
        }
    }

    private void removeLexDeclaredNames(List<Binding> bindings) {
        for (Binding binding : bindings) {
            removeLexDeclaredName(binding);
        }
    }

    private void removeLexDeclaredName(Binding binding) {
        HashSet<String> lexDeclaredNames = context.scopeContext.lexDeclaredNames;
        if (binding instanceof BindingIdentifier) {
            BindingIdentifier bindingIdentifier = (BindingIdentifier) binding;
            String name = BoundName(bindingIdentifier);
            lexDeclaredNames.remove(name);
        } else {
            assert binding instanceof BindingPattern;
            BindingPattern bindingPattern = (BindingPattern) binding;
            for (String name : BoundNames(bindingPattern)) {
                lexDeclaredNames.remove(name);
            }
        }
    }

    private LabelContext enterLabelled(long sourcePosition, StatementType type, Set<String> labelSet) {
        LabelContext cx = context.labels = new LabelContext(context.labels, type, labelSet);
        if (!labelSet.isEmpty() && context.labelSet == null) {
            context.labelSet = new HashMap<>();
        }
        for (String label : labelSet) {
            if (context.labelSet.containsKey(label)) {
                reportSyntaxError(sourcePosition, Messages.Key.DuplicateLabel, label);
            }
            context.labelSet.put(label, cx);
        }
        return cx;
    }

    private LabelContext exitLabelled() {
        for (String label : context.labels.labelSet) {
            context.labelSet.remove(label);
        }
        return context.labels = context.labels.parent;
    }

    private LabelContext enterIteration(long sourcePosition, Set<String> labelSet) {
        return enterLabelled(sourcePosition, StatementType.Iteration, labelSet);
    }

    private void exitIteration() {
        exitLabelled();
    }

    private LabelContext enterBreakable(long sourcePosition, Set<String> labelSet) {
        return enterLabelled(sourcePosition, StatementType.Breakable, labelSet);
    }

    private void exitBreakable() {
        exitLabelled();
    }

    private LabelContext findContinueTarget(String label) {
        for (LabelContext cx = context.labels; cx != null; cx = cx.parent) {
            if (label == null ? cx.type == StatementType.Iteration : cx.labelSet.contains(label)) {
                return cx;
            }
        }
        return null;
    }

    private LabelContext findBreakTarget(String label) {
        for (LabelContext cx = context.labels; cx != null; cx = cx.parent) {
            if (label == null ? cx.type != StatementType.Statement : cx.labelSet.contains(label)) {
                return cx;
            }
        }
        return null;
    }

    private static <T> List<T> newSmallList() {
        return new SmallArrayList<>();
    }

    private static <T> List<T> newList() {
        return new SmallArrayList<>();
    }

    private static <T> List<T> merge(List<T> list1, List<T> list2) {
        if (!(list1.isEmpty() || list2.isEmpty())) {
            List<T> merged = new ArrayList<>();
            merged.addAll(list1);
            merged.addAll(list2);
            return merged;
        }
        return list1.isEmpty() ? list2 : list1;
    }

    private static int toLine(long sourcePosition) {
        return (int) sourcePosition;
    }

    private static int toColumn(long sourcePosition) {
        return (int) (sourcePosition >>> 32);
    }

    private long beginSource() {
        // make columns 1-indexed
        return ((long) 1 << 32) | getSourceLine();
    }

    private ParserException reportException(ParserException exception) {
        throw exception;
    }

    /**
     * Report mismatched token error from tokenstream's current position
     */
    private ParserException reportTokenMismatch(Token expected, Token actual) {
        long sourcePosition = ts.sourcePosition();
        int line = toLine(sourcePosition), col = toColumn(sourcePosition);
        if (actual == Token.EOF) {
            throw new ParserEOFException(sourceFile, line, col, Messages.Key.UnexpectedToken,
                    actual.toString(), expected.toString());
        }
        throw new ParserException(ExceptionType.SyntaxError, sourceFile, line, col,
                Messages.Key.UnexpectedToken, actual.toString(), expected.toString());
    }

    /**
     * Report mismatched token error from tokenstream's current position
     */
    private ParserException reportTokenMismatch(String expected, Token actual) {
        long sourcePosition = ts.sourcePosition();
        int line = toLine(sourcePosition), col = toColumn(sourcePosition);
        if (actual == Token.EOF) {
            throw new ParserEOFException(sourceFile, line, col, Messages.Key.UnexpectedToken,
                    actual.toString(), expected);
        }
        throw new ParserException(ExceptionType.SyntaxError, sourceFile, line, col,
                Messages.Key.UnexpectedToken, actual.toString(), expected);
    }

    /**
     * Report parser error with the given type and position
     */
    private ParserException reportError(ExceptionType type, long sourcePosition,
            Messages.Key messageKey, String... args) {
        int line = toLine(sourcePosition), column = toColumn(sourcePosition);
        throw new ParserException(type, sourceFile, line, column, messageKey, args);
    }

    /**
     * Report syntax error from the given position
     */
    private ParserException reportSyntaxError(long sourcePosition, Messages.Key messageKey,
            String... args) {
        throw reportError(ExceptionType.SyntaxError, sourcePosition, messageKey, args);
    }

    /**
     * Report syntax error from the node's begin source-position
     */
    private ParserException reportSyntaxError(Node node, Messages.Key messageKey, String... args) {
        throw reportSyntaxError(node.getBeginPosition(), messageKey, args);
    }

    /**
     * Report syntax error from tokenstream's current position
     */
    private ParserException reportSyntaxError(Messages.Key messageKey, String... args) {
        throw reportSyntaxError(ts.sourcePosition(), messageKey, args);
    }

    /**
     * Report (or store) strict-mode parser error with the given type and position
     */
    private void reportStrictModeError(ExceptionType type, long sourcePosition,
            Messages.Key messageKey, String... args) {
        if (context.strictMode == StrictMode.Unknown) {
            if (context.strictError == null) {
                int line = toLine(sourcePosition), column = toColumn(sourcePosition);
                context.strictError = new ParserException(type, sourceFile, line, column,
                        messageKey, args);
            }
        } else if (context.strictMode == StrictMode.Strict) {
            reportError(type, sourcePosition, messageKey, args);
        }
    }

    /**
     * Report (or store) strict-mode syntax error from the given position
     */
    private void reportStrictModeSyntaxError(long sourcePosition, Messages.Key messageKey,
            String... args) {
        reportStrictModeError(ExceptionType.SyntaxError, sourcePosition, messageKey, args);
    }

    /**
     * Report (or store) strict-mode syntax error from the node's source-position
     */
    private void reportStrictModeSyntaxError(Node node, Messages.Key messageKey, String... args) {
        reportStrictModeSyntaxError(node.getBeginPosition(), messageKey, args);
    }

    /**
     * Report (or store) strict-mode syntax error from tokenstream's current position
     */
    void reportStrictModeSyntaxError(Messages.Key messageKey, String... args) {
        reportStrictModeSyntaxError(ts.sourcePosition(), messageKey, args);
    }

    /**
     * Peeks the next token in the token-stream
     */
    private Token peek() {
        return ts.peekToken();
    }

    /**
     * Checks whether the next token in the token-stream is equal to the input token
     */
    private boolean LOOKAHEAD(Token token) {
        return ts.peekToken() == token;
    }

    /**
     * Returns the current token in the token-stream
     */
    private Token token() {
        return ts.currentToken();
    }

    /**
     * Consumes the current token in the token-stream and advances the stream to the next token
     */
    private void consume(Token tok) {
        if (tok != token())
            reportTokenMismatch(tok, token());
        Token next = ts.nextToken();
        if (DEBUG)
            System.out.printf("consume(%s) -> %s\n", tok, next);
    }

    /**
     * Consumes the current token in the token-stream and advances the stream to the next token
     */
    private void consume(String name) {
        long sourcePos = ts.sourcePosition();
        String string = ts.getString();
        consume(Token.NAME);
        if (!name.equals(string))
            reportSyntaxError(sourcePos, Messages.Key.UnexpectedName, string, name);
    }

    public Script parseScript(CharSequence source) throws ParserException {
        if (ts != null)
            throw new IllegalStateException();
        ts = new TokenStream(this, new TokenStreamInput(source));
        return script();
    }

    public ModuleDeclaration parseModule(CharSequence source) throws ParserException {
        if (ts != null)
            throw new IllegalStateException();

        module();

        return null;
    }

    public FunctionDefinition parseFunction(CharSequence formals, CharSequence bodyText)
            throws ParserException {
        if (ts != null)
            throw new IllegalStateException();

        newContext(ContextKind.Script);
        try {
            applyStrictMode(false);

            FunctionExpression function;
            newContext(ContextKind.Function);
            try {
                ts = new TokenStream(this, new TokenStreamInput(formals)).initialise();
                FormalParameterList parameters = formalParameters(Token.EOF);
                if (token() != Token.EOF) {
                    reportSyntaxError(Messages.Key.InvalidFormalParameterList);
                }
                if (ts.position() != formals.length()) {
                    // more input after last token (whitespace, comments), add newlines to handle
                    // last token is single-line comment case
                    formals = "\n" + formals + "\n";
                }

                ts = new TokenStream(this, new TokenStreamInput(bodyText)).initialise();
                List<StatementListItem> statements = functionBody(Token.EOF);
                if (token() != Token.EOF) {
                    reportSyntaxError(Messages.Key.InvalidFunctionBody);
                }

                String header = String.format("function anonymous (%s) ", formals);
                String body = String.format("\n%s\n", bodyText);

                FunctionContext scope = context.funContext;
                function = new FunctionExpression(beginSource(), ts.endPosition(), scope,
                        "anonymous", parameters, statements, header, body);
                scope.node = function;

                function_StaticSemantics(function);

                function = inheritStrictness(function);
            } catch (RetryGenerator e) {
                // don't bother with legacy support here
                throw reportSyntaxError(Messages.Key.InvalidYieldStatement);
            } finally {
                restoreContext();
            }

            createScript(new ExpressionStatement(function.getBeginPosition(),
                    function.getEndPosition(), function));

            return function;
        } finally {
            restoreContext();
        }
    }

    public GeneratorDefinition parseGenerator(CharSequence formals, CharSequence bodyText)
            throws ParserException {
        if (ts != null)
            throw new IllegalStateException();

        newContext(ContextKind.Script);
        try {
            applyStrictMode(false);

            GeneratorExpression generator;
            newContext(ContextKind.Generator);
            try {
                ts = new TokenStream(this, new TokenStreamInput(formals)).initialise();
                FormalParameterList parameters = formalParameters(Token.EOF);
                if (token() != Token.EOF) {
                    reportSyntaxError(Messages.Key.InvalidFormalParameterList);
                }
                if (ts.position() != formals.length()) {
                    // more input after last token (whitespace, comments), add newlines to handle
                    // last token is single-line comment case
                    formals = "\n" + formals + "\n";
                }

                ts = new TokenStream(this, new TokenStreamInput(bodyText)).initialise();
                List<StatementListItem> statements = functionBody(Token.EOF);
                if (token() != Token.EOF) {
                    reportSyntaxError(Messages.Key.InvalidFunctionBody);
                }

                String header = String.format("function* anonymous (%s) ", formals);
                String body = String.format("\n%s\n", bodyText);

                FunctionContext scope = context.funContext;
                generator = new GeneratorExpression(beginSource(), ts.endPosition(), scope,
                        "anonymous", parameters, statements, header, body);
                scope.node = generator;

                generator_StaticSemantics(generator);

                generator = inheritStrictness(generator);
            } finally {
                restoreContext();
            }

            createScript(new ExpressionStatement(generator.getBeginPosition(),
                    generator.getEndPosition(), generator));

            return generator;
        } finally {
            restoreContext();
        }
    }

    private Script createScript(StatementListItem statement) {
        List<StatementListItem> statements = singletonList(statement);
        boolean strict = (context.strictMode == StrictMode.Strict);

        FunctionContext scope = context.funContext;
        Script script = new Script(beginSource(), ts.endPosition(), sourceFile, scope, statements,
                options, strict);
        scope.node = script;

        return script;
    }

    /* ***************************************************************************************** */

    /**
     * <strong>[15.2] Scripts</strong>
     * 
     * <pre>
     * Script :
     *     ScriptBody<sub>opt</sub>
     * ScriptBody :
     *     ScriptItemList
     * </pre>
     */
    private Script script() {
        newContext(ContextKind.Script);
        try {
            ts.initialise();
            List<StatementListItem> prologue = directivePrologue();
            List<StatementListItem> body = scriptItemList();
            List<StatementListItem> statements = merge(prologue, body);
            boolean strict = (context.strictMode == StrictMode.Strict);

            FunctionContext scope = context.funContext;
            Script script = new Script(beginSource(), ts.endPosition(), sourceFile, scope,
                    statements, options, strict);
            scope.node = script;

            return script;
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[15.2] Scripts</strong>
     * 
     * <pre>
     * ScriptItemList :
     *     ScriptItem
     *     ScriptItemList ScriptItem
     * ScriptItem :
     *     ModuleDeclaration
     *     ImportDeclaration
     *     StatementListItem
     * </pre>
     */
    private List<StatementListItem> scriptItemList() {
        List<StatementListItem> list = newList();
        while (token() != Token.EOF) {
            if (MODULES_ENABLED) {
                // TODO: implement modules
                if (token() == Token.IMPORT) {
                    importDeclaration();
                } else if (isName("module") && (peek() == Token.STRING || isIdentifier(peek()))
                        && !ts.hasNextLineTerminator()) {
                    moduleDeclaration();
                } else {
                    list.add(statementListItem());
                }
            } else {
                list.add(statementListItem());
            }
        }
        return list;
    }

    /**
     * <strong>[15.1] Modules</strong>
     * 
     * <pre>
     * Module :
     *     ModuleBody<sub>opt</sub>
     * ModuleBody :
     *     ModuleItemList
     * </pre>
     */
    private void module() {
        moduleItemList();
    }

    /**
     * <strong>[15.1] Modules</strong>
     * 
     * <pre>
     * ModuleItemList :
     *     ModuleItem
     *     ModuleItemList  ModuleItem
     * ModuleItem :
     *     ExportDeclaration
     *     ScriptItem
     * ScriptItem :
     *     ModuleDeclaration
     *     ImportDeclaration
     *     StatementListItem
     * </pre>
     */
    private void moduleItemList() {
        while (token() != Token.EOF) {
            if (token() == Token.EXPORT) {
                exportDeclaration();
            } else if (token() == Token.IMPORT) {
                importDeclaration();
            } else if (isName("module") && (peek() == Token.STRING || isIdentifier(peek()))
                    && !ts.hasNextLineTerminator()) {
                moduleDeclaration();
            } else {
                statementListItem();
            }
        }
    }

    /**
     * <strong>[15.1.1] Imports</strong>
     * 
     * <pre>
     * ModuleDeclaration :
     *     module [no <i>LineTerminator</i> here] ImportedBinding FromClause ;
     * </pre>
     */
    private void moduleDeclaration() {
        consume("module");
        if (!noLineTerminator()) {
            reportSyntaxError(Messages.Key.UnexpectedEndOfLine);
        }
        importedBinding();
        fromClause();
        semicolon();
    }

    /**
     * <strong>[15.1.1] Imports</strong>
     * 
     * <pre>
     * FromClause :
     *     from ModuleSpecifier
     * </pre>
     */
    private void fromClause() {
        consume("from");
        moduleSpecifier();
    }

    /**
     * <strong>[15.1.1] Imports</strong>
     * 
     * <pre>
     * ImportDeclaration :
     *     import ImportClause FromClause ;
     *     import ModuleSpecifier ;
     * </pre>
     */
    private void importDeclaration() {
        consume(Token.IMPORT);
        if (token() != Token.STRING) {
            importClause();
            fromClause();
            semicolon();
        } else {
            moduleSpecifier();
            semicolon();
        }
    }

    /**
     * <strong>[15.1.1] Imports</strong>
     * 
     * <pre>
     * ImportClause :
     *     ImportedBinding 
     *     { } 
     *     { ImportsList }
     *     { ImportsList , }
     * ImportsList :
     *     ImportSpecifier
     *     ImportsList , ImportSpecifier
     * </pre>
     */
    private void importClause() {
        if (token() != Token.LC) {
            importedBinding();
        } else {
            consume(Token.LC);
            while (token() != Token.RC) {
                importSpecifier();
                if (token() == Token.COMMA) {
                    consume(Token.COMMA);
                } else {
                    break;
                }
            }
            consume(Token.RC);
        }
    }

    /**
     * <strong>[15.1.1] Imports</strong>
     * 
     * <pre>
     * ImportSpecifier :
     *     ImportedBinding
     *     IdentifierName as ImportedBinding
     * </pre>
     */
    private ImportSpecifier importSpecifier() {
        assert context.strictMode != StrictMode.Unknown : "undefined strict-mode in import specifier";
        long begin = ts.beginPosition();
        String externalName, localName;
        if (!isReservedWord(token())) {
            externalName = importedBinding().getName();
            if (isName("as")) {
                consume("as");
                localName = importedBinding().getName();
            } else {
                localName = externalName;
            }
        } else {
            externalName = identifierName();
            consume("as");
            localName = importedBinding().getName();
        }
        return new ImportSpecifier(begin, ts.endPosition(), externalName, localName);
    }

    /**
     * <strong>[15.1.1] Imports</strong>
     * 
     * <pre>
     * ModuleSpecifier :
     *     StringLiteral
     * </pre>
     */
    private String moduleSpecifier() {
        return stringLiteral();
    }

    /**
     * <strong>[15.1.1] Imports</strong>
     * 
     * <pre>
     * ImportedBinding :
     *     BindingIdentifier
     * </pre>
     */
    private BindingIdentifier importedBinding() {
        return bindingIdentifier();
    }

    /**
     * <strong>[15.1.2] Exports</strong>
     * 
     * <pre>
     * ExportDeclaration :
     *     export * FromClause<sub>opt</sub> ;
     *     export ExportsClause FromClause<sub>opt</sub> ;
     *     export VariableStatement
     *     export Declaration
     *     export BindingList ;
     * </pre>
     */
    private void exportDeclaration() {
        consume(Token.EXPORT);
        switch (token()) {
        case MUL: {
            // export * FromClause<sub>opt</sub> ;
            consume(Token.MUL);
            if (isName("from")) {
                fromClause();
            }
            semicolon();
            return;
        }

        case LC: {
            // export ExportsClause FromClause<sub>opt</sub> ;
            exportsClause();
            if (isName("from")) {
                fromClause();
            }
            semicolon();
            return;
        }

        case VAR: {
            // export VariableStatement
            variableStatement();
            return;
        }

        case FUNCTION:
        case CLASS:
        case LET:
        case CONST: {
            // export Declaration
            declaration();
            return;
        }

        default: {
            // export BindingList ;
            bindingList(false, true);
            semicolon();
            return;
        }
        }
    }

    /**
     * <strong>[15.1.2] Exports</strong>
     * 
     * <pre>
     * ExportsClause :
     *     { } 
     *     { ExportsList }
     *     { ExportsList , }
     * ExportsList :
     *     ExportSpecifier
     *     ExportsList , ExportSpecifier
     * </pre>
     */
    private void exportsClause() {
        consume(Token.LC);
        while (token() != Token.RC) {
            exportSpecifier();
            if (token() == Token.COMMA) {
                consume(Token.COMMA);
            } else {
                break;
            }
        }
        consume(Token.RC);
    }

    /**
     * <strong>[15.1.2] Exports</strong>
     * 
     * <pre>
     * ExportSpecifier :
     *     IdentifierReference
     *     IdentifierReference as IdentifierName
     * </pre>
     */
    private void exportSpecifier() {
        identifier();
        if (isName("as")) {
            consume("as");
            identifierName();
        }
    }

    /**
     * <strong>[15.3] Directive Prologues and the Use Strict Directive</strong>
     * 
     * <pre>
     * DirectivePrologue :
     *   Directive<sub>opt</sub>
     * Directive:
     *   StringLiteral ;
     *   Directive StringLiteral ;
     * </pre>
     */
    private List<StatementListItem> directivePrologue() {
        List<StatementListItem> statements = newSmallList();
        boolean strict = false;
        directive: while (token() == Token.STRING) {
            long begin = ts.beginPosition();
            boolean hasEscape = ts.hasEscape(); // peek() may clear hasEscape flag
            Token next = peek();
            switch (next) {
            case SEMI:
            case RC:
            case EOF:
                break;
            default:
                if (ts.hasNextLineTerminator() && !stringLiteralFollowSetNextLine(next)) {
                    break;
                }
                break directive;
            }
            // found a directive
            String string = stringLiteral();
            if (!hasEscape && "use strict".equals(string)) {
                strict = true;
            }
            StringLiteral stringLiteral = new StringLiteral(begin, ts.endPosition(), string);
            semicolon();
            statements.add(new ExpressionStatement(begin, ts.endPosition(), stringLiteral));
        }
        applyStrictMode(strict);
        return statements;
    }

    private static boolean stringLiteralFollowSetNextLine(Token token) {
        switch (token) {
        case DOT:
        case LB:
        case LP:
        case TEMPLATE:
        case COMMA:
        case HOOK:
            return true;
        default:
            return Token.isBinaryOperator(token) || Token.isAssignmentOperator(token);
        }
    }

    private void applyStrictMode(boolean strict) {
        if (strict) {
            context.strictMode = StrictMode.Strict;
            context.explicitStrict = true;
            if (context.strictError != null) {
                reportException(context.strictError);
            }
        } else {
            if (context.strictMode == StrictMode.Unknown) {
                context.strictMode = context.parent.strictMode;
            }
        }
    }

    /* ***************************************************************************************** */

    private static FunctionNode.StrictMode toFunctionStrictness(boolean strict, boolean explicit) {
        if (!strict) {
            return FunctionNode.StrictMode.NonStrict;
        }
        if (explicit) {
            return FunctionNode.StrictMode.ExplicitStrict;
        }
        return FunctionNode.StrictMode.ImplicitStrict;
    }

    private <FUNCTION extends FunctionNode> FUNCTION inheritStrictness(FUNCTION function) {
        if (context.strictMode != StrictMode.Unknown) {
            boolean strict = (context.strictMode == StrictMode.Strict);
            function.setStrictMode(toFunctionStrictness(strict, context.explicitStrict));
            if (context.deferred != null) {
                for (FunctionNode func : context.deferred) {
                    func.setStrictMode(toFunctionStrictness(strict, false));
                }
                context.deferred = null;
            }
        } else {
            // this case only applies for functions with default parameters
            assert context.parent.strictMode == StrictMode.Unknown;
            ParseContext parent = context.parent;
            if (parent.deferred == null) {
                parent.deferred = newSmallList();
            }
            parent.deferred.add(function);
            if (context.deferred != null) {
                parent.deferred.addAll(context.deferred);
                context.deferred = null;
            }
        }

        return function;
    }

    /**
     * Special case for {@link Token#YIELD} as {@link BindingIdentifier} in functions and generators
     */
    private BindingIdentifier bindingIdentifierFunctionName() {
        // FIXME: Preliminary solution to provide SpiderMonkey/V8 compatibility
        // 'yield' is always a keyword in strict-mode and in generators, but parse function name
        // in the context of the surrounding environment
        if (token() == Token.YIELD) {
            long begin = ts.beginPosition();
            if (isYieldName(context.parent)) {
                consume(Token.YIELD);
                return new BindingIdentifier(begin, ts.endPosition(), getName(Token.YIELD));
            }
            reportStrictModeSyntaxError(begin, Messages.Key.StrictModeInvalidIdentifier,
                    getName(Token.YIELD));
            reportTokenMismatch("<identifier>", Token.YIELD);
        }
        return bindingIdentifier();
    }

    /**
     * <strong>[14.1] Function Definitions</strong>
     * 
     * <pre>
     * FunctionDeclaration :
     *     function BindingIdentifier ( FormalParameters ) { FunctionBody }
     * </pre>
     */
    private FunctionDeclaration functionDeclaration() {
        newContext(ContextKind.Function);
        try {
            long begin = ts.beginPosition();
            consume(Token.FUNCTION);
            int startFunction = ts.position() - "function".length();
            BindingIdentifier identifier = bindingIdentifierFunctionName();
            consume(Token.LP);
            FormalParameterList parameters = formalParameters(Token.RP);
            consume(Token.RP);

            String header, body;
            List<StatementListItem> statements;
            if (token() != Token.LC && isEnabled(Option.ExpressionClosure)) {
                int startBody = ts.position();
                statements = expressionClosureBody();
                int endFunction = ts.position();

                header = ts.range(startFunction, startBody);
                body = "return " + ts.range(startBody, endFunction);
            } else {
                consume(Token.LC);
                int startBody = ts.position();
                statements = functionBody(Token.RC);
                consume(Token.RC);
                int endFunction = ts.position() - 1;

                header = ts.range(startFunction, startBody - 1);
                body = ts.range(startBody, endFunction);
            }

            FunctionContext scope = context.funContext;
            FunctionDeclaration function = new FunctionDeclaration(begin, ts.endPosition(), scope,
                    identifier, parameters, statements, header, body);
            scope.node = function;

            function_StaticSemantics(function);

            addFunctionDeclaration(function);

            return inheritStrictness(function);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[14.1] Function Definitions</strong>
     * 
     * <pre>
     * FunctionExpression :
     *     function BindingIdentifier<sub>opt</sub> ( FormalParameters ) { FunctionBody }
     * </pre>
     */
    private FunctionExpression functionExpression() {
        newContext(ContextKind.Function);
        try {
            long begin = ts.beginPosition();
            consume(Token.FUNCTION);
            int startFunction = ts.position() - "function".length();
            BindingIdentifier identifier = null;
            if (token() != Token.LP) {
                identifier = bindingIdentifierFunctionName();
            }
            consume(Token.LP);
            FormalParameterList parameters = formalParameters(Token.RP);
            consume(Token.RP);

            String header, body;
            List<StatementListItem> statements;
            if (token() != Token.LC && isEnabled(Option.ExpressionClosure)) {
                int startBody = ts.position();
                statements = expressionClosureBody();
                int endFunction = ts.position();

                header = ts.range(startFunction, startBody);
                body = "return " + ts.range(startBody, endFunction);
            } else {
                consume(Token.LC);
                int startBody = ts.position();
                statements = functionBody(Token.RC);
                consume(Token.RC);
                int endFunction = ts.position() - 1;

                header = ts.range(startFunction, startBody - 1);
                body = ts.range(startBody, endFunction);
            }

            FunctionContext scope = context.funContext;
            FunctionExpression function = new FunctionExpression(begin, ts.endPosition(), scope,
                    identifier, parameters, statements, header, body);
            scope.node = function;

            function_StaticSemantics(function);

            return inheritStrictness(function);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[14.1] Function Definitions</strong>
     * 
     * <pre>
     * StrictFormalParameters :
     *     FormalParameters
     * </pre>
     */
    private FormalParameterList strictFormalParameters(Token end) {
        return formalParameters(end);
    }

    /**
     * <strong>[14.1] Function Definitions</strong>
     * 
     * <pre>
     * FormalParameters :
     *     [empty]
     *     FormalParameterList
     * </pre>
     */
    private FormalParameterList formalParameters(Token end) {
        if (token() == end) {
            return new FormalParameterList(ts.beginPosition(), ts.endPosition(),
                    Collections.<FormalParameter> emptyList());
        }
        return formalParameterList();
    }

    /**
     * <strong>[14.1] Function Definitions</strong>
     * 
     * <pre>
     * FormalParameterList :
     *     FunctionRestParameter
     *     FormalsList
     *     FormalsList, FunctionRestParameter
     * FormalsList :
     *     FormalParameter
     *     FormalsList, FormalParameter
     * FunctionRestParameter :
     *     ... BindingIdentifier
     * FormalParameter :
     *     BindingElement
     * </pre>
     */
    private FormalParameterList formalParameterList() {
        long begin = ts.beginPosition();
        List<FormalParameter> formals = newSmallList();
        for (;;) {
            if (token() == Token.TRIPLE_DOT) {
                long beginRest = ts.beginPosition();
                consume(Token.TRIPLE_DOT);
                BindingIdentifier identifier = bindingIdentifierStrict();
                formals.add(new BindingRestElement(beginRest, ts.endPosition(), identifier));
                break;
            } else {
                formals.add(bindingElement());
                if (token() == Token.COMMA) {
                    consume(Token.COMMA);
                } else {
                    break;
                }
            }
        }
        return new FormalParameterList(begin, ts.endPosition(), formals);
    }

    private static <T> T containsAny(Set<T> set, List<T> list) {
        for (T element : list) {
            if (set.contains(element)) {
                return element;
            }
        }
        return null;
    }

    private void checkFormalParameterRedeclaration(FunctionNode node, List<String> boundNames,
            HashSet<String> declaredNames) {
        if (!(declaredNames == null || declaredNames.isEmpty())) {
            String redeclared = containsAny(declaredNames, boundNames);
            if (redeclared != null) {
                reportSyntaxError(node, Messages.Key.FormalParameterRedeclaration, redeclared);
            }
        }
    }

    private void function_StaticSemantics(FunctionDefinition function) {
        assert context.scopeContext == context.funContext;

        FunctionContext scope = context.funContext;
        FormalParameterList parameters = function.getParameters();
        List<String> boundNames = BoundNames(parameters);
        scope.parameterNames = new HashSet<>(boundNames);

        boolean simple = IsSimpleParameterList(parameters);
        if (!simple) {
            checkFormalParameterRedeclaration(function, boundNames, scope.varDeclaredNames);
        }
        checkFormalParameterRedeclaration(function, boundNames, scope.lexDeclaredNames);
        formalParameters_StaticSemantics(function, boundNames, scope.parameterNames, simple);
    }

    private void strictFormalParameters_StaticSemantics(FunctionNode node, List<String> boundNames,
            Set<String> names) {
        boolean hasDuplicates = (boundNames.size() != names.size());
        boolean hasEvalOrArguments = (names.contains("eval") || names.contains("arguments"));
        if (hasDuplicates) {
            reportSyntaxError(node, Messages.Key.StrictModeDuplicateFormalParameter);
        }
        if (hasEvalOrArguments) {
            reportSyntaxError(node, Messages.Key.StrictModeRestrictedIdentifier);
        }
    }

    private void formalParameters_StaticSemantics(FunctionNode node, List<String> boundNames,
            Set<String> names, boolean simple) {
        boolean strict = (context.strictMode != StrictMode.NonStrict);
        if (!strict && simple) {
            return;
        }
        boolean hasDuplicates = (boundNames.size() != names.size());
        boolean hasEvalOrArguments = (names.contains("eval") || names.contains("arguments"));
        if (!simple) {
            if (hasDuplicates) {
                reportSyntaxError(node, Messages.Key.StrictModeDuplicateFormalParameter);
            }
            if (hasEvalOrArguments) {
                reportSyntaxError(node, Messages.Key.StrictModeRestrictedIdentifier);
            }
        }
        if (strict) {
            if (hasDuplicates) {
                reportStrictModeSyntaxError(node, Messages.Key.StrictModeDuplicateFormalParameter);
            }
            if (hasEvalOrArguments) {
                reportStrictModeSyntaxError(node, Messages.Key.StrictModeRestrictedIdentifier);
            }
        }
    }

    /**
     * <strong>[14.1] Function Definitions</strong>
     * 
     * <pre>
     * FunctionBody :
     *     FunctionStatementList
     * FunctionStatementList :
     *     StatementList<sub>opt</sub>
     * </pre>
     */
    private List<StatementListItem> functionBody(Token end) {
        // enable 'yield' if in generator
        context.yieldAllowed = (context.kind == ContextKind.Generator);
        List<StatementListItem> prologue = directivePrologue();
        List<StatementListItem> body = statementList(end);
        return merge(prologue, body);
    }

    /**
     * <strong>[14.1] Function Definitions</strong>
     * 
     * <pre>
     * ExpressionClosureBody :
     *     AssignmentExpression
     * </pre>
     */
    private List<StatementListItem> expressionClosureBody() {
        // need to call manually b/c directivePrologue() isn't used here
        applyStrictMode(false);
        Expression expr = assignmentExpression(true);
        return Collections.<StatementListItem> singletonList(new ReturnStatement(
                ts.beginPosition(), ts.endPosition(), expr));
    }

    /**
     * <strong>[14.2] Arrow Function Definitions</strong>
     * 
     * <pre>
     * ArrowFunction :
     *     ArrowParameters => ConciseBody
     * ArrowParameters :
     *     BindingIdentifier
     *     CoverParenthesisedExpressionAndArrowParameterList
     * ConciseBody :
     *     [LA &#x2209; { <b>{</b> }] AssignmentExpression
     *     { FunctionBody }
     * </pre>
     * 
     * <h2>Supplemental Syntax</h2>
     * 
     * <pre>
     * ArrowFormalParameters :
     *     ( StrictFormalParameters )
     * </pre>
     */
    private ArrowFunction arrowFunction(boolean allowIn) {
        newContext(ContextKind.ArrowFunction);
        try {
            long begin = ts.beginPosition();
            StringBuilder source = new StringBuilder();
            source.append("function anonymous");

            FormalParameterList parameters;
            if (token() == Token.LP) {
                consume(Token.LP);
                int start = ts.position() - 1;
                parameters = strictFormalParameters(Token.RP);
                consume(Token.RP);

                source.append(ts.range(start, ts.position()));
            } else {
                BindingIdentifier identifier = bindingIdentifierStrict();
                FormalParameter parameter = new BindingElement(begin, ts.endPosition(), identifier,
                        null);
                parameters = new FormalParameterList(begin, ts.endPosition(),
                        singletonList(parameter));

                source.append('(').append(identifier.getName()).append(')');
            }
            consume(Token.ARROW);
            if (token() == Token.LC) {
                consume(Token.LC);
                int startBody = ts.position();
                List<StatementListItem> statements = functionBody(Token.RC);
                consume(Token.RC);
                int endFunction = ts.position() - 1;

                String header = source.toString();
                String body = ts.range(startBody, endFunction);

                FunctionContext scope = context.funContext;
                ArrowFunction function = new ArrowFunction(begin, ts.endPosition(), scope,
                        parameters, statements, header, body);
                scope.node = function;

                arrowFunction_StaticSemantics(function);

                return inheritStrictness(function);
            } else {
                // need to call manually b/c functionBody() isn't used here
                applyStrictMode(false);

                int startBody = ts.position();
                Expression expression = assignmentExpression(allowIn);
                int endFunction = ts.position();

                String header = source.toString();
                String body = "return " + ts.range(startBody, endFunction);

                FunctionContext scope = context.funContext;
                ArrowFunction function = new ArrowFunction(begin, ts.endPosition(), scope,
                        parameters, expression, header, body);
                scope.node = function;

                arrowFunction_StaticSemantics(function);

                return inheritStrictness(function);
            }
        } finally {
            restoreContext();
        }
    }

    private void arrowFunction_StaticSemantics(ArrowFunction function) {
        assert context.scopeContext == context.funContext;

        FunctionContext scope = context.funContext;
        FormalParameterList parameters = function.getParameters();
        List<String> boundNames = BoundNames(parameters);
        scope.parameterNames = new HashSet<>(boundNames);

        checkFormalParameterRedeclaration(function, boundNames, scope.varDeclaredNames);
        checkFormalParameterRedeclaration(function, boundNames, scope.lexDeclaredNames);
        strictFormalParameters_StaticSemantics(function, boundNames, scope.parameterNames);
    }

    /**
     * <strong>[14.3] Method Definitions</strong>
     * 
     * <pre>
     * MethodDefinition :
     *     PropertyName ( StrictFormalParameters ) { FunctionBody }
     *     GeneratorMethod
     *     get PropertyName ( ) { FunctionBody }
     *     set PropertyName ( PropertySetParameterList ) { FunctionBody }
     * </pre>
     */
    private MethodDefinition methodDefinition(boolean alwaysStrict) {
        switch (methodType()) {
        case Generator:
            return generatorMethod(alwaysStrict);
        case Getter:
            return getterMethod(alwaysStrict);
        case Setter:
            return setterMethod(alwaysStrict);
        case Function:
        default:
            return normalMethod(alwaysStrict);
        }
    }

    /**
     * <strong>[14.3] Method Definitions</strong>
     * 
     * <pre>
     * MethodDefinition :
     *     PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </pre>
     */
    private MethodDefinition normalMethod(boolean alwaysStrict) {
        long begin = ts.beginPosition();
        PropertyName propertyName = propertyName();
        return normalMethod(begin, propertyName, alwaysStrict);
    }

    private MethodDefinition normalMethod(long begin, PropertyName propertyName,
            boolean alwaysStrict) {
        newContext(ContextKind.Method);
        if (alwaysStrict) {
            context.strictMode = StrictMode.Strict;
        }
        try {
            consume(Token.LP);
            int startFunction = ts.position() - 1;
            FormalParameterList parameters = strictFormalParameters(Token.RP);
            consume(Token.RP);
            consume(Token.LC);
            int startBody = ts.position();
            List<StatementListItem> statements = functionBody(Token.RC);
            consume(Token.RC);
            int endFunction = ts.position() - 1;

            String header = "function " + ts.range(startFunction, startBody - 1);
            String body = ts.range(startBody, endFunction);

            FunctionContext scope = context.funContext;
            MethodType type = MethodType.Function;
            MethodDefinition method = new MethodDefinition(begin, ts.endPosition(), scope, type,
                    propertyName, parameters, statements, context.hasSuperReference(), header, body);
            scope.node = method;

            methodDefinition_StaticSemantics(method);

            return inheritStrictness(method);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[14.3] Method Definitions</strong>
     * 
     * <pre>
     * MethodDefinition :
     *     get PropertyName ( ) { FunctionBody }
     * </pre>
     */
    private MethodDefinition getterMethod(boolean alwaysStrict) {
        long begin = ts.beginPosition();

        consume(Token.NAME); // "get"
        PropertyName propertyName = propertyName();

        newContext(ContextKind.Method);
        if (alwaysStrict) {
            context.strictMode = StrictMode.Strict;
        }
        try {
            consume(Token.LP);
            int startFunction = ts.position() - 1;
            FormalParameterList parameters = new FormalParameterList(ts.beginPosition(),
                    ts.endPosition(), Collections.<FormalParameter> emptyList());
            consume(Token.RP);

            List<StatementListItem> statements;
            String header, body;
            if (token() != Token.LC && isEnabled(Option.ExpressionClosure)) {
                int startBody = ts.position();
                statements = expressionClosureBody();
                int endFunction = ts.position();

                header = "function " + ts.range(startFunction, startBody);
                body = "return " + ts.range(startBody, endFunction);
            } else {
                consume(Token.LC);
                int startBody = ts.position();
                statements = functionBody(Token.RC);
                consume(Token.RC);
                int endFunction = ts.position() - 1;

                header = "function " + ts.range(startFunction, startBody - 1);
                body = ts.range(startBody, endFunction);
            }

            FunctionContext scope = context.funContext;
            MethodType type = MethodType.Getter;
            MethodDefinition method = new MethodDefinition(begin, ts.endPosition(), scope, type,
                    propertyName, parameters, statements, context.hasSuperReference(), header, body);
            scope.node = method;

            methodDefinition_StaticSemantics(method);

            return inheritStrictness(method);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[14.3] Method Definitions</strong>
     * 
     * <pre>
     * MethodDefinition :
     *     set PropertyName ( PropertySetParameterList ) { FunctionBody }
     * </pre>
     */
    private MethodDefinition setterMethod(boolean alwaysStrict) {
        long begin = ts.beginPosition();

        consume(Token.NAME); // "set"
        PropertyName propertyName = propertyName();

        newContext(ContextKind.Method);
        if (alwaysStrict) {
            context.strictMode = StrictMode.Strict;
        }
        try {
            consume(Token.LP);
            int startFunction = ts.position() - 1;
            FormalParameterList parameters = propertySetParameterList();
            consume(Token.RP);

            List<StatementListItem> statements;
            String header, body;
            if (token() != Token.LC && isEnabled(Option.ExpressionClosure)) {
                int startBody = ts.position();
                statements = expressionClosureBody();
                int endFunction = ts.position();

                header = "function " + ts.range(startFunction, startBody);
                body = "return " + ts.range(startBody, endFunction);
            } else {
                consume(Token.LC);
                int startBody = ts.position();
                statements = functionBody(Token.RC);
                consume(Token.RC);
                int endFunction = ts.position() - 1;

                header = "function " + ts.range(startFunction, startBody - 1);
                body = ts.range(startBody, endFunction);
            }

            FunctionContext scope = context.funContext;
            MethodType type = MethodType.Setter;
            MethodDefinition method = new MethodDefinition(begin, ts.endPosition(), scope, type,
                    propertyName, parameters, statements, context.hasSuperReference(), header, body);
            scope.node = method;

            methodDefinition_StaticSemantics(method);

            return inheritStrictness(method);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[14.3] Method Definitions</strong>
     * 
     * <pre>
     * PropertySetParameterList :
     *     BindingIdentifier
     *     BindingPattern
     * </pre>
     */
    private FormalParameterList propertySetParameterList() {
        long begin = ts.beginPosition();
        Binding binding = binding();
        FormalParameter setParameter = new BindingElement(begin, ts.endPosition(), binding, null);
        return new FormalParameterList(begin, ts.endPosition(), singletonList(setParameter));
    }

    private MethodType methodType() {
        if (token() == Token.MUL) {
            return MethodType.Generator;
        }
        if (token() == Token.NAME) {
            String name = getName(Token.NAME);
            if (("get".equals(name) || "set".equals(name)) && isPropertyName(peek())) {
                return "get".equals(name) ? MethodType.Getter : MethodType.Setter;
            }
        }
        return MethodType.Function;
    }

    private boolean isPropertyName(Token token) {
        return token == Token.STRING || token == Token.NUMBER || token == Token.LB
                || isIdentifierName(token);
    }

    private void methodDefinition_StaticSemantics(MethodDefinition method) {
        assert context.scopeContext == context.funContext;

        FunctionContext scope = context.funContext;
        FormalParameterList parameters = method.getParameters();
        List<String> boundNames = BoundNames(parameters);
        scope.parameterNames = new HashSet<>(boundNames);

        switch (method.getType()) {
        case Function:
        case Generator: {
            checkFormalParameterRedeclaration(method, boundNames, scope.varDeclaredNames);
            checkFormalParameterRedeclaration(method, boundNames, scope.lexDeclaredNames);
            strictFormalParameters_StaticSemantics(method, boundNames, scope.parameterNames);
            return;
        }
        case Setter: {
            boolean simple = IsSimpleParameterList(parameters);
            if (!simple) {
                checkFormalParameterRedeclaration(method, boundNames, scope.varDeclaredNames);
            }
            checkFormalParameterRedeclaration(method, boundNames, scope.lexDeclaredNames);
            propertySetParameterList_StaticSemantics(method, boundNames, scope.parameterNames,
                    simple);
            return;
        }
        case Getter:
        default:
            return;
        }
    }

    private void propertySetParameterList_StaticSemantics(FunctionNode node,
            List<String> boundNames, Set<String> names, boolean simple) {
        boolean strict = (context.strictMode != StrictMode.NonStrict);
        boolean hasDuplicates = (boundNames.size() != names.size());
        boolean hasEvalOrArguments = (names.contains("eval") || names.contains("arguments"));
        if (!simple) {
            if (hasDuplicates) {
                reportSyntaxError(node, Messages.Key.StrictModeDuplicateFormalParameter);
            }
            if (hasEvalOrArguments) {
                reportSyntaxError(node, Messages.Key.StrictModeRestrictedIdentifier);
            }
        }
        // FIXME: spec bug - duplicate check done twice
        if (hasDuplicates) {
            reportSyntaxError(node, Messages.Key.StrictModeDuplicateFormalParameter);
        }
        // FIXME: spec bug - not handled in draft
        if (strict) {
            if (hasEvalOrArguments) {
                reportStrictModeSyntaxError(node, Messages.Key.StrictModeRestrictedIdentifier);
            }
        }
    }

    /**
     * <strong>[14.4] Generator Function Definitions</strong>
     * 
     * <pre>
     * GeneratorMethod :
     *     * PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </pre>
     */
    private MethodDefinition generatorMethod(boolean alwaysStrict) {
        long begin = ts.beginPosition();

        consume(Token.MUL);
        PropertyName propertyName = propertyName();

        newContext(ContextKind.Generator);
        if (alwaysStrict) {
            context.strictMode = StrictMode.Strict;
        }
        try {
            consume(Token.LP);
            int startFunction = ts.position() - 1;
            FormalParameterList parameters = strictFormalParameters(Token.RP);
            consume(Token.RP);
            consume(Token.LC);
            int startBody = ts.position();
            List<StatementListItem> statements = functionBody(Token.RC);
            consume(Token.RC);
            int endFunction = ts.position() - 1;

            String header = "function* " + ts.range(startFunction, startBody - 1);
            String body = ts.range(startBody, endFunction);

            FunctionContext scope = context.funContext;
            MethodType type = MethodType.Generator;
            MethodDefinition method = new MethodDefinition(begin, ts.endPosition(), scope, type,
                    propertyName, parameters, statements, context.hasSuperReference(), header, body);
            scope.node = method;

            methodDefinition_StaticSemantics(method);

            return inheritStrictness(method);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[14.4] Generator Function Definitions</strong>
     * 
     * <pre>
     * GeneratorDeclaration :
     *     function * BindingIdentifier ( FormalParameters ) { FunctionBody }
     * </pre>
     */
    private GeneratorDeclaration generatorDeclaration(boolean starless) {
        newContext(ContextKind.Generator);
        try {
            long begin = ts.beginPosition();
            consume(Token.FUNCTION);
            int startFunction = ts.position() - "function".length();
            if (!starless) {
                consume(Token.MUL);
            }
            BindingIdentifier identifier = bindingIdentifierFunctionName();
            consume(Token.LP);
            FormalParameterList parameters = formalParameters(Token.RP);
            consume(Token.RP);
            consume(Token.LC);
            int startBody = ts.position();
            List<StatementListItem> statements = functionBody(Token.RC);
            consume(Token.RC);
            int endFunction = ts.position() - 1;

            String header = ts.range(startFunction, startBody - 1);
            String body = ts.range(startBody, endFunction);

            FunctionContext scope = context.funContext;
            GeneratorDeclaration generator;
            if (!starless) {
                generator = new GeneratorDeclaration(begin, ts.endPosition(), scope, identifier,
                        parameters, statements, header, body);
            } else {
                generator = new LegacyGeneratorDeclaration(begin, ts.endPosition(), scope,
                        identifier, parameters, statements, header, body);
            }
            scope.node = generator;

            generator_StaticSemantics(generator);

            addGeneratorDeclaration(generator);

            return inheritStrictness(generator);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[14.4] Generator Function Definitions</strong>
     * 
     * <pre>
     * GeneratorExpression :
     *     function * BindingIdentifier<sub>opt</sub> ( FormalParameters ) { FunctionBody }
     * </pre>
     */
    private GeneratorExpression generatorExpression(boolean starless) {
        newContext(ContextKind.Generator);
        try {
            long begin = ts.beginPosition();
            consume(Token.FUNCTION);
            int startFunction = ts.position() - "function".length();
            if (!starless) {
                consume(Token.MUL);
            }
            BindingIdentifier identifier = null;
            if (token() != Token.LP) {
                identifier = bindingIdentifierFunctionName();
            }
            consume(Token.LP);
            FormalParameterList parameters = formalParameters(Token.RP);
            consume(Token.RP);
            consume(Token.LC);
            int startBody = ts.position();
            List<StatementListItem> statements = functionBody(Token.RC);
            consume(Token.RC);
            int endFunction = ts.position() - 1;

            String header = ts.range(startFunction, startBody - 1);
            String body = ts.range(startBody, endFunction);

            FunctionContext scope = context.funContext;
            GeneratorExpression generator;
            if (!starless) {
                generator = new GeneratorExpression(begin, ts.endPosition(), scope, identifier,
                        parameters, statements, header, body);
            } else {
                generator = new LegacyGeneratorExpression(begin, ts.endPosition(), scope,
                        identifier, parameters, statements, header, body);
            }
            scope.node = generator;

            generator_StaticSemantics(generator);

            return inheritStrictness(generator);
        } finally {
            restoreContext();
        }
    }

    private void generator_StaticSemantics(GeneratorDefinition generator) {
        assert context.scopeContext == context.funContext;

        FunctionContext scope = context.funContext;
        FormalParameterList parameters = generator.getParameters();
        List<String> boundNames = BoundNames(parameters);
        scope.parameterNames = new HashSet<>(boundNames);

        boolean simple = IsSimpleParameterList(parameters);
        if (!simple) {
            checkFormalParameterRedeclaration(generator, boundNames, scope.varDeclaredNames);
        }
        checkFormalParameterRedeclaration(generator, boundNames, scope.lexDeclaredNames);
        formalParameters_StaticSemantics(generator, boundNames, scope.parameterNames, simple);
    }

    /**
     * <strong>[14.4] Generator Function Definitions</strong>
     * 
     * <pre>
     * YieldExpression :
     *     yield YieldDelegator<sub>opt</sub> <font size="-1">[Lexical goal <i>InputElementRegExp</i>]</font> AssignmentExpression
     * YieldDelegator :
     *     *
     * </pre>
     */
    private YieldExpression yieldExpression(boolean allowIn) {
        if (!context.yieldAllowed) {
            if (context.kind == ContextKind.Function && isEnabled(Option.LegacyGenerator)) {
                throw new RetryGenerator();
            }
            reportSyntaxError(Messages.Key.InvalidYieldStatement);
        }

        long begin = ts.beginPosition();
        consume(Token.YIELD);
        boolean delegatedYield = false;
        if (token() == Token.MUL) {
            consume(Token.MUL);
            delegatedYield = true;
        }
        Expression expr;
        if (delegatedYield) {
            expr = assignmentExpression(allowIn);
        } else if (!isEnabled(Option.LegacyGenerator)) {
            if (noLineTerminator() && assignmentExpressionFirstSet(token())) {
                expr = assignmentExpression(allowIn);
            } else {
                expr = null;
            }
        } else {
            // slightly different rules for optional AssignmentExpression in legacy generators
            if (noLineTerminator() && !assignmentExpressionFollowSet(token())) {
                expr = assignmentExpression(allowIn);
            } else {
                expr = null;
            }
        }
        return new YieldExpression(begin, ts.endPosition(), delegatedYield, expr);
    }

    private boolean assignmentExpressionFirstSet(Token token) {
        // returns FIRST(AssignmentExpression)
        switch (token) {
        case YIELD:
            // FIRST(YieldExpression)
            return true;
        case DELETE:
        case VOID:
        case TYPEOF:
        case INC:
        case DEC:
        case ADD:
        case SUB:
        case BITNOT:
        case NOT:
            // FIRST(UnaryExpression)
            return true;
        case SUPER:
        case NEW:
            // FIRST(LeftHandSideExpression)
            return true;
        case THIS:
        case NULL:
        case FALSE:
        case TRUE:
        case NUMBER:
        case STRING:
        case LB:
        case LC:
        case LP:
        case FUNCTION:
        case CLASS:
        case TEMPLATE:
            // FIRST(PrimaryExpression)
            return true;
        case DIV:
        case ASSIGN_DIV:
            // FIRST(RegularExpressionLiteral)
            return true;
        case LET:
            return isEnabled(Option.LetExpression);
        case NAME:
        case IMPLEMENTS:
        case INTERFACE:
        case PACKAGE:
        case PRIVATE:
        case PROTECTED:
        case PUBLIC:
        case STATIC:
            // FIRST(Identifier)
            return isIdentifier(token);
        default:
            return false;
        }
    }

    private boolean assignmentExpressionFollowSet(Token token) {
        // returns FOLLOW(AssignmentExpression) without { "of", "in", "for", "{" }
        // NB: not the exact follow set, consider `a = let(x=0)x++ ++`, but not relevant here
        switch (token) {
        case COLON:
        case COMMA:
        case RB:
        case RC:
        case RP:
        case SEMI:
        case EOF:
            return true;
        default:
            return false;
        }
    }

    /**
     * <strong>[14.5] Class Definitions</strong>
     * 
     * <pre>
     * ClassDeclaration :
     *     class BindingIdentifier ClassTail
     * ClassTail :
     *     ClassHeritage<sub>opt</sub> { ClassBody<sub>opt</sub> }
     * ClassHeritage :
     *     extends AssignmentExpression
     * </pre>
     */
    private ClassDeclaration classDeclaration() {
        long begin = ts.beginPosition();
        consume(Token.CLASS);
        BindingIdentifier name = bindingIdentifierPureStrict();
        Expression heritage = null;
        if (token() == Token.EXTENDS) {
            consume(Token.EXTENDS);
            heritage = assignmentExpression(true);
        }
        consume(Token.LC);
        List<MethodDefinition> staticMethods = newList();
        List<MethodDefinition> prototypeMethods = newList();
        classBody(name, staticMethods, prototypeMethods);
        consume(Token.RC);

        ClassDeclaration decl = new ClassDeclaration(begin, ts.endPosition(), name, heritage,
                staticMethods, prototypeMethods);
        addLexDeclaredName(name);
        addLexScopedDeclaration(decl);
        return decl;
    }

    /**
     * <strong>[14.5] Class Definitions</strong>
     * 
     * <pre>
     * ClassExpression :
     *     class BindingIdentifier<sub>opt</sub> ClassTail
     * ClassTail :
     *     ClassHeritage<sub>opt</sub> { ClassBody<sub>opt</sub> }
     * ClassHeritage :
     *     extends AssignmentExpression
     * </pre>
     */
    private ClassExpression classExpression() {
        long begin = ts.beginPosition();
        consume(Token.CLASS);
        BindingIdentifier name = null;
        if (token() != Token.EXTENDS && token() != Token.LC) {
            name = bindingIdentifierPureStrict();
        }
        Expression heritage = null;
        if (token() == Token.EXTENDS) {
            consume(Token.EXTENDS);
            heritage = assignmentExpression(true);
        }
        consume(Token.LC);
        if (name != null) {
            enterBlockContext();
            addLexDeclaredName(name);
        }
        List<MethodDefinition> staticMethods = newList();
        List<MethodDefinition> prototypeMethods = newList();
        classBody(name, staticMethods, prototypeMethods);
        if (name != null) {
            exitBlockContext();
        }
        consume(Token.RC);

        return new ClassExpression(begin, ts.endPosition(), name, heritage, staticMethods,
                prototypeMethods);
    }

    /**
     * <strong>[14.5] Class Definitions</strong>
     * 
     * <pre>
     * ClassBody :
     *     ClassElementList
     * ClassElementList :
     *     ClassElement
     *     ClassElementList ClassElement
     * ClassElement :
     *     MethodDefinition
     *     static MethodDefinition
     *     ;
     * </pre>
     */
    private void classBody(BindingIdentifier className, List<MethodDefinition> staticMethods,
            List<MethodDefinition> prototypeMethods) {
        while (token() != Token.RC) {
            if (token() == Token.SEMI) {
                consume(Token.SEMI);
            } else if (token() == Token.STATIC && !LOOKAHEAD(Token.LP)) {
                consume(Token.STATIC);
                staticMethods.add(methodDefinition(true));
            } else {
                prototypeMethods.add(methodDefinition(true));
            }
        }

        classBody_StaticSemantics(className, staticMethods, true);
        classBody_StaticSemantics(className, prototypeMethods, false);
    }

    private void classBody_StaticSemantics(BindingIdentifier className,
            List<MethodDefinition> defs, boolean isStatic) {
        final int VALUE = 0, GETTER = 1, SETTER = 2;
        Map<String, Integer> values = new HashMap<>();
        for (MethodDefinition def : defs) {
            String key = PropName(def);
            if (key == null) {
                assert def.getPropertyName() instanceof ComputedPropertyName;
                continue;
            }
            if (isStatic) {
                if ("prototype".equals(key)) {
                    reportSyntaxError(def, Messages.Key.InvalidPrototypeMethod);
                }
            } else {
                if ("constructor".equals(key) && SpecialMethod(def)) {
                    reportSyntaxError(def, Messages.Key.InvalidConstructorMethod);
                }
                if (className != null) {
                    def.setFunctionName(className.getName());
                }
            }
            MethodDefinition.MethodType type = def.getType();
            final int kind = type == MethodType.Getter ? GETTER
                    : type == MethodType.Setter ? SETTER : VALUE;
            if (values.containsKey(key)) {
                int prev = values.get(key);
                if (kind == VALUE) {
                    reportSyntaxError(def, Messages.Key.DuplicatePropertyDefinition, key);
                }
                if (kind == GETTER && prev != SETTER) {
                    reportSyntaxError(def, Messages.Key.DuplicatePropertyDefinition, key);
                }
                if (kind == SETTER && prev != GETTER) {
                    reportSyntaxError(def, Messages.Key.DuplicatePropertyDefinition, key);
                }
                values.put(key, prev | kind);
            } else {
                values.put(key, kind);
            }
        }
    }

    /* ***************************************************************************************** */

    /**
     * <strong>[13] ECMAScript Language: Statements and Declarations</strong>
     * 
     * <pre>
     * Statement :
     *     BlockStatement
     *     VariableStatement
     *     EmptyStatement
     *     ExpressionStatement
     *     IfStatement
     *     BreakableStatement
     *     ContinueStatement
     *     BreakStatement
     *     ReturnStatement
     *     WithStatement
     *     LabelledStatement
     *     ThrowStatement
     *     TryStatement
     *     DebuggerStatement
     * 
     * BreakableStatement :
     *     IterationStatement
     *     SwitchStatement
     * </pre>
     */
    private Statement statement() {
        switch (token()) {
        case LC:
            return block(NO_INHERITED_BINDING);
        case VAR:
            return variableStatement();
        case SEMI:
            return emptyStatement();
        case IF:
            return ifStatement();
        case FOR:
            return forStatement(EMPTY_LABEL_SET);
        case WHILE:
            return whileStatement(EMPTY_LABEL_SET);
        case DO:
            return doWhileStatement(EMPTY_LABEL_SET);
        case CONTINUE:
            return continueStatement();
        case BREAK:
            return breakStatement();
        case RETURN:
            return returnStatement();
        case WITH:
            return withStatement();
        case SWITCH:
            return switchStatement(EMPTY_LABEL_SET);
        case THROW:
            return throwStatement();
        case TRY:
            return tryStatement();
        case DEBUGGER:
            return debuggerStatement();
        case LET:
            if (isEnabled(Option.LetStatement)) {
                return letStatement();
            }
            break;
        case YIELD:
            if (!isYieldName()) {
                break;
            }
            // fall-through
        case NAME:
            if (LOOKAHEAD(Token.COLON)) {
                return labelledStatement();
            }
        default:
        }
        return expressionStatement();
    }

    /**
     * <strong>[13.1] Block</strong>
     * 
     * <pre>
     * BlockStatement :
     *     Block
     * Block :
     *     { StatementList<sub>opt</sub> }
     * </pre>
     */
    private BlockStatement block(List<Binding> inherited) {
        long begin = ts.beginPosition();
        consume(Token.LC);
        BlockContext scope = enterBlockContext();
        if (!inherited.isEmpty()) {
            addLexDeclaredNames(inherited);
        }
        List<StatementListItem> list = statementList(Token.RC);
        if (!inherited.isEmpty()) {
            removeLexDeclaredNames(inherited);
        }
        exitBlockContext();
        consume(Token.RC);

        BlockStatement block = new BlockStatement(begin, ts.endPosition(), scope, list);
        scope.node = block;
        return block;
    }

    /**
     * <strong>[13.1] Block</strong>
     * 
     * <pre>
     * StatementList :
     *     StatementItem
     *     StatementList StatementListItem
     * </pre>
     */
    private List<StatementListItem> statementList(Token end) {
        List<StatementListItem> list = newList();
        while (token() != end) {
            list.add(statementListItem());
        }
        return list;
    }

    /**
     * <strong>[13.1] Block</strong>
     * 
     * <pre>
     * StatementListItem :
     *     Statement
     *     Declaration
     * Declaration :
     *     FunctionDeclaration
     *     GeneratorDeclaration
     *     ClassDeclaration
     *     LexicalDeclaration
     * </pre>
     */
    private StatementListItem statementListItem() {
        switch (token()) {
        case LET:
            if (LOOKAHEAD(Token.LP)
                    && (isEnabled(Option.LetStatement) || isEnabled(Option.LetExpression))) {
                return statement();
            }
        case FUNCTION:
        case CLASS:
        case CONST:
            return declaration();
        default:
            return statement();
        }
    }

    /**
     * <strong>[13.1] Block</strong>
     * 
     * <pre>
     * Declaration :
     *     FunctionDeclaration
     *     GeneratorDeclaration
     *     ClassDeclaration
     *     LexicalDeclaration
     * </pre>
     */
    private Declaration declaration() {
        switch (token()) {
        case FUNCTION:
            return functionOrGeneratorDeclaration();
        case CLASS:
            return classDeclaration();
        case LET:
        case CONST:
            return lexicalDeclaration(true);
        default:
            throw reportSyntaxError(Messages.Key.InvalidToken, token().toString());
        }
    }

    private Declaration functionOrGeneratorDeclaration() {
        if (LOOKAHEAD(Token.MUL)) {
            return generatorDeclaration(false);
        } else {
            long position = ts.position(), lineinfo = ts.lineinfo();
            try {
                return functionDeclaration();
            } catch (RetryGenerator e) {
                ts.reset(position, lineinfo);
                return generatorDeclaration(true);
            }
        }
    }

    /**
     * <strong>[13.2.1] Let and Const Declarations</strong>
     * 
     * <pre>
     * LexicalDeclaration :
     *     LetOrConst BindingList ;
     * LexicalDeclarationNoIn :
     *     LetOrConst BindingListNoIn
     * LetOrConst :
     *     let
     *     const
     * </pre>
     */
    private LexicalDeclaration lexicalDeclaration(boolean allowIn) {
        long begin = ts.beginPosition();
        LexicalDeclaration.Type type;
        if (token() == Token.LET) {
            consume(Token.LET);
            type = LexicalDeclaration.Type.Let;
        } else {
            consume(Token.CONST);
            type = LexicalDeclaration.Type.Const;
        }
        List<LexicalBinding> list = bindingList((type == LexicalDeclaration.Type.Const), allowIn);
        if (allowIn) {
            semicolon();
        }

        LexicalDeclaration decl = new LexicalDeclaration(begin, ts.endPosition(), type, list);
        addLexScopedDeclaration(decl);
        return decl;
    }

    /**
     * <strong>[13.2.1] Let and Const Declarations</strong>
     * 
     * <pre>
     * BindingList :
     *     LexicalBinding
     *     BindingList, LexicalBinding
     * BindingListNoIn :
     *     LexicalBindingNoIn
     *     BindingListNoIn, LexicalBindingNoIn
     * </pre>
     */
    private List<LexicalBinding> bindingList(boolean isConst, boolean allowIn) {
        List<LexicalBinding> list = newSmallList();
        list.add(lexicalBinding(isConst, allowIn));
        while (token() == Token.COMMA) {
            consume(Token.COMMA);
            list.add(lexicalBinding(isConst, allowIn));
        }

        return list;
    }

    /**
     * <strong>[13.2.1] Let and Const Declarations</strong>
     * 
     * <pre>
     * LexicalBinding :
     *     BindingIdentifier Initialiser<sub>opt</sub>
     *     BindingPattern Initialiser
     * LexicalBindingNoIn :
     *     BindingIdentifier InitialiserNoIn<sub>opt</sub>
     *     BindingPattern InitialiserNoIn
     * </pre>
     */
    private LexicalBinding lexicalBinding(boolean isConst, boolean allowIn) {
        long begin = ts.beginPosition();
        Binding binding;
        Expression initialiser = null;
        if (token() == Token.LC || token() == Token.LB) {
            BindingPattern bindingPattern = bindingPattern();
            addLexDeclaredName(bindingPattern);
            if (allowIn) {
                initialiser = initialiser(allowIn);
            } else if (token() == Token.ASSIGN) {
                // make initialiser optional if `allowIn == false`
                initialiser = initialiser(allowIn);
            }
            binding = bindingPattern;
        } else {
            BindingIdentifier bindingIdentifier = bindingIdentifier();
            addLexDeclaredName(bindingIdentifier);
            if (token() == Token.ASSIGN) {
                initialiser = initialiser(allowIn);
            } else if (isConst && allowIn) {
                // `allowIn == false` indicates for-loop, cf. validateFor{InOf}
                reportSyntaxError(bindingIdentifier, Messages.Key.ConstMissingInitialiser);
            }
            binding = bindingIdentifier;
        }

        return new LexicalBinding(begin, ts.endPosition(), binding, initialiser);
    }

    /**
     * <strong>[13.2.1] Let and Const Declarations</strong>
     * 
     * <pre>
     * BindingIdentifier :
     *     Identifier
     * </pre>
     */
    private BindingIdentifier bindingIdentifier() {
        long begin = ts.beginPosition();
        String identifier = identifier();
        if (context.strictMode != StrictMode.NonStrict) {
            if ("arguments".equals(identifier) || "eval".equals(identifier)) {
                reportStrictModeSyntaxError(begin, Messages.Key.StrictModeRestrictedIdentifier);
            }
        }
        return new BindingIdentifier(begin, ts.endPosition(), identifier);
    }

    /**
     * <strong>[13.2.1] Let and Const Declarations</strong>
     * 
     * <pre>
     * BindingIdentifier :
     *     Identifier
     * </pre>
     */
    private BindingIdentifier bindingIdentifierStrict() {
        long begin = ts.beginPosition();
        String identifier = identifier();
        if ("arguments".equals(identifier) || "eval".equals(identifier)) {
            reportSyntaxError(begin, Messages.Key.StrictModeRestrictedIdentifier);
        }
        return new BindingIdentifier(begin, ts.endPosition(), identifier);
    }

    /**
     * <strong>[13.2.1] Let and Const Declarations</strong>
     * 
     * <pre>
     * BindingIdentifier :
     *     Identifier
     * </pre>
     */
    private BindingIdentifier bindingIdentifierPureStrict() {
        long begin = ts.beginPosition();
        String identifier = strictIdentifier();
        if ("arguments".equals(identifier) || "eval".equals(identifier)) {
            reportSyntaxError(begin, Messages.Key.StrictModeRestrictedIdentifier);
        }
        return new BindingIdentifier(begin, ts.endPosition(), identifier);
    }

    /**
     * <strong>[13.2.1] Let and Const Declarations</strong>
     * 
     * <pre>
     * Initialiser :
     *     = AssignmentExpression
     * InitialiserNoIn :
     *     = AssignmentExpressionNoIn
     * </pre>
     */
    private Expression initialiser(boolean allowIn) {
        consume(Token.ASSIGN);
        return assignmentExpression(allowIn);
    }

    /**
     * <strong>[13.2.2] Variable Statement</strong>
     * 
     * <pre>
     * VariableStatement :
     *     var VariableDeclarationList ;
     * </pre>
     */
    private VariableStatement variableStatement() {
        long begin = ts.beginPosition();
        consume(Token.VAR);
        List<VariableDeclaration> decls = variableDeclarationList(true);
        semicolon();

        VariableStatement varStmt = new VariableStatement(begin, ts.endPosition(), decls);
        addVarScopedDeclaration(varStmt);
        return varStmt;
    }

    /**
     * <strong>[13.2.2] Variable Statement</strong>
     * 
     * <pre>
     * VariableDeclarationList :
     *     VariableDeclaration
     *     VariableDeclarationList , VariableDeclaration
     * VariableDeclarationListNoIn :
     *     VariableDeclarationNoIn
     *     VariableDeclarationListNoIn , VariableDeclarationNoIn
     * </pre>
     */
    private List<VariableDeclaration> variableDeclarationList(boolean allowIn) {
        List<VariableDeclaration> list = newSmallList();
        list.add(variableDeclaration(allowIn));
        while (token() == Token.COMMA) {
            consume(Token.COMMA);
            list.add(variableDeclaration(allowIn));
        }

        return list;
    }

    /**
     * <strong>[13.2.2] Variable Statement</strong>
     * 
     * <pre>
     * VariableDeclaration :
     *     BindingIdentifier Initialiser<sub>opt</sub>
     *     BindingPattern Initialiser
     * VariableDeclarationNoIn :
     *     BindingIdentifier InitialiserNoIn<sub>opt</sub>
     *     BindingPattern InitialiserNoIn
     * </pre>
     */
    private VariableDeclaration variableDeclaration(boolean allowIn) {
        Binding binding;
        Expression initialiser = null;
        if (token() == Token.LC || token() == Token.LB) {
            BindingPattern bindingPattern = bindingPattern();
            addVarDeclaredName(bindingPattern);
            if (allowIn) {
                initialiser = initialiser(allowIn);
            } else if (token() == Token.ASSIGN) {
                // make initialiser optional if `allowIn == false`
                initialiser = initialiser(allowIn);
            }
            binding = bindingPattern;
        } else {
            BindingIdentifier bindingIdentifier = bindingIdentifier();
            addVarDeclaredName(bindingIdentifier);
            if (token() == Token.ASSIGN) {
                initialiser = initialiser(allowIn);
            }
            binding = bindingIdentifier;
        }

        return new VariableDeclaration(binding, initialiser);
    }

    /**
     * <strong>[13.2.4] Destructuring Binding Patterns</strong>
     * 
     * <pre>
     * BindingPattern :
     *     ObjectBindingPattern
     *     ArrayBindingPattern
     * </pre>
     */
    private BindingPattern bindingPattern() {
        if (token() == Token.LC) {
            return objectBindingPattern();
        } else {
            return arrayBindingPattern();
        }
    }

    /**
     * <strong>[13.2.4] Destructuring Binding Patterns</strong>
     * 
     * <pre>
     * ObjectBindingPattern :
     *     { }
     *     { BindingPropertyList }
     *     { BindingPropertyList , }
     * BindingPropertyList :
     *     BindingProperty
     *     BindingPropertyList , BindingProperty
     * BindingProperty :
     *     SingleNameBinding
     *     PropertyName : BindingElement
     * BindingElement :
     *     SingleNameBinding
     *     BindingPattern Initialiser<sub>opt</sub>
     * SingleNameBinding :
     *     BindingIdentifier Initialiser<sub>opt</sub>
     * PropertyName :
     *     IdentifierName
     *     StringLiteral
     *     NumericLiteral
     * </pre>
     */
    private ObjectBindingPattern objectBindingPattern() {
        long begin = ts.beginPosition();
        List<BindingProperty> list = newSmallList();
        consume(Token.LC);
        while (token() != Token.RC) {
            list.add(bindingProperty());
            if (token() == Token.COMMA) {
                consume(Token.COMMA);
            } else {
                break;
            }
        }
        consume(Token.RC);

        objectBindingPattern_StaticSemantics(list);

        return new ObjectBindingPattern(begin, ts.endPosition(), list);
    }

    /**
     * <strong>[13.2.4] Destructuring Binding Patterns</strong>
     * 
     * <pre>
     * BindingProperty :
     *     SingleNameBinding
     *     PropertyName : BindingElement
     * BindingElement :
     *     SingleNameBinding
     *     BindingPattern Initialiser<sub>opt</sub>
     * SingleNameBinding :
     *     BindingIdentifier Initialiser<sub>opt</sub>
     * BindingIdentifier :
     *     Identifier
     * </pre>
     */
    private BindingProperty bindingProperty() {
        if (token() == Token.LB || LOOKAHEAD(Token.COLON)) {
            PropertyName propertyName = propertyName();
            consume(Token.COLON);
            Binding binding;
            if (token() == Token.LC) {
                binding = objectBindingPattern();
            } else if (token() == Token.LB) {
                binding = arrayBindingPattern();
            } else {
                binding = bindingIdentifierStrict();
            }
            Expression initialiser = null;
            if (token() == Token.ASSIGN) {
                initialiser = initialiser(true);
            }
            return new BindingProperty(propertyName, binding, initialiser);
        } else {
            BindingIdentifier binding = bindingIdentifierStrict();
            Expression initialiser = null;
            if (token() == Token.ASSIGN) {
                initialiser = initialiser(true);
            }
            return new BindingProperty(binding, initialiser);
        }
    }

    /**
     * <strong>[13.2.4] Destructuring Binding Patterns</strong>
     * 
     * <pre>
     * ArrayBindingPattern :
     *     [ Elision<sub>opt</sub> BindingRestElement<sub>opt</sub> ]
     *     [ BindingElementList ]
     *     [ BindingElementList , Elision<sub>opt</sub> BindingRestElement<sub>opt</sub> ]
     * BindingElementList :
     *     Elision<sub>opt</sub> BindingElement
     *     BindingElementList , Elision<sub>opt</sub> BindingElement
     * BindingRestElement :
     *     ... BindingIdentifier
     * </pre>
     */
    private ArrayBindingPattern arrayBindingPattern() {
        long begin = ts.beginPosition();
        List<BindingElementItem> list = newSmallList();
        consume(Token.LB);
        boolean needComma = false;
        Token tok;
        while ((tok = token()) != Token.RB) {
            if (needComma) {
                consume(Token.COMMA);
                needComma = false;
            } else if (tok == Token.COMMA) {
                consume(Token.COMMA);
                list.add(new BindingElision(0, 0));
            } else if (tok == Token.TRIPLE_DOT) {
                long beginRest = ts.beginPosition();
                consume(Token.TRIPLE_DOT);
                BindingIdentifier identifier = bindingIdentifierStrict();
                list.add(new BindingRestElement(beginRest, ts.endPosition(), identifier));
                break;
            } else {
                list.add(bindingElementStrict());
                needComma = true;
            }
        }
        consume(Token.RB);

        arrayBindingPattern_StaticSemantics(list);

        return new ArrayBindingPattern(begin, ts.endPosition(), list);
    }

    /**
     * <pre>
     * Binding :
     *     BindingIdentifier
     *     BindingPattern
     * </pre>
     */
    private Binding binding() {
        switch (token()) {
        case LC:
            return objectBindingPattern();
        case LB:
            return arrayBindingPattern();
        default:
            return bindingIdentifier();
        }
    }

    /**
     * <pre>
     * Binding :
     *     BindingIdentifier
     *     BindingPattern
     * </pre>
     */
    private Binding bindingStrict() {
        switch (token()) {
        case LC:
            return objectBindingPattern();
        case LB:
            return arrayBindingPattern();
        default:
            return bindingIdentifierStrict();
        }
    }

    /**
     * <strong>[13.2.4] Destructuring Binding Patterns</strong>
     * 
     * <pre>
     * BindingElement :
     *     SingleNameBinding
     *     BindingPattern Initialiser<sub>opt</sub>
     * SingleNameBinding :
     *     BindingIdentifier Initialiser<sub>opt</sub>
     * </pre>
     */
    private BindingElement bindingElement() {
        long begin = ts.beginPosition();
        Binding binding = binding();
        Expression initialiser = null;
        if (token() == Token.ASSIGN) {
            initialiser = initialiser(true);
        }

        return new BindingElement(begin, ts.endPosition(), binding, initialiser);
    }

    /**
     * <strong>[13.2.4] Destructuring Binding Patterns</strong>
     * 
     * <pre>
     * BindingElement :
     *     SingleNameBinding
     *     BindingPattern Initialiser<sub>opt</sub>
     * SingleNameBinding :
     *     BindingIdentifier Initialiser<sub>opt</sub>
     * </pre>
     */
    private BindingElement bindingElementStrict() {
        long begin = ts.beginPosition();
        Binding binding = bindingStrict();
        Expression initialiser = null;
        if (token() == Token.ASSIGN) {
            initialiser = initialiser(true);
        }

        return new BindingElement(begin, ts.endPosition(), binding, initialiser);
    }

    private static String BoundName(BindingIdentifier binding) {
        return binding.getName();
    }

    private static String BoundName(BindingRestElement element) {
        return element.getBindingIdentifier().getName();
    }

    private void objectBindingPattern_StaticSemantics(List<BindingProperty> list) {
        for (BindingProperty property : list) {
            // BindingProperty : PropertyName ':' BindingElement
            // BindingProperty : BindingIdentifier Initialiser<opt>
            Binding binding = property.getBinding();
            if (binding instanceof BindingIdentifier) {
                String name = BoundName(((BindingIdentifier) binding));
                if ("arguments".equals(name) || "eval".equals(name)) {
                    reportSyntaxError(binding, Messages.Key.StrictModeRestrictedIdentifier);
                }
            } else {
                assert binding instanceof BindingPattern;
                assert property.getPropertyName() != null;
                // already done implicitly
                // objectBindingPattern_StaticSemantics(((ObjectBindingPattern) binding).getList());
                // arrayBindingPattern_StaticSemantics(((ArrayBindingPattern)
                // binding).getElements());
            }
        }
    }

    private void arrayBindingPattern_StaticSemantics(List<BindingElementItem> list) {
        for (BindingElementItem element : list) {
            if (element instanceof BindingElement) {
                Binding binding = ((BindingElement) element).getBinding();
                if (binding instanceof ArrayBindingPattern) {
                    // already done implicitly
                    // arrayBindingPattern_StaticSemantics(((ArrayBindingPattern) binding)
                    // .getElements());
                } else if (binding instanceof ObjectBindingPattern) {
                    // already done implicitly
                    // objectBindingPattern_StaticSemantics(((ObjectBindingPattern)
                    // binding).getList());
                } else {
                    assert (binding instanceof BindingIdentifier);
                    String name = BoundName(((BindingIdentifier) binding));
                    if ("arguments".equals(name) || "eval".equals(name)) {
                        reportSyntaxError(binding, Messages.Key.StrictModeRestrictedIdentifier);
                    }
                }
            } else if (element instanceof BindingRestElement) {
                String name = BoundName(((BindingRestElement) element));
                if ("arguments".equals(name) || "eval".equals(name)) {
                    reportSyntaxError(element, Messages.Key.StrictModeRestrictedIdentifier);
                }
            } else {
                assert element instanceof BindingElision;
            }
        }
    }

    /**
     * <strong>[13.3] Empty Statement</strong>
     * 
     * <pre>
     * EmptyStatement:
     * ;
     * </pre>
     */
    private EmptyStatement emptyStatement() {
        long begin = ts.sourcePosition();
        consume(Token.SEMI);

        return new EmptyStatement(begin, ts.endPosition());
    }

    /**
     * <strong>[13.4] Expression Statement</strong>
     * 
     * <pre>
     * ExpressionStatement :
     *     [LA &#x2209; { <b>{, function, class</b> }] Expression ;
     * </pre>
     */
    private ExpressionStatement expressionStatement() {
        switch (token()) {
        case LC:
        case FUNCTION:
        case CLASS:
            reportSyntaxError(Messages.Key.InvalidToken, token().toString());
        default:
            long begin = ts.beginPosition();
            Expression expr = expression(true);
            semicolon();

            return new ExpressionStatement(begin, ts.endPosition(), expr);
        }
    }

    /**
     * <strong>[13.5] The <code>if</code> Statement</strong>
     * 
     * <pre>
     * IfStatement :
     *     if ( Expression ) Statement else Statement
     *     if ( Expression ) Statement
     * </pre>
     */
    private IfStatement ifStatement() {
        long begin = ts.beginPosition();
        consume(Token.IF);
        consume(Token.LP);
        Expression test = expression(true);
        consume(Token.RP);
        Statement then = statement();
        Statement otherwise = null;
        if (token() == Token.ELSE) {
            consume(Token.ELSE);
            otherwise = statement();
        }

        return new IfStatement(begin, ts.endPosition(), test, then, otherwise);
    }

    /**
     * <strong>[13.6.1] The <code>do-while</code> Statement</strong>
     * 
     * <pre>
     * IterationStatement :
     *     do Statement while ( Expression ) ;<sub>opt</sub>
     * </pre>
     */
    private DoWhileStatement doWhileStatement(Set<String> labelSet) {
        long begin = ts.beginPosition();
        consume(Token.DO);

        LabelContext labelCx = enterIteration(begin, labelSet);
        Statement stmt = statement();
        exitIteration();

        consume(Token.WHILE);
        consume(Token.LP);
        Expression test = expression(true);
        consume(Token.RP);
        if (token() == Token.SEMI) {
            consume(Token.SEMI);
        }

        return new DoWhileStatement(begin, ts.endPosition(), labelCx.abrupts, labelCx.labelSet,
                test, stmt);
    }

    /**
     * <strong>[13.6.2] The <code>while</code> Statement</strong>
     * 
     * <pre>
     * IterationStatement :
     *     while ( Expression ) Statement
     * </pre>
     */
    private WhileStatement whileStatement(Set<String> labelSet) {
        long begin = ts.beginPosition();
        consume(Token.WHILE);
        consume(Token.LP);
        Expression test = expression(true);
        consume(Token.RP);

        LabelContext labelCx = enterIteration(begin, labelSet);
        Statement stmt = statement();
        exitIteration();

        return new WhileStatement(begin, ts.endPosition(), labelCx.abrupts, labelCx.labelSet, test,
                stmt);
    }

    /**
     * <strong>[13.6.3] The <code>for</code> Statement</strong> <br>
     * <strong>[13.6.4] The <code>for-in</code> and <code>for-of</code> Statements</strong>
     * 
     * <pre>
     * IterationStatement :
     *     for ( ExpressionNoIn<sub>opt</sub> ; Expression<sub>opt</sub> ; Expression <sub>opt</sub> ) Statement
     *     for ( var VariableDeclarationListNoIn ; Expression<sub>opt</sub> ; Expression <sub>opt</sub> ) Statement
     *     for ( LexicalDeclarationNoIn ; Expression<sub>opt</sub> ; Expression <sub>opt</sub> ) Statement
     *     for ( LeftHandSideExpression in Expression ) Statement
     *     for ( var ForBinding in Expression ) Statement
     *     for ( ForDeclaration in Expression ) Statement
     *     for ( LeftHandSideExpression of Expression ) Statement
     *     for ( var ForBinding of Expression ) Statement
     *     for ( ForDeclaration of Expression ) Statement
     * ForDeclaration :
     *     LetOrConst ForBinding
     * ForBinding :
     *     BindingIdentifier
     *     BindingPattern
     * </pre>
     */
    private IterationStatement forStatement(Set<String> labelSet) {
        long begin = ts.beginPosition();
        consume(Token.FOR);
        boolean each = false;
        if (token() != Token.LP && isName("each") && isEnabled(Option.ForEachStatement)) {
            consume("each");
            each = true;
        }
        consume(Token.LP);

        BlockContext lexBlockContext = null;
        Node head;
        switch (token()) {
        case VAR:
            long beginVar = ts.beginPosition();
            consume(Token.VAR);
            List<VariableDeclaration> decls = variableDeclarationList(false);
            VariableStatement varStmt = new VariableStatement(beginVar, ts.endPosition(), decls);
            addVarScopedDeclaration(varStmt);
            head = varStmt;
            break;
        case LET:
        case CONST:
            lexBlockContext = enterBlockContext();
            head = lexicalDeclaration(false);
            break;
        case SEMI:
            head = null;
            break;
        default:
            head = expression(false);
            break;
        }

        if (each && token() != Token.IN) {
            reportTokenMismatch(Token.IN, token());
        }

        if (token() == Token.SEMI) {
            head = validateFor(head);
            consume(Token.SEMI);
            Expression test = null;
            if (token() != Token.SEMI) {
                test = expression(true);
            }
            consume(Token.SEMI);
            Expression step = null;
            if (token() != Token.RP) {
                step = expression(true);
            }
            consume(Token.RP);

            LabelContext labelCx = enterIteration(begin, labelSet);
            Statement stmt = statement();
            exitIteration();

            if (lexBlockContext != null) {
                exitBlockContext();
            }

            ForStatement iteration = new ForStatement(begin, ts.endPosition(), lexBlockContext,
                    labelCx.abrupts, labelCx.labelSet, head, test, step, stmt);
            if (lexBlockContext != null) {
                lexBlockContext.node = iteration;
            }
            return iteration;
        } else if (token() == Token.IN) {
            head = validateForInOf(head);
            consume(Token.IN);
            Expression expr;
            if (lexBlockContext == null) {
                expr = expression(true);
            } else {
                exitBlockContext();
                expr = expression(true);
                reenterBlockContext(lexBlockContext);
            }
            consume(Token.RP);

            LabelContext labelCx = enterIteration(begin, labelSet);
            Statement stmt = statement();
            exitIteration();

            if (lexBlockContext != null) {
                exitBlockContext();
            }

            if (each) {
                ForEachStatement iteration = new ForEachStatement(begin, ts.endPosition(),
                        lexBlockContext, labelCx.abrupts, labelCx.labelSet, head, expr, stmt);
                if (lexBlockContext != null) {
                    lexBlockContext.node = iteration;
                }
                return iteration;
            } else {
                ForInStatement iteration = new ForInStatement(begin, ts.endPosition(),
                        lexBlockContext, labelCx.abrupts, labelCx.labelSet, head, expr, stmt);
                if (lexBlockContext != null) {
                    lexBlockContext.node = iteration;
                }
                return iteration;
            }
        } else {
            head = validateForInOf(head);
            consume("of");
            Expression expr;
            if (lexBlockContext == null) {
                expr = assignmentExpression(true);
            } else {
                exitBlockContext();
                expr = assignmentExpression(true);
                reenterBlockContext(lexBlockContext);
            }
            consume(Token.RP);

            LabelContext labelCx = enterIteration(begin, labelSet);
            Statement stmt = statement();
            exitIteration();

            if (lexBlockContext != null) {
                exitBlockContext();
            }

            ForOfStatement iteration = new ForOfStatement(begin, ts.endPosition(), lexBlockContext,
                    labelCx.abrupts, labelCx.labelSet, head, expr, stmt);
            if (lexBlockContext != null) {
                lexBlockContext.node = iteration;
            }
            return iteration;
        }
    }

    /**
     * @see #forStatement(Set)
     */
    private Node validateFor(Node head) {
        if (head instanceof VariableStatement) {
            for (VariableDeclaration decl : ((VariableStatement) head).getElements()) {
                if (decl.getBinding() instanceof BindingPattern && decl.getInitialiser() == null) {
                    reportSyntaxError(head, Messages.Key.DestructuringMissingInitialiser);
                }
            }
        } else if (head instanceof LexicalDeclaration) {
            boolean isConst = ((LexicalDeclaration) head).getType() == LexicalDeclaration.Type.Const;
            for (LexicalBinding decl : ((LexicalDeclaration) head).getElements()) {
                if (decl.getBinding() instanceof BindingPattern && decl.getInitialiser() == null) {
                    reportSyntaxError(head, Messages.Key.DestructuringMissingInitialiser);
                }
                if (isConst && decl.getInitialiser() == null) {
                    reportSyntaxError(head, Messages.Key.ConstMissingInitialiser);
                }
            }
        }
        return head;
    }

    /**
     * @see #forStatement(Set)
     */
    private Node validateForInOf(Node head) {
        if (head instanceof VariableStatement) {
            // expected: single variable declaration with no initialiser
            List<VariableDeclaration> elements = ((VariableStatement) head).getElements();
            if (elements.size() == 1 && elements.get(0).getInitialiser() == null) {
                return head;
            }
        } else if (head instanceof LexicalDeclaration) {
            // expected: single lexical binding with no initialiser
            List<LexicalBinding> elements = ((LexicalDeclaration) head).getElements();
            if (elements.size() == 1 && elements.get(0).getInitialiser() == null) {
                return head;
            }
        } else if (head instanceof Expression) {
            // expected: left-hand side expression
            return validateAssignment((Expression) head, ExceptionType.SyntaxError,
                    Messages.Key.InvalidAssignmentTarget);
        }
        throw reportSyntaxError(head, Messages.Key.InvalidForInOfHead);
    }

    /**
     * Static Semantics: IsValidSimpleAssignmentTarget
     */
    private LeftHandSideExpression validateSimpleAssignment(Expression lhs, ExceptionType type,
            Messages.Key messageKey) {
        if (lhs instanceof Identifier) {
            if (context.strictMode != StrictMode.NonStrict) {
                String name = ((Identifier) lhs).getName();
                if ("eval".equals(name) || "arguments".equals(name)) {
                    reportStrictModeSyntaxError(lhs, Messages.Key.StrictModeInvalidAssignmentTarget);
                }
            }
            return (Identifier) lhs;
        } else if (lhs instanceof ElementAccessor) {
            return (ElementAccessor) lhs;
        } else if (lhs instanceof PropertyAccessor) {
            return (PropertyAccessor) lhs;
        } else if (lhs instanceof SuperExpression) {
            SuperExpression superExpr = (SuperExpression) lhs;
            if (superExpr.getType() == SuperExpression.Type.ElementAccessor
                    || superExpr.getType() == SuperExpression.Type.PropertyAccessor) {
                return superExpr;
            }
        }
        // everything else => invalid lhs
        throw reportError(type, lhs.getBeginPosition(), messageKey);
    }

    /**
     * Static Semantics: IsValidSimpleAssignmentTarget
     */
    private LeftHandSideExpression validateAssignment(Expression lhs, ExceptionType type,
            Messages.Key messageKey) {
        // rewrite object/array literal to destructuring form
        if (lhs instanceof ObjectLiteral) {
            return toDestructuring((ObjectLiteral) lhs);
        } else if (lhs instanceof ArrayLiteral) {
            return toDestructuring((ArrayLiteral) lhs);
        }
        return validateSimpleAssignment(lhs, type, messageKey);
    }

    private ObjectAssignmentPattern toDestructuring(ObjectLiteral object) {
        List<AssignmentProperty> list = newSmallList();
        for (PropertyDefinition p : object.getProperties()) {
            AssignmentProperty property;
            if (p instanceof PropertyValueDefinition) {
                // AssignmentProperty : PropertyName ':' AssignmentElement
                // AssignmentElement : DestructuringAssignmentTarget Initialiser{opt}
                // DestructuringAssignmentTarget : LeftHandSideExpression
                PropertyValueDefinition def = (PropertyValueDefinition) p;
                PropertyName propertyName = def.getPropertyName();
                Expression propertyValue = def.getPropertyValue();
                LeftHandSideExpression target;
                Expression initialiser;
                if (propertyValue instanceof AssignmentExpression) {
                    AssignmentExpression assignment = (AssignmentExpression) propertyValue;
                    if (assignment.getOperator() != AssignmentExpression.Operator.ASSIGN) {
                        reportSyntaxError(p, Messages.Key.InvalidDestructuring);
                    }
                    target = destructuringAssignmentTarget(assignment.getLeft());
                    initialiser = assignment.getRight();
                } else {
                    target = destructuringAssignmentTarget(propertyValue);
                    initialiser = null;
                }
                property = new AssignmentProperty(p.getBeginPosition(), p.getEndPosition(),
                        propertyName, target, initialiser);
            } else if (p instanceof PropertyNameDefinition) {
                // AssignmentProperty : Identifier
                PropertyNameDefinition def = (PropertyNameDefinition) p;
                assignmentProperty_StaticSemantics(def.getPropertyName());
                property = new AssignmentProperty(p.getBeginPosition(), p.getEndPosition(),
                        def.getPropertyName(), null);
            } else if (p instanceof CoverInitialisedName) {
                // AssignmentProperty : Identifier Initialiser
                CoverInitialisedName def = (CoverInitialisedName) p;
                assignmentProperty_StaticSemantics(def.getPropertyName());
                property = new AssignmentProperty(p.getBeginPosition(), p.getEndPosition(),
                        def.getPropertyName(), def.getInitialiser());
            } else {
                assert p instanceof MethodDefinition;
                throw reportSyntaxError(p, Messages.Key.InvalidDestructuring);
            }
            list.add(property);
        }
        context.removeLiteral(object);
        ObjectAssignmentPattern pattern = new ObjectAssignmentPattern(object.getBeginPosition(),
                object.getEndPosition(), list);
        if (object.isParenthesised()) {
            pattern.addParentheses();
        }
        return pattern;
    }

    private ArrayAssignmentPattern toDestructuring(ArrayLiteral array) {
        List<AssignmentElementItem> list = newSmallList();
        for (Iterator<Expression> iterator = array.getElements().iterator(); iterator.hasNext();) {
            Expression e = iterator.next();
            AssignmentElementItem element;
            if (e instanceof Elision) {
                // Elision
                element = (Elision) e;
            } else if (e instanceof SpreadElement) {
                // AssignmentRestElement : ... DestructuringAssignmentTarget
                // DestructuringAssignmentTarget : LeftHandSideExpression
                Expression expression = ((SpreadElement) e).getExpression();
                LeftHandSideExpression target = destructuringSimpleAssignmentTarget(expression);
                element = new AssignmentRestElement(e.getBeginPosition(), e.getEndPosition(),
                        target);
                // no further elements after AssignmentRestElement allowed
                if (iterator.hasNext()) {
                    reportSyntaxError(iterator.next(), Messages.Key.InvalidDestructuring);
                }
            } else {
                // AssignmentElement : DestructuringAssignmentTarget Initialiser{opt}
                // DestructuringAssignmentTarget : LeftHandSideExpression
                LeftHandSideExpression target;
                Expression initialiser;
                if (e instanceof AssignmentExpression) {
                    AssignmentExpression assignment = (AssignmentExpression) e;
                    if (assignment.getOperator() != AssignmentExpression.Operator.ASSIGN) {
                        reportSyntaxError(e, Messages.Key.InvalidDestructuring);
                    }
                    target = destructuringAssignmentTarget(assignment.getLeft());
                    initialiser = assignment.getRight();
                } else {
                    target = destructuringAssignmentTarget(e);
                    initialiser = null;
                }
                element = new AssignmentElement(e.getBeginPosition(), e.getEndPosition(), target,
                        initialiser);
            }
            list.add(element);
        }
        ArrayAssignmentPattern pattern = new ArrayAssignmentPattern(array.getBeginPosition(),
                array.getEndPosition(), list);
        if (array.isParenthesised()) {
            pattern.addParentheses();
        }
        return pattern;
    }

    private LeftHandSideExpression destructuringAssignmentTarget(Expression lhs) {
        return destructuringAssignmentTarget(lhs, true);
    }

    private LeftHandSideExpression destructuringSimpleAssignmentTarget(Expression lhs) {
        return destructuringAssignmentTarget(lhs, false);
    }

    private LeftHandSideExpression destructuringAssignmentTarget(Expression lhs, boolean extended) {
        if (lhs instanceof Identifier) {
            String name = ((Identifier) lhs).getName();
            if ("eval".equals(name) || "arguments".equals(name)) {
                reportSyntaxError(lhs, Messages.Key.InvalidAssignmentTarget);
            }
            return (Identifier) lhs;
        } else if (lhs instanceof ElementAccessor) {
            return (ElementAccessor) lhs;
        } else if (lhs instanceof PropertyAccessor) {
            return (PropertyAccessor) lhs;
        } else if (extended && lhs instanceof ObjectAssignmentPattern) {
            return (ObjectAssignmentPattern) lhs;
        } else if (extended && lhs instanceof ArrayAssignmentPattern) {
            return (ArrayAssignmentPattern) lhs;
        } else if (extended && lhs instanceof ObjectLiteral) {
            return toDestructuring((ObjectLiteral) lhs);
        } else if (extended && lhs instanceof ArrayLiteral) {
            return toDestructuring((ArrayLiteral) lhs);
        } else if (lhs instanceof SuperExpression) {
            SuperExpression superExpr = (SuperExpression) lhs;
            if (superExpr.getType() == SuperExpression.Type.ElementAccessor
                    || superExpr.getType() == SuperExpression.Type.PropertyAccessor) {
                return superExpr;
            }
        }
        // everything else => invalid lhs
        throw reportSyntaxError(lhs, Messages.Key.InvalidDestructuring);
    }

    private void assignmentProperty_StaticSemantics(Identifier identifier) {
        switch (identifier.getName()) {
        case "eval":
        case "arguments":
        case "this":
        case "super":
            reportSyntaxError(identifier, Messages.Key.InvalidDestructuring);
        }
    }

    /**
     * <strong>[13.7] The <code>continue</code> Statement</strong>
     * 
     * <pre>
     * ContinueStatement :
     *     continue ;
     *     continue [no <i>LineTerminator</i> here] Identifier ;
     * </pre>
     */
    private ContinueStatement continueStatement() {
        long begin = ts.beginPosition();
        String label;
        consume(Token.CONTINUE);
        if (noLineTerminator() && isIdentifier(token())) {
            label = identifier();
        } else {
            label = null;
        }
        semicolon();

        LabelContext target = findContinueTarget(label);
        if (target == null) {
            if (label == null) {
                reportSyntaxError(begin, Messages.Key.InvalidContinueTarget);
            } else {
                reportSyntaxError(begin, Messages.Key.LabelTargetNotFound, label);
            }
        }
        if (target.type != StatementType.Iteration) {
            reportSyntaxError(begin, Messages.Key.InvalidContinueTarget);
        }
        target.mark(Abrupt.Continue);

        return new ContinueStatement(begin, ts.endPosition(), label);
    }

    /**
     * <strong>[13.8] The <code>break</code> Statement</strong>
     * 
     * <pre>
     * BreakStatement :
     *     break ;
     *     break [no <i>LineTerminator</i> here] Identifier ;
     * </pre>
     */
    private BreakStatement breakStatement() {
        long begin = ts.beginPosition();
        String label;
        consume(Token.BREAK);
        if (noLineTerminator() && isIdentifier(token())) {
            label = identifier();
        } else {
            label = null;
        }
        semicolon();

        LabelContext target = findBreakTarget(label);
        if (target == null) {
            if (label == null) {
                reportSyntaxError(begin, Messages.Key.InvalidBreakTarget);
            } else {
                reportSyntaxError(begin, Messages.Key.LabelTargetNotFound, label);
            }
        }
        target.mark(Abrupt.Break);

        return new BreakStatement(begin, ts.endPosition(), label);
    }

    /**
     * <strong>[13.9] The <code>return</code> Statement</strong>
     * 
     * <pre>
     * ReturnStatement :
     *     return ;
     *     return [no <i>LineTerminator</i> here] Expression ;
     * </pre>
     */
    private ReturnStatement returnStatement() {
        if (!context.returnAllowed) {
            reportSyntaxError(Messages.Key.InvalidReturnStatement);
        }

        long begin = ts.beginPosition();
        Expression expr = null;
        consume(Token.RETURN);
        if (noLineTerminator()
                && !(token() == Token.SEMI || token() == Token.RC || token() == Token.EOF)) {
            expr = expression(true);
        }
        semicolon();

        return new ReturnStatement(begin, ts.endPosition(), expr);
    }

    /**
     * <strong>[13.10] The <code>with</code> Statement</strong>
     * 
     * <pre>
     * WithStatement :
     *     with ( Expression ) Statement
     * </pre>
     */
    private WithStatement withStatement() {
        long begin = ts.beginPosition();
        reportStrictModeSyntaxError(begin, Messages.Key.StrictModeWithStatement);

        consume(Token.WITH);
        consume(Token.LP);
        Expression expr = expression(true);
        consume(Token.RP);

        BlockContext scope = enterWithContext();
        Statement stmt = statement();
        exitWithContext();

        WithStatement withStatement = new WithStatement(begin, ts.endPosition(), scope, expr, stmt);
        scope.node = withStatement;
        return withStatement;
    }

    /**
     * <strong>[13.11] The <code>switch</code> Statement</strong>
     * 
     * <pre>
     * SwitchStatement :
     *     switch ( Expression ) CaseBlock
     * CaseBlock :
     *     { CaseClauses<sub>opt</sub> }
     *     { CaseClauses<sub>opt</sub> DefaultClause CaseClauses<sub>opt</sub> }
     * CaseClauses :
     *     CaseClause
     *     CaseClauses CaseClause
     * CaseClause :
     *     case Expression : StatementList<sub>opt</sub>
     * DefaultClause :
     *     default : StatementList<sub>opt</sub>
     * </pre>
     */
    private SwitchStatement switchStatement(Set<String> labelSet) {
        List<SwitchClause> clauses = newList();
        long begin = ts.beginPosition();
        consume(Token.SWITCH);
        consume(Token.LP);
        Expression expr = expression(true);
        consume(Token.RP);

        consume(Token.LC);
        LabelContext labelCx = enterBreakable(begin, labelSet);
        BlockContext scope = enterBlockContext();
        boolean hasDefault = false;
        for (;;) {
            long beginClause = ts.beginPosition();
            Expression caseExpr;
            Token tok = token();
            if (tok == Token.CASE) {
                consume(Token.CASE);
                caseExpr = expression(true);
                consume(Token.COLON);
            } else if (tok == Token.DEFAULT && !hasDefault) {
                hasDefault = true;
                consume(Token.DEFAULT);
                consume(Token.COLON);
                caseExpr = null;
            } else {
                break;
            }
            List<StatementListItem> list = newList();
            statementlist: for (;;) {
                switch (token()) {
                case CASE:
                case DEFAULT:
                case RC:
                    break statementlist;
                default:
                    list.add(statementListItem());
                }
            }
            clauses.add(new SwitchClause(beginClause, ts.endPosition(), caseExpr, list));
        }
        exitBlockContext();
        exitBreakable();
        consume(Token.RC);

        SwitchStatement switchStatement = new SwitchStatement(begin, ts.endPosition(), scope,
                labelCx.abrupts, labelCx.labelSet, expr, clauses);
        scope.node = switchStatement;
        return switchStatement;
    }

    /**
     * <strong>[13.12] Labelled Statements</strong>
     * 
     * <pre>
     * LabelledStatement :
     *     Identifier : Statement
     * </pre>
     */
    private Statement labelledStatement() {
        long begin = ts.beginPosition();
        LinkedHashSet<String> labelSet = new LinkedHashSet<>(4);
        labels: for (;;) {
            switch (token()) {
            case FOR:
                return forStatement(labelSet);
            case WHILE:
                return whileStatement(labelSet);
            case DO:
                return doWhileStatement(labelSet);
            case SWITCH:
                return switchStatement(labelSet);
            case YIELD:
                if (!isYieldName()) {
                    break labels;
                }
                // fall-through
            case NAME:
                if (LOOKAHEAD(Token.COLON)) {
                    long beginLabel = ts.beginPosition();
                    String name = identifier();
                    consume(Token.COLON);
                    if (!labelSet.add(name)) {
                        reportSyntaxError(beginLabel, Messages.Key.DuplicateLabel, name);
                    }
                    break;
                }
            case LC:
            case VAR:
            case SEMI:
            case IF:
            case CONTINUE:
            case BREAK:
            case RETURN:
            case WITH:
            case THROW:
            case TRY:
            case DEBUGGER:
            default:
                break labels;
            }
        }

        assert !labelSet.isEmpty();

        LabelContext labelCx = enterLabelled(begin, StatementType.Statement, labelSet);
        Statement stmt = statement();
        exitLabelled();

        return new LabelledStatement(begin, ts.endPosition(), labelCx.abrupts, labelCx.labelSet,
                stmt);
    }

    /**
     * <strong>[13.13] The <code>throw</code> Statement</strong>
     * 
     * <pre>
     * ThrowStatement :
     *     throw [no <i>LineTerminator</i> here] Expression ;
     * </pre>
     */
    private ThrowStatement throwStatement() {
        long begin = ts.beginPosition();
        consume(Token.THROW);
        if (!noLineTerminator()) {
            reportSyntaxError(Messages.Key.UnexpectedEndOfLine);
        }
        Expression expr = expression(true);
        semicolon();

        return new ThrowStatement(begin, ts.endPosition(), expr);
    }

    /**
     * <strong>[13.14] The <code>try</code> Statement</strong>
     * 
     * <pre>
     * TryStatement :
     *     try Block Catch
     *     try Block Finally
     *     try Block Catch Finally
     * Catch :
     *     catch ( CatchParameter ) Block
     * Finally :
     *     finally Block
     * CatchParameter :
     *     BindingIdentifier
     *     BindingPattern
     * </pre>
     */
    private TryStatement tryStatement() {
        BlockStatement tryBlock, finallyBlock = null;
        CatchNode catchNode = null;
        List<GuardedCatchNode> guardedCatchNodes = emptyList();
        long begin = ts.beginPosition();
        consume(Token.TRY);
        tryBlock = block(NO_INHERITED_BINDING);
        Token tok = token();
        if (tok == Token.CATCH) {
            if (isEnabled(Option.GuardedCatch)) {
                guardedCatchNodes = newSmallList();
                while (token() == Token.CATCH && catchNode == null) {
                    long beginCatch = ts.beginPosition();
                    consume(Token.CATCH);
                    BlockContext catchScope = enterBlockContext();

                    consume(Token.LP);
                    Binding catchParameter = binding();
                    addLexDeclaredName(catchParameter);

                    Expression guard;
                    if (token() == Token.IF) {
                        consume(Token.IF);
                        guard = expression(true);
                    } else {
                        guard = null;
                    }

                    consume(Token.RP);

                    // catch-block receives a blacklist of forbidden lexical declarable names
                    BlockStatement catchBlock = block(singletonList(catchParameter));

                    exitBlockContext();
                    if (guard != null) {
                        GuardedCatchNode guardedCatchNode = new GuardedCatchNode(beginCatch,
                                ts.endPosition(), catchScope, catchParameter, guard, catchBlock);
                        catchScope.node = guardedCatchNode;
                        guardedCatchNodes.add(guardedCatchNode);
                    } else {
                        catchNode = new CatchNode(beginCatch, ts.endPosition(), catchScope,
                                catchParameter, catchBlock);
                        catchScope.node = catchNode;
                    }
                }
            } else {
                long beginCatch = ts.beginPosition();
                consume(Token.CATCH);
                BlockContext catchScope = enterBlockContext();

                consume(Token.LP);
                Binding catchParameter = binding();
                addLexDeclaredName(catchParameter);
                consume(Token.RP);

                // catch-block receives a blacklist of forbidden lexical declarable names
                BlockStatement catchBlock = block(singletonList(catchParameter));

                exitBlockContext();
                catchNode = new CatchNode(beginCatch, ts.endPosition(), catchScope, catchParameter,
                        catchBlock);
                catchScope.node = catchNode;
            }

            if (token() == Token.FINALLY) {
                consume(Token.FINALLY);
                finallyBlock = block(NO_INHERITED_BINDING);
            }
        } else {
            consume(Token.FINALLY);
            finallyBlock = block(NO_INHERITED_BINDING);
        }

        return new TryStatement(begin, ts.endPosition(), tryBlock, catchNode, guardedCatchNodes,
                finallyBlock);
    }

    /**
     * <strong>[13.15] The <code>debugger</code> Statement</strong>
     * 
     * <pre>
     * DebuggerStatement :
     *     debugger ;
     * </pre>
     */
    private DebuggerStatement debuggerStatement() {
        long begin = ts.beginPosition();
        consume(Token.DEBUGGER);
        semicolon();

        return new DebuggerStatement(begin, ts.endPosition());
    }

    /**
     * <strong>[Extension] The <code>let</code> Statement</strong>
     * 
     * <pre>
     * LetStatement :
     *     let ( BindingList ) BlockStatement
     * </pre>
     */
    private Statement letStatement() {
        BlockContext scope = enterBlockContext();
        long begin = ts.beginPosition();
        consume(Token.LET);

        consume(Token.LP);
        List<LexicalBinding> bindings = bindingList(false, true);
        consume(Token.RP);

        if (token() != Token.LC && isEnabled(Option.LetExpression)) {
            // let expression disguised as let statement - also error in strict mode(!)
            reportStrictModeSyntaxError(begin, Messages.Key.UnexpectedToken, token().toString());
            Expression expression = assignmentExpression(true);

            exitBlockContext();

            LetExpression letExpression = new LetExpression(begin, ts.endPosition(), scope,
                    bindings, expression);
            scope.node = letExpression;
            return new ExpressionStatement(begin, ts.endPosition(), letExpression);
        } else {
            BlockStatement letBlock = block(toBindings(bindings));

            exitBlockContext();

            LetStatement block = new LetStatement(begin, ts.endPosition(), scope, bindings,
                    letBlock);
            scope.node = block;
            return block;
        }
    }

    private List<Binding> toBindings(List<LexicalBinding> lexicalBindings) {
        ArrayList<Binding> bindings = new ArrayList<>(lexicalBindings.size());
        for (LexicalBinding lexicalBinding : lexicalBindings) {
            bindings.add(lexicalBinding.getBinding());
        }
        return bindings;
    }

    /* ***************************************************************************************** */

    /**
     * <strong>[12.1] Primary Expressions</strong>
     * 
     * <pre>
     * PrimaryExpresion :
     *     this
     *     Identifier
     *     Literal
     *     ArrayInitialiser
     *     ObjectLiteral
     *     FunctionExpression
     *     ClassExpression
     *     GeneratorExpression
     *     GeneratorComprehension
     *     RegularExpressionLiteral
     *     TemplateLiteral
     *     CoverParenthesisedExpressionAndArrowParameterList
     * Literal :
     *     NullLiteral
     *     ValueLiteral
     * ValueLiteral :
     *     BooleanLiteral
     *     NumericLiteral
     *     StringLiteral
     * </pre>
     */
    private Expression primaryExpression() {
        long begin = ts.beginPosition();
        Token tok = token();
        switch (tok) {
        case THIS:
            consume(tok);
            return new ThisExpression(begin, ts.endPosition());
        case NULL:
            consume(tok);
            return new NullLiteral(begin, ts.endPosition());
        case FALSE:
        case TRUE:
            consume(tok);
            return new BooleanLiteral(begin, ts.endPosition(), tok == Token.TRUE);
        case NUMBER:
            double number = numericLiteral();
            return new NumericLiteral(begin, ts.endPosition(), number);
        case STRING:
            String string = stringLiteral();
            return new StringLiteral(begin, ts.endPosition(), string);
        case DIV:
        case ASSIGN_DIV:
            return regularExpressionLiteral(tok);
        case LB:
            return arrayInitialiser();
        case LC:
            return objectLiteral();
        case FUNCTION:
            return functionOrGeneratorExpression();
        case CLASS:
            return classExpression();
        case LP:
            if (LOOKAHEAD(Token.FOR)) {
                return generatorComprehension();
            } else {
                return coverParenthesisedExpressionAndArrowParameterList();
            }
        case TEMPLATE:
            return templateLiteral(false);
        case LET:
            if (isEnabled(Option.LetExpression)) {
                return letExpression();
            }
        default:
            String ident = identifier();
            return new Identifier(begin, ts.endPosition(), ident);
        }
    }

    private Expression functionOrGeneratorExpression() {
        if (LOOKAHEAD(Token.MUL)) {
            return generatorExpression(false);
        } else {
            long position = ts.position(), lineinfo = ts.lineinfo();
            try {
                return functionExpression();
            } catch (RetryGenerator e) {
                ts.reset(position, lineinfo);
                return generatorExpression(true);
            }
        }
    }

    /**
     * <strong>[12.1] Primary Expressions</strong>
     * 
     * <pre>
     * CoverParenthesisedExpressionAndArrowParameterList :
     *     ( Expression )
     *     ( )
     *     ( ... Identifier )
     *     ( Expression , ... Identifier)
     * </pre>
     */
    private Expression coverParenthesisedExpressionAndArrowParameterList() {
        long position = ts.position(), lineinfo = ts.lineinfo();
        consume(Token.LP);
        Expression expr;
        if (token() == Token.RP) {
            expr = arrowFunctionEmptyParameters();
        } else if (token() == Token.TRIPLE_DOT) {
            expr = arrowFunctionRestParameter();
        } else {
            // inlined `expression(true)`
            expr = assignmentExpressionNoValidation(true);
            if (token() == Token.FOR && isEnabled(Option.LegacyComprehension)) {
                ts.reset(position, lineinfo);
                return legacyGeneratorComprehension();
            }
            if (token() == Token.COMMA) {
                List<Expression> list = new ArrayList<>();
                list.add(expr);
                while (token() == Token.COMMA) {
                    consume(Token.COMMA);
                    if (token() == Token.TRIPLE_DOT) {
                        list.add(arrowFunctionRestParameter());
                        break;
                    }
                    expr = assignmentExpression(true);
                    list.add(expr);
                }
                expr = new CommaExpression(list);
            }
        }
        expr.addParentheses();
        consume(Token.RP);
        return expr;
    }

    private EmptyExpression arrowFunctionEmptyParameters() {
        if (!(token() == Token.RP && LOOKAHEAD(Token.ARROW))) {
            reportSyntaxError(Messages.Key.EmptyParenthesisedExpression);
        }
        return new EmptyExpression(0, 0);
    }

    private SpreadElement arrowFunctionRestParameter() {
        long begin = ts.beginPosition();
        consume(Token.TRIPLE_DOT);
        String ident = identifier();
        Identifier identifier = new Identifier(ts.beginPosition(), ts.endPosition(), ident);
        SpreadElement spread = new SpreadElement(begin, ts.endPosition(), identifier);
        if (!(token() == Token.RP && LOOKAHEAD(Token.ARROW))) {
            reportSyntaxError(spread, Messages.Key.InvalidSpreadExpression);
        }
        return spread;
    }

    /**
     * <strong>[12.1.4] Array Initialiser</strong>
     * 
     * <pre>
     * ArrayInitialiser :
     *     ArrayLiteral
     *     ArrayComprehension
     * </pre>
     */
    private ArrayInitialiser arrayInitialiser() {
        if (LOOKAHEAD(Token.FOR)) {
            return arrayComprehension();
        } else {
            long begin = ts.beginPosition();
            if (isEnabled(Option.LegacyComprehension)) {
                switch (peek()) {
                case RB:
                case COMMA:
                case TRIPLE_DOT:
                    break;
                default:
                    // TODO: report eclipse formatter bug
                    long position = ts.position(),
                    lineinfo = ts.lineinfo();
                    consume(Token.LB);
                    Expression expression = assignmentExpressionNoValidation(true);
                    if (token() == Token.FOR) {
                        ts.reset(position, lineinfo);
                        return legacyArrayComprehension();
                    }
                    return arrayLiteral(begin, expression);
                }
            }
            return arrayLiteral(begin, null);
        }
    }

    /**
     * <strong>[12.1.4] Array Initialiser</strong>
     * 
     * <pre>
     * ArrayLiteral :
     *     [ Elision<sub>opt</sub> ]
     *     [ ElementList ]
     *     [ ElementList , Elision<sub>opt</sub> ]
     * ElementList :
     *     Elision<sub>opt</sub> AssignmentExpression
     *     Elision<sub>opt</sub> SpreadElement
     *     ElementList , Elision<sub>opt</sub> AssignmentExpression
     *     ElementList , Elision<sub>opt</sub> SpreadElement
     * Elision :
     *     ,
     *     Elision ,
     * SpreadElement :
     *     ... AssignmentExpression
     * </pre>
     */
    private ArrayLiteral arrayLiteral(long begin, Expression expr) {
        List<Expression> list = newList();
        boolean needComma = false;
        if (expr == null) {
            consume(Token.LB);
        } else {
            list.add(expr);
            needComma = true;
        }
        for (Token tok; (tok = token()) != Token.RB;) {
            if (needComma) {
                consume(Token.COMMA);
                needComma = false;
            } else if (tok == Token.COMMA) {
                consume(Token.COMMA);
                list.add(new Elision(0, 0));
            } else if (tok == Token.TRIPLE_DOT) {
                long beginSpread = ts.beginPosition();
                consume(Token.TRIPLE_DOT);
                Expression expression = assignmentExpression(true);
                list.add(new SpreadElement(beginSpread, ts.endPosition(), expression));
                needComma = true;
            } else {
                list.add(assignmentExpressionNoValidation(true));
                needComma = true;
            }
        }
        consume(Token.RB);

        return new ArrayLiteral(begin, ts.endPosition(), list);
    }

    /**
     * <strong>[12.1.4.2] Array Comprehension</strong>
     * 
     * <pre>
     * ArrayComprehension :
     *     [ Comprehension ]
     * </pre>
     */
    private ArrayComprehension arrayComprehension() {
        long begin = ts.beginPosition();
        consume(Token.LB);
        Comprehension comprehension = comprehension();
        consume(Token.RB);

        return new ArrayComprehension(begin, ts.endPosition(), comprehension);
    }

    /**
     * <strong>[12.1.4.2] Array Comprehension</strong>
     * 
     * <pre>
     * Comprehension :
     *     ComprehensionFor ComprehensionTail
     * ComprehensionTail :
     *     AssignmentExpression
     *     ComprehensionFor ComprehensionTail
     *     ComprehensionIf ComprehensionTail
     * </pre>
     */
    private Comprehension comprehension() {
        assert token() == Token.FOR;
        List<ComprehensionQualifier> list = newSmallList();
        int scopes = 0;
        for (;;) {
            ComprehensionQualifier qualifier;
            if (token() == Token.FOR) {
                scopes += 1;
                qualifier = comprehensionFor();
            } else if (token() == Token.IF) {
                qualifier = comprehensionIf();
            } else {
                break;
            }
            list.add(qualifier);
        }
        Expression expression = assignmentExpression(true);
        while (scopes-- > 0) {
            exitBlockContext();
        }
        return new Comprehension(list, expression);
    }

    /**
     * <strong>[12.1.4.2] Array Comprehension</strong>
     * 
     * <pre>
     * ComprehensionFor :
     *     for ( ForBinding of AssignmentExpression )
     * ForBinding :
     *     BindingIdentifier
     *     BindingPattern
     * </pre>
     */
    private ComprehensionFor comprehensionFor() {
        long begin = ts.beginPosition();
        consume(Token.FOR);
        consume(Token.LP);
        Binding b = binding();
        consume("of");
        Expression expression = assignmentExpression(true);
        consume(Token.RP);
        BlockContext scope = enterBlockContext();
        addLexDeclaredName(b);
        return new ComprehensionFor(begin, ts.endPosition(), scope, b, expression);
    }

    /**
     * <strong>[12.1.4.2] Array Comprehension</strong>
     * 
     * <pre>
     * ComprehensionIf :
     *     if ( AssignmentExpression )
     * </pre>
     */
    private ComprehensionIf comprehensionIf() {
        long begin = ts.beginPosition();
        consume(Token.IF);
        consume(Token.LP);
        Expression expression = assignmentExpression(true);
        consume(Token.RP);
        return new ComprehensionIf(begin, ts.endPosition(), expression);
    }

    /**
     * <strong>[12.1.4.2] Array Comprehension</strong>
     * 
     * <pre>
     * LegacyArrayComprehension :
     *     [ LegacyComprehension ]
     * </pre>
     */
    private ArrayComprehension legacyArrayComprehension() {
        long begin = ts.beginPosition();
        consume(Token.LB);
        LegacyComprehension comprehension = legacyComprehension();
        consume(Token.RB);
        return new ArrayComprehension(begin, ts.endPosition(), comprehension);
    }

    /**
     * <strong>[12.1.4.2] Array Comprehension</strong>
     * 
     * <pre>
     * LegacyComprehension :
     *     AssignmentExpression LegacyComprehensionForList LegacyComprehensionIf<sub>opt</sub>
     * LegacyComprehensionForList :
     *     LegacyComprehensionFor LegacyComprehensionForList<sub>opt</sub>
     * LegacyComprehensionFor :
     *     for ( ForBinding of Expression )
     *     for ( ForBinding in Expression )
     *     for each ( ForBinding in Expression )
     * LegacyComprehensionIf :
     *     if ( Expression )
     * </pre>
     */
    private LegacyComprehension legacyComprehension() {
        BlockContext scope = enterBlockContext();
        Expression expr = assignmentExpression(true);

        assert token() == Token.FOR : "empty legacy comprehension";

        List<ComprehensionQualifier> list = newSmallList();
        while (token() == Token.FOR) {
            long begin = ts.beginPosition();
            consume(Token.FOR);
            boolean each = false;
            if (token() != Token.LP && isName("each")) {
                consume("each");
                each = true;
            }
            consume(Token.LP);
            Binding b = binding();
            addLexDeclaredName(b);

            LegacyComprehensionFor.IterationKind iterationKind;
            if (each) {
                consume(Token.IN);
                iterationKind = LegacyComprehensionFor.IterationKind.EnumerateValues;
            } else if (token() == Token.IN) {
                consume(Token.IN);
                iterationKind = LegacyComprehensionFor.IterationKind.Enumerate;
            } else {
                consume("of");
                iterationKind = LegacyComprehensionFor.IterationKind.Iterate;
            }
            Expression expression = expression(true);
            consume(Token.RP);

            list.add(new LegacyComprehensionFor(begin, ts.endPosition(), iterationKind, b,
                    expression));
        }

        if (token() == Token.IF) {
            long begin = ts.beginPosition();
            consume(Token.IF);
            consume(Token.LP);
            Expression expression = expression(true);
            consume(Token.RP);
            list.add(new ComprehensionIf(begin, ts.endPosition(), expression));
        }

        exitBlockContext();

        return new LegacyComprehension(scope, list, expr);
    }

    /**
     * <strong>[12.1.5] Object Initialiser</strong>
     * 
     * <pre>
     * ObjectLiteral :
     *     { }
     *     { PropertyDefinitionList }
     *     { PropertyDefinitionList , }
     * PropertyDefinitionList :
     *     PropertyDefinition
     *     PropertyDefinitionList , PropertyDefinition
     * </pre>
     */
    private ObjectLiteral objectLiteral() {
        long begin = ts.beginPosition();
        List<PropertyDefinition> defs = newList();
        consume(Token.LC);
        while (token() != Token.RC) {
            defs.add(propertyDefinition());
            if (token() == Token.COMMA) {
                consume(Token.COMMA);
            } else {
                break;
            }
        }
        consume(Token.RC);
        ObjectLiteral object = new ObjectLiteral(begin, ts.endPosition(), defs);
        context.addLiteral(object);
        return object;
    }

    private void objectLiteral_StaticSemantics(int oldCount) {
        ArrayDeque<ObjectLiteral> literals = context.objectLiterals;
        for (int i = oldCount, newCount = literals.size(); i < newCount; ++i) {
            objectLiteral_StaticSemantics(literals.pop());
        }
    }

    private void objectLiteral_StaticSemantics(ObjectLiteral object) {
        final int VALUE = 0, GETTER = 1, SETTER = 2, SPECIAL = 4;
        Map<String, Integer> values = new HashMap<>();
        for (PropertyDefinition def : object.getProperties()) {
            PropertyName propertyName = def.getPropertyName();
            String key = propertyName.getName();
            if (key == null) {
                assert propertyName instanceof ComputedPropertyName;
                continue;
            }
            final int kind;
            if (def instanceof PropertyValueDefinition) {
                kind = VALUE;
            } else if (def instanceof PropertyNameDefinition) {
                kind = SPECIAL;
            } else if (def instanceof MethodDefinition) {
                MethodDefinition method = (MethodDefinition) def;
                if (method.hasSuperReference()) {
                    reportSyntaxError(def, Messages.Key.SuperOutsideClass);
                }
                MethodDefinition.MethodType type = method.getType();
                kind = type == MethodType.Getter ? GETTER : type == MethodType.Setter ? SETTER
                        : SPECIAL;
            } else {
                assert def instanceof CoverInitialisedName;
                // Always throw a Syntax Error if this production is present
                throw reportSyntaxError(def, Messages.Key.MissingColonAfterPropertyId, key);
            }
            // It is a Syntax Error if PropertyNameList of PropertyDefinitionList contains any
            // duplicate entries [...]
            if (values.containsKey(key)) {
                int prev = values.get(key);
                if (kind == VALUE && prev != VALUE) {
                    reportSyntaxError(def, Messages.Key.DuplicatePropertyDefinition, key);
                }
                if (kind == VALUE && prev == VALUE) {
                    reportStrictModeSyntaxError(def, Messages.Key.DuplicatePropertyDefinition, key);
                }
                if (kind == GETTER && prev != SETTER) {
                    reportSyntaxError(def, Messages.Key.DuplicatePropertyDefinition, key);
                }
                if (kind == SETTER && prev != GETTER) {
                    reportSyntaxError(def, Messages.Key.DuplicatePropertyDefinition, key);
                }
                if (kind == SPECIAL) {
                    reportSyntaxError(def, Messages.Key.DuplicatePropertyDefinition, key);
                }
                values.put(key, prev | kind);
            } else {
                values.put(key, kind);
            }
        }
    }

    /**
     * <strong>[12.1.5] Object Initialiser</strong>
     * 
     * <pre>
     * PropertyDefinition :
     *     IdentifierName
     *     CoverInitialisedName
     *     PropertyName : AssignmentExpression
     *     MethodDefinition
     * CoverInitialisedName :
     *     IdentifierName Initialiser
     * </pre>
     */
    private PropertyDefinition propertyDefinition() {
        long begin = ts.beginPosition();
        if (token() == Token.LB) {
            // either `PropertyName : AssignmentExpression` or MethodDefinition (normal)
            PropertyName propertyName = computedPropertyName();
            if (token() == Token.COLON) {
                // it's the `PropertyName : AssignmentExpression` case
                consume(Token.COLON);
                Expression propertyValue = assignmentExpressionNoValidation(true);
                return new PropertyValueDefinition(begin, ts.endPosition(), propertyName,
                        propertyValue);
            }
            // otherwise it's MethodDefinition (normal)
            return normalMethod(begin, propertyName, false);
        }
        if (LOOKAHEAD(Token.COLON)) {
            PropertyName propertyName = literalPropertyName();
            consume(Token.COLON);
            Expression propertyValue = assignmentExpressionNoValidation(true);
            return new PropertyValueDefinition(begin, ts.endPosition(), propertyName, propertyValue);
        }
        if (LOOKAHEAD(Token.COMMA) || LOOKAHEAD(Token.RC)) {
            // Static Semantics: It is a Syntax Error if IdentifierName is a
            // ReservedWord.
            String ident = identifier();
            Identifier identifier = new Identifier(begin, ts.endPosition(), ident);
            return new PropertyNameDefinition(begin, ts.endPosition(), identifier);
        }
        if (LOOKAHEAD(Token.ASSIGN)) {
            String ident = identifier();
            Identifier identifier = new Identifier(begin, ts.endPosition(), ident);
            consume(Token.ASSIGN);
            Expression initialiser = assignmentExpression(true);
            return new CoverInitialisedName(begin, ts.endPosition(), identifier, initialiser);
        }
        return methodDefinition(false);
    }

    /**
     * <strong>[12.1.5] Object Initialiser</strong>
     * 
     * <pre>
     * PropertyName :
     *   LiteralPropertyName
     *   ComputedPropertyName
     * </pre>
     */
    private PropertyName propertyName() {
        if (token() != Token.LB) {
            return literalPropertyName();
        } else {
            return computedPropertyName();
        }
    }

    /**
     * <strong>[12.1.5] Object Initialiser</strong>
     * 
     * <pre>
     * PropertyName :
     *     IdentifierName
     *     StringLiteral
     *     NumericLiteral
     * </pre>
     */
    private PropertyName literalPropertyName() {
        long begin = ts.beginPosition();
        switch (token()) {
        case STRING:
            String string = stringLiteral();
            return new StringLiteral(begin, ts.endPosition(), string);
        case NUMBER:
            double number = numericLiteral();
            return new NumericLiteral(begin, ts.endPosition(), number);
        default:
            String ident = identifierName();
            return new Identifier(begin, ts.endPosition(), ident);
        }
    }

    /**
     * <strong>[12.1.5] Object Initialiser</strong>
     * 
     * <pre>
     * ComputedPropertyName :
     *     [ AssignmentExpression ]
     * </pre>
     */
    private PropertyName computedPropertyName() {
        long begin = ts.beginPosition();
        consume(Token.LB);
        Expression expression = assignmentExpression(true);
        consume(Token.RB);

        return new ComputedPropertyName(begin, ts.endPosition(), expression);
    }

    /**
     * <strong>[12.1.7] Generator Comprehensions</strong>
     * 
     * <pre>
     * GeneratorComprehension :
     *     ( Comprehension )
     * </pre>
     */
    private GeneratorComprehension generatorComprehension() {
        boolean yieldAllowed = context.yieldAllowed;
        try {
            context.yieldAllowed = false;
            long begin = ts.beginPosition();
            consume(Token.LP);
            Comprehension comprehension = comprehension();
            consume(Token.RP);

            return new GeneratorComprehension(begin, ts.endPosition(), comprehension);
        } finally {
            context.yieldAllowed = yieldAllowed;
        }
    }

    /**
     * <strong>[12.1.7] Generator Comprehensions</strong>
     * 
     * <pre>
     * LegacyGeneratorComprehension :
     *     ( LegacyComprehension )
     * </pre>
     */
    private GeneratorComprehension legacyGeneratorComprehension() {
        boolean yieldAllowed = context.yieldAllowed;
        try {
            context.yieldAllowed = false;
            long begin = ts.beginPosition();
            consume(Token.LP);
            LegacyComprehension comprehension = legacyComprehension();
            consume(Token.RP);

            return new GeneratorComprehension(begin, ts.endPosition(), comprehension);
        } finally {
            context.yieldAllowed = yieldAllowed;
        }
    }

    /**
     * <strong>[12.1.8] Regular Expression Literals</strong>
     * 
     * <pre>
     * RegularExpressionLiteral ::
     *     / RegularExpressionBody / RegularExpressionFlags
     * </pre>
     */
    private Expression regularExpressionLiteral(Token tok) {
        long begin = ts.beginPosition();
        String[] re = ts.readRegularExpression(tok);
        regularExpressionLiteral_StaticSemantics(begin, re[0], re[1]);
        consume(tok);
        return new RegularExpressionLiteral(begin, ts.endPosition(), re[0], re[1]);
    }

    private void regularExpressionLiteral_StaticSemantics(long sourcePos, String p, String f) {
        // parse to validate regular expression, but ignore actual result
        RegExpParser.parse(p, f, sourceFile, toLine(sourcePos), toColumn(sourcePos));
    }

    /**
     * <strong>[12.1.9] Template Literals</strong>
     * 
     * <pre>
     * TemplateLiteral :
     *     NoSubstitutionTemplate
     *     TemplateHead Expression [Lexical goal <i>InputElementTemplateTail</i>] TemplateSpans
     * TemplateSpans :
     *     TemplateTail
     *     TemplateMiddleList [Lexical goal <i>InputElementTemplateTail</i>] TemplateTail
     * TemplateMiddleList :
     *     TemplateMiddle Expression
     *     TemplateMiddleList [Lexical goal <i>InputElementTemplateTail</i>] TemplateMiddle Expression
     * </pre>
     */
    private TemplateLiteral templateLiteral(boolean tagged) {
        List<Expression> elements = newList();

        long begin = ts.beginPosition();
        templateCharacters(elements, Token.TEMPLATE);
        while (token() == Token.LC) {
            consume(Token.LC);
            elements.add(expression(true));
            templateCharacters(elements, Token.RC);
        }
        consume(Token.TEMPLATE);

        if ((elements.size() / 2) + 1 > MAX_ARGUMENTS) {
            reportSyntaxError(Messages.Key.FunctionTooManyArguments);
        }

        return new TemplateLiteral(begin, ts.endPosition(), tagged, elements);
    }

    private void templateCharacters(List<Expression> elements, Token start) {
        long begin = ts.beginPosition();
        String[] values = ts.readTemplateLiteral(start);
        elements.add(new TemplateCharacters(begin, ts.endPosition(), values[0], values[1]));
    }

    /**
     * <strong>[Extension] The <code>let</code> Expression</strong>
     * 
     * <pre>
     * LetExpression :
     *     let ( BindingList ) AssignmentExpression
     * </pre>
     */
    private LetExpression letExpression() {
        BlockContext scope = enterBlockContext();
        long begin = ts.beginPosition();
        consume(Token.LET);

        consume(Token.LP);
        List<LexicalBinding> bindings = bindingList(false, true);
        consume(Token.RP);

        Expression expression = assignmentExpression(true);

        exitBlockContext();

        LetExpression letExpression = new LetExpression(begin, ts.endPosition(), scope, bindings,
                expression);
        scope.node = letExpression;

        return letExpression;
    }

    /**
     * <strong>[12.2] Left-Hand-Side Expressions</strong>
     * 
     * <pre>
     * MemberExpression :
     *     PrimaryExpression
     *     MemberExpression [ Expression ]
     *     MemberExpression . IdentifierName
     *     MemberExpression QuasiLiteral
     *     super [ Expression ]
     *     super . IdentifierName
     *     new MemberExpression Arguments
     * NewExpression :
     *     MemberExpression
     *     new NewExpression
     * CallExpression :
     *     MemberExpression Arguments
     *     super Arguments
     *     CallExpression Arguments
     *     CallExpression [ Expression ]
     *     CallExpression . IdentifierName
     *     CallExpression QuasiLiteral
     * LeftHandSideExpression :
     *     NewExpression
     *     CallExpression
     * </pre>
     */
    private Expression leftHandSideExpression(boolean allowCall) {
        long begin = ts.beginPosition();
        Expression lhs;
        if (token() == Token.NEW) {
            consume(Token.NEW);
            Expression expr = leftHandSideExpression(false);
            List<Expression> args = null;
            if (token() == Token.LP) {
                args = arguments();
            } else {
                args = emptyList();
            }
            lhs = new NewExpression(begin, ts.endPosition(), expr, args);
        } else if (token() == Token.SUPER) {
            ParseContext cx = context.findSuperContext();
            if (cx.kind == ContextKind.Script && !isEnabled(Option.FunctionCode)
                    || cx.kind == ContextKind.Module) {
                reportSyntaxError(Messages.Key.InvalidSuperExpression);
            }
            cx.setReferencesSuper();

            consume(Token.SUPER);
            switch (token()) {
            case DOT:
                consume(Token.DOT);
                String name = identifierName();
                lhs = new SuperExpression(begin, ts.endPosition(), name);
                break;
            case LB:
                consume(Token.LB);
                Expression expr = expression(true);
                consume(Token.RB);
                lhs = new SuperExpression(begin, ts.endPosition(), expr);
                break;
            case LP:
                if (!allowCall) {
                    lhs = new SuperExpression(begin, ts.endPosition());
                } else {
                    List<Expression> args = arguments();
                    lhs = new SuperExpression(begin, ts.endPosition(), args);
                }
                break;
            case TEMPLATE:
                // handle "new super``" case
                throw reportSyntaxError(Messages.Key.InvalidToken, token().toString());
            default:
                if (!allowCall) {
                    lhs = new SuperExpression(begin, ts.endPosition());
                } else {
                    throw reportSyntaxError(Messages.Key.InvalidToken, token().toString());
                }
                break;
            }
        } else {
            lhs = primaryExpression();
        }

        for (;;) {
            switch (token()) {
            case DOT:
                begin = ts.beginPosition();
                consume(Token.DOT);
                String name = identifierName();
                lhs = new PropertyAccessor(begin, ts.endPosition(), lhs, name);
                break;
            case LB:
                begin = ts.beginPosition();
                consume(Token.LB);
                Expression expr = expression(true);
                consume(Token.RB);
                lhs = new ElementAccessor(begin, ts.endPosition(), lhs, expr);
                break;
            case LP:
                if (!allowCall) {
                    return lhs;
                }
                if (lhs instanceof Identifier && "eval".equals(((Identifier) lhs).getName())) {
                    context.funContext.directEval = true;
                }
                begin = ts.beginPosition();
                List<Expression> args = arguments();
                lhs = new CallExpression(begin, ts.endPosition(), lhs, args);
                break;
            case TEMPLATE:
                begin = ts.beginPosition();
                TemplateLiteral templ = templateLiteral(true);
                lhs = new TemplateCallExpression(begin, ts.endPosition(), lhs, templ);
                break;
            default:
                return lhs;
            }
        }
    }

    /**
     * <strong>[12.2] Left-Hand-Side Expressions</strong>
     * 
     * <pre>
     * Arguments :
     *     ()
     *     ( ArgumentList )
     * ArgumentList :
     *     AssignmentExpression
     *     ... AssignmentExpression
     *     ArgumentList , AssignmentExpression
     *     ArgumentList , ... AssignmentExpression
     * </pre>
     */
    private List<Expression> arguments() {
        List<Expression> args = newSmallList();
        long position = ts.position(), lineinfo = ts.lineinfo();
        consume(Token.LP);
        if (token() != Token.RP) {
            if (token() != Token.TRIPLE_DOT && isEnabled(Option.LegacyComprehension)) {
                Expression expr = assignmentExpression(true);
                if (token() == Token.FOR) {
                    ts.reset(position, lineinfo);
                    args.add(legacyGeneratorComprehension());
                    return args;
                }
                args.add(expr);
                if (token() == Token.COMMA) {
                    consume(Token.COMMA);
                } else {
                    consume(Token.RP);
                    return args;
                }
            }

            for (;;) {
                Expression expr;
                if (token() == Token.TRIPLE_DOT) {
                    long begin = ts.beginPosition();
                    consume(Token.TRIPLE_DOT);
                    Expression e = assignmentExpression(true);
                    expr = new CallSpreadElement(begin, ts.endPosition(), e);
                } else {
                    expr = assignmentExpression(true);
                }
                args.add(expr);
                if (token() == Token.COMMA) {
                    consume(Token.COMMA);
                } else {
                    break;
                }
            }

            if (args.size() > MAX_ARGUMENTS) {
                reportSyntaxError(Messages.Key.FunctionTooManyArguments);
            }
        }
        consume(Token.RP);

        return args;
    }

    /**
     * <strong>[12.3] Postfix Expressions</strong><br>
     * <strong>[12.4] Unary Operators</strong>
     * 
     * <pre>
     * PostfixExpression :
     *     LeftHandSideExpression
     *     LeftHandSideExpression [no <i>LineTerminator</i> here] ++
     *     LeftHandSideExpression [no <i>LineTerminator</i> here] --
     * UnaryExpression :
     *     PostfixExpression
     *     delete UnaryExpression
     *     void UnaryExpression
     *     typeof UnaryExpression
     *     ++ UnaryExpression
     *     -- UnaryExpression
     *     + UnaryExpression
     *     - UnaryExpression
     *     ~ UnaryExpression
     *     ! UnaryExpression
     * </pre>
     */
    private Expression unaryExpression() {
        long begin = ts.beginPosition();
        Token tok = token();
        switch (tok) {
        case DELETE:
        case VOID:
        case TYPEOF:
        case INC:
        case DEC:
        case ADD:
        case SUB:
        case BITNOT:
        case NOT: {
            consume(tok);
            Expression operand = unaryExpression();
            UnaryExpression unary = new UnaryExpression(begin, ts.endPosition(),
                    unaryOp(tok, false), operand);
            if (tok == Token.INC || tok == Token.DEC) {
                validateSimpleAssignment(operand, ExceptionType.ReferenceError,
                        Messages.Key.InvalidIncDecTarget);
            }
            if (tok == Token.DELETE) {
                if (operand instanceof Identifier) {
                    reportStrictModeSyntaxError(unary, Messages.Key.StrictModeInvalidDeleteOperand);
                }
            }
            return unary;
        }
        default: {
            Expression lhs = leftHandSideExpression(true);
            if (noLineTerminator()) {
                tok = token();
                if (tok == Token.INC || tok == Token.DEC) {
                    validateSimpleAssignment(lhs, ExceptionType.ReferenceError,
                            Messages.Key.InvalidIncDecTarget);
                    consume(tok);
                    return new UnaryExpression(begin, ts.endPosition(), unaryOp(tok, true), lhs);
                }
            }
            return lhs;
        }
        }
    }

    private static UnaryExpression.Operator unaryOp(Token tok, boolean postfix) {
        switch (tok) {
        case DELETE:
            return UnaryExpression.Operator.DELETE;
        case VOID:
            return UnaryExpression.Operator.VOID;
        case TYPEOF:
            return UnaryExpression.Operator.TYPEOF;
        case INC:
            return postfix ? UnaryExpression.Operator.POST_INC : UnaryExpression.Operator.PRE_INC;
        case DEC:
            return postfix ? UnaryExpression.Operator.POST_DEC : UnaryExpression.Operator.PRE_DEC;
        case ADD:
            return UnaryExpression.Operator.POS;
        case SUB:
            return UnaryExpression.Operator.NEG;
        case BITNOT:
            return UnaryExpression.Operator.BITNOT;
        case NOT:
            return UnaryExpression.Operator.NOT;
        default:
            throw new IllegalStateException();
        }
    }

    /**
     * <strong>[12.5] Multiplicative Operators</strong><br>
     * <strong>[12.6] Additive Operators</strong><br>
     * <strong>[12.7] Bitwise Shift Operators</strong><br>
     * <strong>[12.8] Relational Operators</strong><br>
     * <strong>[12.9] Equality Operators</strong><br>
     * <strong>[12.10] Binary Bitwise Operators</strong><br>
     * <strong>[12.11] Binary Logical Operators</strong><br>
     * 
     * <pre>
     * MultiplicativeExpression :
     *     UnaryExpression
     *     MultiplicativeExpression * UnaryExpression
     *     MultiplicativeExpression / UnaryExpression
     *     MultiplicativeExpression % UnaryExpression
     * AdditiveExpression :
     *     MultiplicativeExpression
     *     AdditiveExpression + MultiplicativeExpression
     *     AdditiveExpression - MultiplicativeExpression
     * ShiftExpression :
     *     AdditiveExpression
     *     ShiftExpression << AdditiveExpression
     *     ShiftExpression >> AdditiveExpression
     *     ShiftExpression >>> AdditiveExpression
     * RelationalExpression :
     *     ShiftExpression
     *     RelationalExpression < ShiftExpression
     *     RelationalExpression > ShiftExpression
     *     RelationalExpression <= ShiftExpression
     *     RelationalExpression >= ShiftExpression
     *     RelationalExpression instanceof ShiftExpression
     *     RelationalExpression in ShiftExpression
     * RelationalExpressionNoIn :
     *     ShiftExpressionNoIn
     *     RelationalExpressionNoIn < ShiftExpression
     *     RelationalExpressionNoIn > ShiftExpression
     *     RelationalExpressionNoIn <= ShiftExpression
     *     RelationalExpressionNoIn >= ShiftExpression
     *     RelationalExpressionNoIn instanceof ShiftExpression
     * EqualityExpression :
     *     RelationalExpression
     *     EqualityExpression == RelationalExpression
     *     EqualityExpression != RelationalExpression
     *     EqualityExpression === RelationalExpression
     *     EqualityExpression !== RelationalExpression
     *     EqualityExpression [no <i>LineTerminator</i> here] is RelationalExpression
     *     EqualityExpression [no <i>LineTerminator</i> here] isnt RelationalExpression
     * EqualityExpressionNoIn :
     *     RelationalExpressionNoIn
     *     EqualityExpressionNoIn == RelationalExpressionNoIn
     *     EqualityExpressionNoIn != RelationalExpressionNoIn
     *     EqualityExpressionNoIn === RelationalExpressionNoIn
     *     EqualityExpressionNoIn !== RelationalExpressionNoIn
     *     EqualityExpression [no <i>LineTerminator</i> here] is RelationalExpression
     *     EqualityExpression [no <i>LineTerminator</i> here] isnt RelationalExpression
     * BitwiseANDExpression :
     *     EqualityExpression
     *     BitwiseANDExpression & EqualityExpression
     * BitwiseANDExpressionNoIn :
     *     EqualityExpressionNoIn
     *     BitwiseANDExpressionNoIn & EqualityExpressionNoIn
     * BitwiseXORExpression :
     *     EqualityExpression
     *     BitwiseXORExpression ^ EqualityExpression
     * BitwiseXORExpressionNoIn :
     *     EqualityExpressionNoIn
     *     BitwiseXORExpressionNoIn ^ EqualityExpressionNoIn
     * BitwiseORExpression :
     *     EqualityExpression
     *     BitwiseORExpression | EqualityExpression
     * BitwiseORExpressionNoIn :
     *     EqualityExpressionNoIn
     *     BitwiseORExpressionNoIn | EqualityExpressionNoIn
     * LogicalANDExpression :
     *     EqualityExpression
     *     LogicalANDExpression && EqualityExpression
     * LogicalANDExpressionNoIn :
     *     EqualityExpressionNoIn
     *     LogicalANDExpressionNoIn && EqualityExpressionNoIn
     * LogicalORExpression :
     *     EqualityExpression
     *     LogicalORExpression || EqualityExpression
     * LogicalORExpressionNoIn :
     *     EqualityExpressionNoIn
     *     LogicalORExpressionNoIn || EqualityExpressionNoIn
     * </pre>
     */
    private Expression binaryExpression(boolean allowIn) {
        Expression lhs = unaryExpression();
        return binaryExpression(allowIn, lhs, BinaryExpression.Operator.OR.getPrecedence());
    }

    private Expression binaryExpression(boolean allowIn, Expression lhs, int minpred) {
        // Recursive-descent parsers require multiple levels of recursion to
        // parse binary expressions, to avoid this we're using precedence
        // climbing here
        for (;;) {
            Token tok = token();
            if (tok == Token.IN && !allowIn) {
                break;
            }
            BinaryExpression.Operator op = binaryOp(tok);
            int pred = (op != null ? op.getPrecedence() : -1);
            if (pred < minpred) {
                break;
            }
            consume(tok);
            Expression rhs = unaryExpression();
            for (;;) {
                BinaryExpression.Operator op2 = binaryOp(token());
                int pred2 = (op2 != null ? op2.getPrecedence() : -1);
                if (pred2 <= pred) {
                    break;
                }
                rhs = binaryExpression(allowIn, rhs, pred2);
            }
            lhs = new BinaryExpression(op, lhs, rhs);
        }
        return lhs;
    }

    private static BinaryExpression.Operator binaryOp(Token token) {
        switch (token) {
        case OR:
            return BinaryExpression.Operator.OR;
        case AND:
            return BinaryExpression.Operator.AND;
        case BITOR:
            return BinaryExpression.Operator.BITOR;
        case BITXOR:
            return BinaryExpression.Operator.BITXOR;
        case BITAND:
            return BinaryExpression.Operator.BITAND;
        case EQ:
            return BinaryExpression.Operator.EQ;
        case NE:
            return BinaryExpression.Operator.NE;
        case SHEQ:
            return BinaryExpression.Operator.SHEQ;
        case SHNE:
            return BinaryExpression.Operator.SHNE;
        case LT:
            return BinaryExpression.Operator.LT;
        case LE:
            return BinaryExpression.Operator.LE;
        case GT:
            return BinaryExpression.Operator.GT;
        case GE:
            return BinaryExpression.Operator.GE;
        case IN:
            return BinaryExpression.Operator.IN;
        case INSTANCEOF:
            return BinaryExpression.Operator.INSTANCEOF;
        case SHL:
            return BinaryExpression.Operator.SHL;
        case SHR:
            return BinaryExpression.Operator.SHR;
        case USHR:
            return BinaryExpression.Operator.USHR;
        case ADD:
            return BinaryExpression.Operator.ADD;
        case SUB:
            return BinaryExpression.Operator.SUB;
        case MUL:
            return BinaryExpression.Operator.MUL;
        case DIV:
            return BinaryExpression.Operator.DIV;
        case MOD:
            return BinaryExpression.Operator.MOD;
        default:
            return null;
        }
    }

    /**
     * <strong>[12.12] Conditional Operator</strong><br>
     * <strong>[12.13] Assignment Operators</strong>
     * 
     * <pre>
     * ConditionalExpression :
     *     LogicalORExpression
     *     LogicalORExpression ? AssignmentExpression : AssignmentExpression
     * ConditionalExpressionNoIn :
     *     LogicalORExpressionNoIn
     *     LogicalORExpressionNoIn ? AssignmentExpression : AssignmentExpressionNoIn
     * AssignmentExpression :
     *     ConditionalExpression
     *     YieldExpression
     *     ArrowFunction
     *     LeftHandSideExpression = AssignmentExpression
     *     LeftHandSideExpression AssignmentOperator AssignmentExpression
     * AssignmentExpressionNoIn :
     *     ConditionalExpressionNoIn
     *     YieldExpression
     *     ArrowFunction
     *     LeftHandSideExpression = AssignmentExpressionNoIn
     *     LeftHandSideExpression AssignmentOperator AssignmentExpressionNoIn
     * </pre>
     */
    private Expression assignmentExpression(boolean allowIn) {
        int count = context.countLiterals();
        Expression expr = assignmentExpression(allowIn, count);
        if (count < context.countLiterals()) {
            objectLiteral_StaticSemantics(count);
        }
        return expr;
    }

    private Expression assignmentExpressionNoValidation(boolean allowIn) {
        return assignmentExpression(allowIn, context.countLiterals());
    }

    private Expression assignmentExpression(boolean allowIn, int oldCount) {
        if (token() == Token.YIELD) {
            if (context.kind == ContextKind.Generator) {
                return yieldExpression(allowIn);
            } else if (context.kind == ContextKind.Function && isEnabled(Option.LegacyGenerator)) {
                throw new RetryGenerator();
            }
        }
        long position = ts.position(), lineinfo = ts.lineinfo();
        Expression left = binaryExpression(allowIn);
        Token tok = token();
        if (tok == Token.HOOK) {
            consume(Token.HOOK);
            Expression then = assignmentExpression(true);
            consume(Token.COLON);
            Expression otherwise = assignmentExpression(allowIn);
            return new ConditionalExpression(left, then, otherwise);
        } else if (tok == Token.ARROW) {
            // discard parsed object literals
            if (oldCount < context.countLiterals()) {
                ArrayDeque<ObjectLiteral> literals = context.objectLiterals;
                for (int i = oldCount, newCount = literals.size(); i < newCount; ++i) {
                    literals.pop();
                }
            }
            ts.reset(position, lineinfo);
            return arrowFunction(allowIn);
        } else if (tok == Token.ASSIGN) {
            LeftHandSideExpression lhs = validateAssignment(left, ExceptionType.ReferenceError,
                    Messages.Key.InvalidAssignmentTarget);
            consume(Token.ASSIGN);
            Expression right = assignmentExpression(allowIn);
            return new AssignmentExpression(assignmentOp(tok), lhs, right);
        } else if (isAssignmentOperator(tok)) {
            LeftHandSideExpression lhs = validateSimpleAssignment(left,
                    ExceptionType.ReferenceError, Messages.Key.InvalidAssignmentTarget);
            consume(tok);
            Expression right = assignmentExpression(allowIn);
            return new AssignmentExpression(assignmentOp(tok), lhs, right);
        } else {
            return left;
        }
    }

    private static AssignmentExpression.Operator assignmentOp(Token token) {
        switch (token) {
        case ASSIGN:
            return AssignmentExpression.Operator.ASSIGN;
        case ASSIGN_ADD:
            return AssignmentExpression.Operator.ASSIGN_ADD;
        case ASSIGN_SUB:
            return AssignmentExpression.Operator.ASSIGN_SUB;
        case ASSIGN_MUL:
            return AssignmentExpression.Operator.ASSIGN_MUL;
        case ASSIGN_DIV:
            return AssignmentExpression.Operator.ASSIGN_DIV;
        case ASSIGN_MOD:
            return AssignmentExpression.Operator.ASSIGN_MOD;
        case ASSIGN_SHL:
            return AssignmentExpression.Operator.ASSIGN_SHL;
        case ASSIGN_SHR:
            return AssignmentExpression.Operator.ASSIGN_SHR;
        case ASSIGN_USHR:
            return AssignmentExpression.Operator.ASSIGN_USHR;
        case ASSIGN_BITAND:
            return AssignmentExpression.Operator.ASSIGN_BITAND;
        case ASSIGN_BITOR:
            return AssignmentExpression.Operator.ASSIGN_BITOR;
        case ASSIGN_BITXOR:
            return AssignmentExpression.Operator.ASSIGN_BITXOR;
        default:
            throw new IllegalStateException();
        }
    }

    /**
     * <strong>[12.13] Assignment Operators</strong>
     * 
     * <pre>
     * AssignmentOperator : <b>one of</b>
     *     *=  /=  %=  +=  -=  <<=  >>=  >>>=  &=  ^=  |=
     * </pre>
     */
    private boolean isAssignmentOperator(Token tok) {
        switch (tok) {
        case ASSIGN_ADD:
        case ASSIGN_BITAND:
        case ASSIGN_BITOR:
        case ASSIGN_BITXOR:
        case ASSIGN_DIV:
        case ASSIGN_MOD:
        case ASSIGN_MUL:
        case ASSIGN_SHL:
        case ASSIGN_SHR:
        case ASSIGN_SUB:
        case ASSIGN_USHR:
            return true;
        default:
            return false;
        }
    }

    /**
     * <strong>[12.14] Comma Operator</strong>
     * 
     * <pre>
     * Expression :
     *     AssignmentExpression
     *     Expression , AssignmentExpression
     * ExpressionNoIn :
     *     AssignmentExpressionNoIn
     *     ExpressionNoIn , AssignmentExpressionNoIn
     * </pre>
     */
    private Expression expression(boolean allowIn) {
        Expression expr = assignmentExpression(allowIn);
        if (token() == Token.COMMA) {
            List<Expression> list = new ArrayList<>();
            list.add(expr);
            while (token() == Token.COMMA) {
                consume(Token.COMMA);
                expr = assignmentExpression(allowIn);
                list.add(expr);
            }
            return new CommaExpression(list);
        }
        return expr;
    }

    /* ***************************************************************************************** */

    /**
     * <strong>[11.9] Automatic Semicolon Insertion</strong>
     * 
     * <pre>
     * </pre>
     */
    private void semicolon() {
        switch (token()) {
        case SEMI:
            consume(Token.SEMI);
            // fall-through
        case RC:
        case EOF:
            break;
        default:
            if (noLineTerminator()) {
                reportSyntaxError(Messages.Key.MissingSemicolon);
            }
        }
    }

    /**
     * Peek next token and check for line-terminator
     */
    private boolean noLineTerminator() {
        return !ts.hasCurrentLineTerminator();
    }

    /**
     * Returns <code>true</code> if {@link Token#YIELD} should be treated as {@link Token#NAME} in
     * the current context
     */
    private boolean isYieldName() {
        return isYieldName(context);
    }

    /**
     * Returns <code>true</code> if {@link Token#YIELD} should be treated as {@link Token#NAME} in
     * the supplied context
     */
    private boolean isYieldName(ParseContext context) {
        // 'yield' is always a keyword in strict-mode and in generators
        if (context.strictMode == StrictMode.Strict || context.kind == ContextKind.Generator) {
            return false;
        }
        // proactively flag as syntax error if current strict mode is unknown
        reportStrictModeSyntaxError(Messages.Key.StrictModeInvalidIdentifier, getName(Token.YIELD));
        return true;
    }

    /**
     * Returns true if the current token is of type {@link Token#NAME} and its name is {@code name}
     */
    private boolean isName(String name) {
        Token tok = token();
        return (tok == Token.NAME && name.equals(getName(tok)));
    }

    /**
     * Return token name
     */
    private String getName(Token tok) {
        if (tok == Token.NAME) {
            return ts.getString();
        }
        return tok.getName();
    }

    /**
     * <strong>[11.6] Identifier Names and Identifiers</strong>
     * 
     * <pre>
     * Identifier ::
     *     IdentifierName but not ReservedWord
     * ReservedWord ::
     *     Keyword
     *     FutureReservedWord
     *     NullLiteral
     *     BooleanLiteral
     * </pre>
     */
    private String identifier() {
        Token tok = token();
        if (!isIdentifier(tok)) {
            reportTokenMismatch("<identifier>", tok);
        }
        String name = getName(tok);
        consume(tok);
        return name;
    }

    /**
     * <strong>[11.6] Identifier Names and Identifiers</strong>
     * 
     * <pre>
     * Identifier ::
     *     IdentifierName but not ReservedWord
     * ReservedWord ::
     *     Keyword
     *     FutureReservedWord
     *     NullLiteral
     *     BooleanLiteral
     * </pre>
     */
    private String strictIdentifier() {
        Token tok = token();
        if (!isStrictIdentifier(tok)) {
            reportTokenMismatch("<identifier>", tok);
        }
        String name = getName(tok);
        consume(tok);
        return name;
    }

    /**
     * <strong>[11.6] Identifier Names and Identifiers</strong>
     */
    private boolean isIdentifier(Token tok) {
        switch (tok) {
        case NAME:
            return true;
        case YIELD:
            return isYieldName();
        case IMPLEMENTS:
        case INTERFACE:
        case PACKAGE:
        case PRIVATE:
        case PROTECTED:
        case PUBLIC:
        case STATIC:
            if (context.strictMode != StrictMode.NonStrict) {
                reportStrictModeSyntaxError(Messages.Key.StrictModeInvalidIdentifier, getName(tok));
            }
            return (context.strictMode != StrictMode.Strict);
        default:
            return false;
        }
    }

    /**
     * <strong>[11.6] Identifier Names and Identifiers</strong>
     */
    private boolean isStrictIdentifier(Token tok) {
        switch (tok) {
        case NAME:
            return true;
        case YIELD:
        case IMPLEMENTS:
        case INTERFACE:
        case PACKAGE:
        case PRIVATE:
        case PROTECTED:
        case PUBLIC:
        case STATIC:
            throw reportSyntaxError(Messages.Key.StrictModeInvalidIdentifier, getName(tok));
        default:
            return false;
        }
    }

    /**
     * <strong>[11.6] Identifier Names and Identifiers</strong>
     */
    private String identifierName() {
        Token tok = token();
        if (!isIdentifierName(tok)) {
            reportTokenMismatch("<identifier-name>", tok);
        }
        String name = getName(tok);
        consume(tok);
        return name;
    }

    /**
     * <strong>[11.6] Identifier Names and Identifiers</strong>
     */
    private static boolean isIdentifierName(Token tok) {
        switch (tok) {
        case NAME:
            // Literals
        case NULL:
        case FALSE:
        case TRUE:
            // Keywords
        case BREAK:
        case CASE:
        case CATCH:
        case CLASS:
        case CONST:
        case CONTINUE:
        case DEBUGGER:
        case DEFAULT:
        case DELETE:
        case DO:
        case ELSE:
        case EXPORT:
        case FINALLY:
        case FOR:
        case FUNCTION:
        case IF:
        case IMPORT:
        case IN:
        case INSTANCEOF:
        case LET:
        case NEW:
        case RETURN:
        case SUPER:
        case SWITCH:
        case THIS:
        case THROW:
        case TRY:
        case TYPEOF:
        case VAR:
        case VOID:
        case WHILE:
        case WITH:
            // Future Reserved Words
        case ENUM:
        case EXTENDS:
        case IMPLEMENTS:
        case INTERFACE:
        case PACKAGE:
        case PRIVATE:
        case PROTECTED:
        case PUBLIC:
        case STATIC:
        case YIELD:
            return true;
        default:
            return false;
        }
    }

    /**
     * <strong>[11.6.1] Reserved Words</strong>
     */
    private boolean isReservedWord(Token tok) {
        switch (tok) {
        case FALSE:
        case NULL:
        case TRUE:
            return true;
        default:
            return isKeyword(tok) || isFutureReservedWord(tok);
        }
    }

    /**
     * <strong>[11.6.1.1] Keywords</strong>
     */
    private boolean isKeyword(Token tok) {
        switch (tok) {
        case BREAK:
        case CASE:
        case CATCH:
        case CLASS:
        case CONST:
        case CONTINUE:
        case DEBUGGER:
        case DEFAULT:
        case DELETE:
        case DO:
        case ELSE:
        case EXPORT:
        case FINALLY:
        case FOR:
        case FUNCTION:
        case IF:
        case IMPORT:
        case IN:
        case INSTANCEOF:
        case LET:
        case NEW:
        case RETURN:
        case SUPER:
        case SWITCH:
        case THIS:
        case THROW:
        case TRY:
        case TYPEOF:
        case VAR:
        case VOID:
        case WHILE:
        case WITH:
            return true;
        default:
            return false;
        }
    }

    /**
     * <strong>[11.6.1.2] Future Reserved Words</strong>
     */
    private boolean isFutureReservedWord(Token tok) {
        switch (tok) {
        case ENUM:
        case EXTENDS:
            return true;
        case IMPLEMENTS:
        case INTERFACE:
        case PACKAGE:
        case PRIVATE:
        case PROTECTED:
        case PUBLIC:
        case STATIC:
        case YIELD:
            return (context.strictMode != StrictMode.Strict);
        default:
            return false;
        }
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     */
    private double numericLiteral() {
        double number = ts.getNumber();
        consume(Token.NUMBER);
        return number;
    }

    /**
     * <strong>[11.8.4] String Literals</strong>
     */
    private String stringLiteral() {
        String string = ts.getString();
        consume(Token.STRING);
        return string;
    }
}

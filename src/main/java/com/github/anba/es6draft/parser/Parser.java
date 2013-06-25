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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.BreakableStatement.Abrupt;
import com.github.anba.es6draft.ast.MethodDefinition.MethodType;
import com.github.anba.es6draft.parser.ParserException.ExceptionType;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.SmallArrayList;

/**
 * Parser for ECMAScript6 source code
 * <ul>
 * <li>11 Expressions
 * <li>12 Statements and Declarations
 * <li>13 Functions and Generators
 * <li>14 Scripts and Modules
 * </ul>
 */
public class Parser {
    private static final boolean MODULES_ENABLED = false;
    private static final boolean DEBUG = false;

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

        boolean isFunction() {
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
        Set<String> parameterNames = null;
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
        Strict, FunctionCode, LocalScope, DirectEval, EvalScript,

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
        LegacyGenerator;

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
            return options;
        }
    }

    public Parser(String sourceFile, int sourceLine, Set<Option> options) {
        this.sourceFile = sourceFile;
        this.sourceLine = sourceLine;
        this.options = EnumSet.copyOf(options);
        context = new ParseContext();
        context.strictMode = options.contains(Option.Strict) ? StrictMode.Strict
                : StrictMode.NonStrict;
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

    private void addFunctionDecl(FunctionDeclaration decl) {
        String name = BoundName(decl.getIdentifier());
        ScopeContext parentScope = context.parent.scopeContext;
        if (parentScope.isTopLevel()) {
            // top-level function declaration
            parentScope.addVarScopedDeclaration(decl);
            if (!parentScope.addVarDeclaredName(name)) {
                reportSyntaxError(Messages.Key.VariableRedeclaration, name);
            }
        } else {
            // block-scoped function declaration
            parentScope.addLexScopedDeclaration(decl);
            if (!parentScope.addLexDeclaredName(name)) {
                reportSyntaxError(Messages.Key.VariableRedeclaration, name);
            }
        }
    }

    private void addGeneratorDecl(GeneratorDeclaration decl) {
        String name = BoundName(decl.getIdentifier());
        ScopeContext parentScope = context.parent.scopeContext;
        parentScope.addLexScopedDeclaration(decl);
        if (!parentScope.addLexDeclaredName(name)) {
            reportSyntaxError(Messages.Key.VariableRedeclaration, name);
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
            reportSyntaxError(Messages.Key.VariableRedeclaration, name);
        }
    }

    private void addLexDeclaredName(ScopeContext scope, String name) {
        if (!scope.addLexDeclaredName(name)) {
            reportSyntaxError(Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * <strong>[12.1] Block</strong>
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
        addVarDeclaredName(context.scopeContext, name);
    }

    private void addVarDeclaredName(BindingPattern bindingPattern) {
        for (String name : BoundNames(bindingPattern)) {
            addVarDeclaredName(context.scopeContext, name);
        }
    }

    /**
     * <strong>[12.1] Block</strong>
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
        addLexDeclaredName(context.scopeContext, name);
    }

    private void addLexDeclaredName(BindingPattern bindingPattern) {
        for (String name : BoundNames(bindingPattern)) {
            addLexDeclaredName(context.scopeContext, name);
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

    private LabelContext enterLabelled(StatementType type, Set<String> labelSet) {
        LabelContext cx = context.labels = new LabelContext(context.labels, type, labelSet);
        if (!labelSet.isEmpty() && context.labelSet == null) {
            context.labelSet = new HashMap<>();
        }
        for (String label : labelSet) {
            if (context.labelSet.containsKey(label)) {
                reportSyntaxError(Messages.Key.DuplicateLabel, label);
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

    private LabelContext enterIteration(Set<String> labelSet) {
        return enterLabelled(StatementType.Iteration, labelSet);
    }

    private void exitIteration() {
        exitLabelled();
    }

    private LabelContext enterBreakable(Set<String> labelSet) {
        return enterLabelled(StatementType.Breakable, labelSet);
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

    private ParserException reportException(ParserException exception) {
        throw exception;
    }

    private ParserException reportTokenMismatch(Token expected, Token actual) {
        if (actual == Token.EOF) {
            throw new ParserEOFException(ts.getLine(), Messages.Key.UnexpectedToken,
                    actual.toString(), expected.toString());
        }
        throw new ParserException(ExceptionType.SyntaxError, ts.getLine(),
                Messages.Key.UnexpectedToken, actual.toString(), expected.toString());
    }

    private ParserException reportTokenMismatch(String expected, Token actual) {
        if (actual == Token.EOF) {
            throw new ParserEOFException(ts.getLine(), Messages.Key.UnexpectedToken,
                    actual.toString(), expected);
        }
        throw new ParserException(ExceptionType.SyntaxError, ts.getLine(),
                Messages.Key.UnexpectedToken, actual.toString(), expected);
    }

    private ParserException reportTokenMismatch(Token expected, String actual) {
        throw new ParserException(ExceptionType.SyntaxError, ts.getLine(),
                Messages.Key.UnexpectedToken, actual, expected.toString());
    }

    private static ParserException reportError(ExceptionType type, int line,
            Messages.Key messageKey, String... args) {
        throw new ParserException(type, line, messageKey, args);
    }

    private static ParserException reportSyntaxError(Messages.Key messageKey, int line,
            String... args) {
        throw reportError(ExceptionType.SyntaxError, line, messageKey, args);
    }

    private ParserException reportSyntaxError(Messages.Key messageKey, String... args) {
        throw reportError(ExceptionType.SyntaxError, ts.getLine(), messageKey, args);
    }

    private ParserException reportReferenceError(Messages.Key messageKey, String... args) {
        throw reportError(ExceptionType.ReferenceError, ts.getLine(), messageKey, args);
    }

    private void reportStrictModeError(ExceptionType type, int line, Messages.Key messageKey,
            String... args) {
        if (context.strictMode == StrictMode.Unknown) {
            if (context.strictError == null) {
                context.strictError = new ParserException(type, line, messageKey, args);
            }
        } else if (context.strictMode == StrictMode.Strict) {
            reportError(type, line, messageKey, args);
        }
    }

    private void reportStrictModeSyntaxError(Messages.Key messageKey, int line, String... args) {
        reportStrictModeError(ExceptionType.SyntaxError, line, messageKey, args);
    }

    void reportStrictModeSyntaxError(Messages.Key messageKey, String... args) {
        reportStrictModeError(ExceptionType.SyntaxError, ts.getLine(), messageKey, args);
    }

    void reportStrictModeReferenceError(Messages.Key messageKey, String... args) {
        reportStrictModeError(ExceptionType.ReferenceError, ts.getLine(), messageKey, args);
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
        String string = ts.getString();
        consume(Token.NAME);
        if (!name.equals(string))
            reportSyntaxError(Messages.Key.UnexpectedName, string, name);
    }

    public Script parse(CharSequence source) throws ParserException {
        return parseScript(source);
    }

    public Script parseScript(CharSequence source) throws ParserException {
        if (ts != null)
            throw new IllegalStateException();
        ts = new TokenStream(this, new StringTokenStreamInput(source), sourceLine);
        return script();
    }

    public ModuleDeclaration parseModule(CharSequence source) throws ParserException {
        if (ts != null)
            throw new IllegalStateException();

        newContext(ContextKind.Script);
        try {
            applyStrictMode(true); // defaults to strict?

            ModuleDeclaration module;
            newContext(ContextKind.Module);
            try {
                ts = new TokenStream(this, new StringTokenStreamInput(source), sourceLine);
                ts.init();

                String moduleName = sourceFile; // only basename(sourceFile)?
                List<StatementListItem> body = moduleBody(Token.EOF);

                FunctionContext scope = context.funContext;
                module = new ModuleDeclaration(moduleName, body, scope);
                scope.node = module;
            } finally {
                restoreContext();
            }

            createScript(module);

            return module;
        } finally {
            restoreContext();
        }
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
                ts = new TokenStream(this, new StringTokenStreamInput(formals), sourceLine);
                ts.init();
                FormalParameterList parameters = formalParameters(Token.EOF);
                if (token() != Token.EOF) {
                    reportSyntaxError(Messages.Key.InvalidFormalParameterList);
                }
                if (ts.position() != formals.length()) {
                    // more input after last token (whitespace, comments), add newlines to handle
                    // last token is single-line comment case
                    formals = "\n" + formals + "\n";
                }

                ts = new TokenStream(this, new StringTokenStreamInput(bodyText), sourceLine);
                ts.init();
                List<StatementListItem> statements = functionBody(Token.EOF);
                if (token() != Token.EOF) {
                    reportSyntaxError(Messages.Key.InvalidFunctionBody);
                }

                String header = String.format("function anonymous (%s) ", formals);
                String body = String.format("\n%s\n", bodyText);

                FunctionContext scope = context.funContext;
                function = new FunctionExpression(scope, "anonymous", parameters, statements,
                        header, body);
                function.setLine(sourceLine);
                scope.node = function;

                function_StaticSemantics(function);

                function = inheritStrictness(function);
            } catch (RetryGenerator e) {
                // don't bother with legacy support here
                throw reportSyntaxError(Messages.Key.InvalidYieldStatement);
            } finally {
                restoreContext();
            }

            createScript(new ExpressionStatement(function));

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
                ts = new TokenStream(this, new StringTokenStreamInput(formals), sourceLine);
                ts.init();
                FormalParameterList parameters = formalParameters(Token.EOF);
                if (token() != Token.EOF) {
                    reportSyntaxError(Messages.Key.InvalidFormalParameterList);
                }
                if (ts.position() != formals.length()) {
                    // more input after last token (whitespace, comments), add newlines to handle
                    // last token is single-line comment case
                    formals = "\n" + formals + "\n";
                }

                ts = new TokenStream(this, new StringTokenStreamInput(bodyText), sourceLine);
                ts.init();
                List<StatementListItem> statements = functionBody(Token.EOF);
                if (token() != Token.EOF) {
                    reportSyntaxError(Messages.Key.InvalidFunctionBody);
                }

                String header = String.format("function* anonymous (%s) ", formals);
                String body = String.format("\n%s\n", bodyText);

                FunctionContext scope = context.funContext;
                generator = new GeneratorExpression(scope, "anonymous", parameters, statements,
                        header, body);
                generator.setLine(sourceLine);
                scope.node = generator;

                generator_StaticSemantics(generator);

                generator = inheritStrictness(generator);
            } finally {
                restoreContext();
            }

            createScript(new ExpressionStatement(generator));

            return generator;
        } finally {
            restoreContext();
        }
    }

    private Script createScript(StatementListItem statement) {
        List<StatementListItem> statements = singletonList(statement);
        boolean strict = (context.strictMode == StrictMode.Strict);

        FunctionContext scope = context.funContext;
        Script script = new Script(sourceFile, scope, statements, options, strict);
        script.setLine(sourceLine);
        scope.node = script;

        return script;
    }

    /* ***************************************************************************************** */

    /**
     * <strong>[14.1] Script</strong>
     * 
     * <pre>
     * Script :
     *     ScriptBody<sub>opt</sub>
     * ScriptBody :
     *     OuterStatementList
     * </pre>
     */
    private Script script() {
        newContext(ContextKind.Script);
        try {
            ts.init();
            List<StatementListItem> prologue = directivePrologue();
            List<StatementListItem> body = outerStatementList();
            boolean strict = (context.strictMode == StrictMode.Strict);

            FunctionContext scope = context.funContext;
            Script script = new Script(sourceFile, scope, merge(prologue, body), options, strict);
            script.setLine(sourceLine);
            scope.node = script;

            return script;
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[14.1] Script</strong>
     * 
     * <pre>
     * OuterStatementList :
     *     OuterItem
     *     OuterStatementList OuterItem
     * OuterItem :
     *     ModuleDeclaration
     *     ImportDeclaration
     *     StatementListItem
     * </pre>
     */
    private List<StatementListItem> outerStatementList() {
        List<StatementListItem> list = newList();
        while (token() != Token.EOF) {
            if (MODULES_ENABLED) {
                // TODO: implement modules
                if (token() == Token.IMPORT) {
                    list.add(importDeclaration());
                } else if (isName("module") && (peek() == Token.STRING || isIdentifier(peek()))
                        && !ts.hasNextLineTerminator()) {
                    list.add(moduleDeclaration());
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
     * <strong>[14.2] Modules</strong>
     * 
     * <pre>
     * ModuleDeclaration ::= "module" [NoNewline] StringLiteral "{" ModuleBody "}"
     *                    |  "module" Identifier "from" StringLiteral ";"
     * </pre>
     */
    private ModuleDeclaration moduleDeclaration() {
        newContext(ContextKind.Module);
        try {
            consume("module");
            if (token() == Token.STRING) {
                String moduleName = stringLiteral();
                consume(Token.LC);
                List<StatementListItem> body = moduleBody(Token.RC);
                consume(Token.RC);

                FunctionContext scope = context.funContext;
                ModuleDeclaration module = new ModuleDeclaration(moduleName, body, scope);
                scope.node = module;

                return module;
            } else {
                String identifier = identifier();
                consume("from");
                String moduleName = stringLiteral();
                semicolon();

                FunctionContext scope = context.funContext;
                ModuleDeclaration module = new ModuleDeclaration(identifier, moduleName, scope);
                scope.node = module;

                return module;
            }
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[14.2] Modules</strong>
     * 
     * <pre>
     * ModuleBody    ::= ModuleElement*
     * ModuleElement ::= ScriptElement
     *                |  ExportDeclaration
     * </pre>
     */
    private List<StatementListItem> moduleBody(Token end) {
        List<StatementListItem> list = newList();
        while (token() != end) {
            // actually: ExportDeclaration | ImportDeclaration | StatementListItem
            // TODO: are nested modules (still) allowed? (disabled for now)
            if (token() == Token.EXPORT) {
                list.add(exportDeclaration());
            } else if (token() == Token.IMPORT) {
                list.add(importDeclaration());
            } else {
                list.add(statementListItem());
            }
        }
        return list;
    }

    /**
     * <strong>[14.2] Modules</strong>
     * 
     * <pre>
     * ExportDeclaration ::= "export" ExportSpecifierSet ";"
     *                    |  "export" "default" AssignmentExpression ";"
     *                    |  "export" VariableDeclaration
     *                    |  "export" FunctionDeclaration
     *                    |  "export" ClassDeclaration
     * </pre>
     */
    private ExportDeclaration exportDeclaration() {
        consume(Token.EXPORT);
        switch (token()) {
        case LC:
        case MUL: {
            // "export" ExportSpecifierSet ";"
            ExportSpecifierSet exportSpecifierSet = exportSpecifierSet();
            semicolon();
            return new ExportDeclaration(exportSpecifierSet);
        }

        case DEFAULT: {
            // "export" "default" AssignmentExpression ";"
            consume(Token.DEFAULT);
            Expression expression = assignmentExpression(true);
            semicolon();
            return new ExportDeclaration(expression);
        }

        case VAR: {
            // "export" VariableDeclaration
            VariableStatement variableStatement = variableStatement();
            return new ExportDeclaration(variableStatement);
        }

        case FUNCTION:
        case CLASS:
        case LET:
        case CONST: {
            // "export" FunctionDeclaration
            // "export" ClassDeclaration
            Declaration declaration = declaration();
            return new ExportDeclaration(declaration);
        }

        default:
            throw reportSyntaxError(Messages.Key.InvalidToken, token().toString());
        }
    }

    /**
     * <strong>[14.2] Modules</strong>
     * 
     * <pre>
     * ExportSpecifierSet ::= "{" (ExportSpecifier ("," ExportSpecifier)* ","?)? "}"
     *                     |   "*" ("from" ModuleSpecifier)?
     * </pre>
     */
    private ExportSpecifierSet exportSpecifierSet() {
        if (token() == Token.LC) {
            List<ExportSpecifier> exports = newSmallList();
            consume(Token.LC);
            while (token() != Token.RC) {
                exports.add(exportSpecifier());
                if (token() == Token.COMMA) {
                    consume(Token.COMMA);
                } else {
                    break;
                }
            }
            consume(Token.RC);
            // FIXME: re-export should also work with named exports
            String sourceModule = null;
            if (isName("from")) {
                consume("from");
                sourceModule = moduleSpecifier();
            }

            return new ExportSpecifierSet(exports, sourceModule);
        } else {
            consume(Token.MUL);
            String sourceModule = null;
            if (isName("from")) {
                consume("from");
                sourceModule = moduleSpecifier();
            }

            return new ExportSpecifierSet(sourceModule);
        }
    }

    /**
     * <strong>[14.2] Modules</strong>
     * 
     * <pre>
     * ExportSpecifier ::= Identifier ("as" IdentifierName)?
     * </pre>
     */
    private ExportSpecifier exportSpecifier() {
        String localName = identifier();
        String externalName;
        if (isName("as")) {
            consume("as");
            externalName = identifierName();
        } else {
            externalName = localName;
        }
        return new ExportSpecifier(localName, externalName);
    }

    /**
     * <strong>[14.2] Modules</strong>
     * 
     * <pre>
     * ModuleSpecifier ::= StringLiteral
     * </pre>
     */
    private String moduleSpecifier() {
        return stringLiteral();
    }

    /**
     * <strong>[14.2] Modules</strong>
     * 
     * <pre>
     * ImportDeclaration ::= "import" ImportSpecifierSet "from" ModuleSpecifier ";"
     *                    |  "import" ModuleSpecifier ";"
     * </pre>
     */
    private ImportDeclaration importDeclaration() {
        consume(Token.IMPORT);
        if (token() == Token.STRING) {
            String moduleSpecifier = moduleSpecifier();
            semicolon();

            return new ImportDeclaration(moduleSpecifier);
        } else {
            ImportSpecifierSet importSpecifierSet = importSpecifierSet();
            consume("from");
            String moduleSpecifier = moduleSpecifier();
            semicolon();

            return new ImportDeclaration(importSpecifierSet, moduleSpecifier);
        }
    }

    /**
     * <strong>[14.2] Modules</strong>
     * 
     * <pre>
     * ImportSpecifierSet ::= Identifier
     *                     |  "{" (ImportSpecifier ("," ImportSpecifier)* ","?)? "}"
     * </pre>
     */
    private ImportSpecifierSet importSpecifierSet() {
        if (isIdentifier(token())) {
            String defaultImport = identifier();

            return new ImportSpecifierSet(defaultImport);
        } else {
            List<ImportSpecifier> imports = newSmallList();
            consume(Token.LC);
            while (token() != Token.RC) {
                imports.add(importSpecifier());
                if (token() == Token.COMMA) {
                    consume(Token.COMMA);
                } else {
                    break;
                }
            }
            consume(Token.RC);

            return new ImportSpecifierSet(imports);
        }
    }

    /**
     * <strong>[14.2] Modules</strong>
     * 
     * <pre>
     * ImportSpecifier ::= Identifier ("as" Identifier)?
     *                  |  ReservedWord "as" Identifier
     * </pre>
     */
    private ImportSpecifier importSpecifier() {
        String externalName, localName;
        if (isIdentifier(token())) {
            externalName = identifier();
            if (isName("as")) {
                consume("as");
                localName = identifier();
            } else {
                localName = externalName;
            }
        } else {
            externalName = identifierName();
            consume("as");
            localName = identifier();
        }
        return new ImportSpecifier(externalName, localName);
    }

    /**
     * <strong>[14.1] Directive Prologues and the Use Strict Directive</strong>
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
            boolean hasEscape = ts.hasEscape(); // peek() may clear hasEscape flag
            Token next = peek();
            switch (next) {
            case SEMI:
            case RC:
            case EOF:
                break;
            default:
                if (ts.hasNextLineTerminator() && !isOperator(next)) {
                    break;
                }
                break directive;
            }
            // got a directive
            String string = stringLiteral();
            if (!hasEscape && "use strict".equals(string)) {
                strict = true;
            }
            semicolon();
            statements.add(new ExpressionStatement(new StringLiteral(string)));
        }
        applyStrictMode(strict);
        return statements;
    }

    private static boolean isOperator(Token token) {
        switch (token) {
        case DOT:
        case LB:
        case LP:
        case TEMPLATE:
        case COMMA:
        case HOOK:
        case ASSIGN:
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
        case OR:
        case AND:
        case BITAND:
        case BITOR:
        case BITXOR:
        case EQ:
        case NE:
        case SHEQ:
        case SHNE:
        case LT:
        case LE:
        case GT:
        case GE:
        case INSTANCEOF:
        case IN:
        case SHL:
        case SHR:
        case USHR:
        case ADD:
        case SUB:
        case MUL:
        case DIV:
        case MOD:
            return true;
        default:
            return false;
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
        if (strict) {
            if (explicit) {
                return FunctionNode.StrictMode.ExplicitStrict;
            }
            return FunctionNode.StrictMode.ImplicitStrict;
        }
        return FunctionNode.StrictMode.NonStrict;
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
     * <strong>[13.1] Function Definitions</strong>
     * 
     * <pre>
     * FunctionDeclaration :
     *     function BindingIdentifier ( FormalParameters ) { FunctionBody }
     * </pre>
     */
    private FunctionDeclaration functionDeclaration() {
        newContext(ContextKind.Function);
        try {
            int line = ts.getLine();
            consume(Token.FUNCTION);
            int startFunction = ts.position() - "function".length();
            BindingIdentifier identifier = bindingIdentifier();
            consume(Token.LP);
            FormalParameterList parameters = formalParameters(Token.RP);
            consume(Token.RP);

            String header, body;
            List<StatementListItem> statements;
            if (token() != Token.LC && isEnabled(Option.ExpressionClosure)) {
                // need to call manually b/c functionBody() isn't used here
                applyStrictMode(false);

                int startBody = ts.position();
                statements = Collections.<StatementListItem> singletonList(new ReturnStatement(
                        assignmentExpression(true)));
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
            FunctionDeclaration function = new FunctionDeclaration(scope, identifier, parameters,
                    statements, header, body);
            function.setLine(line);
            scope.node = function;

            function_StaticSemantics(function);

            addFunctionDecl(function);

            return inheritStrictness(function);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[13.1] Function Definitions</strong>
     * 
     * <pre>
     * FunctionExpression :
     *     function BindingIdentifier<sub>opt</sub> ( FormalParameters ) { FunctionBody }
     * </pre>
     */
    private FunctionExpression functionExpression() {
        newContext(ContextKind.Function);
        try {
            int line = ts.getLine();
            consume(Token.FUNCTION);
            int startFunction = ts.position() - "function".length();
            BindingIdentifier identifier = null;
            if (token() != Token.LP) {
                identifier = bindingIdentifier();
            }
            consume(Token.LP);
            FormalParameterList parameters = formalParameters(Token.RP);
            consume(Token.RP);

            String header, body;
            List<StatementListItem> statements;
            if (token() != Token.LC && isEnabled(Option.ExpressionClosure)) {
                // need to call manually b/c functionBody() isn't used here
                applyStrictMode(false);

                int startBody = ts.position();
                statements = Collections.<StatementListItem> singletonList(new ReturnStatement(
                        assignmentExpression(true)));
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
            FunctionExpression function = new FunctionExpression(scope, identifier, parameters,
                    statements, header, body);
            function.setLine(line);
            scope.node = function;

            function_StaticSemantics(function);

            return inheritStrictness(function);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[13.1] Function Definitions</strong>
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
     * <strong>[13.1] Function Definitions</strong>
     * 
     * <pre>
     * FormalParameters :
     *     [empty]
     *     FormalParameterList
     * </pre>
     */
    private FormalParameterList formalParameters(Token end) {
        if (token() == end) {
            return new FormalParameterList(Collections.<FormalParameter> emptyList());
        }
        return formalParameterList();
    }

    /**
     * <strong>[13.1] Function Definitions</strong>
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
        List<FormalParameter> formals = newSmallList();
        for (;;) {
            if (token() == Token.TRIPLE_DOT) {
                consume(Token.TRIPLE_DOT);
                formals.add(new BindingRestElement(bindingIdentifierStrict()));
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
        return new FormalParameterList(formals);
    }

    private static <T> T containsAny(Set<T> set, List<T> list) {
        for (T element : list) {
            if (set.contains(element)) {
                return element;
            }
        }
        return null;
    }

    private void checkFormalParameterRedeclaration(List<String> boundNames,
            HashSet<String> declaredNames) {
        if (!(declaredNames == null || declaredNames.isEmpty())) {
            String redeclared = containsAny(declaredNames, boundNames);
            if (redeclared != null) {
                reportSyntaxError(Messages.Key.FormalParameterRedeclaration, redeclared);
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
            checkFormalParameterRedeclaration(boundNames, scope.varDeclaredNames);
        }
        checkFormalParameterRedeclaration(boundNames, scope.lexDeclaredNames);
        formalParameters_StaticSemantics(boundNames, scope.parameterNames, simple);
    }

    private void strictFormalParameters_StaticSemantics(List<String> boundNames, Set<String> names) {
        boolean hasDuplicates = (boundNames.size() != names.size());
        boolean hasEvalOrArguments = (names.contains("eval") || names.contains("arguments"));
        if (hasDuplicates) {
            reportSyntaxError(Messages.Key.StrictModeDuplicateFormalParameter);
        }
        if (hasEvalOrArguments) {
            reportSyntaxError(Messages.Key.StrictModeRestrictedIdentifier);
        }
    }

    private void formalParameters_StaticSemantics(List<String> boundNames, Set<String> names,
            boolean simple) {
        boolean strict = (context.strictMode != StrictMode.NonStrict);
        if (!strict && simple) {
            return;
        }
        boolean hasDuplicates = (boundNames.size() != names.size());
        boolean hasEvalOrArguments = (names.contains("eval") || names.contains("arguments"));
        if (!simple) {
            if (hasDuplicates) {
                reportSyntaxError(Messages.Key.StrictModeDuplicateFormalParameter);
            }
            if (hasEvalOrArguments) {
                reportSyntaxError(Messages.Key.StrictModeRestrictedIdentifier);
            }
        }
        if (strict) {
            if (hasDuplicates) {
                reportStrictModeSyntaxError(Messages.Key.StrictModeDuplicateFormalParameter);
            }
            if (hasEvalOrArguments) {
                reportStrictModeSyntaxError(Messages.Key.StrictModeRestrictedIdentifier);
            }
        }
    }

    /**
     * <strong>[13.1] Function Definitions</strong>
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
     * <strong>[13.2] Arrow Function Definitions</strong>
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
    private ArrowFunction arrowFunction() {
        newContext(ContextKind.ArrowFunction);
        try {
            int line = ts.getLine();
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
                FormalParameter parameter = new BindingElement(identifier, null);
                parameters = new FormalParameterList(singletonList(parameter));

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
                ArrowFunction function = new ArrowFunction(scope, parameters, statements, header,
                        body);
                function.setLine(line);
                scope.node = function;

                arrowFunction_StaticSemantics(function);

                return inheritStrictness(function);
            } else {
                // need to call manually b/c functionBody() isn't used here
                applyStrictMode(false);

                int startBody = ts.position();
                Expression expression = assignmentExpression(true);
                int endFunction = ts.position();

                String header = source.toString();
                String body = "return " + ts.range(startBody, endFunction);

                FunctionContext scope = context.funContext;
                ArrowFunction function = new ArrowFunction(scope, parameters, expression, header,
                        body);
                function.setLine(line);
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

        checkFormalParameterRedeclaration(boundNames, scope.varDeclaredNames);
        checkFormalParameterRedeclaration(boundNames, scope.lexDeclaredNames);
        strictFormalParameters_StaticSemantics(boundNames, scope.parameterNames);
    }

    /**
     * <strong>[13.3] Method Definitions</strong>
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
        if (token() == Token.MUL) {
            return generatorMethod(alwaysStrict);
        }

        MethodType type = methodType();
        newContext(ContextKind.Method);
        if (alwaysStrict) {
            context.strictMode = StrictMode.Strict;
        }
        try {
            PropertyName propertyName;
            FormalParameterList parameters;
            List<StatementListItem> statements;

            int line = ts.getLine();
            String header, body;
            switch (type) {
            case Getter: {
                consume(Token.NAME);
                propertyName = propertyName();
                consume(Token.LP);
                int startFunction = ts.position() - 1;
                parameters = new FormalParameterList(Collections.<FormalParameter> emptyList());
                consume(Token.RP);

                if (token() != Token.LC && isEnabled(Option.ExpressionClosure)) {
                    // need to call manually b/c functionBody() isn't used here
                    applyStrictMode(false);

                    int startBody = ts.position();
                    statements = Collections.<StatementListItem> singletonList(new ReturnStatement(
                            assignmentExpression(true)));
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
                break;
            }
            case Setter: {
                consume(Token.NAME);
                propertyName = propertyName();
                consume(Token.LP);
                int startFunction = ts.position() - 1;
                parameters = propertySetParameterList();
                consume(Token.RP);

                if (token() != Token.LC && isEnabled(Option.ExpressionClosure)) {
                    // need to call manually b/c functionBody() isn't used here
                    applyStrictMode(false);

                    int startBody = ts.position();
                    statements = Collections.<StatementListItem> singletonList(new ReturnStatement(
                            assignmentExpression(true)));
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
                break;
            }
            case Function:
            default: {
                propertyName = propertyName();
                consume(Token.LP);
                int startFunction = ts.position() - 1;
                parameters = strictFormalParameters(Token.RP);
                consume(Token.RP);
                consume(Token.LC);
                int startBody = ts.position();
                statements = functionBody(Token.RC);
                consume(Token.RC);
                int endFunction = ts.position() - 1;

                header = "function " + ts.range(startFunction, startBody - 1);
                body = ts.range(startBody, endFunction);
                break;
            }
            }

            FunctionContext scope = context.funContext;
            MethodDefinition method = new MethodDefinition(scope, type, propertyName, parameters,
                    statements, context.hasSuperReference(), header, body);
            method.setLine(line);
            scope.node = method;

            methodDefinition_StaticSemantics(method);

            return inheritStrictness(method);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[13.3] Method Definitions</strong>
     * 
     * <pre>
     * PropertySetParameterList :
     *     BindingIdentifier
     *     BindingPattern
     * </pre>
     */
    private FormalParameterList propertySetParameterList() {
        FormalParameter setParameter = new BindingElement(binding(), null);
        return new FormalParameterList(singletonList(setParameter));
    }

    private MethodType methodType() {
        if (token() == Token.NAME) {
            String name = getName(Token.NAME);
            if (("get".equals(name) || "set".equals(name)) && isPropertyName(peek())) {
                return "get".equals(name) ? MethodType.Getter : MethodType.Setter;
            }
        }
        return MethodType.Function;
    }

    private boolean isPropertyName(Token token) {
        return token == Token.STRING || token == Token.NUMBER || isIdentifierName(token);
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
            checkFormalParameterRedeclaration(boundNames, scope.varDeclaredNames);
            checkFormalParameterRedeclaration(boundNames, scope.lexDeclaredNames);
            strictFormalParameters_StaticSemantics(boundNames, scope.parameterNames);
            return;
        }
        case Setter: {
            boolean simple = IsSimpleParameterList(parameters);
            if (!simple) {
                checkFormalParameterRedeclaration(boundNames, scope.varDeclaredNames);
            }
            checkFormalParameterRedeclaration(boundNames, scope.lexDeclaredNames);
            propertySetParameterList_StaticSemantics(boundNames, scope.parameterNames, simple);
            return;
        }
        case Getter:
        default:
            return;
        }
    }

    private void propertySetParameterList_StaticSemantics(List<String> boundNames,
            Set<String> names, boolean simple) {
        boolean strict = (context.strictMode != StrictMode.NonStrict);
        boolean hasDuplicates = (boundNames.size() != names.size());
        boolean hasEvalOrArguments = (names.contains("eval") || names.contains("arguments"));
        if (!simple) {
            if (hasDuplicates) {
                reportSyntaxError(Messages.Key.StrictModeDuplicateFormalParameter);
            }
            if (hasEvalOrArguments) {
                reportSyntaxError(Messages.Key.StrictModeRestrictedIdentifier);
            }
        }
        // FIXME: spec bug - duplicate check done twice
        if (hasDuplicates) {
            reportSyntaxError(Messages.Key.StrictModeDuplicateFormalParameter);
        }
        // FIXME: spec bug - not handled in draft
        if (strict) {
            if (hasEvalOrArguments) {
                reportStrictModeSyntaxError(Messages.Key.StrictModeRestrictedIdentifier);
            }
        }
    }

    /**
     * <strong>[13.4] Generator Function Definitions</strong>
     * 
     * <pre>
     * GeneratorMethod :
     *     * PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </pre>
     */
    private MethodDefinition generatorMethod(boolean alwaysStrict) {
        newContext(ContextKind.Generator);
        if (alwaysStrict) {
            context.strictMode = StrictMode.Strict;
        }
        try {
            int line = ts.getLine();
            consume(Token.MUL);
            PropertyName propertyName = propertyName();
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
            MethodDefinition method = new MethodDefinition(scope, type, propertyName, parameters,
                    statements, context.hasSuperReference(), header, body);
            method.setLine(line);
            scope.node = method;

            methodDefinition_StaticSemantics(method);

            return inheritStrictness(method);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[13.4] Generator Function Definitions</strong>
     * 
     * <pre>
     * GeneratorDeclaration :
     *     function * BindingIdentifier ( FormalParameters ) { FunctionBody }
     * </pre>
     */
    private GeneratorDeclaration generatorDeclaration(boolean starless) {
        newContext(ContextKind.Generator);
        try {
            int line = ts.getLine();
            consume(Token.FUNCTION);
            int startFunction = ts.position() - "function".length();
            if (!starless) {
                consume(Token.MUL);
            }
            BindingIdentifier identifier = bindingIdentifier();
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
            GeneratorDeclaration generator = new GeneratorDeclaration(scope, identifier,
                    parameters, statements, header, body);
            generator.setLine(line);
            scope.node = generator;

            generator_StaticSemantics(generator);

            addGeneratorDecl(generator);

            return inheritStrictness(generator);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[13.4] Generator Function Definitions</strong>
     * 
     * <pre>
     * GeneratorExpression :
     *     function * BindingIdentifier<sub>opt</sub> ( FormalParameters ) { FunctionBody }
     * </pre>
     */
    private GeneratorExpression generatorExpression(boolean starless) {
        newContext(ContextKind.Generator);
        try {
            int line = ts.getLine();
            consume(Token.FUNCTION);
            int startFunction = ts.position() - "function".length();
            if (!starless) {
                consume(Token.MUL);
            }
            BindingIdentifier identifier = null;
            if (token() != Token.LP) {
                identifier = bindingIdentifier();
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
            GeneratorExpression generator = new GeneratorExpression(scope, identifier, parameters,
                    statements, header, body);
            generator.setLine(line);
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
            checkFormalParameterRedeclaration(boundNames, scope.varDeclaredNames);
        }
        checkFormalParameterRedeclaration(boundNames, scope.lexDeclaredNames);
        formalParameters_StaticSemantics(boundNames, scope.parameterNames, simple);
    }

    /**
     * <strong>[13.4] Generator Function Definitions</strong>
     * 
     * <pre>
     * YieldExpression :
     *     yield YieldDelegator<sub>opt</sub> <font size="-1">[Lexical goal <i>InputElementRegExp</i>]</font> AssignmentExpression
     * YieldDelegator :
     *     *
     * </pre>
     */
    private YieldExpression yieldExpression() {
        if (!context.yieldAllowed) {
            if (context.kind == ContextKind.Function && isEnabled(Option.LegacyGenerator)) {
                throw new RetryGenerator();
            }
            reportSyntaxError(Messages.Key.InvalidYieldStatement);
        }

        consume(Token.YIELD);
        boolean delegatedYield = false;
        if (token() == Token.MUL) {
            consume(Token.MUL);
            delegatedYield = true;
        }
        if (token() == Token.YIELD) {
            // disallow `yield yield x` but allow `yield (yield x)`
            // TODO: track spec changes, syntax not yet settled
            reportSyntaxError(Messages.Key.InvalidYieldStatement);
        }
        // TODO: NoLineTerminator() restriction or context dependent?
        Expression expr;
        if (delegatedYield || !(token() == Token.SEMI || token() == Token.RC)) {
            expr = assignmentExpression(true);
        } else {
            // extension: allow Spidermonkey syntax
            expr = null;
        }
        return new YieldExpression(delegatedYield, expr);
    }

    /**
     * <strong>[13.5] Class Definitions</strong>
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
        consume(Token.CLASS);
        BindingIdentifier name = bindingIdentifierStrict();
        Expression heritage = null;
        if (token() == Token.EXTENDS) {
            consume(Token.EXTENDS);
            heritage = assignmentExpression(true);
        }
        consume(Token.LC);
        List<MethodDefinition> staticMethods = newList();
        List<MethodDefinition> prototypeMethods = newList();
        classBody(staticMethods, prototypeMethods);
        consume(Token.RC);

        ClassDeclaration decl = new ClassDeclaration(name, heritage, staticMethods,
                prototypeMethods);
        addLexDeclaredName(name);
        addLexScopedDeclaration(decl);
        return decl;
    }

    /**
     * <strong>[13.5] Class Definitions</strong>
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
        consume(Token.CLASS);
        BindingIdentifier name = null;
        if (token() != Token.EXTENDS && token() != Token.LC) {
            name = bindingIdentifierStrict();
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
        classBody(staticMethods, prototypeMethods);
        if (name != null) {
            exitBlockContext();
        }
        consume(Token.RC);

        return new ClassExpression(name, heritage, staticMethods, prototypeMethods);
    }

    /**
     * <strong>[13.5] Class Definitions</strong>
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
    private void classBody(List<MethodDefinition> staticMethods,
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

        classBody_StaticSemantics(staticMethods, true);
        classBody_StaticSemantics(prototypeMethods, false);
    }

    private void classBody_StaticSemantics(List<MethodDefinition> defs, boolean isStatic) {
        final int VALUE = 0, GETTER = 1, SETTER = 2;
        Map<String, Integer> values = new HashMap<>();
        for (MethodDefinition def : defs) {
            String key = PropName(def);
            if (isStatic) {
                if ("prototype".equals(key)) {
                    reportSyntaxError(Messages.Key.InvalidPrototypeMethod);
                }
            } else {
                if ("constructor".equals(key) && SpecialMethod(def)) {
                    reportSyntaxError(Messages.Key.InvalidConstructorMethod);
                }
            }
            MethodDefinition.MethodType type = def.getType();
            final int kind = type == MethodType.Getter ? GETTER
                    : type == MethodType.Setter ? SETTER : VALUE;
            if (values.containsKey(key)) {
                int prev = values.get(key);
                if (kind == VALUE) {
                    reportSyntaxError(Messages.Key.DuplicatePropertyDefinition, key);
                }
                if (kind == GETTER && prev != SETTER) {
                    reportSyntaxError(Messages.Key.DuplicatePropertyDefinition, key);
                }
                if (kind == SETTER && prev != GETTER) {
                    reportSyntaxError(Messages.Key.DuplicatePropertyDefinition, key);
                }
                values.put(key, prev | kind);
            } else {
                values.put(key, kind);
            }
        }
    }

    /* ***************************************************************************************** */

    /**
     * <strong>[12] Statements</strong>
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
        case NAME:
            if (LOOKAHEAD(Token.COLON)) {
                return labelledStatement();
            }
        default:
        }
        return expressionStatement();
    }

    /**
     * <strong>[12.1] Block</strong>
     * 
     * <pre>
     * BlockStatement :
     *     Block
     * Block :
     *     { StatementList<sub>opt</sub> }
     * </pre>
     */
    private BlockStatement block(List<Binding> inherited) {
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

        BlockStatement block = new BlockStatement(scope, list);
        scope.node = block;
        return block;
    }

    /**
     * <strong>[12.1] Block</strong>
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
     * <strong>[12.1] Block</strong>
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
     * <strong>[12.1] Block</strong>
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
            long marker = ts.marker();
            try {
                return functionDeclaration();
            } catch (RetryGenerator e) {
                ts.reset(marker);
                return generatorDeclaration(true);
            }
        }
    }

    /**
     * <strong>[12.2.1] Let and Const Declarations</strong>
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

        LexicalDeclaration decl = new LexicalDeclaration(type, list);
        addLexScopedDeclaration(decl);
        return decl;
    }

    /**
     * <strong>[12.2.1] Let and Const Declarations</strong>
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
     * <strong>[12.2.1] Let and Const Declarations</strong>
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
                reportSyntaxError(Messages.Key.ConstMissingInitialiser);
            }
            binding = bindingIdentifier;
        }

        return new LexicalBinding(binding, initialiser);
    }

    /**
     * <strong>[12.2.1] Let and Const Declarations</strong>
     * 
     * <pre>
     * BindingIdentifier :
     *     Identifier
     * </pre>
     */
    private BindingIdentifier bindingIdentifier() {
        String identifier = identifier();
        if (context.strictMode != StrictMode.NonStrict) {
            if ("arguments".equals(identifier) || "eval".equals(identifier)) {
                reportStrictModeSyntaxError(Messages.Key.StrictModeRestrictedIdentifier);
            }
        }
        return new BindingIdentifier(identifier);
    }

    /**
     * <strong>[12.2.1] Let and Const Declarations</strong>
     * 
     * <pre>
     * BindingIdentifier :
     *     Identifier
     * </pre>
     */
    private BindingIdentifier bindingIdentifierStrict() {
        String identifier = identifier();
        if ("arguments".equals(identifier) || "eval".equals(identifier)) {
            reportSyntaxError(Messages.Key.StrictModeRestrictedIdentifier);
        }
        return new BindingIdentifier(identifier);
    }

    /**
     * <strong>[12.2.1] Let and Const Declarations</strong>
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
     * <strong>[12.2.2] Variable Statement</strong>
     * 
     * <pre>
     * VariableStatement :
     *     var VariableDeclarationList ;
     * </pre>
     */
    private VariableStatement variableStatement() {
        consume(Token.VAR);
        List<VariableDeclaration> decls = variableDeclarationList(true);
        semicolon();

        VariableStatement varStmt = new VariableStatement(decls);
        addVarScopedDeclaration(varStmt);
        return varStmt;
    }

    /**
     * <strong>[12.2.2] Variable Statement</strong>
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
     * <strong>[12.2.2] Variable Statement</strong>
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
     * <strong>[12.2.4] Destructuring Binding Patterns</strong>
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
     * <strong>[12.2.4] Destructuring Binding Patterns</strong>
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

        return new ObjectBindingPattern(list);
    }

    /**
     * <strong>[12.2.4] Destructuring Binding Patterns</strong>
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
        if (LOOKAHEAD(Token.COLON)) {
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
     * <strong>[12.2.4] Destructuring Binding Patterns</strong>
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
                list.add(new BindingElision());
            } else if (tok == Token.TRIPLE_DOT) {
                consume(Token.TRIPLE_DOT);
                list.add(new BindingRestElement(bindingIdentifierStrict()));
                break;
            } else {
                list.add(bindingElementStrict());
                needComma = true;
            }
        }
        consume(Token.RB);

        arrayBindingPattern_StaticSemantics(list);

        return new ArrayBindingPattern(list);
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
     * <strong>[12.2.4] Destructuring Binding Patterns</strong>
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
        Binding binding = binding();
        Expression initialiser = null;
        if (token() == Token.ASSIGN) {
            initialiser = initialiser(true);
        }

        return new BindingElement(binding, initialiser);
    }

    /**
     * <strong>[12.2.4] Destructuring Binding Patterns</strong>
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
        Binding binding = bindingStrict();
        Expression initialiser = null;
        if (token() == Token.ASSIGN) {
            initialiser = initialiser(true);
        }

        return new BindingElement(binding, initialiser);
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
                    reportSyntaxError(Messages.Key.StrictModeRestrictedIdentifier);
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
                        reportSyntaxError(Messages.Key.StrictModeRestrictedIdentifier);
                    }
                }
            } else if (element instanceof BindingRestElement) {
                String name = BoundName(((BindingRestElement) element));
                if ("arguments".equals(name) || "eval".equals(name)) {
                    reportSyntaxError(Messages.Key.StrictModeRestrictedIdentifier);
                }
            } else {
                assert element instanceof BindingElision;
            }
        }
    }

    /**
     * <strong>[12.3] Empty Statement</strong>
     * 
     * <pre>
     * EmptyStatement:
     * ;
     * </pre>
     */
    private EmptyStatement emptyStatement() {
        consume(Token.SEMI);

        return new EmptyStatement();
    }

    /**
     * <strong>[12.4] Expression Statement</strong>
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
            Expression expr = expression(true);
            semicolon();

            return new ExpressionStatement(expr);
        }
    }

    /**
     * <strong>[12.5] The <code>if</code> Statement</strong>
     * 
     * <pre>
     * IfStatement :
     *     if ( Expression ) Statement else Statement
     *     if ( Expression ) Statement
     * </pre>
     */
    private IfStatement ifStatement() {
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

        return new IfStatement(test, then, otherwise);
    }

    /**
     * <strong>[12.6.1] The <code>do-while</code> Statement</strong>
     * 
     * <pre>
     * IterationStatement :
     *     do Statement while ( Expression ) ;
     * </pre>
     */
    private DoWhileStatement doWhileStatement(Set<String> labelSet) {
        consume(Token.DO);

        LabelContext labelCx = enterIteration(labelSet);
        Statement stmt = statement();
        exitIteration();

        consume(Token.WHILE);
        consume(Token.LP);
        Expression test = expression(true);
        consume(Token.RP);
        if (token() == Token.SEMI) {
            consume(Token.SEMI);
        }

        return new DoWhileStatement(labelCx.abrupts, labelCx.labelSet, test, stmt);
    }

    /**
     * <strong>[12.6.2] The <code>while</code> Statement</strong>
     * 
     * <pre>
     * IterationStatement :
     *     while ( Expression ) Statement
     * </pre>
     */
    private WhileStatement whileStatement(Set<String> labelSet) {
        consume(Token.WHILE);
        consume(Token.LP);
        Expression test = expression(true);
        consume(Token.RP);

        LabelContext labelCx = enterIteration(labelSet);
        Statement stmt = statement();
        exitIteration();

        return new WhileStatement(labelCx.abrupts, labelCx.labelSet, test, stmt);
    }

    /**
     * <strong>[12.6.3] The <code>for</code> Statement</strong> <br>
     * <strong>[12.6.4] The <code>for-in</code> and <code>for-of</code> Statements</strong>
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
            consume(Token.VAR);
            VariableStatement varStmt = new VariableStatement(variableDeclarationList(false));
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
            reportTokenMismatch(Token.LP, "each");
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

            LabelContext labelCx = enterIteration(labelSet);
            Statement stmt = statement();
            exitIteration();

            if (lexBlockContext != null) {
                exitBlockContext();
            }

            ForStatement iteration = new ForStatement(lexBlockContext, labelCx.abrupts,
                    labelCx.labelSet, head, test, step, stmt);
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

            LabelContext labelCx = enterIteration(labelSet);
            Statement stmt = statement();
            exitIteration();

            if (lexBlockContext != null) {
                exitBlockContext();
            }

            if (each) {
                ForEachStatement iteration = new ForEachStatement(lexBlockContext, labelCx.abrupts,
                        labelCx.labelSet, head, expr, stmt);
                if (lexBlockContext != null) {
                    lexBlockContext.node = iteration;
                }
                return iteration;
            } else {
                ForInStatement iteration = new ForInStatement(lexBlockContext, labelCx.abrupts,
                        labelCx.labelSet, head, expr, stmt);
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

            LabelContext labelCx = enterIteration(labelSet);
            Statement stmt = statement();
            exitIteration();

            if (lexBlockContext != null) {
                exitBlockContext();
            }

            ForOfStatement iteration = new ForOfStatement(lexBlockContext, labelCx.abrupts,
                    labelCx.labelSet, head, expr, stmt);
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
                    reportSyntaxError(Messages.Key.DestructuringMissingInitialiser);
                }
            }
        } else if (head instanceof LexicalDeclaration) {
            boolean isConst = ((LexicalDeclaration) head).getType() == LexicalDeclaration.Type.Const;
            for (LexicalBinding decl : ((LexicalDeclaration) head).getElements()) {
                if (decl.getBinding() instanceof BindingPattern && decl.getInitialiser() == null) {
                    reportSyntaxError(Messages.Key.DestructuringMissingInitialiser);
                }
                if (isConst && decl.getInitialiser() == null) {
                    reportSyntaxError(Messages.Key.ConstMissingInitialiser);
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
            LeftHandSideExpression lhs = validateAssignment((Expression) head);
            if (lhs == null) {
                reportSyntaxError(Messages.Key.InvalidAssignmentTarget);
            }
            return lhs;
        }
        throw reportSyntaxError(Messages.Key.InvalidForInOfHead);
    }

    /**
     * Static Semantics: IsValidSimpleAssignmentTarget
     */
    private LeftHandSideExpression validateSimpleAssignment(Expression lhs) {
        if (lhs instanceof Identifier) {
            if (context.strictMode != StrictMode.NonStrict) {
                String name = ((Identifier) lhs).getName();
                if ("eval".equals(name) || "arguments".equals(name)) {
                    reportStrictModeSyntaxError(Messages.Key.StrictModeInvalidAssignmentTarget);
                }
            }
            return (Identifier) lhs;
        } else if (lhs instanceof ElementAccessor) {
            return (ElementAccessor) lhs;
        } else if (lhs instanceof PropertyAccessor) {
            return (PropertyAccessor) lhs;
        } else if (lhs instanceof SuperExpression) {
            SuperExpression superExpr = (SuperExpression) lhs;
            if (superExpr.getExpression() != null || superExpr.getName() != null) {
                return superExpr;
            }
        }
        // everything else => invalid lhs
        return null;
    }

    /**
     * Static Semantics: IsValidSimpleAssignmentTarget
     */
    private LeftHandSideExpression validateAssignment(Expression lhs) {
        // rewrite object/array literal to destructuring form
        if (lhs instanceof ObjectLiteral) {
            ObjectAssignmentPattern pattern = toDestructuring((ObjectLiteral) lhs);
            if (lhs.isParenthesised()) {
                pattern.addParentheses();
            }
            return pattern;
        } else if (lhs instanceof ArrayLiteral) {
            ArrayAssignmentPattern pattern = toDestructuring((ArrayLiteral) lhs);
            if (lhs.isParenthesised()) {
                pattern.addParentheses();
            }
            return pattern;
        }
        return validateSimpleAssignment(lhs);
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
                        reportSyntaxError(Messages.Key.InvalidDestructuring, p.getLine());
                    }
                    target = destructuringAssignmentTarget(assignment.getLeft());
                    initialiser = assignment.getRight();
                } else {
                    target = destructuringAssignmentTarget(propertyValue);
                    initialiser = null;
                }
                property = new AssignmentProperty(propertyName, target, initialiser);
            } else if (p instanceof PropertyNameDefinition) {
                // AssignmentProperty : Identifier
                PropertyNameDefinition def = (PropertyNameDefinition) p;
                property = assignmentProperty(def.getPropertyName(), null);
            } else if (p instanceof CoverInitialisedName) {
                // AssignmentProperty : Identifier Initialiser
                CoverInitialisedName def = (CoverInitialisedName) p;
                property = assignmentProperty(def.getPropertyName(), def.getInitialiser());
            } else {
                assert p instanceof MethodDefinition;
                throw reportSyntaxError(Messages.Key.InvalidDestructuring, p.getLine());
            }
            list.add(property);
        }
        context.removeLiteral(object);
        return new ObjectAssignmentPattern(list);
    }

    private ArrayAssignmentPattern toDestructuring(ArrayLiteral array) {
        List<AssignmentElementItem> list = newSmallList();
        for (Expression e : array.getElements()) {
            AssignmentElementItem element;
            if (e instanceof Elision) {
                // Elision
                element = (Elision) e;
            } else if (e instanceof SpreadElement) {
                // AssignmentRestElement : ... DestructuringAssignmentTarget
                // DestructuringAssignmentTarget : LeftHandSideExpression
                Expression expression = ((SpreadElement) e).getExpression();
                LeftHandSideExpression target = destructuringSimpleAssignmentTarget(expression);
                element = new AssignmentRestElement(target);
            } else {
                // AssignmentElement : DestructuringAssignmentTarget Initialiser{opt}
                // DestructuringAssignmentTarget : LeftHandSideExpression
                LeftHandSideExpression target;
                Expression initialiser;
                if (e instanceof AssignmentExpression) {
                    AssignmentExpression assignment = (AssignmentExpression) e;
                    if (assignment.getOperator() != AssignmentExpression.Operator.ASSIGN) {
                        reportSyntaxError(Messages.Key.InvalidDestructuring, e.getLine());
                    }
                    target = destructuringAssignmentTarget(assignment.getLeft());
                    initialiser = assignment.getRight();
                } else {
                    target = destructuringAssignmentTarget(e);
                    initialiser = null;
                }
                element = new AssignmentElement(target, initialiser);
            }
            list.add(element);
        }
        return new ArrayAssignmentPattern(list);
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
                reportSyntaxError(Messages.Key.InvalidAssignmentTarget, lhs.getLine());
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
            ObjectAssignmentPattern pattern = toDestructuring((ObjectLiteral) lhs);
            if (lhs.isParenthesised()) {
                pattern.addParentheses();
            }
            return pattern;
        } else if (extended && lhs instanceof ArrayLiteral) {
            ArrayAssignmentPattern pattern = toDestructuring((ArrayLiteral) lhs);
            if (lhs.isParenthesised()) {
                pattern.addParentheses();
            }
            return pattern;
        } else if (lhs instanceof SuperExpression) {
            SuperExpression superExpr = (SuperExpression) lhs;
            if (superExpr.getExpression() != null || superExpr.getName() != null) {
                return superExpr;
            }
        }
        // FIXME: spec bug (IsInvalidAssignmentPattern not defined) (Bug 716)
        // everything else => invalid lhs
        throw reportSyntaxError(Messages.Key.InvalidDestructuring, lhs.getLine());
    }

    private AssignmentProperty assignmentProperty(Identifier identifier, Expression initialiser) {
        switch (identifier.getName()) {
        case "eval":
        case "arguments":
        case "this":
        case "super":
            reportSyntaxError(Messages.Key.InvalidDestructuring, identifier.getLine());
        }
        return new AssignmentProperty(identifier, initialiser);
    }

    /**
     * <strong>[12.7] The <code>continue</code> Statement</strong>
     * 
     * <pre>
     * ContinueStatement :
     *     continue ;
     *     continue [no <i>LineTerminator</i> here] Identifier ;
     * </pre>
     */
    private ContinueStatement continueStatement() {
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
                throw reportSyntaxError(Messages.Key.InvalidContinueTarget);
            } else {
                throw reportSyntaxError(Messages.Key.LabelTargetNotFound, label);
            }
        }
        if (target.type != StatementType.Iteration) {
            reportSyntaxError(Messages.Key.InvalidContinueTarget);
        }
        target.mark(Abrupt.Continue);

        return new ContinueStatement(label);
    }

    /**
     * <strong>[12.8] The <code>break</code> Statement</strong>
     * 
     * <pre>
     * BreakStatement :
     *     break ;
     *     break [no <i>LineTerminator</i> here] Identifier ;
     * </pre>
     */
    private BreakStatement breakStatement() {
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
                throw reportSyntaxError(Messages.Key.InvalidBreakTarget);
            } else {
                throw reportSyntaxError(Messages.Key.LabelTargetNotFound, label);
            }
        }
        target.mark(Abrupt.Break);

        return new BreakStatement(label);
    }

    /**
     * <strong>[12.9] The <code>return</code> Statement</strong>
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

        Expression expr = null;
        consume(Token.RETURN);
        if (noLineTerminator() && !(token() == Token.SEMI || token() == Token.RC)) {
            expr = expression(true);
        }
        semicolon();

        return new ReturnStatement(expr);
    }

    /**
     * <strong>[12.10] The <code>with</code> Statement</strong>
     * 
     * <pre>
     * WithStatement :
     *     with ( Expression ) Statement
     * </pre>
     */
    private WithStatement withStatement() {
        reportStrictModeSyntaxError(Messages.Key.StrictModeWithStatement);

        consume(Token.WITH);
        consume(Token.LP);
        Expression expr = expression(true);
        consume(Token.RP);

        BlockContext scope = enterWithContext();
        Statement stmt = statement();
        exitWithContext();

        WithStatement withStatement = new WithStatement(scope, expr, stmt);
        scope.node = withStatement;
        return withStatement;
    }

    /**
     * <strong>[12.11] The <code>switch</code> Statement</strong>
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
        consume(Token.SWITCH);
        consume(Token.LP);
        Expression expr = expression(true);
        consume(Token.RP);

        consume(Token.LC);
        LabelContext labelCx = enterBreakable(labelSet);
        BlockContext scope = enterBlockContext();
        boolean hasDefault = false;
        for (;;) {
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
            clauses.add(new SwitchClause(caseExpr, list));
        }
        exitBlockContext();
        exitBreakable();
        consume(Token.RC);

        SwitchStatement switchStatement = new SwitchStatement(scope, labelCx.abrupts,
                labelCx.labelSet, expr, clauses);
        scope.node = switchStatement;
        return switchStatement;
    }

    /**
     * <strong>[12.12] Labelled Statements</strong>
     * 
     * <pre>
     * LabelledStatement :
     *     Identifier : Statement
     * </pre>
     */
    private Statement labelledStatement() {
        HashSet<String> labelSet = new HashSet<>(4);
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
            case NAME:
                if (LOOKAHEAD(Token.COLON)) {
                    String name = identifier();
                    consume(Token.COLON);
                    labelSet.add(name);
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

        LabelContext labelCx = enterLabelled(StatementType.Statement, labelSet);
        Statement stmt = statement();
        exitLabelled();

        return new LabelledStatement(labelCx.abrupts, labelCx.labelSet, stmt);
    }

    /**
     * <strong>[12.13] The <code>throw</code> Statement</strong>
     * 
     * <pre>
     * ThrowStatement :
     *     throw [no <i>LineTerminator</i> here] Expression ;
     * </pre>
     */
    private ThrowStatement throwStatement() {
        consume(Token.THROW);
        if (!noLineTerminator()) {
            reportSyntaxError(Messages.Key.UnexpectedEndOfLine);
        }
        Expression expr = expression(true);
        semicolon();

        return new ThrowStatement(expr);
    }

    /**
     * <strong>[12.14] The <code>try</code> Statement</strong>
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
        consume(Token.TRY);
        tryBlock = block(NO_INHERITED_BINDING);
        Token tok = token();
        if (tok == Token.CATCH) {
            if (isEnabled(Option.GuardedCatch)) {
                guardedCatchNodes = newSmallList();
                while (token() == Token.CATCH && catchNode == null) {
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
                        GuardedCatchNode guardedCatchNode = new GuardedCatchNode(catchScope,
                                catchParameter, guard, catchBlock);
                        catchScope.node = guardedCatchNode;
                        guardedCatchNodes.add(guardedCatchNode);
                    } else {
                        catchNode = new CatchNode(catchScope, catchParameter, catchBlock);
                        catchScope.node = catchNode;
                    }
                }
            } else {
                consume(Token.CATCH);
                BlockContext catchScope = enterBlockContext();

                consume(Token.LP);
                Binding catchParameter = binding();
                addLexDeclaredName(catchParameter);
                consume(Token.RP);

                // catch-block receives a blacklist of forbidden lexical declarable names
                BlockStatement catchBlock = block(singletonList(catchParameter));

                exitBlockContext();
                catchNode = new CatchNode(catchScope, catchParameter, catchBlock);
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

        return new TryStatement(tryBlock, catchNode, guardedCatchNodes, finallyBlock);
    }

    /**
     * <strong>[12.15] The <code>debugger</code> Statement</strong>
     * 
     * <pre>
     * DebuggerStatement :
     *     debugger ;
     * </pre>
     */
    private DebuggerStatement debuggerStatement() {
        consume(Token.DEBUGGER);
        semicolon();

        return new DebuggerStatement();
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
        consume(Token.LET);

        consume(Token.LP);
        List<LexicalBinding> bindings = bindingList(false, true);
        consume(Token.RP);

        if (token() != Token.LC && isEnabled(Option.LetExpression)) {
            // let expression disguised as let statement
            Expression expression = assignmentExpression(true);

            exitBlockContext();

            LetExpression letExpression = new LetExpression(scope, bindings, expression);
            scope.node = letExpression;
            return new ExpressionStatement(letExpression);
        } else {
            BlockStatement letBlock = block(toBindings(bindings));

            exitBlockContext();

            LetStatement block = new LetStatement(scope, bindings, letBlock);
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
     * <strong>[11.1] Primary Expressions</strong>
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
        Token tok = token();
        switch (tok) {
        case THIS:
            consume(tok);
            return new ThisExpression();
        case NULL:
            consume(tok);
            return new NullLiteral();
        case FALSE:
        case TRUE:
            consume(tok);
            return new BooleanLiteral(tok == Token.TRUE);
        case NUMBER:
            return new NumericLiteral(numericLiteral());
        case STRING:
            return new StringLiteral(stringLiteral());
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
            int line = ts.getLine();
            Identifier identifier = new Identifier(identifier());
            identifier.setLine(line);
            return identifier;
        }
    }

    private Expression functionOrGeneratorExpression() {
        if (LOOKAHEAD(Token.MUL)) {
            return generatorExpression(false);
        } else {
            long marker = ts.marker();
            try {
                return functionExpression();
            } catch (RetryGenerator e) {
                ts.reset(marker);
                return generatorExpression(true);
            }
        }
    }

    /**
     * <strong>[11.1] Primary Expressions</strong>
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
        consume(Token.LP);
        Expression expr;
        if (token() == Token.RP) {
            expr = arrowFunctionEmptyParameters();
        } else if (token() == Token.TRIPLE_DOT) {
            expr = arrowFunctionRestParameter();
        } else {
            // inlined `expression(true)`
            expr = assignmentExpressionNoValidation(true);
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
        return new EmptyExpression();
    }

    private SpreadElement arrowFunctionRestParameter() {
        consume(Token.TRIPLE_DOT);
        SpreadElement spread = new SpreadElement(new Identifier(identifier()));
        if (!(token() == Token.RP && LOOKAHEAD(Token.ARROW))) {
            reportSyntaxError(Messages.Key.InvalidSpreadExpression);
        }
        return spread;
    }

    /**
     * <strong>[11.1.4] Array Initialiser</strong>
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
            return arrayLiteral();
        }
    }

    /**
     * <strong>[11.1.4] Array Initialiser</strong>
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
    private ArrayInitialiser arrayLiteral() {
        consume(Token.LB);
        List<Expression> list = newList();
        boolean needComma = false;
        for (Token tok; (tok = token()) != Token.RB;) {
            if (needComma) {
                consume(Token.COMMA);
                needComma = false;
            } else if (tok == Token.COMMA) {
                consume(Token.COMMA);
                list.add(new Elision());
            } else if (tok == Token.TRIPLE_DOT) {
                consume(Token.TRIPLE_DOT);
                list.add(new SpreadElement(assignmentExpression(true)));
                needComma = true;
            } else {
                list.add(assignmentExpressionNoValidation(true));
                needComma = true;
            }
        }
        consume(Token.RB);

        return new ArrayLiteral(list);
    }

    /**
     * <strong>[11.1.4.2] Array Comprehension</strong>
     * 
     * <pre>
     * ArrayComprehension :
     *     [ Comprehension ]
     * </pre>
     */
    private ArrayComprehension arrayComprehension() {
        consume(Token.LB);
        Comprehension comprehension = comprehension();
        consume(Token.RB);

        return new ArrayComprehension(comprehension);
    }

    /**
     * <strong>[11.1.4.2] Array Comprehension</strong>
     * 
     * <pre>
     * Comprehension :
     *     ComprehensionFor ComprehensionQualifierTail
     * ComprehensionQualifierTail :
     *     AssignmentExpression
     *     ComprehensionQualifier ComprehensionQualifierTail
     * ComprehensionQualifier :
     *     ComprehensionFor
     *     ComprehensionIf
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
     * <strong>[11.1.4.2] Array Comprehension</strong>
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
        consume(Token.FOR);
        consume(Token.LP);
        Binding b = binding();
        consume("of");
        Expression expression = assignmentExpression(true);
        consume(Token.RP);
        BlockContext scope = enterBlockContext();
        addLexDeclaredName(b);
        return new ComprehensionFor(scope, b, expression);
    }

    /**
     * <strong>[11.1.4.2] Array Comprehension</strong>
     * 
     * <pre>
     * ComprehensionIf :
     *     if ( AssignmentExpression )
     * </pre>
     */
    private ComprehensionIf comprehensionIf() {
        consume(Token.IF);
        consume(Token.LP);
        Expression expression = assignmentExpression(true);
        consume(Token.RP);
        return new ComprehensionIf(expression);
    }

    /**
     * <strong>[11.1.5] Object Initialiser</strong>
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
        ObjectLiteral object = new ObjectLiteral(defs);
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
        final int VALUE = 0, GETTER = 1, SETTER = 2;
        Map<String, Integer> values = new HashMap<>();
        for (PropertyDefinition def : object.getProperties()) {
            PropertyName propertyName = def.getPropertyName();
            String key = propertyName.getName();
            final int kind;
            if (def instanceof PropertyValueDefinition || def instanceof PropertyNameDefinition) {
                kind = VALUE;
            } else if (def instanceof MethodDefinition) {
                MethodDefinition method = (MethodDefinition) def;
                if (method.hasSuperReference()) {
                    throw reportSyntaxError(Messages.Key.SuperOutsideClass, def.getLine());
                }
                MethodDefinition.MethodType type = method.getType();
                kind = type == MethodType.Getter ? GETTER : type == MethodType.Setter ? SETTER
                        : VALUE;
            } else {
                assert def instanceof CoverInitialisedName;
                // Always throw a Syntax Error if this production is present
                throw reportSyntaxError(Messages.Key.MissingColonAfterPropertyId, def.getLine(),
                        key);
            }
            // It is a Syntax Error if PropertyNameList of PropertyDefinitionList contains any
            // duplicate entries [...]
            if (values.containsKey(key)) {
                int prev = values.get(key);
                if (kind == VALUE && prev != VALUE) {
                    reportSyntaxError(Messages.Key.DuplicatePropertyDefinition, def.getLine(), key);
                }
                if (kind == VALUE && prev == VALUE) {
                    reportStrictModeSyntaxError(Messages.Key.DuplicatePropertyDefinition,
                            def.getLine(), key);
                }
                if (kind == GETTER && prev != SETTER) {
                    reportSyntaxError(Messages.Key.DuplicatePropertyDefinition, def.getLine(), key);
                }
                if (kind == SETTER && prev != GETTER) {
                    reportSyntaxError(Messages.Key.DuplicatePropertyDefinition, def.getLine(), key);
                }
                values.put(key, prev | kind);
            } else {
                values.put(key, kind);
            }
        }
    }

    /**
     * <strong>[11.1.5] Object Initialiser</strong>
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
        if (LOOKAHEAD(Token.COLON)) {
            PropertyName propertyName = propertyName();
            consume(Token.COLON);
            Expression propertyValue = assignmentExpressionNoValidation(true);
            return new PropertyValueDefinition(propertyName, propertyValue);
        }
        if (LOOKAHEAD(Token.COMMA) || LOOKAHEAD(Token.RC)) {
            // Static Semantics: It is a Syntax Error if IdentifierName is a
            // ReservedWord.
            int line = ts.getLine();
            Identifier identifier = new Identifier(identifier());
            identifier.setLine(line);
            return new PropertyNameDefinition(identifier);
        }
        if (LOOKAHEAD(Token.ASSIGN)) {
            int line = ts.getLine();
            Identifier identifier = new Identifier(identifier());
            identifier.setLine(line);
            consume(Token.ASSIGN);
            Expression initialiser = assignmentExpression(true);
            return new CoverInitialisedName(identifier, initialiser);
        }
        return methodDefinition(false);
    }

    /**
     * <strong>[11.1.5] Object Initialiser</strong>
     * 
     * <pre>
     * PropertyName :
     *     IdentifierName
     *     StringLiteral
     *     NumericLiteral
     * </pre>
     */
    private PropertyName propertyName() {
        switch (token()) {
        case STRING:
            return new StringLiteral(stringLiteral());
        case NUMBER:
            return new NumericLiteral(numericLiteral());
        default:
            return new Identifier(identifierName());
        }
    }

    /**
     * <strong>[11.1.7] Generator Comprehensions</strong>
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
            consume(Token.LP);
            Comprehension comprehension = comprehension();
            consume(Token.RP);

            return new GeneratorComprehension(comprehension);
        } finally {
            context.yieldAllowed = yieldAllowed;
        }
    }

    /**
     * <strong>[11.1.8] Regular Expression Literals</strong>
     * 
     * <pre>
     * RegularExpressionLiteral ::
     *     / RegularExpressionBody / RegularExpressionFlags
     * </pre>
     */
    private Expression regularExpressionLiteral(Token tok) {
        String[] re = ts.readRegularExpression(tok);
        consume(tok);
        regularExpressionLiteral_StaticSemantics(re[0], re[1]);
        return new RegularExpressionLiteral(re[0], re[1]);
    }

    private void regularExpressionLiteral_StaticSemantics(String p, String f) {
        // parse to validate regular expression, but ignore actual result
        RegExpParser.parse(p, f, ts.getLine());
    }

    /**
     * <strong>[11.1.9] Template Literals</strong>
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

        String[] values = ts.readTemplateLiteral(Token.TEMPLATE);
        elements.add(new TemplateCharacters(values[0], values[1]));
        while (token() == Token.LC) {
            consume(Token.LC);
            elements.add(expression(true));
            values = ts.readTemplateLiteral(Token.RC);
            elements.add(new TemplateCharacters(values[0], values[1]));
        }
        consume(Token.TEMPLATE);

        return new TemplateLiteral(tagged, elements);
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
        consume(Token.LET);

        consume(Token.LP);
        List<LexicalBinding> bindings = bindingList(false, true);
        consume(Token.RP);

        Expression expression = assignmentExpression(true);

        exitBlockContext();

        LetExpression letExpression = new LetExpression(scope, bindings, expression);
        scope.node = letExpression;

        return letExpression;
    }

    /**
     * <strong>[11.2] Left-Hand-Side Expressions</strong>
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
        int line = ts.getLine();
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
            lhs = new NewExpression(expr, args);
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
                lhs = new SuperExpression(name);
                break;
            case LB:
                consume(Token.LB);
                Expression expr = expression(true);
                consume(Token.RB);
                lhs = new SuperExpression(expr);
                break;
            case LP:
                if (!allowCall) {
                    lhs = new SuperExpression();
                } else {
                    List<Expression> args = arguments();
                    lhs = new SuperExpression(args);
                }
                break;
            case TEMPLATE:
                // handle "new super``" case
                throw reportSyntaxError(Messages.Key.InvalidToken, token().toString());
            default:
                if (!allowCall) {
                    lhs = new SuperExpression();
                } else {
                    throw reportSyntaxError(Messages.Key.InvalidToken, token().toString());
                }
                break;
            }
        } else {
            lhs = primaryExpression();
        }
        lhs.setLine(line);

        for (;;) {
            switch (token()) {
            case DOT:
                line = ts.getLine();
                consume(Token.DOT);
                String name = identifierName();
                lhs = new PropertyAccessor(lhs, name);
                lhs.setLine(line);
                break;
            case LB:
                line = ts.getLine();
                consume(Token.LB);
                Expression expr = expression(true);
                consume(Token.RB);
                lhs = new ElementAccessor(lhs, expr);
                lhs.setLine(line);
                break;
            case LP:
                if (!allowCall) {
                    return lhs;
                }
                if (lhs instanceof Identifier && "eval".equals(((Identifier) lhs).getName())) {
                    context.funContext.directEval = true;
                }
                line = ts.getLine();
                List<Expression> args = arguments();
                lhs = new CallExpression(lhs, args);
                lhs.setLine(line);
                break;
            case TEMPLATE:
                line = ts.getLine();
                TemplateLiteral templ = templateLiteral(true);
                lhs = new TemplateCallExpression(lhs, templ);
                lhs.setLine(line);
                break;
            default:
                return lhs;
            }
        }
    }

    /**
     * <strong>[11.2] Left-Hand-Side Expressions</strong>
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
        consume(Token.LP);
        if (token() != Token.RP) {
            for (;;) {
                Expression expr;
                if (token() == Token.TRIPLE_DOT) {
                    consume(Token.TRIPLE_DOT);
                    expr = new CallSpreadElement(assignmentExpression(true));
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
        }
        consume(Token.RP);

        return args;
    }

    /**
     * <strong>[11.3] Postfix Expressions</strong><br>
     * <strong>[11.4] Unary Operators</strong>
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
            int line = ts.getLine();
            consume(tok);
            UnaryExpression unary = new UnaryExpression(unaryOp(tok, false), unaryExpression());
            unary.setLine(line);
            if (tok == Token.INC || tok == Token.DEC) {
                if (validateSimpleAssignment(unary.getOperand()) == null) {
                    reportReferenceError(Messages.Key.InvalidIncDecTarget);
                }
            }
            if (tok == Token.DELETE) {
                Expression operand = unary.getOperand();
                if (operand instanceof Identifier) {
                    reportStrictModeSyntaxError(Messages.Key.StrictModeInvalidDeleteOperand);
                }
            }
            return unary;
        }
        default: {
            Expression lhs = leftHandSideExpression(true);
            if (noLineTerminator()) {
                tok = token();
                if (tok == Token.INC || tok == Token.DEC) {
                    if (validateSimpleAssignment(lhs) == null) {
                        reportReferenceError(Messages.Key.InvalidIncDecTarget);
                    }
                    int line = ts.getLine();
                    consume(tok);
                    UnaryExpression unary = new UnaryExpression(unaryOp(tok, true), lhs);
                    unary.setLine(line);
                    return unary;
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
            return null;
        }
    }

    /**
     * <strong>[11.5] Multiplicative Operators</strong><br>
     * <strong>[11.6] Additive Operators</strong><br>
     * <strong>[11.7] Bitwise Shift Operators</strong><br>
     * <strong>[11.8] Relational Operators</strong><br>
     * <strong>[11.9] Equality Operators</strong><br>
     * <strong>[11.10] Binary Bitwise Operators</strong><br>
     * <strong>[11.11] Binary Logical Operators</strong><br>
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
     * <strong>[11.12] Conditional Operator</strong><br>
     * <strong>[11.13] Assignment Operators</strong>
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
        // TODO: this may need to be changed...
        if (token() == Token.YIELD) {
            return yieldExpression();
        }
        long marker = ts.marker();
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
            ts.reset(marker);
            return arrowFunction();
        } else if (tok == Token.ASSIGN) {
            LeftHandSideExpression lhs = validateAssignment(left);
            if (lhs == null) {
                reportReferenceError(Messages.Key.InvalidAssignmentTarget);
            }
            consume(Token.ASSIGN);
            Expression right = assignmentExpression(allowIn);
            return new AssignmentExpression(assignmentOp(tok), lhs, right);
        } else if (isAssignmentOperator(tok)) {
            LeftHandSideExpression lhs = validateSimpleAssignment(left);
            if (lhs == null) {
                reportReferenceError(Messages.Key.InvalidAssignmentTarget);
            }
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
            return null;
        }
    }

    /**
     * <strong>[11.13] Assignment Operators</strong>
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
     * <strong>[11.14] Comma Operator</strong>
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
     * <strong>[7.9] Automatic Semicolon Insertion</strong>
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
     * <strong>[7.6] Identifier Names and Identifiers</strong>
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
     * <strong>[7.6] Identifier Names and Identifiers</strong>
     */
    private boolean isIdentifier(Token tok) {
        return isIdentifier(tok, context.strictMode);
    }

    /**
     * <strong>[7.6] Identifier Names and Identifiers</strong>
     */
    private boolean isIdentifier(Token tok, StrictMode strictMode) {
        switch (tok) {
        case NAME:
            return true;
        case IMPLEMENTS:
        case INTERFACE:
        case PACKAGE:
        case PRIVATE:
        case PROTECTED:
        case PUBLIC:
        case STATIC:
            // TODO: otherwise cannot parse YieldExpression, context dependent syntax restriction?
            // case YIELD:
            if (strictMode != StrictMode.NonStrict) {
                reportStrictModeSyntaxError(Messages.Key.StrictModeInvalidIdentifier, getName(tok));
            }
            return (strictMode != StrictMode.Strict);
        default:
            return false;
        }
    }

    /**
     * <strong>[7.6] Identifier Names and Identifiers</strong>
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
     * <strong>[7.6] Identifier Names and Identifiers</strong>
     */
    private static boolean isIdentifierName(Token tok) {
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
        case ENUM:
        case EXPORT:
        case EXTENDS:
        case FALSE:
        case FINALLY:
        case FOR:
        case FUNCTION:
        case IF:
        case IMPLEMENTS:
        case IMPORT:
        case IN:
        case INSTANCEOF:
        case INTERFACE:
        case LET:
        case NAME:
        case NEW:
        case NULL:
        case PACKAGE:
        case PRIVATE:
        case PROTECTED:
        case PUBLIC:
        case RETURN:
        case STATIC:
        case SUPER:
        case SWITCH:
        case THIS:
        case THROW:
        case TRUE:
        case TRY:
        case TYPEOF:
        case VAR:
        case VOID:
        case WHILE:
        case WITH:
        case YIELD:
            return true;
        default:
            return false;
        }
    }

    /**
     * <strong>[7.8.3] Numeric Literals</strong>
     */
    private double numericLiteral() {
        double number = ts.getNumber();
        consume(Token.NUMBER);
        return number;
    }

    /**
     * <strong>[7.8.4] String Literals</strong>
     */
    private String stringLiteral() {
        String string = ts.getString();
        consume(Token.STRING);
        return string;
    }
}

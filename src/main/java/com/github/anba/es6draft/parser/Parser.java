/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import static com.github.anba.es6draft.semantics.StaticSemantics.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.nio.file.Path;
import java.util.*;

import com.github.anba.es6draft.ast.AbruptNode.Abrupt;
import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.MethodDefinition.MethodType;
import com.github.anba.es6draft.parser.ParserException.ExceptionType;
import com.github.anba.es6draft.regexp.RegExpParser;
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
public final class Parser {
    private static final boolean DEBUG = false;

    private static final int MAX_ARGUMENTS = FunctionPrototype.getMaxArguments();
    private static final List<Binding> NO_INHERITED_BINDING = Collections.emptyList();
    private static final Set<String> EMPTY_LABEL_SET = Collections.emptySet();

    private final Path sourceFile;
    private final String sourceName;
    private final int sourceLine;
    private final EnumSet<CompatibilityOption> options;
    private final EnumSet<Option> parserOptions;
    private TokenStream ts;
    private ParseContext context;

    private enum StrictMode {
        Unknown, Strict, NonStrict
    }

    private enum StatementType {
        Iteration, Breakable, Statement
    }

    private enum ContextKind {
        Script, Module, AsyncFunction, Function, Generator, ArrowFunction, GeneratorComprehension,
        Method;

        final boolean isScript() {
            return this == Script;
        }

        final boolean isModule() {
            return this == Module;
        }

        final boolean isFunction() {
            switch (this) {
            case ArrowFunction:
            case AsyncFunction:
            case Function:
            case Generator:
            case GeneratorComprehension:
            case Method:
                return true;
            case Module:
            case Script:
            default:
                return false;
            }
        }
    }

    private static final class ParseContext {
        final ParseContext parent;
        final ContextKind kind;

        boolean superReference = false;
        boolean yieldAllowed = false;
        boolean awaitAllowed = false;
        boolean returnAllowed = false;
        boolean noDivAfterYield = false;
        boolean legacyGenerator = false;
        boolean explicitStrict = false;

        StrictMode strictMode = StrictMode.Unknown;
        ParserException strictError = null;
        List<FunctionNode> deferred = null;
        ArrayDeque<ObjectLiteral> objectLiterals = null;

        HashMap<String, LabelContext> labelSet = null;
        LabelContext labels = null;

        ScopeContext scopeContext;
        final TopContext topContext;
        final ScriptContext scriptContext;
        final ModuleContext modContext;
        final FunctionContext funContext;

        ParseContext() {
            this.parent = null;
            this.kind = null;
            this.topContext = null;
            this.scriptContext = null;
            this.modContext = null;
            this.funContext = null;
        }

        ParseContext(ParseContext parent, ContextKind kind) {
            this.parent = parent;
            this.kind = kind;
            if (kind.isScript()) {
                this.scriptContext = new ScriptContext(this);
                this.modContext = null;
                this.funContext = null;
                this.topContext = scriptContext;
            } else if (kind.isModule()) {
                this.scriptContext = null;
                this.modContext = new ModuleContext(this);
                this.funContext = null;
                this.topContext = modContext;
            } else {
                assert kind.isFunction();
                this.scriptContext = null;
                this.modContext = null;
                this.funContext = new FunctionContext(this);
                this.topContext = funContext;
            }
            this.scopeContext = topContext;
            this.returnAllowed = kind.isFunction();
            if (parent.strictMode == StrictMode.Strict) {
                this.strictMode = parent.strictMode;
            }
        }

        ParseContext findSuperContext() {
            ParseContext cx = this;
            while (cx.kind == ContextKind.ArrowFunction
                    || cx.kind == ContextKind.GeneratorComprehension) {
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

        int countLiterals() {
            return objectLiterals != null ? objectLiterals.size() : 0;
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

        boolean assertLiteralsUnchecked(int expected) {
            int count = countLiterals();
            assert count == expected : String.format(
                    "%d unchecked object literals, but expected %d", count, expected);
            return count == expected;
        }
    }

    private static final class FunctionContext extends TopContext implements FunctionScope {
        FunctionNode node = null;
        HashSet<String> parameterNames = null;

        FunctionContext(ParseContext context) {
            super(context);
        }

        @Override
        public FunctionNode getNode() {
            return node;
        }

        @Override
        public Set<String> parameterNames() {
            return parameterNames;
        }

        @Override
        public boolean isDynamic() {
            return directEval && !IsStrict(node);
        }

        @Override
        public boolean isDeclared(String name) {
            if ("arguments".equals(name)
                    && !(node instanceof ArrowFunction || node instanceof GeneratorComprehension)) {
                return true;
            }
            if (parameterNames.contains(name)) {
                return true;
            }
            return super.isDeclared(name);
        }
    }

    private static final class ScriptContext extends TopContext implements ScriptScope {
        Script node = null;

        ScriptContext(ParseContext context) {
            super(context);
        }

        @Override
        public Script getNode() {
            return node;
        }
    }

    private static final class ModuleContext extends TopContext implements ModuleScope {
        LinkedHashSet<String> moduleRequests = new LinkedHashSet<>();
        HashSet<String> exportBindings = new HashSet<>();
        Module node = null;

        ModuleContext(ParseContext context) {
            super(context);
        }

        @Override
        public Module getNode() {
            return node;
        }

        @Override
        public Set<String> getModuleRequests() {
            return moduleRequests;
        }

        @Override
        public Set<String> getExportBindings() {
            return exportBindings;
        }

        void addModuleRequest(String moduleSpecifier) {
            moduleRequests.add(moduleSpecifier);
        }

        boolean addExportedBindings(String binding) {
            return exportBindings.add(binding);
        }
    }

    private static abstract class TopContext extends ScopeContext implements TopLevelScope {
        final ScopeContext enclosing;
        boolean directEval = false;
        List<StatementListItem> varScopedDeclarations = null;

        TopContext(ParseContext context) {
            super(null);
            this.enclosing = context.parent.scopeContext;
        }

        void addVarScopedDeclaration(StatementListItem decl) {
            if (varScopedDeclarations == null) {
                varScopedDeclarations = newSmallList();
            }
            varScopedDeclarations.add(decl);
        }

        @Override
        public ScopeContext getEnclosingScope() {
            return enclosing;
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

        @Override
        public boolean isDeclared(String name) {
            if (varDeclaredNames != null && varDeclaredNames.contains(name)) {
                return true;
            }
            if (lexDeclaredNames != null && lexDeclaredNames.contains(name)) {
                return true;
            }
            return false;
        }
    }

    private static final class BlockContext extends ScopeContext implements BlockScope {
        ScopedNode node = null;

        BlockContext(ScopeContext parent) {
            super(parent);
        }

        @Override
        public ScopedNode getNode() {
            return node;
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
        public boolean isDeclared(String name) {
            return lexDeclaredNames != null && lexDeclaredNames.contains(name);
        }
    }

    private static final class WithContext extends ScopeContext implements WithScope {
        WithStatement node = null;

        WithContext(ScopeContext parent) {
            super(parent);
        }

        @Override
        public WithStatement getNode() {
            return node;
        }

        @Override
        public boolean isDeclared(String name) {
            return false;
        }
    }

    private abstract static class ScopeContext implements Scope {
        final ScopeContext parent;

        HashSet<String> varDeclaredNames = null;
        HashSet<String> lexDeclaredNames = null;
        List<Declaration> lexScopedDeclarations = null;

        ScopeContext(ScopeContext parent) {
            this.parent = parent;
        }

        @Override
        public Scope getParent() {
            return parent;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("var: ").append(varDeclaredNames != null ? varDeclaredNames : "<null>");
            sb.append("\t");
            sb.append("lex: ").append(lexDeclaredNames != null ? lexDeclaredNames : "<null>");
            return sb.toString();
        }

        boolean allowVarDeclaredName(String name) {
            return lexDeclaredNames == null || !lexDeclaredNames.contains(name);
        }

        void addVarDeclaredNames(HashSet<String> names) {
            if (varDeclaredNames == null) {
                varDeclaredNames = names;
            } else {
                varDeclaredNames.addAll(names);
            }
        }

        boolean addVarDeclaredName(String name) {
            if (varDeclaredNames == null) {
                varDeclaredNames = new HashSet<>();
            }
            varDeclaredNames.add(name);
            return lexDeclaredNames == null || !lexDeclaredNames.contains(name);
        }

        boolean addLexDeclaredName(String name) {
            if (lexDeclaredNames == null) {
                lexDeclaredNames = new HashSet<>();
            }
            return lexDeclaredNames.add(name)
                    && (varDeclaredNames == null || !varDeclaredNames.contains(name));
        }

        void addLexScopedDeclaration(Declaration decl) {
            if (lexScopedDeclarations == null) {
                lexScopedDeclarations = newSmallList();
            }
            lexScopedDeclarations.add(decl);
        }
    }

    private static final class LabelContext {
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
    private static final class RetryGenerator extends RuntimeException {
    }

    public enum Option {
        Strict, FunctionCode, LocalScope, DirectEval, EvalScript, EnclosedByWithStatement
    }

    public Parser(String sourceName, int sourceLine, Set<CompatibilityOption> compatOptions) {
        this(null, sourceName, sourceLine, compatOptions, EnumSet.noneOf(Option.class));
    }

    public Parser(Path sourceFile, String sourceName, int sourceLine,
            Set<CompatibilityOption> compatOptions) {
        this(sourceFile, sourceName, sourceLine, compatOptions, EnumSet.noneOf(Option.class));
    }

    public Parser(String sourceName, int sourceLine, Set<CompatibilityOption> compatOptions,
            EnumSet<Option> options) {
        this(null, sourceName, sourceLine, compatOptions, options);
    }

    public Parser(Path sourceFile, String sourceName, int sourceLine,
            Set<CompatibilityOption> compatOptions, EnumSet<Option> options) {
        this.sourceFile = sourceFile;
        this.sourceName = sourceName;
        this.sourceLine = sourceLine;
        this.options = EnumSet.copyOf(compatOptions);
        this.parserOptions = EnumSet.copyOf(options);
        context = new ParseContext();
        context.strictMode = this.parserOptions.contains(Option.Strict) ? StrictMode.Strict
                : StrictMode.NonStrict;
    }

    String getSourceName() {
        return sourceName;
    }

    int getSourceLine() {
        return sourceLine;
    }

    boolean isEnabled(CompatibilityOption option) {
        return options.contains(option);
    }

    boolean isEnabled(Option option) {
        return parserOptions.contains(option);
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

    private WithContext enterWithContext() {
        WithContext cx = new WithContext(context.scopeContext);
        context.scopeContext = cx;
        return cx;
    }

    private ScopeContext exitWithContext() {
        return exitScopeContext();
    }

    private BlockContext enterBlockContext() {
        BlockContext cx = new BlockContext(context.scopeContext);
        context.scopeContext = cx;
        return cx;
    }

    private BlockContext enterBlockContext(Binding binding) {
        BlockContext cx = enterBlockContext();
        addLexDeclaredName(binding);
        return cx;
    }

    private BlockContext enterBlockContext(List<Binding> bindings) {
        BlockContext cx = enterBlockContext();
        addLexDeclaredNames(bindings);
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
            parent.addVarDeclaredNames(varDeclaredNames);
            scope.varDeclaredNames = null;
        }
        return context.scopeContext = parent;
    }

    private static String BoundName(BindingIdentifier binding) {
        return binding.getName();
    }

    private void addAsyncFunctionDeclaration(AsyncFunctionDeclaration decl) {
        addDeclaration(decl, BoundName(decl.getIdentifier()));
    }

    private void addFunctionDeclaration(FunctionDeclaration decl) {
        addDeclaration(decl, BoundName(decl.getIdentifier()));
    }

    private void addGeneratorDeclaration(GeneratorDeclaration decl) {
        addDeclaration(decl, BoundName(decl.getIdentifier()));
    }

    private <DECLARATION extends Declaration & FunctionNode> void addDeclaration(DECLARATION decl,
            String name) {
        ParseContext parentContext = context.parent;
        ScopeContext parentScope = parentContext.scopeContext;
        TopContext topScope = parentContext.topContext;
        if (parentScope == topScope && !parentContext.kind.isModule()) {
            // top-level function declaration in scripts/functions context
            topScope.addVarScopedDeclaration(decl);
            if (!topScope.addVarDeclaredName(name)) {
                reportSyntaxError(decl, Messages.Key.VariableRedeclaration, name);
            }
        } else {
            // lexical-scoped function declaration in module/block context
            parentScope.addLexScopedDeclaration(decl);
            if (!parentScope.addLexDeclaredName(name)) {
                reportSyntaxError(decl, Messages.Key.VariableRedeclaration, name);
            }
        }
    }

    private void addLexScopedDeclaration(Declaration decl) {
        context.scopeContext.addLexScopedDeclaration(decl);
    }

    private void addLexScopedDeclaration(ImportDeclaration decl) {
        assert context.scopeContext == context.modContext : "not in module scope";
        // TODO: check if ImportDeclarations really need to be stored as lexically scoped
    }

    private void addVarScopedDeclaration(VariableStatement decl) {
        context.topContext.addVarScopedDeclaration(decl);
    }

    /**
     * <strong>[13.1] Block</strong>
     * <p>
     * Static Semantics: Early Errors<br>
     * <ul>
     * <li>It is a Syntax Error if any element of the LexicallyDeclaredNames of StatementList also
     * occurs in the VarDeclaredNames of StatementList.
     * </ul>
     * 
     * @param binding
     *            the binding to add to the variable scope
     */
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
        addVarDeclaredName(bindingIdentifier, name);
    }

    private void addVarDeclaredName(BindingPattern bindingPattern) {
        for (String name : BoundNames(bindingPattern)) {
            addVarDeclaredName(bindingPattern, name);
        }
    }

    private void addVarDeclaredName(Binding binding, String name) {
        ScopeContext scope = context.scopeContext;
        if (!scope.addVarDeclaredName(name)) {
            reportSyntaxError(binding, Messages.Key.VariableRedeclaration, name);
        }
        for (ScopeContext parent = scope.parent; parent != null; parent = parent.parent) {
            if (!parent.allowVarDeclaredName(name)) {
                reportSyntaxError(binding, Messages.Key.VariableRedeclaration, name);
            }
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
     * 
     * @param binding
     *            the binding to add to the lexical scope
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
        addLexDeclaredName(bindingIdentifier, name);
    }

    private void addLexDeclaredName(BindingPattern bindingPattern) {
        for (String name : BoundNames(bindingPattern)) {
            addLexDeclaredName(bindingPattern, name);
        }
    }

    private void addLexDeclaredName(Binding binding, String name) {
        ScopeContext scope = context.scopeContext;
        if (!scope.addLexDeclaredName(name)) {
            reportSyntaxError(binding, Messages.Key.VariableRedeclaration, name);
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

    private void addExportedBindings(long sourcePosition, List<String> bindings) {
        for (String binding : bindings) {
            addExportedBindings(sourcePosition, binding);
        }
    }

    private void addExportedBindings(long sourcePosition, String binding) {
        assert context.scopeContext == context.modContext : "not in module scope";
        if (!context.modContext.addExportedBindings(binding)) {
            reportSyntaxError(sourcePosition, Messages.Key.DuplicateExport, binding);
        }
    }

    private void addModuleRequest(String moduleSpecifier) {
        assert context.scopeContext == context.modContext : "not in module scope";
        // TODO: remove from parser if not subject to early error restrictions
        context.modContext.addModuleRequest(moduleSpecifier);
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
     * Report mismatched token error from tokenstream's current position.
     * 
     * @param expected
     *            the expected token
     * @param actual
     *            the actual token in the token stream
     * @return the parser exception
     */
    private ParserException reportTokenMismatch(Token expected, Token actual) {
        throw reportTokenMismatch(expected.toString(), actual);
    }

    /**
     * Report mismatched token error from tokenstream's current position.
     * 
     * @param actual
     *            the actual token in the token stream
     * @return the parser exception
     */
    private ParserException reportTokenNotIdentifier(Token actual) {
        if (Token.isIdentifierName(actual)) {
            throw reportSyntaxError(Messages.Key.InvalidIdentifier, getName(actual));
        }
        throw reportTokenMismatch("<identifier>", actual);
    }

    /**
     * Report mismatched token error from tokenstream's current position.
     * 
     * @param actual
     *            the actual token in the token stream
     * @return the parser exception
     */
    private ParserException reportTokenNotIdentifierName(Token actual) {
        throw reportTokenMismatch("<identifier-name>", actual);
    }

    /**
     * Report mismatched token error from tokenstream's current position.
     * 
     * @param expected
     *            the expected token description
     * @param actual
     *            the actual token in the token stream
     * @return the parser exception
     */
    private ParserException reportTokenMismatch(String expected, Token actual) {
        if (actual == Token.EOF) {
            throw reportEofError(ts.sourcePosition(), Messages.Key.UnexpectedToken,
                    actual.toString(), expected);
        }
        throw reportError(ExceptionType.SyntaxError, ts.sourcePosition(),
                Messages.Key.UnexpectedToken, actual.toString(), expected);
    }

    /**
     * Report parser eof-error with the given position.
     * 
     * @param sourcePosition
     *            the source position for the error
     * @param messageKey
     *            the error message key
     * @param args
     *            the error message arguments
     * @return the parser exception
     */
    private ParserEOFException reportEofError(long sourcePosition, Messages.Key messageKey,
            String... args) {
        int line = toLine(sourcePosition), column = toColumn(sourcePosition);
        throw new ParserEOFException(sourceName, line, column, messageKey, args);
    }

    /**
     * Report parser error with the given type and position.
     * 
     * @param type
     *            the exception type for the error
     * @param sourcePosition
     *            the source position for the error
     * @param messageKey
     *            the error message key
     * @param args
     *            the error message arguments
     * @return the parser exception
     */
    private ParserException reportError(ExceptionType type, long sourcePosition,
            Messages.Key messageKey, String... args) {
        int line = toLine(sourcePosition), column = toColumn(sourcePosition);
        throw new ParserException(type, sourceName, line, column, messageKey, args);
    }

    /**
     * Report syntax error from the given position.
     * 
     * @param sourcePosition
     *            the source position for the error
     * @param messageKey
     *            the error message key
     * @param args
     *            the error message arguments
     * @return the parser exception
     */
    private ParserException reportSyntaxError(long sourcePosition, Messages.Key messageKey,
            String... args) {
        throw reportError(ExceptionType.SyntaxError, sourcePosition, messageKey, args);
    }

    /**
     * Report syntax error from the node's begin source-position.
     * 
     * @param node
     *            the node which could not be parsed without errors
     * @param messageKey
     *            the error message key
     * @param args
     *            the error message arguments
     * @return the parser exception
     */
    private ParserException reportSyntaxError(Node node, Messages.Key messageKey, String... args) {
        throw reportSyntaxError(node.getBeginPosition(), messageKey, args);
    }

    /**
     * Report syntax error from tokenstream's current position.
     * 
     * @param messageKey
     *            the error message key
     * @param args
     *            the error message arguments
     * @return the parser exception
     */
    private ParserException reportSyntaxError(Messages.Key messageKey, String... args) {
        throw reportSyntaxError(ts.sourcePosition(), messageKey, args);
    }

    /**
     * Report (or store) strict-mode parser error with the given type and position.
     * 
     * @param type
     *            the exception type for the error
     * @param sourcePosition
     *            the source position for the error
     * @param messageKey
     *            the error message key
     * @param args
     *            the error message arguments
     */
    private void reportStrictModeError(ExceptionType type, long sourcePosition,
            Messages.Key messageKey, String... args) {
        if (context.strictMode == StrictMode.Unknown) {
            if (context.strictError == null) {
                int line = toLine(sourcePosition), column = toColumn(sourcePosition);
                context.strictError = new ParserException(type, sourceName, line, column,
                        messageKey, args);
            }
        } else if (context.strictMode == StrictMode.Strict) {
            reportError(type, sourcePosition, messageKey, args);
        }
    }

    /**
     * Report (or store) strict-mode syntax error from the given position.
     * 
     * @param sourcePosition
     *            the source position for the error
     * @param messageKey
     *            the error message key
     * @param args
     *            the error message arguments
     */
    private void reportStrictModeSyntaxError(long sourcePosition, Messages.Key messageKey,
            String... args) {
        reportStrictModeError(ExceptionType.SyntaxError, sourcePosition, messageKey, args);
    }

    /**
     * Report (or store) strict-mode syntax error from the node's source-position.
     * 
     * @param node
     *            the node which could not be parsed without errors
     * @param messageKey
     *            the error message key
     * @param args
     *            the error message arguments
     */
    private void reportStrictModeSyntaxError(Node node, Messages.Key messageKey, String... args) {
        reportStrictModeError(ExceptionType.SyntaxError, node.getBeginPosition(), messageKey, args);
    }

    /**
     * Report (or store) strict-mode syntax error from tokenstream's current position.
     * 
     * @param messageKey
     *            the error message key
     * @param args
     *            the error message arguments
     */
    void reportStrictModeSyntaxError(Messages.Key messageKey, String... args) {
        reportStrictModeError(ExceptionType.SyntaxError, ts.sourcePosition(), messageKey, args);
    }

    /**
     * Peeks the next token in the token-stream.
     * 
     * @return the next token
     */
    private Token peek() {
        return ts.peekToken();
    }

    /**
     * Checks whether the next token in the token-stream is equal to the input token.
     *
     * @param token
     *            the token to test
     * @return {@code true} if the next token matches
     */
    private boolean LOOKAHEAD(Token token) {
        return ts.peekToken() == token;
    }

    /**
     * Returns the current token in the token-stream.
     * 
     * @return the current token
     */
    private Token token() {
        return ts.currentToken();
    }

    /**
     * Consumes the current token in the token-stream and advances the stream to the next token.
     * 
     * @param tok
     *            the token to consume
     */
    private void consume(Token tok) {
        if (tok != token())
            reportTokenMismatch(tok, token());
        Token next = ts.nextToken();
        if (DEBUG)
            System.out.printf("consume(%s) -> %s\n", tok, next);
    }

    /**
     * Consumes the current token in the token-stream and advances the stream to the next token.
     * 
     * @param name
     *            the name string to consume
     */
    private void consume(String name) {
        long sourcePos = ts.sourcePosition();
        String string = ts.getString();
        consume(Token.NAME);
        if (!name.equals(string))
            reportSyntaxError(sourcePos, Messages.Key.UnexpectedName, string, name);
    }

    /**
     * Parses the input source as script code.
     * 
     * @param source
     *            the source string to parse
     * @return the parsed script
     * @throws ParserException
     *             if the input source could not be parsed successfully
     */
    public Script parseScript(CharSequence source) throws ParserException {
        if (ts != null)
            throw new IllegalStateException();
        ts = new TokenStream(this, new TokenStreamInput(source));
        return script();
    }

    /**
     * Parses the input source as module code.
     * 
     * @param source
     *            the source string to parse
     * @return the parsed module
     * @throws ParserException
     *             if the input source could not be parsed successfully
     */
    public Module parseModule(CharSequence source) throws ParserException {
        if (ts != null)
            throw new IllegalStateException();
        ts = new TokenStream(this, new TokenStreamInput(source));
        return module();
    }

    /**
     * Parses the input source as function code.
     * 
     * @param formals
     *            the function formal parameters source
     * @param bodyText
     *            the function body source text
     * @return the parsed function
     * @throws ParserException
     *             if the input source could not be parsed successfully
     */
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
                ts = new TokenStream(this, new TokenStreamInput(formals)).initialize();
                FormalParameterList parameters = formalParameters(Token.EOF);
                if (token() != Token.EOF) {
                    reportSyntaxError(Messages.Key.InvalidFormalParameterList);
                }
                if (ts.position() != formals.length()) {
                    // more input after last token (whitespace, comments), add newlines to handle
                    // last token is single-line comment case
                    formals = "\n" + formals + "\n";
                }

                ts = new TokenStream(this, new TokenStreamInput(bodyText)).initialize();
                List<StatementListItem> statements = functionBody(Token.EOF);
                if (token() != Token.EOF) {
                    reportSyntaxError(Messages.Key.InvalidFunctionBody);
                }

                String header = String.format("function anonymous (%s) ", formals);
                String body = String.format("\n%s\n", bodyText);

                FunctionContext scope = context.funContext;
                function = new FunctionExpression(beginSource(), ts.endPosition(), scope,
                        "anonymous", parameters, statements, context.hasSuperReference(), header,
                        body);
                scope.node = function;

                function_EarlyErrors(function);

                function = inheritStrictness(function);
            } catch (RetryGenerator e) {
                // don't bother with legacy support here
                throw reportSyntaxError(Messages.Key.InvalidYieldExpression);
            } finally {
                restoreContext();
            }

            createScript(function);

            return function;
        } finally {
            restoreContext();
        }
    }

    /**
     * Parses the input source as generator function code.
     * 
     * @param formals
     *            the function formal parameters source
     * @param bodyText
     *            the function body source text
     * @return the parsed function
     * @throws ParserException
     *             if the input source could not be parsed successfully
     */
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
                ts = new TokenStream(this, new TokenStreamInput(formals)).initialize();
                FormalParameterList parameters = formalParameters(Token.EOF);
                if (token() != Token.EOF) {
                    reportSyntaxError(Messages.Key.InvalidFormalParameterList);
                }
                if (ts.position() != formals.length()) {
                    // more input after last token (whitespace, comments), add newlines to handle
                    // last token is single-line comment case
                    formals = "\n" + formals + "\n";
                }

                ts = new TokenStream(this, new TokenStreamInput(bodyText)).initialize();
                List<StatementListItem> statements = functionBody(Token.EOF);
                if (token() != Token.EOF) {
                    reportSyntaxError(Messages.Key.InvalidFunctionBody);
                }

                String header = String.format("function* anonymous (%s) ", formals);
                String body = String.format("\n%s\n", bodyText);

                FunctionContext scope = context.funContext;
                generator = new GeneratorExpression(beginSource(), ts.endPosition(), scope,
                        "anonymous", parameters, statements, context.hasSuperReference(), header,
                        body);
                scope.node = generator;

                generator_EarlyErrors(generator);

                generator = inheritStrictness(generator);
            } finally {
                restoreContext();
            }

            createScript(generator);

            return generator;
        } finally {
            restoreContext();
        }
    }

    private <FUNEXPR extends Expression & FunctionNode> Script createScript(FUNEXPR funExpr) {
        StatementListItem statement = new ExpressionStatement(funExpr.getBeginPosition(),
                funExpr.getEndPosition(), funExpr);
        List<StatementListItem> statements = singletonList(statement);
        boolean strict = (context.strictMode == StrictMode.Strict);

        ScriptContext scope = context.scriptContext;
        Script script = new Script(beginSource(), ts.endPosition(), sourceFile, sourceName, scope,
                statements, options, parserOptions, strict);
        scope.node = script;

        return script;
    }

    /* ***************************************************************************************** */

    /**
     * <strong>[15.1] Scripts</strong>
     * 
     * <pre>
     * Script :
     *     ScriptBody<span><sub>opt</sub></span>
     * ScriptBody :
     *     StatementList
     * </pre>
     * 
     * @return the parsed script node
     */
    private Script script() {
        newContext(ContextKind.Script);
        try {
            ts.initialize();
            List<StatementListItem> prologue = directivePrologue();
            List<StatementListItem> body = statementList(Token.EOF);
            List<StatementListItem> statements = merge(prologue, body);
            assert context.assertLiteralsUnchecked(0);
            boolean strict = (context.strictMode == StrictMode.Strict);

            ScriptContext scope = context.scriptContext;
            Script script = new Script(beginSource(), ts.endPosition(), sourceFile, sourceName,
                    scope, statements, options, parserOptions, strict);
            scope.node = script;

            return script;
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[15.2] Modules</strong>
     * 
     * <pre>
     * Module :
     *     ModuleBody<span><sub>opt</sub></span>
     * ModuleBody :
     *     ModuleItemList
     * </pre>
     * 
     * @return the parsed module node
     */
    private Module module() {
        newContext(ContextKind.Module);
        context.strictMode = StrictMode.Strict;
        try {
            ts.initialize();
            List<ModuleItem> statements = moduleItemList();
            assert context.assertLiteralsUnchecked(0);

            ModuleContext scope = context.modContext;
            Module module = new Module(beginSource(), ts.endPosition(), sourceName, scope,
                    statements, options, parserOptions);
            scope.node = module;

            return module;
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[15.2] Modules</strong>
     * 
     * <pre>
     * ModuleItemList :
     *     ModuleItem
     *     ModuleItemList ModuleItem
     * ModuleItem :
     *     ImportDeclaration
     *     ExportDeclaration
     *     StatementListItem
     * </pre>
     * 
     * @return the list of parsed module items
     */
    private List<ModuleItem> moduleItemList() {
        List<ModuleItem> moduleItemList = newList();
        while (token() != Token.EOF) {
            if (token() == Token.EXPORT) {
                moduleItemList.add(exportDeclaration());
            } else if (token() == Token.IMPORT) {
                moduleItemList.add(importDeclaration());
            } else if (isName("module") && isIdentifier(peek()) && noNextLineTerminator()) {
                // FIXME: broken `module <linebreak> let a;`
                moduleItemList.add(importDeclaration());
            } else {
                moduleItemList.add(statementListItem());
            }
        }
        return moduleItemList;
    }

    /**
     * <strong>[15.2.1] Imports</strong>
     * 
     * <pre>
     * ImportDeclaration :
     *     ModuleImport
     *     import ImportClause FromClause ;
     *     import ModuleSpecifier ;
     * </pre>
     * 
     * @return the parsed import declaration
     */
    private ImportDeclaration importDeclaration() {
        long begin = ts.beginPosition();
        ImportDeclaration decl;
        if (token() != Token.IMPORT) {
            ModuleImport moduleImport = moduleImport();

            decl = new ImportDeclaration(begin, ts.endPosition(), moduleImport);
            addLexScopedDeclaration(decl);
        } else {
            consume(Token.IMPORT);
            if (token() != Token.STRING) {
                ImportClause importClause = importClause();
                String moduleSpecifier = fromClause();
                semicolon();

                // Only lexically scoped if BoundNames(ImportDeclaration) is not empty
                boolean isLexicallyScoped = importClause.getDefaultEntry() != null
                        || !importClause.getNamedImports().isEmpty();

                decl = new ImportDeclaration(begin, ts.endPosition(), importClause, moduleSpecifier);
                if (isLexicallyScoped) {
                    addLexScopedDeclaration(decl);
                }
                addModuleRequest(moduleSpecifier);
            } else {
                String moduleSpecifier = moduleSpecifier();
                semicolon();

                decl = new ImportDeclaration(begin, ts.endPosition(), moduleSpecifier);
                addModuleRequest(moduleSpecifier);
            }
        }
        return decl;
    }

    /**
     * <strong>[15.2.1] Imports</strong>
     * 
     * <pre>
     * ModuleImport :
     *     module [no <i>LineTerminator</i> here] ImportedBinding FromClause ;
     * </pre>
     * 
     * @return the parsed module import
     */
    private ModuleImport moduleImport() {
        long begin = ts.beginPosition();
        consume("module");
        if (!noLineTerminator()) {
            reportSyntaxError(Messages.Key.UnexpectedEndOfLine);
        }
        BindingIdentifier importedBinding = importedBinding();
        String moduleSpecifier = fromClause();
        semicolon();

        addLexDeclaredName(importedBinding);
        addModuleRequest(moduleSpecifier);

        return new ModuleImport(begin, ts.endPosition(), importedBinding, moduleSpecifier);
    }

    /**
     * <strong>[15.2.1] Imports</strong>
     * 
     * <pre>
     * FromClause :
     *     from ModuleSpecifier
     * </pre>
     * 
     * @return the parsed from-clause's module specifier
     */
    private String fromClause() {
        consume("from");
        return moduleSpecifier();
    }

    /**
     * <strong>[15.2.1] Imports</strong>
     * 
     * <pre>
     * ImportClause :
     *     ImportedBinding
     *     ImportedBinding , NamedImports
     *     NamedImports
     * </pre>
     * 
     * @return the parsed import clause
     */
    private ImportClause importClause() {
        long begin = ts.beginPosition();
        BindingIdentifier defaultEntry = null;
        List<ImportSpecifier> namedImports = emptyList();
        if (token() != Token.LC) {
            defaultEntry = importedBinding();
            addLexDeclaredName(defaultEntry);

            if (token() == Token.COMMA) {
                consume(Token.COMMA);
                namedImports = namedImports();
            }
        } else {
            namedImports = namedImports();
        }
        return new ImportClause(begin, ts.endPosition(), defaultEntry, namedImports);
    }

    /**
     * <strong>[15.2.1] Imports</strong>
     * 
     * <pre>
     * NamedImports :
     *     { } 
     *     { ImportsList }
     *     { ImportsList , }
     * ImportsList :
     *     ImportSpecifier
     *     ImportsList , ImportSpecifier
     * </pre>
     * 
     * @return the list of parsed named imports
     */
    private List<ImportSpecifier> namedImports() {
        List<ImportSpecifier> namedImports = newList();
        consume(Token.LC);
        while (token() != Token.RC) {
            namedImports.add(importSpecifier());
            if (token() == Token.COMMA) {
                consume(Token.COMMA);
            } else {
                break;
            }
        }
        consume(Token.RC);
        return namedImports;
    }

    /**
     * <strong>[15.2.1] Imports</strong>
     * 
     * <pre>
     * ImportSpecifier :
     *     ImportedBinding
     *     IdentifierName as ImportedBinding
     * </pre>
     * 
     * @return the parsed import specifier
     */
    private ImportSpecifier importSpecifier() {
        long begin = ts.beginPosition();
        String importName;
        BindingIdentifier localName;
        if (importSpecifierFollowSet(peek())) {
            BindingIdentifier binding = importedBinding();
            importName = binding.getName();
            localName = binding;
        } else {
            importName = identifierName();
            consume("as");
            localName = importedBinding();
        }
        addLexDeclaredName(localName);

        return new ImportSpecifier(begin, ts.endPosition(), importName, localName);
    }

    /**
     * Returns FOLLOW(ImportSpecifier): « <tt>,</tt> , <tt>}</tt> »
     *
     * @param token
     *            the token to test
     * @return {@code true} if token is in the follow-set
     */
    private boolean importSpecifierFollowSet(Token token) {
        switch (token) {
        case COMMA:
        case RC:
            return true;
        default:
            return false;
        }
    }

    /**
     * <strong>[15.2.1] Imports</strong>
     * 
     * <pre>
     * ModuleSpecifier :
     *     StringLiteral
     * </pre>
     * 
     * @return the parsed module specifier string
     */
    private String moduleSpecifier() {
        return stringLiteral();
    }

    /**
     * <strong>[15.2.1] Imports</strong>
     * 
     * <pre>
     * ImportedBinding :
     *     BindingIdentifier
     * </pre>
     * 
     * @return the parsed imported binding node
     */
    private BindingIdentifier importedBinding() {
        return bindingIdentifier();
    }

    /**
     * <strong>[15.2.2] Exports</strong>
     * 
     * <pre>
     * ExportDeclaration :
     *     export * FromClause ;
     *     export ExportsClause<span><sub>[NoReference]</sub></span> FromClause ;
     *     export ExportsClause ;
     *     export VariableStatement
     *     export Declaration<span><sub>[Default]</sub></span>
     *     export default AssignmentExpression<span><sub>[In]</sub></span> ;
     * </pre>
     * 
     * @return the parsed export declaration
     */
    private ExportDeclaration exportDeclaration() {
        long begin = ts.beginPosition();
        consume(Token.EXPORT);
        switch (token()) {
        case MUL: {
            // export * FromClause ;
            consume(Token.MUL);
            String moduleSpecifier = fromClause();
            semicolon();

            addModuleRequest(moduleSpecifier);

            return new ExportDeclaration(begin, ts.endPosition(), moduleSpecifier);
        }

        case LC: {
            // export ExportsClause[NoReference] FromClause ;
            // export ExportsClause ;
            long position = ts.position(), lineinfo = ts.lineinfo();
            ExportsClause exportsClause = exportsClause(true);
            if (isName("from")) {
                String moduleSpecifier = fromClause();
                semicolon();

                for (ExportSpecifier export : exportsClause.getExports()) {
                    addExportedBindings(export.getBeginPosition(), export.getExportName());
                }
                addModuleRequest(moduleSpecifier);

                return new ExportDeclaration(begin, ts.endPosition(), exportsClause,
                        moduleSpecifier);
            }
            // rollback and try resolving exports with local references
            ts.reset(position, lineinfo);
            exportsClause = exportsClause(false);
            semicolon();

            for (ExportSpecifier export : exportsClause.getExports()) {
                addExportedBindings(export.getBeginPosition(), export.getExportName());
            }

            return new ExportDeclaration(begin, ts.endPosition(), exportsClause);
        }

        case VAR: {
            // export VariableStatement
            VariableStatement variableStatement = variableStatement();

            addExportedBindings(begin, BoundNames(variableStatement));

            return new ExportDeclaration(begin, ts.endPosition(), variableStatement);
        }

        case FUNCTION:
        case CLASS:
        case CONST:
        case LET:
        case NAME: {
            // export Declaration[Default]
            Declaration declaration = declaration(true);

            addExportedBindings(begin, BoundNames(declaration));

            return new ExportDeclaration(begin, ts.endPosition(), declaration);
        }

        case DEFAULT:
        default: {
            // export default AssignmentExpression[In] ;
            consume(Token.DEFAULT);
            Expression expression = assignmentExpression(true);
            semicolon();

            // FIXME: default exports add a lexical declared name with value "default" (spec bug?)
            // BindingIdentifier id = new BindingIdentifier(beginPosition, ts.endPosition(),
            // "default");
            // addLexDeclaredName(id);

            addExportedBindings(begin, "default");

            return new ExportDeclaration(begin, ts.endPosition(), expression);
        }
        }
    }

    /**
     * <strong>[15.2.2] Exports</strong>
     * 
     * <pre>
     * ExportsClause<span><sub>[NoReference]</sub></span> :
     *     { } 
     *     { ExportsList<span><sub>[?NoReference]</sub></span> }
     *     { ExportsList<span><sub>[?NoReference]</sub></span> , }
     * ExportsList<span><sub>[NoReference]</sub></span> :
     *     ExportSpecifier<span><sub>[?NoReference]</sub></span>
     *     ExportsList<span><sub>[?NoReference]</sub></span> , ExportSpecifier<span><sub>[?NoReference]</sub></span>
     * </pre>
     * 
     * @param noReference
     *            the flag to allow/disallow references
     * @return the parsed exports clause
     */
    private ExportsClause exportsClause(boolean noReference) {
        long begin = ts.beginPosition();
        List<ExportSpecifier> exports = newList();
        consume(Token.LC);
        while (token() != Token.RC) {
            exports.add(exportSpecifier(noReference));
            if (token() == Token.COMMA) {
                consume(Token.COMMA);
            } else {
                break;
            }
        }
        consume(Token.RC);
        return new ExportsClause(begin, ts.endPosition(), exports);
    }

    /**
     * <strong>[15.2.2] Exports</strong>
     * 
     * <pre>
     * ExportSpecifier<span><sub>[NoReference]</sub></span> :
     *     <span><sub>[~NoReference]</sub></span>IdentifierReference
     *     <span><sub>[~NoReference]</sub></span>IdentifierReference as IdentifierName
     *     <span><sub>[+NoReference]</sub></span>IdentifierName
     *     <span><sub>[+NoReference]</sub></span>IdentifierName as IdentifierName
     * </pre>
     * 
     * @param noReference
     *            the flag to allow/disallow references
     * @return the parsed export specifier
     */
    private ExportSpecifier exportSpecifier(boolean noReference) {
        long begin = ts.beginPosition();
        String importName, localName, exportName;
        if (noReference) {
            String sourceName = identifierName();
            if (isName("as")) {
                consume("as");
                exportName = identifierName();
            } else {
                exportName = sourceName;
            }
            importName = sourceName;
            localName = null;
        } else {
            importName = null;
            localName = identifierReference().getName();
            if (isName("as")) {
                consume("as");
                exportName = identifierName();
            } else {
                exportName = localName;
            }
        }

        return new ExportSpecifier(begin, ts.endPosition(), importName, localName, exportName);
    }

    /**
     * <strong>[15.3] Directive Prologues and the Use Strict Directive</strong>
     * 
     * <pre>
     * DirectivePrologue :
     *   Directive<span><sub>opt</sub></span>
     * Directive:
     *   StringLiteral ;
     *   Directive StringLiteral ;
     * </pre>
     * 
     * @return the parsed list of directives
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
                if (noNextLineTerminator() || stringLiteralFollowSetNextLine(next)) {
                    break directive;
                }
                break;
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
     * <strong>[14.1] Function Definitions</strong>
     * 
     * <pre>
     * FunctionDeclaration<span><sub>[Yield, Default]</sub></span> :
     *     function BindingIdentifier<span><sub>[?Yield, ?Default]</sub></span> ( FormalParameters ) { FunctionBody }
     * </pre>
     * 
     * @param allowDefault
     *            the flag to select if 'default' is allowed as the function name
     * @return the parsed function declaration
     */
    private FunctionDeclaration functionDeclaration(boolean allowDefault) {
        newContext(ContextKind.Function);
        try {
            long begin = ts.beginPosition();
            consume(Token.FUNCTION);
            int startFunction = ts.position() - "function".length();
            BindingIdentifier identifier = bindingIdentifierFunctionName(true, allowDefault);
            consume(Token.LP);
            FormalParameterList parameters = formalParameters(Token.RP);
            consume(Token.RP);

            String header, body;
            List<StatementListItem> statements;
            if (token() != Token.LC && isEnabled(CompatibilityOption.ExpressionClosure)) {
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
                    identifier, parameters, statements, context.hasSuperReference(), header, body);
            scope.node = function;

            function_EarlyErrors(function);

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
     *     function BindingIdentifier<span><sub>opt</sub></span> ( FormalParameters ) { FunctionBody }
     * </pre>
     * 
     * @return the parsed function expression
     */
    private FunctionExpression functionExpression() {
        newContext(ContextKind.Function);
        try {
            long begin = ts.beginPosition();
            consume(Token.FUNCTION);
            int startFunction = ts.position() - "function".length();
            BindingIdentifier identifier = null;
            if (token() != Token.LP) {
                identifier = bindingIdentifierFunctionName(false, false);
            }
            consume(Token.LP);
            FormalParameterList parameters = formalParameters(Token.RP);
            consume(Token.RP);

            String header, body;
            List<StatementListItem> statements;
            if (token() != Token.LC && isEnabled(CompatibilityOption.ExpressionClosure)) {
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
                    identifier, parameters, statements, context.hasSuperReference(), header, body);
            scope.node = function;

            function_EarlyErrors(function);

            return inheritStrictness(function);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[14.1] Function Definitions</strong>
     * 
     * <pre>
     * StrictFormalParameters<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     FormalParameters<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     * </pre>
     * 
     * @param end
     *            the end token
     * @return the parsed formal parameters list
     */
    private FormalParameterList strictFormalParameters(Token end) {
        return formalParameters(end);
    }

    /**
     * <strong>[14.1] Function Definitions</strong>
     * 
     * <pre>
     * FormalParameters<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     [empty]
     *     FormalParameterList<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     * </pre>
     * 
     * @param end
     *            the end token
     * @return the parsed formal parameters list
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
     * FormalParameterList<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     FunctionRestParameter<span><sub>[?Yield]</sub></span>
     *     FormalsList<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     *     FormalsList<span><sub>[?Yield, ?GeneratorParameter]</sub></span>, FunctionRestParameter<span><sub>[?Yield]</sub></span>
     * FormalsList<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     FormalParameter<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     *     FormalsList<span><sub>[?Yield, ?GeneratorParameter]</sub></span>, FormalParameter<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     * </pre>
     * 
     * @return the parsed formal parameters list
     */
    private FormalParameterList formalParameterList() {
        long begin = ts.beginPosition();
        List<FormalParameter> formals = newSmallList();
        for (;;) {
            if (token() == Token.TRIPLE_DOT) {
                formals.add(functionRestParameter());
                break;
            } else {
                formals.add(formalParameter());
                if (token() == Token.COMMA) {
                    consume(Token.COMMA);
                } else {
                    break;
                }
            }
        }
        return new FormalParameterList(begin, ts.endPosition(), formals);
    }

    /**
     * <strong>[14.1] Function Definitions</strong>
     * 
     * <pre>
     * FunctionRestParameter<span><sub>[Yield]</sub></span> :
     *     BindingRestElement<span><sub>[?Yield]</sub></span>
     * </pre>
     * 
     * @return the parsed formal parameter
     */
    private FormalParameter functionRestParameter() {
        return bindingRestElement();
    }

    /**
     * <strong>[14.1] Function Definitions</strong>
     * 
     * <pre>
     * FormalParameter<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     BindingElement<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     * </pre>
     * 
     * @return the parsed formal parameter
     */
    private FormalParameter formalParameter() {
        return bindingElement();
    }

    private static String containsAny(HashSet<String> set, List<String> list) {
        for (String element : list) {
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

    private static String findDuplicate(HashSet<String> set, List<String> list) {
        assert list.size() > set.size();
        HashSet<String> copy = new HashSet<>(set);
        for (String element : list) {
            if (!copy.remove(element)) {
                return element;
            }
        }
        assert false : String.format("no duplicate: %s - %s", set, list);
        return null;
    }

    private void checkFormalParameterDuplication(FunctionNode node, List<String> boundNames,
            HashSet<String> names) {
        boolean hasDuplicates = (boundNames.size() != names.size());
        if (hasDuplicates) {
            String duplicate = findDuplicate(names, boundNames);
            reportSyntaxError(node, Messages.Key.DuplicateFormalParameter, duplicate);
        }
    }

    /**
     * 14.1.1 Static Semantics: Early Errors
     * 
     * @param function
     *            the function definition to validate
     */
    private void function_EarlyErrors(FunctionDefinition function) {
        assert context.scopeContext == context.funContext;

        FunctionContext scope = context.funContext;
        FormalParameterList parameters = function.getParameters();
        List<String> boundNames = BoundNames(parameters);
        scope.parameterNames = new HashSet<>(boundNames);

        boolean strict = (context.strictMode != StrictMode.NonStrict);
        boolean simple = IsSimpleParameterList(parameters);
        if (!simple) {
            checkFormalParameterRedeclaration(function, boundNames, scope.varDeclaredNames);
        }
        checkFormalParameterRedeclaration(function, boundNames, scope.lexDeclaredNames);
        if (strict) {
            strictFormalParameters_EarlyErrors(function, boundNames, scope.parameterNames, simple);
        } else {
            formalParameters_EarlyErrors(function, boundNames, scope.parameterNames, simple);
        }
    }

    /**
     * 14.1.1 Static Semantics: Early Errors
     * 
     * @param node
     *            the function definition to validate
     * @param boundNames
     *            the list of bound parameter names
     * @param names
     *            the set of names to validate
     * @param simple
     *            the flag to describe simple parameter lists
     */
    private void strictFormalParameters_EarlyErrors(FunctionNode node, List<String> boundNames,
            HashSet<String> names, boolean simple) {
        checkFormalParameterDuplication(node, boundNames, names);
        formalParameters_EarlyErrors(node, boundNames, names, simple);
    }

    /**
     * 14.1.1 Static Semantics: Early Errors
     * 
     * @param node
     *            the function definition to validate
     * @param boundNames
     *            the list of bound parameter names
     * @param names
     *            the set of names to validate
     * @param simple
     *            the flag to describe simple parameter lists
     */
    private void formalParameters_EarlyErrors(FunctionNode node, List<String> boundNames,
            HashSet<String> names, boolean simple) {
        if (!simple) {
            checkFormalParameterDuplication(node, boundNames, names);
        }
    }

    /**
     * <strong>[14.1] Function Definitions</strong>
     * 
     * <pre>
     * FunctionBody<span><sub>[Yield]</sub></span> :
     *     FunctionStatementList<span><sub>[?Yield]</sub></span>
     * FunctionStatementList<span><sub>[Yield]</sub></span> :
     *     StatementList<span><sub>[?Yield, Return]opt</sub></span>
     * </pre>
     * 
     * @param end
     *            the end token
     * @return the list of parsed statement list items
     */
    private List<StatementListItem> functionBody(Token end) {
        // enable 'yield' if in generator
        context.yieldAllowed = (context.kind == ContextKind.Generator);
        // enable 'await' if in async function
        context.awaitAllowed = (context.kind == ContextKind.AsyncFunction);
        List<StatementListItem> prologue = directivePrologue();
        List<StatementListItem> body = statementList(end);
        assert context.assertLiteralsUnchecked(0);
        return merge(prologue, body);
    }

    /**
     * <strong>[14.1] Function Definitions</strong>
     * 
     * <pre>
     * ExpressionClosureBody<span><sub>[Yield]</sub></span> :
     *     AssignmentExpression<span><sub>[In, ?Yield]</sub></span>
     * </pre>
     * 
     * @return the list of parsed statement list items
     */
    private List<StatementListItem> expressionClosureBody() {
        // need to call manually b/c directivePrologue() isn't used here
        applyStrictMode(false);
        Expression expr = assignmentExpression(true);
        assert context.assertLiteralsUnchecked(0);
        return Collections.<StatementListItem> singletonList(new ReturnStatement(
                ts.beginPosition(), ts.endPosition(), expr));
    }

    /**
     * <strong>[14.2] Arrow Function Definitions</strong>
     * 
     * <pre>
     * ArrowFunction<span><sub>[In, Yield]</sub></span> :
     *     ArrowParameters<span><sub>[?Yield]</sub></span> {@literal =>} ConciseBody<span><sub>[?In]</sub></span>
     * ArrowParameters<span><sub>[Yield]</sub></span> :
     *     BindingIdentifier<span><sub>[?Yield]</sub></span>
     *     CoverParenthesizedExpressionAndArrowParameterList<span><sub>[?Yield]</sub></span>
     * ConciseBody<span><sub>[In]</sub></span> :
     *     [LA &#x2209; { <b>{</b> }] AssignmentExpression<span><sub>[?In]</sub></span>
     *     { FunctionBody }
     * </pre>
     * 
     * <h2>Supplemental Syntax</h2>
     * 
     * <pre>
     * ArrowFormalParameters<span><sub>[Yield]</sub></span> :
     *     ( StrictFormalParameters<span><sub>[?Yield]</sub></span> )
     * </pre>
     * 
     * @param allowIn
     *            the flag to select whether or not the in-operator is allowed
     * @return the parsed arrow function
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
                // handle `YieldExpression = yield RegExp` in ArrowParameters
                context.noDivAfterYield = context.parent.kind == ContextKind.Generator;
                parameters = strictFormalParameters(Token.RP);
                context.noDivAfterYield = false;
                consume(Token.RP);

                source.append(ts.range(start, ts.position()));
            } else {
                BindingIdentifier identifier = bindingIdentifier();
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

                arrowFunction_EarlyErrors(function);

                return inheritStrictness(function);
            } else {
                // need to call manually b/c functionBody() isn't used here
                applyStrictMode(false);

                int startBody = ts.position();
                Expression expression = assignmentExpression(allowIn);
                assert context.assertLiteralsUnchecked(0);
                int endFunction = ts.position();

                String header = source.toString();
                String body = "return " + ts.range(startBody, endFunction);

                FunctionContext scope = context.funContext;
                ArrowFunction function = new ArrowFunction(begin, ts.endPosition(), scope,
                        parameters, expression, header, body);
                scope.node = function;

                arrowFunction_EarlyErrors(function);

                return inheritStrictness(function);
            }
        } finally {
            restoreContext();
        }
    }

    /**
     * 14.2.1 Static Semantics: Early Errors
     * 
     * @param function
     *            the arrow function node to validate
     */
    private void arrowFunction_EarlyErrors(ArrowFunction function) {
        assert context.scopeContext == context.funContext;

        FunctionContext scope = context.funContext;
        FormalParameterList parameters = function.getParameters();
        List<String> boundNames = BoundNames(parameters);
        scope.parameterNames = new HashSet<>(boundNames);

        boolean simple = IsSimpleParameterList(parameters);
        checkFormalParameterRedeclaration(function, boundNames, scope.varDeclaredNames);
        checkFormalParameterRedeclaration(function, boundNames, scope.lexDeclaredNames);
        strictFormalParameters_EarlyErrors(function, boundNames, scope.parameterNames, simple);
    }

    /**
     * <strong>[14.3] Method Definitions</strong>
     * 
     * <pre>
     * MethodDefinition<span><sub>[Yield]</sub></span> :
     *     PropertyName<span><sub>[?Yield]</sub></span> ( StrictFormalParameters ) { FunctionBody }
     *     GeneratorMethod<span><sub>[?Yield]</sub></span>
     *     get PropertyName<span><sub>[?Yield]</sub></span> ( ) { FunctionBody }
     *     set PropertyName<span><sub>[?Yield]</sub></span> ( PropertySetParameterList ) { FunctionBody }
     * </pre>
     * 
     * @return the parsed method definition
     */
    private MethodDefinition methodDefinition() {
        switch (methodType()) {
        case AsyncFunction:
            return asyncMethod();
        case Generator:
            return generatorMethod();
        case Getter:
            return getterMethod();
        case Setter:
            return setterMethod();
        case Function:
        default:
            return normalMethod();
        }
    }

    /**
     * <strong>[14.3] Method Definitions</strong>
     * 
     * <pre>
     * MethodDefinition<span><sub>[Yield]</sub></span> :
     *     PropertyName<span><sub>[?Yield]</sub></span> ( StrictFormalParameters ) { FunctionBody }
     * </pre>
     * 
     * @return the parsed method definition
     */
    private MethodDefinition normalMethod() {
        long begin = ts.beginPosition();
        PropertyName propertyName = propertyName();
        return normalMethod(begin, propertyName);
    }

    private MethodDefinition normalMethod(long begin, PropertyName propertyName) {
        newContext(ContextKind.Method);
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

            methodDefinition_EarlyErrors(method);

            return inheritStrictness(method);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[14.3] Method Definitions</strong>
     * 
     * <pre>
     * MethodDefinition<span><sub>[Yield]</sub></span> :
     *     get PropertyName<span><sub>[?Yield]</sub></span> ( ) { FunctionBody }
     * </pre>
     * 
     * @return the parsed getter method definition
     */
    private MethodDefinition getterMethod() {
        long begin = ts.beginPosition();

        consume(Token.NAME); // "get"
        PropertyName propertyName = propertyName();

        newContext(ContextKind.Method);
        try {
            consume(Token.LP);
            int startFunction = ts.position() - 1;
            FormalParameterList parameters = new FormalParameterList(ts.beginPosition(),
                    ts.endPosition(), Collections.<FormalParameter> emptyList());
            consume(Token.RP);

            List<StatementListItem> statements;
            String header, body;
            if (token() != Token.LC && isEnabled(CompatibilityOption.ExpressionClosure)) {
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

            methodDefinition_EarlyErrors(method);

            return inheritStrictness(method);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[14.3] Method Definitions</strong>
     * 
     * <pre>
     * MethodDefinition<span><sub>[Yield]</sub></span> :
     *     set PropertyName<span><sub>[?Yield]</sub></span> ( PropertySetParameterList ) { FunctionBody }
     * </pre>
     * 
     * @return the parsed setter method definition
     */
    private MethodDefinition setterMethod() {
        long begin = ts.beginPosition();

        consume(Token.NAME); // "set"
        PropertyName propertyName = propertyName();

        newContext(ContextKind.Method);
        try {
            consume(Token.LP);
            int startFunction = ts.position() - 1;
            FormalParameterList parameters = propertySetParameterList();
            consume(Token.RP);

            List<StatementListItem> statements;
            String header, body;
            if (token() != Token.LC && isEnabled(CompatibilityOption.ExpressionClosure)) {
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

            methodDefinition_EarlyErrors(method);

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
     *     FormalParameter
     * </pre>
     * 
     * @return the parsed formal parameters list
     */
    private FormalParameterList propertySetParameterList() {
        long begin = ts.beginPosition();
        FormalParameter setParameter = formalParameter();
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
            if (isEnabled(CompatibilityOption.AsyncFunction) && "async".equals(name)
                    && isPropertyName(peek())) {
                return MethodType.AsyncFunction;
            }
        }
        return MethodType.Function;
    }

    private static boolean isPropertyName(Token token) {
        switch (token) {
        case STRING:
        case NUMBER:
        case LB:
            return true;
        default:
            return Token.isIdentifierName(token);
        }
    }

    /**
     * 14.3.1 Static Semantics: Early Errors
     * 
     * @param method
     *            the method definition node to validate
     */
    private void methodDefinition_EarlyErrors(MethodDefinition method) {
        assert context.scopeContext == context.funContext;

        FunctionContext scope = context.funContext;
        FormalParameterList parameters = method.getParameters();
        List<String> boundNames = BoundNames(parameters);
        scope.parameterNames = new HashSet<>(boundNames);

        boolean simple = IsSimpleParameterList(parameters);
        switch (method.getType()) {
        case AsyncFunction:
        case Function:
        case Generator: {
            checkFormalParameterRedeclaration(method, boundNames, scope.varDeclaredNames);
            checkFormalParameterRedeclaration(method, boundNames, scope.lexDeclaredNames);
            strictFormalParameters_EarlyErrors(method, boundNames, scope.parameterNames, simple);
            return;
        }
        case Setter: {
            if (!simple) {
                checkFormalParameterRedeclaration(method, boundNames, scope.varDeclaredNames);
            }
            checkFormalParameterRedeclaration(method, boundNames, scope.lexDeclaredNames);
            propertySetParameterList_EarlyErrors(method, boundNames, scope.parameterNames, simple);
            return;
        }
        case Getter:
        default:
            return;
        }
    }

    /**
     * 14.3.1 Static Semantics: Early Errors
     * 
     * @param node
     *            the function definition to validate
     * @param boundNames
     *            the list of bound parameter names
     * @param names
     *            the set of names to validate
     * @param simple
     *            the flag to describe simple parameter lists
     */
    private void propertySetParameterList_EarlyErrors(FunctionNode node, List<String> boundNames,
            HashSet<String> names, boolean simple) {
        boolean hasEvalOrArguments = (names.contains("eval") || names.contains("arguments"));
        if (!simple && hasEvalOrArguments) {
            reportSyntaxError(node, Messages.Key.StrictModeRestrictedIdentifier);
        }
        checkFormalParameterDuplication(node, boundNames, names);
    }

    /**
     * <strong>[14.4] Generator Function Definitions</strong>
     * 
     * <pre>
     * GeneratorMethod<span><sub>[Yield]</sub></span> :
     *     * PropertyName<span><sub>[?Yield]</sub></span> ( StrictFormalParameters<span><sub>[Yield, GeneratorParameter]</sub></span> ) { FunctionBody<span><sub>[Yield]</sub></span> }
     * </pre>
     * 
     * @return the parsed generator method definition
     */
    private MethodDefinition generatorMethod() {
        long begin = ts.beginPosition();

        consume(Token.MUL);
        PropertyName propertyName = propertyName();

        newContext(ContextKind.Generator);
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

            methodDefinition_EarlyErrors(method);

            return inheritStrictness(method);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[14.4] Generator Function Definitions</strong>
     * 
     * <pre>
     * GeneratorDeclaration<span><sub>[Yield, Default]</sub></span> :
     *     function * BindingIdentifier<span><sub>[?Yield, ?Default]</sub></span> ( FormalParameters<span><sub>[Yield, GeneratorParameter]</sub></span> ) { FunctionBody<span><sub>[Yield]</sub></span> }
     * </pre>
     * 
     * @param allowDefault
     *            the flag to select if 'default' is allowed as the function name
     * @param isLegacy
     *            the flag to mark this generator as a legacy, star-less generator
     * @return the parsed generator declaration
     */
    private GeneratorDeclaration generatorDeclaration(boolean allowDefault, boolean isLegacy) {
        newContext(ContextKind.Generator);
        try {
            context.legacyGenerator = isLegacy;

            long begin = ts.beginPosition();
            consume(Token.FUNCTION);
            int startFunction = ts.position() - "function".length();
            if (!isLegacy) {
                consume(Token.MUL);
            }
            BindingIdentifier identifier = bindingIdentifierFunctionName(true, allowDefault);
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
            if (!isLegacy) {
                generator = new GeneratorDeclaration(begin, ts.endPosition(), scope, identifier,
                        parameters, statements, context.hasSuperReference(), header, body);
            } else {
                generator = new LegacyGeneratorDeclaration(begin, ts.endPosition(), scope,
                        identifier, parameters, statements, context.hasSuperReference(), header,
                        body);
            }
            scope.node = generator;

            generator_EarlyErrors(generator);

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
     *     function * BindingIdentifier<span><sub>[Yield]opt</sub></span> ( FormalParameters<span><sub>[Yield, GeneratorParameter]</sub></span> ) { FunctionBody<span><sub>[Yield]</sub></span> }
     * </pre>
     * 
     * @param isLegacy
     *            the flag to mark this generator as a legacy, star-less generator
     * @return the parsed generator expression declaration
     */
    private GeneratorExpression generatorExpression(boolean isLegacy) {
        newContext(ContextKind.Generator);
        try {
            context.legacyGenerator = isLegacy;

            long begin = ts.beginPosition();
            consume(Token.FUNCTION);
            int startFunction = ts.position() - "function".length();
            if (!isLegacy) {
                consume(Token.MUL);
            }
            BindingIdentifier identifier = null;
            if (token() != Token.LP) {
                identifier = bindingIdentifierFunctionName(false, false);
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
            if (!isLegacy) {
                generator = new GeneratorExpression(begin, ts.endPosition(), scope, identifier,
                        parameters, statements, context.hasSuperReference(), header, body);
            } else {
                generator = new LegacyGeneratorExpression(begin, ts.endPosition(), scope,
                        identifier, parameters, statements, context.hasSuperReference(), header,
                        body);
            }
            scope.node = generator;

            generator_EarlyErrors(generator);

            return inheritStrictness(generator);
        } finally {
            restoreContext();
        }
    }

    /**
     * 14.4.1 Static Semantics: Early Errors
     * 
     * @param generator
     *            the generator to validate
     */
    private void generator_EarlyErrors(GeneratorDefinition generator) {
        assert context.scopeContext == context.funContext;

        FunctionContext scope = context.funContext;
        FormalParameterList parameters = generator.getParameters();
        List<String> boundNames = BoundNames(parameters);
        scope.parameterNames = new HashSet<>(boundNames);

        boolean strict = (context.strictMode != StrictMode.NonStrict);
        boolean simple = IsSimpleParameterList(parameters);
        if (!simple) {
            checkFormalParameterRedeclaration(generator, boundNames, scope.varDeclaredNames);
        }
        checkFormalParameterRedeclaration(generator, boundNames, scope.lexDeclaredNames);
        if (strict) {
            strictFormalParameters_EarlyErrors(generator, boundNames, scope.parameterNames, simple);
        } else {
            formalParameters_EarlyErrors(generator, boundNames, scope.parameterNames, simple);
        }
    }

    /**
     * <strong>[14.4] Generator Function Definitions</strong>
     * 
     * <pre>
     * YieldExpression<span><sub>[In]</sub></span> :
     *     yield
     *     yield [no <i>LineTerminator</i> here] <span style="font-size:smaller">[Lexical goal <i>InputElementRegExp</i>]</span> AssignmentExpression<span><sub>[?In, Yield]</sub></span>
     *     yield [no <i>LineTerminator</i> here] * <span style="font-size:smaller">[Lexical goal <i>InputElementRegExp</i>]</span> AssignmentExpression<span><sub>[?In, Yield]</sub></span>
     * </pre>
     * 
     * @param allowIn
     *            the flag to select whether or not the in-operator is allowed
     * @return the parsed yield expression
     */
    private YieldExpression yieldExpression(boolean allowIn) {
        assert context.kind == ContextKind.Generator && context.yieldAllowed;
        long begin = ts.beginPosition();
        consume(Token.YIELD);
        boolean delegatedYield = false;
        if (token() == Token.MUL) {
            if (!noLineTerminator()) {
                reportSyntaxError(Messages.Key.UnexpectedEndOfLine);
            }
            consume(Token.MUL);
            delegatedYield = true;
        }
        Expression expr;
        if (delegatedYield) {
            expr = assignmentExpression(allowIn);
        } else if (!context.legacyGenerator) {
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
        case IMPLEMENTS:
        case INTERFACE:
        case PACKAGE:
        case PRIVATE:
        case PROTECTED:
        case PUBLIC:
        case STATIC:
        case NAME:
        case ESCAPED_NAME:
        case ESCAPED_RESERVED_WORD:
        case ESCAPED_STRICT_RESERVED_WORD:
        case ESCAPED_YIELD:
        case ESCAPED_LET:
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
     * ClassDeclaration<span><sub>[Yield, Default]</sub></span> :
     *     class BindingIdentifier<span><sub>[?Yield, ?Default]</sub></span> ClassTail<span><sub>[?Yield]</sub></span>
     * ClassTail<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     [~GeneratorParameter] ClassHeritage<span><sub>[?Yield]opt</sub></span> { ClassBody<span><sub>[?Yield]opt</sub></span> }
     *     [+GeneratorParameter] ClassHeritage<span><sub>opt</sub></span> { ClassBody<span><sub>opt</sub></span> }
     * </pre>
     * 
     * @param allowDefault
     *            the flag to select if 'default' is allowed as the class name
     * @return the parsed class declaration
     */
    private ClassDeclaration classDeclaration(boolean allowDefault) {
        StrictMode strictMode = context.strictMode;
        try {
            // 10.2.1 - ClassDeclaration and ClassExpression is always strict code
            context.strictMode = StrictMode.Strict;
            long begin = ts.beginPosition();
            consume(Token.CLASS);
            BindingIdentifier name = bindingIdentifierClassName(allowDefault);
            Expression heritage = null;
            if (token() == Token.EXTENDS) {
                heritage = classHeritage();
            }
            consume(Token.LC);
            BlockContext scope = enterBlockContext(name);
            List<MethodDefinition> staticMethods = newList();
            List<MethodDefinition> prototypeMethods = newList();
            classBody(name, staticMethods, prototypeMethods);
            exitBlockContext();
            consume(Token.RC);

            ClassDeclaration decl = new ClassDeclaration(begin, ts.endPosition(), scope, name,
                    heritage, staticMethods, prototypeMethods);
            scope.node = decl;
            addLexDeclaredName(name);
            addLexScopedDeclaration(decl);
            return decl;
        } finally {
            context.strictMode = strictMode;
        }
    }

    /**
     * <strong>[14.5] Class Definitions</strong>
     * 
     * <pre>
     * ClassExpression<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     class BindingIdentifier<span><sub>[?Yield]opt</sub></span> ClassTail<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     * ClassTail<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     [~GeneratorParameter] ClassHeritage<span><sub>[?Yield]opt</sub></span> { ClassBody<span><sub>[?Yield]opt</sub></span> }
     *     [+GeneratorParameter] ClassHeritage<span><sub>opt</sub></span> { ClassBody<span><sub>opt</sub></span> }
     * </pre>
     * 
     * @return the parsed class expression
     */
    private ClassExpression classExpression() {
        StrictMode strictMode = context.strictMode;
        try {
            // 10.2.1 - ClassDeclaration and ClassExpression is always strict code
            context.strictMode = StrictMode.Strict;
            long begin = ts.beginPosition();
            consume(Token.CLASS);
            BindingIdentifier name = null;
            if (token() != Token.EXTENDS && token() != Token.LC) {
                name = bindingIdentifierClassName(false);
            }
            Expression heritage = null;
            if (token() == Token.EXTENDS) {
                heritage = classHeritage();
            }
            consume(Token.LC);
            BlockContext scope = null;
            if (name != null) {
                scope = enterBlockContext(name);
            }
            List<MethodDefinition> staticMethods = newList();
            List<MethodDefinition> prototypeMethods = newList();
            classBody(name, staticMethods, prototypeMethods);
            if (name != null) {
                exitBlockContext();
            }
            consume(Token.RC);

            ClassExpression expr = new ClassExpression(begin, ts.endPosition(), scope, name,
                    heritage, staticMethods, prototypeMethods);
            if (name != null) {
                scope.node = expr;
            }
            return expr;
        } finally {
            context.strictMode = strictMode;
        }
    }

    /**
     * <strong>[14.5] Class Definitions</strong>
     * 
     * <pre>
     * ClassHeritage<span><sub>[Yield]</sub></span> :
     *     extends LeftHandSideExpression<span><sub>[?Yield]</sub></span>
     * </pre>
     * 
     * @return the parsed class heritage expression
     */
    private Expression classHeritage() {
        consume(Token.EXTENDS);
        return leftHandSideExpressionWithValidation();
    }

    /**
     * <strong>[14.5] Class Definitions</strong>
     * 
     * <pre>
     * ClassBody<span><sub>[Yield]</sub></span> :
     *     ClassElementList<span><sub>[?Yield]</sub></span>
     * ClassElementList<span><sub>[Yield]</sub></span> :
     *     ClassElement<span><sub>[?Yield]</sub></span>
     *     ClassElementList<span><sub>[?Yield]</sub></span> ClassElement<span><sub>[?Yield]</sub></span>
     * ClassElement<span><sub>[Yield]</sub></span> :
     *     MethodDefinition<span><sub>[?Yield]</sub></span>
     *     static MethodDefinition<span><sub>[?Yield]</sub></span>
     *     ;
     * </pre>
     * 
     * @param className
     *            the class' name if present, otherwise {@code null}
     * @param staticMethods
     *            the list to hold static methods
     * @param prototypeMethods
     *            the list to hold prototype methods
     */
    private void classBody(BindingIdentifier className, List<MethodDefinition> staticMethods,
            List<MethodDefinition> prototypeMethods) {
        while (token() != Token.RC) {
            if (token() == Token.SEMI) {
                consume(Token.SEMI);
            } else if (token() == Token.STATIC && !LOOKAHEAD(Token.LP)) {
                consume(Token.STATIC);
                staticMethods.add(methodDefinition());
            } else {
                prototypeMethods.add(methodDefinition());
            }
        }

        classBody_EarlyErrors(className, staticMethods, true);
        classBody_EarlyErrors(className, prototypeMethods, false);
    }

    /**
     * 14.5.1 Static Semantics: Early Errors
     * 
     * @param className
     *            the class' name if present, otherwise {@code null}
     * @param defs
     *            the method definitions to validate
     * @param isStatic
     *            the flag to select whether to perform static or prototype method error detection
     */
    private void classBody_EarlyErrors(BindingIdentifier className, List<MethodDefinition> defs,
            boolean isStatic) {
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
                // Give methods a better for name for stacktraces
                if (className != null) {
                    if ("constructor".equals(key)) {
                        def.setFunctionName(className.getName());
                    } else {
                        def.setFunctionName(className.getName() + "." + key);
                    }
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

    /**
     * <strong>[Extension] <code>async</code> Function Definitions</strong>
     * 
     * <pre>
     * AsyncFunctionDeclaration<span><sub>[Default]</sub></span> :
     *     async function BindingIdentifier<span><sub>[?Default]</sub></span> ( FormalParameters ) { FunctionBody }
     * </pre>
     * 
     * @param allowDefault
     *            the flag to select if 'default' is allowed as the function name
     * @return the parsed async function declaration
     */
    private AsyncFunctionDeclaration asyncFunctionDeclaration(boolean allowDefault) {
        newContext(ContextKind.AsyncFunction);
        try {
            long begin = ts.beginPosition();
            consume("async");
            int startFunction = ts.position() - "async".length();
            consume(Token.FUNCTION);
            BindingIdentifier identifier = bindingIdentifierFunctionName(true, allowDefault);
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
            AsyncFunctionDeclaration function = new AsyncFunctionDeclaration(begin,
                    ts.endPosition(), scope, identifier, parameters, statements,
                    context.hasSuperReference(), header, body);
            scope.node = function;

            asyncFunction_EarlyErrors(function);

            addAsyncFunctionDeclaration(function);

            return inheritStrictness(function);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[Extension] <code>async</code> Function Definitions</strong>
     * 
     * <pre>
     * AsyncFunctionExpression :
     *     async function BindingIdentifier<span><sub>opt</sub></span> ( FormalParameters ) { FunctionBody }
     * </pre>
     * 
     * @return the parsed async function expression
     */
    private AsyncFunctionExpression asyncFunctionExpression() {
        newContext(ContextKind.AsyncFunction);
        try {
            long begin = ts.beginPosition();
            consume("async");
            int startFunction = ts.position() - "async".length();
            consume(Token.FUNCTION);
            BindingIdentifier identifier = null;
            if (token() != Token.LP) {
                identifier = bindingIdentifierFunctionName(false, false);
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
            AsyncFunctionExpression function = new AsyncFunctionExpression(begin, ts.endPosition(),
                    scope, identifier, parameters, statements, context.hasSuperReference(), header,
                    body);
            scope.node = function;

            asyncFunction_EarlyErrors(function);

            return inheritStrictness(function);
        } finally {
            restoreContext();
        }
    }

    private void asyncFunction_EarlyErrors(AsyncFunctionDefinition function) {
        assert context.scopeContext == context.funContext;

        FunctionContext scope = context.funContext;
        FormalParameterList parameters = function.getParameters();
        List<String> boundNames = BoundNames(parameters);
        scope.parameterNames = new HashSet<>(boundNames);

        boolean strict = (context.strictMode != StrictMode.NonStrict);
        boolean simple = IsSimpleParameterList(parameters);
        if (!simple) {
            checkFormalParameterRedeclaration(function, boundNames, scope.varDeclaredNames);
        }
        checkFormalParameterRedeclaration(function, boundNames, scope.lexDeclaredNames);
        if (strict) {
            strictFormalParameters_EarlyErrors(function, boundNames, scope.parameterNames, simple);
        } else {
            formalParameters_EarlyErrors(function, boundNames, scope.parameterNames, simple);
        }
    }

    /**
     * <strong>[Extension] <code>async</code> Function Definitions</strong>
     * 
     * <pre>
     * AsyncMethod<span><sub>[Yield]</sub></span> :
     *     async PropertyName ( StrictFormalParameters ) { FunctionBody }
     * </pre>
     * 
     * @return the parsed async method
     */
    private MethodDefinition asyncMethod() {
        long begin = ts.beginPosition();

        consume("async");
        PropertyName propertyName = propertyName();

        newContext(ContextKind.AsyncFunction);
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

            String header = "async function " + ts.range(startFunction, startBody - 1);
            String body = ts.range(startBody, endFunction);

            FunctionContext scope = context.funContext;
            MethodType type = MethodType.AsyncFunction;
            MethodDefinition method = new MethodDefinition(begin, ts.endPosition(), scope, type,
                    propertyName, parameters, statements, context.hasSuperReference(), header, body);
            scope.node = method;

            methodDefinition_EarlyErrors(method);

            return inheritStrictness(method);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[Extension] <code>async</code> Function Definitions</strong>
     * 
     * <pre>
     * AwaitExpression :
     *     await UnaryExpression
     * </pre>
     * 
     * @return the parsed await expression
     */
    private AwaitExpression awaitExpression() {
        assert context.kind == ContextKind.AsyncFunction && context.awaitAllowed;
        long begin = ts.beginPosition();
        consume("await");
        Expression expr = unaryExpression();
        return new AwaitExpression(begin, ts.endPosition(), expr);
    }

    /* ***************************************************************************************** */

    /**
     * <strong>[13] ECMAScript Language: Statements and Declarations</strong>
     * 
     * <pre>
     * Statement<span><sub>[Yield, Return]</sub></span> :
     *     BlockStatement<span><sub>[?Yield, ?Return]</sub></span>
     *     VariableStatement<span><sub>[?Yield]</sub></span>
     *     EmptyStatement
     *     ExpressionStatement<span><sub>[?Yield]</sub></span>
     *     IfStatement<span><sub>[?Yield, ?Return]</sub></span>
     *     BreakableStatement<span><sub>[?Yield, ?Return]</sub></span>
     *     ContinueStatement<span><sub>[?Yield]</sub></span>
     *     BreakStatement<span><sub>[?Yield]</sub></span>
     *     <span><sub>[+Return]</sub></span>ReturnStatement<span><sub>[?Yield]</sub></span>
     *     WithStatement<span><sub>[?Yield, ?Return]</sub></span>
     *     LabelledStatement<span><sub>[?Yield, ?Return]</sub></span>
     *     ThrowStatement<span><sub>[?Yield]</sub></span>
     *     TryStatement<span><sub>[?Yield, ?Return]</sub></span>
     *     DebuggerStatement
     * 
     * BreakableStatement<span><sub>[Yield, Return]</sub></span> :
     *     IterationStatement<span><sub>[?Yield, ?Return]</sub></span>
     *     SwitchStatement<span><sub>[?Yield, ?Return]</sub></span>
     * </pre>
     * 
     * @return the parsed statement node
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
            return forStatementOrForInOfStatement(EMPTY_LABEL_SET);
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
            if (isEnabled(CompatibilityOption.LetStatement)
                    || isEnabled(CompatibilityOption.LetExpression)) {
                return letStatement();
            }
            // fall-through
        case YIELD:
        case IMPLEMENTS:
        case INTERFACE:
        case PACKAGE:
        case PRIVATE:
        case PROTECTED:
        case PUBLIC:
        case STATIC:
        case NAME:
        case ESCAPED_NAME:
        case ESCAPED_RESERVED_WORD:
        case ESCAPED_STRICT_RESERVED_WORD:
        case ESCAPED_YIELD:
        case ESCAPED_LET:
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
     * BlockStatement<span><sub>[Yield, Return]</sub></span> :
     *     Block<span><sub>[?Yield, ?Return]</sub></span>
     * Block<span><sub>[Yield, Return]</sub></span> :
     *     { StatementList<span><sub>[?Yield, ?Return]opt</sub></span> }
     * </pre>
     * 
     * @param inherited
     *            the list of inherited lexical bindings
     * @return the parsed block statement
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
     * StatementList<span><sub>[Yield, Return]</sub></span> :
     *     StatementItem<span><sub>[?Yield, ?Return]</sub></span>
     *     StatementList<span><sub>[?Yield, ?Return]</sub></span> StatementListItem<span><sub>[?Yield, ?Return]</sub></span>
     * </pre>
     * 
     * @param end
     *            the end marker token
     * @return the list of parsed statement list items
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
     * StatementListItem<span><sub>[Yield, Return]</sub></span> :
     *     Statement<span><sub>[?Yield, ?Return]</sub></span>
     *     Declaration<span><sub>[?Yield]</sub></span>
     * </pre>
     * 
     * @return the parsed statement list item
     */
    private StatementListItem statementListItem() {
        switch (token()) {
        case FUNCTION:
        case CLASS:
        case CONST:
            return declaration(false);
        case NAME:
            if (isEnabled(CompatibilityOption.AsyncFunction) && LOOKAHEAD(Token.FUNCTION)
                    && noNextLineTerminator() && isName("async")) {
                return declaration(false);
            }
            break;
        case LET:
            if (lexicalBindingFirstSet(peek())) {
                return declaration(false);
            }
            // 'let' as identifier, e.g. `let + 1`
            break;
        default:
        }
        return statement();
    }

    /**
     * <strong>[13] ECMAScript Language: Statements and Declarations</strong>
     * 
     * <pre>
     * Declaration<span><sub>[Yield, Default]</sub></span> :
     *     FunctionDeclaration<span><sub>[?Yield, ?Default]</sub></span>
     *     GeneratorDeclaration<span><sub>[?Yield, ?Default]</sub></span>
     *     ClassDeclaration<span><sub>[?Yield, ?Default]</sub></span>
     *     LexicalDeclaration<span><sub>[In, ?Yield]</sub></span>
     * </pre>
     * 
     * @param allowDefault
     *            the flag to select if 'default' is allowed as declaration name
     * @return the parsed declaration node
     */
    private Declaration declaration(boolean allowDefault) {
        switch (token()) {
        case FUNCTION:
            return functionOrGeneratorDeclaration(allowDefault);
        case CLASS:
            return classDeclaration(allowDefault);
        case LET:
        case CONST:
            return lexicalDeclaration(true);
        case NAME:
            if (isEnabled(CompatibilityOption.AsyncFunction)) {
                return asyncFunctionDeclaration(allowDefault);
            }
        default:
            throw reportSyntaxError(Messages.Key.InvalidToken, token().toString());
        }
    }

    private Declaration functionOrGeneratorDeclaration(boolean allowDefault) {
        if (LOOKAHEAD(Token.MUL)) {
            return generatorDeclaration(allowDefault, false);
        } else {
            long position = ts.position(), lineinfo = ts.lineinfo();
            try {
                return functionDeclaration(allowDefault);
            } catch (RetryGenerator e) {
                ts.reset(position, lineinfo);
                return generatorDeclaration(allowDefault, true);
            }
        }
    }

    /**
     * <strong>[13.2.1] Let and Const Declarations</strong>
     * 
     * <pre>
     * LexicalDeclaration<span><sub>[In, Yield]</sub></span> :
     *     LetOrConst BindingList<span><sub>[?In, ?Yield]</sub></span> ;
     * LetOrConst :
     *     let
     *     const
     * </pre>
     * 
     * @param allowIn
     *            the flag to select whether or not the in-operator is allowed
     * @return the parsed lexical declaration
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
            // semicolon() not called if "in" not allowed, cf. forStatement()
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
     * BindingList<span><sub>[In, Yield]</sub></span> :
     *     LexicalBinding<span><sub>[?In, ?Yield]</sub></span>
     *     BindingList<span><sub>[?In, ?Yield]</sub></span>, LexicalBinding<span><sub>[?In, ?Yield]</sub></span>
     * </pre>
     * 
     * @param isConst
     *            the flag for const lexical bindings
     * @param allowIn
     *            the flag to select whether or not the in-operator is allowed
     * @return the list of parsed lexical bindings
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
     * LexicalBinding<span><sub>[In, Yield]</sub></span> :
     *     BindingIdentifier<span><sub>[?Yield]</sub></span> Initializer<span><sub>[?In, ?Yield]opt</sub></span>
     *     BindingPattern<span><sub>[?Yield]</sub></span> Initializer<span><sub>[?In, ?Yield]</sub></span>
     * </pre>
     * 
     * @param isConst
     *            the flag for const lexical bindings
     * @param allowIn
     *            the flag to select whether or not the in-operator is allowed
     * @return the parsed lexical binding
     */
    private LexicalBinding lexicalBinding(boolean isConst, boolean allowIn) {
        long begin = ts.beginPosition();
        Binding binding;
        Expression initializer = null;
        if (token() == Token.LC || token() == Token.LB) {
            BindingPattern bindingPattern = bindingPattern(false);
            addLexDeclaredName(bindingPattern);
            if (token() == Token.ASSIGN || allowIn) {
                // make initializer optional if `allowIn == false`, cf. forStatement()
                initializer = initializer(allowIn);
            }
            binding = bindingPattern;
        } else {
            BindingIdentifier bindingIdentifier = bindingIdentifier(false);
            addLexDeclaredName(bindingIdentifier);
            if (token() == Token.ASSIGN) {
                initializer = initializer(allowIn);
            } else if (isConst && allowIn) {
                // `allowIn == false` indicates for-loop, cf. forStatement()
                reportSyntaxError(bindingIdentifier, Messages.Key.ConstMissingInitializer);
            }
            binding = bindingIdentifier;
        }

        return new LexicalBinding(begin, ts.endPosition(), binding, initializer);
    }

    /**
     * Returns {@code true} iff {@code token} is in the first-set of LexicalBinding.
     * 
     * @param token
     *            the token to inspect
     * @return {@code true} if the token is in the first-set
     */
    private boolean lexicalBindingFirstSet(Token token) {
        switch (token) {
        default:
            if (!isIdentifier(token)) {
                return false;
            }
        case LB:
        case LC:
            return true;
        }
    }

    /**
     * <strong>[12.2.5] Object Initializer</strong>
     * 
     * <pre>
     * Initializer<span><sub>[In, Yield]</sub></span> :
     *     = AssignmentExpression<span><sub>[?In, ?Yield]</sub></span>
     * </pre>
     * 
     * @param allowIn
     *            the flag to select whether or not the in-operator is allowed
     * @return the parsed initializer expression
     */
    private Expression initializer(boolean allowIn) {
        consume(Token.ASSIGN);
        return assignmentExpression(allowIn);
    }

    /**
     * <strong>[13.2.2] Variable Statement</strong>
     * 
     * <pre>
     * VariableStatement<span><sub>[Yield]</sub></span> :
     *     var VariableDeclarationList<span><sub>[In, ?Yield]</sub></span> ;
     * </pre>
     * 
     * @return the parsed variable statement
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
     * VariableDeclarationList<span><sub>[In, Yield]</sub></span> :
     *     VariableDeclaration<span><sub>[?In, ?Yield]</sub></span>
     *     VariableDeclarationList<span><sub>[?In, ?Yield]</sub></span> , VariableDeclaration<span><sub>[?In, ?Yield]</sub></span>
     * </pre>
     * 
     * @param allowIn
     *            the flag to select whether or not the in-operator is allowed
     * @return the parsed list of variable declarations
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
     * VariableDeclaration<span><sub>[In, Yield]</sub></span> :
     *     BindingIdentifier<span><sub>[?Yield]</sub></span> Initializer<span><sub>[?In, ?Yield]opt</sub></span>
     *     BindingPattern<span><sub>[Yield]</sub></span> Initializer<span><sub>[?In, ?Yield]</sub></span>
     * </pre>
     * 
     * @param allowIn
     *            the flag to select whether or not the in-operator is allowed
     * @return the parsed variable declaration
     */
    private VariableDeclaration variableDeclaration(boolean allowIn) {
        Binding binding;
        Expression initializer = null;
        if (token() == Token.LC || token() == Token.LB) {
            BindingPattern bindingPattern = bindingPattern();
            addVarDeclaredName(bindingPattern);
            if (allowIn) {
                initializer = initializer(allowIn);
            } else if (token() == Token.ASSIGN) {
                // make initializer optional if `allowIn == false`, cf. forStatement()
                initializer = initializer(allowIn);
            }
            binding = bindingPattern;
        } else {
            BindingIdentifier bindingIdentifier = bindingIdentifier();
            addVarDeclaredName(bindingIdentifier);
            if (token() == Token.ASSIGN) {
                initializer = initializer(allowIn);
            }
            binding = bindingIdentifier;
        }

        return new VariableDeclaration(binding, initializer);
    }

    /**
     * <strong>[13.2.3] Destructuring Binding Patterns</strong>
     * 
     * <pre>
     * BindingPattern<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     ObjectBindingPattern<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     *     ArrayBindingPattern<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     * </pre>
     * 
     * @return the parsed binding pattern
     */
    private BindingPattern bindingPattern() {
        return bindingPattern(true);
    }

    /**
     * <strong>[13.2.3] Destructuring Binding Patterns</strong>
     * 
     * <pre>
     * BindingPattern<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     ObjectBindingPattern<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     *     ArrayBindingPattern<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     * </pre>
     * 
     * @param allowLet
     *            the flag to select if 'let' is allowed as a binding name
     * @return the parsed binding pattern
     */
    private BindingPattern bindingPattern(boolean allowLet) {
        if (token() == Token.LC) {
            return objectBindingPattern(allowLet);
        } else {
            return arrayBindingPattern(allowLet);
        }
    }

    /**
     * <strong>[13.2.3] Destructuring Binding Patterns</strong>
     * 
     * <pre>
     * ObjectBindingPattern<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     { }
     *     { BindingPropertyList<span><sub>[?Yield, ?GeneratorParameter]</sub></span> }
     *     { BindingPropertyList<span><sub>[?Yield, ?GeneratorParameter]</sub></span> , }
     * BindingPropertyList<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     BindingProperty<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     *     BindingPropertyList<span><sub>[?Yield, ?GeneratorParameter]</sub></span> , BindingProperty<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     * </pre>
     * 
     * @param allowLet
     *            the flag to select if 'let' is allowed as a binding name
     * @return the parsed object binding pattern
     */
    private ObjectBindingPattern objectBindingPattern(boolean allowLet) {
        long begin = ts.beginPosition();
        List<BindingProperty> list = newSmallList();
        consume(Token.LC);
        while (token() != Token.RC) {
            list.add(bindingProperty(allowLet));
            if (token() == Token.COMMA) {
                consume(Token.COMMA);
            } else {
                break;
            }
        }
        consume(Token.RC);

        return new ObjectBindingPattern(begin, ts.endPosition(), list);
    }

    /**
     * <strong>[13.2.3] Destructuring Binding Patterns</strong>
     * 
     * <pre>
     * BindingProperty<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     SingleNameBinding<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     *     PropertyName<span><sub>[?Yield, ?GeneratorParameter]</sub></span> : BindingElement<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     * SingleNameBinding<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     <span><sub>[+GeneratorParameter]</sub></span>BindingIdentifier<span><sub>[Yield]</sub></span> Initializer<span><sub>[In]opt</sub></span>
     *     <span><sub>[~GeneratorParameter]</sub></span>BindingIdentifier<span><sub>[?Yield]</sub></span> Initializer<span><sub>[In, ?Yield]opt</sub></span>
     * </pre>
     * 
     * @param allowLet
     *            the flag to select if 'let' is allowed as a binding name
     * @return the parsed binding property
     */
    private BindingProperty bindingProperty(boolean allowLet) {
        if (token() == Token.LB || LOOKAHEAD(Token.COLON)) {
            PropertyName propertyName = propertyName();
            consume(Token.COLON);
            Binding binding;
            if (token() == Token.LC) {
                binding = objectBindingPattern(allowLet);
            } else if (token() == Token.LB) {
                binding = arrayBindingPattern(allowLet);
            } else {
                binding = bindingIdentifier(allowLet);
            }
            Expression initializer = null;
            if (token() == Token.ASSIGN) {
                initializer = initializer(true);
            }
            return new BindingProperty(propertyName, binding, initializer);
        } else {
            BindingIdentifier binding = bindingIdentifier(allowLet);
            Expression initializer = null;
            if (token() == Token.ASSIGN) {
                initializer = initializer(true);
            }
            return new BindingProperty(binding, initializer);
        }
    }

    /**
     * <strong>[13.2.3] Destructuring Binding Patterns</strong>
     * 
     * <pre>
     * ArrayBindingPattern<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     [ Elision<span><sub>opt</sub></span> BindingRestElement<span><sub>[?Yield, ?GeneratorParameter]opt</sub></span> ]
     *     [ BindingElementList<span><sub>[?Yield, ?GeneratorParameter]</sub></span> ]
     *     [ BindingElementList<span><sub>[?Yield, ?GeneratorParameter]</sub></span> , Elision<span><sub>opt</sub></span> BindingRestElement<span><sub>[?Yield, ?GeneratorParameter]opt</sub></span> ]
     * BindingElementList<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     BindingElisionElement<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     *     BindingElementList<span><sub>[?Yield, ?GeneratorParameter]</sub></span> , BindingElisionElement<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     * BindingElisionElement<span><sub>[Yield, GeneratorParameter]</sub></span>:
     *     Elision<span><sub>opt</sub></span> BindingElement<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     * </pre>
     * 
     * @param allowLet
     *            the flag to select if 'let' is allowed as a binding name
     * @return the parsed array binding pattern
     */
    private ArrayBindingPattern arrayBindingPattern(boolean allowLet) {
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
                list.add(bindingRestElement(allowLet));
                break;
            } else {
                list.add(bindingElement(allowLet));
                needComma = true;
            }
        }
        consume(Token.RB);

        return new ArrayBindingPattern(begin, ts.endPosition(), list);
    }

    /**
     * <pre>
     * Binding<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     BindingIdentifier<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     *     BindingPattern<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     * </pre>
     * 
     * @return the parsed binding node
     */
    private Binding binding() {
        return binding(true);
    }

    /**
     * <pre>
     * Binding<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     BindingIdentifier<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     *     BindingPattern<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     * </pre>
     * 
     * @param allowLet
     *            the flag to select if 'let' is allowed as a binding name
     * @return the parsed binding node
     */
    private Binding binding(boolean allowLet) {
        switch (token()) {
        case LC:
            return objectBindingPattern(allowLet);
        case LB:
            return arrayBindingPattern(allowLet);
        default:
            return bindingIdentifier(allowLet);
        }
    }

    /**
     * <strong>[13.2.3] Destructuring Binding Patterns</strong>
     * 
     * <pre>
     * BindingElement<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     SingleNameBinding<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     *     <span><sub>[+GeneratorParameter]</sub></span>BindingPattern<span><sub>[?Yield, GeneratorParameter]</sub></span> Initializer<span><sub>[In]opt</sub></span>
     *     <span><sub>[~GeneratorParameter]</sub></span>BindingPattern<span><sub>[?Yield]</sub></span> Initializer<span><sub>[In, ?Yield]opt</sub></span>
     * </pre>
     * 
     * @return the parsed binding element
     */
    private BindingElement bindingElement() {
        return bindingElement(true);
    }

    /**
     * <strong>[13.2.3] Destructuring Binding Patterns</strong>
     * 
     * <pre>
     * BindingElement<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     SingleNameBinding<span><sub>[?Yield, ?GeneratorParameter]</sub></span>
     *     <span><sub>[+GeneratorParameter]</sub></span>BindingPattern<span><sub>[?Yield, GeneratorParameter]</sub></span> Initializer<span><sub>[In]opt</sub></span>
     *     <span><sub>[~GeneratorParameter]</sub></span>BindingPattern<span><sub>[?Yield]</sub></span> Initializer<span><sub>[In, ?Yield]opt</sub></span>
     * </pre>
     * 
     * @param allowLet
     *            the flag to select if 'let' is allowed as a binding name
     * @return the parsed binding element
     */
    private BindingElement bindingElement(boolean allowLet) {
        long begin = ts.beginPosition();
        Binding binding = binding(allowLet);
        Expression initializer = null;
        if (token() == Token.ASSIGN) {
            initializer = initializer(true);
        }

        return new BindingElement(begin, ts.endPosition(), binding, initializer);
    }

    /**
     * <strong>[13.2.3] Destructuring Binding Patterns</strong>
     * 
     * <pre>
     * BindingRestElement<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     <span><sub>[+GeneratorParameter]</sub></span>... BindingIdentifier<span><sub>[Yield]</sub></span>
     *     <span><sub>[~GeneratorParameter]</sub></span>... BindingIdentifier<span><sub>[?Yield]</sub></span>
     * </pre>
     * 
     * @return the parsed binding rest element
     */
    private BindingRestElement bindingRestElement() {
        return bindingRestElement(true);
    }

    /**
     * <strong>[13.2.3] Destructuring Binding Patterns</strong>
     * 
     * <pre>
     * BindingRestElement<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *     <span><sub>[+GeneratorParameter]</sub></span>... BindingIdentifier<span><sub>[Yield]</sub></span>
     *     <span><sub>[~GeneratorParameter]</sub></span>... BindingIdentifier<span><sub>[?Yield]</sub></span>
     * </pre>
     * 
     * @param allowLet
     *            the flag to select if 'let' is allowed as a binding name
     * @return the parsed binding rest element
     */
    private BindingRestElement bindingRestElement(boolean allowLet) {
        long begin = ts.beginPosition();
        consume(Token.TRIPLE_DOT);
        BindingIdentifier identifier = bindingIdentifier(allowLet);

        return new BindingRestElement(begin, ts.endPosition(), identifier);
    }

    /**
     * <strong>[13.3] Empty Statement</strong>
     * 
     * <pre>
     * EmptyStatement:
     * ;
     * </pre>
     * 
     * @return the parsed empty statement
     */
    private EmptyStatement emptyStatement() {
        long begin = ts.beginPosition();
        consume(Token.SEMI);

        return new EmptyStatement(begin, ts.endPosition());
    }

    /**
     * <strong>[13.4] Expression Statement</strong>
     * 
     * <pre>
     * ExpressionStatement<span><sub>[Yield]</sub></span> :
     *     [LA &#x2209; { <b>{, function, class, let [</b> }] Expression<span><sub>[In, ?Yield]</sub></span> ;
     * </pre>
     * 
     * @return the parsed expression statement
     */
    private ExpressionStatement expressionStatement() {
        switch (token()) {
        case LC:
        case FUNCTION:
        case CLASS:
            break;
        case LET:
            if (LOOKAHEAD(Token.LB)) {
                break;
            }
        default:
            long begin = ts.beginPosition();
            Expression expr = expression(true);
            semicolon();

            return new ExpressionStatement(begin, ts.endPosition(), expr);
        }
        throw reportSyntaxError(Messages.Key.InvalidToken, token().toString());
    }

    /**
     * <strong>[13.5] The <code>if</code> Statement</strong>
     * 
     * <pre>
     * IfStatement<span><sub>[Yield, Return]</sub></span> :
     *     if ( Expression<span><sub>[In, ?Yield]</sub></span> ) Statement<span><sub>[?Yield, ?Return]</sub></span> else Statement<span><sub>[?Yield, ?Return]</sub></span>
     *     if ( Expression<span><sub>[In, ?Yield]</sub></span> ) Statement<span><sub>[?Yield, ?Return]</sub></span>
     * </pre>
     * 
     * @return the parsed if-statement
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
     * IterationStatement<span><sub>[Yield, Return]</sub></span> :
     *     do Statement<span><sub>[?Yield, ?Return]</sub></span> while ( Expression<span><sub>[In, ?Yield]</sub></span> ) ;<span><sub>opt</sub></span>
     * </pre>
     * 
     * @param labelSet
     *            the label set
     * @return the parsed do-while statement
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
     * IterationStatement<span><sub>[Yield, Return]</sub></span> :
     *     while ( Expression<span><sub>[In, ?Yield]</sub></span> ) StatementStatement<span><sub>[?Yield, ?Return]</sub></span>
     * </pre>
     * 
     * @param labelSet
     *            the label set
     * @return the parsed while statement
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
     * @param labelSet
     *            the label set
     * @return the parsed for-loop or for-in/of statement
     */
    private IterationStatement forStatementOrForInOfStatement(Set<String> labelSet) {
        long position = ts.position(), lineinfo = ts.lineinfo();
        ForStatement forStatement = forStatement(labelSet);
        if (forStatement != null) {
            return forStatement;
        }
        // Reset tokenstream and parse again to ensure correct block scopes are created
        ts.reset(position, lineinfo);
        return forInOfStatement(labelSet);
    }

    /**
     * <strong>[13.6.3] The <code>for</code> Statement</strong> <br>
     * 
     * <pre>
     * IterationStatement<span><sub>[Yield, Return]</sub></span> :
     *     for ( [LA &#x2209; { <b>let [</b> }] Expression<span><sub>[?Yield]opt</sub></span> ; Expression<span><sub>[In, ?Yield]opt</sub></span> ; Expression<span><sub>[In, ?Yield]opt</sub></span> ) Statement<span><sub>[?Yield, ?Return]</sub></span>
     *     for ( var VariableDeclarationList<span><sub>[?Yield]</sub></span> ; Expression<span><sub>[In, ?Yield]opt</sub></span> ; Expression<span><sub>[In, ?Yield]opt</sub></span> ) Statement<span><sub>[?Yield, ?Return]</sub></span>
     *     for ( LexicalDeclaration<span><sub>[?Yield]</sub></span> Expression<span><sub>[In, ?Yield]opt</sub></span> ; Expression<span><sub>[In, ?Yield]opt</sub></span> ) Statement<span><sub>[?Yield, ?Return]</sub></span>
     * </pre>
     * 
     * @param labelSet
     *            the label set
     * @return the parsed for-loop statement
     */
    private ForStatement forStatement(Set<String> labelSet) {
        long begin = ts.beginPosition();
        consume(Token.FOR);
        if (token() != Token.LP && isName("each")
                && isEnabled(CompatibilityOption.ForEachStatement)) {
            // Don't bother to make the legacy ForEachStatement case fast
            return null;
        }
        consume(Token.LP);

        // NB: This code needs to be able to parse ForStatement and ForIn/OfStatement
        BlockContext lexBlockContext = null;
        Node head;
        switch (token()) {
        case VAR:
            long beginVar = ts.beginPosition();
            consume(Token.VAR);
            List<VariableDeclaration> decls = variableDeclarationList(false);
            head = new VariableStatement(beginVar, ts.endPosition(), decls);
            break;
        case SEMI:
            head = null;
            break;
        case CONST:
            lexBlockContext = enterBlockContext();
            head = lexicalDeclaration(false);
            break;
        case LET:
            if (lexicalBindingFirstSet(peek())) {
                lexBlockContext = enterBlockContext();
                head = lexicalDeclaration(false);
                break;
            }
            // 'let' as identifier, e.g. `for (let ;;) {}`
            // fall-through
        default:
            head = expressionOrLeftHandSideExpression(false);
            break;
        }

        if (token() != Token.SEMI) {
            // This is not a for-statement, try for-in/of next
            if (lexBlockContext != null) {
                // Leave block context before rollback
                exitBlockContext();
            }
            return null;
        }

        if (head instanceof VariableStatement) {
            // Enforce initializer for BindingPattern
            VariableStatement varStmt = (VariableStatement) head;
            for (VariableDeclaration decl : varStmt.getElements()) {
                if (decl.getBinding() instanceof BindingPattern && decl.getInitializer() == null) {
                    reportSyntaxError(varStmt, Messages.Key.DestructuringMissingInitializer);
                }
            }
            // Add variable statement after for-statement type is known
            addVarScopedDeclaration(varStmt);
        } else if (head instanceof LexicalDeclaration) {
            // Enforce initializer for BindingPattern and const declarations
            LexicalDeclaration lexDecl = (LexicalDeclaration) head;
            boolean isConst = lexDecl.getType() == LexicalDeclaration.Type.Const;
            for (LexicalBinding decl : lexDecl.getElements()) {
                if (decl.getBinding() instanceof BindingPattern && decl.getInitializer() == null) {
                    reportSyntaxError(lexDecl, Messages.Key.DestructuringMissingInitializer);
                }
                if (isConst && decl.getInitializer() == null) {
                    reportSyntaxError(lexDecl, Messages.Key.ConstMissingInitializer);
                }
            }
        }

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
    }

    /**
     * Parses the Expression or LeftHandSideExpression production in ForStatement.
     * 
     * @param allowIn
     *            the flag to select whether or not the in-operator is allowed
     * @return the parsed expression or left-hand side expression
     * @see #expression(boolean)
     * @see #leftHandSideExpression(boolean)
     */
    private Expression expressionOrLeftHandSideExpression(boolean allowIn) {
        int count = context.countLiterals();
        Expression expr = assignmentExpressionNoValidation(allowIn);
        if (token() == Token.SEMI || token() == Token.COMMA) {
            // ForStatement, apply early error checks for object literals
            objectLiteral_EarlyErrors(count);
            // Proceed to parse expression tail, if any
            if (token() == Token.COMMA) {
                return commaExpression(expr, allowIn);
            }
            return expr;
        }
        // ForInStatement or ForOfStatement, discard unchecked object literals
        discardUncheckedObjectLiterals(count);
        return expr;
    }

    /**
     * <strong>[13.6.4] The <code>for-in</code> and <code>for-of</code> Statements</strong>
     * 
     * <pre>
     * IterationStatement<span><sub>[Yield, Return]</sub></span> :
     *     for ( [LA &#x2209; { <b>let [</b> }] LeftHandSideExpression<span><sub>[?Yield]</sub></span> in Expression<span><sub>[In, ?Yield]</sub></span> ) Statement<span><sub>[?Yield, ?Return]</sub></span>
     *     for ( var ForBinding<span><sub>[?Yield]</sub></span> in Expression<span><sub>[In, ?Yield]</sub></span> ) Statement<span><sub>[?Yield, ?Return]</sub></span>
     *     for ( ForDeclaration<span><sub>[?Yield]</sub></span> in Expression<span><sub>[In, ?Yield]</sub></span> ) Statement<span><sub>[?Yield, ?Return]</sub></span>
     *     for ( [LA &#x2209; { <b>let [</b> }] LeftHandSideExpression<span><sub>[?Yield]</sub></span> of AssignmentExpression<span><sub>[In, ?Yield]</sub></span> ) Statement<span><sub>[?Yield, ?Return]</sub></span>
     *     for ( var ForBinding<span><sub>[?Yield]</sub></span> of AssignmentExpression<span><sub>[In, ?Yield]</sub></span> ) Statement<span><sub>[?Yield, ?Return]</sub></span>
     *     for ( ForDeclaration<span><sub>[?Yield]</sub></span> of AssignmentExpression<span><sub>[In, ?Yield]</sub></span> ) Statement<span><sub>[?Yield, ?Return]</sub></span>
     * ForDeclaration<span><sub>[Yield]</sub></span> :
     *     LetOrConst ForBinding<span><sub>[?Yield]</sub></span>
     * ForBinding<span><sub>[Yield]</sub></span> :
     *     BindingIdentifier<span><sub>[?Yield]</sub></span>
     *     BindingPattern<span><sub>[?Yield]</sub></span>
     * </pre>
     * 
     * @param labelSet
     *            the label set
     * @return the parsed for-in/of statement
     */
    private IterationStatement forInOfStatement(Set<String> labelSet) {
        long begin = ts.beginPosition();
        consume(Token.FOR);
        boolean forEach = false, forOf = false;
        if (token() != Token.LP && isName("each")
                && isEnabled(CompatibilityOption.ForEachStatement)) {
            consume("each");
            forEach = true;
        }
        consume(Token.LP);

        boolean lexicalBinding = false;
        Node head;
        switch (token()) {
        case VAR:
            VariableStatement varStmt = forVarDeclaration();
            assert varStmt.getElements().size() == 1;
            assert varStmt.getElements().get(0).getInitializer() == null;
            addVarDeclaredName(varStmt.getElements().get(0).getBinding());
            addVarScopedDeclaration(varStmt);
            head = varStmt;
            break;
        case CONST:
            lexicalBinding = true;
            head = forDeclaration();
            break;
        case LET:
            if (lexicalBindingFirstSet(peek())) {
                lexicalBinding = true;
                head = forDeclaration();
                break;
            }
            // 'let' as identifier, e.g. `for (let in "") {}`
            // fall-through
        default:
            int count = context.countLiterals();
            Expression lhs = leftHandSideExpression(true);
            head = validateAssignment(lhs, ExceptionType.SyntaxError,
                    Messages.Key.InvalidAssignmentTarget);
            // Number of unchecked object literals should not have changed
            assert context.assertLiteralsUnchecked(count);
            break;
        }

        Expression expr;
        if (forEach || token() == Token.IN) {
            consume(Token.IN);
            expr = expression(true);
        } else {
            forOf = true;
            consume("of");
            expr = assignmentExpression(true);
        }
        consume(Token.RP);

        BlockContext lexBlockContext = null;
        if (lexicalBinding) {
            lexBlockContext = enterBlockContext();
            LexicalDeclaration lexDecl = (LexicalDeclaration) head;
            assert lexDecl.getElements().size() == 1;
            assert lexDecl.getElements().get(0).getInitializer() == null;
            addLexDeclaredName(lexDecl.getElements().get(0).getBinding());
            addLexScopedDeclaration(lexDecl);
        }

        LabelContext labelCx = enterIteration(begin, labelSet);
        Statement stmt = statement();
        exitIteration();

        if (lexicalBinding) {
            exitBlockContext();
        }

        if (forEach) {
            ForEachStatement iteration = new ForEachStatement(begin, ts.endPosition(),
                    lexBlockContext, labelCx.abrupts, labelCx.labelSet, head, expr, stmt);
            if (lexBlockContext != null) {
                lexBlockContext.node = iteration;
            }
            return iteration;
        } else if (!forOf) {
            ForInStatement iteration = new ForInStatement(begin, ts.endPosition(), lexBlockContext,
                    labelCx.abrupts, labelCx.labelSet, head, expr, stmt);
            if (lexBlockContext != null) {
                lexBlockContext.node = iteration;
            }
            return iteration;
        } else {
            ForOfStatement iteration = new ForOfStatement(begin, ts.endPosition(), lexBlockContext,
                    labelCx.abrupts, labelCx.labelSet, head, expr, stmt);
            if (lexBlockContext != null) {
                lexBlockContext.node = iteration;
            }
            return iteration;
        }
    }

    /**
     * <strong>[13.6.4] The <code>for-in</code> and <code>for-of</code> Statements</strong>
     * 
     * <pre>
     * ForDeclaration<span><sub>[Yield]</sub></span> :
     *     LetOrConst ForBinding<span><sub>[?Yield]</sub></span>
     * </pre>
     * 
     * @return the parsed for-declaration node
     */
    private LexicalDeclaration forDeclaration() {
        long begin = ts.beginPosition();
        LexicalDeclaration.Type type;
        if (token() == Token.LET) {
            consume(Token.LET);
            type = LexicalDeclaration.Type.Let;
        } else {
            consume(Token.CONST);
            type = LexicalDeclaration.Type.Const;
        }
        Binding binding = forBinding(false);
        LexicalBinding lexicalBinding = new LexicalBinding(begin, ts.endPosition(), binding, null);
        return new LexicalDeclaration(begin, ts.endPosition(), type, singletonList(lexicalBinding));
    }

    /**
     * <strong>[13.6.4] The <code>for-in</code> and <code>for-of</code> Statements</strong>
     * 
     * <pre>
     * var ForBinding<span><sub>[?Yield]</sub></span>
     * </pre>
     * 
     * @return the parsed for-loop variable declaration node
     */
    private VariableStatement forVarDeclaration() {
        long beginVar = ts.beginPosition();
        consume(Token.VAR);
        Binding binding = forBinding(true);
        VariableDeclaration variableDeclaration = new VariableDeclaration(binding, null);
        return new VariableStatement(beginVar, ts.endPosition(), singletonList(variableDeclaration));
    }

    /**
     * <strong>[13.7] The <code>continue</code> Statement</strong>
     * 
     * <pre>
     * ContinueStatement<span><sub>[Yield]</sub></span> :
     *     continue ;
     *     continue [no <i>LineTerminator</i> here] LabelIdentifier<span><sub>[?Yield]</sub></span> ;
     * </pre>
     * 
     * @return the parsed continue statement
     */
    private ContinueStatement continueStatement() {
        long begin = ts.beginPosition();
        String label;
        consume(Token.CONTINUE);
        if (noLineTerminator() && isIdentifier(token())) {
            label = labelIdentifier();
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
     * BreakStatement<span><sub>[Yield]</sub></span> :
     *     break ;
     *     break [no <i>LineTerminator</i> here] LabelIdentifier<span><sub>[?Yield]</sub></span> ;
     * </pre>
     * 
     * @return the parsed break statement
     */
    private BreakStatement breakStatement() {
        long begin = ts.beginPosition();
        String label;
        consume(Token.BREAK);
        if (noLineTerminator() && isIdentifier(token())) {
            label = labelIdentifier();
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
     * ReturnStatement<span><sub>[Yield]</sub></span> :
     *     return ;
     *     return [no <i>LineTerminator</i> here] Expression<span><sub>[In, ?Yield]</sub></span> ;
     * </pre>
     * 
     * @return the parsed return statement
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
     * WithStatement<span><sub>[Yield, Return]</sub></span> :
     *     with ( Expression<span><sub>[In, ?Yield]</sub></span> ) Statement<span><sub>[?Yield, ?Return]</sub></span>
     * </pre>
     * 
     * @return the parsed with statement
     */
    private WithStatement withStatement() {
        long begin = ts.beginPosition();
        reportStrictModeSyntaxError(begin, Messages.Key.StrictModeWithStatement);

        consume(Token.WITH);
        consume(Token.LP);
        Expression expr = expression(true);
        consume(Token.RP);

        WithContext scope = enterWithContext();
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
     * SwitchStatement<span><sub>[Yield, Return]</sub></span> :
     *     switch ( Expression<span><sub>[In, ?Yield]</sub></span> ) CaseBlock<span><sub>[?Yield, ?Return]</sub></span>
     * CaseBlock<span><sub>[Yield, Return]</sub></span> :
     *     { CaseClauses<span><sub>[?Yield, ?Return]opt</sub></span> }
     *     { CaseClauses<span><sub>[?Yield, ?Return]opt</sub></span> DefaultClause<span><sub>[?Yield, ?Return]</sub></span> CaseClauses<span><sub>[?Yield, ?Return]opt</sub></span> }
     * CaseClauses<span><sub>[Yield, Return]</sub></span> :
     *     CaseClause<span><sub>[?Yield, ?Return]</sub></span>
     *     CaseClauses<span><sub>[?Yield, ?Return]</sub></span> CaseClause<span><sub>[?Yield, ?Return]</sub></span>
     * CaseClause<span><sub>[Yield, Return]</sub></span> :
     *     case Expression<span><sub>[In, ?Yield]</sub></span> : StatementList<span><sub>[?Yield, ?Return]opt</sub></span>
     * DefaultClause :
     *     default : StatementList<span><sub>[?Yield, ?Return]opt</sub></span>
     * </pre>
     * 
     * @param labelSet
     *            the label set
     * @return the parsed switch statement
     */
    private SwitchStatement switchStatement(Set<String> labelSet) {
        List<SwitchClause> clauses = newList();
        long begin = ts.beginPosition();
        consume(Token.SWITCH);
        consume(Token.LP);
        Expression expr = expression(true);
        consume(Token.RP);

        consume(Token.LC);
        BlockContext scope = enterBlockContext();
        LabelContext labelCx = enterBreakable(begin, labelSet);
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
     * LabelledStatement<span><sub>[Yield, Return]</sub></span> :
     *     LabelIdentifier<span><sub>[?Yield]</sub></span> : Statement<span><sub>[?Yield, ?Return]</sub></span>
     * </pre>
     * 
     * @return the parsed labelled statement
     */
    private Statement labelledStatement() {
        long begin = ts.beginPosition();
        LinkedHashSet<String> labelSet = new LinkedHashSet<>(4);
        labels: for (;;) {
            switch (token()) {
            case FOR:
                return forStatementOrForInOfStatement(labelSet);
            case WHILE:
                return whileStatement(labelSet);
            case DO:
                return doWhileStatement(labelSet);
            case SWITCH:
                return switchStatement(labelSet);
            case LET:
                if (isEnabled(CompatibilityOption.LetStatement)
                        || isEnabled(CompatibilityOption.LetExpression)) {
                    break labels;
                }
                // fall-through
            case YIELD:
            case IMPLEMENTS:
            case INTERFACE:
            case PACKAGE:
            case PRIVATE:
            case PROTECTED:
            case PUBLIC:
            case STATIC:
            case NAME:
            case ESCAPED_NAME:
            case ESCAPED_RESERVED_WORD:
            case ESCAPED_STRICT_RESERVED_WORD:
            case ESCAPED_YIELD:
            case ESCAPED_LET:
                if (LOOKAHEAD(Token.COLON)) {
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
            long beginLabel = ts.beginPosition();
            String name = labelIdentifier();
            consume(Token.COLON);
            if (!labelSet.add(name)) {
                reportSyntaxError(beginLabel, Messages.Key.DuplicateLabel, name);
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
     * ThrowStatement<span><sub>[Yield]</sub></span> :
     *     throw [no <i>LineTerminator</i> here] Expression<span><sub>[In, ?Yield]</sub></span> ;
     * </pre>
     * 
     * @return the parsed throw statement
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
     * TryStatement<span><sub>[Yield, Return]</sub></span> :
     *     try Block<span><sub>[?Yield, ?Return]</sub></span> Catch<span><sub>[?Yield, ?Return]</sub></span>
     *     try Block<span><sub>[?Yield, ?Return]</sub></span> Finally<span><sub>[?Yield, ?Return]</sub></span>
     *     try Block<span><sub>[?Yield, ?Return]</sub></span> Catch<span><sub>[?Yield, ?Return]</sub></span> Finally<span><sub>[?Yield, ?Return]</sub></span>
     * Catch<span><sub>[Yield, Return]</sub></span> :
     *     catch ( CatchParameter<span><sub>[?Yield]</sub></span> ) Block<span><sub>[?Yield, ?Return]</sub></span>
     * Finally<span><sub>[Yield, Return]</sub></span> :
     *     finally Block<span><sub>[?Yield, ?Return]</sub></span>
     * CatchParameter<span><sub>[Yield]</sub></span> :
     *     BindingIdentifier<span><sub>[?Yield]</sub></span>
     *     BindingPattern<span><sub>[?Yield]</sub></span>
     * </pre>
     * 
     * @return the parsed try-statement node
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
            if (isEnabled(CompatibilityOption.GuardedCatch)) {
                guardedCatchNodes = newSmallList();
                while (token() == Token.CATCH && catchNode == null) {
                    long beginCatch = ts.beginPosition();
                    consume(Token.CATCH);
                    consume(Token.LP);
                    Binding catchParameter = binding();
                    BlockContext catchScope = enterBlockContext(catchParameter);

                    Expression guard;
                    if (token() == Token.IF) {
                        consume(Token.IF);
                        guard = expression(true);
                    } else {
                        guard = null;
                    }

                    consume(Token.RP);

                    // CatchBlock receives a list of non-available lexical declarable names to
                    // fulfill the early error restriction that the BoundNames of CatchParameter
                    // must not also occur in either the LexicallyDeclaredNames or the
                    // VarDeclaredNames of CatchBlock
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
                consume(Token.LP);
                Binding catchParameter = binding();
                BlockContext catchScope = enterBlockContext(catchParameter);
                consume(Token.RP);

                // CatchBlock receives a list of non-available lexical declarable names to
                // fulfill the early error restriction that the BoundNames of CatchParameter
                // must not also occur in either the LexicallyDeclaredNames or the
                // VarDeclaredNames of CatchBlock
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
     * 
     * @return the parsed debugger statement
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
     * LetStatement<span><sub>[Yield, Return]</sub></span> :
     *     let ( BindingList<span><sub>[In, ?Yield]</sub></span> ) BlockStatement<span><sub>[?Yield, ?Return]</sub></span>
     * </pre>
     * 
     * @return the parsed let-statement
     */
    private Statement letStatement() {
        long begin = ts.beginPosition();
        consume(Token.LET);

        consume(Token.LP);
        List<LexicalBinding> lexicalBindings = letBindingList();
        List<Binding> bindings = toBindings(lexicalBindings);
        consume(Token.RP);

        if (token() != Token.LC && isEnabled(CompatibilityOption.LetExpression)) {
            // let expression disguised as let statement - also error in strict mode(!)
            reportStrictModeSyntaxError(begin, Messages.Key.UnexpectedToken, token().toString());

            BlockContext scope = enterBlockContext(bindings);
            Expression expression = assignmentExpression(true);
            exitBlockContext();

            LetExpression letExpression = new LetExpression(begin, ts.endPosition(), scope,
                    lexicalBindings, expression);
            scope.node = letExpression;
            return new ExpressionStatement(begin, ts.endPosition(), letExpression);
        } else {
            BlockContext scope = enterBlockContext(bindings);
            BlockStatement letBlock = block(bindings);
            exitBlockContext();

            LetStatement block = new LetStatement(begin, ts.endPosition(), scope, lexicalBindings,
                    letBlock);
            scope.node = block;
            return block;
        }
    }

    private List<LexicalBinding> letBindingList() {
        List<LexicalBinding> list = newSmallList();
        list.add(letBinding());
        while (token() == Token.COMMA) {
            consume(Token.COMMA);
            list.add(letBinding());
        }
        return list;
    }

    private LexicalBinding letBinding() {
        long begin = ts.beginPosition();
        Binding binding;
        Expression initializer = null;
        if (token() == Token.LC || token() == Token.LB) {
            binding = bindingPattern(false);
            initializer = initializer(true);
        } else {
            binding = bindingIdentifier(false);
            if (token() == Token.ASSIGN) {
                initializer = initializer(true);
            }
        }

        return new LexicalBinding(begin, ts.endPosition(), binding, initializer);
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
     * <strong>[12.1] Identifiers</strong>
     * 
     * <pre>
     * IdentifierReference<span><sub>[Yield]</sub></span> :
     *     Identifier
     *     [~Yield] yield
     * </pre>
     * 
     * @return the parsed identifier reference
     */
    private Identifier identifierReference() {
        long begin = ts.beginPosition();
        String identifier = identifier();
        return new Identifier(begin, ts.endPosition(), identifier);
    }

    /**
     * <strong>[12.1] Identifiers</strong>
     * 
     * <pre>
     * BindingIdentifier<span><sub>[Default, Yield]</sub></span> :
     *     <span><sub>[+Default]</sub></span> default
     *     <span><sub>[~Yield]</sub></span> yield
     *     Identifier
     * </pre>
     * 
     * @return the parsed binding identifier
     */
    private BindingIdentifier bindingIdentifier() {
        return bindingIdentifier(true);
    }

    /**
     * <strong>[12.1] Identifiers</strong>
     * 
     * <pre>
     * BindingIdentifier<span><sub>[Default, Yield]</sub></span> :
     *     <span><sub>[+Default]</sub></span> default
     *     <span><sub>[~Yield]</sub></span> yield
     *     Identifier
     * </pre>
     * 
     * @param allowLet
     *            the flag to select if 'let' is allowed as a binding name
     * @return the parsed binding identifier
     */
    private BindingIdentifier bindingIdentifier(boolean allowLet) {
        long begin = ts.beginPosition();
        if (!allowLet) {
            Token tok = token();
            if (tok == Token.LET || tok == Token.ESCAPED_LET) {
                reportTokenNotIdentifier(Token.LET);
            }
        }
        String identifier = identifier();
        if (context.strictMode != StrictMode.NonStrict) {
            if ("arguments".equals(identifier) || "eval".equals(identifier)) {
                reportStrictModeSyntaxError(begin, Messages.Key.StrictModeRestrictedIdentifier);
            }
        }
        return new BindingIdentifier(begin, ts.endPosition(), identifier);
    }

    /**
     * <strong>[12.1] Identifiers</strong>
     * <p>
     * Difference when compared to {@link #bindingIdentifier()}:<br>
     * Neither "arguments" nor "eval" nor "let" is allowed.
     * 
     * <pre>
     * BindingIdentifier<span><sub>[Default, Yield]</sub></span> :
     *     <span><sub>[+Default]</sub></span> default
     *     <span><sub>[~Yield]</sub></span> yield
     *     Identifier
     * </pre>
     * 
     * @param allowDefault
     *            the flag to select if 'default' is allowed as the class name
     * @return the parsed binding identifier
     */
    private BindingIdentifier bindingIdentifierClassName(boolean allowDefault) {
        long begin = ts.beginPosition();
        Token tok = token();
        if (allowDefault && tok == Token.DEFAULT) {
            consume(Token.DEFAULT);
            return new BindingIdentifier(begin, ts.endPosition(), getName(Token.DEFAULT));
        }
        if (tok == Token.LET || tok == Token.ESCAPED_LET) {
            reportTokenNotIdentifier(Token.LET);
        }
        // [10.2.1 Strict Mode Code]
        String identifier = identifier();
        if ("arguments".equals(identifier) || "eval".equals(identifier)) {
            reportSyntaxError(begin, Messages.Key.StrictModeRestrictedIdentifier);
        }
        return new BindingIdentifier(begin, ts.endPosition(), identifier);
    }

    /**
     * <strong>[12.1] Identifiers</strong>
     * <p>
     * Special case for {@link Token#YIELD} as {@link BindingIdentifier} in functions and generators
     * 
     * <pre>
     * BindingIdentifier<span><sub>[Default, Yield]</sub></span> :
     *     <span><sub>[+Default]</sub></span> default
     *     <span><sub>[~Yield]</sub></span> yield
     *     Identifier
     * </pre>
     * 
     * @param isDeclaration
     *            {@code true} if the function is declaration node
     * @param allowDefault
     *            the flag to select if 'default' is allowed as the function name
     * @return the parsed binding identifier
     */
    private BindingIdentifier bindingIdentifierFunctionName(boolean isDeclaration,
            boolean allowDefault) {
        Token tok = token();
        if (tok == Token.YIELD || tok == Token.ESCAPED_YIELD) {
            // function declarations inherit the yield mode from the parent context
            long begin = ts.beginPosition();
            if (isYieldName(isDeclaration ? context.parent : context)) {
                consume(tok);
                return new BindingIdentifier(begin, ts.endPosition(), getName(Token.YIELD));
            }
            reportStrictModeSyntaxError(begin, Messages.Key.StrictModeInvalidIdentifier,
                    getName(Token.YIELD));
            reportTokenNotIdentifier(Token.YIELD);
        }
        if (allowDefault && tok == Token.DEFAULT) {
            long begin = ts.beginPosition();
            consume(tok);
            return new BindingIdentifier(begin, ts.endPosition(), getName(Token.DEFAULT));
        }
        return bindingIdentifier(true);
    }

    /**
     * <strong>[12.1] Identifiers</strong>
     * 
     * <pre>
     * LabelIdentifier<span><sub>[Yield]</sub></span> :
     *     Identifier
     *     <span><sub>[~Yield]</sub></span> yield
     * </pre>
     * 
     * @return the parsed label identifier
     */
    private String labelIdentifier() {
        return identifier();
    }

    /**
     * <strong>[12.1] Identifiers</strong>
     * 
     * <pre>
     * Identifier :
     *     IdentifierName <strong>but not</strong> ReservedWord
     * </pre>
     * 
     * @return the parsed identifier
     */
    private String identifier() {
        Token tok = token();
        if (!isIdentifier(tok)) {
            reportTokenNotIdentifier(tok);
        }
        String name = getName(tok);
        consume(tok);
        return name;
    }

    /**
     * <strong>[12.1] Identifiers</strong>
     * 
     * @param tok
     *            the token to inspect
     * @return {@code true} if the token is valid identifier
     */
    private boolean isIdentifier(Token tok) {
        switch (tok) {
        case NAME:
        case ESCAPED_NAME:
            return true;
        case ESCAPED_RESERVED_WORD:
            throw reportSyntaxError(Messages.Key.InvalidIdentifier, getName(tok));
        case YIELD:
        case ESCAPED_YIELD:
            return isYieldName(context);
        case LET:
        case ESCAPED_LET:
        case IMPLEMENTS:
        case INTERFACE:
        case PACKAGE:
        case PRIVATE:
        case PROTECTED:
        case PUBLIC:
        case STATIC:
        case ESCAPED_STRICT_RESERVED_WORD:
            if (context.strictMode != StrictMode.NonStrict) {
                reportStrictModeSyntaxError(Messages.Key.StrictModeInvalidIdentifier, getName(tok));
            }
            return context.strictMode != StrictMode.Strict;
        default:
            return false;
        }
    }

    /**
     * Returns <code>true</code> if {@link Token#YIELD} should be treated as {@link Token#NAME} in
     * the supplied context.
     * 
     * @param context
     *            the context to use
     * @return {@code true} if 'yield' is a valid name in the parse context
     */
    private boolean isYieldName(ParseContext context) {
        // TODO: make `function*g(){yield yiel\u0064}` a SyntaxError for invalid identifier

        // 'yield' is always a keyword in strict-mode and in generators
        if (context.strictMode == StrictMode.Strict || context.kind == ContextKind.Generator) {
            return false;
        }
        // 'yield' nested in generator comprehension, nested in generator
        if (context.kind == ContextKind.GeneratorComprehension && context.yieldAllowed) {
            return false;
        }
        // proactively flag as syntax error if current strict mode is unknown
        reportStrictModeSyntaxError(Messages.Key.StrictModeInvalidIdentifier, getName(Token.YIELD));
        return true;
    }

    /**
     * <strong>[12.2] Primary Expression</strong>
     * 
     * <pre>
     * PrimaryExpresion<span><sub>[Yield]</sub></span> :
     *     this
     *     IdentifierReference<span><sub>[?Yield]</sub></span>
     *     Literal
     *     ArrayInitializer<span><sub>[?Yield]</sub></span>
     *     ObjectLiteral<span><sub>[?Yield]</sub></span>
     *     FunctionExpression
     *     ClassExpression
     *     GeneratorExpression
     *     GeneratorComprehension<span><sub>[?Yield]</sub></span>
     *     RegularExpressionLiteral
     *     TemplateLiteral<span><sub>[?Yield]</sub></span>
     *     CoverParenthesizedExpressionAndArrowParameterList<span><sub>[?Yield]</sub></span>
     * Literal :
     *     NullLiteral
     *     ValueLiteral
     * ValueLiteral :
     *     BooleanLiteral
     *     NumericLiteral
     *     StringLiteral
     * </pre>
     * 
     * @return the parsed primary expression node
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
            return arrayInitializer();
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
                return coverParenthesizedExpressionAndArrowParameterList();
            }
        case TEMPLATE:
            return templateLiteral(false);
        case LET:
            if (isEnabled(CompatibilityOption.LetExpression)) {
                return letExpression();
            }
            break;
        case YIELD:
            if (context.noDivAfterYield && (LOOKAHEAD(Token.DIV) || LOOKAHEAD(Token.ASSIGN_DIV))) {
                consume(Token.YIELD);
                reportTokenMismatch(Token.DIV, Token.REGEXP);
            }
            break;
        case NAME:
            if (isEnabled(CompatibilityOption.AsyncFunction) && LOOKAHEAD(Token.FUNCTION)
                    && noNextLineTerminator() && isName("async")) {
                return asyncFunctionExpression();
            }
            break;
        default:
        }
        return identifierReference();
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
     * <strong>[12.2] Primary Expression</strong>
     * 
     * <pre>
     * CoverParenthesizedExpressionAndArrowParameterList<span><sub>[Yield]</sub></span> :
     *     ( Expression<span><sub>[In, ?Yield]</sub></span> )
     *     ( )
     *     ( ... BindingIdentifier<span><sub>[?Yield]</sub></span> )
     *     ( Expression<span><sub>[In, ?Yield]</sub></span> , ... BindingIdentifier<span><sub>[?Yield]</sub></span>)
     * </pre>
     * 
     * @return the parsed expression node
     */
    private Expression coverParenthesizedExpressionAndArrowParameterList() {
        long position = ts.position(), lineinfo = ts.lineinfo();
        consume(Token.LP);
        Expression expr;
        if (token() == Token.RP) {
            expr = arrowFunctionEmptyParameters();
        } else if (token() == Token.TRIPLE_DOT) {
            expr = arrowFunctionRestParameter();
        } else {
            // Inlined `expression(true)`, all calls to assignmentExpression() are replaced with
            // assignmentExpressionNoValidation() to support cover-init-name and duplicate property
            // names in case this production is an ArrowParameterList.
            expr = assignmentExpressionNoValidation(true);
            if (token() == Token.FOR && isEnabled(CompatibilityOption.LegacyComprehension)) {
                // NB: It is not necessary to remove unchecked object literals from
                // assignmentExpressionNoValidation(), because any early errors will reappear
                // in legacyGeneratorComprehension().
                ts.reset(position, lineinfo);
                return legacyGeneratorComprehension();
            }
            if (token() == Token.COMMA) {
                List<Expression> list = newList();
                list.add(expr);
                while (token() == Token.COMMA) {
                    consume(Token.COMMA);
                    if (token() == Token.TRIPLE_DOT) {
                        list.add(arrowFunctionRestParameter());
                        break;
                    }
                    expr = assignmentExpressionNoValidation(true);
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
            reportSyntaxError(Messages.Key.EmptyParenthesizedExpression);
        }
        return new EmptyExpression(0, 0);
    }

    private SpreadElement arrowFunctionRestParameter() {
        long begin = ts.beginPosition();
        consume(Token.TRIPLE_DOT);
        String ident = bindingIdentifier().getName();
        Identifier identifier = new Identifier(ts.beginPosition(), ts.endPosition(), ident);
        SpreadElement spread = new SpreadElement(begin, ts.endPosition(), identifier);
        if (!(token() == Token.RP && LOOKAHEAD(Token.ARROW))) {
            reportSyntaxError(spread, Messages.Key.InvalidSpreadExpression);
        }
        return spread;
    }

    /**
     * <strong>[12.2.4] Array Initializer</strong>
     * 
     * <pre>
     * ArrayInitializer<span><sub>[Yield]</sub></span> :
     *     ArrayLiteral<span><sub>[?Yield]</sub></span>
     *     ArrayComprehension<span><sub>[?Yield]</sub></span>
     * </pre>
     * 
     * @return the parsed array initializer
     */
    private ArrayInitializer arrayInitializer() {
        if (LOOKAHEAD(Token.FOR)) {
            return arrayComprehension();
        } else {
            long begin = ts.beginPosition();
            if (isEnabled(CompatibilityOption.LegacyComprehension)) {
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
                        // NB: It is not necessary to remove unchecked object literals from
                        // assignmentExpressionNoValidation(), because any early errors will
                        // reappear in legacyArrayComprehension()
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
     * <strong>[12.2.4] Array Initializer</strong>
     * 
     * <pre>
     * ArrayLiteral<span><sub>[Yield]</sub></span> :
     *     [ Elision<span><sub>opt</sub></span> ]
     *     [ ElementList<span><sub>[?Yield]</sub></span> ]
     *     [ ElementList<span><sub>[?Yield]</sub></span> , Elision<span><sub>opt</sub></span> ]
     * ElementList<span><sub>[Yield]</sub></span> :
     *     Elision<span><sub>opt</sub></span> AssignmentExpression<span><sub>[In, ?Yield]</sub></span>
     *     Elision<span><sub>opt</sub></span> SpreadElement<span><sub>[?Yield]</sub></span>
     *     ElementList<span><sub>[?Yield]</sub></span> , Elision<span><sub>opt</sub></span> AssignmentExpression<span><sub>[In, ?Yield]</sub></span>
     *     ElementList<span><sub>[?Yield]</sub></span> , Elision<span><sub>opt</sub></span> SpreadElement<span><sub>[?Yield]</sub></span>
     * Elision :
     *     ,
     *     Elision ,
     * SpreadElement<span><sub>[Yield]</sub></span> :
     *     ... AssignmentExpression<span><sub>[In, ?Yield]</sub></span>
     * </pre>
     * 
     * @param begin
     *            the begin source position
     * @param expr
     *            the first array element or {@code null} if not yet parsed
     * @return the parsed array literal
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

        return new ArrayLiteral(begin, ts.endPosition(), list, !needComma && !list.isEmpty());
    }

    /**
     * <strong>[12.2.4.2] Array Comprehension</strong>
     * 
     * <pre>
     * ArrayComprehension<span><sub>[Yield]</sub></span> :
     *     [ Comprehension<span><sub>[?Yield]</sub></span> ]
     * </pre>
     * 
     * @return the parsed array comprehension
     */
    private ArrayComprehension arrayComprehension() {
        long begin = ts.beginPosition();
        consume(Token.LB);
        Comprehension comprehension = comprehension();
        consume(Token.RB);

        return new ArrayComprehension(begin, ts.endPosition(), comprehension);
    }

    /**
     * <strong>[12.2.4.2] Array Comprehension</strong>
     * 
     * <pre>
     * Comprehension<span><sub>[Yield]</sub></span> :
     *     ComprehensionFor<span><sub>[?Yield]</sub></span> ComprehensionTail<span><sub>[?Yield]</sub></span>
     * ComprehensionTail<span><sub>[Yield]</sub></span> :
     *     AssignmentExpression<span><sub>[In, ?Yield]</sub></span>
     *     ComprehensionFor<span><sub>[?Yield]</sub></span> ComprehensionTail<span><sub>[?Yield]</sub></span>
     *     ComprehensionIf<span><sub>[?Yield]</sub></span> ComprehensionTail<span><sub>[?Yield]</sub></span>
     * </pre>
     * 
     * @return the parsed comprehension node
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
     * <strong>[12.2.4.2] Array Comprehension</strong>
     * 
     * <pre>
     * ComprehensionFor<span><sub>[Yield]</sub></span> :
     *     for ( ForBinding<span><sub>[?Yield]</sub></span> of AssignmentExpression<span><sub>[In, ?Yield]</sub></span> )
     * ForBinding<span><sub>[Yield]</sub></span> :
     *     BindingIdentifier<span><sub>[?Yield]</sub></span>
     *     BindingPattern<span><sub>[?Yield]</sub></span>
     * </pre>
     * 
     * @return the parsed comprehension-for node
     */
    private ComprehensionFor comprehensionFor() {
        long begin = ts.beginPosition();
        consume(Token.FOR);
        consume(Token.LP);
        Binding b = forBinding(false);
        consume("of");
        Expression expression = assignmentExpression(true);
        consume(Token.RP);
        BlockContext scope = enterBlockContext(b);
        ComprehensionFor node = new ComprehensionFor(begin, ts.endPosition(), scope, b, expression);
        scope.node = node;
        return node;
    }

    /**
     * <strong>[12.2.4.2] Array Comprehension</strong>
     * 
     * <pre>
     * ForBinding<span><sub>[Yield]</sub></span> :
     *     BindingIdentifier<span><sub>[?Yield]</sub></span>
     *     BindingPattern<span><sub>[?Yield]</sub></span>
     * </pre>
     * 
     * @param allowLet
     *            the flag to select if 'let' is allowed as a binding name
     * @return the parsed for-binding
     */
    private Binding forBinding(boolean allowLet) {
        return binding(allowLet);
    }

    /**
     * <strong>[12.2.4.2] Array Comprehension</strong>
     * 
     * <pre>
     * ComprehensionIf<span><sub>[Yield]</sub></span> :
     *     if ( AssignmentExpression<span><sub>[In, ?Yield]</sub></span> )
     * </pre>
     * 
     * @return the parsed comprehension-if node
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
     * <strong>[12.2.4.2] Array Comprehension</strong>
     * 
     * <pre>
     * LegacyArrayComprehension<span><sub>[Yield]</sub></span> :
     *     [ LegacyComprehension<span><sub>[?Yield]</sub></span> ]
     * </pre>
     * 
     * @return the parsed legacy array comprehension
     */
    private ArrayComprehension legacyArrayComprehension() {
        long begin = ts.beginPosition();
        consume(Token.LB);
        LegacyComprehension comprehension = legacyComprehension();
        consume(Token.RB);
        return new ArrayComprehension(begin, ts.endPosition(), comprehension);
    }

    /**
     * <strong>[12.2.4.2] Array Comprehension</strong>
     * 
     * <pre>
     * LegacyComprehension<span><sub>[Yield]</sub></span> :
     *     AssignmentExpression<span><sub>[In, ?Yield]</sub></span> LegacyComprehensionForList<span><sub>[?Yield]</sub></span> LegacyComprehensionIf<span><sub>[?Yield]opt</sub></span>
     * LegacyComprehensionForList<span><sub>[Yield]</sub></span> :
     *     LegacyComprehensionFor<span><sub>[?Yield]</sub></span> LegacyComprehensionForList<span><sub>[?Yield]opt</sub></span>
     * LegacyComprehensionFor<span><sub>[Yield]</sub></span> :
     *     for ( ForBinding<span><sub>[?Yield]</sub></span> of Expression<span><sub>[In, ?Yield]</sub></span> )
     *     for ( ForBinding<span><sub>[?Yield]</sub></span> in Expression<span><sub>[In, ?Yield]</sub></span> )
     *     for each ( ForBinding<span><sub>[?Yield]</sub></span> in Expression<span><sub>[In, ?Yield]</sub></span> )
     * LegacyComprehensionIf<span><sub>[Yield]</sub></span> :
     *     if ( Expression<span><sub>[In, ?Yield]</sub></span> )
     * </pre>
     * 
     * @return the parsed legacy comprehension
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
            Binding b = forBinding(false);
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

        LegacyComprehension comprehension = new LegacyComprehension(scope, list, expr);
        scope.node = comprehension;
        return comprehension;
    }

    /**
     * <strong>[12.2.5] Object Initializer</strong>
     * 
     * <pre>
     * ObjectLiteral<span><sub>[Yield]</sub></span> :
     *     { }
     *     { PropertyDefinitionList<span><sub>[?Yield]</sub></span> }
     *     { PropertyDefinitionList<span><sub>[?Yield]</sub></span> , }
     * PropertyDefinitionList<span><sub>[Yield]</sub></span> :
     *     PropertyDefinition<span><sub>[?Yield]</sub></span>
     *     PropertyDefinitionList<span><sub>[?Yield]</sub></span> , PropertyDefinition<span><sub>[?Yield]</sub></span>
     * </pre>
     * 
     * @return the parsed object literal
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

    private void discardUncheckedObjectLiterals(int oldCount) {
        ArrayDeque<ObjectLiteral> literals = context.objectLiterals;
        if (literals != null) {
            for (int i = oldCount, newCount = literals.size(); i < newCount; ++i) {
                literals.pop();
            }
        }
    }

    /**
     * 12.2.5.1 Static Semantics: Early Errors
     * 
     * @param oldCount
     *            the previous count of object literals
     */
    private void objectLiteral_EarlyErrors(int oldCount) {
        ArrayDeque<ObjectLiteral> literals = context.objectLiterals;
        if (literals != null) {
            for (int i = oldCount, newCount = literals.size(); i < newCount; ++i) {
                objectLiteral_EarlyErrors(literals.pop());
            }
        }
    }

    /**
     * 12.2.5.1 Static Semantics: Early Errors
     * 
     * @param object
     *            the object literal to check for early errors
     */
    private void objectLiteral_EarlyErrors(ObjectLiteral object) {
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
                MethodDefinition.MethodType type = ((MethodDefinition) def).getType();
                kind = type == MethodType.Getter ? GETTER : type == MethodType.Setter ? SETTER
                        : SPECIAL;
            } else {
                assert def instanceof CoverInitializedName;
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
     * <strong>[12.2.5] Object Initializer</strong>
     * 
     * <pre>
     * PropertyDefinition<span><sub>[Yield]</sub></span> :
     *     IdentifierReference<span><sub>[?Yield]</sub></span>
     *     CoverInitializedName<span><sub>[?Yield]</sub></span>
     *     PropertyName<span><sub>[?Yield]</sub></span> : AssignmentExpression<span><sub>[In, ?Yield]</sub></span>
     *     MethodDefinition<span><sub>[?Yield]</sub></span>
     * CoverInitializedName<span><sub>[Yield]</sub></span> :
     *     IdentifierReference Initializer<span><sub>[In, ?Yield]</sub></span>
     * Initializer<span><sub>[In, Yield]</sub></span> :
     *     = AssignmentExpression<span><sub>[?In, ?Yield]</sub></span>
     * </pre>
     * 
     * @return the parsed property definition
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
            return normalMethod(begin, propertyName);
        }
        if (LOOKAHEAD(Token.COLON)) {
            PropertyName propertyName = literalPropertyName();
            consume(Token.COLON);
            Expression propertyValue = assignmentExpressionNoValidation(true);
            return new PropertyValueDefinition(begin, ts.endPosition(), propertyName, propertyValue);
        }
        if (LOOKAHEAD(Token.COMMA) || LOOKAHEAD(Token.RC)) {
            Identifier identifier = identifierReference();
            return new PropertyNameDefinition(begin, ts.endPosition(), identifier);
        }
        if (LOOKAHEAD(Token.ASSIGN)) {
            Identifier identifier = identifierReference();
            consume(Token.ASSIGN);
            Expression initializer = assignmentExpression(true);
            return new CoverInitializedName(begin, ts.endPosition(), identifier, initializer);
        }
        return methodDefinition();
    }

    /**
     * <strong>[12.2.5] Object Initializer</strong>
     * 
     * <pre>
     * PropertyName<span><sub>[Yield, GeneratorParameter]</sub></span> :
     *   LiteralPropertyName
     *   <span><sub>[+GeneratorParameter]</sub></span>ComputedPropertyName
     *   <span><sub>[~GeneratorParameter]</sub></span>ComputedPropertyName<span><sub>[?Yield]</sub></span>
     * </pre>
     * 
     * @return the parsed property name
     */
    private PropertyName propertyName() {
        if (token() != Token.LB) {
            return literalPropertyName();
        } else {
            return computedPropertyName();
        }
    }

    /**
     * <strong>[12.2.5] Object Initializer</strong>
     * 
     * <pre>
     * LiteralPropertyName :
     *     IdentifierName
     *     StringLiteral
     *     NumericLiteral
     * </pre>
     * 
     * @return the parsed literal property name
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
     * <strong>[12.2.5] Object Initializer</strong>
     * 
     * <pre>
     * ComputedPropertyName<span><sub>[Yield]</sub></span> :
     *     [ AssignmentExpression<span><sub>[In, ?Yield]</sub></span> ]
     * </pre>
     * 
     * @return the parsed computed property name
     */
    private PropertyName computedPropertyName() {
        long begin = ts.beginPosition();
        consume(Token.LB);
        Expression expression = assignmentExpression(true);
        consume(Token.RB);

        return new ComputedPropertyName(begin, ts.endPosition(), expression);
    }

    /**
     * <strong>[12.2.7] Generator Comprehensions</strong>
     * 
     * <pre>
     * GeneratorComprehension<span><sub>[Yield]</sub></span> :
     *     ( Comprehension<span><sub>[?Yield]</sub></span> )
     * </pre>
     * 
     * @return the parsed generator comprehension
     */
    private GeneratorComprehension generatorComprehension() {
        boolean yieldAllowed = context.yieldAllowed;
        boolean awaitAllowed = context.awaitAllowed;
        newContext(ContextKind.GeneratorComprehension);
        try {
            // need to call manually b/c functionBody() isn't used here
            applyStrictMode(false);

            // propagate the outer context's 'yield' state
            context.yieldAllowed = yieldAllowed;
            // propagate the outer context's 'await' state
            context.awaitAllowed = awaitAllowed;

            long begin = ts.beginPosition();
            consume(Token.LP);
            Comprehension comprehension = comprehension();
            consume(Token.RP);
            assert context.assertLiteralsUnchecked(0);

            FunctionContext scope = context.funContext;
            GeneratorComprehension generator = new GeneratorComprehension(begin, ts.endPosition(),
                    scope, comprehension);
            scope.node = generator;

            // generator comprehensions have no named parameters
            scope.parameterNames = new HashSet<>();

            return inheritStrictness(generator);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[12.2.7] Generator Comprehensions</strong>
     * 
     * <pre>
     * LegacyGeneratorComprehension<span><sub>[Yield]</sub></span> :
     *     ( LegacyComprehension<span><sub>[?Yield]</sub></span> )
     * </pre>
     * 
     * @return the parsed legacy generator comprehension
     */
    private GeneratorComprehension legacyGeneratorComprehension() {
        boolean yieldAllowed = context.yieldAllowed;
        boolean awaitAllowed = context.awaitAllowed;
        newContext(ContextKind.GeneratorComprehension);
        try {
            // need to call manually b/c functionBody() isn't used here
            applyStrictMode(false);

            // propagate the outer context's 'yield' state
            context.yieldAllowed = yieldAllowed;
            // propagate the outer context's 'await' state
            context.awaitAllowed = awaitAllowed;

            long begin = ts.beginPosition();
            consume(Token.LP);
            LegacyComprehension comprehension = legacyComprehension();
            consume(Token.RP);
            assert context.assertLiteralsUnchecked(0);

            FunctionContext scope = context.funContext;
            GeneratorComprehension generator = new GeneratorComprehension(begin, ts.endPosition(),
                    scope, comprehension);
            scope.node = generator;

            // generator comprehensions have no named parameters
            scope.parameterNames = new HashSet<>();

            return inheritStrictness(generator);
        } finally {
            restoreContext();
        }
    }

    /**
     * <strong>[12.2.8] Regular Expression Literals</strong>
     * 
     * <pre>
     * RegularExpressionLiteral ::
     *     / RegularExpressionBody / RegularExpressionFlags
     * </pre>
     * 
     * @param tok
     *            the start token of the regular expression, must be either {@link Token#DIV} or
     *            {@link Token#ASSIGN_DIV}
     * @return the parsed regular expression literal
     */
    private Expression regularExpressionLiteral(Token tok) {
        long begin = ts.beginPosition();
        String[] re = ts.readRegularExpression(tok);
        regularExpressionLiteral_EarlyErrors(begin, re[0], re[1]);
        consume(tok);
        return new RegularExpressionLiteral(begin, ts.endPosition(), re[0], re[1]);
    }

    /**
     * 12.2.8.1 Static Semantics: Early Errors
     * 
     * @param sourcePos
     *            the source position where the regular expression literal starts
     * @param pattern
     *            the regular expression literal's pattern part
     * @param flags
     *            the regular expression literal's flags part
     */
    private void regularExpressionLiteral_EarlyErrors(long sourcePos, String pattern, String flags) {
        // parse to validate regular expression, but ignore actual result
        RegExpParser.parse(pattern, flags, sourceName, toLine(sourcePos), toColumn(sourcePos));
    }

    /**
     * <strong>[12.2.9] Template Literals</strong>
     * 
     * <pre>
     * TemplateLiteral<span><sub>[Yield]</sub></span> :
     *     NoSubstitutionTemplate
     *     TemplateHead Expression<span><sub>[In, ?Yield]</sub></span> [Lexical goal <i>InputElementTemplateTail</i>] TemplateSpans<span><sub>[?Yield]</sub></span>
     * TemplateSpans<span><sub>[Yield]</sub></span> :
     *     TemplateTail
     *     TemplateMiddleList<span><sub>[?Yield]</sub></span> [Lexical goal <i>InputElementTemplateTail</i>] TemplateTail
     * TemplateMiddleList<span><sub>[Yield]</sub></span> :
     *     TemplateMiddle Expression<span><sub>[In, ?Yield]</sub></span>
     *     TemplateMiddleList<span><sub>[?Yield]</sub></span> [Lexical goal <i>InputElementTemplateTail</i>] TemplateMiddle Expression<span><sub>[In, ?Yield]</sub></span>
     * </pre>
     * 
     * @param tagged
     *            the flag for tagged template literals
     * @return the parsed template literal
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
     * LetExpression<span><sub>[Yield]</sub></span> :
     *     let ( BindingList<span><sub>[In, ?Yield]</sub></span> ) AssignmentExpression<span><sub>[In, ?Yield]</sub></span>
     * </pre>
     * 
     * @return the parsed let expression
     */
    private LetExpression letExpression() {
        long begin = ts.beginPosition();
        consume(Token.LET);

        consume(Token.LP);
        List<LexicalBinding> lexicalBindings = letBindingList();
        List<Binding> bindings = toBindings(lexicalBindings);
        consume(Token.RP);

        BlockContext scope = enterBlockContext(bindings);
        Expression expression = assignmentExpression(true);
        exitBlockContext();

        LetExpression letExpression = new LetExpression(begin, ts.endPosition(), scope,
                lexicalBindings, expression);
        scope.node = letExpression;
        return letExpression;
    }

    /**
     * <strong>[12.3] Left-Hand-Side Expressions</strong>
     * 
     * <pre>
     * MemberExpression<span><sub>[Yield]</sub></span> :
     *     [Lexical goal <i>InputElementRegExp</i>] PrimaryExpression<span><sub>[?Yield]</sub></span>
     *     MemberExpression<span><sub>[?Yield]</sub></span> [ Expression<span><sub>[In, ?Yield]</sub></span> ]
     *     MemberExpression<span><sub>[?Yield]</sub></span> . IdentifierName
     *     MemberExpression<span><sub>[?Yield]</sub></span> TemplateLiteral<span><sub>[?Yield]</sub></span>
     *     super [ Expression<span><sub>[In, ?Yield]</sub></span> ]
     *     super . IdentifierName
     *     new super Arguments<span><sub>[?Yield]opt</sub></span>
     *     new MemberExpression<span><sub>[?Yield]</sub></span> Arguments<span><sub>[?Yield]</sub></span>
     * NewExpression<span><sub>[Yield]</sub></span> :
     *     MemberExpression<span><sub>[?Yield]</sub></span>
     *     new NewExpression<span><sub>[?Yield]</sub></span>
     * CallExpression<span><sub>[Yield]</sub></span> :
     *     MemberExpression<span><sub>[?Yield]</sub></span> Arguments<span><sub>[?Yield]</sub></span>
     *     super Arguments<span><sub>[?Yield]</sub></span>
     *     CallExpression<span><sub>[?Yield]</sub></span> Arguments<span><sub>[?Yield]</sub></span>
     *     CallExpression<span><sub>[?Yield]</sub></span> [ Expression<span><sub>[In, ?Yield]</sub></span> ]
     *     CallExpression<span><sub>[?Yield]</sub></span> . IdentifierName
     *     CallExpression<span><sub>[?Yield]</sub></span> TemplateLiteral<span><sub>[?Yield]</sub></span>
     * LeftHandSideExpression<span><sub>[Yield]</sub></span> :
     *     NewExpression<span><sub>[?Yield]</sub></span>
     *     CallExpression<span><sub>[?Yield]</sub></span>
     * </pre>
     * 
     * @param allowCall
     *            the flag to select whether or not call expressions are allowed
     * @return the parsed left-hand side expression
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
                if (allowCall) {
                    List<Expression> args = arguments();
                    lhs = new SuperExpression(begin, ts.endPosition(), args);
                    break;
                }
                // fall-through
            case TEMPLATE:
            default:
                if (!allowCall) {
                    return new SuperExpression(begin, ts.endPosition());
                }
                throw reportSyntaxError(Messages.Key.InvalidToken, token().toString());
            }
        } else {
            lhs = primaryExpression();
        }

        for (;;) {
            switch (token()) {
            case DOT:
                consume(Token.DOT);
                String name = identifierName();
                lhs = new PropertyAccessor(begin, ts.endPosition(), lhs, name);
                break;
            case LB:
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
                    context.topContext.directEval = true;
                }
                List<Expression> args = arguments();
                lhs = new CallExpression(begin, ts.endPosition(), lhs, args);
                break;
            case TEMPLATE:
                TemplateLiteral templ = templateLiteral(true);
                lhs = new TemplateCallExpression(begin, ts.endPosition(), lhs, templ);
                break;
            default:
                return lhs;
            }
        }
    }

    /**
     * Entry point for {@link #leftHandSideExpression(boolean)} which additionally performs object
     * literal early error checks.
     * 
     * @return the parsed left-hand side expression
     */
    private Expression leftHandSideExpressionWithValidation() {
        int count = context.countLiterals();
        Expression lhs = leftHandSideExpression(true);
        objectLiteral_EarlyErrors(count);
        return lhs;
    }

    /**
     * <strong>[12.3] Left-Hand-Side Expressions</strong>
     * 
     * <pre>
     * Arguments<span><sub>[Yield]</sub></span> :
     *     ()
     *     ( ArgumentList<span><sub>[?Yield]</sub></span> )
     * ArgumentList<span><sub>[Yield]</sub></span> :
     *     AssignmentExpression<span><sub>[In, ?Yield]</sub></span>
     *     ... AssignmentExpression<span><sub>[In, ?Yield]</sub></span>
     *     ArgumentList<span><sub>[?Yield]</sub></span> , AssignmentExpression<span><sub>[In, ?Yield]</sub></span>
     *     ArgumentList<span><sub>[?Yield]</sub></span> , ... AssignmentExpression<span><sub>[In, ?Yield]</sub></span>
     * </pre>
     * 
     * @return the list of parsed function call arguments
     */
    private List<Expression> arguments() {
        List<Expression> args = newSmallList();
        long position = ts.position(), lineinfo = ts.lineinfo();
        consume(Token.LP);
        if (token() != Token.RP) {
            if (token() != Token.TRIPLE_DOT && isEnabled(CompatibilityOption.LegacyComprehension)) {
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
     * <strong>[12.4] Postfix Expressions</strong><br>
     * <strong>[12.5] Unary Operators</strong>
     * 
     * <pre>
     * PostfixExpression<span><sub>[Yield]</sub></span> :
     *     LeftHandSideExpression<span><sub>[?Yield]</sub></span>
     *     LeftHandSideExpression<span><sub>[?Yield]</sub></span> [no <i>LineTerminator</i> here] ++
     *     LeftHandSideExpression<span><sub>[?Yield]</sub></span> [no <i>LineTerminator</i> here] --
     * UnaryExpression<span><sub>[Yield]</sub></span> :
     *     PostfixExpression<span><sub>[?Yield]</sub></span>
     *     delete UnaryExpression<span><sub>[?Yield]</sub></span>
     *     void UnaryExpression<span><sub>[?Yield]</sub></span>
     *     typeof UnaryExpression<span><sub>[?Yield]</sub></span>
     *     ++ UnaryExpression<span><sub>[?Yield]</sub></span>
     *     -- UnaryExpression<span><sub>[?Yield]</sub></span>
     *     + UnaryExpression<span><sub>[?Yield]</sub></span>
     *     - UnaryExpression<span><sub>[?Yield]</sub></span>
     *     ~ UnaryExpression<span><sub>[?Yield]</sub></span>
     *     ! UnaryExpression<span><sub>[?Yield]</sub></span>
     * </pre>
     * 
     * @return the parsed unary expression
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
                // 12.5.1 Static Semantics: Early Errors
                validateSimpleAssignment(operand, ExceptionType.ReferenceError,
                        Messages.Key.InvalidIncDecTarget);
            }
            if (tok == Token.DELETE) {
                // 12.5.4.1 Static Semantics: Early Errors
                if (operand instanceof Identifier) {
                    reportStrictModeSyntaxError(unary, Messages.Key.StrictModeInvalidDeleteOperand);
                }
            }
            return unary;
        }
        case NAME:
            if (isEnabled(CompatibilityOption.AsyncFunction)
                    && (context.awaitAllowed || context.kind == ContextKind.AsyncFunction)
                    && isName("await")) {
                if (context.kind == ContextKind.AsyncFunction) {
                    if (!context.awaitAllowed) {
                        // await in default parameters
                        reportSyntaxError(Messages.Key.InvalidAwaitExpression);
                    }
                    return awaitExpression();
                } else if (context.kind == ContextKind.GeneratorComprehension) {
                    // await nested in generator comprehension, nested in generator
                    reportSyntaxError(Messages.Key.InvalidAwaitExpression);
                }
                assert false : "unexpected context-kind: " + context.kind;
            }
            // fall-through
        default: {
            Expression lhs = leftHandSideExpression(true);
            if (noLineTerminator()) {
                tok = token();
                if (tok == Token.INC || tok == Token.DEC) {
                    // 12.4.1 Static Semantics: Early Errors
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
     * <strong>[12.6] Multiplicative Operators</strong><br>
     * <strong>[12.7] Additive Operators</strong><br>
     * <strong>[12.8] Bitwise Shift Operators</strong><br>
     * <strong>[12.9] Relational Operators</strong><br>
     * <strong>[12.10] Equality Operators</strong><br>
     * <strong>[12.11] Binary Bitwise Operators</strong><br>
     * <strong>[12.12] Binary Logical Operators</strong><br>
     * 
     * <pre>
     * MultiplicativeExpression<span><sub>[Yield]</sub></span> :
     *     UnaryExpression<span><sub>[?Yield]</sub></span>
     *     MultiplicativeExpression<span><sub>[?Yield]</sub></span> * UnaryExpression<span><sub>[?Yield]</sub></span>
     *     MultiplicativeExpression<span><sub>[?Yield]</sub></span> / UnaryExpression<span><sub>[?Yield]</sub></span>
     *     MultiplicativeExpression<span><sub>[?Yield]</sub></span> % UnaryExpression<span><sub>[?Yield]</sub></span>
     * AdditiveExpression<span><sub>[Yield]</sub></span> :
     *     MultiplicativeExpression<span><sub>[?Yield]</sub></span>
     *     AdditiveExpression<span><sub>[?Yield]</sub></span> + MultiplicativeExpression<span><sub>[?Yield]</sub></span>
     *     AdditiveExpression<span><sub>[?Yield]</sub></span> - MultiplicativeExpression<span><sub>[?Yield]</sub></span>
     * ShiftExpression<span><sub>[Yield]</sub></span> :
     *     AdditiveExpression<span><sub>[?Yield]</sub></span>
     *     ShiftExpression<span><sub>[?Yield]</sub></span> {@literal <<} AdditiveExpression<span><sub>[?Yield]</sub></span>
     *     ShiftExpression<span><sub>[?Yield]</sub></span> {@literal >>} AdditiveExpression<span><sub>[?Yield]</sub></span>
     *     ShiftExpression<span><sub>[?Yield]</sub></span> {@literal >>>} AdditiveExpression<span><sub>[?Yield]</sub></span>
     * RelationalExpression<span><sub>[In, Yield]</sub></span> :
     *     ShiftExpression<span><sub>[?Yield]</sub></span>
     *     RelationalExpression<span><sub>[?in, ?Yield]</sub></span> {@literal <} ShiftExpression<span><sub>[?Yield]</sub></span>
     *     RelationalExpression<span><sub>[?in, ?Yield]</sub></span> {@literal >} ShiftExpression<span><sub>[?Yield]</sub></span>
     *     RelationalExpression<span><sub>[?in, ?Yield]</sub></span> {@literal <=} ShiftExpression<span><sub>[?Yield]</sub></span>
     *     RelationalExpression<span><sub>[?in, ?Yield]</sub></span> {@literal >=} ShiftExpression<span><sub>[?Yield]</sub></span>
     *     RelationalExpression<span><sub>[?in, ?Yield]</sub></span> instanceof ShiftExpression<span><sub>[?Yield]</sub></span>
     *     <span><sub>[+In]</sub></span> RelationalExpression<span><sub>[In, ?Yield]</sub></span> in ShiftExpression<span><sub>[?Yield]</sub></span>
     * EqualityExpression<span><sub>[In, Yield]</sub></span> :
     *     RelationalExpression<span><sub>[?In, ?Yield]</sub></span>
     *     EqualityExpression<span><sub>[?In, ?Yield]</sub></span> == RelationalExpression<span><sub>[?In, ?Yield]</sub></span>
     *     EqualityExpression<span><sub>[?In, ?Yield]</sub></span> != RelationalExpression<span><sub>[?In, ?Yield]</sub></span>
     *     EqualityExpression<span><sub>[?In, ?Yield]</sub></span> === RelationalExpression<span><sub>[?In, ?Yield]</sub></span>
     *     EqualityExpression<span><sub>[?In, ?Yield]</sub></span> !== RelationalExpression<span><sub>[?In, ?Yield]</sub></span>
     * BitwiseANDExpression<span><sub>[In, Yield]</sub></span> :
     *     EqualityExpression<span><sub>[?In, ?Yield]</sub></span>
     *     BitwiseANDExpression<span><sub>[?In, ?Yield]</sub></span> {@literal &} EqualityExpression<span><sub>[?In, ?Yield]</sub></span>
     * BitwiseXORExpression<span><sub>[In, Yield]</sub></span> :
     *     EqualityExpression<span><sub>[?In, ?Yield]</sub></span>
     *     BitwiseXORExpression<span><sub>[?In, ?Yield]</sub></span> ^ EqualityExpression<span><sub>[?In, ?Yield]</sub></span>
     * BitwiseORExpression<span><sub>[In, Yield]</sub></span> :
     *     EqualityExpression<span><sub>[?In, ?Yield]</sub></span>
     *     BitwiseORExpression<span><sub>[?In, ?Yield]</sub></span> | EqualityExpression<span><sub>[?In, ?Yield]</sub></span>
     * LogicalANDExpression<span><sub>[In, Yield]</sub></span> :
     *     EqualityExpression<span><sub>[?In, ?Yield]</sub></span>
     *     LogicalANDExpression<span><sub>[?In, ?Yield]</sub></span> {@literal &&} EqualityExpression<span><sub>[?In, ?Yield]</sub></span>
     * LogicalORExpression<span><sub>[In, Yield]</sub></span> :
     *     EqualityExpression<span><sub>[?In, ?Yield]</sub></span>
     *     LogicalORExpression<span><sub>[?In, ?Yield]</sub></span> || EqualityExpression<span><sub>[?In, ?Yield]</sub></span>
     * </pre>
     * 
     * @param allowIn
     *            the flag to select whether or not the in-operator is allowed
     * @return the parsed binary expression
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
     * <strong>[12.13] Conditional Operator</strong><br>
     * <strong>[12.14] Assignment Operators</strong>
     * 
     * <pre>
     * ConditionalExpression<span><sub>[In, Yield]</sub></span> :
     *     LogicalORExpression<span><sub>[?In, ?Yield]</sub></span>
     *     LogicalORExpression<span><sub>[?In, ?Yield]</sub></span> ? AssignmentExpression<span><sub>[In, ?Yield]</sub></span> : AssignmentExpression<span><sub>[?In, ?Yield]</sub></span>
     * AssignmentExpression<span><sub>[In, Yield]</sub></span> :
     *     ConditionalExpression<span><sub>[?In, ?Yield]</sub></span>
     *     <span><sub>[+Yield]</sub></span> YieldExpression<span><sub>[?In]</sub></span>
     *     ArrowFunction<span><sub>[?In]</sub></span>
     *     LeftHandSideExpression<span><sub>[?Yield]</sub></span> = AssignmentExpression<span><sub>[?In, ?Yield]</sub></span>
     *     LeftHandSideExpression<span><sub>[?Yield]</sub></span> AssignmentOperator AssignmentExpression<span><sub>[?In, ?Yield]</sub></span>
     * </pre>
     * 
     * @param allowIn
     *            the flag to select whether or not the in-operator is allowed
     * @return the parsed assignment expression
     */
    private Expression assignmentExpression(boolean allowIn) {
        int count = context.countLiterals();
        Expression expr = assignmentExpression(allowIn, count);
        objectLiteral_EarlyErrors(count);
        return expr;
    }

    /**
     * Same as {@link #assignmentExpression(boolean)} except object literal early errors are not
     * checked. This method needs to be used if the AssignmentExpression is in a possible
     * destructuring assignment position.
     * 
     * @param allowIn
     *            the flag to select whether or not the in-operator is allowed
     * @return the parsed assignment expression
     */
    private Expression assignmentExpressionNoValidation(boolean allowIn) {
        return assignmentExpression(allowIn, context.countLiterals());
    }

    private Expression assignmentExpression(boolean allowIn, int oldCount) {
        if (token() == Token.YIELD) {
            if (context.kind == ContextKind.Generator) {
                if (!context.yieldAllowed) {
                    // FIXME: maybe this is actually allowed and "yield" is parsed as identifier?!
                    // yield in default parameters
                    reportSyntaxError(Messages.Key.InvalidYieldExpression);
                }
                return yieldExpression(allowIn);
            } else if (context.kind == ContextKind.GeneratorComprehension && context.yieldAllowed) {
                // 12.2.7.1 Static Semantics: Early Errors for GeneratorComprehension
                // yield nested in generator comprehension, nested in generator
                reportSyntaxError(Messages.Key.InvalidYieldExpression);
            } else if (context.kind == ContextKind.Function
                    && isEnabled(CompatibilityOption.LegacyGenerator)) {
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
            discardUncheckedObjectLiterals(oldCount);
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
     * <strong>[12.14] Assignment Operators</strong>
     * 
     * <pre>
     * AssignmentOperator : <b>one of</b>
     *     {@literal *=  /=  %=  +=  -=  <<=  >>=  >>>=  &=  ^=  |=}
     * </pre>
     * 
     * @param tok
     *            the token to inspect
     * @return {@code true} if the token is a compound assignment token
     */
    private static boolean isAssignmentOperator(Token tok) {
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
     * Static Semantics: IsValidSimpleAssignmentTarget
     * <ul>
     * <li>12.2.0.4 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.2.10.3 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.3.1.4 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.4.3 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.5.3 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.6.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.7.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.8.2 Semantics: IsValidSimpleAssignmentTarget
     * <li>12.9.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.10.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.11.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.12.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.13.2 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.14.3 Static Semantics: IsValidSimpleAssignmentTarget
     * <li>12.15.2 Static Semantics: IsValidSimpleAssignmentTarget
     * </ul>
     * 
     * @param lhs
     *            the left-hand side expression to validate
     * @param type
     *            the exception type for errors
     * @param messageKey
     *            the message key for errors
     * @return the {@code lhs} parameter as a left-hand side expression node
     */
    private LeftHandSideExpression validateSimpleAssignment(Expression lhs, ExceptionType type,
            Messages.Key messageKey) {
        if (lhs instanceof Identifier) {
            Identifier ident = (Identifier) lhs;
            if (context.strictMode != StrictMode.NonStrict) {
                String name = ident.getName();
                if ("eval".equals(name) || "arguments".equals(name)) {
                    reportStrictModeSyntaxError(ident,
                            Messages.Key.StrictModeInvalidAssignmentTarget);
                }
            }
            return ident;
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
     * <strong>[12.14.5] Destructuring Assignment</strong>
     * 
     * <ul>
     * <li>12.14.1 Static Semantics: Early Errors
     * <li>12.14.5.1 Static Semantics: Early Errors
     * <li>13.6.4.1 Static Semantics: Early Errors
     * </ul>
     * 
     * @param lhs
     *            the left-hand side expression to validate
     * @param type
     *            the exception type for errors
     * @param messageKey
     *            the message key for errors
     * @return the {@code lhs} parameter as a left-hand side expression node
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

    /**
     * <strong>[12.14.5] Destructuring Assignment</strong>
     * 
     * <pre>
     * ObjectAssignmentPattern<span><sub>[Yield]</sub></span> :
     *     { }
     *     { AssignmentPropertyList<span><sub>[?Yield]</sub></span> }
     *     { AssignmentPropertyList<span><sub>[?Yield]</sub></span> , }
     * AssignmentPropertyList<span><sub>[Yield]</sub></span> :
     *     AssignmentProperty<span><sub>[?Yield]</sub></span>
     *     AssignmentPropertyList<span><sub>[?Yield]</sub></span> , AssignmentProperty<span><sub>[?Yield]</sub></span>
     * AssignmentProperty<span><sub>[Yield]</sub></span> :
     *     IdentifierReference<span><sub>[?Yield]</sub></span> Initializer<span><sub>[In, ?Yield]opt</sub></span>
     *     PropertyName : AssignmentElement<span><sub>[?Yield]</sub></span>
     * AssignmentElement<span><sub>[Yield]</sub></span> :
     *     DestructuringAssignmentTarget<span><sub>[?Yield]</sub></span> Initializer<span><sub>[In, ?Yield]opt</sub></span>
     * AssignmentElement<span><sub>[Yield]</sub></span> :
     *     DestructuringAssignmentTarget<span><sub>[?Yield]</sub></span> Initializer<span><sub>[In, ?Yield]opt</sub></span>
     * DestructuringAssignmentTarget<span><sub>[Yield]</sub></span> :
     *     LeftHandSideExpression<span><sub>[?Yield]</sub></span>
     * </pre>
     * 
     * @param object
     *            the object literal to convert
     * @return the object assignment pattern for the object literal
     */
    private ObjectAssignmentPattern toDestructuring(ObjectLiteral object) {
        List<AssignmentProperty> list = newSmallList();
        for (PropertyDefinition p : object.getProperties()) {
            AssignmentProperty property;
            if (p instanceof PropertyValueDefinition) {
                // AssignmentProperty : PropertyName ':' AssignmentElement
                PropertyValueDefinition def = (PropertyValueDefinition) p;
                PropertyName propertyName = def.getPropertyName();
                Expression propertyValue = def.getPropertyValue();
                LeftHandSideExpression target;
                Expression initializer;
                if (propertyValue instanceof AssignmentExpression) {
                    // AssignmentElement : DestructuringAssignmentTarget Initializer
                    AssignmentExpression assignment = (AssignmentExpression) propertyValue;
                    if (assignment.getOperator() != AssignmentExpression.Operator.ASSIGN) {
                        reportSyntaxError(p, Messages.Key.InvalidDestructuring);
                    }
                    target = destructuringAssignmentTarget_EarlyErrors(assignment.getLeft());
                    initializer = assignment.getRight();
                } else {
                    // AssignmentElement : DestructuringAssignmentTarget
                    target = destructuringAssignmentTarget_EarlyErrors(propertyValue);
                    initializer = null;
                }
                property = new AssignmentProperty(p.getBeginPosition(), p.getEndPosition(),
                        propertyName, target, initializer);
            } else if (p instanceof PropertyNameDefinition) {
                // AssignmentProperty : IdentifierReference
                PropertyNameDefinition def = (PropertyNameDefinition) p;
                assignmentProperty_EarlyErrors(def.getPropertyName());
                property = new AssignmentProperty(p.getBeginPosition(), p.getEndPosition(),
                        def.getPropertyName(), null);
            } else if (p instanceof CoverInitializedName) {
                // AssignmentProperty : IdentifierReference Initializer
                CoverInitializedName def = (CoverInitializedName) p;
                assignmentProperty_EarlyErrors(def.getPropertyName());
                property = new AssignmentProperty(p.getBeginPosition(), p.getEndPosition(),
                        def.getPropertyName(), def.getInitializer());
            } else {
                assert p instanceof MethodDefinition;
                throw reportSyntaxError(p, Messages.Key.InvalidDestructuring);
            }
            list.add(property);
        }
        context.removeLiteral(object);
        ObjectAssignmentPattern pattern = new ObjectAssignmentPattern(object.getBeginPosition(),
                object.getEndPosition(), list);
        if (object.isParenthesized()) {
            pattern.addParentheses();
        }
        return pattern;
    }

    /**
     * <strong>[12.14.5] Destructuring Assignment</strong>
     * 
     * <pre>
     * ArrayAssignmentPattern<span><sub>[Yield]</sub></span> :
     *     [ Elision<span><sub>opt</sub></span> AssignmentRestElement<span><sub>[?Yield]opt</sub></span> ]
     *     [ AssignmentElementList<span><sub>[?Yield]</sub></span>  ]
     *     [ AssignmentElementList<span><sub>[?Yield]</sub></span> , Elision<span><sub>opt</sub></span> AssignmentRestElement<span><sub>[?Yield]opt</sub></span> ]
     * AssignmentElementList<span><sub>[Yield]</sub></span> :
     *     AssignmentElisionElement<span><sub>[?Yield]</sub></span>
     *     AssignmentElementList<span><sub>[?Yield]</sub></span> , AssignmentElisionElement<span><sub>[?Yield]</sub></span>
     * AssignmentElisionElement<span><sub>[Yield]</sub></span> :
     *     Elision<span><sub>opt</sub></span>  AssignmentElement<span><sub>[?Yield]</sub></span>
     * AssignmentElement<span><sub>[Yield]</sub></span> :
     *     DestructuringAssignmentTarget<span><sub>[?Yield]</sub></span> Initializer<span><sub>[In, ?Yield]opt</sub></span>
     * AssignmentRestElement<span><sub>[Yield]</sub></span> : 
     *     ... DestructuringAssignmentTarget<span><sub>[?Yield]</sub></span>
     * DestructuringAssignmentTarget<span><sub>[Yield]</sub></span> :
     *     LeftHandSideExpression<span><sub>[?Yield]</sub></span>
     * </pre>
     * 
     * @param array
     *            the array literal to convert
     * @return the array assignment pattern for the array literal
     */
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
                Expression expression = ((SpreadElement) e).getExpression();
                LeftHandSideExpression target = assignmentRestElement_EarlyErrors(expression);
                element = new AssignmentRestElement(e.getBeginPosition(), e.getEndPosition(),
                        target);
                // no further elements after AssignmentRestElement allowed
                if (iterator.hasNext()) {
                    reportSyntaxError(iterator.next(), Messages.Key.InvalidDestructuring);
                }
                // no trailing comma allowed
                if (array.hasTrailingComma()) {
                    reportSyntaxError(expression, Messages.Key.InvalidDestructuring);
                }
            } else {
                LeftHandSideExpression target;
                Expression initializer;
                if (e instanceof AssignmentExpression) {
                    // AssignmentElement : DestructuringAssignmentTarget Initializer
                    AssignmentExpression assignment = (AssignmentExpression) e;
                    if (assignment.getOperator() != AssignmentExpression.Operator.ASSIGN) {
                        reportSyntaxError(e, Messages.Key.InvalidDestructuring);
                    }
                    target = destructuringAssignmentTarget_EarlyErrors(assignment.getLeft());
                    initializer = assignment.getRight();
                } else {
                    // AssignmentElement : DestructuringAssignmentTarget
                    target = destructuringAssignmentTarget_EarlyErrors(e);
                    initializer = null;
                }
                element = new AssignmentElement(e.getBeginPosition(), e.getEndPosition(), target,
                        initializer);
            }
            list.add(element);
        }
        ArrayAssignmentPattern pattern = new ArrayAssignmentPattern(array.getBeginPosition(),
                array.getEndPosition(), list);
        if (array.isParenthesized()) {
            pattern.addParentheses();
        }
        return pattern;
    }

    /**
     * 12.14.5.1 Static Semantics: Early Errors
     * 
     * @param lhs
     *            the left-hand side expression to check
     * @return the {@code lhs} parameter as a left-hand side expression node
     */
    private LeftHandSideExpression destructuringAssignmentTarget_EarlyErrors(Expression lhs) {
        if (lhs instanceof ObjectAssignmentPattern) {
            return (ObjectAssignmentPattern) lhs;
        } else if (lhs instanceof ArrayAssignmentPattern) {
            return (ArrayAssignmentPattern) lhs;
        }
        return validateAssignment(lhs, ExceptionType.SyntaxError, Messages.Key.InvalidDestructuring);
    }

    /**
     * 12.14.5.1 Static Semantics: Early Errors
     * 
     * @param lhs
     *            the left-hand side expression to check
     * @return the {@code lhs} parameter as a left-hand side expression node
     */
    private LeftHandSideExpression assignmentRestElement_EarlyErrors(Expression lhs) {
        return validateSimpleAssignment(lhs, ExceptionType.SyntaxError,
                Messages.Key.InvalidDestructuring);
    }

    /**
     * 12.14.5.1 Static Semantics: Early Errors
     * 
     * @param identifier
     *            the identifier to check
     */
    private void assignmentProperty_EarlyErrors(Identifier identifier) {
        validateSimpleAssignment(identifier, ExceptionType.SyntaxError,
                Messages.Key.InvalidDestructuring);
    }

    /**
     * <strong>[12.15] Comma Operator</strong>
     * 
     * <pre>
     * Expression<span><sub>[In, Yield]</sub></span> :
     *     AssignmentExpression<span><sub>[?In, ?Yield]</sub></span>
     *     Expression<span><sub>[?In, ?Yield]</sub></span> , AssignmentExpression<span><sub>[?In, ?Yield]</sub></span>
     * </pre>
     * 
     * @param allowIn
     *            the flag to select whether or not the in-operator is allowed
     * @return the parsed expression
     */
    private Expression expression(boolean allowIn) {
        Expression expr = assignmentExpression(allowIn);
        if (token() == Token.COMMA) {
            return commaExpression(expr, allowIn);
        }
        return expr;
    }

    /**
     * <strong>[12.15] Comma Operator</strong>
     * 
     * <pre>
     * Expression<span><sub>[In, Yield]</sub></span> :
     *     AssignmentExpression<span><sub>[?In, ?Yield]</sub></span>
     *     Expression<span><sub>[?In, ?Yield]</sub></span> , AssignmentExpression<span><sub>[?In, ?Yield]</sub></span>
     * </pre>
     * 
     * @param expr
     *            the first expression
     * @param allowIn
     *            the flag to select whether or not the in-operator is allowed
     * @return the parsed comma expression
     */
    private CommaExpression commaExpression(Expression expr, boolean allowIn) {
        List<Expression> list = newList();
        list.add(expr);
        while (token() == Token.COMMA) {
            consume(Token.COMMA);
            expr = assignmentExpression(allowIn);
            list.add(expr);
        }
        return new CommaExpression(list);
    }

    /* ***************************************************************************************** */

    /**
     * <strong>[11.9] Automatic Semicolon Insertion</strong>
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
     * Returns {@code true} if the last and the current token are not separated from each other by a
     * line-terminator.
     * 
     * @return {@code true} if there is no line separator
     */
    private boolean noLineTerminator() {
        return !ts.hasCurrentLineTerminator();
    }

    /**
     * Returns {@code true} if the current and the next token are not separated from each other by a
     * line-terminator.
     * 
     * @return {@code true} if there is no line separator
     */
    private boolean noNextLineTerminator() {
        return !ts.hasNextLineTerminator();
    }

    /**
     * Returns true if the current token is of type {@link Token#NAME} and its name is {@code name}.
     * 
     * @param name
     *            the name to test
     * @return {@code true} if the current token is the requested name
     */
    private boolean isName(String name) {
        Token tok = token();
        return tok == Token.NAME && name.equals(getName(tok));
    }

    /**
     * Returns the token's name.
     * 
     * @param tok
     *            the token to inspect
     * @return the token name
     */
    private String getName(Token tok) {
        switch (tok) {
        case NAME:
        case ESCAPED_NAME:
        case ESCAPED_RESERVED_WORD:
        case ESCAPED_STRICT_RESERVED_WORD:
        case ESCAPED_YIELD:
        case ESCAPED_LET:
            return ts.getString();
        default:
            return tok.getName();
        }
    }

    /**
     * <strong>[11.6] Identifier Names and Identifiers</strong>
     * 
     * @return the parsed identifier name
     */
    private String identifierName() {
        Token tok = token();
        if (!Token.isIdentifierName(tok)) {
            reportTokenNotIdentifierName(tok);
        }
        String name = getName(tok);
        consume(tok);
        return name;
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * @return the parsed number literal
     */
    private double numericLiteral() {
        double number = ts.getNumber();
        consume(Token.NUMBER);
        return number;
    }

    /**
     * <strong>[11.8.4] String Literals</strong>
     * 
     * @return the parsed string literal
     */
    private String stringLiteral() {
        String string = ts.getString();
        consume(Token.STRING);
        return string;
    }
}

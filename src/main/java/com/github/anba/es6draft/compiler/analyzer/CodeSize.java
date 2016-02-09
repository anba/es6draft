/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;
import com.github.anba.es6draft.ast.synthetic.MethodDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.SpreadArrayLiteral;
import com.github.anba.es6draft.ast.synthetic.SpreadElementMethod;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;
import com.github.anba.es6draft.ast.synthetic.SyntheticNode;

/**
 * Estimates the generated code size and splits statements or expressions into sub-methods to avoid exceeding the 64K
 * bytecode size limit.
 */
public final class CodeSize implements IntNodeVisitor<CodeSize.State> {
    private static final boolean DEBUG = false;
    private static final int MAX_SIZE = 65535;
    private static final int MAX_SIZE_ALLOWED = MAX_SIZE / 2;
    private static final int MAX_EXPRESSION_SIZE = 1024 + 512;
    private static final int MAX_STATEMENT_SIZE = 8192;
    private static final int MAX_TOP_STATEMENT_SIZE = MAX_SIZE_ALLOWED;
    private static final int MAX_ARRAY_ELEMENTS_SIZE = 2 * MAX_EXPRESSION_SIZE;
    private static final int MAX_CLASS_PROPERTIES_SIZE = 2 * MAX_EXPRESSION_SIZE;
    private static final int MAX_OBJECT_PROPERTIES_SIZE = 2 * MAX_EXPRESSION_SIZE;
    private static final int MAX_TEMPLATE_ELEMENTS_SIZE = 2 * MAX_EXPRESSION_SIZE;

    /**
     * Splits statements or expressions into sub-methods to avoid exceeding the 64K bytecode size limit.
     * 
     * @param topLevelNode
     *            the top level node
     * @throws CodeSizeException
     *             if the estimated code size exceeds the bytecode limit
     */
    public static void analyze(TopLevelNode<?> topLevelNode) throws CodeSizeException {
        topLevelNode.accept(new CodeSize(size -> {
            throw new CodeSizeException(size);
        }), null);
    }

    /**
     * Splits statements or expressions into sub-methods to avoid exceeding the 64K bytecode size limit and returns the
     * estimated size for the generated code.
     * 
     * @param topLevelNode
     *            the top level node
     * @return the estimated code size
     */
    public static int calculate(TopLevelNode<?> topLevelNode) {
        return topLevelNode.accept(new CodeSize(size -> {
            // Ignored.
        }), null);
    }

    private final Consumer<Integer> onSizeViolation;

    private CodeSize(Consumer<Integer> onSizeViolation) {
        this.onSizeViolation = onSizeViolation;
    }

    // TODO: Add 'quota' field to track current size?
    static final class State {
        final TopLevelNode<?> top;

        State(TopLevelNode<?> top) {
            this.top = top;
        }

        private boolean isGeneratorOrAsync() {
            if (top instanceof FunctionNode) {
                FunctionNode function = (FunctionNode) top;
                return function.isGenerator() || function.isAsync();
            }
            return false;
        }

        void notifySyntheticNodes(Node newNode) {
            if (isGeneratorOrAsync()) {
                newNode.accept(new FindYieldOrAwait(), yieldOrAwait -> {
                    ((SyntheticNode) newNode).setResumePoint(true);
                });
            }
        }

        Program program() {
            TopLevelNode<?> top = this.top;
            while (!(top instanceof Program)) {
                top = top.getScope().getEnclosingScope().getTop().getNode();
            }
            return (Program) top;
        }
    }

    private static void debug(String format, Object... args) {
        System.out.printf(format, args);
    }

    private int accept(Node node, State state) {
        return node.accept(this, state);
    }

    private int accept(List<? extends Node> nodes, State state) {
        // return nodes.stream().mapToInt(e -> e.accept(this, state)).sum();
        int size = 0;
        for (Node node : nodes) {
            size += node.accept(this, state);
        }
        return size;
    }

    private int acceptIfPresent(Node node, State state) {
        if (node != null) {
            return node.accept(this, state);
        }
        return 0;
    }

    private <NODE extends Node> int accept(NODE node, State state, Function<NODE, NODE> mapper, Consumer<NODE> updater,
            int limit) {
        int size = node.accept(this, state);
        if (size < limit) {
            return size;
        }
        if (DEBUG)
            debug("Replace %s=%d [%s]%n", node.getClass().getSimpleName(), size, state.program().getSource());
        NODE newNode = mapper.apply(node);
        state.notifySyntheticNodes(newNode);
        updater.accept(newNode);
        return newNode.accept(this, state);
    }

    private <NODE extends Node> int accept(List<NODE> nodes, State state, Function<NODE, NODE> mapper,
            Consumer<List<NODE>> updater, int limit) {
        int size = 0;
        List<NODE> newNodes = null;
        for (int i = 0; i < nodes.size(); ++i) {
            NODE node = nodes.get(i);
            int nodeSize = node.accept(this, state);
            if (nodeSize < limit) {
                size += nodeSize;
                continue;
            }
            if (DEBUG)
                debug("Replace %s=%d [%s]%n", node.getClass().getSimpleName(), nodeSize, state.program().getSource());
            NODE newNode = mapper.apply(node);
            if (newNodes == null) {
                newNodes = new ArrayList<>(nodes);
            }
            newNodes.set(i, newNode);
            state.notifySyntheticNodes(newNode);
            size += newNode.accept(this, state);
        }
        if (newNodes != null) {
            updater.accept(newNodes);
        }
        return size;
    }

    private int expression(Expression expression, State state, Consumer<Expression> updater) {
        return accept(expression, state, ExpressionMethod::new, updater, MAX_EXPRESSION_SIZE);
    }

    private int statement(Statement statement, State state, Consumer<Statement> updater) {
        return accept(statement, state, StatementListMethod::new, updater, MAX_STATEMENT_SIZE);
    }

    private int statementList(Supplier<List<StatementListItem>> getter, State state,
            Consumer<List<StatementListItem>> updater) {
        return statementList(getter, state, updater, MAX_TOP_STATEMENT_SIZE);
    }

    private int statementList(Supplier<List<StatementListItem>> getter, State state,
            Consumer<List<StatementListItem>> updater, int limit) {
        int statements = accept(getter.get(), state, StatementListMethod::new, updater, MAX_STATEMENT_SIZE);
        if (statements < limit) {
            return statements;
        }
        int newStatements = split(getter.get(), state, StatementListMethod::new, updater, limit);
        if (DEBUG)
            debug("Split statementList: %d -> %d [%s]%n", statements, newStatements, state.program().getSource());
        return newStatements;
    }

    private int moduleItemList(Supplier<List<ModuleItem>> getter, State state, Consumer<List<ModuleItem>> updater) {
        int statements = accept(getter.get(), state, StatementListMethod::new, updater, MAX_STATEMENT_SIZE);
        if (statements < MAX_TOP_STATEMENT_SIZE) {
            return statements;
        }
        int newStatements = split(getter.get(), state, StatementListMethod::new, updater, MAX_TOP_STATEMENT_SIZE);
        if (DEBUG)
            debug("Split moduleItemList: %d -> %d [%s]%n", statements, newStatements, state.program().getSource());
        return newStatements;
    }

    private <NODE extends Node> int split(List<NODE> nodes, State state, Function<List<NODE>, NODE> mapper,
            Consumer<List<NODE>> updater, int limit) {
        ArrayList<NODE> list = new ArrayList<>(nodes);
        int newSize;
        boolean replaced;
        do {
            newSize = 0;
            replaced = false;
            int chunkSize = 0, end = list.size();
            for (int i = list.size() - 1; i >= 0; --i) {
                int nodeSize = list.get(i).accept(this, state);
                if (chunkSize + nodeSize < limit) {
                    chunkSize += nodeSize;
                } else {
                    // Insert new chunk.
                    int start = i + 1;
                    if (start < end) {
                        int rangeSize = replaceRange(list, start, end, state, mapper);
                        if (DEBUG)
                            debug("ReplaceRange: %d -> %d [%s]%n", chunkSize, rangeSize, state.program().getSource());
                        newSize += rangeSize;
                        replaced = true;
                    }
                    chunkSize = nodeSize;
                    end = start;
                }
            }
            if (chunkSize > limit) {
                int rangeSize = replaceRange(list, 0, end, state, mapper);
                if (DEBUG)
                    debug("ReplaceRange: %d -> %d [%s]%n", chunkSize, rangeSize, state.program().getSource());
                newSize += rangeSize;
                replaced = true;
            } else {
                newSize += chunkSize;
            }
        } while (replaced && newSize > limit);
        updater.accept(list);
        return newSize;
    }

    private <NODE extends Node> int replaceRange(List<NODE> list, int start, int end, State state,
            Function<List<NODE>, NODE> mapper) {
        List<NODE> view = list.subList(start, end);
        NODE newNode = mapper.apply(new ArrayList<>(view));
        view.clear();
        list.add(start, newNode);
        state.notifySyntheticNodes(newNode);
        return newNode.accept(this, state);
    }

    private void checkValidSize(int size) {
        if (size > MAX_SIZE_ALLOWED) {
            onSizeViolation.accept(size);
        }
    }

    private int listSize(List<?> list, int size) {
        return list.size() * size;
    }

    private int stringSize(String s) {
        if (s.length() <= 32768) {
            return 5;
        }
        return ((s.length() / 32768) + 1) * 10;
    }

    @Override
    public int visit(ArrayAssignmentPattern node, State state) {
        int elements = accept(node.getElements(), state);
        return 30 + elements + listSize(node.getElements(), 5);
    }

    @Override
    public int visit(ArrayBindingPattern node, State state) {
        int elements = accept(node.getElements(), state);
        return 30 + elements + listSize(node.getElements(), 5);
    }

    @Override
    public int visit(ArrayComprehension node, State state) {
        int comprehension = accept(node.getComprehension(), state);
        return 25 + comprehension;
    }

    @Override
    public int visit(ArrayLiteral node, State state) {
        int elements = accept(node.getElements(), state, ExpressionMethod::new, node::setElements, MAX_EXPRESSION_SIZE);
        if (elements > MAX_ARRAY_ELEMENTS_SIZE) {
            elements = split(node.getElements(), state, SpreadElementMethod::new, node::setElements,
                    MAX_ARRAY_ELEMENTS_SIZE);
        }
        return 25 + elements + listSize(node.getElements(), 5);
    }

    @Override
    public int visit(ArrowFunction node, State state) {
        if (node.getExpression() != null) {
            visitConciseFunction(node, node::getExpression, state);
        } else {
            visitFunction(node, state);
        }
        return 10;
    }

    @Override
    public int visit(AssignmentElement node, State state) {
        int target = accept(node.getTarget(), state);
        int initializer = acceptIfPresent(node.getInitializer(), state);
        return 25 + target + initializer;
    }

    @Override
    public int visit(AssignmentExpression node, State state) {
        int left = accept(node.getLeft(), state);
        int right = accept(node.getRight(), state);
        return 10 + left + right;
    }

    @Override
    public int visit(AssignmentProperty node, State state) {
        int propertyName = acceptIfPresent(node.getPropertyName(), state);
        int target = accept(node.getTarget(), state);
        int initializer = acceptIfPresent(node.getInitializer(), state);
        return 25 + propertyName + target + initializer;
    }

    @Override
    public int visit(AssignmentRestElement node, State state) {
        int target = accept(node.getTarget(), state);
        return 25 + target;
    }

    @Override
    public int visit(AssignmentRestProperty node, State state) {
        int target = accept(node.getTarget(), state);
        return 25 + target;
    }

    @Override
    public int visit(AsyncArrowFunction node, State state) {
        if (node.getExpression() != null) {
            visitConciseFunction(node, node::getExpression, state);
        } else {
            visitFunction(node, state);
        }
        return 10;
    }

    @Override
    public int visit(AsyncFunctionDeclaration node, State state) {
        visitFunction(node, state);
        return 0;
    }

    @Override
    public int visit(AsyncFunctionExpression node, State state) {
        visitFunction(node, state);
        return 10;
    }

    @Override
    public int visit(AsyncGeneratorDeclaration node, State state) {
        visitFunction(node, state);
        return 0;
    }

    @Override
    public int visit(AsyncGeneratorExpression node, State state) {
        visitFunction(node, state);
        return 10;
    }

    @Override
    public int visit(AwaitExpression node, State state) {
        int expression = accept(node.getExpression(), state);
        return 150 + expression;
    }

    @Override
    public int visit(BinaryExpression node, State state) {
        int left = expression(node.getLeft(), state, node::setLeft);
        int right = expression(node.getRight(), state, node::setRight);
        return 20 + left + right;
    }

    @Override
    public int visit(BindingElement node, State state) {
        int binding = accept(node.getBinding(), state);
        int initializer = acceptIfPresent(node.getInitializer(), state);
        return 25 + binding + initializer;
    }

    @Override
    public int visit(BindingElision node, State state) {
        return 0;
    }

    @Override
    public int visit(BindingIdentifier node, State state) {
        return 15;
    }

    @Override
    public int visit(BindingProperty node, State state) {
        int propertyName = acceptIfPresent(node.getPropertyName(), state);
        int binding = accept(node.getBinding(), state);
        int initializer = acceptIfPresent(node.getInitializer(), state);
        return 25 + propertyName + binding + initializer;
    }

    @Override
    public int visit(BindingRestElement node, State state) {
        int binding = accept(node.getBinding(), state);
        return 25 + binding;
    }

    @Override
    public int visit(BindingRestProperty node, State state) {
        int bindingIdentifier = accept(node.getBindingIdentifier(), state);
        return 25 + bindingIdentifier;
    }

    @Override
    public int visit(BlockStatement node, State state) {
        int statements = statementList(node::getStatements, state, node::setStatements, MAX_STATEMENT_SIZE);
        return 15 + statements;
    }

    @Override
    public int visit(BooleanLiteral node, State state) {
        return 5;
    }

    @Override
    public int visit(BreakStatement node, State state) {
        return 5;
    }

    @Override
    public int visit(CallExpression node, State state) {
        int base = accept(node.getBase(), state);
        int arguments = accept(node.getArguments(), state);
        return 30 + base + arguments + listSize(node.getArguments(), 5);
    }

    @Override
    public int visit(CallSpreadElement node, State state) {
        int expression = accept(node.getExpression(), state);
        return 10 + expression;
    }

    @Override
    public int visit(CatchNode node, State state) {
        int catchParameter = accept(node.getCatchParameter(), state);
        int catchBlock = accept(node.getCatchBlock(), state);
        return 35 + catchParameter + catchBlock;
    }

    @Override
    public int visit(ClassDeclaration node, State state) {
        return visitClassDefinition(node, state);
    }

    @Override
    public int visit(ClassExpression node, State state) {
        return visitClassDefinition(node, state);
    }

    private int visitClassDefinition(ClassDefinition node, State state) {
        int heritage = acceptIfPresent(node.getHeritage(), state);
        int decorators = accept(node.getDecorators(), state);
        int properties = accept(node.getProperties(), state);
        if (properties > MAX_CLASS_PROPERTIES_SIZE) {
            properties = split(node.getProperties(), state, MethodDefinitionsMethod::new, node::setProperties,
                    MAX_CLASS_PROPERTIES_SIZE);
        }
        return 50 + heritage + decorators + properties + listSize(node.getDecorators(), 10)
                + listSize(node.getProperties(), 10);
    }

    @Override
    public int visit(CommaExpression node, State state) {
        int operands = accept(node.getOperands(), state, ExpressionMethod::new, node::setOperands, MAX_EXPRESSION_SIZE);
        if (operands > MAX_EXPRESSION_SIZE) {
            operands = split(node.getOperands(), state, list -> new ExpressionMethod(new CommaExpression(list)),
                    node::setOperands, MAX_EXPRESSION_SIZE);
        }
        return 5 + operands;
    }

    @Override
    public int visit(Comprehension node, State state) {
        int list = accept(node.getList(), state);
        int expression = accept(node.getExpression(), state);
        return 50 + list + expression;
    }

    @Override
    public int visit(ComprehensionFor node, State state) {
        int binding = accept(node.getBinding(), state);
        int expression = accept(node.getExpression(), state);
        return 40 + binding + expression;
    }

    @Override
    public int visit(ComprehensionIf node, State state) {
        int test = accept(node.getTest(), state);
        return 15 + test;
    }

    @Override
    public int visit(ComputedPropertyName node, State state) {
        int expression = accept(node.getExpression(), state);
        return 15 + expression;
    }

    @Override
    public int visit(ConditionalExpression node, State state) {
        int test = accept(node.getTest(), state);
        int then = accept(node.getThen(), state);
        int otherwise = accept(node.getOtherwise(), state);
        return 20 + test + then + otherwise;
    }

    @Override
    public int visit(ContinueStatement node, State state) {
        return 5;
    }

    @Override
    public int visit(DebuggerStatement node, State state) {
        return 5;
    }

    @Override
    public int visit(DoExpression node, State state) {
        int statement = accept(node.getStatement(), state);
        checkValidSize(statement);
        return 25;
    }

    @Override
    public int visit(DoWhileStatement node, State state) {
        int test = accept(node.getTest(), state);
        int statement = statement(node.getStatement(), state, node::setStatement);
        // int statement = accept(node.getStatement(), state, StatementListMethod::new, node::setStatement, 0);
        return 25 + test + statement;
    }

    @Override
    public int visit(ElementAccessor node, State state) {
        int base = accept(node.getBase(), state);
        int element = accept(node.getElement(), state);
        return 10 + base + element;
    }

    @Override
    public int visit(Elision node, State state) {
        return 0;
    }

    @Override
    public int visit(EmptyExpression node, State state) {
        return 0;
    }

    @Override
    public int visit(EmptyStatement node, State state) {
        return 0;
    }

    @Override
    public int visit(ExportClause node, State state) {
        return 0;
    }

    @Override
    public int visit(ExportDeclaration node, State state) {
        switch (node.getType()) {
        case All:
        case External:
        case Local:
            return 0;
        case Variable:
            return accept(node.getVariableStatement(), state);
        case Declaration:
            return accept(node.getDeclaration(), state);
        case DefaultHoistableDeclaration:
            return accept(node.getHoistableDeclaration(), state);
        case DefaultClassDeclaration:
            return accept(node.getClassDeclaration(), state);
        case DefaultExpression:
            return accept(node.getExpression(), state);
        default:
            throw new AssertionError();
        }
    }

    @Override
    public int visit(ExportDefaultExpression node, State state) {
        int expression = accept(node.getExpression(), state);
        return 15 + expression;
    }

    @Override
    public int visit(ExportSpecifier node, State state) {
        return 0;
    }

    @Override
    public int visit(ExpressionMethod node, State state) {
        // Don't descend into synthetic nodes.
        return 5;
    }

    @Override
    public int visit(ExpressionStatement node, State state) {
        int expression = accept(node.getExpression(), state);
        return 5 + expression;
    }

    @Override
    public int visit(ForAwaitStatement node, State state) {
        return visitForIteration(node, state);
    }

    @Override
    public int visit(ForEachStatement node, State state) {
        return visitForIteration(node, state);
    }

    @Override
    public int visit(ForInStatement node, State state) {
        return visitForIteration(node, state);
    }

    @Override
    public int visit(FormalParameter node, State state) {
        return accept(node.getElement(), state);
    }

    @Override
    public int visit(FormalParameterList node, State state) {
        return accept(node.getFormals(), state);
    }

    @Override
    public int visit(ForOfStatement node, State state) {
        return visitForIteration(node, state);
    }

    private int visitForIteration(ForIterationNode node, State state) {
        int head = acceptIfPresent(node.getHead(), state);
        int expression = acceptIfPresent(node.getExpression(), state);
        int statement = statement(node.getStatement(), state, node::setStatement);
        return 150 + head + expression + statement;
    }

    @Override
    public int visit(ForStatement node, State state) {
        int head = acceptIfPresent(node.getHead(), state);
        int test = acceptIfPresent(node.getTest(), state);
        int step = acceptIfPresent(node.getStep(), state);
        int statement = statement(node.getStatement(), state, node::setStatement);
        return 30 + head + test + step + statement;
    }

    @Override
    public int visit(FunctionDeclaration node, State state) {
        visitFunction(node, state);
        return 0;
    }

    @Override
    public int visit(FunctionExpression node, State state) {
        visitFunction(node, state);
        return 10;
    }

    private void visitConciseFunction(FunctionNode node, Supplier<Expression> concise, State state) {
        int parameters = accept(node.getParameters(), new State(node));
        checkValidSize(parameters);
        int expression = accept(concise.get(), new State(node));
        checkValidSize(expression);
    }

    private void visitFunction(FunctionNode node, State state) {
        int parameters = accept(node.getParameters(), new State(node));
        checkValidSize(parameters);
        int statements = statementList(node::getStatements, new State(node), node::setStatements);
        checkValidSize(statements);
    }

    @Override
    public int visit(FunctionSent node, State state) {
        return 5;
    }

    @Override
    public int visit(GeneratorComprehension node, State state) {
        int comprehension = accept(node.getComprehension(), new State(node));
        checkValidSize(comprehension);
        return 10;
    }

    @Override
    public int visit(GeneratorDeclaration node, State state) {
        visitFunction(node, state);
        return 0;
    }

    @Override
    public int visit(GeneratorExpression node, State state) {
        visitFunction(node, state);
        return 10;
    }

    @Override
    public int visit(GuardedCatchNode node, State state) {
        int catchParameter = accept(node.getCatchParameter(), state);
        int guard = accept(node.getGuard(), state);
        int catchBlock = accept(node.getCatchBlock(), state);
        return 45 + catchParameter + guard + catchBlock;
    }

    @Override
    public int visit(IdentifierName node, State state) {
        return 5;
    }

    @Override
    public int visit(IdentifierReference node, State state) {
        return 10;
    }

    @Override
    public int visit(IfStatement node, State state) {
        int test = accept(node.getTest(), state);
        int then = statement(node.getThen(), state, node::setThen);
        int otherwise = node.getOtherwise() != null ? statement(node.getOtherwise(), state, node::setOtherwise) : 0;
        return 10 + test + then + otherwise;
    }

    @Override
    public int visit(ImportClause node, State state) {
        throw new IllegalStateException();
    }

    @Override
    public int visit(ImportDeclaration node, State state) {
        return 0;
    }

    @Override
    public int visit(ImportSpecifier node, State state) {
        throw new IllegalStateException();
    }

    @Override
    public int visit(LabelledFunctionStatement node, State state) {
        return accept(node.getFunction(), state);
    }

    @Override
    public int visit(LabelledStatement node, State state) {
        int statement = accept(node.getStatement(), state);
        return 15 + statement;
    }

    @Override
    public int visit(LegacyComprehension node, State state) {
        int list = accept(node.getList(), state);
        int expression = accept(node.getExpression(), state);
        return 50 + list + expression;
    }

    @Override
    public int visit(LegacyComprehensionFor node, State state) {
        int binding = accept(node.getBinding(), state);
        int expression = accept(node.getExpression(), state);
        return 40 + binding + expression;
    }

    @Override
    public int visit(LegacyGeneratorDeclaration node, State state) {
        visitFunction(node, state);
        return 0;
    }

    @Override
    public int visit(LegacyGeneratorExpression node, State state) {
        visitFunction(node, state);
        return 10;
    }

    @Override
    public int visit(LetExpression node, State state) {
        int bindings = accept(node.getBindings(), state);
        int expression = accept(node.getExpression(), state);
        return 25 + bindings + expression;
    }

    @Override
    public int visit(LetStatement node, State state) {
        int bindings = accept(node.getBindings(), state);
        int statements = accept(node.getStatement(), state);
        return 25 + bindings + statements;
    }

    @Override
    public int visit(LexicalBinding node, State state) {
        int binding = accept(node.getBinding(), state);
        int initializer = acceptIfPresent(node.getInitializer(), state);
        return 20 + binding + initializer;
    }

    @Override
    public int visit(LexicalDeclaration node, State state) {
        return accept(node.getElements(), state);
    }

    @Override
    public int visit(MethodDefinition node, State state) {
        visitFunction(node, state);
        int propertyName = accept(node.getPropertyName(), state);
        int decorators = accept(node.getDecorators(), state);
        return 10 + propertyName + decorators + listSize(node.getDecorators(), 10);
    }

    @Override
    public int visit(MethodDefinitionsMethod node, State state) {
        // Don't descend into synthetic nodes.
        return 10;
    }

    @Override
    public int visit(Module node, State state) {
        int statements = moduleItemList(node::getStatements, new State(node), node::setStatements);
        checkValidSize(statements);
        return statements;
    }

    @Override
    public int visit(NativeCallExpression node, State state) {
        int base = accept(node.getBase(), state);
        int arguments = accept(node.getArguments(), state);
        return 30 + base + arguments + listSize(node.getArguments(), 5);
    }

    @Override
    public int visit(NewExpression node, State state) {
        int expression = accept(node.getExpression(), state);
        int arguments = accept(node.getArguments(), state);
        return 15 + expression + arguments + listSize(node.getArguments(), 5);
    }

    @Override
    public int visit(NewTarget node, State state) {
        return 5;
    }

    @Override
    public int visit(NullLiteral node, State state) {
        return 5;
    }

    @Override
    public int visit(NumericLiteral node, State state) {
        return 5;
    }

    @Override
    public int visit(ObjectAssignmentPattern node, State state) {
        int properties = accept(node.getProperties(), state);
        int rest = acceptIfPresent(node.getRest(), state);
        return 20 + properties + rest + listSize(node.getProperties(), 5);
    }

    @Override
    public int visit(ObjectBindingPattern node, State state) {
        int properties = accept(node.getProperties(), state);
        int rest = acceptIfPresent(node.getRest(), state);
        return 20 + properties + rest + listSize(node.getProperties(), 5);
    }

    @Override
    public int visit(ObjectLiteral node, State state) {
        int properties = accept(node.getProperties(), state, PropertyDefinitionsMethod::new, node::setProperties,
                MAX_EXPRESSION_SIZE);
        if (properties > MAX_OBJECT_PROPERTIES_SIZE) {
            properties = split(node.getProperties(), state, PropertyDefinitionsMethod::new, node::setProperties,
                    MAX_OBJECT_PROPERTIES_SIZE);
        }
        return 15 + properties + listSize(node.getProperties(), 5);
    }

    @Override
    public int visit(PropertyAccessor node, State state) {
        int base = accept(node.getBase(), state);
        int propertyName = stringSize(node.getName());
        return 5 + base + propertyName;
    }

    @Override
    public int visit(PropertyDefinitionsMethod node, State state) {
        // Don't descend into synthetic nodes.
        return 10;
    }

    @Override
    public int visit(PropertyNameDefinition node, State state) {
        return 15;
    }

    @Override
    public int visit(PropertyValueDefinition node, State state) {
        int propertyName = accept(node.getPropertyName(), state);
        int propertyValue = accept(node.getPropertyValue(), state);
        return 5 + propertyName + propertyValue;
    }

    @Override
    public int visit(RegularExpressionLiteral node, State state) {
        return 10;
    }

    @Override
    public int visit(ReturnStatement node, State state) {
        int expression = acceptIfPresent(node.getExpression(), state);
        return 10 + expression;
    }

    @Override
    public int visit(Script node, State state) {
        int statements = statementList(node::getStatements, new State(node), node::setStatements);
        checkValidSize(statements);
        return statements;
    }

    @Override
    public int visit(SpreadArrayLiteral node, State state) {
        // Don't descend into synthetic nodes.
        return 10;
    }

    @Override
    public int visit(SpreadElement node, State state) {
        int expression = accept(node.getExpression(), state);
        return 10 + expression;
    }

    @Override
    public int visit(SpreadElementMethod node, State state) {
        // Don't descend into synthetic nodes.
        return 10;
    }

    @Override
    public int visit(SpreadProperty node, State state) {
        int expression = accept(node.getExpression(), state);
        return 10 + expression;
    }

    @Override
    public int visit(StatementListMethod node, State state) {
        // Don't descend into synthetic nodes.
        return 25;
    }

    @Override
    public int visit(StringLiteral node, State state) {
        return stringSize(node.getValue());
    }

    @Override
    public int visit(SuperCallExpression node, State state) {
        int arguments = accept(node.getArguments(), state);
        return 25 + arguments + listSize(node.getArguments(), 5);
    }

    @Override
    public int visit(SuperElementAccessor node, State state) {
        int element = accept(node.getElement(), state);
        return 15 + element;
    }

    @Override
    public int visit(SuperNewExpression node, State state) {
        int arguments = accept(node.getArguments(), state);
        return 25 + arguments + listSize(node.getArguments(), 5);
    }

    @Override
    public int visit(SuperPropertyAccessor node, State state) {
        int propertyName = stringSize(node.getName());
        return 10 + propertyName;
    }

    @Override
    public int visit(SwitchClause node, State state) {
        int expression = acceptIfPresent(node.getExpression(), state);
        int statements = statementList(node::getStatements, state, node::setStatements, MAX_STATEMENT_SIZE);
        return 15 + expression + statements;
    }

    @Override
    public int visit(SwitchStatement node, State state) {
        // TODO: Doesn't take optimized switches (int,char,string) into account.
        int expression = accept(node.getExpression(), state);
        int clauses = accept(node.getClauses(), state);
        return 100 + expression + clauses;
    }

    @Override
    public int visit(TemplateCallExpression node, State state) {
        int base = accept(node.getBase(), state);
        int template = accept(node.getTemplate(), state);
        return 30 + base + template;
    }

    @Override
    public int visit(TemplateCharacters node, State state) {
        return stringSize(node.getValue()) + stringSize(node.getRawValue());
    }

    @Override
    public int visit(TemplateLiteral node, State state) {
        int elements = accept(node.getElements(), state, ExpressionMethod::new, node::setElements, MAX_EXPRESSION_SIZE);
        if (elements > MAX_TEMPLATE_ELEMENTS_SIZE) {
            elements = split(node.getElements(), state, list -> {
                assert !list.isEmpty();
                long beginPosition = list.get(0).getBeginPosition();
                long endPosition = list.get(list.size() - 1).getEndPosition();
                return new ExpressionMethod(new TemplateLiteral(beginPosition, endPosition, false, list));
            } , node::setElements, MAX_TEMPLATE_ELEMENTS_SIZE);
        }
        return 25 + elements + listSize(node.getElements(), 10);
    }

    @Override
    public int visit(ThisExpression node, State state) {
        return 10;
    }

    @Override
    public int visit(ThrowStatement node, State state) {
        int expression = accept(node.getExpression(), state);
        return 10 + expression;
    }

    @Override
    public int visit(TryStatement node, State state) {
        int tryBlock = accept(node.getTryBlock(), state);
        int catchNode = acceptIfPresent(node.getCatchNode(), state);
        int guardedNodes = accept(node.getGuardedCatchNodes(), state);
        int finallyBlock = acceptIfPresent(node.getFinallyBlock(), state);
        return 40 + tryBlock + catchNode + guardedNodes + finallyBlock + listSize(node.getGuardedCatchNodes(), 20);
    }

    @Override
    public int visit(UnaryExpression node, State state) {
        int operand = accept(node.getOperand(), state);
        return 15 + operand;
    }

    @Override
    public int visit(VariableDeclaration node, State state) {
        int binding = accept(node.getBinding(), state);
        int initializer = acceptIfPresent(node.getInitializer(), state);
        return 15 + binding + initializer;
    }

    @Override
    public int visit(VariableStatement node, State state) {
        return accept(node.getElements(), state);
    }

    @Override
    public int visit(WhileStatement node, State state) {
        int test = accept(node.getTest(), state);
        int statement = statement(node.getStatement(), state, node::setStatement);
        return 25 + test + statement;
    }

    @Override
    public int visit(WithStatement node, State state) {
        int expression = accept(node.getExpression(), state);
        int statement = statement(node.getStatement(), state, node::setStatement);
        return 25 + expression + statement;
    }

    @Override
    public int visit(YieldExpression node, State state) {
        int expression = acceptIfPresent(node.getExpression(), state);
        if (node.isDelegatedYield()) {
            return 300 + expression;
        }
        return 150 + expression;
    }
}

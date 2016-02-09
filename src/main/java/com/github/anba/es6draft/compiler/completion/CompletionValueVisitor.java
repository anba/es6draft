/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.completion;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.compiler.completion.CompletionValueVisitor.Completion;
import com.github.anba.es6draft.compiler.completion.CompletionValueVisitor.State;

/**
 * Compute statement nodes which require a completion value.
 */
public final class CompletionValueVisitor extends DefaultNodeVisitor<Completion, State> {
    // TODO: This visitor class performs (more or less) live variable analysis. Or rather, with
    // proper live variable analysis the visitor shouldn't be necessary at all.

    enum Completion {
        Empty, Value;

        Completion then(Completion c) {
            return (this == Value || c == Value) ? Value : Empty;
        }
    }

    private static final class Context {
        // unlabelled breaks and continues
        private final ArrayDeque<BreakableStatement> breakTargets = new ArrayDeque<>();
        private final ArrayDeque<IterationStatement> continueTargets = new ArrayDeque<>();

        // labelled breaks and continues
        private final HashMap<String, Statement> namedBreakLabels = new HashMap<>();
        private final HashMap<String, IterationStatement> namedContinueLabels = new HashMap<>();

        void enterIteration(IterationStatement node) {
            boolean hasBreak = node.hasBreak();
            boolean hasContinue = node.hasContinue();
            if (hasBreak || hasContinue) {
                if (hasBreak)
                    breakTargets.push(node);
                if (hasContinue)
                    continueTargets.push(node);
                for (String label : node.getLabelSet()) {
                    if (hasBreak)
                        namedBreakLabels.put(label, node);
                    if (hasContinue)
                        namedContinueLabels.put(label, node);
                }
            }
        }

        void exitIteration(IterationStatement node) {
            boolean hasBreak = node.hasBreak();
            boolean hasContinue = node.hasContinue();
            if (hasBreak || hasContinue) {
                if (hasBreak)
                    breakTargets.pop();
                if (hasContinue)
                    continueTargets.pop();
                for (String label : node.getLabelSet()) {
                    if (hasBreak)
                        namedBreakLabels.remove(label);
                    if (hasContinue)
                        namedContinueLabels.remove(label);
                }
            }
        }

        void enterBreakable(BreakableStatement node) {
            if (node.hasBreak()) {
                breakTargets.push(node);
                for (String label : node.getLabelSet()) {
                    namedBreakLabels.put(label, node);
                }
            }
        }

        void exitBreakable(BreakableStatement node) {
            if (node.hasBreak()) {
                breakTargets.pop();
                for (String label : node.getLabelSet()) {
                    namedBreakLabels.remove(label);
                }
            }
        }

        void enterLabelled(LabelledStatement node) {
            if (node.hasBreak()) {
                for (String label : node.getLabelSet()) {
                    namedBreakLabels.put(label, node);
                }
            }
        }

        void exitLabelled(LabelledStatement node) {
            if (node.hasBreak()) {
                for (String label : node.getLabelSet()) {
                    namedBreakLabels.remove(label);
                }
            }
        }

        Statement breakTarget(BreakStatement node) {
            String name = node.getLabel();
            return name == null ? breakTargets.peek() : namedBreakLabels.get(name);
        }

        IterationStatement continueTarget(ContinueStatement node) {
            String name = node.getLabel();
            return name == null ? continueTargets.peek() : namedContinueLabels.get(name);
        }
    }

    static final class State {
        final Context context;
        boolean computeValue = true;

        State() {
            context = new Context();
            computeValue = true;
        }

        private State(State state) {
            context = state.context;
            computeValue = state.computeValue;
        }

        void valueStatement() {
            computeValue = false;
        }

        State newState() {
            return new State(this);
        }

        void merge(State left, State right) {
            computeValue = left.computeValue || right.computeValue;
        }
    }

    private static <T> Iterable<T> reverse(List<T> list) {
        return () -> new Iterator<T>() {
            final ListIterator<T> iter = list.listIterator(list.size());

            @Override
            public boolean hasNext() {
                return iter.hasPrevious();
            }

            @Override
            public T next() {
                return iter.previous();
            }
        };
    }

    private static final CompletionValueVisitor INSTANCE = new CompletionValueVisitor();

    public static void performCompletion(Script script) {
        script.accept(INSTANCE, new State());
    }

    public static void performCompletion(Module module) {
        module.accept(INSTANCE, new State());
    }

    @Override
    protected Completion visit(Node node, State state) {
        throw new IllegalStateException();
    }

    public <STATEMENT extends ModuleItem> Completion statements(List<STATEMENT> statements,
            State state) {
        Completion result = Completion.Empty;
        for (STATEMENT statement : reverse(statements)) {
            result = statement.accept(this, state).then(result);
        }
        return result;
    }

    @Override
    public Completion visit(Script node, State state) {
        return statements(node.getStatements(), state);
    }

    @Override
    public Completion visit(Module node, State state) {
        return statements(node.getStatements(), state);
    }

    @Override
    public Completion visit(BlockStatement node, State state) {
        node.setCompletionValue(state.computeValue);
        return statements(node.getStatements(), state);
    }

    @Override
    public Completion visit(ExpressionStatement node, State state) {
        node.setCompletionValue(state.computeValue);
        state.valueStatement();
        return Completion.Value;
    }

    @Override
    public Completion visit(IfStatement node, State state) {
        node.setCompletionValue(state.computeValue);
        if (node.getOtherwise() == null) {
            Completion thenResult = node.getThen().accept(this, state);
            if (thenResult == Completion.Empty) {
                state.valueStatement();
            }
            return Completion.Value;
        }
        State thenState = state.newState();
        Completion thenResult = node.getThen().accept(this, thenState);
        if (thenResult == Completion.Empty) {
            thenState.valueStatement();
        }
        State otherwiseState = state.newState();
        Completion otherwiseResult = node.getOtherwise().accept(this, otherwiseState);
        if (otherwiseResult == Completion.Empty) {
            otherwiseState.valueStatement();
        }
        state.merge(thenState, otherwiseState);
        return Completion.Value;
    }

    @Override
    public Completion visit(BreakStatement node, State state) {
        node.setCompletionValue(state.computeValue);
        Statement target = state.context.breakTarget(node);
        if (target.hasCompletionValue()) {
            state.computeValue = true;
        }
        return Completion.Value;
    }

    @Override
    public Completion visit(ContinueStatement node, State state) {
        node.setCompletionValue(state.computeValue);
        IterationStatement target = state.context.continueTarget(node);
        if (target.hasCompletionValue()) {
            state.computeValue = true;
        }
        return Completion.Value;
    }

    @Override
    public Completion visit(LabelledStatement node, State state) {
        node.setCompletionValue(state.computeValue);
        state.context.enterLabelled(node);
        Completion result = node.getStatement().accept(this, state);
        state.context.exitLabelled(node);
        return result;
    }

    @Override
    public Completion visit(DoWhileStatement node, State state) {
        return visitIteration(node, node.getStatement(), state);
    }

    @Override
    public Completion visit(ForOfStatement node, State state) {
        return visitIteration(node, node.getStatement(), state);
    }

    @Override
    public Completion visit(ForEachStatement node, State state) {
        return visitIteration(node, node.getStatement(), state);
    }

    @Override
    public Completion visit(ForInStatement node, State state) {
        return visitIteration(node, node.getStatement(), state);
    }

    @Override
    public Completion visit(ForStatement node, State state) {
        return visitIteration(node, node.getStatement(), state);
    }

    @Override
    public Completion visit(WhileStatement node, State state) {
        return visitIteration(node, node.getStatement(), state);
    }

    private Completion visitIteration(IterationStatement iteration, Statement inner, State state) {
        iteration.setCompletionValue(state.computeValue);
        state.context.enterIteration(iteration);
        inner.accept(this, state);
        state.context.exitIteration(iteration);
        state.valueStatement();
        return Completion.Value;
    }

    @Override
    public Completion visit(WithStatement node, State state) {
        node.setCompletionValue(state.computeValue);
        Completion result = node.getStatement().accept(this, state);
        if (result == Completion.Empty) {
            state.valueStatement();
            result = Completion.Value;
        }
        return result;
    }

    @Override
    public Completion visit(ThrowStatement node, State state) {
        state.valueStatement();
        return Completion.Value;
    }

    @Override
    public Completion visit(TryStatement node, State state) {
        final boolean computeValue = state.computeValue;
        node.setCompletionValue(computeValue);
        node.getTryBlock().accept(this, state);
        if (node.getCatchNode() != null) {
            state.computeValue = computeValue;
            node.getCatchNode().getCatchBlock().accept(this, state);
        }
        for (GuardedCatchNode guardedCatchNode : node.getGuardedCatchNodes()) {
            state.computeValue = computeValue;
            guardedCatchNode.getCatchBlock().accept(this, state);
        }
        if (node.getFinallyBlock() != null) {
            state.computeValue = computeValue;
            node.getFinallyBlock().accept(this, state);
        }
        state.valueStatement();
        return Completion.Value;
    }

    @Override
    public Completion visit(SwitchStatement node, State state) {
        final boolean computeValue = state.computeValue;
        node.setCompletionValue(computeValue);
        state.context.enterBreakable(node);
        for (SwitchClause clause : node.getClauses()) {
            state.computeValue = computeValue;
            statements(clause.getStatements(), state);
        }
        state.context.exitBreakable(node);
        state.valueStatement();
        return Completion.Value;
    }

    @Override
    public Completion visit(LetStatement node, State state) {
        return node.getStatement().accept(this, state);
    }

    @Override
    public Completion visit(VariableStatement node, State state) {
        return Completion.Empty;
    }

    @Override
    protected Completion visit(Declaration node, State state) {
        return Completion.Empty;
    }

    @Override
    public Completion visit(ExportDeclaration node, State value) {
        return Completion.Empty;
    }

    @Override
    public Completion visit(ImportDeclaration node, State value) {
        return Completion.Empty;
    }

    @Override
    public Completion visit(EmptyStatement node, State state) {
        return Completion.Empty;
    }

    @Override
    public Completion visit(DebuggerStatement node, State state) {
        return Completion.Empty;
    }

    @Override
    public Completion visit(LabelledFunctionStatement node, State value) {
        return Completion.Empty;
    }
}

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

        void valueStatement() {
            computeValue = false;
        }

        void enter(Statement node) {
            node.setCompletionValue(computeValue);
        }

        void exit(Statement node) {
            node.setCompletionValue(computeValue);
        }

        void exit(Statement node, boolean computeValue) {
            node.setCompletionValue(computeValue);
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

    public <STATEMENT extends ModuleItem> Completion statements(List<STATEMENT> statements, State state) {
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
        state.enter(node);
        Completion result = statements(node.getStatements(), state);
        state.exit(node);
        return result;
    }

    @Override
    public Completion visit(ExpressionStatement node, State state) {
        state.enter(node);
        state.exit(node);
        state.valueStatement();
        return Completion.Value;
    }

    @Override
    public Completion visit(IfStatement node, State state) {
        final boolean computeValue = state.computeValue;
        boolean innerComputeValue = false;

        state.enter(node);
        node.getThen().accept(this, state);
        innerComputeValue |= state.computeValue;
        if (node.getOtherwise() != null) {
            state.computeValue = computeValue;
            node.getOtherwise().accept(this, state);
            innerComputeValue |= state.computeValue;
        }
        state.exit(node, innerComputeValue);
        state.valueStatement();
        return Completion.Value;
    }

    @Override
    public Completion visit(BreakStatement node, State state) {
        state.enter(node);
        Statement target = state.context.breakTarget(node);
        state.exit(node);
        if (target.hasCompletionValue()) {
            state.computeValue = true;
        }
        return Completion.Value;
    }

    @Override
    public Completion visit(ContinueStatement node, State state) {
        state.enter(node);
        IterationStatement target = state.context.continueTarget(node);
        state.exit(node);
        if (target.hasCompletionValue()) {
            state.computeValue = true;
        }
        return Completion.Value;
    }

    @Override
    public Completion visit(LabelledStatement node, State state) {
        state.enter(node);
        state.context.enterLabelled(node);
        Completion result = node.getStatement().accept(this, state);
        state.context.exitLabelled(node);
        state.exit(node);
        return result;
    }

    @Override
    protected Completion visit(IterationStatement node, State state) {
        state.enter(node);
        state.context.enterIteration(node);
        node.getStatement().accept(this, state);
        state.context.exitIteration(node);
        state.exit(node);
        state.valueStatement();
        return Completion.Value;
    }

    @Override
    public Completion visit(WithStatement node, State state) {
        state.enter(node);
        node.getStatement().accept(this, state);
        state.exit(node);
        state.valueStatement();
        return Completion.Value;
    }

    @Override
    public Completion visit(ThrowStatement node, State state) {
        state.valueStatement();
        return Completion.Value;
    }

    @Override
    public Completion visit(TryStatement node, State state) {
        final boolean computeValue = state.computeValue;
        boolean innerComputeValue = false;

        state.enter(node);
        node.getTryBlock().accept(this, state);
        innerComputeValue |= state.computeValue;
        if (node.getCatchNode() != null) {
            state.computeValue = computeValue;
            node.getCatchNode().getCatchBlock().accept(this, state);
            innerComputeValue |= state.computeValue;
        }
        for (GuardedCatchNode guardedCatchNode : node.getGuardedCatchNodes()) {
            state.computeValue = computeValue;
            guardedCatchNode.getCatchBlock().accept(this, state);
            innerComputeValue |= state.computeValue;
        }
        if (node.getFinallyBlock() != null) {
            state.computeValue = computeValue;
            node.getFinallyBlock().accept(this, state);
            innerComputeValue |= state.computeValue;
        }
        // Always request completion value when 'computeValue' is true and the finally block is present.
        state.exit(node, innerComputeValue || (computeValue && node.getFinallyBlock() != null));
        state.valueStatement();
        return Completion.Value;
    }

    @Override
    public Completion visit(SwitchStatement node, State state) {
        final boolean computeValue = state.computeValue;
        boolean innerComputeValue = false;

        state.enter(node);
        state.context.enterBreakable(node);
        for (SwitchClause clause : node.getClauses()) {
            state.computeValue = computeValue;
            statements(clause.getStatements(), state);
            innerComputeValue |= state.computeValue;
        }
        state.context.exitBreakable(node);
        // Always request completion value when 'computeValue' is true and no catch clauses are present.
        state.exit(node, innerComputeValue || (computeValue && node.getClauses().isEmpty()));
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

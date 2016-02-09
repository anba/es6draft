/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.Labels.BreakLabel;
import com.github.anba.es6draft.compiler.Labels.ContinueLabel;
import com.github.anba.es6draft.compiler.Labels.ReturnLabel;
import com.github.anba.es6draft.compiler.Labels.TempLabel;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.Variable;

/**
 * 
 */
abstract class StatementVisitor extends ExpressionVisitor {
    private static final class Labels {
        // unlabelled breaks and continues
        final ArrayDeque<Jump> breakTargets = new ArrayDeque<>(4);
        final ArrayDeque<Jump> continueTargets = new ArrayDeque<>(4);

        // labelled breaks and continues
        final HashMap<String, Jump> namedBreakLabels = new HashMap<>(4);
        final HashMap<String, Jump> namedContinueLabels = new HashMap<>(4);

        Jump returnLabel = null;

        // label after last catch node
        final ArrayDeque<Jump> catchLabels = new ArrayDeque<>(4);

        // temporary labels in try-catch-finally blocks
        ArrayList<TempLabel> tempLabels = null;

        final Labels parent;
        final Variable<Object> completion;

        Labels(Labels parent, Variable<Object> completion) {
            this.parent = parent;
            this.completion = completion;
        }

        private TempLabel newTemp(Jump actual) {
            assert actual != null;
            TempLabel temp = new TempLabel(actual);
            if (tempLabels == null) {
                tempLabels = new ArrayList<>(4);
            }
            tempLabels.add(temp);
            return temp;
        }

        private Jump getBreakLabel(String name) {
            return name == null ? breakTargets.peek() : namedBreakLabels.get(name);
        }

        private void setBreakLabel(String name, Jump label) {
            if (name == null) {
                assert breakTargets.isEmpty();
                breakTargets.push(label);
            } else {
                namedBreakLabels.put(name, label);
            }
        }

        private Jump getContinueLabel(String name) {
            return name == null ? continueTargets.peek() : namedContinueLabels.get(name);
        }

        private void setContinueLabel(String name, Jump label) {
            if (name == null) {
                assert continueTargets.isEmpty();
                continueTargets.push(label);
            } else {
                namedContinueLabels.put(name, label);
            }
        }

        Jump returnLabel() {
            Jump lbl = returnLabel;
            if (lbl == null) {
                assert parent != null : "return outside of function";
                lbl = newTemp(parent.returnLabel());
                returnLabel = lbl;
            }
            return lbl;
        }

        Jump breakLabel(String name) {
            Jump label = getBreakLabel(name);
            if (label == null) {
                assert parent != null : "Label not found: " + name;
                label = newTemp(parent.breakLabel(name));
                setBreakLabel(name, label);
            }
            return label;
        }

        Jump continueLabel(String name) {
            Jump label = getContinueLabel(name);
            if (label == null) {
                assert parent != null : "Label not found: " + name;
                label = newTemp(parent.continueLabel(name));
                setContinueLabel(name, label);
            }
            return label;
        }
    }

    private static final int COMPLETION_SLOT = 1;

    private final boolean isFunction;
    private final boolean isGeneratorOrAsync;
    private Variable<Object> completionValue;
    private Labels labels = new Labels(null, null);

    // tail-call support
    private int wrapped = 0;

    protected StatementVisitor(MethodCode method, StatementVisitor parent) {
        super(method, parent);
        this.isFunction = parent.isFunction;
        this.isGeneratorOrAsync = parent.isGeneratorOrAsync;
        // no return in script/module code
        this.labels.returnLabel = isFunction ? new ReturnLabel() : null;
        this.wrapped = parent.wrapped;
    }

    protected StatementVisitor(MethodCode method, TopLevelNode<?> topLevelNode, boolean strict) {
        super(method, topLevelNode, strict);
        boolean isFunction = topLevelNode instanceof FunctionNode;
        this.isFunction = isFunction;
        this.isGeneratorOrAsync = isFunction && isGeneratorOrAsync(topLevelNode);
        // no return in script/module code
        this.labels.returnLabel = isFunction ? new ReturnLabel() : null;
    }

    private static boolean isGeneratorOrAsync(TopLevelNode<?> node) {
        assert node instanceof FunctionNode;
        return ((FunctionNode) node).isGenerator() || ((FunctionNode) node).isAsync();
    }

    @Override
    public void begin() {
        super.begin();
        if (!isFunction) {
            completionValue = hasParameter(COMPLETION_SLOT, Object.class) ? getParameter(
                    COMPLETION_SLOT, Object.class) : newVariable("completion", Object.class);
        }
    }

    final boolean isFunction() {
        return isFunction;
    }

    @Override
    final boolean isGeneratorOrAsync() {
        return isGeneratorOrAsync;
    }

    final Variable<Object> completionVariable() {
        if (isAbruptRegion()) {
            return labels.completion;
        }
        return completionValue;
    }

    /**
     * Pushes the completion value onto the stack.
     */
    final void loadCompletionValue() {
        if (hasCompletion()) {
            load(completionVariable());
        } else {
            aconst(null);
        }
    }

    /**
     * Pops the stack's top element and stores it into the completion value.
     * 
     * @param type
     *            the value type of top stack value
     */
    final void storeCompletionValue(ValType type) {
        if (hasCompletion()) {
            toBoxed(type);
            store(completionVariable());
        } else {
            pop(type);
        }
    }

    final void storeCompletionValue(Variable<Object> completionValue) {
        if (hasCompletion()) {
            load(completionValue);
            store(completionVariable());
        }
    }

    final void storeUndefinedAsCompletionValue() {
        if (hasCompletion()) {
            loadUndefined();
            store(completionVariable());
        }
    }

    final boolean hasCompletion() {
        return !isFunction;
    }

    @Override
    void enterTailCallPosition(Expression expr) {
        if (!isWrapped() && !isGeneratorOrAsync()) {
            assert isFunction;
            super.enterTailCallPosition(expr);
        }
    }

    /**
     * Returns <code>true</code> when currently within a finally scoped block.
     * 
     * @return <code>true</code> if currently in a finally block
     */
    private boolean isAbruptRegion() {
        return labels.parent != null;
    }

    private Variable<Object> enterAbruptRegion(boolean statementCompletion) {
        assert labels != null;
        Variable<Object> completion;
        if (hasCompletion() && !statementCompletion) {
            // Re-use the existing completion variable if the statement doesn't track its completion state.
            completion = completionVariable();
        } else {
            completion = newVariable("completion", Object.class);
            loadCompletionValue();
            store(completion);
        }
        labels = new Labels(labels, completion);
        return completion;
    }

    private List<TempLabel> exitAbruptRegion() {
        assert isAbruptRegion();
        ArrayList<TempLabel> tempLabels = labels.tempLabels;
        labels = labels.parent;
        return tempLabels != null ? tempLabels : Collections.<TempLabel> emptyList();
    }

    @Override
    Variable<Object> enterIteration() {
        if (isGeneratorOrAsync()) {
            return enterAbruptRegion(false);
        }
        return super.enterIteration();
    }

    @Override
    List<TempLabel> exitIteration() {
        if (isGeneratorOrAsync()) {
            return exitAbruptRegion();
        }
        return super.exitIteration();
    }

    /**
     * Enter an iteration body block.
     * 
     * @param <FORSTATEMENT>
     *            the for-statement node type
     * @param node
     *            the iteration statement
     * @return the temporary completion object variable
     */
    <FORSTATEMENT extends IterationStatement & ForIterationNode> Variable<Object> enterIterationBody(
            FORSTATEMENT node) {
        Variable<Object> completion = enterAbruptRegion(false);
        if (node.hasContinue()) {
            // Copy current continue labels from parent to new labelset, so we won't create
            // temp-labels for own continue labels.
            Labels labels = this.labels;
            assert !labels.parent.continueTargets.isEmpty();
            Jump lblContinue = labels.parent.continueTargets.peek();
            assert lblContinue instanceof ContinueLabel;
            labels.continueTargets.push(lblContinue);
            for (String label : node.getLabelSet()) {
                labels.namedContinueLabels.put(label, lblContinue);
            }
        }
        return completion;
    }

    /**
     * Exit an iteration body block.
     * 
     * @param <FORSTATEMENT>
     *            the for-statement node type
     * @param node
     *            the iteration statement
     * @return the list of generated labels
     */
    <FORSTATEMENT extends IterationStatement & ForIterationNode> List<TempLabel> exitIterationBody(
            FORSTATEMENT node) {
        return exitAbruptRegion();
    }

    /**
     * Enter a finally scoped block.
     * 
     * @param node
     *            the try-statement
     * @return the temporary completion object variable
     */
    Variable<Object> enterFinallyScoped(TryStatement node) {
        return enterAbruptRegion(node.hasCompletionValue());
    }

    /**
     * Exit a finally scoped block.
     * 
     * @return the list of generated labels
     */
    List<TempLabel> exitFinallyScoped() {
        return exitAbruptRegion();
    }

    /**
     * Returns <code>true</code> when currently within a wrapped block.
     * 
     * @return <code>true</code> if currently wrapped
     */
    private boolean isWrapped() {
        return wrapped != 0;
    }

    /**
     * Enter a try-catch-finally wrapped block.
     */
    void enterWrapped() {
        ++wrapped;
    }

    /**
     * Exit a try-catch-finally wrapped block.
     */
    void exitWrapped() {
        --wrapped;
    }

    /**
     * Start code generation for {@link IterationStatement} nodes.
     * 
     * @param node
     *            the iteration statement
     * @param lblBreak
     *            the break label for the statement
     * @param lblContinue
     *            the continue label for the statement
     */
    void enterIteration(IterationStatement node, BreakLabel lblBreak, ContinueLabel lblContinue) {
        boolean hasBreak = node.hasBreak();
        boolean hasContinue = node.hasContinue();
        if (!(hasBreak || hasContinue))
            return;
        Labels labels = this.labels;
        if (hasBreak)
            labels.breakTargets.push(lblBreak);
        if (hasContinue)
            labels.continueTargets.push(lblContinue);
        for (String label : node.getLabelSet()) {
            if (hasBreak)
                labels.namedBreakLabels.put(label, lblBreak);
            if (hasContinue)
                labels.namedContinueLabels.put(label, lblContinue);
        }
    }

    /**
     * Stop code generation for {@link IterationStatement} nodes.
     * 
     * @param node
     *            the iteration statement
     */
    void exitIteration(IterationStatement node) {
        boolean hasBreak = node.hasBreak();
        boolean hasContinue = node.hasContinue();
        if (!(hasBreak || hasContinue))
            return;
        Labels labels = this.labels;
        if (hasBreak)
            labels.breakTargets.pop();
        if (hasContinue)
            labels.continueTargets.pop();
        for (String label : node.getLabelSet()) {
            if (hasBreak)
                labels.namedBreakLabels.remove(label);
            if (hasContinue)
                labels.namedContinueLabels.remove(label);
        }
    }

    /**
     * Start code generation for {@link BreakableStatement} nodes.
     * 
     * @param node
     *            the breakable statement
     * @param lblBreak
     *            the break label for the statement
     */
    void enterBreakable(BreakableStatement node, BreakLabel lblBreak) {
        if (!node.hasBreak())
            return;
        Labels labels = this.labels;
        labels.breakTargets.push(lblBreak);
        for (String label : node.getLabelSet()) {
            labels.namedBreakLabels.put(label, lblBreak);
        }
    }

    /**
     * Stop code generation for {@link BreakableStatement} nodes.
     * 
     * @param node
     *            the breakable statement
     */
    void exitBreakable(BreakableStatement node) {
        if (!node.hasBreak())
            return;
        Labels labels = this.labels;
        labels.breakTargets.pop();
        for (String label : node.getLabelSet()) {
            labels.namedBreakLabels.remove(label);
        }
    }

    /**
     * Start code generation for {@link LabelledStatement} nodes.
     * 
     * @param node
     *            the labelled statement
     * @param lblBreak
     *            the break label for the statement
     */
    void enterLabelled(LabelledStatement node, BreakLabel lblBreak) {
        if (!node.hasBreak())
            return;
        Labels labels = this.labels;
        for (String label : node.getLabelSet()) {
            labels.namedBreakLabels.put(label, lblBreak);
        }
    }

    /**
     * Stop code generation for {@link LabelledStatement} nodes.
     * 
     * @param node
     *            the labelled statement
     */
    void exitLabelled(LabelledStatement node) {
        if (!node.hasBreak())
            return;
        Labels labels = this.labels;
        for (String label : node.getLabelSet()) {
            labels.namedBreakLabels.remove(label);
        }
    }

    /**
     * Start code generation for {@link TryStatement} nodes with {@link GuardedCatchNode}
     * 
     * @param node
     *            the try-statement node
     * @param catchLabel
     *            the catch label
     */
    void enterCatchWithGuarded(TryStatement node, Jump catchLabel) {
        labels.catchLabels.push(catchLabel);
    }

    /**
     * Stop code generation for {@link TryStatement} nodes with {@link GuardedCatchNode}.
     * 
     * @param node
     *            the try-statement node
     */
    void exitCatchWithGuarded(TryStatement node) {
        labels.catchLabels.pop();
    }

    @Override
    void returnCompletion() {
        if (isAbruptRegion()) {
            // If currently enclosed by a finally scoped block, store value in completion register
            // and jump to (temporary) return label
            store(labels.completion);
            goTo(returnLabel());
        } else {
            // Otherwise emit direct return instruction
            _return();
        }
    }

    /**
     * Returns the current return-label.
     * 
     * @return the return label
     */
    private Jump returnLabel() {
        return labels.returnLabel();
    }

    /**
     * Returns the break-label for the given {@link BreakStatement}.
     * 
     * @param node
     *            the break statement
     * @return the label to the target instruction
     */
    Jump breakLabel(BreakStatement node) {
        String name = node.getLabel();
        return labels.breakLabel(name);
    }

    /**
     * Returns the continue-label for the given {@link ContinueStatement}.
     * 
     * @param node
     *            the continue statement
     * @return the label to the target instruction
     */
    Jump continueLabel(ContinueStatement node) {
        String name = node.getLabel();
        return labels.continueLabel(name);
    }

    /**
     * Returns the catch-label originally set in {@link #enterCatchWithGuarded(TryStatement, Jump)}
     * .
     * 
     * @return the guarded catch label
     */
    Jump catchWithGuardedLabel() {
        return labels.catchLabels.peek();
    }
}

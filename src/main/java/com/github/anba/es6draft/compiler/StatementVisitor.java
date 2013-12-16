/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;

import com.github.anba.es6draft.ast.AbruptNode.Abrupt;
import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.compiler.Code.MethodCode;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.JumpLabel.BreakLabel;
import com.github.anba.es6draft.compiler.JumpLabel.ContinueLabel;
import com.github.anba.es6draft.compiler.JumpLabel.ReturnLabel;
import com.github.anba.es6draft.compiler.JumpLabel.TempLabel;

/**
 * 
 */
abstract class StatementVisitor extends ExpressionVisitor {
    private static final class Labels {
        // unlabelled breaks and continues
        final Deque<JumpLabel> breakTargets = new ArrayDeque<>(4);
        final Deque<JumpLabel> continueTargets = new ArrayDeque<>(4);

        // labelled breaks and continues
        final Map<String, JumpLabel> namedBreakLabels = new HashMap<>(4);
        final Map<String, JumpLabel> namedContinueLabels = new HashMap<>(4);

        JumpLabel returnLabel = null;

        // label after last catch node
        final Deque<Label> catchLabels = new ArrayDeque<>(4);

        // temporary labels in try-catch-finally blocks
        List<TempLabel> tempLabels = null;

        final Labels parent;

        Labels(Labels parent) {
            this.parent = parent;
        }

        TempLabel newTemp(JumpLabel actual) {
            assert actual != null;
            TempLabel temp = new TempLabel(actual);
            if (tempLabels == null) {
                tempLabels = new ArrayList<>(4);
            }
            tempLabels.add(temp);
            return temp;
        }

        JumpLabel returnLabel() {
            JumpLabel lbl = returnLabel;
            if (lbl == null) {
                assert parent != null;
                lbl = newTemp(parent.returnLabel());
                returnLabel = lbl;
            }
            return lbl;
        }

        JumpLabel breakLabel(String name) {
            JumpLabel label;
            if (name == null) {
                label = breakTargets.peek();
            } else {
                label = namedBreakLabels.get(name);
            }
            if (label == null) {
                assert parent != null;
                label = newTemp(parent.breakLabel(name));
                if (name == null) {
                    assert breakTargets.isEmpty();
                    breakTargets.push(label);
                } else {
                    namedBreakLabels.put(name, label);
                }
            }
            return label;
        }

        JumpLabel continueLabel(String name) {
            JumpLabel label;
            if (name == null) {
                label = continueTargets.peek();
            } else {
                label = namedContinueLabels.get(name);
            }
            if (label == null) {
                assert parent != null;
                label = newTemp(parent.continueLabel(name));
                if (name == null) {
                    assert continueTargets.isEmpty();
                    continueTargets.push(label);
                } else {
                    namedContinueLabels.put(name, label);
                }
            }
            return label;
        }
    }

    enum CodeType {
        GlobalScript, NonGlobalScript, Function,
    }

    private static final int COMPLETION_SLOT = 1;

    private final TopLevelNode topLevelNode;
    private final CodeType codeType;
    private final boolean isScriptCode;
    private final boolean isGenerator;
    private Variable<Object> completionValue;
    private Labels labels = new Labels(null);
    private int finallyDepth = 0;

    // tail-call support
    private int wrapped = 0;

    protected StatementVisitor(MethodCode method, boolean strict, TopLevelNode topLevelNode,
            CodeType codeType) {
        super(method, strict, codeType == CodeType.GlobalScript);
        this.topLevelNode = topLevelNode;
        this.codeType = codeType;
        this.isScriptCode = codeType != CodeType.Function;
        this.isGenerator = codeType == CodeType.Function && isGeneratorNode(topLevelNode);
        // no return in script code
        this.labels.returnLabel = codeType == CodeType.Function ? new ReturnLabel() : null;
    }

    private static boolean isGeneratorNode(TopLevelNode node) {
        assert node instanceof FunctionNode;
        return ((FunctionNode) node).isGenerator();
    }

    @Override
    public void begin() {
        super.begin();
        completionValue = hasParameter(COMPLETION_SLOT, Object.class) ? getParameter(
                COMPLETION_SLOT, Object.class) : reserveFixedSlot("completion", COMPLETION_SLOT,
                Object.class);
    }

    /**
     * Returns the {@link TopLevelNode} for this statement visitor
     */
    TopLevelNode getTopLevelNode() {
        return topLevelNode;
    }

    /**
     * Returns the {@link CodeType} for this statement visitor
     */
    CodeType getCodeType() {
        return codeType;
    }

    /**
     * Pushes the completion value onto the stack
     */
    void loadCompletionValue() {
        load(completionValue);
    }

    /**
     * Pops the stack's top element and stores it into the completion value
     */
    void storeCompletionValue() {
        store(completionValue);
    }

    /**
     * Pops the stack's top element and conditionally stores it into the completion value.
     */
    void storeCompletionValueForScript(ValType type) {
        if (isScriptCode && finallyDepth == 0) {
            toBoxed(type);
            storeCompletionValue();
        } else {
            pop(type);
        }
    }

    @Override
    void enterTailCallPosition(Expression expr) {
        if (!isWrapped() && !isGenerator) {
            assert !isScriptCode;
            super.enterTailCallPosition(expr);
        }
    }

    /**
     * Returns <code>true</code> when currently within a finally scoped block
     */
    private boolean isFinallyScoped() {
        return labels.parent != null;
    }

    /**
     * Enter a finally scoped block
     */
    void enterFinallyScoped() {
        assert labels != null;
        labels = new Labels(labels);
    }

    /**
     * Exit a finally scoped block
     */
    List<TempLabel> exitFinallyScoped() {
        List<TempLabel> tempLabels = labels.tempLabels;
        labels = labels.parent;
        assert labels != null;
        return tempLabels != null ? tempLabels : Collections.<TempLabel> emptyList();
    }

    /**
     * Returns <code>true</code> when currently within a wrapped block
     */
    private boolean isWrapped() {
        return wrapped != 0;
    }

    /**
     * Enter a try-catch-finally wrapped block
     */
    void enterWrapped() {
        ++wrapped;
    }

    /**
     * Exit a try-catch-finally wrapped block
     */
    void exitWrapped() {
        --wrapped;
    }

    /**
     * Enter a finally block
     */
    void enterFinally() {
        ++finallyDepth;
    }

    /**
     * Exit a finally block
     */
    void exitFinally() {
        --finallyDepth;
    }

    /**
     * Start code generation for {@link IterationStatement} nodes
     */
    void enterIteration(IterationStatement node, BreakLabel lblBreak, ContinueLabel lblContinue) {
        boolean hasBreak = node.getAbrupt().contains(Abrupt.Break);
        boolean hasContinue = node.getAbrupt().contains(Abrupt.Continue);
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
     * Stop code generation for {@link IterationStatement} nodes
     */
    void exitIteration(IterationStatement node) {
        boolean hasBreak = node.getAbrupt().contains(Abrupt.Break);
        boolean hasContinue = node.getAbrupt().contains(Abrupt.Continue);
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
     * Start code generation for {@link BreakableStatement} nodes
     */
    void enterBreakable(BreakableStatement node, BreakLabel lblBreak) {
        if (!node.getAbrupt().contains(Abrupt.Break))
            return;
        Labels labels = this.labels;
        labels.breakTargets.push(lblBreak);
        for (String label : node.getLabelSet()) {
            labels.namedBreakLabels.put(label, lblBreak);
        }
    }

    /**
     * Stop code generation for {@link BreakableStatement} nodes
     */
    void exitBreakable(BreakableStatement node) {
        if (!node.getAbrupt().contains(Abrupt.Break))
            return;
        Labels labels = this.labels;
        labels.breakTargets.pop();
        for (String label : node.getLabelSet()) {
            labels.namedBreakLabels.remove(label);
        }
    }

    /**
     * Start code generation for {@link LabelledStatement} nodes
     */
    void enterLabelled(LabelledStatement node, BreakLabel lblBreak) {
        if (!node.getAbrupt().contains(Abrupt.Break))
            return;
        Labels labels = this.labels;
        for (String label : node.getLabelSet()) {
            labels.namedBreakLabels.put(label, lblBreak);
        }
    }

    /**
     * Stop code generation for {@link LabelledStatement} nodes
     */
    void exitLabelled(LabelledStatement node) {
        if (!node.getAbrupt().contains(Abrupt.Break))
            return;
        Labels labels = this.labels;
        for (String label : node.getLabelSet()) {
            labels.namedBreakLabels.remove(label);
        }
    }

    /**
     * Start code generation for {@link TryStatement} nodes with {@link GuardedCatchNode}
     */
    void enterCatchWithGuarded(TryStatement node, Label lblCatch) {
        labels.catchLabels.push(lblCatch);
    }

    /**
     * Stop code generation for {@link TryStatement} nodes with {@link GuardedCatchNode}
     */
    void exitCatchWithGuarded(TryStatement node) {
        labels.catchLabels.pop();
    }

    /**
     * Pops the stack's top element and emits a return instruction
     */
    void returnCompletion() {
        if (isFinallyScoped()) {
            // If currently enclosed by a finally scoped block, store value in completion register
            // and jump to (temporary) return label
            storeCompletionValue();
            goTo(returnLabel());
        } else {
            // Otherwise emit direct return instruction
            areturn();
        }
    }

    /**
     * Returns the current return-label
     */
    private Label returnLabel() {
        return labels.returnLabel().mark();
    }

    /**
     * Returns the break-label for the given {@link BreakStatement}
     */
    Label breakLabel(BreakStatement node) {
        String name = node.getLabel();
        return labels.breakLabel(name).mark();
    }

    /**
     * Returns the continue-label for the given {@link ContinueStatement}
     */
    Label continueLabel(ContinueStatement node) {
        String name = node.getLabel();
        return labels.continueLabel(name).mark();
    }

    /**
     * Returns the catch-label originally set in {@link #enterCatchWithGuarded(TryStatement, Label)}
     */
    Label catchWithGuardedLabel() {
        return labels.catchLabels.peek();
    }

    /**
     * Emit goto instruction to jump to {@code label}'s actual target
     */
    void goTo(TempLabel label) {
        JumpLabel actual = label.getActual();
        if (actual instanceof ReturnLabel) {
            // specialize return label to emit direct return instruction
            loadCompletionValue();
            areturn();
        } else {
            goTo(actual.mark());
        }
    }
}

/**
 * Copyright (c) 2012-2014 André Bargull
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
        final Variable<Object> completion;

        Labels(Labels parent, Variable<Object> completion) {
            this.parent = parent;
            this.completion = completion;
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

    private final TopLevelNode<?> topLevelNode;
    private final CodeType codeType;
    private final boolean isScriptCode;
    private final boolean isGeneratorOrAsync;
    private Variable<Object> completionValue;
    private Labels labels = new Labels(null, null);
    private int finallyDepth = 0;

    // tail-call support
    private int wrapped = 0;

    protected StatementVisitor(MethodCode method, StatementVisitor parent) {
        super(method, parent);
        this.topLevelNode = parent.getTopLevelNode();
        this.codeType = parent.getCodeType();
        this.isScriptCode = codeType != CodeType.Function;
        this.isGeneratorOrAsync = codeType == CodeType.Function && isGeneratorOrAsync(topLevelNode);
        // no return in script code
        this.labels.returnLabel = codeType == CodeType.Function ? new ReturnLabel() : null;
    }

    protected StatementVisitor(MethodCode method, boolean strict, TopLevelNode<?> topLevelNode,
            CodeType codeType) {
        super(method, strict, codeType == CodeType.GlobalScript, topLevelNode.hasSyntheticNodes());
        this.topLevelNode = topLevelNode;
        this.codeType = codeType;
        this.isScriptCode = codeType != CodeType.Function;
        this.isGeneratorOrAsync = codeType == CodeType.Function && isGeneratorOrAsync(topLevelNode);
        // no return in script code
        this.labels.returnLabel = codeType == CodeType.Function ? new ReturnLabel() : null;
    }

    private static boolean isGeneratorOrAsync(TopLevelNode<?> node) {
        assert node instanceof FunctionNode;
        return ((FunctionNode) node).isGenerator() || ((FunctionNode) node).isAsync();
    }

    @Override
    public void begin() {
        super.begin();
        if (isScriptCode) {
            completionValue = hasParameter(COMPLETION_SLOT, Object.class) ? getParameter(
                    COMPLETION_SLOT, Object.class) : newVariable("completion", Object.class);
        }
    }

    /**
     * Returns the {@link TopLevelNode} for this statement visitor
     */
    TopLevelNode<?> getTopLevelNode() {
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
        if (isScriptCode) {
            load(completionValue);
        } else {
            aconst(null);
        }
    }

    /**
     * Pops the stack's top element and stores it into the completion value
     */
    void storeCompletionValue(ValType type) {
        if (isScriptCode && finallyDepth == 0) {
            toBoxed(type);
            store(completionValue);
        } else {
            pop(type);
        }
    }

    @Override
    void enterTailCallPosition(Expression expr) {
        if (!isWrapped() && !isGeneratorOrAsync) {
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
    Variable<Object> enterFinallyScoped() {
        assert labels != null;
        Variable<Object> completion = labels.completion;
        if (completion == null) {
            completion = newVariable("completion", Object.class);
            // TODO: correct live variable analysis, initialised with null for now
            aconst(null);
            store(completion);
        }
        labels = new Labels(labels, completion);
        return completion;
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
     * Stop code generation for {@link IterationStatement} nodes
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
     * Start code generation for {@link BreakableStatement} nodes
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
     * Stop code generation for {@link BreakableStatement} nodes
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
     * Start code generation for {@link LabelledStatement} nodes
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
     * Stop code generation for {@link LabelledStatement} nodes
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
            store(labels.completion);
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
    void goTo(TempLabel label, Variable<Object> completion) {
        JumpLabel actual = label.getActual();
        if (actual instanceof ReturnLabel) {
            // specialize return label to emit direct return instruction
            load(completion);
            areturn();
        } else {
            goTo(actual.mark());
        }
    }
}

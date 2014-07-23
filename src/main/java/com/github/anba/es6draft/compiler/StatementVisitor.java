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
import java.util.HashMap;
import java.util.List;

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
        final ArrayDeque<JumpLabel> breakTargets = new ArrayDeque<>(4);
        final ArrayDeque<JumpLabel> continueTargets = new ArrayDeque<>(4);

        // labelled breaks and continues
        final HashMap<String, JumpLabel> namedBreakLabels = new HashMap<>(4);
        final HashMap<String, JumpLabel> namedContinueLabels = new HashMap<>(4);

        JumpLabel returnLabel = null;

        // label after last catch node
        final ArrayDeque<Label> catchLabels = new ArrayDeque<>(4);

        // temporary labels in try-catch-finally blocks
        ArrayList<TempLabel> tempLabels = null;

        final Labels parent;
        final Variable<Object> completion;
        final boolean isFinallyScope;

        Labels(Labels parent, Variable<Object> completion, boolean isFinallyScope) {
            this.parent = parent;
            this.completion = completion;
            this.isFinallyScope = isFinallyScope;
        }

        private TempLabel newTemp(JumpLabel actual) {
            assert actual != null;
            TempLabel temp = new TempLabel(actual);
            if (tempLabels == null) {
                tempLabels = new ArrayList<>(4);
            }
            tempLabels.add(temp);
            return temp;
        }

        private JumpLabel getBreakLabel(String name) {
            return name == null ? breakTargets.peek() : namedBreakLabels.get(name);
        }

        private void setBreakLabel(String name, JumpLabel label) {
            if (name == null) {
                assert breakTargets.isEmpty();
                breakTargets.push(label);
            } else {
                namedBreakLabels.put(name, label);
            }
        }

        private JumpLabel getContinueLabel(String name) {
            return name == null ? continueTargets.peek() : namedContinueLabels.get(name);
        }

        private void setContinueLabel(String name, JumpLabel label) {
            if (name == null) {
                assert continueTargets.isEmpty();
                continueTargets.push(label);
            } else {
                namedContinueLabels.put(name, label);
            }
        }

        JumpLabel returnLabel() {
            JumpLabel lbl = returnLabel;
            if (lbl == null) {
                assert parent != null : "return outside of function";
                lbl = newTemp(parent.returnLabel());
                returnLabel = lbl;
            }
            return lbl;
        }

        JumpLabel breakLabel(String name) {
            JumpLabel label = getBreakLabel(name);
            if (label == null) {
                assert parent != null : "Label not found: " + name;
                label = newTemp(parent.breakLabel(name));
                setBreakLabel(name, label);
            }
            return label;
        }

        JumpLabel continueLabel(String name) {
            JumpLabel label = getContinueLabel(name);
            if (label == null) {
                assert parent != null : "Label not found: " + name;
                label = parent.continueLabel(name);
                if (isFinallyScope) {
                    setContinueLabel(name, label = newTemp(label));
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
    private Labels labels = new Labels(null, null, false);
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
     * Returns the {@link TopLevelNode} for this statement visitor.
     * 
     * @return the top level node for this statement visitor
     */
    TopLevelNode<?> getTopLevelNode() {
        return topLevelNode;
    }

    /**
     * Returns the {@link CodeType} for this statement visitor.
     * 
     * @return the code type for this statement visitor
     */
    CodeType getCodeType() {
        return codeType;
    }

    /**
     * Returns {@code true} if compiling generator or async function code.
     * 
     * @return {@code true} if generator or async function
     */
    boolean isGeneratorOrAsync() {
        return isGeneratorOrAsync;
    }

    /**
     * Pushes the completion value onto the stack.
     */
    void loadCompletionValue() {
        if (isScriptCode) {
            load(completionValue);
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
     * Returns <code>true</code> when currently within a finally scoped block.
     * 
     * @return <code>true</code> if currently in a finally block
     */
    private boolean isAbruptRegion() {
        return labels.parent != null;
    }

    private Variable<Object> enterAbruptRegion(boolean isFinallyScope) {
        assert labels != null;
        Variable<Object> completion = labels.completion;
        if (completion == null) {
            completion = newVariable("completion", Object.class);
            // TODO: correct live variable analysis, initialized with null for now
            aconst(null);
            store(completion);
        }
        labels = new Labels(labels, completion, isFinallyScope);
        return completion;
    }

    private List<TempLabel> exitAbruptRegion(boolean isFinallyScope) {
        assert labels.isFinallyScope == isFinallyScope;
        ArrayList<TempLabel> tempLabels = labels.tempLabels;
        labels = labels.parent;
        assert labels != null;
        return tempLabels != null ? tempLabels : Collections.<TempLabel> emptyList();
    }

    /**
     * Enter an iteration body block.
     * 
     * @return the temporary completion object variable
     */
    Variable<Object> enterIterationBody() {
        return enterAbruptRegion(false);
    }

    /**
     * Exit an iteration body block.
     * 
     * @return the list of generated labels
     */
    List<TempLabel> exitIterationBody() {
        return exitAbruptRegion(false);
    }

    /**
     * Enter a finally scoped block.
     * 
     * @return the temporary completion object variable
     */
    Variable<Object> enterFinallyScoped() {
        return enterAbruptRegion(true);
    }

    /**
     * Exit a finally scoped block.
     * 
     * @return the list of generated labels
     */
    List<TempLabel> exitFinallyScoped() {
        return exitAbruptRegion(true);
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
     * Enter a finally block.
     */
    void enterFinally() {
        ++finallyDepth;
    }

    /**
     * Exit a finally block.
     */
    void exitFinally() {
        --finallyDepth;
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
    void enterCatchWithGuarded(TryStatement node, Label catchLabel) {
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
            areturn();
        }
    }

    /**
     * Returns the current return-label.
     * 
     * @return the return label
     */
    private Label returnLabel() {
        return labels.returnLabel().mark();
    }

    /**
     * Returns the break-label for the given {@link BreakStatement}.
     * 
     * @param node
     *            the break statement
     * @return the label to the target instruction
     */
    Label breakLabel(BreakStatement node) {
        String name = node.getLabel();
        return labels.breakLabel(name).mark();
    }

    /**
     * Returns the continue-label for the given {@link ContinueStatement}.
     * 
     * @param node
     *            the continue statement
     * @return the label to the target instruction
     */
    Label continueLabel(ContinueStatement node) {
        String name = node.getLabel();
        return labels.continueLabel(name).mark();
    }

    /**
     * Returns the catch-label originally set in {@link #enterCatchWithGuarded(TryStatement, Label)}
     * .
     * 
     * @return the guarded catch label
     */
    Label catchWithGuardedLabel() {
        return labels.catchLabels.peek();
    }

    /**
     * Emit goto instruction to jump to {@code label}'s actual target.
     * 
     * @param label
     *            the target instruction
     * @param completion
     *            the variable which holds the current completion value
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

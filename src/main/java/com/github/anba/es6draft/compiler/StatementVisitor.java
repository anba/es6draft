/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.AbruptNode.Abrupt;
import com.github.anba.es6draft.ast.BreakStatement;
import com.github.anba.es6draft.ast.BreakableStatement;
import com.github.anba.es6draft.ast.ContinueStatement;
import com.github.anba.es6draft.ast.IterationStatement;
import com.github.anba.es6draft.ast.LabelledStatement;
import com.github.anba.es6draft.ast.TryStatement;

/**
 * 
 */
abstract class StatementVisitor extends ExpressionVisitor {
    private static class Labels {
        // unlabelled breaks and continues
        final Deque<Label> breakTargets = new ArrayDeque<>(4);
        final Deque<Label> continueTargets = new ArrayDeque<>(4);

        // labelled breaks and continues
        final Map<String, Label> namedBreakLabels = new HashMap<>(4);
        final Map<String, Label> namedContinueLabels = new HashMap<>(4);

        Label returnLabel = null;

        // label after last catch node
        final Deque<Label> catchLabels = new ArrayDeque<>(4);

        // temporary labels in try-catch-finally blocks
        List<Label> tempLabels = null;

        final Labels parent;

        Labels(Labels parent) {
            this.parent = parent;
        }

        Label newTemp(Label actual) {
            assert actual != null;
            Label temp = new Label();
            if (tempLabels == null) {
                tempLabels = new ArrayList<>(8);
            }
            tempLabels.add(actual);
            tempLabels.add(temp);
            return temp;
        }

        Label returnLabel() {
            Label lbl = returnLabel;
            if (lbl == null) {
                assert parent != null;
                lbl = newTemp(parent.returnLabel());
                returnLabel = lbl;
            }
            return lbl;
        }

        Label breakLabel(String name) {
            Label label;
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

        Label continueLabel(String name) {
            Label label;
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

    private final CodeType codeType;
    private final boolean completionValue;
    private Labels labels = new Labels(null);
    private int finallyDepth = 0;

    // tail-call support
    private int wrapped = 0;

    protected StatementVisitor(MethodVisitor mv, String methodName, Type methodDescriptor,
            boolean strict, CodeType codeType, boolean completionValue) {
        super(mv, methodName, methodDescriptor, strict, codeType == CodeType.GlobalScript);
        this.codeType = codeType;
        this.completionValue = completionValue;
        // no return in script code
        this.labels.returnLabel = codeType == CodeType.Function ? new Label() : null;
    }

    abstract void storeCompletionValue();

    abstract void loadCompletionValue();

    CodeType getCodeType() {
        return codeType;
    }

    boolean isCompletionValue() {
        return completionValue && finallyDepth == 0;
    }

    void enterFinallyScoped() {
        assert labels != null;
        labels = new Labels(labels);
    }

    List<Label> exitFinallyScoped() {
        List<Label> tempLabels = labels.tempLabels;
        labels = labels.parent;
        assert labels != null;
        return tempLabels;
    }

    boolean isWrapped() {
        return wrapped != 0;
    }

    void enterWrapped() {
        ++wrapped;
    }

    void exitWrapped() {
        --wrapped;
    }

    void enterFinally() {
        ++finallyDepth;
    }

    void exitFinally() {
        --finallyDepth;
    }

    void enterIteration(IterationStatement node, Label lblBreak, Label lblContinue) {
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

    void enterBreakable(BreakableStatement node, Label lblBreak) {
        if (!node.getAbrupt().contains(Abrupt.Break))
            return;
        Labels labels = this.labels;
        labels.breakTargets.push(lblBreak);
        for (String label : node.getLabelSet()) {
            labels.namedBreakLabels.put(label, lblBreak);
        }
    }

    void exitBreakable(BreakableStatement node) {
        if (!node.getAbrupt().contains(Abrupt.Break))
            return;
        Labels labels = this.labels;
        labels.breakTargets.pop();
        for (String label : node.getLabelSet()) {
            labels.namedBreakLabels.remove(label);
        }
    }

    void enterLabelled(LabelledStatement node, Label lblBreak) {
        if (!node.getAbrupt().contains(Abrupt.Break))
            return;
        Labels labels = this.labels;
        for (String label : node.getLabelSet()) {
            labels.namedBreakLabels.put(label, lblBreak);
        }
    }

    void exitLabelled(LabelledStatement node) {
        if (!node.getAbrupt().contains(Abrupt.Break))
            return;
        Labels labels = this.labels;
        for (String label : node.getLabelSet()) {
            labels.namedBreakLabels.remove(label);
        }
    }

    void enterCatchWithGuarded(TryStatement node, Label lblCatch) {
        labels.catchLabels.push(lblCatch);
    }

    void exitCatchWithGuarded(TryStatement node) {
        labels.catchLabels.pop();
    }

    Label returnLabel() {
        return labels.returnLabel();
    }

    Label breakLabel(BreakStatement node) {
        String name = node.getLabel();
        return labels.breakLabel(name);
    }

    Label continueLabel(ContinueStatement node) {
        String name = node.getLabel();
        return labels.continueLabel(name);
    }

    Label catchWithGuardedLabel() {
        return labels.catchLabels.peek();
    }
}

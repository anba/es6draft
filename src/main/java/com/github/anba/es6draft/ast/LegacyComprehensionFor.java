/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * Extension: Array and Generator Comprehension
 */
public final class LegacyComprehensionFor extends ComprehensionQualifier {
    private final IterationKind iterationKind;
    private final Binding binding;
    private final Expression expression;

    public enum IterationKind {
        Enumerate, Iterate, EnumerateValues
    }

    public LegacyComprehensionFor(long beginPosition, long endPosition,
            IterationKind iterationKind, Binding binding, Expression expression) {
        super(beginPosition, endPosition);
        this.iterationKind = iterationKind;
        this.binding = binding;
        this.expression = expression;
    }

    public IterationKind getIterationKind() {
        return iterationKind;
    }

    public Binding getBinding() {
        return binding;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> int accept(IntNodeVisitor<V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> void accept(VoidNodeVisitor<V> visitor, V value) {
        visitor.visit(this, value);
    }
}

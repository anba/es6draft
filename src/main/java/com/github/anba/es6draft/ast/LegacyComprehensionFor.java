/**
 * 
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>11 Expressions</h1><br>
 * <h2>11.1 Primary Expressions</h2><br>
 * <h3>11.1.4 Array Initialiser</h3>
 * <ul>
 * <li>11.1.4.2 Array Comprehension
 * </ul>
 */
public class LegacyComprehensionFor extends ComprehensionQualifier {
    private IterationKind iterationKind;
    private Binding binding;
    private Expression expression;

    public enum IterationKind {
        Enumerate, Iterate, EnumerateValues
    }

    public LegacyComprehensionFor(IterationKind iterationKind, Binding binding,
            Expression expression) {
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
}

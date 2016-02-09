/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/**
 * 
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1><br>
 * <h2>13.3 Declarations and the Variable Statement</h2>
 * <ul>
 * <li>13.3.3 Destructuring Binding Patterns
 * </ul>
 */
public final class BindingProperty extends AstNode {
    private final PropertyName propertyName;
    private final Binding binding;
    private final Expression initializer;

    public BindingProperty(PropertyName propertyName, Binding binding, Expression initializer) {
        super(propertyName.getBeginPosition(), eitherOr(initializer, binding).getEndPosition());
        this.propertyName = propertyName;
        this.binding = binding;
        this.initializer = initializer;
    }

    public BindingProperty(BindingIdentifier binding, Expression initializer) {
        super(binding.getBeginPosition(), eitherOr(initializer, binding).getEndPosition());
        this.propertyName = null;
        this.binding = binding;
        this.initializer = initializer;
    }

    /**
     * Returns the binding property name or {@code null} for <code>SingleNameBinding</code> nodes.
     * 
     * @return the property name or {@code null}
     */
    public PropertyName getPropertyName() {
        return propertyName;
    }

    /**
     * Returns the target binding.
     * 
     * @return the binding node
     */
    public Binding getBinding() {
        return binding;
    }

    /**
     * Returns the optional initializer expression.
     * 
     * @return the initializer or {@code null}
     */
    public Expression getInitializer() {
        return initializer;
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

    private static AstNode eitherOr(AstNode left, AstNode right) {
        return left != null ? left : right;
    }
}

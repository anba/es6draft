/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.2 Modules</h2>
 * <ul>
 * <li>15.2.3 Exports
 * </ul>
 */
public final class ExportDefaultExpression extends Declaration {
    private final BindingIdentifier binding;
    private final Expression expression;

    public ExportDefaultExpression(long beginPosition, long endPosition, BindingIdentifier binding,
            Expression expression) {
        super(beginPosition, endPosition);
        this.binding = binding;
        this.expression = expression;
    }

    /**
     * Returns the binding name ({@code *default*}) of this default export.
     * 
     * @return the default binding name
     */
    public BindingIdentifier getBinding() {
        return binding;
    }

    /**
     * Returns the default export expression.
     * 
     * @return the export expression
     */
    public Expression getExpression() {
        return expression;
    }

    @Override
    public boolean isConstDeclaration() {
        return true;
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

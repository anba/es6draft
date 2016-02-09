/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.runtime.internal.Strings;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.2 Primary Expression</h2>
 * <ul>
 * <li>12.2.6 Object Initializer
 * </ul>
 */
public final class ComputedPropertyName extends AstNode implements PropertyName {
    private final Expression expression;

    public ComputedPropertyName(long beginPosition, long endPosition, Expression expression) {
        super(beginPosition, endPosition);
        this.expression = expression;
    }

    /**
     * Returns the computed property name expression.
     * 
     * @return the expression
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Always returns {@code null} for computed property names.
     */
    @Override
    public String getName() {
        return null;
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

    @Override
    public String toString() {
        String cname = accept(new ComputedNameToString(), null);
        if (cname != null) {
            return '[' + cname + ']';
        }
        return "[<...>]";
    }

    private static final class ComputedNameToString extends DefaultNodeVisitor<String, Void> {
        @Override
        protected String visit(Node node, Void value) {
            return null;
        }

        @Override
        public String visit(ComputedPropertyName node, Void value) {
            return node.getExpression().accept(this, value);
        }

        @Override
        public String visit(NumericLiteral node, Void value) {
            return node.getName();
        }

        @Override
        public String visit(StringLiteral node, Void value) {
            return Strings.quote(node.getName());
        }

        @Override
        public String visit(IdentifierReference node, Void value) {
            return node.getName();
        }

        @Override
        public String visit(PropertyAccessor node, Void value) {
            String baseValue = node.getBase().accept(this, value);
            return baseValue != null ? baseValue + '.' + node.getName() : null;
        }
    }
}

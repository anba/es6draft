/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.1 Primary Expressions</h2>
 * <ul>
 * <li>12.1.9 Template Literals
 * </ul>
 */
public class TemplateLiteral extends Expression {
    private List<Expression> elements;
    private boolean tagged;

    public TemplateLiteral(long beginPosition, long endPosition, boolean tagged,
            List<Expression> elements) {
        super(beginPosition, endPosition);
        this.tagged = tagged;
        this.elements = elements;
    }

    public boolean isTagged() {
        return tagged;
    }

    public List<Expression> getElements() {
        return elements;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}

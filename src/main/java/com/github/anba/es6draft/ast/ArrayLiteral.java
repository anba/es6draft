/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.2 Primary Expression</h2>
 * <ul>
 * <li>12.2.5 Array Initializer
 * </ul>
 */
public class ArrayLiteral extends ArrayInitializer {
    private List<Expression> elements;
    private final boolean trailingComma;

    public ArrayLiteral(long beginPosition, long endPosition, List<Expression> elements,
            boolean trailingComma) {
        super(beginPosition, endPosition);
        this.elements = elements;
        this.trailingComma = trailingComma;
    }

    /**
     * Returns the array element expressions.
     * 
     * @return the array elements
     */
    public List<Expression> getElements() {
        return elements;
    }

    /**
     * Sets the array element expressions
     * 
     * @param elements
     *            the array elements
     */
    public void setElements(List<Expression> elements) {
        this.elements = elements;
    }

    /**
     * Returns {@code true} if the <code>ArrayLiteral</code> has a trailing comma.
     * 
     * @return {@code true} if the array literal has a trailing comma
     */
    public boolean hasTrailingComma() {
        return trailingComma;
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

/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.1 Primary Expressions</h2><br>
 * <h3>12.1.4 Array Initializer</h3>
 * <ul>
 * <li>12.1.4.1 Array Literal
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

    public List<Expression> getElements() {
        return elements;
    }

    public void setElements(List<Expression> elements) {
        this.elements = elements;
    }

    public boolean hasTrailingComma() {
        return trailingComma;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}

/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.1 Primary Expressions</h2>
 * <ul>
 * <li>12.1.3 Literals
 * </ul>
 */
public class NumericLiteral extends ValueLiteral<Double> implements PropertyName {
    private double value;

    public NumericLiteral(long beginPosition, long endPosition, double value) {
        super(beginPosition, endPosition);
        this.value = value;
    }

    @Override
    public Double getValue() {
        return value;
    }

    @Override
    public String getName() {
        return ToString(value);
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}

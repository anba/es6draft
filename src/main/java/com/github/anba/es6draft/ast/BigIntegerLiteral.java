/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.math.BigInteger;

/**
 * Extension: BigInt
 */
public final class BigIntegerLiteral extends ValueLiteral<BigInteger> implements PropertyName {
    private final BigInteger value;

    public BigIntegerLiteral(long beginPosition, long endPosition, BigInteger value) {
        super(beginPosition, endPosition);
        this.value = value;
    }

    @Override
    public BigInteger getValue() {
        return value;
    }

    @Override
    public String getName() {
        return value.toString();
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

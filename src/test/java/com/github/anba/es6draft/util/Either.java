/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.util.Objects;
import java.util.function.Function;

/**
 * 
 */
public final class Either<L, R> {
    private final L left;
    private final R right;

    private Either(L left, R right) {
        this.left = left;
        this.right = right;
    }

    /**
     * 
     * @param left
     *            the left component
     * @return the new either value
     */
    public static <L, R> Either<L, R> left(L left) {
        return new Either<>(Objects.requireNonNull(left), null);
    }

    /**
     * 
     * @param right
     *            the right component
     * @return the new either value
     */
    public static <L, R> Either<L, R> right(R right) {
        return new Either<>(null, Objects.requireNonNull(right));
    }

    /**
     * 
     * @param mapLeft
     *            the function to apply on the left component if present
     * @param mapRight
     *            the function to apply on the right component if present
     * @return the mapped value
     */
    public <T> T map(Function<L, T> mapLeft, Function<R, T> mapRight) {
        if (left != null) {
            return mapLeft.apply(left);
        }
        return mapRight.apply(right);
    }
}

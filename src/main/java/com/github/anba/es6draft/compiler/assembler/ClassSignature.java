/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

/**
 *
 */
public final class ClassSignature extends Signature {
    /**
     * Singleton instance to represent an absent class signature.
     */
    public static final ClassSignature NONE = new ClassSignature(null);

    private final String signature;

    public ClassSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        return signature;
    }
}

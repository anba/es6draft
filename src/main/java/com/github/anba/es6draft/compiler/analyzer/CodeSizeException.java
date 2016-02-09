/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

/**
 * Exception thrown when the maximum code size is exceeded
 */
@SuppressWarnings("serial")
public final class CodeSizeException extends RuntimeException {
    CodeSizeException(int size) {
        super("code exceeds maximum size: " + size);
    }
}

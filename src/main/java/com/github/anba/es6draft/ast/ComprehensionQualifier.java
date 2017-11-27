/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * Extension: Array and Generator Comprehension
 */
public abstract class ComprehensionQualifier extends AstNode {
    protected ComprehensionQualifier(long beginPosition, long endPosition) {
        super(beginPosition, endPosition);
    }
}

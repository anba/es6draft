/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.EnumSet;
import java.util.Set;

/**
 * <h1>12 Statements and Declarations</h1>
 */
public abstract class BreakableStatement extends Statement {
    protected BreakableStatement() {
    }

    public abstract Set<String> getLabelSet();

    public abstract EnumSet<Abrupt> getAbrupt();

    public enum Abrupt {
        Break, Continue
    }
}

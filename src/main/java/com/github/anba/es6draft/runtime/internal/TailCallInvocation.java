/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;

/**
 *
 */
public final class TailCallInvocation {
    private final OrdinaryFunction function;
    private final Object thisValue;
    private final Object[] argumentsList;

    public TailCallInvocation(OrdinaryFunction function, Object thisValue, Object[] argumentsList) {
        this.function = function;
        this.thisValue = thisValue;
        this.argumentsList = argumentsList;
    }

    public OrdinaryFunction getFunction() {
        return function;
    }

    public Object getThisValue() {
        return thisValue;
    }

    public Object[] getArgumentsList() {
        return argumentsList;
    }
}

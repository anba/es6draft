/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import com.github.anba.es6draft.runtime.internal.RuntimeInfo;

/**
 *
 */
public abstract class CompiledFunction {
    private final RuntimeInfo.Function function;

    protected CompiledFunction(RuntimeInfo.Function function) {
        this.function = function;
    }

    public RuntimeInfo.Function getFunction() {
        return function;
    }
}

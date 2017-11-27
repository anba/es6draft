/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.Source;

/**
 * Base class for compiled functions.
 */
public class CompiledFunction extends CompiledObject {
    protected CompiledFunction(Source source, RuntimeInfo.Function function) {
        super(source, function);
    }

    /**
     * Returns the runtime information for this function.
     * 
     * @return the runtime information object
     */
    public final RuntimeInfo.Function getFunction() {
        return (RuntimeInfo.Function) getRuntimeObject();
    }
}

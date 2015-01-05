/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import com.github.anba.es6draft.Module;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;

/**
 * Abstract base class for compiled modules.
 */
public abstract class CompiledModule extends CompiledObject implements Module {
    protected CompiledModule(RuntimeInfo.ModuleBody module) {
        super(module);
    }

    @Override
    public final RuntimeInfo.ModuleBody getModuleBody() {
        return (RuntimeInfo.ModuleBody) getSourceObject();
    }

    @Override
    public final Object evaluate(ExecutionContext cx) {
        return getModuleBody().evaluate(cx);
    }
}

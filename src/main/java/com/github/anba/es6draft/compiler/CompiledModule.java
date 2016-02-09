/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import com.github.anba.es6draft.Module;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;

/**
 * Base class for compiled modules.
 */
public class CompiledModule extends CompiledObject implements Module {
    protected CompiledModule(RuntimeInfo.ModuleBody module) {
        super(module);
    }

    @Override
    public final RuntimeInfo.ModuleBody getModuleBody() {
        return (RuntimeInfo.ModuleBody) getSourceObject();
    }
}

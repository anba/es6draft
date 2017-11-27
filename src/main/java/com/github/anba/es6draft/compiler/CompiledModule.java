/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import com.github.anba.es6draft.Module;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.Source;

/**
 * Base class for compiled modules.
 */
public class CompiledModule extends CompiledObject implements Module {
    protected CompiledModule(Source source, RuntimeInfo.ModuleBody module) {
        super(source, module);
    }

    @Override
    public final RuntimeInfo.ModuleBody getModuleBody() {
        return (RuntimeInfo.ModuleBody) getRuntimeObject();
    }
}

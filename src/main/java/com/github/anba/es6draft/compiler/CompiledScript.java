/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;

/**
 * Base class for compiled scripts.
 */
public class CompiledScript extends CompiledObject implements Script {
    protected CompiledScript(RuntimeInfo.ScriptBody scriptBody) {
        super(scriptBody);
    }

    @Override
    public final RuntimeInfo.ScriptBody getScriptBody() {
        return (RuntimeInfo.ScriptBody) getSourceObject();
    }
}

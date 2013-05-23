/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.interpreter;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;

/**
 * 
 */
public final class InterpretedScript implements Script {
    private final RuntimeInfo.ScriptBody scriptBody;

    InterpretedScript(RuntimeInfo.ScriptBody scriptBody) {
        this.scriptBody = scriptBody;
    }

    @Override
    public RuntimeInfo.ScriptBody getScriptBody() {
        return scriptBody;
    }

    @Override
    public Object evaluate(ExecutionContext cx) {
        return scriptBody.evaluate(cx);
    }
}

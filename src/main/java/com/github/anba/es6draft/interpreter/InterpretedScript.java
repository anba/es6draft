/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.interpreter;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.Source;

/**
 * 
 */
public final class InterpretedScript implements Script {
    private final Source source;
    private final InterpretedScriptBody scriptBody;

    InterpretedScript(com.github.anba.es6draft.ast.Script parsedScript) {
        this.source = parsedScript.getSource();
        this.scriptBody = new InterpretedScriptBody(parsedScript);
    }

    @Override
    public Source getSource() {
        return source;
    }

    @Override
    public RuntimeInfo.RuntimeObject getRuntimeObject() {
        return scriptBody;
    }

    @Override
    public RuntimeInfo.ScriptBody getScriptBody() {
        return scriptBody;
    }
}

/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.github.anba.es6draft.Script;

/**
 *
 */
final class CompiledScriptImpl extends CompiledScript {
    private final ScriptEngineImpl scriptEngine;
    private final Script script;

    public CompiledScriptImpl(ScriptEngineImpl scriptEngine, Script script) {
        this.scriptEngine = scriptEngine;
        this.script = script;
    }

    @Override
    public ScriptEngine getEngine() {
        return scriptEngine;
    }

    @Override
    public Object eval(ScriptContext context) throws ScriptException {
        return scriptEngine.eval(script, context);
    }
}

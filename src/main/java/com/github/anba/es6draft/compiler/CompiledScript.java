/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.HashMap;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;

/**
 * Abstract base class for compiled scripts.
 */
public abstract class CompiledScript implements Script {
    private final RuntimeInfo.ScriptBody scriptBody;
    private HashMap<Integer, ExoticArray> templateCallSites;

    protected CompiledScript(RuntimeInfo.ScriptBody scriptBody) {
        this.scriptBody = scriptBody;
    }

    @Override
    public final RuntimeInfo.ScriptBody getScriptBody() {
        return scriptBody;
    }

    @Override
    public final Object evaluate(ExecutionContext cx) {
        return scriptBody.evaluate(cx);
    }

    /**
     * Returns the template call-site object for {@code key}.
     * 
     * @param key
     *            the template literal key
     * @return the call-site object
     */
    public final ExoticArray getTemplateCallSite(int key) {
        return templateCallSites != null ? templateCallSites.get(key) : null;
    }

    /**
     * Stores the template call-site object.
     * 
     * @param key
     *            the template literal key
     * @param callSite
     *            the call-site object
     */
    public final void addTemplateCallSite(int key, ExoticArray callSite) {
        if (templateCallSites == null) {
            templateCallSites = new HashMap<>();
        }
        templateCallSites.put(key, callSite);
    }
}

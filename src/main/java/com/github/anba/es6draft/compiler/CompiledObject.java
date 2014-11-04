/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.HashMap;

import com.github.anba.es6draft.Executable;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;

/**
 * Abstract base class for compiled objects.
 */
public abstract class CompiledObject implements Executable {
    private final RuntimeInfo.SourceObject sourceObject;
    private HashMap<Integer, ArrayObject> templateCallSites;

    protected CompiledObject(RuntimeInfo.SourceObject sourceObject) {
        this.sourceObject = sourceObject;
    }

    @Override
    public final RuntimeInfo.SourceObject getSourceObject() {
        return sourceObject;
    }

    /**
     * Returns the template call-site object for {@code key}.
     * 
     * @param key
     *            the template literal key
     * @return the call-site object
     */
    public final ArrayObject getTemplateCallSite(int key) {
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
    public final void addTemplateCallSite(int key, ArrayObject callSite) {
        if (templateCallSites == null) {
            templateCallSites = new HashMap<>();
        }
        templateCallSites.put(key, callSite);
    }
}

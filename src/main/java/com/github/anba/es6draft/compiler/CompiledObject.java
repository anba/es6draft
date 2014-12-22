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
    private HashMap<Integer, ArrayObject> templateObjects;

    protected CompiledObject(RuntimeInfo.SourceObject sourceObject) {
        this.sourceObject = sourceObject;
    }

    @Override
    public final RuntimeInfo.SourceObject getSourceObject() {
        return sourceObject;
    }

    /**
     * Returns the template object for {@code key}.
     * 
     * @param key
     *            the template literal key
     * @return the template object
     */
    public final ArrayObject getTemplateObject(int key) {
        return templateObjects != null ? templateObjects.get(key) : null;
    }

    /**
     * Stores the template object.
     * 
     * @param key
     *            the template literal key
     * @param template
     *            the template object
     */
    public final void setTemplateObject(int key, ArrayObject template) {
        if (templateObjects == null) {
            templateObjects = new HashMap<>();
        }
        templateObjects.put(key, template);
    }
}

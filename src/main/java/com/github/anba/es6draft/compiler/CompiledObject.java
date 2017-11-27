/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.HashMap;

import com.github.anba.es6draft.Executable;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;

/**
 * Base class for compiled objects.
 */
public class CompiledObject implements Executable {
    private final Source source;
    private final RuntimeInfo.RuntimeObject sourceObject;
    private HashMap<Integer, ArrayObject> templateObjects;

    protected CompiledObject(Source source, RuntimeInfo.RuntimeObject sourceObject) {
        this.source = source;
        this.sourceObject = sourceObject;
    }

    @Override
    public final Source getSource() {
        return source;
    }

    @Override
    public final RuntimeInfo.RuntimeObject getRuntimeObject() {
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

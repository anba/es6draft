/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.objects.GlobalObject;

/**
 * {@link GlobalObject} implementation for the JSR-223 Scripting API.
 */
public final class ScriptingGlobalObject extends GlobalObject {
    public ScriptingGlobalObject(Realm realm) {
        super(realm);
    }

    @Override
    public void initializeExtensions() {
        super.initializeExtensions();
        createGlobalProperties(new ScriptingFunctions(), ScriptingFunctions.class);
    }

    /**
     * Returns an object to allocate new instances of this class.
     * 
     * @return the object allocator to construct new global object instances
     */
    public static ObjectAllocator<ScriptingGlobalObject> newGlobalObjectAllocator() {
        return new ObjectAllocator<ScriptingGlobalObject>() {
            @Override
            public ScriptingGlobalObject newInstance(Realm realm) {
                return new ScriptingGlobalObject(realm);
            }
        };
    }
}

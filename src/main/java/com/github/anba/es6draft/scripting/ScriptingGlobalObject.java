/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.objects.GlobalObject;

/**
 * {@link GlobalObject} implementation for the JSR-223 Scripting API.
 */
public final class ScriptingGlobalObject extends GlobalObject {
    public ScriptingGlobalObject(Realm realm) {
        super(realm);
    }

    @Override
    protected void initializeExtensions() {
        super.initializeExtensions();
        Realm realm = getRealm();
        createProperties(realm.defaultContext(), realm.getGlobalThis(), this,
                ScriptingGlobalObject.class);
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

    /**
     * builtin-function: {@code print(message)}
     *
     * @param messages
     *            the string to print
     */
    @Function(name = "print", arity = 1)
    public void print(String... messages) {
        System.out.println(Strings.concatWith(' ', messages));
    }
}

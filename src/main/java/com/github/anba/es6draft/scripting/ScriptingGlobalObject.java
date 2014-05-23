/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.objects.GlobalObject;

/**
 * {@link GlobalObject} implementation for the JSR-223 Scripting API.
 */
public final class ScriptingGlobalObject extends GlobalObject {
    public ScriptingGlobalObject(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        super.initialize(cx);
        createProperties(cx, this, this, ScriptingGlobalObject.class);
    }

    @Override
    public void initialize() {
        /* no initialization required */
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

    private static String concat(String... strings) {
        if (strings.length == 0) {
            return "";
        } else if (strings.length == 1) {
            return strings[0];
        } else {
            StringBuilder sb = new StringBuilder();
            for (String string : strings) {
                sb.append(string).append(' ');
            }
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }
    }

    /**
     * builtin-function: {@code print(message)}
     *
     * @param messages
     *            the string to print
     */
    @Function(name = "print", arity = 1)
    public void print(String... messages) {
        System.out.println(concat(messages));
    }
}

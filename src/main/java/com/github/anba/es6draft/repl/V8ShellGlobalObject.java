/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import java.nio.file.Path;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * Global object class with support for some v8-shell functions
 */
public class V8ShellGlobalObject extends ShellGlobalObject {
    public V8ShellGlobalObject(Realm realm, ShellConsole console, Path baseDir, Path script,
            ScriptCache scriptCache) {
        super(realm, console, baseDir, script, scriptCache);
    }

    @Override
    public void defineBuiltinProperties(ExecutionContext cx, ScriptObject object) {
        super.defineBuiltinProperties(cx, object);
        createProperties(object, this, cx, V8ShellGlobalObject.class);
    }

    /**
     * Returns an object to allocate new instances of this class
     */
    public static ObjectAllocator<V8ShellGlobalObject> newGlobalObjectAllocator(
            final ShellConsole console, final Path baseDir, final Path script,
            final ScriptCache scriptCache) {
        return new ObjectAllocator<V8ShellGlobalObject>() {
            @Override
            public V8ShellGlobalObject newInstance(Realm realm) {
                return new V8ShellGlobalObject(realm, console, baseDir, script, scriptCache);
            }
        };
    }

    private String concat(String... strings) {
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

    /** shell-function: {@code write(message)} */
    @Function(name = "write", arity = 1)
    public void write(String... messages) {
        console.putstr(concat(messages));
    }

    /** shell-function: {@code gc()} */
    @Function(name = "gc", arity = 0)
    public void gc() {
        // empty
    }

    /** shell-function: {@code getDefaultLocale()} */
    @Function(name = "getDefaultLocale", arity = 0)
    public String getDefaultLocale() {
        return IntlAbstractOperations.DefaultLocale(getRealm());
    }

    /** shell-function: {@code getDefaultTimeZone()} */
    @Function(name = "getDefaultTimeZone", arity = 0)
    public String getDefaultTimeZone() {
        return IntlAbstractOperations.DefaultTimeZone(getRealm());
    }
}

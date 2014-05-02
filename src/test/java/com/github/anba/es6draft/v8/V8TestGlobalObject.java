/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.v8;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import java.nio.file.Path;

import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.global.V8ShellGlobalObject;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptCache;

/**
 * 
 */
public final class V8TestGlobalObject extends V8ShellGlobalObject {
    protected V8TestGlobalObject(Realm realm, ShellConsole console, Path baseDir, Path script,
            ScriptCache scriptCache) {
        super(realm, console, baseDir, script, scriptCache);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        super.initialize(cx);
        createProperties(cx, this, new TestingFunctions(), TestingFunctions.class);
    }

    /**
     * Returns an object to allocate new instances of this class.
     * 
     * @param console
     *            the console object
     * @param baseDir
     *            the base directory
     * @param script
     *            the main script file
     * @param scriptCache
     *            the script cache
     * @return the object allocator to construct new global object instances
     */
    public static ObjectAllocator<V8TestGlobalObject> newTestGlobalObjectAllocator(
            final ShellConsole console, final Path baseDir, final Path script,
            final ScriptCache scriptCache) {
        return new ObjectAllocator<V8TestGlobalObject>() {
            @Override
            public V8TestGlobalObject newInstance(Realm realm) {
                return new V8TestGlobalObject(realm, console, baseDir, script, scriptCache);
            }
        };
    }
}

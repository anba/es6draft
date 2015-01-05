/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.traceur;

import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.global.V8ShellGlobalObject;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.util.TestInfo;

/**
 * 
 */
final class TraceurTestGlobalObject extends V8ShellGlobalObject {
    protected TraceurTestGlobalObject(Realm realm, ShellConsole console, TestInfo test,
            ScriptCache scriptCache) {
        super(realm, console, test.getBaseDir(), test.getScript(), scriptCache);
    }

    @Override
    protected void initializeExtensions(ExecutionContext cx) {
        super.initializeExtensions(cx);
    }

    /**
     * Returns an object to allocate new instances of this class.
     * 
     * @param console
     *            the console object
     * @param test
     *            the test descriptor
     * @param scriptCache
     *            the script cache
     * @return the object allocator to construct new global object instances
     */
    static ObjectAllocator<TraceurTestGlobalObject> newGlobalObjectAllocator(
            final ShellConsole console, final TestInfo test, final ScriptCache scriptCache) {
        return new ObjectAllocator<TraceurTestGlobalObject>() {
            @Override
            public TraceurTestGlobalObject newInstance(Realm realm) {
                return new TraceurTestGlobalObject(realm, console, test, scriptCache);
            }
        };
    }
}

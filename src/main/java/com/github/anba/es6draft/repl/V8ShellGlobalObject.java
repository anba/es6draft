/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations;

/**
 * Global object class with support for some v8-shell functions
 */
public class V8ShellGlobalObject extends ShellGlobalObject {
    public V8ShellGlobalObject(Realm realm, ShellConsole console, Path baseDir, Path script,
            ScriptCache scriptCache) {
        super(realm, console, baseDir, script, scriptCache);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        super.initialise(cx);
        createProperties(this, cx, V8ShellGlobalObject.class);
    }

    /**
     * Returns a new instance of this class
     */
    public static V8ShellGlobalObject newGlobal(final ShellConsole console, final Path baseDir,
            final Path script, final ScriptCache scriptCache, final Set<CompatibilityOption> options) {
        Realm realm = Realm.newRealm(new ObjectAllocator<V8ShellGlobalObject>() {
            @Override
            public V8ShellGlobalObject newInstance(Realm realm) {
                return new V8ShellGlobalObject(realm, console, baseDir, script, scriptCache);
            }
        }, options);
        return (V8ShellGlobalObject) realm.getGlobalThis();
    }

    /** shell-function: {@code load(filename)} */
    @Function(name = "load", arity = 1)
    public Object load(String filename) {
        return load(Paths.get(filename), absolutePath(Paths.get(filename)));
    }

    /** shell-function: {@code read(filename)} */
    @Function(name = "read", arity = 1)
    public Object read(String filename) {
        return read(absolutePath(Paths.get(filename)));
    }

    /** shell-function: {@code readline()} */
    @Function(name = "readline", arity = 0)
    public String readline() {
        return console.readLine();
    }

    /** shell-function: {@code print(message)} */
    @Function(name = "print", arity = 1)
    public void print(String message) {
        console.print(message);
    }

    /** shell-function: {@code write(message)} */
    @Function(name = "write", arity = 1)
    public void write(String message) {
        console.putstr(message);
    }

    /** shell-function: {@code quit()} */
    @Function(name = "quit", arity = 0)
    public void quit() {
        throw new StopExecutionException(StopExecutionException.Reason.Quit);
    }

    /** shell-function: {@code gc()} */
    @Function(name = "gc", arity = 0)
    public void gc() {
    }

    /** shell-function: {@code getDefaultLocale()} */
    @Function(name = "getDefaultLocale", arity = 0)
    public String getDefaultLocale() {
        return IntlAbstractOperations.DefaultLocale(realm);
    }

    /** shell-function: {@code getDefaultTimeZone()} */
    @Function(name = "getDefaultTimeZone", arity = 0)
    public String getDefaultTimeZone() {
        return IntlAbstractOperations.DefaultTimeZone(realm);
    }
}

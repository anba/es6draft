/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.Realm.GlobalObjectCreator;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.Properties.Function;

/**
 *
 */
public class SimpleShellGlobalObject extends ShellGlobalObject {
    public SimpleShellGlobalObject(Realm realm, ShellConsole console, Path baseDir, Path script,
            ScriptCache scriptCache) {
        super(realm, console, baseDir, script, scriptCache);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        super.initialise(cx);
        createProperties(this, cx, SimpleShellGlobalObject.class);
    }

    public static SimpleShellGlobalObject newGlobal(final ShellConsole console, final Path baseDir,
            final Path script, final ScriptCache scriptCache, final Set<CompatibilityOption> options) {
        Realm realm = Realm.newRealm(new GlobalObjectCreator<SimpleShellGlobalObject>() {
            @Override
            public SimpleShellGlobalObject createGlobal(Realm realm) {
                return new SimpleShellGlobalObject(realm, console, baseDir, script, scriptCache);
            }
        }, options);
        return (SimpleShellGlobalObject) realm.getGlobalThis();
    }

    /** shell-function: {@code compile(filename)} */
    @Function(name = "compile", arity = 1)
    public String compile(String filename) {
        try {
            scriptCache.script(filename, 1, absolutePath(Paths.get(filename)));
        } catch (ParserException | CompilationException | IOException e) {
            return "error: " + e.getMessage();
        }
        return "success";
    }

    /** shell-function: {@code print(message)} */
    @Function(name = "print", arity = 1)
    public void print(String message) {
        console.print(message);
    }

    /** shell-function: {@code quit()} */
    @Function(name = "quit", arity = 0)
    public void quit() {
        throw new StopExecutionException(StopExecutionException.Reason.Quit);
    }
}

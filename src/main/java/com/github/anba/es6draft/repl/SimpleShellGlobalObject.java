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
import java.util.EnumSet;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.ScriptCache;

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

    /**
     * Returns a new instance of this class
     */
    public static SimpleShellGlobalObject newGlobal(World world, final ShellConsole console,
            final Path baseDir, final Path script, final ScriptCache scriptCache) {
        Realm realm = world.newRealm(new ObjectAllocator<SimpleShellGlobalObject>() {
            @Override
            public SimpleShellGlobalObject newInstance(Realm realm) {
                return new SimpleShellGlobalObject(realm, console, baseDir, script, scriptCache);
            }
        });
        return (SimpleShellGlobalObject) realm.getGlobalThis();
    }

    /** shell-function: {@code parseModule(source)} */
    @Function(name = "parseModule", arity = 1)
    public String parseModule(ExecutionContext cx, String source) {
        EnumSet<Parser.Option> options = Parser.Option.from(getRealm().getOptions());
        Parser parser = new Parser("<module>", 1, options);
        try {
            parser.parseModule(source);
            return "success";
        } catch (ParserException e) {
            throw e.toScriptException(cx);
        }
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

    /** shell-function: {@code loadRelativeToScript(filename)} */
    @Function(name = "loadRelativeToScript", arity = 1)
    public Object loadRelativeToScript(ExecutionContext cx, String filename) {
        return load(cx, Paths.get(filename), relativePath(Paths.get(filename)));
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

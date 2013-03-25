/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.moztest;

import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.ScriptRuntime;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.util.ScriptCache;

/**
 * Global object class for the Mozilla test-suite, also provides implementation for some
 * shell-functions which are called in the test-suite.
 */
public class MozTestGlobalObject extends GlobalObject {
    private List<Throwable> failures = new ArrayList<Throwable>();
    private final Path basedir;
    private final Path script;
    private final ScriptCache scriptCache;

    public MozTestGlobalObject(Realm realm, Path basedir, Path file, ScriptCache scriptCache) {
        super(realm);
        this.basedir = basedir;
        this.script = file;
        this.scriptCache = scriptCache;
    }

    public List<Throwable> getFailures() {
        return failures;
    }

    /**
     * Parses, compiles and executes the javascript file
     */
    public void eval(Path file) throws IOException {
        String sourceName = file.getFileName().toString();
        Script script = scriptCache.script(sourceName, file);
        evaluate(script);
    }

    /**
     * Parses, compiles and executes the javascript file (uses {@link #scriptCache})
     */
    public void include(Path file) throws IOException {
        Path p = basedir.resolve(file);
        Script script = scriptCache.get(p);
        evaluate(script);
    }

    /**
     * Evalutes the {@code script}
     */
    public Object evaluate(Script script) throws IOException {
        return ScriptLoader.ScriptEvaluation(script, realm(), false);
    }

    /** shell-function: {@code print()} */
    @Function(name = "quit", arity = 0)
    public void quit() {
        throw new StopExecutionException();
    }

    /** shell-function: {@code print([exp, ...])} */
    @Function(name = "print", arity = 1)
    public void print(String message) {
        if (message.startsWith(" FAILED! ")) {
            // collect all failures instead of calling fail() directly
            failures.add(new AssertionError(message));
        }
        // System.out.println(message);
    }

    /** shell-function: {@code load(path)} */
    @Function(name = "load", arity = 1)
    public Object load(String path) {
        Path p = basedir.resolve(script.getParent().resolve(Paths.get(path)));
        if (!Files.exists(p)) {
            String s = p.toString();
            Object e = ((Constructor) realm().getIntrinsic(Intrinsics.Error)).construct(s);
            ScriptRuntime._throw(e);
        }
        try {
            eval(p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return UNDEFINED;
    }

    /** shell-function: {@code gc()} */
    @Function(name = "gc", arity = 0)
    public String gc() {
        return "";
    }

    /** shell-function: {@code gczeal()} */
    @Function(name = "gczeal", arity = 0)
    public String gczeal() {
        return "";
    }

    /** shell-function: {@code options([name])} */
    @Function(name = "options", arity = 0)
    public String options() {
        return "";
    }

    /** shell-function: {@code version([number])} */
    @Function(name = "version", arity = 1)
    public String version() {
        return "185";
    }
}

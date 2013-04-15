/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.moztest;

import static com.github.anba.es6draft.runtime.AbstractOperations.SameValue;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.EvaluateConstructorCall;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime._throw;
import static com.github.anba.es6draft.runtime.internal.SourceBuilder.ToSource;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.ast.ExpressionStatement;
import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ExoticSymbolObject;
import com.github.anba.es6draft.util.ScriptCache;

/**
 * Global object class for the Mozilla test-suite, also provides implementation for some
 * shell-functions which are called in the test-suite.
 */
public class MozTestGlobalObject extends GlobalObject {
    private List<Throwable> failures = new ArrayList<Throwable>();
    private final Realm realm;
    private final Path basedir;
    private final Path script;
    private final ScriptCache scriptCache;

    public MozTestGlobalObject(Realm realm, Path basedir, Path file, ScriptCache scriptCache) {
        super(realm);
        this.realm = realm;
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
        return ScriptLoader.ScriptEvaluation(script, realm, false);
    }

    private static ScriptException throwError(Realm realm, String message) {
        Object error = EvaluateConstructorCall(realm.getIntrinsic(Intrinsics.Error),
                new Object[] { message }, realm.defaultContext());
        return _throw(error);
    }

    private Path absolutePath(String filename) {
        return basedir.resolve(Paths.get(filename));
    }

    private Path relativePath(String filename) {
        return basedir.resolve(script.getParent().resolve(Paths.get(filename)));
    }

    private String read(Path path) {
        if (!Files.exists(path)) {
            _throw(String.format("can't open '%s'", path.toString()));
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Object load(Path path) {
        if (!Files.exists(path)) {
            _throw(String.format("can't open '%s'", path.toString()));
        }
        try {
            eval(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return UNDEFINED;
    }

    /**
     * Returns the well-known symbol {@code name} or undefined if there is no such symbol
     */
    @Function(name = "getSym", arity = 1)
    public Object getSym(String name) {
        try {
            if (name.startsWith("@@")) {
                return BuiltinSymbol.valueOf(name.substring(2)).get();
            }
        } catch (IllegalArgumentException e) {
        }
        return UNDEFINED;
    }

    /**
     * Creates a new Symbol object
     */
    @Function(name = "newSym", arity = 2)
    public Object newSym(String name, boolean _private) {
        return new ExoticSymbolObject(name, _private);
    }

    /** shell-function: {@code quit()} */
    @Function(name = "quit", arity = 0)
    public void quit() {
        throw new StopExecutionException();
    }

    /** shell-function: {@code assertEq()} */
    @Function(name = "assertEq", arity = 2)
    public void assertEq(Object actual, Object expected, Object message) {
        if (!SameValue(actual, expected)) {
            ExecutionContext cx = realm.defaultContext();
            StringBuilder msg = new StringBuilder();
            msg.append(String.format("Assertion failed: got %s, expected %s", ToSource(cx, actual),
                    ToSource(cx, expected)));
            if (!Type.isUndefined(message)) {
                msg.append(": ").append(ToFlatString(cx, message));
            }
            throwError(realm, msg.toString());
        }
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

    /** shell-function: {@code snarf(filename)} */
    @Function(name = "snarf", arity = 1)
    public Object snarf(String filename) {
        return read(filename);
    }

    /** shell-function: {@code read(filename)} */
    @Function(name = "read", arity = 1)
    public Object read(String filename) {
        return read(absolutePath(filename));
    }

    /** shell-function: {@code readRelativeToScript(filename)} */
    @Function(name = "readRelativeToScript", arity = 1)
    public Object readRelativeToScript(String filename) {
        return read(relativePath(filename));
    }

    /** shell-function: {@code load(filename)} */
    @Function(name = "load", arity = 1)
    public Object load(String filename) {
        return load(absolutePath(filename));
    }

    /** shell-function: {@code loadRelativeToScript(filename)} */
    @Function(name = "loadRelativeToScript", arity = 1)
    public Object loadRelativeToScript(String filename) {
        return load(relativePath(filename));
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

    /** shell-function: {@code decompileFunction(function)} */
    @Function(name = "decompileFunction", arity = 1)
    public Object decompileFunction(Object function) {
        if (!(function instanceof Callable)) {
            return UNDEFINED;
        }
        return ((Callable) function).toSource();
    }

    /** shell-function: {@code decompileBody(function)} */
    @Function(name = "decompileBody", arity = 1)
    public Object decompileBody(Object function) {
        if (!(function instanceof Callable)) {
            return UNDEFINED;
        }
        String source = ((Callable) function).toSource();
        Parser parser = new Parser("<decompileBody>", 1);
        com.github.anba.es6draft.ast.Script parsedScript = parser.parse("(" + source + ")");
        ExpressionStatement expr = (ExpressionStatement) parsedScript.getStatements().get(0);
        FunctionNode fnode = (FunctionNode) expr.getExpression();
        return fnode.getBodySource();
    }
}

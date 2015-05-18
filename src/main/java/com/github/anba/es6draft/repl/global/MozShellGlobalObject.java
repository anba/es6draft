/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import static com.github.anba.es6draft.repl.SourceBuilder.ToSource;
import static com.github.anba.es6draft.repl.global.WrapperProxy.CreateWrapProxy;
import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.objects.FunctionPrototype;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.objects.collection.WeakMapObject;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ProxyObject;

/**
 * Global object class with support for some moz-shell functions
 */
public class MozShellGlobalObject extends ShellGlobalObject {
    private static final String LEGACY_SCRIPT = "mozlegacy.js";
    private final long startMilli = System.currentTimeMillis();
    private final long startNano = System.nanoTime();

    public MozShellGlobalObject(Realm realm, ShellConsole console, Path baseDir, Path script,
            ScriptCache scriptCache) {
        super(realm, console, baseDir, script, scriptCache);
    }

    @Override
    protected void initializeExtensions() {
        super.initializeExtensions();
        install(this, MozShellGlobalObject.class);
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
    public static ObjectAllocator<MozShellGlobalObject> newGlobalObjectAllocator(
            final ShellConsole console, final Path baseDir, final Path script,
            final ScriptCache scriptCache) {
        return new ObjectAllocator<MozShellGlobalObject>() {
            @Override
            public MozShellGlobalObject newInstance(Realm realm) {
                return new MozShellGlobalObject(realm, console, baseDir, script, scriptCache);
            }
        };
    }

    @Override
    public void initializeScripted() throws IOException, URISyntaxException, ParserException,
            CompilationException {
        includeNative(LEGACY_SCRIPT);
    }

    private Object evaluate(Realm realm, Source source, String sourceCode) {
        try {
            Script script = getScriptLoader().script(source, sourceCode);
            return eval(script, realm);
        } catch (ParserException | CompilationException e) {
            // Create a script exception from the requested code realm, not from the caller's realm!
            throw e.toScriptException(realm.defaultContext());
        }
    }

    /**
     * shell-function: {@code loadRelativeToScript(filename)}
     * 
     * @param cx
     *            the execution context
     * @param caller
     *            the caller execution context
     * @param filename
     *            the file path
     * @return the result value
     *
     */
    @Function(name = "loadRelativeToScript", arity = 1)
    public Object loadRelativeToScript(ExecutionContext cx, ExecutionContext caller, String filename) {
        return load(cx, Paths.get(filename), relativePathToScript(caller, Paths.get(filename)));
    }

    /**
     * shell-function: {@code evaluate(code, [options])}
     *
     * @param cx
     *            the execution context
     * @param caller
     *            the caller context
     * @param code
     *            the source code to evaluate
     * @param options
     *            additional options object
     * @return the eval result value
     */
    @Function(name = "evaluate", arity = 2)
    public Object evaluate(ExecutionContext cx, ExecutionContext caller, Object code, Object options) {
        if (!(Type.isString(code) && (Type.isUndefined(options) || Type.isObject(options)))) {
            throw newError(cx, "invalid arguments");
        }

        String sourceCode = Type.stringValue(code).toString();
        String sourceName = "@evaluate";
        int sourceLine = 1;
        boolean noScriptRval = false;
        boolean catchTermination = false;
        GlobalObject global = this;
        if (Type.isObject(options)) {
            ScriptObject opts = Type.objectValue(options);
            Object fileName = Get(cx, opts, "fileName");
            if (!Type.isUndefined(fileName)) {
                sourceName = Type.isNull(fileName) ? "" : ToFlatString(cx, fileName);
            }
            Object lineNumber = Get(cx, opts, "lineNumber");
            if (!Type.isUndefined(lineNumber)) {
                sourceLine = ToInt32(cx, lineNumber);
            }
            Object g = Get(cx, opts, "global");
            if (!Type.isUndefined(g)) {
                ScriptObject obj = ToObject(cx, g);
                if (!(obj instanceof GlobalObject)) {
                    throw newError(cx, "invalid global argument");
                }
                global = (GlobalObject) obj;
            }
            noScriptRval = ToBoolean(Get(cx, opts, "noScriptRval"));
            catchTermination = ToBoolean(Get(cx, opts, "catchTermination"));
        }

        try {
            Source source = new Source(cx.getRealm().sourceInfo(caller), sourceName, sourceLine);
            Object result = evaluate(global.getRealm(), source, sourceCode);
            return (!noScriptRval ? result : UNDEFINED);
        } catch (ScriptException | StackOverflowError e) {
            throw e;
        } catch (Error | Exception e) {
            if (catchTermination) {
                return "terminated";
            }
            throw newError(cx, e.getMessage());
        }
    }

    /**
     * shell-function: {@code run(file)}
     *
     * @param cx
     *            the execution context
     * @param file
     *            the file to evaluate
     * @return the execution time in milli-seconds
     */
    @Function(name = "run", arity = 1)
    public double run(ExecutionContext cx, String file) {
        long start = System.nanoTime();
        load(cx, file);
        long end = System.nanoTime();
        return (double) TimeUnit.NANOSECONDS.toMillis(end - start);
    }

    /**
     * shell-function: {@code printErr(message)}
     *
     * @param message
     *            the message to write
     */
    @Function(name = "printErr", arity = 1)
    public void printErr(String message) {
        console.printErr(message);
    }

    /**
     * shell-function: {@code putstr(message)}
     *
     * @param message
     *            the message to write
     */
    @Function(name = "putstr", arity = 1)
    public void putstr(String message) {
        console.putstr(message);
    }

    /**
     * shell-function: {@code dateNow()}
     *
     * @return the current date in micro-seconds resolution
     */
    @Function(name = "dateNow", arity = 0)
    public double dateNow() {
        long elapsed = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startNano);
        double date = startMilli + TimeUnit.MICROSECONDS.toMillis(elapsed);
        double subdate = (elapsed % 1000) / 1000d;
        return date + subdate;
    }

    /**
     * shell-function: {@code assertEq()}
     *
     * @param cx
     *            the execution context
     * @param actual
     *            the actual value
     * @param expected
     *            the expected value
     * @param message
     *            the optional error message
     */
    @Function(name = "assertEq", arity = 2)
    public void assertEq(ExecutionContext cx, Object actual, Object expected, Object message) {
        if (!SameValue(actual, expected)) {
            StringBuilder msg = new StringBuilder();
            msg.append(String.format("Assertion failed: got %s, expected %s", ToSource(cx, actual),
                    ToSource(cx, expected)));
            if (!Type.isUndefined(message)) {
                msg.append(": ").append(ToFlatString(cx, message));
            }
            throw newError(cx, msg.toString());
        }
    }

    /**
     * shell-function: {@code throwError()}
     * 
     * @param cx
     *            the execution context
     */
    @Function(name = "throwError", arity = 0)
    public void throwError(ExecutionContext cx) {
        throw newError(cx, "This is an error");
    }

    /**
     * shell-function: {@code build()}
     *
     * @param cx
     *            the execution context
     * @return the build identifier string
     */
    @Function(name = "build", arity = 0)
    public String build(ExecutionContext cx) {
        return getResourceInfo("/build-date", "<unknown build>");
    }

    /**
     * shell-function: {@code evalcx(s, [o])}
     *
     * @param cx
     *            the execution context
     * @param caller
     *            the caller context
     * @param s
     *            the source to evaluate
     * @param o
     *            the global object
     * @return the eval result value
     */
    @Function(name = "evalcx", arity = 1)
    public Object evalcx(ExecutionContext cx, ExecutionContext caller, String s, Object o) {
        ScriptObject global;
        if (Type.isUndefinedOrNull(o)) {
            global = newGlobal(cx);
        } else {
            global = ToObject(cx, o);
        }
        if (s.isEmpty() || "lazy".equals(s)) {
            return global;
        }
        if (!(global instanceof GlobalObject)) {
            throw newError(cx, "invalid global argument");
        }
        Source source = new Source(cx.getRealm().sourceInfo(caller), "evalcx", 1);
        return evaluate(((GlobalObject) global).getRealm(), source, s);
    }

    /**
     * shell-function: {@code sleep(dt)}
     *
     * @param cx
     *            the execution context
     * @param dt
     *            the number of seconds to pause the application
     */
    @Function(name = "sleep", arity = 1)
    public void sleep(ExecutionContext cx, double dt) {
        try {
            TimeUnit.SECONDS.sleep(ToUint32(dt));
        } catch (InterruptedException e) {
            throw newError(cx, e.getMessage());
        }
    }

    /**
     * shell-function: {@code snarf(filename)}
     *
     * @param cx
     *            the execution context
     * @param filename
     *            the file path
     * @return the file content
     */
    @Function(name = "snarf", arity = 1)
    public String snarf(ExecutionContext cx, String filename) {
        return read(cx, filename);
    }

    /**
     * shell-function: {@code readRelativeToScript(filename)}
     *
     * @param cx
     *            the execution context
     * @param caller
     *            the caller context
     * @param filename
     *            the file path
     * @return the file content
     */
    @Function(name = "readRelativeToScript", arity = 1)
    public String readRelativeToScript(ExecutionContext cx, ExecutionContext caller, String filename) {
        return read(cx, Paths.get(filename), relativePathToScript(caller, Paths.get(filename)));
    }

    /**
     * shell-function: {@code elapsed()}
     * 
     * @return the micro-seconds elapsed since application start-up
     */
    @Function(name = "elapsed", arity = 0)
    public double elapsed() {
        return (double) TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startNano);
    }

    /**
     * shell-function: {@code decompileFunction(function)}
     *
     * @param function
     *            the function object
     * @return the function source string
     */
    @Function(name = "decompileFunction", arity = 1)
    public Object decompileFunction(Object function) {
        if (!(function instanceof Callable)) {
            return UNDEFINED;
        }
        return ((Callable) function).toSource(Callable.SourceSelector.Function);
    }

    /**
     * shell-function: {@code decompileBody(function)}
     *
     * @param cx
     *            the execution context
     * @param function
     *            the function object
     * @return the function body source
     */
    @Function(name = "decompileBody", arity = 1)
    public Object decompileBody(ExecutionContext cx, Object function) {
        if (!(function instanceof Callable)) {
            return UNDEFINED;
        }
        return ((Callable) function).toSource(Callable.SourceSelector.Body);
    }

    /**
     * shell-function: {@code wrap(obj)}
     *
     * @param cx
     *            the execution context
     * @param obj
     *            the proxy target object
     * @return the new proxy object
     */
    @Function(name = "wrap", arity = 1)
    public Object wrap(ExecutionContext cx, Object obj) {
        if (!Type.isObject(obj)) {
            return obj;
        }
        return CreateWrapProxy(cx, obj);
    }

    /**
     * shell-function: {@code wrapWithProto(obj, proto)}
     *
     * @param cx
     *            the execution context
     * @param obj
     *            the proxy target object
     * @param proto
     *            the proxy prototype object
     * @return the new proxy object
     */
    @Function(name = "wrapWithProto", arity = 2)
    public WrapperProxy wrapWithProto(ExecutionContext cx, Object obj, Object proto) {
        return CreateWrapProxy(cx, obj, proto);
    }

    /**
     * shell-function: {@code newGlobal()}
     *
     * @param cx
     *            the execution context
     * @return a new global object instance
     */
    @Function(name = "newGlobal", arity = 0)
    public ScriptObject newGlobal(ExecutionContext cx) {
        MozShellGlobalObject global;
        try {
            global = (MozShellGlobalObject) cx.getRealm().getWorld().newInitializedGlobal();
        } catch (ParserException | CompilationException e) {
            throw e.toScriptException(cx);
        } catch (IOException | URISyntaxException e) {
            throw newError(cx, e.getMessage());
        }
        return global.getRealm().getGlobalThis();
    }

    /**
     * shell-function: {@code getMaxArgs()}
     *
     * @return the maximum number of allowed function arguments
     */
    @Function(name = "getMaxArgs", arity = 0)
    public int getMaxArgs() {
        return FunctionPrototype.getMaxArguments();
    }

    /**
     * shell-function: {@code isProxy(p)}
     * 
     * @param p
     *            the proxy object
     * @return {@code true} if <var>p</var> is a proxy object
     */
    @Function(name = "isProxy", arity = 1)
    public boolean isProxy(Object p) {
        return (p instanceof ProxyObject || p instanceof WrapperProxy);
    }

    /**
     * shell-function: {@code parse(source)}
     * 
     * @param cx
     *            the execution context
     * @param source
     *            the source string to parse
     */
    @Function(name = "parse", arity = 1)
    public void parse(ExecutionContext cx, String source) {
        try {
            cx.getRealm().getScriptLoader().parseScript(new Source("<script>", 1), source);
        } catch (ParserException e) {
            throw e.toScriptException(cx);
        }
    }

    /**
     * shell-function: {@code thisFilename()}
     * 
     * @param cx
     *            the execution context
     * @param caller
     *            the caller context
     * @return the current file name
     */
    @Function(name = "thisFilename", arity = 0)
    public String thisFilename(ExecutionContext cx, ExecutionContext caller) {
        Source source = cx.getRealm().sourceInfo(caller);
        if (source == null) {
            return "";
        }
        if (source.getFile() != null) {
            return source.getFile().toString();
        }
        return source.getName();
    }

    /**
     * shell-property: {@code scriptArgs}
     * 
     * @param cx
     *            the execution context
     * @return the array of arguments passed to the script
     */
    @Value(name = "scriptArgs")
    public Object scriptArgs(ExecutionContext cx) {
        return Get(cx, cx.getGlobalObject(), "arguments");
    }

    /**
     * shell-function: {@code objectAddress(object)}
     * 
     * @param cx
     *            the execution context
     * @param object
     *            the script object
     * @return the object's address
     */
    @Function(name = "objectAddress", arity = 1)
    public String objectAddress(ExecutionContext cx, ScriptObject object) {
        return Integer.toString(System.identityHashCode(object), 16);
    }

    /**
     * shell-function: {@code nondeterministicGetWeakMapKeys(weakMap)}
     * 
     * @param cx
     *            the execution context
     * @param weakMap
     *            the weak map
     * @return the object's address
     */
    @Function(name = "nondeterministicGetWeakMapKeys", arity = 1)
    public ScriptObject nondeterministicGetWeakMapKeys(ExecutionContext cx, WeakMapObject weakMap) {
        return CreateArrayFromList(cx, new ArrayList<>(weakMap.getWeakMapData().keySet()));
    }
}

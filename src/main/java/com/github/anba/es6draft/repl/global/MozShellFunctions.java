/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import static com.github.anba.es6draft.repl.SourceBuilder.ToSource;
import static com.github.anba.es6draft.repl.global.SharedFunctions.absolutePath;
import static com.github.anba.es6draft.repl.global.SharedFunctions.loadScript;
import static com.github.anba.es6draft.repl.global.SharedFunctions.readFile;
import static com.github.anba.es6draft.repl.global.SharedFunctions.relativePathToScript;
import static com.github.anba.es6draft.repl.global.WrapperProxy.CreateWrapProxy;
import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.github.anba.es6draft.Executable;
import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.DebugInfo;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.objects.FunctionPrototype;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.objects.collection.WeakMapObject;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BoundFunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.ProxyObject;

/**
 * Built-in functions for the mozilla-shell.
 */
public final class MozShellFunctions {
    private final long startMilli = System.currentTimeMillis();
    private final long startNano = System.nanoTime();

    /**
     * shell-function: {@code loadRelativeToScript(filename)}
     * 
     * @param cx
     *            the execution context
     * @param caller
     *            the caller execution context
     * @param fileName
     *            the file path
     */
    @Function(name = "loadRelativeToScript", arity = 1)
    public void loadRelativeToScript(ExecutionContext cx, ExecutionContext caller, String fileName) {
        Path file = Paths.get(fileName);
        loadScript(cx, file, relativePathToScript(cx, caller, file));
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
            throw Errors.newError(cx, "invalid arguments");
        }

        String sourceCode = Type.stringValue(code).toString();
        String sourceName = "@evaluate";
        int sourceLine = 1;
        boolean noScriptRval = false;
        boolean catchTermination = false;
        GlobalObject global = cx.getRealm().getGlobalObject();
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
                    throw Errors.newError(cx, "invalid global argument");
                }
                global = (GlobalObject) obj;
            }
            noScriptRval = ToBoolean(Get(cx, opts, "noScriptRval"));
            catchTermination = ToBoolean(Get(cx, opts, "catchTermination"));
        }

        Source source = new Source(cx.getRealm().sourceInfo(caller), sourceName, sourceLine);
        Realm realm = global.getRealm();
        try {
            Script script = realm.getScriptLoader().script(source, sourceCode);
            Object result = script.evaluate(realm);
            return (!noScriptRval ? result : UNDEFINED);
        } catch (ParserException | CompilationException e) {
            // Create a script exception from the requested code realm, not from the caller's realm.
            throw e.toScriptException(realm.defaultContext());
        } catch (ScriptException | StackOverflowError e) {
            throw e;
        } catch (Error | Exception e) {
            if (catchTermination) {
                return "terminated";
            }
            throw Errors.newError(cx, Objects.toString(e.getMessage(), ""));
        }
    }

    /**
     * shell-function: {@code run(file)}
     *
     * @param cx
     *            the execution context
     * @param fileName
     *            the file to evaluate
     * @return the execution time in milli-seconds
     */
    @Function(name = "run", arity = 1)
    public double run(ExecutionContext cx, String fileName) {
        long start = System.nanoTime();
        Path file = Paths.get(fileName);
        loadScript(cx, file, absolutePath(cx, file));
        long end = System.nanoTime();
        return (double) TimeUnit.NANOSECONDS.toMillis(end - start);
    }

    /**
     * shell-function: {@code printErr(message)}
     *
     * @param cx
     *            the execution context
     * @param message
     *            the message to write
     */
    @Function(name = "printErr", arity = 1)
    public void printErr(ExecutionContext cx, String message) {
        PrintWriter errorWriter = cx.getRuntimeContext().getErrorWriter();
        errorWriter.println(message);
    }

    /**
     * shell-function: {@code putstr(message)}
     *
     * @param cx
     *            the execution context
     * @param message
     *            the message to write
     */
    @Function(name = "putstr", arity = 1)
    public void putstr(ExecutionContext cx, String message) {
        PrintWriter writer = cx.getRuntimeContext().getWriter();
        writer.print(message);
        writer.flush();
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
            throw Errors.newError(cx, msg.toString());
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
        throw Errors.newError(cx, "This is an error");
    }

    /**
     * shell-function: {@code evalcx(s, [o])}
     *
     * @param cx
     *            the execution context
     * @param caller
     *            the caller context
     * @param sourceCode
     *            the source to evaluate
     * @param o
     *            the global object
     * @return the eval result value
     */
    @Function(name = "evalcx", arity = 1)
    public Object evalcx(ExecutionContext cx, ExecutionContext caller, String sourceCode, Object o) {
        ScriptObject global;
        if (Type.isUndefinedOrNull(o)) {
            global = newGlobal(cx);
        } else {
            global = ToObject(cx, o);
        }
        if (sourceCode.isEmpty() || "lazy".equals(sourceCode)) {
            return global;
        }
        if (!(global instanceof GlobalObject)) {
            throw Errors.newError(cx, "invalid global argument");
        }
        Source source = new Source(cx.getRealm().sourceInfo(caller), "evalcx", 1);
        Realm realm = ((GlobalObject) global).getRealm();
        try {
            Script script = realm.getScriptLoader().script(source, sourceCode);
            return script.evaluate(realm);
        } catch (ParserException | CompilationException e) {
            // Create a script exception from the requested code realm, not from the caller's realm.
            throw e.toScriptException(realm.defaultContext());
        }
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
            throw Errors.newError(cx, Objects.toString(e.getMessage(), ""));
        }
    }

    /**
     * shell-function: {@code snarf(filename)}
     *
     * @param cx
     *            the execution context
     * @param fileName
     *            the file path
     * @return the file content
     */
    @Function(name = "snarf", arity = 1)
    public String snarf(ExecutionContext cx, String fileName) {
        Path file = Paths.get(fileName);
        return readFile(cx, file, absolutePath(cx, file));
    }

    /**
     * shell-function: {@code readRelativeToScript(filename)}
     *
     * @param cx
     *            the execution context
     * @param caller
     *            the caller context
     * @param fileName
     *            the file path
     * @return the file content
     */
    @Function(name = "readRelativeToScript", arity = 1)
    public String readRelativeToScript(ExecutionContext cx, ExecutionContext caller, String fileName) {
        Path file = Paths.get(fileName);
        return readFile(cx, file, relativePathToScript(cx, caller, file));
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
     * @param cx
     *            the execution context
     * @param function
     *            the function object
     * @return the function source string
     */
    @Function(name = "decompileFunction", arity = 1)
    public Object decompileFunction(ExecutionContext cx, Object function) {
        if (!(function instanceof FunctionObject || function instanceof BuiltinFunction
                || function instanceof BoundFunctionObject)) {
            return UNDEFINED;
        }
        return ((Callable) function).toSource(cx);
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
        Realm realm;
        try {
            realm = cx.getRealm().getWorld().newInitializedRealm();
        } catch (IOException | URISyntaxException e) {
            throw Errors.newError(cx, Objects.toString(e.getMessage(), ""));
        }
        return realm.getGlobalThis();
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
        cx.getRealm().getScriptLoader().parseScript(new Source("<script>", 1), source);
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
        return CreateArrayFromList(cx, weakMap.getWeakMapData().keySet());
    }

    /**
     * shell-function: {@code dis([function])}
     * 
     * @param cx
     *            the execution context
     * @param caller
     *            the caller context
     * @param args
     *            the arguments
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if the module name cannot be normalized
     */
    @Function(name = "dis", arity = 1)
    public void dis(ExecutionContext cx, ExecutionContext caller, Object... args)
            throws IOException, MalformedNameException {
        DebugInfo debugInfo = debugInfo(caller, args);
        if (debugInfo != null) {
            PrintWriter writer = cx.getRuntimeContext().getWriter();
            for (DebugInfo.Method method : debugInfo.getMethods()) {
                writer.println(method.disassemble());
                writer.flush();
            }
        }
    }

    /**
     * shell-function: {@code disassemble([function])}
     * 
     * @param cx
     *            the execution context
     * @param caller
     *            the caller context
     * @param args
     *            the arguments
     * @return the disassembled byte code
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if the module name cannot be normalized
     */
    @Function(name = "disassemble", arity = 1)
    public String disassemble(ExecutionContext cx, ExecutionContext caller, Object... args)
            throws IOException, MalformedNameException {
        DebugInfo debugInfo = debugInfo(caller, args);
        if (debugInfo != null) {
            StringBuilder sb = new StringBuilder();
            for (DebugInfo.Method method : debugInfo.getMethods()) {
                sb.append(method.disassemble()).append('\n');
            }
            return sb.toString();
        }
        return "";
    }

    private static DebugInfo debugInfo(ExecutionContext caller, Object... args) {
        if (args.length == 0) {
            FunctionObject currentFunction = caller.getCurrentFunction();
            Executable currentExec = caller.getCurrentExecutable();
            if (currentFunction != null && currentFunction.getExecutable() == currentExec) {
                return currentFunction.getCode().debugInfo();
            } else if (currentExec != null && currentExec.getSourceObject() != null) {
                return currentExec.getSourceObject().debugInfo();
            }
        } else if (args[0] instanceof FunctionObject) {
            return ((FunctionObject) args[0]).getCode().debugInfo();
        }
        return null;
    }
}

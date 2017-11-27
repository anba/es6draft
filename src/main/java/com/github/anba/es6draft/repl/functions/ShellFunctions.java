/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.functions;

import static com.github.anba.es6draft.repl.SourceBuilder.ToSource;
import static com.github.anba.es6draft.repl.functions.SharedFunctions.absolutePath;
import static com.github.anba.es6draft.repl.functions.SharedFunctions.loadScript;
import static com.github.anba.es6draft.repl.functions.SharedFunctions.readFile;
import static com.github.anba.es6draft.repl.functions.SharedFunctions.relativePathToScript;
import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.modules.ModuleSemantics.GetModuleNamespace;
import static com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord.ParseModule;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.DetachArrayBuffer;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.github.anba.es6draft.Executable;
import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.compiler.CompiledScript;
import com.github.anba.es6draft.compiler.assembler.Code;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.DebugInfo;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.AliasFunction;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ModuleSource;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;
import com.github.anba.es6draft.runtime.modules.loader.StringModuleSource;
import com.github.anba.es6draft.runtime.objects.ErrorObject;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBufferObject;
import com.github.anba.es6draft.runtime.objects.collection.WeakMapObject;
import com.github.anba.es6draft.runtime.objects.reflect.RealmObject;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 * Built-in functions for the default shell.
 */
public final class ShellFunctions {
    private final long startMilli = System.currentTimeMillis();
    private final long startNano = System.nanoTime();

    private static String getResourceInfo(String resourceName, String defaultValue) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                ShellFunctions.class.getResourceAsStream(resourceName), StandardCharsets.UTF_8))) {
            return reader.readLine();
        } catch (IOException e) {
            return defaultValue;
        }
    }

    /**
     * shell-function: {@code parseModule(sourceCode)}
     *
     * @param cx
     *            the execution context
     * @param sourceCode
     *            the source string to compile
     * @return the status message
     */
    @Function(name = "parseModule", arity = 1)
    public String parseModule(ExecutionContext cx, String sourceCode) {
        Source source = new Source("<module>", 1);
        cx.getRealm().getScriptLoader().parseModule(source, sourceCode);
        return "success";
    }

    /**
     * shell-function: {@code parseScript(sourceCode)}
     * 
     * @param cx
     *            the execution context
     * @param sourceCode
     *            the source string to compile
     * @return the status message
     */
    @Function(name = "parseScript", arity = 1)
    public String parseScript(ExecutionContext cx, String sourceCode) {
        Source source = new Source("<script>", 1);
        cx.getRealm().getScriptLoader().parseScript(source, sourceCode);
        return "success";
    }

    /**
     * shell-function: {@code compile(filename)}
     *
     * @param cx
     *            the execution context
     * @param filename
     *            the file to load
     * @return the status message
     */
    @Function(name = "compile", arity = 1)
    public String compile(ExecutionContext cx, String filename) {
        try {
            Path file = absolutePath(cx, Paths.get(filename));
            cx.getRealm().getScriptLoader().script(new Source(file, filename, 1), file);
        } catch (ParserException | CompilationException | IOException e) {
            return "error: " + e.getMessage();
        }
        return "success";
    }

    /**
     * shell-function: {@code loadRelativeToScript(filename)}
     * 
     * @param cx
     *            the execution context
     * @param caller
     *            the caller execution context
     * @param filename
     *            the file to load
     */
    @Function(name = "loadRelativeToScript", arity = 1)
    public void loadRelativeToScript(ExecutionContext cx, ExecutionContext caller, String filename) {
        Path file = Paths.get(filename);
        loadScript(cx, file, relativePathToScript(cx, caller, file));
    }

    /**
     * shell-function: {@code readRelativeToScript(filename)}
     * 
     * @param cx
     *            the execution context
     * @param caller
     *            the caller execution context
     * @param filename
     *            the file to load
     * @return the result value
     */
    @Function(name = "readRelativeToScript", arity = 1)
    public Object readRelativeToScript(ExecutionContext cx, ExecutionContext caller, String filename) {
        Path file = Paths.get(filename);
        return readFile(cx, file, relativePathToScript(cx, caller, file));
    }

    /**
     * shell-function: {@code loadModule(moduleName, [realmObject])}
     * 
     * @param cx
     *            the execution context
     * @param moduleName
     *            the module name
     * @param realmObject
     *            the optional realm object
     * @return the module namespace object
     * @throws MalformedNameException
     *             if any imported module request cannot be normalized
     * @throws ResolutionException
     *             if any export binding cannot be resolved
     */
    @Function(name = "loadModule", arity = 1)
    public ScriptObject loadModule(ExecutionContext cx, String moduleName, Object realmObject)
            throws MalformedNameException, ResolutionException {
        Realm realm;
        if (!Type.isUndefined(realmObject)) {
            if (!(realmObject instanceof RealmObject)) {
                throw Errors.newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            realm = ((RealmObject) realmObject).getRealm();
        } else {
            realm = cx.getRealm();
        }
        try {
            ModuleLoader moduleLoader = realm.getModuleLoader();
            SourceIdentifier moduleId = moduleLoader.normalizeName(moduleName, null);
            ModuleRecord module = moduleLoader.resolve(moduleId, realm);
            module.instantiate();
            module.evaluate();
            return GetModuleNamespace(cx, module);
        } catch (IOException e) {
            throw Errors.newInternalError(cx, e, Messages.Key.ModulesIOException, e.getMessage());
        }
    }

    /**
     * shell-function: {@code dump(object)}
     * 
     * @param cx
     *            the execution context
     * @param object
     *            the object to inspect
     */
    @Function(name = "dump", arity = 1)
    public void dump(ExecutionContext cx, Object object) {
        String id;
        if (!Type.isType(object)) {
            // foreign object or null
            id = Objects.toString(object, "<null>");
        } else if (!Type.isObject(object)) {
            // primitive value
            id = String.format("%s (%s)", object.toString(), object.getClass().getSimpleName());
        } else {
            // script objects
            id = String.format("%s@%x", object.getClass().getSimpleName(), System.identityHashCode(object));
        }
        PrintWriter writer = cx.getRuntimeContext().getConsole().writer();
        writer.println(id);
    }

    /**
     * shell-function: {@code dumpObject(object)}
     * 
     * @param cx
     *            the execution context
     * @param object
     *            the object to inspect
     */
    @Function(name = "dumpObject", arity = 1)
    public void dumpObject(ExecutionContext cx, ScriptObject object) {
        PrintWriter writer = cx.getRuntimeContext().getConsole().writer();
        writer.println(object.toString());
    }

    /**
     * shell-function: {@code dumpScope()}
     * 
     * @param cx
     *            the execution context
     * @param caller
     *            the caller context
     */
    @Function(name = "dumpScope", arity = 0)
    public void dumpScope(ExecutionContext cx, ExecutionContext caller) {
        if (caller.getLexicalEnvironment() != null) {
            PrintWriter writer = cx.getRuntimeContext().getConsole().writer();
            writer.println(caller.getLexicalEnvironment().toString());
        }
    }

    /**
     * shell-function: {@code dumpSymbolRegistry()}
     * 
     * @param cx
     *            the execution context
     */
    @Function(name = "dumpSymbolRegistry", arity = 0)
    public void dumpSymbolRegistry(ExecutionContext cx) {
        PrintWriter writer = cx.getRuntimeContext().getConsole().writer();
        writer.println(cx.getRealm().getSymbolRegistry().toString());
    }

    /**
     * shell-function: {@code dumpTemplateMap()}
     * 
     * @param cx
     *            the execution context
     */
    @Function(name = "dumpTemplateMap", arity = 0)
    public void dumpTemplateMap(ExecutionContext cx) {
        PrintWriter writer = cx.getRuntimeContext().getConsole().writer();
        writer.println(cx.getRealm().getTemplateMap().toString());
    }

    /**
     * shell-function: {@code gc()}
     */
    @Function(name = "gc", arity = 0)
    public void gc() {
        System.gc();
    }

    /**
     * shell-function: {@code runFinalization()}
     */
    @Function(name = "runFinalization", arity = 0)
    public void runFinalization() {
        System.runFinalization();
    }

    /**
     * shell-function: {@code enqueueJob(queueName, job, ...arguments)}
     * 
     * @param cx
     *            the execution context
     * @param queueName
     *            the job queue name
     * @param job
     *            the job function
     * @param arguments
     *            the optional arguments
     */
    @Function(name = "enqueueJob", arity = 2)
    public void enqueueJob(ExecutionContext cx, String queueName, Callable job, Object... arguments) {
        switch (queueName) {
        case "script":
            cx.getRealm().enqueueScriptJob(() -> job.call(cx, UNDEFINED, arguments));
            break;
        case "promise":
            cx.getRealm().enqueuePromiseJob(() -> job.call(cx, UNDEFINED, arguments));
            break;
        case "finalizer":
            cx.getRealm().enqueueFinalizerJob(() -> job.call(cx, UNDEFINED, arguments));
            break;
        case "async":
            cx.getRealm().enqueueAsyncJob(() -> job.call(cx, UNDEFINED, arguments));
            break;
        default:
            throw Errors.newError(cx, "invalid queue-name: " + queueName);
        }
    }

    /**
     * shell-function: {@code error()}
     */
    @Function(name = "error", arity = 0)
    public void error() {
        throw new AssertionError();
    }

    /**
     * shell-function: {@code printStackTrace(object)}
     * 
     * @param object
     *            the error object
     */
    @Function(name = "printStackTrace", arity = 1)
    public void printStackTrace(ErrorObject object) {
        object.getException().printStackTrace();
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
        PrintWriter writer = cx.getRuntimeContext().getConsole().writer();
        writer.print(message);
        writer.flush();
    }

    /**
     * shell-function: {@code nextTick(function)}
     * 
     * @param cx
     *            the execution context
     * @param function
     *            the callback function
     */
    @Function(name = "nextTick", arity = 1)
    public void nextTick(ExecutionContext cx, Callable function) {
        cx.getRealm().enqueuePromiseJob(() -> function.call(cx, UNDEFINED));
    }

    /**
     * shell-function: {@code detachArrayBuffer(arrayBuffer)}
     * 
     * @param cx
     *            the execution context
     * @param arrayBuffer
     *            the array buffer object
     */
    @Function(name = "detachArrayBuffer", arity = 1)
    public void detachArrayBuffer(ExecutionContext cx, ArrayBufferObject arrayBuffer) {
        DetachArrayBuffer(cx, arrayBuffer);
    }

    /**
     * shell-function: {@code weakMapSize(weakMap)}
     * 
     * @param weakMap
     *            the WeakMap object
     * @return the WeakMap's current size
     */
    @Function(name = "weakMapSize", arity = 1)
    public int weakMapSize(WeakMapObject weakMap) {
        return weakMap.getWeakMapData().size();
    }

    /**
     * shell-function: {@code version()}
     *
     * @return the version string
     */
    @Function(name = "version", arity = 0)
    public String version() {
        return getResourceInfo("/version", "<unknown version>");
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
     * @throws IOException
     *             if there was any I/O error
     * @throws MalformedNameException
     *             if the module name cannot be normalized
     */
    @Function(name = "disassemble", arity = 1)
    @AliasFunction(name = "dis")
    public void disassemble(ExecutionContext cx, ExecutionContext caller, Object... args)
            throws IOException, MalformedNameException {
        DebugInfo debugInfo = null;
        if (args.length == 0) {
            FunctionObject currentFunction = caller.getCurrentFunction();
            Executable currentExec = caller.getCurrentExecutable();
            if (currentFunction != null && currentFunction.getExecutable() == currentExec) {
                debugInfo = currentFunction.getCode().debugInfo();
            } else {
                debugInfo = currentExec.getRuntimeObject().debugInfo();
            }
        } else if (args[0] instanceof FunctionObject) {
            debugInfo = ((FunctionObject) args[0]).getCode().debugInfo();
        } else {
            String sourceCode = ToFlatString(cx, args[0]);
            boolean isModule = false;
            if (args.length > 1 && Type.isObject(args[1])) {
                isModule = ToBoolean(Get(cx, Type.objectValue(args[1]), "module"));
            }
            ScriptLoader scriptLoader = cx.getRealm().getScriptLoader();
            if (isModule) {
                ModuleLoader moduleLoader = cx.getRealm().getModuleLoader();
                SourceIdentifier identifier = moduleLoader.normalizeName("disassemble", null);
                ModuleSource moduleSource = new StringModuleSource(identifier, "disassemble", sourceCode);
                SourceTextModuleRecord module = ParseModule(scriptLoader, identifier, moduleSource);
                debugInfo = module.getScriptCode().getRuntimeObject().debugInfo();
            } else {
                Source source = new Source("<disassemble>", 1);
                Script script = scriptLoader.compile(scriptLoader.parseScript(source, sourceCode), "#disassemble");
                debugInfo = script.getRuntimeObject().debugInfo();
            }
        }
        if (debugInfo != null) {
            PrintWriter writer = cx.getRuntimeContext().getConsole().writer();
            for (DebugInfo.Method method : debugInfo.getMethods()) {
                writer.println(method.disassemble());
            }
        }
    }

    /**
     * shell-function: {@code disScript(sourceCode)}
     * 
     * @param cx
     *            the execution context
     * @param sourceCode
     *            the script source code
     * @throws ReflectiveOperationException
     *             if there was an error getting the class bytes
     */
    @Function(name = "disScript", arity = 1)
    public void disScript(ExecutionContext cx, String sourceCode) throws ReflectiveOperationException {
        ScriptLoader scriptLoader = cx.getRealm().getScriptLoader();
        Source source = new Source("<disassemble>", 1);
        CompiledScript script = scriptLoader.compile(scriptLoader.parseScript(source, sourceCode), "#disassemble");
        byte[] classBytes = DebugInfo.classBytes(script.getClass());
        String disassembled = Code.toByteCode(classBytes, true);
        cx.getRuntimeContext().getConsole().writer().println(disassembled);
    }

    /**
     * shell-function: {@code evalScript(sourceString, [options])}
     * 
     * @param cx
     *            the execution context
     * @param caller
     *            the caller execution context
     * @param sourceString
     *            the source string
     * @param options
     *            the options object (optional)
     * @return the evaluation result
     */
    @Function(name = "evalScript", arity = 1)
    public Object evalScript(ExecutionContext cx, ExecutionContext caller, String sourceString, Object options) {
        String name = "";
        int line = 1;
        Realm realm = cx.getRealm();
        if (Type.isObject(options)) {
            ScriptObject opts = Type.objectValue(options);
            Object fileName = Get(cx, opts, "fileName");
            if (!Type.isUndefined(fileName)) {
                name = ToFlatString(cx, fileName);
            }
            Object lineNumber = Get(cx, opts, "lineNumber");
            if (!Type.isUndefined(lineNumber)) {
                line = ToInt32(cx, lineNumber);
            }
            Object g = Get(cx, opts, "global");
            if (!Type.isUndefined(g)) {
                if (!(g instanceof GlobalObject)) {
                    throw Errors.newError(cx, "invalid global argument");
                }
                realm = ((GlobalObject) g).getRealm();
            }
            Object r = Get(cx, opts, "realm");
            if (!Type.isUndefined(r)) {
                if (!(r instanceof RealmObject)) {
                    throw Errors.newError(cx, "invalid realm argument");
                }
                realm = ((RealmObject) r).getRealm();
            }
        }
        Source source = new Source(caller.sourceInfo(), name, line);
        Script script = realm.getScriptLoader().script(source, sourceString);
        return script.evaluate(realm);
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
            realm = Realm.InitializeHostDefinedRealm(cx.getRealm().getWorld());
        } catch (IOException e) {
            throw Errors.newError(cx, Objects.toString(e.getMessage(), ""));
        }
        return realm.getGlobalThis();
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
     * shell-function: {@code getTimeZone()}
     *
     * @param cx
     *            the execution context
     * @return the current time zone
     */
    @Function(name = "getTimeZone", arity = 0)
    public String getTimeZone(ExecutionContext cx) {
        return cx.getRuntimeContext().getTimeZone().getID();
    }

    /**
     * shell-function: {@code setTimeZone(timeZoneId)}
     *
     * @param cx
     *            the execution context
     * @param timeZoneId
     *            the time zone id
     */
    @Function(name = "setTimeZone", arity = 1)
    public void setTimeZone(ExecutionContext cx, String timeZoneId) {
        cx.getRuntimeContext().setTimeZone(TimeZone.getTimeZone(timeZoneId));
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
     * shell-function: {@code drainJobQueue()}
     * 
     * @param cx
     *            the execution context
     */
    @Function(name = "drainJobQueue", arity = 0)
    public void drainJobQueue(ExecutionContext cx) {
        cx.getRealm().getWorld().runEventLoop();
    }
}

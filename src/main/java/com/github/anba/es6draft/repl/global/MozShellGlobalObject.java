/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import static com.github.anba.es6draft.repl.SourceBuilder.ToSource;
import static com.github.anba.es6draft.repl.global.WrapperProxy.CreateWrapProxy;
import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.ast.ExpressionStatement;
import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.objects.FunctionPrototype;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ExoticProxy;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Global object class with support for some moz-shell functions
 */
public final class MozShellGlobalObject extends ShellGlobalObject {
    private final long startMilli = System.currentTimeMillis();
    private final long startNano = System.nanoTime();

    private MozShellGlobalObject(Realm realm, ShellConsole console, Path baseDir, Path script,
            ScriptCache scriptCache) {
        super(realm, console, baseDir, script, scriptCache);
    }

    @Override
    public void defineBuiltinProperties(ExecutionContext cx, OrdinaryObject object) {
        super.defineBuiltinProperties(cx, object);
        createProperties(object, this, cx, MozShellGlobalObject.class);
    }

    /**
     * Returns an object to allocate new instances of this class
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
    protected List<Script> initialisationScripts() throws IOException, ParserException,
            CompilationException {
        List<Script> scripts = super.initialisationScripts();
        scripts.add(compileScript(scriptCache, "mozlegacy.js"));
        return scripts;
    }

    private Object evaluate(GlobalObject global, String source, String sourceName, int sourceLine)
            throws IOException {
        Realm realm = global.getRealm();
        try {
            StringReader reader = new StringReader(source);
            Script script = scriptCache.script(sourceName, sourceLine, reader, realm.getExecutor());
            return ScriptLoader.ScriptEvaluation(script, realm, false);
        } catch (ParserException | CompilationException e) {
            // create a script exception from the requested code realm, not from the caller's realm!
            throw e.toScriptException(realm.defaultContext());
        }
    }

    /** shell-function: {@code version([number])} */
    @Function(name = "version", arity = 1)
    public String version() {
        return "185";
    }

    /** shell-function: {@code options([name])} */
    @Function(name = "options", arity = 0)
    public String options() {
        return "";
    }

    /** shell-function: {@code loadRelativeToScript(filename)} */
    @Function(name = "loadRelativeToScript", arity = 1)
    public Object loadRelativeToScript(ExecutionContext cx, String filename) {
        return load(cx, Paths.get(filename), relativePathToScript(Paths.get(filename)));
    }

    /** shell-function: {@code evaluate(code, [options])} */
    @Function(name = "evaluate", arity = 2)
    public Object evaluate(ExecutionContext cx, Object code, Object options) {
        if (!(Type.isString(code) && (Type.isUndefined(options) || Type.isObject(options)))) {
            throw newError(cx, "invalid arguments");
        }

        String source = Type.stringValue(code).toString();
        String sourceName = "@evaluate";
        int sourceLine = 1;
        boolean noScriptRval = false;
        boolean catchTermination = false;
        GlobalObject global = cx.getRealm().getGlobalThis();
        if (Type.isObject(options)) {
            ScriptObject opts = Type.objectValue(options);

            Object fileName = opts.get(cx, "fileName", opts);
            if (!Type.isUndefined(fileName)) {
                sourceName = Type.isNull(fileName) ? "" : ToFlatString(cx, fileName);
            }
            Object lineNumber = opts.get(cx, "lineNumber", opts);
            if (!Type.isUndefined(lineNumber)) {
                sourceLine = ToInt32(cx, lineNumber);
            }
            Object g = opts.get(cx, "global", opts);
            if (!Type.isUndefined(g)) {
                ScriptObject obj = ToObject(cx, g);
                if (!(obj instanceof GlobalObject)) {
                    throw newError(cx, "invalid global argument");
                }
                global = (GlobalObject) obj;
            }
            noScriptRval = ToBoolean(opts.get(cx, "noScriptRval", opts));
            catchTermination = ToBoolean(opts.get(cx, "catchTermination", opts));
        }

        try {
            Object result = evaluate(global, source, sourceName, sourceLine);
            return (!noScriptRval ? result : UNDEFINED);
        } catch (StackOverflowError e) {
            throw e;
        } catch (IOException | Error e) {
            if (catchTermination) {
                return "terminated";
            }
            throw newError(cx, e.getMessage());
        }
    }

    /** shell-function: {@code run(file)} */
    @Function(name = "run", arity = 1)
    public double run(ExecutionContext cx, String file) {
        long start = System.nanoTime();
        load(cx, file);
        long end = System.nanoTime();
        return (double) TimeUnit.NANOSECONDS.toMillis(end - start);
    }

    /** shell-function: {@code printErr(message)} */
    @Function(name = "printErr", arity = 1)
    public void printErr(String message) {
        console.printErr(message);
    }

    /** shell-function: {@code putstr(message)} */
    @Function(name = "putstr", arity = 1)
    public void putstr(String message) {
        console.putstr(message);
    }

    /** shell-function: {@code dateNow()} */
    @Function(name = "dateNow", arity = 0)
    public double dateNow() {
        long elapsed = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startNano);
        double date = startMilli + TimeUnit.MICROSECONDS.toMillis(elapsed);
        double subdate = (elapsed % 1000) / 1000d;
        return date + subdate;
    }

    /** shell-function: {@code assertEq()} */
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

    /** shell-function: {@code throwError()} */
    @Function(name = "throwError", arity = 0)
    public void throwError(ExecutionContext cx) {
        throw newError(cx, "This is an error");
    }

    /** shell-function: {@code build()} */
    @Function(name = "build", arity = 0)
    public String build(ExecutionContext cx) {
        return getResourceInfo("/build-date", "<unknown build>");
    }

    /** shell-function: {@code evalcx(s, [o])} */
    @Function(name = "evalcx", arity = 1)
    public Object evalcx(ExecutionContext cx, String s, Object o) {
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
        try {
            return evaluate((GlobalObject) global, s, "evalcx", 1);
        } catch (IOException e) {
            throw newError(cx, e.getMessage());
        }
    }

    /** shell-function: {@code sleep(dt)} */
    @Function(name = "sleep", arity = 1)
    public void sleep(ExecutionContext cx, double dt) {
        try {
            TimeUnit.SECONDS.sleep(ToUint32(dt));
        } catch (InterruptedException e) {
            throw newError(cx, e.getMessage());
        }
    }

    /** shell-function: {@code snarf(filename)} */
    @Function(name = "snarf", arity = 1)
    public Object snarf(ExecutionContext cx, String filename) {
        return read(cx, filename);
    }

    /** shell-function: {@code readRelativeToScript(filename)} */
    @Function(name = "readRelativeToScript", arity = 1)
    public Object readRelativeToScript(ExecutionContext cx, String filename) {
        return read(cx, relativePath(Paths.get(filename)));
    }

    /** shell-function: {@code elapsed()} */
    @Function(name = "elapsed", arity = 0)
    public double elapsed() {
        return (double) TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startNano);
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
    public Object decompileBody(ExecutionContext cx, Object function) {
        if (!(function instanceof Callable)) {
            return UNDEFINED;
        }
        String source = ((Callable) function).toSource();
        Parser parser = new Parser("<decompileBody>", 1, cx.getRealm().getOptions());
        com.github.anba.es6draft.ast.Script parsedScript = parser.parseScript("(" + source + ")");
        ExpressionStatement expr = (ExpressionStatement) parsedScript.getStatements().get(0);
        FunctionNode fnode = (FunctionNode) expr.getExpression();
        return fnode.getBodySource();
    }

    /** shell-function: {@code wrap(obj)} */
    @Function(name = "wrap", arity = 1)
    public Object wrap(ExecutionContext cx, Object obj) {
        if (!Type.isObject(obj)) {
            return obj;
        }
        return CreateWrapProxy(cx, obj);
    }

    /** shell-function: {@code wrapWithProto(obj, proto)} */
    @Function(name = "wrapWithProto", arity = 2)
    public Object wrapWithProto(ExecutionContext cx, Object obj, Object proto) {
        return CreateWrapProxy(cx, obj, proto);
    }

    /** shell-function: {@code newGlobal()} */
    @Function(name = "newGlobal", arity = 0)
    public GlobalObject newGlobal(ExecutionContext cx) {
        MozShellGlobalObject global = (MozShellGlobalObject) cx.getRealm().getWorld().newGlobal();
        try {
            global.executeInitialisation();
        } catch (ParserException | CompilationException | IOException e) {
            throw newError(cx, e.getMessage());
        }
        return global;
    }

    /** shell-function: {@code getMaxArgs()} */
    @Function(name = "getMaxArgs", arity = 0)
    public double getMaxArgs() {
        return FunctionPrototype.getMaxArguments();
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

    /** shell-function: {@code getBuildConfiguration()} */
    @Function(name = "getBuildConfiguration", arity = 0)
    public ScriptObject getBuildConfiguration(ExecutionContext cx) {
        return OrdinaryObject.ObjectCreate(cx);
    }

    /** shell-function: {@code isProxy(p)} */
    @Function(name = "isProxy", arity = 1)
    public boolean isProxy(Object p) {
        return (p instanceof ExoticProxy || p instanceof WrapperProxy);
    }

    /** shell-function: {@code terminate()} */
    @Function(name = "terminate", arity = 0)
    public void terminate() {
        throw new StopExecutionException(StopExecutionException.Reason.Terminate);
    }

    /** shell-function: {@code enableOsiPointRegisterChecks()} */
    @Function(name = "enableOsiPointRegisterChecks", arity = 0)
    public void enableOsiPointRegisterChecks() {
        // empty
    }

    /** shell-function: {@code isAsmJSCompilationAvailable()} */
    @Function(name = "isAsmJSCompilationAvailable", arity = 0)
    public boolean isAsmJSCompilationAvailable() {
        return false;
    }
}

/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import static com.github.anba.es6draft.runtime.ExecutionContext.newScriptExecutionContext;

import java.util.EnumSet;

import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.compiler.CompiledFunction;
import com.github.anba.es6draft.compiler.CompiledScript;
import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.interpreter.InterpretedScript;
import com.github.anba.es6draft.interpreter.Interpreter;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;

/**
 * <h1>14 Scripts and Modules</h1>
 * <ul>
 * <li>14.1 Script
 * </ul>
 */
public class ScriptLoader {
    private ScriptLoader() {
    }

    /**
     * [14.1 ScriptEvaluation]
     */
    public static Object ScriptEvaluation(Script script, Realm realm, boolean deletableBindings) {
        /* step 1-2 */
        RuntimeInfo.ScriptBody scriptBody = script.getScriptBody();
        if (scriptBody == null)
            return null;
        /* step 3 */
        LexicalEnvironment globalEnv = realm.getGlobalEnv();
        /* step 4-5 */
        scriptBody.globalDeclarationInstantiation(realm.defaultContext(), globalEnv,
                deletableBindings);
        /* step 6-9 */
        ExecutionContext progCxt = newScriptExecutionContext(realm);
        /* step 10-14 */
        Object result = script.evaluate(progCxt);
        /* step 15 */
        return result;
    }

    /**
     * [14.1 ScriptEvaluation]
     */
    public static Object ScriptEvaluation(Script script, ExecutionContext cx,
            boolean deletableBindings) {
        Realm realm = cx.getRealm();
        /* step 1-2 */
        RuntimeInfo.ScriptBody scriptBody = script.getScriptBody();
        if (scriptBody == null)
            return null;
        /* step 3 */
        LexicalEnvironment globalEnv = realm.getGlobalEnv();
        /* step 4-5 */
        scriptBody.globalDeclarationInstantiation(realm.defaultContext(), globalEnv,
                deletableBindings);
        /* step 6-9 */
        ExecutionContext progCxt = newScriptExecutionContext(cx);
        /* step 10-14 */
        Object result = script.evaluate(progCxt);
        /* step 15 */
        return result;
    }

    /**
     * Returns an executable {@link Script} object for given
     * {@link com.github.anba.es6draft.ast.Script} AST-node. This may either be an
     * {@link InterpretedScript} or {@link CompiledScript} instance.
     */
    public static Script load(String className, com.github.anba.es6draft.ast.Script parsedScript)
            throws CompilationException {
        Script script = Interpreter.script(parsedScript);
        if (script == null) {
            script = compile(className, parsedScript, EnumSet.noneOf(Compiler.Option.class));
        }
        return script;
    }

    /**
     * Compiles the given {@link com.github.anba.es6draft.ast.Script} to an executable
     * {@link Script} object
     */
    public static CompiledScript compile(String className,
            com.github.anba.es6draft.ast.Script parsedScript, EnumSet<Compiler.Option> options)
            throws CompilationException {
        try {
            // prepend '#' to mark generated classes, cf. ErrorPrototype
            String clazzName = "#" + className;
            Compiler compiler = new Compiler(options);
            byte[] bytes = compiler.compile(parsedScript, clazzName);
            ClassLoader cl = new ByteClassLoader(clazzName, bytes);
            Class<?> c = cl.loadClass(clazzName);
            CompiledScript instance = (CompiledScript) c.newInstance();
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Compiles the given {@link FunctionNode} to a {@link RuntimeInfo.Function} object
     */
    public static RuntimeInfo.Function compile(String className, FunctionNode function)
            throws CompilationException {
        try {
            // prepend '#' to mark generated classes, cf. ErrorPrototype
            String clazzName = "#" + className;
            Compiler compiler = new Compiler(EnumSet.noneOf(Compiler.Option.class));
            byte[] bytes = compiler.compile(function, clazzName);
            ClassLoader cl = new ByteClassLoader(clazzName, bytes);
            Class<?> c = cl.loadClass(clazzName);
            CompiledFunction instance = (CompiledFunction) c.newInstance();
            return instance.getFunction();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ByteClassLoader extends ClassLoader {
        private final String className;
        private byte[] bytes;

        public ByteClassLoader(String className, byte[] bytes) {
            this(ClassLoader.getSystemClassLoader(), className, bytes);
        }

        public ByteClassLoader(ClassLoader parent, String className, byte[] bytes) {
            super(parent);
            this.className = className;
            this.bytes = bytes;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (className.equals(name)) {
                byte[] bytes = this.bytes;
                this.bytes = null;
                return this.defineClass(name, bytes, 0, bytes.length);
            }
            return super.findClass(name);
        }
    }
}

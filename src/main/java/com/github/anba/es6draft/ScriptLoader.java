/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import static com.github.anba.es6draft.runtime.ExecutionContext.newScriptExecutionContext;

import java.util.EnumSet;

import com.github.anba.es6draft.compiler.CompiledScript;
import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.interpreter.Interpreter;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
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

    public static Script load(String sourceFile, String className, String source)
            throws ParserException {
        Parser parser = new Parser(sourceFile, 1);
        com.github.anba.es6draft.ast.Script parsedScript = parser.parse(source);
        Script script = compile(className, parsedScript, EnumSet.noneOf(Compiler.Option.class));
        return script;
    }

    public static Script load(String className, com.github.anba.es6draft.ast.Script parsedScript)
            throws ParserException {
        Script script = Interpreter.script(parsedScript);
        if (script == null) {
            script = compile(className, parsedScript, EnumSet.noneOf(Compiler.Option.class));
        }
        return script;
    }

    public static CompiledScript compile(String className,
            com.github.anba.es6draft.ast.Script parsedScript, EnumSet<Compiler.Option> options)
            throws ParserException {
        try {
            Compiler compiler = new Compiler(options);
            byte[] bytes = compiler.compile(parsedScript, className);
            ClassLoader cl = new ByteClassLoader(className, bytes);
            Class<?> c = cl.loadClass(className);
            CompiledScript instance = (CompiledScript) c.newInstance();
            return instance;
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } catch (Throwable e) {
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

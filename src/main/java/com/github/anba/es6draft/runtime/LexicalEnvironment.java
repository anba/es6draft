/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.internal.Errors.newReferenceError;

import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.ThisMode;

/**
 * <h1>8 Executable Code and Execution Contexts</h1><br>
 * <h2>8.1 Lexical Environments</h2>
 * <ul>
 * <li>8.1.2 Lexical Environment Operations
 * </ul>
 */
public final class LexicalEnvironment<RECORD extends EnvironmentRecord> {
    private final ExecutionContext cx;
    private final LexicalEnvironment<?> outer;
    private final RECORD envRec;

    public LexicalEnvironment(ExecutionContext cx, RECORD envRec) {
        this.cx = cx;
        this.outer = null;
        this.envRec = envRec;
    }

    public LexicalEnvironment(LexicalEnvironment<?> outer, RECORD envRec) {
        this.cx = outer.cx;
        this.outer = outer;
        this.envRec = envRec;
    }

    @Override
    public String toString() {
        return String.format("%s: {envRec=%s}", getClass().getSimpleName(), envRec);
    }

    public RECORD getEnvRec() {
        return envRec;
    }

    public LexicalEnvironment<?> getOuter() {
        return outer;
    }

    /**
     * Returns the first {@link EnvironmentRecord} which has a binding for {@code name}.
     * 
     * @param lex
     *            the lexical environment
     * @param name
     *            the identifier name
     * @return the first environment record which has a binding for <var>name</var>
     */
    private static EnvironmentRecord getIdentifierRecord(LexicalEnvironment<?> lex, String name) {
        for (; lex != null; lex = lex.outer) {
            EnvironmentRecord envRec = lex.envRec;
            if (envRec.hasBinding(name)) {
                return envRec;
            }
        }
        return null;
    }

    /**
     * Retrieves the binding value of the first {@link EnvironmentRecord} which has a binding for
     * {@code name}, if no such binding exists a ReferenceError is thrown.
     * 
     * @param lex
     *            the lexical environment
     * @param name
     *            the identifier name
     * @param strict
     *            the strict mode flag
     * @return the resolved identifier value
     */
    static Object getIdentifierValueOrThrow(LexicalEnvironment<?> lex, String name, boolean strict) {
        EnvironmentRecord envRec = getIdentifierRecord(lex, name);
        if (envRec != null) {
            return envRec.getBindingValue(name, strict);
        }
        throw newReferenceError(lex.cx, Messages.Key.UnresolvableReference, name);
    }

    /**
     * Clones the given declarative {@link LexicalEnvironment}.
     * <p>
     * [Called from generated code]
     * 
     * @param e
     *            the source lexical environment
     * @return the cloned lexical environment
     */
    public static LexicalEnvironment<DeclarativeEnvironmentRecord> cloneDeclarativeEnvironment(
            LexicalEnvironment<DeclarativeEnvironmentRecord> e) {
        DeclarativeEnvironmentRecord envRec = new DeclarativeEnvironmentRecord(e.envRec);
        return new LexicalEnvironment<>(e.outer, envRec);
    }

    /**
     * 8.1.2.1 GetIdentifierReference (lex, name, strict) Abstract Operation
     * 
     * @param lex
     *            the lexical environment
     * @param name
     *            the identifier name
     * @param strict
     *            the strict mode flag
     * @return the resolved identifier reference
     */
    public static Reference<EnvironmentRecord, String> getIdentifierReference(
            LexicalEnvironment<?> lex, String name, boolean strict) {
        /* steps 2-3, 5 */
        EnvironmentRecord envRec = getIdentifierRecord(lex, name);
        /* steps 1, 4 */
        return new Reference.IdentifierReference(envRec, name, strict);
    }

    /**
     * 8.1.2.2 NewDeclarativeEnvironment (E) Abstract Operation
     * 
     * @param e
     *            the outer lexical environment
     * @return the new declarative environment
     */
    public static LexicalEnvironment<DeclarativeEnvironmentRecord> newDeclarativeEnvironment(
            LexicalEnvironment<?> e) {
        /* step 2 */
        DeclarativeEnvironmentRecord envRec = new DeclarativeEnvironmentRecord(e.cx);
        /* steps 1, 3-4 */
        LexicalEnvironment<DeclarativeEnvironmentRecord> env = new LexicalEnvironment<>(e, envRec);
        /* step 5 */
        return env;
    }

    /**
     * 8.1.2.3 NewObjectEnvironment (O, E) Abstract Operation
     * 
     * @param o
     *            the script object
     * @param e
     *            the outer lexical environment
     * @return the new object environment
     */
    public static LexicalEnvironment<ObjectEnvironmentRecord> newObjectEnvironment(ScriptObject o,
            LexicalEnvironment<?> e) {
        return newObjectEnvironment(o, e, false);
    }

    /**
     * 8.1.2.3 NewObjectEnvironment (O, E) Abstract Operation
     * 
     * @param o
     *            the script object
     * @param e
     *            the outer lexical environment
     * @param withEnvironment
     *            the withEnvironment flag
     * @return the new object environment
     */
    public static LexicalEnvironment<ObjectEnvironmentRecord> newObjectEnvironment(ScriptObject o,
            LexicalEnvironment<?> e, boolean withEnvironment) {
        /* step 2 */
        ObjectEnvironmentRecord envRec = new ObjectEnvironmentRecord(e.cx, o, withEnvironment);
        /* steps 1, 3-4 */
        LexicalEnvironment<ObjectEnvironmentRecord> env = new LexicalEnvironment<>(e, envRec);
        /* step 5 */
        return env;
    }

    /**
     * 8.1.2.4 NewFunctionEnvironment (F, T) Abstract Operation
     * 
     * @param callerContext
     *            the caller execution context
     * @param f
     *            the function object
     * @param t
     *            the function this-binding
     * @return the new function environment
     */
    public static LexicalEnvironment<FunctionEnvironmentRecord> newFunctionEnvironment(
            ExecutionContext callerContext, FunctionObject f, Object t) {
        /* step 1 */
        assert f.getThisMode() != ThisMode.Lexical;
        /* step 5 */
        if (f.isNeedsSuper() && f.getHomeObject() == null) {
            throw newReferenceError(callerContext, Messages.Key.MissingSuperBinding);
        }
        LexicalEnvironment<?> e = f.getEnvironment();
        /* steps 3-6 */
        FunctionEnvironmentRecord envRec = new FunctionEnvironmentRecord(e.cx, t,
                f.getHomeObject(), f.getMethodName());
        /* steps 2, 7-8 */
        LexicalEnvironment<FunctionEnvironmentRecord> env = new LexicalEnvironment<>(e, envRec);
        /* step 9 */
        return env;
    }

    /**
     * 8.1.2.5 NewGlobalEnvironment ( G ) Abstract Operation
     * 
     * @param cx
     *            the default execution context
     * @param g
     *            the global object
     * @return the new global environment
     */
    public static LexicalEnvironment<GlobalEnvironmentRecord> newGlobalEnvironment(
            ExecutionContext cx, ScriptObject g) {
        /* steps 2-8 */
        GlobalEnvironmentRecord globalRec = new GlobalEnvironmentRecord(cx, g);
        /* steps 1, 9-10 */
        LexicalEnvironment<GlobalEnvironmentRecord> env = new LexicalEnvironment<>(cx, globalRec);
        /* step 11 */
        return env;
    }

    /**
     * 8.1.2.? NewModuleEnvironment (E) Abstract Operation
     * 
     * @param e
     *            the outer lexical environment
     * @return the new module environment
     */
    public static LexicalEnvironment<DeclarativeEnvironmentRecord> newModuleEnvironment(
            LexicalEnvironment<?> e) {
        /* step 2 */
        DeclarativeEnvironmentRecord envRec = new DeclarativeEnvironmentRecord(e.cx);
        /* steps 1, 3-4 */
        LexicalEnvironment<DeclarativeEnvironmentRecord> env = new LexicalEnvironment<>(e, envRec);
        /* step 5 */
        return env;
    }
}

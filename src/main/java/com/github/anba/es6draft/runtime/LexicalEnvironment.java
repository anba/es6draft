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
public final class LexicalEnvironment {
    private final ExecutionContext cx;
    private final LexicalEnvironment outer;
    private final EnvironmentRecord envRec;

    public LexicalEnvironment(ExecutionContext cx, EnvironmentRecord envRec) {
        this.cx = cx;
        this.outer = null;
        this.envRec = envRec;
    }

    public LexicalEnvironment(LexicalEnvironment outer, EnvironmentRecord envRec) {
        this.cx = outer.cx;
        this.outer = outer;
        this.envRec = envRec;
    }

    @Override
    public String toString() {
        return String.format("%s: {envRec=%s}", getClass().getSimpleName(), envRec);
    }

    public EnvironmentRecord getEnvRec() {
        return envRec;
    }

    public LexicalEnvironment getOuter() {
        return outer;
    }

    private static EnvironmentRecord getIdentifierRecord(LexicalEnvironment lex, String name) {
        for (; lex != null; lex = lex.outer) {
            EnvironmentRecord envRec = lex.envRec;
            if (envRec.hasBinding(name)) {
                return envRec;
            }
        }
        return null;
    }

    static Object getIdentifierValueOrThrow(LexicalEnvironment lex, String name, boolean strict) {
        EnvironmentRecord envRec = getIdentifierRecord(lex, name);
        if (envRec != null) {
            return envRec.getBindingValue(name, strict);
        }
        throw newReferenceError(lex.cx, Messages.Key.UnresolvableReference, name);
    }

    /**
     * 8.1.2.1 GetIdentifierReference (lex, name, strict)
     */
    public static Reference<EnvironmentRecord, String> getIdentifierReference(
            LexicalEnvironment lex, String name, boolean strict) {
        /* steps 2-3, 5 */
        EnvironmentRecord envRec = getIdentifierRecord(lex, name);
        /* steps 1, 4 */
        return new Reference.IdentifierReference(envRec, name, strict);
    }

    /**
     * 8.1.2.2 NewDeclarativeEnvironment (E)
     */
    public static LexicalEnvironment newDeclarativeEnvironment(LexicalEnvironment e) {
        /* step 2 */
        EnvironmentRecord envRec = new DeclarativeEnvironmentRecord(e.cx);
        /* steps 1, 3-4 */
        LexicalEnvironment env = new LexicalEnvironment(e, envRec);
        /* step 5 */
        return env;
    }

    /**
     * 8.1.2.3 NewObjectEnvironment (O, E)
     */
    public static LexicalEnvironment newObjectEnvironment(ScriptObject o, LexicalEnvironment e) {
        /* steps 2-3 */
        EnvironmentRecord envRec = new ObjectEnvironmentRecord(e.cx, o, false);
        /* steps 1, 4-5 */
        LexicalEnvironment env = new LexicalEnvironment(e, envRec);
        /* step 6 */
        return env;
    }

    /**
     * 8.1.2.3 NewObjectEnvironment (O, E)
     */
    public static LexicalEnvironment newObjectEnvironment(ScriptObject o, LexicalEnvironment e,
            boolean withEnvironment) {
        /* steps 2-3 */
        EnvironmentRecord envRec = new ObjectEnvironmentRecord(e.cx, o, withEnvironment);
        /* steps 1, 4-5 */
        LexicalEnvironment env = new LexicalEnvironment(e, envRec);
        /* step 6 */
        return env;
    }

    /**
     * 8.1.2.4 NewFunctionEnvironment (F, T)
     */
    public static LexicalEnvironment newFunctionEnvironment(ExecutionContext cx, FunctionObject f,
            Object t) {
        /* step 1 */
        assert f.getThisMode() != ThisMode.Lexical;
        /* steps 3-6 */
        EnvironmentRecord envRec = new FunctionEnvironmentRecord(cx, t, f.getHomeObject(),
                f.getMethodName());
        /* steps 2, 7-8 */
        LexicalEnvironment env = new LexicalEnvironment(f.getScope(), envRec);
        /* step 9 */
        return env;
    }

    /**
     * 8.1.2.? NewModuleEnvironment (E)
     */
    public static LexicalEnvironment newModuleEnvironment(LexicalEnvironment e) {
        /* step 2 */
        EnvironmentRecord envRec = new DeclarativeEnvironmentRecord(e.cx);
        /* steps 1, 3-4 */
        LexicalEnvironment env = new LexicalEnvironment(e, envRec);
        /* step 5 */
        return env;
    }
}

/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.internal.Errors.throwReferenceError;

import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject.ThisMode;

/**
 * <h1>10 Executable Code and Execution Contexts</h1><br>
 * <h2>10.2 Lexical Environments</h2>
 * <ul>
 * <li>10.2.2 Lexical Environment Operations
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

    /**
     * 10.2.2.1 GetIdentifierReference (lex, name, strict)
     */
    public static Reference<EnvironmentRecord, String> getIdentifierReference(
            LexicalEnvironment lex, String name, boolean strict) {
        EnvironmentRecord envRec = getIdentifierRecord(lex, name);
        return new Reference.IdentifierReference(envRec, name, strict);
    }

    static Object getIdentifierValueOrThrow(LexicalEnvironment lex, String name, boolean strict) {
        EnvironmentRecord envRec = getIdentifierRecord(lex, name);
        if (envRec != null) {
            return envRec.getBindingValue(name, strict);
        }
        throw throwReferenceError(lex.cx, Messages.Key.UnresolvableReference, name);
    }

    /**
     * 10.2.2.2 NewDeclarativeEnvironment (E)
     */
    public static LexicalEnvironment newDeclarativeEnvironment(LexicalEnvironment e) {
        EnvironmentRecord envRec = new DeclarativeEnvironmentRecord(e.cx);
        LexicalEnvironment env = new LexicalEnvironment(e, envRec);
        return env;
    }

    /**
     * 10.2.2.3 NewObjectEnvironment (O, E)
     */
    public static LexicalEnvironment newObjectEnvironment(ScriptObject o, LexicalEnvironment e) {
        EnvironmentRecord envRec = new ObjectEnvironmentRecord(e.cx, o, false);
        LexicalEnvironment env = new LexicalEnvironment(e, envRec);
        return env;
    }

    /**
     * 10.2.2.3 NewObjectEnvironment (O, E)
     */
    public static LexicalEnvironment newObjectEnvironment(ScriptObject o, LexicalEnvironment e,
            boolean withEnvironment) {
        EnvironmentRecord envRec = new ObjectEnvironmentRecord(e.cx, o, withEnvironment);
        LexicalEnvironment env = new LexicalEnvironment(e, envRec);
        return env;
    }

    /**
     * 10.2.2.4 NewFunctionEnvironment (F, T)
     */
    public static LexicalEnvironment newFunctionEnvironment(ExecutionContext cx, FunctionObject f,
            Object t) {
        assert f.getThisMode() != ThisMode.Lexical;
        EnvironmentRecord envRec = new FunctionEnvironmentRecord(cx, t, f.getHomeObject(),
                f.getMethodName());
        LexicalEnvironment env = new LexicalEnvironment(f.getScope(), envRec);
        return env;
    }
}

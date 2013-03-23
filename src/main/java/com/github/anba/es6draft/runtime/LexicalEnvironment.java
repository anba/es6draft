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

/**
 * <h1>10 Executable Code and Execution Contexts</h1><br>
 * <h2>10.2 Lexical Environments</h2>
 * <ul>
 * <li>10.2.2 Lexical Environment Operations
 * </ul>
 */
public final class LexicalEnvironment {
    private final Realm realm;
    private final LexicalEnvironment outer;
    private final EnvironmentRecord envRec;

    public LexicalEnvironment(Realm realm, EnvironmentRecord envRec) {
        this.realm = realm;
        this.outer = null;
        this.envRec = envRec;
    }

    public LexicalEnvironment(LexicalEnvironment outer, EnvironmentRecord envRec) {
        this.realm = outer.realm;
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
    public static Reference getIdentifierReference(LexicalEnvironment lex, String name,
            boolean strict) {
        EnvironmentRecord envRec = getIdentifierRecord(lex, name);
        return new Reference.IdentifierReference(envRec, name, strict);
    }

    static Object getIdentifierValueOrThrow(LexicalEnvironment lex, String name, boolean strict) {
        EnvironmentRecord envRec = getIdentifierRecord(lex, name);
        if (envRec != null) {
            return envRec.getBindingValue(name, strict);
        }
        throw throwReferenceError(lex.realm, Messages.Key.UnresolvableReference, name);
    }

    /**
     * 10.2.2.2 NewDeclarativeEnvironment (E)
     */
    public static LexicalEnvironment newDeclarativeEnvironment(LexicalEnvironment e) {
        EnvironmentRecord envRec = new DeclarativeEnvironmentRecord(e.realm);
        LexicalEnvironment env = new LexicalEnvironment(e, envRec);
        return env;
    }

    /**
     * 10.2.2.3 NewObjectEnvironment (O, E)
     */
    public static LexicalEnvironment newObjectEnvironment(ScriptObject o, LexicalEnvironment e) {
        EnvironmentRecord envRec = new ObjectEnvironmentRecord(e.realm, o, false);
        LexicalEnvironment env = new LexicalEnvironment(e, envRec);
        return env;
    }

    /**
     * 10.2.2.3 NewObjectEnvironment (O, E)
     */
    public static LexicalEnvironment newObjectEnvironment(ScriptObject o, LexicalEnvironment e,
            boolean withEnvironment) {
        EnvironmentRecord envRec = new ObjectEnvironmentRecord(e.realm, o, withEnvironment);
        LexicalEnvironment env = new LexicalEnvironment(e, envRec);
        return env;
    }

    /**
     * 10.2.2.4 NewFunctionEnvironment (F, T)
     */
    public static LexicalEnvironment newFunctionEnvironment(FunctionObject f, Object t) {
        EnvironmentRecord envRec = new FunctionEnvironmentRecord(f.getRealm(), t, f.getHome(),
                f.getMethodName());
        LexicalEnvironment env = new LexicalEnvironment(f.getScope(), envRec);
        return env;
    }
}

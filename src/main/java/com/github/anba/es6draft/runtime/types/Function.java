/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.3 Ordinary Object Internal Methods and Internal Data Properties</h2>
 * <ul>
 * <li>8.3.19 Ordinary Function Objects
 * </ul>
 */
public interface Function extends Scriptable, Callable, Constructor {
    public enum FunctionKind {
        Normal, Method, Arrow
    }

    public enum ThisMode {
        Lexical, Strict, Global
    }

    /**
     * Compiled function object
     */
    RuntimeInfo.Function getFunction();

    /**
     * [[Scope]]
     */
    LexicalEnvironment getScope();

    /**
     * [[Code]]
     */
    RuntimeInfo.Code getCode();

    /**
     * [[Realm]]
     */
    Realm getRealm();

    /**
     * [[ThisMode]]
     */
    ThisMode getThisMode();

    /**
     * [[Strict]]
     */
    boolean isStrict();

    /**
     * [[Home]]
     */
    Scriptable getHome();

    /**
     * [[MethodName]]
     */
    String getMethodName();
}

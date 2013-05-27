/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>10 Executable Code and Execution Contexts</h1><br>
 * <h2>10.2 Lexical Environments</h2><br>
 * <h3>10.2.1 Environment Records</h3>
 * <ul>
 * <li>10.2.1.3 Function Environment Records
 * </ul>
 */
public final class FunctionEnvironmentRecord extends DeclarativeEnvironmentRecord {
    private final Object thisValue;
    private final ScriptObject homeObject;
    private final String methodName;

    public FunctionEnvironmentRecord(ExecutionContext cx, Object thisValue,
            ScriptObject homeObject, String methodName) {
        super(cx);
        this.thisValue = thisValue;
        this.homeObject = homeObject;
        this.methodName = methodName;
    }

    /**
     * 10.2.1.3.1 HasThisBinding ()
     */
    @Override
    public boolean hasThisBinding() {
        return true;
    }

    /**
     * 10.2.1.3.2 HasSuperBinding ()
     */
    @Override
    public boolean hasSuperBinding() {
        return homeObject != null;
    }

    /**
     * 10.2.1.3.3 GetThisBinding ()
     */
    @Override
    public Object getThisBinding() {
        return thisValue;
    }

    /**
     * 10.2.1.3.4 GetSuperBase ()
     */
    public ScriptObject getSuperBase() {
        ScriptObject home = homeObject;
        if (home == null) {
            return null;
        }
        return home.getInheritance(cx);
    }

    /**
     * 10.2.1.3.5 GetMethodName ()
     */
    public String getMethodName() {
        return methodName;
    }
}

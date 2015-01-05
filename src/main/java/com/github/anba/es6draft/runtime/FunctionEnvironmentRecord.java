/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 * <h1>8 Executable Code and Execution Contexts</h1><br>
 * <h2>8.1 Lexical Environments</h2><br>
 * <h3>8.1.1 Environment Records</h3>
 * <ul>
 * <li>8.1.1.3 Function Environment Records
 * </ul>
 */
public final class FunctionEnvironmentRecord extends DeclarativeEnvironmentRecord {
    private final FunctionObject functionObject;
    private final Object thisValue;
    private final ScriptObject homeObject;
    private DeclarativeEnvironmentRecord topLex;

    public FunctionEnvironmentRecord(ExecutionContext cx, FunctionObject functionObject,
            Object thisValue, ScriptObject homeObject) {
        super(cx);
        this.functionObject = functionObject;
        this.thisValue = thisValue;
        this.homeObject = homeObject;
    }

    @Override
    public String toString() {
        return String.format(
                "%s:{%n\tfunctionObject=%s,%n\tthisValue=%s,%n\thomeObject=%s,%n\tbindings=%s%n}",
                getClass().getSimpleName(), functionObject, thisValue, homeObject,
                bindingsToString());
    }

    /**
     * Returns the {@code FunctionObject} state component.
     * 
     * @return the {@code FunctionObject} component
     */
    public FunctionObject getFunctionObject() {
        return functionObject;
    }

    /**
     * Returns the {@code topLex} state component.
     * 
     * @return the {@code topLex} component
     */
    public DeclarativeEnvironmentRecord getTopLex() {
        // FIXME: spec bug - eval in default parameter initializer (bug 3383)
        if (topLex == null) {
            return this;
        }
        return topLex;
    }

    /**
     * [Called from generated code]
     * 
     * @param topLex
     *            the top lexical environment record
     */
    public void setTopLex(DeclarativeEnvironmentRecord topLex) {
        this.topLex = topLex;
    }

    /**
     * 8.1.1.3.1 HasThisBinding ()
     */
    @Override
    public boolean hasThisBinding() {
        /* step 1 */
        return thisValue != null;
    }

    /**
     * 8.1.1.3.2 HasSuperBinding ()
     */
    @Override
    public boolean hasSuperBinding() {
        /* steps 1-2 */
        return thisValue != null && homeObject != null;
    }

    /**
     * 8.1.1.3.3 GetThisBinding ()
     */
    @Override
    public Object getThisBinding() {
        /* step 1 */
        return thisValue;
    }

    /**
     * 8.1.1.3.4 GetSuperBase ()
     * 
     * @return the prototype of the home object or {@code null} if no super binding was set
     */
    public ScriptObject getSuperBase() {
        /* step 1 */
        ScriptObject home = homeObject;
        /* step 2 */
        if (home == null) {
            return null;
        }
        /* step 3 (not applicable) */
        /* step 4 */
        return home.getPrototypeOf(cx);
    }
}

/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.internal.Errors.newReferenceError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.Constructor;
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
    private final Constructor newTarget;
    private final ScriptObject homeObject;
    private Object thisValue;
    private boolean thisInitializationState;
    private DeclarativeEnvironmentRecord topLex;

    public FunctionEnvironmentRecord(ExecutionContext cx, FunctionObject functionObject,
            Constructor newTarget, Object thisValue) {
        super(cx);
        this.functionObject = functionObject;
        this.newTarget = newTarget;
        this.homeObject = functionObject.getHomeObject();
        this.thisValue = thisValue;
        this.thisInitializationState = true;
    }

    public FunctionEnvironmentRecord(ExecutionContext cx, FunctionObject functionObject,
            Constructor newTarget) {
        super(cx);
        this.functionObject = functionObject;
        this.newTarget = newTarget;
        this.homeObject = functionObject.getHomeObject();
        this.thisValue = UNDEFINED;
        this.thisInitializationState = false;
    }

    @Override
    public String toString() {
        return String
                .format("%s:{%n\tfunctionObject=%s,%n\tnewTarget=%s,%n\thomeObject=%s,%n\tthisValue=%s,%n\tthisInitializationState=%b,%n\tbindings=%s%n}",
                        getClass().getSimpleName(), functionObject, newTarget, homeObject,
                        thisValue, thisInitializationState, bindingsToString());
    }

    /**
     * Returns the {@code FunctionObject} state field.
     * 
     * @return the {@code FunctionObject} field
     */
    public FunctionObject getFunctionObject() {
        return functionObject;
    }

    /**
     * Returns the {@code newTarget} state field.
     * 
     * @return the {@code newTarget} field
     */
    public Constructor getNewTarget() {
        return newTarget;
    }

    /**
     * Returns the {@code thisInitializationState} state field.
     * 
     * @return the {@code thisInitializationState} field
     */
    public boolean isThisInitialized() {
        return thisInitializationState;
    }

    /**
     * Returns the {@code topLex} state field.
     * 
     * @return the {@code topLex} field
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
     * 8.1.1.3.1 BindThisValue(V)
     * 
     * @param cx
     *            the execution context
     * @param thisValue
     *            the function {@code this} value
     */
    public void bindThisValue(ExecutionContext cx, ScriptObject thisValue) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (thisInitializationState) {
            throw newReferenceError(cx, Messages.Key.InitializedThis);
        }
        /* step 3 */
        this.thisValue = thisValue;
        /* step 4 */
        this.thisInitializationState = true;
        /* step 5 (not applicable) */
    }

    /**
     * 8.1.1.3.2 HasThisBinding ()
     */
    @Override
    public boolean hasThisBinding() {
        /* steps 1-2 */
        return thisValue != null;
    }

    /**
     * 8.1.1.3.3 HasSuperBinding ()
     */
    @Override
    public boolean hasSuperBinding() {
        /* steps 1-3 */
        return thisValue != null && homeObject != null;
    }

    /**
     * 8.1.1.3.4 GetThisBinding ()
     * 
     * @param cx
     *            the execution context
     */
    @Override
    public Object getThisBinding(ExecutionContext cx) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (!thisInitializationState) {
            throw newReferenceError(cx, Messages.Key.UninitializedThis);
        }
        /* step 3 */
        return thisValue;
    }

    /**
     * 8.1.1.3.5 GetSuperBase ()
     * 
     * @param cx
     *            the execution context
     * @return the prototype of the home object or {@code null} if no super binding was set
     */
    public ScriptObject getSuperBase(ExecutionContext cx) {
        /* step 1 (not applicable) */
        /* step 2 */
        ScriptObject home = homeObject;
        /* step 3 */
        if (home == null) {
            return null;
        }
        /* step 4 (not applicable) */
        /* step 5 */
        return home.getPrototypeOf(cx);
    }
}

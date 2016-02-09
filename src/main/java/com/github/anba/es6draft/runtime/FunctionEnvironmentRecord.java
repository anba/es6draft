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
    private ThisBindingStatus thisBindingStatus;

    public enum ThisBindingStatus {
        Lexical, Initialized, Uninitialized
    }

    public FunctionEnvironmentRecord(ExecutionContext cx, FunctionObject functionObject, Constructor newTarget,
            Object thisValue) {
        super(cx, false);
        this.functionObject = functionObject;
        this.newTarget = newTarget;
        this.homeObject = functionObject.getHomeObject();
        this.thisValue = thisValue;
        this.thisBindingStatus = thisValue != null ? ThisBindingStatus.Initialized : ThisBindingStatus.Lexical;
    }

    public FunctionEnvironmentRecord(ExecutionContext cx, FunctionObject functionObject, Constructor newTarget) {
        super(cx, false);
        this.functionObject = functionObject;
        this.newTarget = newTarget;
        this.homeObject = functionObject.getHomeObject();
        this.thisValue = UNDEFINED;
        this.thisBindingStatus = ThisBindingStatus.Uninitialized;
    }

    @Override
    public String toString() {
        return String
                .format("%s:{%n\tfunctionObject=%s,%n\tnewTarget=%s,%n\thomeObject=%s,%n\tthisValue=%s,%n\tthisBindingStatus=%s,%n\tbindings=%s%n}",
                        getClass().getSimpleName(), functionObject, newTarget, homeObject,
                        thisValue, thisBindingStatus, bindingsToString());
    }

    /**
     * Returns the {@code FunctionObject} field.
     * 
     * @return the {@code FunctionObject} field
     */
    public FunctionObject getFunctionObject() {
        return functionObject;
    }

    /**
     * Returns the {@code newTarget} field.
     * 
     * @return the {@code newTarget} field
     */
    public Constructor getNewTarget() {
        return newTarget;
    }

    /**
     * Returns the {@code thisBindingStatus} field.
     * 
     * @return the {@code thisBindingStatus} field
     */
    public ThisBindingStatus getThisBindingStatus() {
        return thisBindingStatus;
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
        assert thisBindingStatus != ThisBindingStatus.Lexical;
        /* step 3 */
        if (thisBindingStatus == ThisBindingStatus.Initialized) {
            throw newReferenceError(cx, Messages.Key.InitializedThis);
        }
        /* step 4 */
        this.thisValue = thisValue;
        /* step 5 */
        this.thisBindingStatus = ThisBindingStatus.Initialized;
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
        assert thisBindingStatus != ThisBindingStatus.Lexical;
        /* step 3 */
        if (thisBindingStatus == ThisBindingStatus.Uninitialized) {
            throw newReferenceError(cx, Messages.Key.UninitializedThis);
        }
        /* step 4 */
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

/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.promise;

import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>25 Control Abstraction Objects</h1><br>
 * <h2>25.4 Promise Objects</h2>
 * <ul>
 * <li>25.4.6 Properties of Promise Instances
 * </ul>
 */
public final class PromiseObject extends OrdinaryObject {
    public enum Status {
        Unresolved, HasResolution, HasRejection
    }

    public enum State {
        Pending, Fulfilled, Rejected
    }

    /** [[PromiseStatus]] */
    private Status status;

    /** [[PromiseState]] */
    private State state;

    /** [[PromiseConstructor]] */
    private Constructor constructor;

    /** [[PromiseResult]] */
    private Object result;

    /** [[PromiseResolveReactions]] */
    private List<PromiseReaction> resolveReactions;

    /** [[PromiseRejectReactions]] */
    private List<PromiseReaction> rejectReactions;

    public PromiseObject(Realm realm) {
        super(realm);
    }

    public void initialise() {
        assert status == null && state == null;
        status = PromiseObject.Status.Unresolved;
        state = PromiseObject.State.Pending;
        resolveReactions = new ArrayList<>();
        rejectReactions = new ArrayList<>();
    }

    /**
     * [[PromiseStatus]]
     * 
     * @return the promise status
     */
    @Deprecated
    public Status getStatus() {
        return status;
    }

    /**
     * [[PromiseState]]
     * 
     * @return the promise state
     */
    public State getState() {
        return state;
    }

    /**
     * [[PromiseConstructor]]
     * 
     * @return the promise constructor function
     */
    public Constructor getConstructor() {
        return constructor;
    }

    /**
     * [[PromiseConstructor]]
     * 
     * @param constructor
     *            the promise constructor function
     */
    public void setConstructor(Constructor constructor) {
        assert this.constructor == null && constructor != null;
        this.constructor = constructor;
    }

    /**
     * [[PromiseResult]]
     * 
     * @return the promise result value
     */
    public Object getResult() {
        return result;
    }

    /**
     * [[PromiseResolveReactions]]
     * 
     * @param reaction
     *            the resolve reaction
     */
    public void addResolveReaction(PromiseReaction reaction) {
        assert state == State.Pending;
        resolveReactions.add(reaction);
    }

    /**
     * [[PromiseRejectReactions]]
     * 
     * @param reaction
     *            the reject reaction
     */
    public void addRejectReaction(PromiseReaction reaction) {
        assert state == State.Pending;
        rejectReactions.add(reaction);
    }

    public List<PromiseReaction> resolve(Object resolution) {
        List<PromiseReaction> reactions = resolveReactions;
        resolve(State.Fulfilled, resolution);
        return reactions;
    }

    public List<PromiseReaction> reject(Object reason) {
        List<PromiseReaction> reactions = rejectReactions;
        resolve(State.Rejected, reason);
        return reactions;
    }

    private void resolve(State state, Object result) {
        assert this.state == State.Pending;
        this.result = result;
        this.resolveReactions = null;
        this.rejectReactions = null;
        this.state = state;
    }
}

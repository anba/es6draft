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
    public enum State {
        Pending, Fulfilled, Rejected
    }

    /** [[PromiseState]] */
    private State state;

    /** [[PromiseConstructor]] */
    private Constructor constructor;

    /** [[PromiseResult]] */
    private Object result;

    /** [[PromiseFulfillReactions]] */
    private List<PromiseReaction> fulfillReactions;

    /** [[PromiseRejectReactions]] */
    private List<PromiseReaction> rejectReactions;

    public PromiseObject(Realm realm) {
        super(realm);
    }

    public void initialize() {
        assert state == null;
        state = PromiseObject.State.Pending;
        fulfillReactions = new ArrayList<>();
        rejectReactions = new ArrayList<>();
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
     * [[PromiseFulfillReactions]]
     * 
     * @param reaction
     *            the fulfill reaction
     */
    public void addFulfillReaction(PromiseReaction reaction) {
        assert state == State.Pending;
        fulfillReactions.add(reaction);
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
        List<PromiseReaction> reactions = fulfillReactions;
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
        this.fulfillReactions = null;
        this.rejectReactions = null;
        this.state = state;
    }
}

/**
 * Copyright (c) 2012-2015 André Bargull
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
public class PromiseObject extends OrdinaryObject {
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
    private ArrayList<PromiseReaction> fulfillReactions;

    /** [[PromiseRejectReactions]] */
    private ArrayList<PromiseReaction> rejectReactions;

    /**
     * Constructs a new Promise object.
     * 
     * @param realm
     *            the realm object
     */
    public PromiseObject(Realm realm) {
        super(realm);
    }

    /**
     * Initializes a Promise object.
     * <p>
     * <strong>Must not be called on initialized Promise objects!</strong>
     */
    public final void initialize() {
        assert state == null;
        state = PromiseObject.State.Pending;
        fulfillReactions = new ArrayList<>();
        rejectReactions = new ArrayList<>();
    }

    @Override
    public String toString() {
        return String.format("%s, state=%s", super.toString(), state);
    }

    /*package*/void notifyRejectReaction(PromiseReaction reaction) {
        // override in sub-class
    }

    /*package*/void notifyReject(Object reason) {
        // override in sub-class
    }

    /**
     * [[PromiseState]]
     * 
     * @return the promise state
     */
    public final State getState() {
        return state;
    }

    /**
     * [[PromiseConstructor]]
     * 
     * @return the promise constructor function
     */
    public final Constructor getConstructor() {
        return constructor;
    }

    /**
     * [[PromiseConstructor]]
     * 
     * @param constructor
     *            the promise constructor function
     */
    public final void setConstructor(Constructor constructor) {
        assert this.constructor == null && constructor != null;
        this.constructor = constructor;
    }

    /**
     * [[PromiseResult]]
     * 
     * @return the promise result value
     */
    public final Object getResult() {
        assert state == State.Fulfilled || state == State.Rejected;
        return result;
    }

    /**
     * [[PromiseFulfillReactions]]
     * 
     * @param reaction
     *            the fulfill reaction
     */
    public final void addFulfillReaction(PromiseReaction reaction) {
        assert state == State.Pending;
        fulfillReactions.add(reaction);
    }

    /**
     * [[PromiseRejectReactions]]
     * 
     * @param reaction
     *            the reject reaction
     */
    public final void addRejectReaction(PromiseReaction reaction) {
        assert state == State.Pending;
        rejectReactions.add(reaction);
    }

    /**
     * Fulfills the pending promise with <var>value</var>.
     * 
     * @param value
     *            the fulfillment value
     * @return the list of collected promise reaction records
     */
    public final List<PromiseReaction> fufill(Object value) {
        List<PromiseReaction> reactions = fulfillReactions;
        resolve(State.Fulfilled, value);
        return reactions;
    }

    /**
     * Rejects the pending promise with <var>reason</var>.
     * 
     * @param reason
     *            the rejection value
     * @return the list of collected promise reaction records
     */
    public final List<PromiseReaction> reject(Object reason) {
        List<PromiseReaction> reactions = rejectReactions;
        resolve(State.Rejected, reason);
        notifyReject(reason);
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

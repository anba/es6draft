/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.promise;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 *
 */
public final class FinalizablePromiseObject extends PromiseObject {
    private boolean trackRejection = true;
    private RejectReason rejectReason;

    public FinalizablePromiseObject(Realm realm) {
        super(realm);
        rejectReason = new RejectReason(realm);
    }

    @Override
    void notifyReject(Object reason) {
        if (trackRejection) {
            rejectReason.set(reason);
        }
    }

    @Override
    void notifyRejectReaction(PromiseReaction reaction) {
        assert getState() == State.Pending || getState() == State.Rejected;
        if (reaction.getType() != PromiseReaction.Type.Thrower) {
            trackRejection = false;
            rejectReason.clear();
        } else {
            ScriptObject promise = reaction.getCapabilities().getPromise();
            if (promise instanceof FinalizablePromiseObject) {
                FinalizablePromiseObject promiseObject = (FinalizablePromiseObject) promise;
                if (promiseObject.getState() == State.Pending) {
                    // TODO: This is not quite correct when `promiseObject` is rejected on its own.
                    // Don't track rejection for dependent promises.
                    promiseObject.trackRejection = false;
                    promiseObject.rejectReason = rejectReason;
                }
            }
        }
    }

    /**
     * Holder object for promise rejection reasons.
     */
    private static final class RejectReason {
        private final Realm realm;
        private boolean valueSet;
        private Object value;

        RejectReason(Realm realm) {
            this.realm = realm;
        }

        private void setInternal(Object value) {
            this.value = value;
            this.valueSet = true;
        }

        void set(Object reason) {
            if (!valueSet) {
                setInternal(reason);
            }
        }

        void clear() {
            setInternal(null);
        }

        @Override
        protected void finalize() throws Throwable {
            if (value != null) {
                realm.enqueueUnhandledPromiseRejection(value);
            }
            super.finalize();
        }
    }
}

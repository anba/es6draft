/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.internal;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateIterResultObject;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.Iterator;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.objects.internal.CompoundIterator.State;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>7 Abstract Operations</h1><br>
 * <h2>7.4 Operations on Iterator Objects</h2><br>
 * <h3>7.4.9 CreateCompoundIterator ( iterator1, iterator2 )</h3>
 * <ul>
 * <li>7.4.9.1 CompoundIterator next( )
 * </ul>
 */
public final class CompoundIteratorNext extends BuiltinFunction {
    public CompoundIteratorNext(Realm realm) {
        super(realm, "next", 0);
        createDefaultFunctionProperties();
    }

    private CompoundIteratorNext(Realm realm, Void ignore) {
        super(realm, "next", 0);
    }

    @Override
    public CompoundIteratorNext clone() {
        return new CompoundIteratorNext(getRealm(), null);
    }

    @Override
    public OrdinaryObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* steps 1-2 (omitted) */
        /* steps 3, 6 */
        if (!(thisValue instanceof CompoundIterator)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        CompoundIterator<?> compound = (CompoundIterator<?>) thisValue;
        /* steps 4-5 */
        if (this != compound.getIteratorNext()) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        /* step 7 (not applicable) */
        /* steps 8-9 */
        if (compound.getState() == State.First) {
            /* step 9 */
            Iterator<?> iterator1 = compound.getFirstIterator();
            if (iterator1.hasNext()) {
                return CreateIterResultObject(calleeContext, iterator1.next(), false);
            }
            compound.setState(State.Second);
        }
        /* step 10 */
        Iterator<?> iterator2 = compound.getSecondIterator();
        /* step 11 */
        if (iterator2.hasNext()) {
            return CreateIterResultObject(calleeContext, iterator2.next(), false);
        }
        return CreateIterResultObject(calleeContext, UNDEFINED, true);
    }
}

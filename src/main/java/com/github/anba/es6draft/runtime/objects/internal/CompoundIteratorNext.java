/**
 * Copyright (c) 2012-2014 André Bargull
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
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>7 Abstract Operations</h1><br>
 * <h2>7.4 Operations on Iterator Objects</h2><br>
 * <h3>7.4.10 CreateCompoundIterator ( iterator1, iterator2 )</h3>
 * <ul>
 * <li>7.4.10.1 CompoundIterator next( )
 * </ul>
 */
public final class CompoundIteratorNext extends BuiltinFunction {
    public CompoundIteratorNext(Realm realm) {
        super(realm, "next");
        createDefaultFunctionProperties("next", 0);
    }

    private CompoundIteratorNext(Realm realm, Void ignore) {
        super(realm, "next");
    }

    @Override
    public CompoundIteratorNext clone() {
        return new CompoundIteratorNext(getRealm(), null);
    }

    @Override
    public OrdinaryObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* step 1 (omitted) */
        /* step 2 */
        if (!(thisValue instanceof CompoundIterator)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        /* step 3 */
        CompoundIterator<?> compound = (CompoundIterator<?>) thisValue;
        /* steps 4-5 */
        if (compound.isFirstIteratorActive()) {
            /* step 5 */
            Iterator<?> iterator = compound.getFirstIterator();
            if (iterator.hasNext()) {
                return CreateIterResultObject(calleeContext, iterator.next(), false);
            }
            compound.setFirstIteratorActive(false);
        }
        /* step 6 */
        Iterator<?> iterator = compound.getSecondIterator();
        /* step 7 */
        if (!iterator.hasNext()) {
            return CreateIterResultObject(calleeContext, UNDEFINED, true);
        }
        /* step 7 */
        return CreateIterResultObject(calleeContext, iterator.next(), false);
    }
}

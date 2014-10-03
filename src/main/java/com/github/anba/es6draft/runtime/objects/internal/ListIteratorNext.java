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
 * <h3>7.4.9 CreateListIterator (list)</h3>
 * <ul>
 * <li>7.4.9.1 ListIterator next( )
 * </ul>
 */
public final class ListIteratorNext extends BuiltinFunction {
    public ListIteratorNext(Realm realm) {
        super(realm, "next");
        createDefaultFunctionProperties("next", 0);
    }

    private ListIteratorNext(Realm realm, Void ignore) {
        super(realm, "next");
    }

    @Override
    public ListIteratorNext clone() {
        return new ListIteratorNext(getRealm(), null);
    }

    @Override
    public OrdinaryObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* steps 1-2 (omitted) */
        /* steps 3, 6 */
        if (!(thisValue instanceof ListIterator)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        ListIterator<?> listIterator = (ListIterator<?>) thisValue;
        /* steps 4-5 */
        if (this != listIterator.getIteratorNext()) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        /* step 7 */
        Iterator<?> iterator = listIterator.getIterator();
        /* steps 8-10 */
        if (!iterator.hasNext()) {
            return CreateIterResultObject(calleeContext, UNDEFINED, true);
        }
        /* steps 11-12 */
        return CreateIterResultObject(calleeContext, iterator.next(), false);
    }
}

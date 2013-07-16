/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.internal;

import static com.github.anba.es6draft.runtime.AbstractOperations.SetIntegrityLevel;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.CreateItrResultObject;

import java.util.Iterator;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.IntegrityLevel;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 *
 */
public class ListIteratorPrototype extends OrdinaryObject implements Initialisable {
    public ListIteratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        SetIntegrityLevel(cx, this, IntegrityLevel.Frozen);
    }

    public enum Properties {
        ;

        private static ListIterator<?> listIterator(ExecutionContext cx, Object object) {
            if (object instanceof ListIterator) {
                return (ListIterator<?>) object;
            }
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        @Function(name = "next", arity = 0)
        public static Object send(ExecutionContext cx, Object thisValue) {
            Iterator<?> iterator = listIterator(cx, thisValue).getIterator();
            if (!iterator.hasNext()) {
                return CreateItrResultObject(cx, Undefined.UNDEFINED, true);
            }
            return CreateItrResultObject(cx, iterator.next(), false);
        }

        @Function(name = "@@iterator", symbol = BuiltinSymbol.iterator, arity = 0)
        public static Object iterator(ExecutionContext cx, Object thisValue) {
            return thisValue;
        }
    }
}

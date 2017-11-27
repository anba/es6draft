/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateArrayFromList;
import static com.github.anba.es6draft.runtime.AbstractOperations.CreateIterResultObject;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.Iterator;
import java.util.Map.Entry;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.collection.SetIteratorObject.SetIterationKind;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.NativeFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>23 Keyed Collection</h1><br>
 * <h2>23.2 Set Objects</h2>
 * <ul>
 * <li>23.2.5 Set Iterator Objects
 * </ul>
 */
public final class SetIteratorPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Set Iterator prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public SetIteratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * 23.2.5.1 CreateSetIterator Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param obj
     *            the set object
     * @param kind
     *            the set iteration kind
     * @param method
     *            the method name
     * @return the new set iterator
     */
    public static SetIteratorObject CreateSetIterator(ExecutionContext cx, Object obj, SetIterationKind kind,
            String method) {
        /* steps 1-2 */
        if (!(obj instanceof SetObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(obj).toString());
        }
        SetObject set = (SetObject) obj;
        /* steps 3-7 */
        return new SetIteratorObject(cx.getRealm(), set, kind, cx.getIntrinsic(Intrinsics.SetIteratorPrototype));
    }

    /**
     * 23.2.5.1 CreateSetIterator Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param iterator
     *            the iterator
     * @param kind
     *            the set iteration kind
     * @return the new set iterator
     */
    public static SetIteratorObject CreateSetIterator(ExecutionContext cx, Iterator<Entry<Object, Void>> iterator,
            SetIterationKind kind) {
        /* steps 1-7 */
        return new SetIteratorObject(cx.getRealm(), iterator, kind, cx.getIntrinsic(Intrinsics.SetIteratorPrototype));
    }

    /**
     * Marker class for {@code %SetIteratorPrototype%.next}.
     */
    private static final class SetIteratorPrototypeNext {
    }

    /**
     * Returns {@code true} if <var>next</var> is the built-in {@code %SetIteratorPrototype%.next} function for the
     * requested realm.
     * 
     * @param realm
     *            the function realm
     * @param next
     *            the next function
     * @return {@code true} if <var>next</var> is the built-in {@code %SetIteratorPrototype%.next} function
     */
    public static boolean isBuiltinNext(Realm realm, Object next) {
        return NativeFunction.isNative(realm, next, SetIteratorPrototypeNext.class);
    }

    /**
     * 23.2.5.2 The %SetIteratorPrototype% Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.IteratorPrototype;

        /**
         * 23.2.5.2.1 %SetIteratorPrototype%.next( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the next iterator result object
         */
        @Function(name = "next", arity = 0, nativeId = SetIteratorPrototypeNext.class)
        public static Object next(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            if (!(thisValue instanceof SetIteratorObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleThis, "%SetIteratorPrototype%.next",
                        Type.of(thisValue).toString());
            }
            SetIteratorObject o = (SetIteratorObject) thisValue;
            /* steps 4-5 */
            Iterator<Entry<Object, Void>> iter = o.getIterator();
            /* step 6 */
            SetIterationKind itemKind = o.getIterationKind();
            /* step 7 */
            if (iter == null) {
                return CreateIterResultObject(cx, UNDEFINED, true);
            }
            /* step 8 (implicit) */
            /* steps 9-10 */
            if (iter.hasNext()) {
                Entry<Object, Void> e = iter.next();
                Object result;
                if (itemKind != SetIterationKind.KeyValue) {
                    result = e.getKey();
                } else {
                    result = CreateArrayFromList(cx, e.getKey(), e.getKey());
                }
                return CreateIterResultObject(cx, result, false);
            }
            /* step 11 */
            o.setIterator(null);
            /* step 12 */
            return CreateIterResultObject(cx, UNDEFINED, true);
        }

        /**
         * 23.2.5.2.2 %SetIteratorPrototype% [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Set Iterator";
    }
}

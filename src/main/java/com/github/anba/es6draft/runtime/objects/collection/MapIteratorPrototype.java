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
import com.github.anba.es6draft.runtime.objects.collection.MapIteratorObject.MapIterationKind;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.NativeFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>23 Keyed Collection</h1><br>
 * <h2>23.1 Map Objects</h2>
 * <ul>
 * <li>23.1.5 Map Iterator Objects
 * </ul>
 */
public final class MapIteratorPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Map Iterator prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public MapIteratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * 23.1.5.1 CreateMapIterator Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param obj
     *            the map object
     * @param kind
     *            the map iteration kind
     * @param method
     *            the method name
     * @return the new map iterator
     */
    public static MapIteratorObject CreateMapIterator(ExecutionContext cx, Object obj, MapIterationKind kind,
            String method) {
        /* steps 1-2 */
        if (!(obj instanceof MapObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(obj).toString());
        }
        MapObject map = (MapObject) obj;
        /* steps 3-7 */
        return new MapIteratorObject(cx.getRealm(), map, kind, cx.getIntrinsic(Intrinsics.MapIteratorPrototype));
    }

    /**
     * 23.1.5.1 CreateMapIterator Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param iterator
     *            the iterator
     * @param kind
     *            the map iteration kind
     * @return the new map iterator
     */
    public static MapIteratorObject CreateMapIterator(ExecutionContext cx, Iterator<Entry<Object, Object>> iterator,
            MapIterationKind kind) {
        /* steps 1-7 */
        return new MapIteratorObject(cx.getRealm(), iterator, kind, cx.getIntrinsic(Intrinsics.MapIteratorPrototype));
    }

    /**
     * Marker class for {@code %MapIteratorPrototype%.next}.
     */
    private static final class MapIteratorPrototypeNext {
    }

    /**
     * Returns {@code true} if <var>next</var> is the built-in {@code %MapIteratorPrototype%.next} function for the
     * requested realm.
     * 
     * @param realm
     *            the function realm
     * @param next
     *            the next function
     * @return {@code true} if <var>next</var> is the built-in {@code %MapIteratorPrototype%.next} function
     */
    public static boolean isBuiltinNext(Realm realm, Object next) {
        return NativeFunction.isNative(realm, next, MapIteratorPrototypeNext.class);
    }

    /**
     * 23.1.5.2 The %MapIteratorPrototype% Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.IteratorPrototype;

        /**
         * 23.1.5.2.1 %MapIteratorPrototype%.next( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the next iterator result object
         */
        @Function(name = "next", arity = 0, nativeId = MapIteratorPrototypeNext.class)
        public static Object next(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            if (!(thisValue instanceof MapIteratorObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleThis, "%MapIteratorPrototype%.next",
                        Type.of(thisValue).toString());
            }
            MapIteratorObject o = (MapIteratorObject) thisValue;
            /* steps 4-5 */
            Iterator<Entry<Object, Object>> iter = o.getIterator();
            /* step 6 */
            MapIterationKind itemKind = o.getIterationKind();
            /* step 7 */
            if (iter == null) {
                return CreateIterResultObject(cx, UNDEFINED, true);
            }
            /* step 8 (implicit) */
            /* steps 9-10 */
            if (iter.hasNext()) {
                Entry<Object, Object> e = iter.next();
                Object result;
                if (itemKind == MapIterationKind.Key) {
                    result = e.getKey();
                } else if (itemKind == MapIterationKind.Value) {
                    result = e.getValue();
                } else {
                    assert itemKind == MapIterationKind.KeyValue;
                    result = CreateArrayFromList(cx, e.getKey(), e.getValue());
                }
                return CreateIterResultObject(cx, result, false);
            }
            /* step 11 */
            o.setIterator(null);
            /* step 12 */
            return CreateIterResultObject(cx, UNDEFINED, true);
        }

        /**
         * 23.1.5.2.2 %MapIteratorPrototype% [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Map Iterator";
    }
}

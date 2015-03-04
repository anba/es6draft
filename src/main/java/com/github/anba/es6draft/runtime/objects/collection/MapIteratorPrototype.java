/**
 * Copyright (c) 2012-2015 André Bargull
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
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
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

    public enum MapIterationKind {
        Key, Value, KeyValue
    }

    /**
     * 23.1.5.3 Properties of Map Iterator Instances
     */
    private static final class MapIterator extends OrdinaryObject {
        /** [[Map]] / [[MapNextIndex]] */
        Iterator<Entry<Object, Object>> iterator;

        /** [[MapIterationKind]] */
        final MapIterationKind iterationKind;

        MapIterator(Realm realm, MapObject map, MapIterationKind kind, ScriptObject prototype) {
            super(realm);
            this.iterator = map.getMapData().iterator();
            this.iterationKind = kind;
            setPrototype(prototype);
        }
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
     * @return the new map iterator
     */
    public static OrdinaryObject CreateMapIterator(ExecutionContext cx, Object obj,
            MapIterationKind kind) {
        /* steps 1-2 */
        if (!(obj instanceof MapObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        MapObject map = (MapObject) obj;
        /* steps 3-7 */
        return new MapIterator(cx.getRealm(), map, kind,
                cx.getIntrinsic(Intrinsics.MapIteratorPrototype));
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
        @Function(name = "next", arity = 0)
        public static Object next(ExecutionContext cx, Object thisValue) {
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 3 */
            if (!(thisValue instanceof MapIterator)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            /* step 1 */
            MapIterator o = (MapIterator) thisValue;
            /* steps 4-5 */
            Iterator<Entry<Object, Object>> iter = o.iterator;
            /* step 6 */
            MapIterationKind itemKind = o.iterationKind;
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
            o.iterator = null;
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

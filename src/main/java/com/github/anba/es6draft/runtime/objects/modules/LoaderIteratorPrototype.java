/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.modules;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.CreateIterResultObject;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;

import java.util.Iterator;
import java.util.Map.Entry;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.modules.ModuleObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>1 Modules: Semantics</h1><br>
 * <h2>1.6 Loader Objects</h2>
 * <ul>
 * <li>1.6.4 Loader Iterator Objects
 * </ul>
 */
public class LoaderIteratorPrototype extends OrdinaryObject implements Initialisable {
    public LoaderIteratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    public enum MapIterationKind {
        Key, Value, KeyValue
    }

    /**
     * 1.6.4 Loader Iterator Objects
     */
    private static class LoaderIterator extends OrdinaryObject {
        /** [[Loader]] */
        LoaderObject loader;

        /** [[ModuleMapNextIndex]] */
        @SuppressWarnings("unused")
        int nextIndex;

        /** [[MapIterationKind]] */
        MapIterationKind iterationKind;

        Iterator<Entry<String, ModuleObject>> iterator;

        LoaderIterator(Realm realm) {
            super(realm);
        }
    }

    private static class LoaderIteratorAllocator implements ObjectAllocator<LoaderIterator> {
        static final ObjectAllocator<LoaderIterator> INSTANCE = new LoaderIteratorAllocator();

        @Override
        public LoaderIterator newInstance(Realm realm) {
            return new LoaderIterator(realm);
        }
    }

    /**
     * 1.6.4.1 CreateLoaderIterator(loader, kind) Abstract Operation
     */
    public static OrdinaryObject CreateLoaderIterator(ExecutionContext cx, Object obj,
            MapIterationKind kind) {
        /* steps 1-2 */
        if (!(obj instanceof LoaderObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        LoaderObject loader = (LoaderObject) obj;
        if (loader.getModules() == null) {
            throw newTypeError(cx, Messages.Key.UninitialisedObject);
        }
        /* step 3 */
        LoaderIterator iterator = ObjectCreate(cx, Intrinsics.LoaderIteratorPrototype,
                LoaderIteratorAllocator.INSTANCE);
        /* steps 4-6*/
        iterator.loader = loader;
        iterator.nextIndex = 0;
        iterator.iterationKind = kind;
        iterator.iterator = loader.getModules().iterator();
        /* step 7 */
        return iterator;
    }

    /**
     * 1.6.4.2 The %LoaderIteratorPrototype% Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 1.6.4.2.1 %LoaderIteratorPrototype%.next ( )
         */
        @Function(name = "next", arity = 0)
        public static Object next(ExecutionContext cx, Object thisValue) {
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 3 */
            if (!(thisValue instanceof LoaderIterator)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            /* step 1 */
            LoaderIterator o = (LoaderIterator) thisValue;
            /* step 4 */
            LoaderObject loader = o.loader;
            /* step 5 */
            // int index = o.nextIndex;
            /* step 6 */
            MapIterationKind itemKind = o.iterationKind;
            // FIXME: spec bug - not updated to new Iterator contract
            if (loader == null) {
                return CreateIterResultObject(cx, UNDEFINED, true);
            }
            /* step 7 */
            assert loader.getModules() != null;
            /* step 8 */
            Iterator<Entry<String, ModuleObject>> iter = o.iterator;
            if (iter.hasNext()) {
                Entry<String, ModuleObject> e = iter.next();
                assert e != null;
                Object result;
                if (itemKind == MapIterationKind.Key) {
                    result = e.getKey();
                } else if (itemKind == MapIterationKind.Value) {
                    // FIXME: spec bug? need to protect against returning half-init modules?
                    result = e.getValue();
                } else {
                    // FIXME: spec bug? need to protect against returning half-init modules?
                    assert itemKind == MapIterationKind.KeyValue;
                    ScriptObject array = ArrayCreate(cx, 2);
                    CreateDataProperty(cx, array, "0", e.getKey());
                    CreateDataProperty(cx, array, "1", e.getValue());
                    result = array;
                }
                return CreateIterResultObject(cx, result, false);
            }
            // FIXME: spec bug - not updated to new Iterator contract
            o.loader = null;
            /* step 9 */
            return CreateIterResultObject(cx, UNDEFINED, true);
        }

        /**
         * 1.6.4.2.2 %LoaderIteratorPrototype% [ @@iterator ] ()
         */
        @Function(name = "[Symbol.iterator]", symbol = BuiltinSymbol.iterator, arity = 0)
        public static Object iterator(ExecutionContext cx, Object thisValue) {
            return thisValue;
        }

        /**
         * 1.6.4.2.3 %LoaderIteratorPrototype% [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "Loader Iterator";
    }
}

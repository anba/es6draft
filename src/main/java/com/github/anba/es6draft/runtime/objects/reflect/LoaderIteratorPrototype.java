/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

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
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.modules.ModuleLinkage;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>26 Reflection</h1><br>
 * <h2>26.3 Loader Objects</h2>
 * <ul>
 * <li>26.3.5 Loader Iterator Objects
 * </ul>
 */
public final class LoaderIteratorPrototype extends OrdinaryObject implements Initializable {
    public LoaderIteratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    public enum LoaderIterationKind {
        Key, Value, KeyValue
    }

    /**
     * 26.3.5.3 Properties of Loader Iterator Instances
     */
    private static final class LoaderIterator extends OrdinaryObject {
        /** [[Loader]] */
        LoaderObject loader;

        /** [[LoaderNextIndex]] */
        @SuppressWarnings("unused")
        int nextIndex;

        /** [[LoaderIterationKind]] */
        LoaderIterationKind iterationKind;

        Iterator<Entry<String, ModuleLinkage>> iterator;

        LoaderIterator(Realm realm) {
            super(realm);
        }
    }

    private static final class LoaderIteratorAllocator implements ObjectAllocator<LoaderIterator> {
        static final ObjectAllocator<LoaderIterator> INSTANCE = new LoaderIteratorAllocator();

        @Override
        public LoaderIterator newInstance(Realm realm) {
            return new LoaderIterator(realm);
        }
    }

    /**
     * 26.3.5.1 CreateLoaderIterator Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param loader
     *            the loader object
     * @param kind
     *            the loder iteration kind
     * @return the new loader iterator
     */
    public static OrdinaryObject CreateLoaderIterator(ExecutionContext cx, LoaderObject loader,
            LoaderIterationKind kind) {
        /* step 1 */
        assert loader.getLoader() != null;
        /* step 2 */
        LoaderIterator iterator = ObjectCreate(cx, Intrinsics.LoaderIteratorPrototype,
                LoaderIteratorAllocator.INSTANCE);
        /* steps 3-5 */
        iterator.loader = loader;
        iterator.nextIndex = 0;
        iterator.iterationKind = kind;
        iterator.iterator = loader.getLoader().getModules().iterator();
        /* step 6 */
        return iterator;
    }

    /**
     * 26.3.5.2 The %LoaderIteratorPrototype% Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 26.3.5.2.1 %LoaderIteratorPrototype%.next( )
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
            if (!(thisValue instanceof LoaderIterator)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            /* step 1 */
            LoaderIterator o = (LoaderIterator) thisValue;
            /* step 4 */
            LoaderObject m = o.loader;
            /* step 6 */
            // int index = o.nextIndex;
            /* step 7 */
            LoaderIterationKind itemKind = o.iterationKind;
            /* step 8 */
            if (m == null) {
                return CreateIterResultObject(cx, UNDEFINED, true);
            }
            /* step 5 (not applicable) */
            /* steps 9-10 */
            Iterator<Entry<String, ModuleLinkage>> iter = o.iterator;
            if (iter.hasNext()) {
                Entry<String, ModuleLinkage> e = iter.next();
                assert e != null;
                Object result;
                if (itemKind == LoaderIterationKind.Key) {
                    result = e.getKey();
                } else if (itemKind == LoaderIterationKind.Value) {
                    // FIXME: spec bug? need to protect against returning half-init modules?
                    result = e.getValue().getModuleObject();
                } else {
                    // FIXME: spec bug? need to protect against returning half-init modules?
                    assert itemKind == LoaderIterationKind.KeyValue;
                    ScriptObject array = ArrayCreate(cx, 2);
                    CreateDataProperty(cx, array, "0", e.getKey());
                    CreateDataProperty(cx, array, "1", e.getValue().getModuleObject());
                    result = array;
                }
                return CreateIterResultObject(cx, result, false);
            }
            /* step 11 */
            o.loader = null;
            /* step 12 */
            return CreateIterResultObject(cx, UNDEFINED, true);
        }

        /**
         * 26.3.5.2.2 %LoaderIteratorPrototype% [ @@iterator ] ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the this-value
         */
        @Function(name = "[Symbol.iterator]", symbol = BuiltinSymbol.iterator, arity = 0)
        public static Object iterator(ExecutionContext cx, Object thisValue) {
            return thisValue;
        }

        /**
         * 26.3.5.2.3 %LoaderIteratorPrototype% [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "Loader Iterator";
    }
}

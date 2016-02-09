/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.collection.SetIteratorPrototype.CreateSetIterator;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.Map.Entry;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.LinkedMap;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.AliasFunction;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.collection.SetIteratorObject.SetIterationKind;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.NativeFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>23 Keyed Collection</h1><br>
 * <h2>23.2 Set Objects</h2>
 * <ul>
 * <li>23.2.3 Properties of the Set Prototype Object
 * </ul>
 */
public final class SetPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Set prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public SetPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * Marker class for {@code Set.prototype.add}.
     */
    private static final class SetPrototypeAdd {
    }

    /**
     * Returns {@code true} if <var>add</var> is the built-in {@code Set.prototype.add} function.
     * 
     * @param add
     *            the add function
     * @return {@code true} if <var>add</var> is the built-in {@code Set.prototype.add} function
     */
    static boolean isBuiltinAdd(Object add) {
        return NativeFunction.isNative(add, SetPrototypeAdd.class);
    }

    /**
     * Marker class for {@code Set.prototype.values}.
     */
    private static final class SetPrototypeValues {
    }

    /**
     * Returns {@code true} if <var>values</var> is the built-in {@code Set.prototype.values} function for the requested
     * realm.
     * 
     * @param realm
     *            the function realm
     * @param values
     *            the values function
     * @return {@code true} if <var>values</var> is the built-in {@code Set.prototype.values} function
     */
    public static boolean isBuiltinValues(Realm realm, Object values) {
        return NativeFunction.isNative(realm, values, SetPrototypeValues.class);
    }

    /**
     * 23.2.3 Properties of the Set Prototype Object
     */
    public enum Properties {
        ;

        private static SetObject thisSetObject(ExecutionContext cx, Object obj) {
            if (obj instanceof SetObject) {
                return (SetObject) obj;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 23.2.3.3 Set.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Set;

        /**
         * 23.2.3.1 Set.prototype.add (value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the new value
         * @return this set object
         */
        @Function(name = "add", arity = 1, nativeId = SetPrototypeAdd.class)
        public static Object add(ExecutionContext cx, Object thisValue, Object value) {
            /* steps 1-3 */
            SetObject s = thisSetObject(cx, thisValue);
            /* step 4 */
            LinkedMap<Object, Void> entries = s.getSetData();
            /* steps 5-7 */
            entries.set(value, null);
            /* step 8 */
            return s;
        }

        /**
         * 23.2.3.2 Set.prototype.clear ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the undefined value
         */
        @Function(name = "clear", arity = 0)
        public static Object clear(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            SetObject s = thisSetObject(cx, thisValue);
            /* step 4 */
            LinkedMap<Object, Void> entries = s.getSetData();
            /* step 5 */
            entries.clear();
            /* step 6 */
            return UNDEFINED;
        }

        /**
         * 23.2.3.4 Set.prototype.delete ( value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the value
         * @return {@code true} if the entry was deleted
         */
        @Function(name = "delete", arity = 1)
        public static Object delete(ExecutionContext cx, Object thisValue, Object value) {
            /* steps 1-3 */
            SetObject s = thisSetObject(cx, thisValue);
            /* step 4 */
            LinkedMap<Object, Void> entries = s.getSetData();
            /* steps 5-6 */
            return entries.delete(value);
        }

        /**
         * 23.2.3.5 Set.prototype.entries ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the entries iterator
         */
        @Function(name = "entries", arity = 0)
        public static Object entries(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            return CreateSetIterator(cx, thisValue, SetIterationKind.KeyValue);
        }

        /**
         * 23.2.3.6 Set.prototype.forEach ( callbackfn [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param callbackfn
         *            the callback function
         * @param thisArg
         *            the optional this-argument
         * @return the undefined value
         */
        @Function(name = "forEach", arity = 1)
        public static Object forEach(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            /* steps 1-3 */
            SetObject s = thisSetObject(cx, thisValue);
            /* step 4 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 5 (omitted) */
            /* step 6 */
            LinkedMap<Object, Void> entries = s.getSetData();
            /* step 7 */
            for (Entry<Object, Void> e : entries) {
                callback.call(cx, thisArg, e.getKey(), e.getKey(), s);
            }
            /* step 8 */
            return UNDEFINED;
        }

        /**
         * 23.2.3.7 Set.prototype.has ( value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the value
         * @return {@code true} if the entry was found
         */
        @Function(name = "has", arity = 1)
        public static Object has(ExecutionContext cx, Object thisValue, Object value) {
            /* steps 1-3 */
            SetObject s = thisSetObject(cx, thisValue);
            /* step 4 */
            LinkedMap<Object, Void> entries = s.getSetData();
            /* steps 5-6 */
            return entries.has(value);
        }

        /**
         * 23.2.3.9 get Set.prototype.size
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the number of entries
         */
        @Accessor(name = "size", type = Accessor.Type.Getter)
        public static Object size(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            SetObject s = thisSetObject(cx, thisValue);
            /* step 4 */
            LinkedMap<Object, Void> entries = s.getSetData();
            /* steps 5-7 */
            return entries.size();
        }

        /**
         * 23.2.3.8 Set.prototype.keys ( )<br>
         * 23.2.3.10 Set.prototype.values ( )<br>
         * 23.2.3.11 Set.prototype[ @@iterator ] ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the values iterator
         */
        @Function(name = "values", arity = 0, nativeId = SetPrototypeValues.class)
        @AliasFunction(name = "keys")
        @AliasFunction(name = "[Symbol.iterator]", symbol = BuiltinSymbol.iterator)
        public static Object values(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            return CreateSetIterator(cx, thisValue, SetIterationKind.Value);
        }

        /**
         * 23.2.3.12 Set.prototype[ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Set";
    }
}

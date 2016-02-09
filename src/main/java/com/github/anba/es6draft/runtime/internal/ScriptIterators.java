/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.objects.iteration.GeneratorAbstractOperations.GeneratorResume;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.Iterator;
import java.util.Map.Entry;

import org.mozilla.javascript.ConsString;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.objects.ArrayIteratorObject;
import com.github.anba.es6draft.runtime.objects.ArrayIteratorPrototype;
import com.github.anba.es6draft.runtime.objects.ArrayPrototype;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayObject;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayPrototypePrototype;
import com.github.anba.es6draft.runtime.objects.collection.MapIteratorObject;
import com.github.anba.es6draft.runtime.objects.collection.MapIteratorPrototype;
import com.github.anba.es6draft.runtime.objects.collection.MapObject;
import com.github.anba.es6draft.runtime.objects.collection.MapPrototype;
import com.github.anba.es6draft.runtime.objects.collection.SetIteratorObject;
import com.github.anba.es6draft.runtime.objects.collection.SetIteratorPrototype;
import com.github.anba.es6draft.runtime.objects.collection.SetObject;
import com.github.anba.es6draft.runtime.objects.collection.SetPrototype;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorPrototype;
import com.github.anba.es6draft.runtime.objects.iteration.IteratorPrototype;
import com.github.anba.es6draft.runtime.objects.iteration.ListIterator;
import com.github.anba.es6draft.runtime.objects.text.StringIteratorPrototype;
import com.github.anba.es6draft.runtime.objects.text.StringPrototype;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.builtins.ArgumentsObject;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * 
 */
public final class ScriptIterators {
    private ScriptIterators() {
    }

    /**
     * 
     * @param cx
     *            the execution context
     * @param map
     *            the map object
     * @return {@code true}
     */
    public static boolean isBuiltinIterator(ExecutionContext cx, MapObject map) {
        return MapScriptIterator.isBuiltinIterator(cx, map);
    }

    /**
     * 
     * @param cx
     *            the execution context
     * @param set
     *            the set object
     * @return {@code true}
     */
    public static boolean isBuiltinIterator(ExecutionContext cx, SetObject set) {
        return SetScriptIterator.isBuiltinIterator(cx, set);
    }

    /**
     * Returns a {@link ScriptIterator} for {@code iterable}.
     * 
     * @param cx
     *            the execution context
     * @param iterable
     *            the iterable object
     * @return the iterator object
     */
    public static ScriptIterator<?> GetScriptIterator(ExecutionContext cx, Object iterable) {
        if (iterable instanceof ArrayObject) {
            ArrayObject array = (ArrayObject) iterable;
            if (ArrayScriptIterator.isBuiltinIterator(cx, array)) {
                return new ArrayScriptIterator(cx, array);
            }
        } else if (iterable instanceof ArrayIteratorObject) {
            ArrayIteratorObject arrayIterator = (ArrayIteratorObject) iterable;
            if (ArrayIteratorScriptIterator.isBuiltinIterator(cx, arrayIterator)) {
                return new ArrayIteratorScriptIterator(cx, arrayIterator);
            }
        } else if (iterable instanceof TypedArrayObject) {
            TypedArrayObject typedArray = (TypedArrayObject) iterable;
            if (TypedArrayScriptIterator.isBuiltinIterator(cx, typedArray)) {
                return new TypedArrayScriptIterator(cx, typedArray);
            }
        } else if (iterable instanceof String || iterable instanceof ConsString) {
            if (StringScriptIterator.isBuiltinIterator(cx)) {
                return new StringScriptIterator(cx, iterable.toString());
            }
        } else if (iterable instanceof MapObject) {
            MapObject map = (MapObject) iterable;
            if (MapScriptIterator.isBuiltinIterator(cx, map)) {
                return new MapScriptIterator(cx, map);
            }
        } else if (iterable instanceof MapIteratorObject) {
            MapIteratorObject mapIterator = (MapIteratorObject) iterable;
            if (MapIteratorScriptIterator.isBuiltinIterator(cx, mapIterator)) {
                return new MapIteratorScriptIterator(cx, mapIterator);
            }
        } else if (iterable instanceof SetObject) {
            SetObject set = (SetObject) iterable;
            if (SetScriptIterator.isBuiltinIterator(cx, set)) {
                return new SetScriptIterator(cx, set);
            }
        } else if (iterable instanceof SetIteratorObject) {
            SetIteratorObject setIterator = (SetIteratorObject) iterable;
            if (SetIteratorScriptIterator.isBuiltinIterator(cx, setIterator)) {
                return new SetIteratorScriptIterator(cx, setIterator);
            }
        } else if (iterable instanceof ArgumentsObject) {
            ArgumentsObject arguments = (ArgumentsObject) iterable;
            if (ArgumentsScriptIterator.isBuiltinIterator(cx, arguments)) {
                return new ArgumentsScriptIterator(cx, arguments);
            }
        } else if (iterable instanceof ListIterator<?>) {
            ListIterator<?> listIterator = (ListIterator<?>) iterable;
            if (ListIteratorScriptIterator.isBuiltinIterator(cx, listIterator)) {
                return new ListIteratorScriptIterator(cx, listIterator);
            }
        } else if (iterable instanceof GeneratorObject) {
            GeneratorObject generator = (GeneratorObject) iterable;
            if (GeneratorScriptIterator.isBuiltinIterator(cx, generator)) {
                return new GeneratorScriptIterator(cx, generator);
            }
        }
        return new ScriptIteratorImpl(cx, GetIterator(cx, iterable));
    }

    /**
     * Returns a {@link ScriptIterator} for {@code iterable}.
     * 
     * @param cx
     *            the execution context
     * @param iterable
     *            the iterable object
     * @param method
     *            the iterator method
     * @return the iterator object
     */
    public static ScriptIterator<?> GetScriptIterator(ExecutionContext cx, Object iterable, Callable method) {
        if (iterable instanceof ArrayObject) {
            ArrayObject array = (ArrayObject) iterable;
            if (ArrayScriptIterator.isBuiltinIterator(cx, array, method)) {
                return new ArrayScriptIterator(cx, array);
            }
        } else if (iterable instanceof TypedArrayObject) {
            TypedArrayObject typedArray = (TypedArrayObject) iterable;
            if (TypedArrayScriptIterator.isBuiltinIterator(cx, typedArray, method)) {
                return new TypedArrayScriptIterator(cx, typedArray);
            }
        } else if (iterable instanceof String || iterable instanceof ConsString) {
            if (StringScriptIterator.isBuiltinIterator(cx, method)) {
                return new StringScriptIterator(cx, iterable.toString());
            }
        } else if (iterable instanceof MapObject) {
            MapObject map = (MapObject) iterable;
            if (MapScriptIterator.isBuiltinIterator(cx, map, method)) {
                return new MapScriptIterator(cx, map);
            }
        } else if (iterable instanceof SetObject) {
            SetObject set = (SetObject) iterable;
            if (SetScriptIterator.isBuiltinIterator(cx, set, method)) {
                return new SetScriptIterator(cx, set);
            }
        } else if (iterable instanceof ArgumentsObject) {
            ArgumentsObject arguments = (ArgumentsObject) iterable;
            if (ArgumentsScriptIterator.isBuiltinIterator(cx, arguments, method)) {
                return new ArgumentsScriptIterator(cx, arguments);
            }
        }
        return new ScriptIteratorImpl(cx, GetIterator(cx, iterable, method));
    }

    /**
     * Returns a {@link ScriptIterator} for {@code iterator}. {@code iterator} is expected to comply to the
     * <code>"25.1.2 The Iterator Interface"</code>.
     * 
     * @param cx
     *            the execution context
     * @param iterator
     *            the script iterator object
     * @return the iterator object
     */
    public static ScriptIterator<?> ToScriptIterator(ExecutionContext cx, ScriptObject iterator) {
        return new ScriptIteratorImpl(cx, iterator);
    }

    private static final class ScriptIteratorImpl extends SimpleIterator<Object> implements ScriptIterator<Object> {
        private final ExecutionContext cx;
        private final ScriptObject iterator;
        private boolean done = false;

        ScriptIteratorImpl(ExecutionContext cx, ScriptObject iterator) {
            this.cx = cx;
            this.iterator = iterator;
        }

        @Override
        protected Object findNext() throws ScriptException {
            if (!done) {
                try {
                    ScriptObject next = IteratorStep(cx, iterator);
                    if (next != null) {
                        return IteratorValue(cx, next);
                    }
                } catch (ScriptException e) {
                    done = true;
                    throw e;
                }
                done = true;
            }
            return null;
        }

        @Override
        public void close() throws ScriptException {
            if (!done) {
                IteratorClose(cx, iterator);
            }
        }

        @Override
        public void close(Throwable cause) throws ScriptException {
            if (!done) {
                IteratorClose(cx, iterator, cause);
            }
        }
    }

    private static abstract class BuiltinScriptIterator extends SimpleIterator<Object>
            implements ScriptIterator<Object> {
        protected final ExecutionContext cx;
        protected ScriptObject iteratorObject;
        protected boolean done = false;

        protected BuiltinScriptIterator(ExecutionContext cx) {
            this.cx = cx;
        }

        protected static final Property findNextProperty(OrdinaryObject object) {
            final int MAX_PROTO_CHAIN_LENGTH = 5;
            for (int i = 0; i < MAX_PROTO_CHAIN_LENGTH; ++i) {
                Property iterProp = object.lookupOwnProperty("next");
                if (iterProp != null) {
                    return iterProp;
                }
                ScriptObject proto = object.getPrototype();
                if (!(proto instanceof OrdinaryObject)) {
                    break;
                }
                object = (OrdinaryObject) proto;
            }
            return null;
        }

        protected static final Property findIteratorProperty(OrdinaryObject object) {
            final int MAX_PROTO_CHAIN_LENGTH = 5;
            Symbol name = BuiltinSymbol.iterator.get();
            for (int i = 0; i < MAX_PROTO_CHAIN_LENGTH; ++i) {
                Property iterProp = object.lookupOwnProperty(name);
                if (iterProp != null) {
                    return iterProp;
                }
                ScriptObject proto = object.getPrototype();
                if (!(proto instanceof OrdinaryObject)) {
                    break;
                }
                object = (OrdinaryObject) proto;
            }
            return null;
        }

        /**
         * Returns the intrinsic iterator prototype.
         * 
         * @return the iterator prototype
         */
        protected abstract Intrinsics getIntrinsic();

        /**
         * Creates an iterator object.
         * 
         * @return the new iterator object
         */
        protected abstract OrdinaryObject createIteratorObject();

        /**
         * Slow path: Perform iteration with an iterator object.
         * 
         * @return the new iterator result or {@code null}
         */
        protected final Object slowNext() {
            assert !done;
            try {
                ScriptObject next = IteratorStep(cx, getScriptObject());
                if (next != null) {
                    return IteratorValue(cx, next);
                }
            } catch (ScriptException e) {
                done = true;
                throw e;
            }
            done = true;
            return null;
        }

        @Override
        public final void close() throws ScriptException {
            if (!done && hasReturn()) {
                IteratorClose(cx, getScriptObject());
            }
        }

        @Override
        public final void close(Throwable cause) throws ScriptException {
            if (!done && hasReturn()) {
                IteratorClose(cx, getScriptObject(), cause);
            }
        }

        private ScriptObject getScriptObject() {
            if (iteratorObject == null) {
                iteratorObject = createIteratorObject();
            }
            return iteratorObject;
        }

        private boolean hasReturn() {
            if (iteratorObject != null) {
                return true;
            }
            OrdinaryObject iterProto = cx.getIntrinsic(getIntrinsic());
            for (;;) {
                if (iterProto.lookupOwnProperty("return") != null) {
                    return true;
                }
                ScriptObject proto = iterProto.getPrototype();
                if (proto == null) {
                    return false;
                }
                if (!(proto instanceof OrdinaryObject)) {
                    return true;
                }
                iterProto = (OrdinaryObject) proto;
            }
        }
    }

    private static final class ArrayScriptIterator extends BuiltinScriptIterator {
        private final ArrayObject array;
        private long index;

        ArrayScriptIterator(ExecutionContext cx, ArrayObject array) {
            super(cx);
            this.array = array;
            this.index = 0;
        }

        static boolean isBuiltinIterator(ExecutionContext cx, ArrayObject array) {
            Property iterProp = findIteratorProperty(array);
            return iterProp != null && isBuiltinIterator(cx, array, iterProp.getValue());
        }

        static boolean isBuiltinIterator(ExecutionContext cx, ArrayObject array, Object method) {
            // Test 1: Is array[Symbol.iterator] == %ArrayPrototype%.values?
            // Test 2: Is %ArrayIteratorPrototype%.next the built-in next method?
            return array.isDenseArray() && ArrayPrototype.isBuiltinValues(cx.getRealm(), method) && isBuiltinNext(cx);
        }

        private static boolean isBuiltinNext(ExecutionContext cx) {
            Property iterNextProp = cx.getIntrinsic(Intrinsics.ArrayIteratorPrototype).lookupOwnProperty("next");
            return iterNextProp != null && ArrayIteratorPrototype.isBuiltinNext(cx.getRealm(), iterNextProp.getValue());
        }

        @Override
        protected OrdinaryObject createIteratorObject() {
            return ArrayIteratorPrototype.CreateArrayIterator(cx, array, index,
                    ArrayIteratorObject.ArrayIterationKind.Value);
        }

        @Override
        protected Intrinsics getIntrinsic() {
            return Intrinsics.ArrayIteratorPrototype;
        }

        @Override
        protected Object findNext() throws ScriptException {
            if (!done) {
                if (iteratorObject == null && isBuiltinNext(cx) && array.isDenseArray()) {
                    if (index < array.getLength()) {
                        return array.getDenseElement(index++);
                    }
                    done = true;
                    return null;
                }
                return slowNext();
            }
            return null;
        }
    }

    private static final class ArrayIteratorScriptIterator extends BuiltinScriptIterator {
        private final ArrayIteratorObject arrayIterator;

        ArrayIteratorScriptIterator(ExecutionContext cx, ArrayIteratorObject arrayIterator) {
            super(cx);
            this.arrayIterator = arrayIterator;
            this.iteratorObject = arrayIterator;
        }

        static boolean isBuiltinIterator(ExecutionContext cx, ArrayIteratorObject arrayIterator) {
            Property iterProp = findIteratorProperty(arrayIterator);
            return iterProp != null && isBuiltinIterator(cx, arrayIterator, iterProp.getValue());
        }

        static boolean isBuiltinIterator(ExecutionContext cx, ArrayIteratorObject arrayIterator, Object method) {
            // Test 1: Is arrayIterator[Symbol.iterator] == %IteratorPrototype%[Symbol.iterator]?
            // Test 2: Is %ArrayIteratorPrototype%.next the built-in next method?
            return IteratorPrototype.isBuiltinIterator(cx.getRealm(), method) && isBuiltinNext(cx, arrayIterator);
        }

        private static boolean isBuiltinNext(ExecutionContext cx, ArrayIteratorObject arrayIterator) {
            Property iterNextProp = findNextProperty(arrayIterator);
            return iterNextProp != null && ArrayIteratorPrototype.isBuiltinNext(cx.getRealm(), iterNextProp.getValue());
        }

        @Override
        protected OrdinaryObject createIteratorObject() {
            throw new AssertionError();
        }

        @Override
        protected Intrinsics getIntrinsic() {
            return Intrinsics.ArrayIteratorPrototype;
        }

        @Override
        protected Object findNext() throws ScriptException {
            if (!done) {
                if (isBuiltinNext(cx, arrayIterator)) {
                    try {
                        ArrayIteratorObject iter = arrayIterator;
                        ScriptObject array = iter.getIteratedObject();
                        if (array == null) {
                            done = true;
                            return null;
                        }
                        long index = iter.getNextIndex();
                        long len;
                        if (array instanceof TypedArrayObject) {
                            len = ((TypedArrayObject) array).getArrayLength();
                        } else if (array instanceof ArrayObject) {
                            len = ((ArrayObject) array).getLength();
                        } else {
                            len = ToLength(cx, Get(cx, array, "length"));
                        }
                        if (index >= len) {
                            iter.setIteratedObject(null);
                            done = true;
                            return null;
                        }
                        iter.setNextIndex(index + 1);
                        switch (iter.getIterationKind()) {
                        case Key:
                            return index;
                        case KeyValue:
                            return CreateArrayFromList(cx, index, Get(cx, array, index));
                        case Value:
                            return Get(cx, array, index);
                        default:
                            throw new AssertionError();
                        }
                    } catch (ScriptException e) {
                        // Don't call `iter.setIteratedObject(null)`, other iterations may reuse the iterator.
                        done = true;
                        throw e;
                    }
                }
                return slowNext();
            }
            return null;
        }
    }

    private static final class TypedArrayScriptIterator extends BuiltinScriptIterator {
        private final TypedArrayObject typedArray;
        private long index;

        TypedArrayScriptIterator(ExecutionContext cx, TypedArrayObject typedArray) {
            super(cx);
            this.typedArray = typedArray;
            this.index = 0;
        }

        static boolean isBuiltinIterator(ExecutionContext cx, TypedArrayObject typedArray) {
            Property iterProp = findIteratorProperty(typedArray);
            return iterProp != null && isBuiltinIterator(cx, typedArray, iterProp.getValue());
        }

        static boolean isBuiltinIterator(ExecutionContext cx, TypedArrayObject typedArray, Object method) {
            // Test 1: Is typedArray[Symbol.iterator] == %TypedArrayPrototype%.values?
            // Test 2: Is %ArrayIteratorPrototype%.next the built-in next method?
            return !typedArray.getBuffer().isDetached()
                    && TypedArrayPrototypePrototype.isBuiltinValues(cx.getRealm(), method) && isBuiltinNext(cx);
        }

        private static boolean isBuiltinNext(ExecutionContext cx) {
            Property iterNextProp = cx.getIntrinsic(Intrinsics.ArrayIteratorPrototype).lookupOwnProperty("next");
            return iterNextProp != null && ArrayIteratorPrototype.isBuiltinNext(cx.getRealm(), iterNextProp.getValue());
        }

        @Override
        protected OrdinaryObject createIteratorObject() {
            return ArrayIteratorPrototype.CreateArrayIterator(cx, typedArray, index,
                    ArrayIteratorObject.ArrayIterationKind.Value);
        }

        @Override
        protected Intrinsics getIntrinsic() {
            return Intrinsics.ArrayIteratorPrototype;
        }

        @Override
        protected Object findNext() throws ScriptException {
            if (!done) {
                if (iteratorObject == null && isBuiltinNext(cx) && !typedArray.getBuffer().isDetached()) {
                    if (index < typedArray.getArrayLength()) {
                        return typedArray.get(cx, index++, typedArray);
                    }
                    done = true;
                    return null;
                }
                return slowNext();
            }
            return null;
        }
    }

    private static final class ArgumentsScriptIterator extends BuiltinScriptIterator {
        private final ArgumentsObject arguments;
        private long index;

        ArgumentsScriptIterator(ExecutionContext cx, ArgumentsObject arguments) {
            super(cx);
            this.arguments = arguments;
            this.index = 0;
        }

        static boolean isBuiltinIterator(ExecutionContext cx, ArgumentsObject arguments) {
            Property iterProp = findIteratorProperty(arguments);
            return iterProp != null && isBuiltinIterator(cx, arguments, iterProp.getValue());
        }

        static boolean isBuiltinIterator(ExecutionContext cx, ArgumentsObject arguments, Object method) {
            // Test 1: Is arguments[Symbol.iterator] == %ArrayPrototype%.values?
            // Test 2: Is %ArrayIteratorPrototype%.next the built-in next method?
            return arguments.isDenseArray() && ArrayPrototype.isBuiltinValues(cx.getRealm(), method)
                    && isBuiltinNext(cx);
        }

        private static boolean isBuiltinNext(ExecutionContext cx) {
            Property iterNextProp = cx.getIntrinsic(Intrinsics.ArrayIteratorPrototype).lookupOwnProperty("next");
            return iterNextProp != null && ArrayIteratorPrototype.isBuiltinNext(cx.getRealm(), iterNextProp.getValue());
        }

        @Override
        protected OrdinaryObject createIteratorObject() {
            return ArrayIteratorPrototype.CreateArrayIterator(cx, arguments, index,
                    ArrayIteratorObject.ArrayIterationKind.Value);
        }

        @Override
        protected Intrinsics getIntrinsic() {
            return Intrinsics.ArrayIteratorPrototype;
        }

        @Override
        protected Object findNext() throws ScriptException {
            if (!done) {
                if (iteratorObject == null && isBuiltinNext(cx)) {
                    long length = arguments.getLength();
                    if (arguments.isDenseArray(length)) {
                        if (index < length) {
                            return arguments.get(cx, index++, arguments);
                        }
                        done = true;
                        return null;
                    }
                }
                return slowNext();
            }
            return null;
        }
    }

    private static final class StringScriptIterator extends BuiltinScriptIterator {
        private final String string;
        private int index;

        StringScriptIterator(ExecutionContext cx, String string) {
            super(cx);
            this.string = string;
            this.index = 0;
        }

        static boolean isBuiltinIterator(ExecutionContext cx) {
            Property iterProp = findIteratorProperty(cx.getIntrinsic(Intrinsics.StringPrototype));
            return iterProp != null && isBuiltinIterator(cx, iterProp.getValue());
        }

        static boolean isBuiltinIterator(ExecutionContext cx, Object method) {
            // Test 1: Is string[Symbol.iterator] == %StringPrototype%.iterator?
            // Test 2: Is %StringIteratorPrototype%.next the built-in next method?
            return StringPrototype.isBuiltinIterator(cx.getRealm(), method) && isBuiltinNext(cx);
        }

        private static boolean isBuiltinNext(ExecutionContext cx) {
            Property iterNextProp = cx.getIntrinsic(Intrinsics.StringIteratorPrototype).lookupOwnProperty("next");
            return iterNextProp != null
                    && StringIteratorPrototype.isBuiltinNext(cx.getRealm(), iterNextProp.getValue());
        }

        @Override
        protected OrdinaryObject createIteratorObject() {
            return StringIteratorPrototype.CreateStringIterator(cx, string, index);
        }

        @Override
        protected Intrinsics getIntrinsic() {
            return Intrinsics.StringIteratorPrototype;
        }

        @Override
        protected Object findNext() throws ScriptException {
            if (!done) {
                if (iteratorObject == null && isBuiltinNext(cx)) {
                    if (index < string.length()) {
                        int cp = string.codePointAt(index);
                        index += Character.charCount(cp);
                        return Strings.fromCodePoint(cp);
                    }
                    done = true;
                    return null;
                }
                return slowNext();
            }
            return null;
        }
    }

    private static final class MapScriptIterator extends BuiltinScriptIterator {
        private final Iterator<Entry<Object, Object>> iterator;

        MapScriptIterator(ExecutionContext cx, MapObject map) {
            super(cx);
            this.iterator = map.getMapData().iterator();
        }

        static boolean isBuiltinIterator(ExecutionContext cx, MapObject map) {
            Property iterProp = findIteratorProperty(map);
            return iterProp != null && isBuiltinIterator(cx, map, iterProp.getValue());
        }

        static boolean isBuiltinIterator(ExecutionContext cx, MapObject map, Object method) {
            // Test 1: Is map[Symbol.iterator] == %MapPrototype%.entries?
            // Test 2: Is %MapIteratorPrototype%.next the built-in next method?
            return MapPrototype.isBuiltinEntries(cx.getRealm(), method) && isBuiltinNext(cx);
        }

        private static boolean isBuiltinNext(ExecutionContext cx) {
            Property iterNextProp = cx.getIntrinsic(Intrinsics.MapIteratorPrototype).lookupOwnProperty("next");
            return iterNextProp != null && MapIteratorPrototype.isBuiltinNext(cx.getRealm(), iterNextProp.getValue());
        }

        @Override
        protected OrdinaryObject createIteratorObject() {
            return MapIteratorPrototype.CreateMapIterator(cx, iterator, MapIteratorObject.MapIterationKind.KeyValue);
        }

        @Override
        protected Intrinsics getIntrinsic() {
            return Intrinsics.MapIteratorPrototype;
        }

        @Override
        protected Object findNext() throws ScriptException {
            if (!done) {
                if (iteratorObject == null && isBuiltinNext(cx)) {
                    if (iterator.hasNext()) {
                        Entry<Object, Object> e = iterator.next();
                        return CreateArrayFromList(cx, e.getKey(), e.getValue());
                    }
                    done = true;
                    return null;
                }
                return slowNext();
            }
            return null;
        }
    }

    private static final class MapIteratorScriptIterator extends BuiltinScriptIterator {
        private final MapIteratorObject mapIterator;

        MapIteratorScriptIterator(ExecutionContext cx, MapIteratorObject mapIterator) {
            super(cx);
            this.mapIterator = mapIterator;
            this.iteratorObject = mapIterator;
        }

        static boolean isBuiltinIterator(ExecutionContext cx, MapIteratorObject mapIterator) {
            Property iterProp = findIteratorProperty(mapIterator);
            return iterProp != null && isBuiltinIterator(cx, mapIterator, iterProp.getValue());
        }

        static boolean isBuiltinIterator(ExecutionContext cx, MapIteratorObject mapIterator, Object method) {
            // Test 1: Is mapIterator[Symbol.iterator] == %IteratorPrototype%[Symbol.iterator]?
            // Test 2: Is %MapIteratorPrototype%.next the built-in next method?
            return IteratorPrototype.isBuiltinIterator(cx.getRealm(), method) && isBuiltinNext(cx, mapIterator);
        }

        private static boolean isBuiltinNext(ExecutionContext cx, MapIteratorObject mapIterator) {
            Property iterNextProp = findNextProperty(mapIterator);
            return iterNextProp != null && MapIteratorPrototype.isBuiltinNext(cx.getRealm(), iterNextProp.getValue());
        }

        @Override
        protected OrdinaryObject createIteratorObject() {
            throw new AssertionError();
        }

        @Override
        protected Intrinsics getIntrinsic() {
            return Intrinsics.MapIteratorPrototype;
        }

        @Override
        protected Object findNext() throws ScriptException {
            if (!done) {
                if (isBuiltinNext(cx, mapIterator)) {
                    MapIteratorObject mapIter = mapIterator;
                    Iterator<Entry<Object, Object>> iter = mapIter.getIterator();
                    if (iter == null) {
                        done = true;
                        return null;
                    }
                    if (iter.hasNext()) {
                        Entry<Object, Object> e = iter.next();
                        switch (mapIter.getIterationKind()) {
                        case Key:
                            return e.getKey();
                        case Value:
                            return e.getValue();
                        case KeyValue:
                            return CreateArrayFromList(cx, e.getKey(), e.getValue());
                        default:
                            throw new AssertionError();
                        }
                    }
                    mapIter.setIterator(null);
                    done = true;
                    return null;
                }
                return slowNext();
            }
            return null;
        }
    }

    private static final class SetScriptIterator extends BuiltinScriptIterator {
        private final Iterator<Entry<Object, Void>> iterator;

        SetScriptIterator(ExecutionContext cx, SetObject set) {
            super(cx);
            this.iterator = set.getSetData().iterator();
        }

        static boolean isBuiltinIterator(ExecutionContext cx, SetObject set) {
            Property iterProp = findIteratorProperty(set);
            return iterProp != null && isBuiltinIterator(cx, set, iterProp.getValue());
        }

        static boolean isBuiltinIterator(ExecutionContext cx, SetObject set, Object method) {
            // Test 1: Is set[Symbol.iterator] == %SetPrototype%.values?
            // Test 2: Is %SetIteratorPrototype%.next the built-in next method?
            return SetPrototype.isBuiltinValues(cx.getRealm(), method) && isBuiltinNext(cx);
        }

        private static boolean isBuiltinNext(ExecutionContext cx) {
            Property iterNextProp = cx.getIntrinsic(Intrinsics.SetIteratorPrototype).lookupOwnProperty("next");
            return iterNextProp != null && SetIteratorPrototype.isBuiltinNext(cx.getRealm(), iterNextProp.getValue());
        }

        @Override
        protected OrdinaryObject createIteratorObject() {
            return SetIteratorPrototype.CreateSetIterator(cx, iterator, SetIteratorObject.SetIterationKind.Value);
        }

        @Override
        protected Intrinsics getIntrinsic() {
            return Intrinsics.SetIteratorPrototype;
        }

        @Override
        protected Object findNext() throws ScriptException {
            if (!done) {
                if (iteratorObject == null && isBuiltinNext(cx)) {
                    if (iterator.hasNext()) {
                        Entry<Object, Void> e = iterator.next();
                        return e.getKey();
                    }
                    done = true;
                    return null;
                }
                return slowNext();
            }
            return null;
        }
    }

    private static final class SetIteratorScriptIterator extends BuiltinScriptIterator {
        private final SetIteratorObject setIterator;

        SetIteratorScriptIterator(ExecutionContext cx, SetIteratorObject setIterator) {
            super(cx);
            this.setIterator = setIterator;
            this.iteratorObject = setIterator;
        }

        static boolean isBuiltinIterator(ExecutionContext cx, SetIteratorObject setIterator) {
            Property iterProp = findIteratorProperty(setIterator);
            return iterProp != null && isBuiltinIterator(cx, setIterator, iterProp.getValue());
        }

        static boolean isBuiltinIterator(ExecutionContext cx, SetIteratorObject setIterator, Object method) {
            // Test 1: Is setIterator[Symbol.iterator] == %IteratorPrototype%[Symbol.iterator]?
            // Test 2: Is %SetIteratorPrototype%.next the built-in next method?
            return IteratorPrototype.isBuiltinIterator(cx.getRealm(), method) && isBuiltinNext(cx, setIterator);
        }

        private static boolean isBuiltinNext(ExecutionContext cx, SetIteratorObject setIterator) {
            Property iterNextProp = findNextProperty(setIterator);
            return iterNextProp != null && SetIteratorPrototype.isBuiltinNext(cx.getRealm(), iterNextProp.getValue());
        }

        @Override
        protected OrdinaryObject createIteratorObject() {
            throw new AssertionError();
        }

        @Override
        protected Intrinsics getIntrinsic() {
            return Intrinsics.SetIteratorPrototype;
        }

        @Override
        protected Object findNext() throws ScriptException {
            if (!done) {
                if (isBuiltinNext(cx, setIterator)) {
                    SetIteratorObject setIter = setIterator;
                    Iterator<Entry<Object, Void>> iter = setIter.getIterator();
                    if (iter == null) {
                        done = true;
                        return null;
                    }
                    if (iter.hasNext()) {
                        Entry<Object, Void> e = iter.next();
                        switch (setIter.getIterationKind()) {
                        case Key:
                        case Value:
                            return e.getKey();
                        case KeyValue:
                            return CreateArrayFromList(cx, e.getKey(), e.getKey());
                        default:
                            throw new AssertionError();
                        }
                    }
                    setIter.setIterator(null);
                    done = true;
                    return null;
                }
                return slowNext();
            }
            return null;
        }
    }

    private static final class ListIteratorScriptIterator extends BuiltinScriptIterator {
        private final ListIterator<?> listIterator;

        ListIteratorScriptIterator(ExecutionContext cx, ListIterator<?> listIterator) {
            super(cx);
            this.listIterator = listIterator;
            this.iteratorObject = listIterator;
        }

        static boolean isBuiltinIterator(ExecutionContext cx, ListIterator<?> listIterator) {
            Property iterProp = findIteratorProperty(listIterator);
            return iterProp != null && isBuiltinIterator(cx, listIterator, iterProp.getValue());
        }

        static boolean isBuiltinIterator(ExecutionContext cx, ListIterator<?> listIterator, Object method) {
            // Test 1: Is listIterator[Symbol.iterator] == %IteratorPrototype%[Symbol.iterator]?
            // Test 2: Is listIterator.next the built-in next method?
            return IteratorPrototype.isBuiltinIterator(cx.getRealm(), method) && isBuiltinNext(cx, listIterator);
        }

        private static boolean isBuiltinNext(ExecutionContext cx, ListIterator<?> listIterator) {
            Property iterNextProp = listIterator.lookupOwnProperty("next");
            return iterNextProp != null && iterNextProp.getValue() == listIterator.getIteratorNext();
        }

        @Override
        protected OrdinaryObject createIteratorObject() {
            throw new AssertionError();
        }

        @Override
        protected Intrinsics getIntrinsic() {
            return Intrinsics.IteratorPrototype;
        }

        @Override
        protected Object findNext() throws ScriptException {
            if (!done) {
                if (isBuiltinNext(cx, listIterator)) {
                    Iterator<?> iterator = listIterator.getIterator();
                    if (iterator.hasNext()) {
                        return iterator.next();
                    }
                    done = true;
                    return null;
                }
                return slowNext();
            }
            return null;
        }
    }

    private static final class GeneratorScriptIterator extends BuiltinScriptIterator {
        private final GeneratorObject generator;

        GeneratorScriptIterator(ExecutionContext cx, GeneratorObject generator) {
            super(cx);
            this.generator = generator;
            this.iteratorObject = generator;
        }

        static boolean isBuiltinIterator(ExecutionContext cx, GeneratorObject generator) {
            Property iterProp = findIteratorProperty(generator);
            return iterProp != null && isBuiltinIterator(cx, generator, iterProp.getValue());
        }

        static boolean isBuiltinIterator(ExecutionContext cx, GeneratorObject generator, Object method) {
            // Test 1: Is generator[Symbol.iterator] == %IteratorPrototype%[Symbol.iterator]?
            // Test 2: Is %GeneratorPrototype%.next the built-in next method?
            return IteratorPrototype.isBuiltinIterator(cx.getRealm(), method) && isBuiltinNext(cx, generator);
        }

        private static boolean isBuiltinNext(ExecutionContext cx, GeneratorObject generator) {
            Property iterNextProp = findNextProperty(generator);
            return iterNextProp != null && GeneratorPrototype.isBuiltinNext(cx.getRealm(), iterNextProp.getValue());
        }

        @Override
        protected OrdinaryObject createIteratorObject() {
            throw new AssertionError();
        }

        @Override
        protected Intrinsics getIntrinsic() {
            return Intrinsics.GeneratorPrototype;
        }

        @Override
        protected Object findNext() throws ScriptException {
            if (!done) {
                if (isBuiltinNext(cx, generator)) {
                    GeneratorObject gen = generator;
                    if (gen.getState() != GeneratorObject.GeneratorState.Completed) {
                        // TODO: Remove iterator result boxing.
                        try {
                            ScriptObject result = GeneratorResume(cx, gen, UNDEFINED);
                            if (!IteratorComplete(cx, result)) {
                                return IteratorValue(cx, result);
                            }
                        } catch (ScriptException e) {
                            done = true;
                            throw e;
                        }
                    }
                    done = true;
                    return null;
                }
                return slowNext();
            }
            return null;
        }
    }
}

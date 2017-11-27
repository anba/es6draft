/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.IsDetachedBuffer;
import static com.github.anba.es6draft.runtime.objects.iteration.GeneratorAbstractOperations.GeneratorResume;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.mozilla.javascript.ConsString;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.objects.ArrayIteratorObject;
import com.github.anba.es6draft.runtime.objects.ArrayIteratorPrototype;
import com.github.anba.es6draft.runtime.objects.ArrayPrototype;
import com.github.anba.es6draft.runtime.objects.async.iteration.AsyncFromSyncIteratorObject;
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
import com.github.anba.es6draft.runtime.objects.text.StringIteratorObject;
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
     * Returns {@code true} if the object uses the built-in array iterator.
     * 
     * @param cx
     *            the execution context
     * @param object
     *            the object
     * @param length
     *            the length value
     * @return {@code true} if the built-in array iterator is used
     */
    public static boolean isBuiltinArrayIterator(ExecutionContext cx, OrdinaryObject object, long length) {
        return ArrayScriptIterator.isBuiltinIterator(cx, object, length);
    }

    /**
     * Returns {@code true} if the array uses the built-in array iterator.
     * 
     * @param cx
     *            the execution context
     * @param array
     *            the array object
     * @param method
     *            the iterator method
     * @return {@code true} if the built-in array iterator is used
     */
    public static boolean isBuiltinArrayIterator(ExecutionContext cx, ArrayObject array, Callable method) {
        return ArrayScriptIterator.isBuiltinIterator(cx, array, method);
    }

    /**
     * Returns {@code true} if the object uses the built-in typed array iterator.
     * 
     * @param cx
     *            the execution context
     * @param typedArray
     *            the typed array object
     * @return {@code true} if the built-in typed array iterator is used
     */
    public static boolean isBuiltinTypedArrayIterator(ExecutionContext cx, TypedArrayObject typedArray) {
        return TypedArrayScriptIterator.isBuiltinIterator(cx, typedArray);
    }

    /**
     * Returns {@code true} if the object uses the built-in typed array iterator.
     * 
     * @param cx
     *            the execution context
     * @param typedArray
     *            the typed array object
     * @param method
     *            the iterator method
     * @return {@code true} if the built-in typed array iterator is used
     */
    public static boolean isBuiltinTypedArrayIterator(ExecutionContext cx, TypedArrayObject typedArray,
            Callable method) {
        return TypedArrayScriptIterator.isBuiltinIterator(cx, typedArray, method);
    }

    /**
     * Returns {@code true} if the object uses the built-in map iterator.
     * 
     * @param cx
     *            the execution context
     * @param map
     *            the map object
     * @return {@code true} if the built-in map iterator is used
     */
    public static boolean isBuiltinMapIterator(ExecutionContext cx, MapObject map) {
        return MapScriptIterator.isBuiltinIterator(cx, map);
    }

    /**
     * Returns {@code true} if the object uses the built-in set iterator.
     * 
     * @param cx
     *            the execution context
     * @param set
     *            the set object
     * @return {@code true} if the built-in set iterator is used
     */
    public static boolean isBuiltinSetIterator(ExecutionContext cx, SetObject set) {
        return SetScriptIterator.isBuiltinIterator(cx, set);
    }

    /**
     * Returns a {@link ScriptIterator} for {@code iterable}.
     * 
     * @param cx
     *            the execution context
     * @param iterable
     *            the iterable object
     * @return the iterator object or {@code null}
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
        } else if (iterable instanceof GeneratorObject) {
            GeneratorObject generator = (GeneratorObject) iterable;
            if (GeneratorScriptIterator.isBuiltinIterator(cx, generator)) {
                return new GeneratorScriptIterator(cx, generator);
            }
        }
        return null;
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
     * @return the iterator object or {@code null}
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
        return null;
    }

    /**
     * Returns a {@link ScriptIterator} for {@code iterator}. {@code iterator} is expected to comply to the
     * <code>"25.1.2 The Iterator Interface"</code>.
     * 
     * @param cx
     *            the execution context
     * @param iterator
     *            the script iterator object
     * @param nextMethod
     *            the next method
     * @return the iterator object
     */
    public static ScriptIterator<?> ToScriptIterator(ExecutionContext cx, ScriptObject iterator, Object nextMethod) {
        return new ScriptIteratorImpl(cx, iterator, nextMethod);
    }

    /**
     * Returns a {@link ScriptIterator} for {@code iterator}.
     * 
     * @param cx
     *            the execution context
     * @param iterator
     *            the script iterator object
     * @return the iterator object
     */
    public static ScriptIterator<?> GetAsyncScriptIterator(ExecutionContext cx, AsyncFromSyncIteratorObject iterator) {
        return new AsyncFromSyncScriptIterator(cx, iterator);
    }

    private static final class ScriptIteratorImpl extends SimpleIterator<Object> implements ScriptIterator<Object> {
        private final ExecutionContext cx;
        private final ScriptObject iterator;
        private final Object nextMethod;
        private boolean done = false;

        ScriptIteratorImpl(ExecutionContext cx, ScriptObject iterator, Object nextMethod) {
            this.cx = cx;
            this.iterator = iterator;
            this.nextMethod = nextMethod;
        }

        @Override
        public ScriptObject getScriptObject() {
            return iterator;
        }

        @Override
        public Object nextIterResult() throws ScriptException {
            return Call(cx, nextMethod, iterator);
        }

        @Override
        public Object nextIterResult(Object value) throws ScriptException {
            return Call(cx, nextMethod, iterator, value);
        }

        @Override
        protected Object findNext() throws ScriptException {
            if (!done) {
                try {
                    ScriptObject next = IteratorStep(cx, this);
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
                IteratorClose(cx, this);
            }
        }

        @Override
        public void close(Throwable cause) throws ScriptException {
            if (!done) {
                IteratorClose(cx, this, cause);
            }
        }
    }

    private static abstract class BuiltinScriptIterator<ITER extends ScriptObject> extends SimpleIterator<Object>
            implements ScriptIterator<Object> {
        protected final ExecutionContext cx;
        private ITER iteratorObject;
        protected boolean done = false;

        protected BuiltinScriptIterator(ExecutionContext cx) {
            this.cx = cx;
        }

        protected BuiltinScriptIterator(ExecutionContext cx, ITER iteratorObject) {
            this.cx = cx;
            this.iteratorObject = iteratorObject;
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
        protected Intrinsics getIntrinsic() {
            throw new AssertionError();
        }

        /**
         * Creates an iterator object.
         * 
         * @return the new iterator object
         */
        protected ITER createIteratorObject() {
            throw new AssertionError();
        }

        @Override
        public final void close() throws ScriptException {
            if (!done && hasReturn()) {
                IteratorClose(cx, this);
            }
        }

        @Override
        public final void close(Throwable cause) throws ScriptException {
            if (!done && hasReturn()) {
                IteratorClose(cx, this, cause);
            }
        }

        protected ScriptObject nextIterResultValue(Object value) {
            if (hasNext()) {
                Object iterValue = next();
                assert iterValue != null;
                return CreateIterResultObject(cx, iterValue, false);
            }
            return null;
        }

        @Override
        public final ScriptObject nextIterResult() throws ScriptException {
            return nextIterResult(UNDEFINED);
        }

        @Override
        public final ScriptObject nextIterResult(Object value) throws ScriptException {
            if (!done) {
                try {
                    ScriptObject result = nextIterResultValue(value);
                    if (result != null) {
                        return result;
                    }
                } catch (ScriptException e) {
                    done = true;
                    throw e;
                }
                done = true;
            }
            return CreateIterResultObject(cx, UNDEFINED, true);
        }

        protected abstract Object nextValue();

        @Override
        protected final Object findNext() {
            if (!done) {
                try {
                    Object value = nextValue();
                    if (value != null) {
                        return value;
                    }
                } catch (ScriptException e) {
                    done = true;
                    throw e;
                }
                done = true;
            }
            return null;
        }

        protected final boolean hasScriptObject() {
            return iteratorObject != null;
        }

        protected final void forEachRemainingDefault(Consumer<? super Object> action) {
            super.forEachRemaining(action);
        }

        protected void forEachRemainingValue(Consumer<? super Object> action) {
            forEachRemainingDefault(action);
        }

        protected boolean callForEachRemainingValue() {
            return !hasScriptObject();
        }

        @Override
        public final void forEachRemaining(Consumer<? super Object> action) {
            if (!done) {
                if (callForEachRemainingValue()) {
                    try {
                        forEachRemainingValue(action);
                    } catch (ScriptException e) {
                        done = true;
                        throw e;
                    }
                } else {
                    super.forEachRemaining(action);
                }
                done = true;
            }
        }

        @Override
        public final ITER getScriptObject() {
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

    private static final class ArrayScriptIterator extends BuiltinScriptIterator<ArrayIteratorObject> {
        private final ArrayObject array;
        private long index;

        ArrayScriptIterator(ExecutionContext cx, ArrayObject array) {
            super(cx);
            this.array = array;
            this.index = 0;
        }

        static boolean isBuiltinIterator(ExecutionContext cx, ArrayObject array) {
            if (!array.isDenseArray()) {
                return false;
            }
            // Test 1: Is array[Symbol.iterator] == %ArrayPrototype%.values?
            Property iterProp = findIteratorProperty(array);
            if (iterProp == null || !ArrayPrototype.isBuiltinValues(cx.getRealm(), iterProp.getValue())) {
                return false;
            }
            // Test 2: Is %ArrayIteratorPrototype%.next the built-in next method?
            return isBuiltinNext(cx);
        }

        static boolean isBuiltinIterator(ExecutionContext cx, ArrayObject array, Object method) {
            if (!array.isDenseArray()) {
                return false;
            }
            // Test 1: Is array[Symbol.iterator] == %ArrayPrototype%.values?
            // Test 2: Is %ArrayIteratorPrototype%.next the built-in next method?
            return ArrayPrototype.isBuiltinValues(cx.getRealm(), method) && isBuiltinNext(cx);
        }

        static boolean isBuiltinIterator(ExecutionContext cx, OrdinaryObject array, long length) {
            if (!array.isDenseArray(length)) {
                return false;
            }
            // Test 1: Is array[Symbol.iterator] == %ArrayPrototype%.values?
            Property iterProp = findIteratorProperty(array);
            if (iterProp == null || !ArrayPrototype.isBuiltinValues(cx.getRealm(), iterProp.getValue())) {
                return false;
            }
            // Test 2: Is %ArrayIteratorPrototype%.next the built-in next method?
            return isBuiltinNext(cx);
        }

        private static boolean isBuiltinNext(ExecutionContext cx) {
            Property iterNextProp = cx.getIntrinsic(Intrinsics.ArrayIteratorPrototype).lookupOwnProperty("next");
            return iterNextProp != null && ArrayIteratorPrototype.isBuiltinNext(cx.getRealm(), iterNextProp.getValue());
        }

        @Override
        protected ArrayIteratorObject createIteratorObject() {
            return ArrayIteratorPrototype.CreateArrayIterator(cx, array, index,
                    ArrayIteratorObject.ArrayIterationKind.Value);
        }

        @Override
        protected Intrinsics getIntrinsic() {
            return Intrinsics.ArrayIteratorPrototype;
        }

        private Object getNextElement() {
            if (array.isDenseArray()) {
                return array.getDenseElement(index++);
            }
            return Get(cx, array, index++);
        }

        @Override
        protected Object nextValue() {
            if (hasScriptObject()) {
                // Call the ArrayIterator implementation if the iterator object has escaped.
                return ArrayIteratorScriptIterator.next(this);
            }
            if (index < array.getLength()) {
                return getNextElement();
            }
            return null;
        }

        @Override
        protected void forEachRemainingValue(Consumer<? super Object> action) {
            while (index < array.getLength()) {
                action.accept(getNextElement());
            }
        }
    }

    private static final class ArrayIteratorScriptIterator extends BuiltinScriptIterator<ArrayIteratorObject> {
        ArrayIteratorScriptIterator(ExecutionContext cx, ArrayIteratorObject arrayIterator) {
            super(cx, arrayIterator);
        }

        static boolean isBuiltinIterator(ExecutionContext cx, ArrayIteratorObject arrayIterator) {
            Property iterProp = findIteratorProperty(arrayIterator);
            return iterProp != null && isBuiltinIterator(cx, arrayIterator, iterProp.getValue());
        }

        static boolean isBuiltinIterator(ExecutionContext cx, ArrayIteratorObject arrayIterator, Object method) {
            // Test 1: Is arrayIterator[Symbol.iterator] == %IteratorPrototype%[Symbol.iterator]?
            if (IteratorPrototype.isBuiltinIterator(cx.getRealm(), method)) {
                // Test 2: Is %ArrayIteratorPrototype%.next the built-in next method?
                Property iterNext = findNextProperty(arrayIterator);
                return iterNext != null && ArrayIteratorPrototype.isBuiltinNext(cx.getRealm(), iterNext.getValue());
            }
            return false;
        }

        static Object next(BuiltinScriptIterator<ArrayIteratorObject> builtinIter) {
            ArrayIteratorObject iter = builtinIter.getScriptObject();
            ScriptObject array = iter.getIteratedObject();
            if (array == null) {
                return null;
            }
            long index = iter.getNextIndex();
            long len;
            if (array instanceof TypedArrayObject) {
                TypedArrayObject typedArray = (TypedArrayObject) array;
                if (IsDetachedBuffer(typedArray.getBuffer())) {
                    throw newTypeError(builtinIter.cx, Messages.Key.BufferDetached);
                }
                len = typedArray.getArrayLength();
            } else if (array instanceof ArrayObject) {
                len = ((ArrayObject) array).getLength();
            } else {
                len = ToLength(builtinIter.cx, Get(builtinIter.cx, array, "length"));
            }
            if (index >= len) {
                iter.setIteratedObject(null);
                return null;
            }
            iter.setNextIndex(index + 1);
            switch (iter.getIterationKind()) {
            case Key:
                return index;
            case KeyValue:
                return CreateArrayFromList(builtinIter.cx, index, Get(builtinIter.cx, array, index));
            case Value:
                return Get(builtinIter.cx, array, index);
            default:
                throw new AssertionError();
            }
        }

        @Override
        protected Object nextValue() {
            return next(this);
        }
    }

    private static final class TypedArrayScriptIterator extends BuiltinScriptIterator<ArrayIteratorObject> {
        private final TypedArrayObject typedArray;
        private long index;

        TypedArrayScriptIterator(ExecutionContext cx, TypedArrayObject typedArray) {
            super(cx);
            this.typedArray = typedArray;
            this.index = 0;
        }

        static boolean isBuiltinIterator(ExecutionContext cx, TypedArrayObject typedArray) {
            if (typedArray.getBuffer().isDetached()) {
                return false;
            }
            // Test 1: Is typedArray[Symbol.iterator] == %TypedArrayPrototype%.values?
            Property iterProp = findIteratorProperty(typedArray);
            if (iterProp == null || !TypedArrayPrototypePrototype.isBuiltinValues(cx.getRealm(), iterProp.getValue())) {
                return false;
            }
            // Test 2: Is %ArrayIteratorPrototype%.next the built-in next method?
            return isBuiltinNext(cx);
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
        protected ArrayIteratorObject createIteratorObject() {
            return ArrayIteratorPrototype.CreateArrayIterator(cx, typedArray, index,
                    ArrayIteratorObject.ArrayIterationKind.Value);
        }

        @Override
        protected Intrinsics getIntrinsic() {
            return Intrinsics.ArrayIteratorPrototype;
        }

        @Override
        protected Object nextValue() {
            if (hasScriptObject()) {
                // Call the ArrayIterator implementation if the iterator object has escaped.
                return ArrayIteratorScriptIterator.next(this);
            }
            if (typedArray.getBuffer().isDetached()) {
                throw newTypeError(cx, Messages.Key.BufferDetached);
            }
            if (index < typedArray.getArrayLength()) {
                return typedArray.get(cx, index++, typedArray);
            }
            return null;
        }

        @Override
        protected void forEachRemainingValue(Consumer<? super Object> action) {
            if (typedArray.getBuffer().isDetached()) {
                throw newTypeError(cx, Messages.Key.BufferDetached);
            }
            while (index < typedArray.getArrayLength()) {
                action.accept(typedArray.get(cx, index++, typedArray));
            }
        }
    }

    private static final class ArgumentsScriptIterator extends BuiltinScriptIterator<ArrayIteratorObject> {
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
        protected ArrayIteratorObject createIteratorObject() {
            return ArrayIteratorPrototype.CreateArrayIterator(cx, arguments, index,
                    ArrayIteratorObject.ArrayIterationKind.Value);
        }

        @Override
        protected Intrinsics getIntrinsic() {
            return Intrinsics.ArrayIteratorPrototype;
        }

        @Override
        protected Object nextValue() {
            if (hasScriptObject()) {
                // Call the ArrayIterator implementation if the iterator object has escaped.
                return ArrayIteratorScriptIterator.next(this);
            }
            long len = ToLength(cx, Get(cx, arguments, "length"));
            if (index < len) {
                return Get(cx, arguments, index++);
            }
            return null;
        }

        @Override
        protected void forEachRemainingValue(Consumer<? super Object> action) {
            long length = arguments.getLength();
            if (!arguments.isDenseArray(length)) {
                forEachRemainingDefault(action);
            } else {
                while (index < length) {
                    action.accept(arguments.get(cx, index++, arguments));
                }
            }
        }
    }

    private static final class StringScriptIterator extends BuiltinScriptIterator<StringIteratorObject> {
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
        protected StringIteratorObject createIteratorObject() {
            return StringIteratorPrototype.CreateStringIterator(cx, string, index);
        }

        @Override
        protected Intrinsics getIntrinsic() {
            return Intrinsics.StringIteratorPrototype;
        }

        @Override
        protected Object nextValue() {
            if (hasScriptObject()) {
                // Call the StringIterator implementation if the iterator object has escaped.
                return StringIteratorScriptIterator.next(this);
            }
            if (index < string.length()) {
                int cp = string.codePointAt(index);
                index += Character.charCount(cp);
                return Strings.fromCodePoint(cp);
            }
            return null;
        }

        @Override
        protected void forEachRemainingValue(Consumer<? super Object> action) {
            while (index < string.length()) {
                int cp = string.codePointAt(index);
                index += Character.charCount(cp);
                action.accept(Strings.fromCodePoint(cp));
            }
        }
    }

    private static final class StringIteratorScriptIterator extends BuiltinScriptIterator<StringIteratorObject> {
        StringIteratorScriptIterator(ExecutionContext cx, StringIteratorObject stringIterator) {
            super(cx, stringIterator);
        }

        static Object next(BuiltinScriptIterator<StringIteratorObject> builtinIter) {
            StringIteratorObject iterator = builtinIter.getScriptObject();
            String string = iterator.getIteratedString();
            if (string == null) {
                return null;
            }
            int position = iterator.getNextIndex();
            int len = string.length();
            if (position >= len) {
                iterator.setIteratedString(null);
                return null;
            }
            int cp = string.codePointAt(position);
            String resultString = Strings.fromCodePoint(cp);
            int resultSize = Character.charCount(cp);
            iterator.setNextIndex(position + resultSize);
            return resultString;
        }

        @Override
        protected Object nextValue() {
            return next(this);
        }
    }

    private static final class MapScriptIterator extends BuiltinScriptIterator<MapIteratorObject> {
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
        protected MapIteratorObject createIteratorObject() {
            return MapIteratorPrototype.CreateMapIterator(cx, iterator, MapIteratorObject.MapIterationKind.KeyValue);
        }

        @Override
        protected Intrinsics getIntrinsic() {
            return Intrinsics.MapIteratorPrototype;
        }

        @Override
        protected Object nextValue() {
            if (hasScriptObject()) {
                // Call the MapIterator implementation if the iterator object has escaped.
                return MapIteratorScriptIterator.next(this);
            }
            if (iterator.hasNext()) {
                Entry<Object, Object> e = iterator.next();
                return CreateArrayFromList(cx, e.getKey(), e.getValue());
            }
            return null;
        }

        @Override
        protected void forEachRemainingValue(Consumer<? super Object> action) {
            while (iterator.hasNext()) {
                Entry<Object, Object> e = iterator.next();
                action.accept(CreateArrayFromList(cx, e.getKey(), e.getValue()));
            }
        }
    }

    private static final class MapIteratorScriptIterator extends BuiltinScriptIterator<MapIteratorObject> {
        MapIteratorScriptIterator(ExecutionContext cx, MapIteratorObject mapIterator) {
            super(cx, mapIterator);
        }

        static boolean isBuiltinIterator(ExecutionContext cx, MapIteratorObject mapIterator) {
            Property iterProp = findIteratorProperty(mapIterator);
            return iterProp != null && isBuiltinIterator(cx, mapIterator, iterProp.getValue());
        }

        static boolean isBuiltinIterator(ExecutionContext cx, MapIteratorObject mapIterator, Object method) {
            // Test 1: Is mapIterator[Symbol.iterator] == %IteratorPrototype%[Symbol.iterator]?
            if (IteratorPrototype.isBuiltinIterator(cx.getRealm(), method)) {
                // Test 2: Is %MapIteratorPrototype%.next the built-in next method?
                Property iterNext = findNextProperty(mapIterator);
                return iterNext != null && MapIteratorPrototype.isBuiltinNext(cx.getRealm(), iterNext.getValue());
            }
            return false;
        }

        static Object next(BuiltinScriptIterator<MapIteratorObject> builtinIter) {
            MapIteratorObject mapIter = builtinIter.getScriptObject();
            Iterator<Entry<Object, Object>> iter = mapIter.getIterator();
            if (iter == null) {
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
                    return CreateArrayFromList(builtinIter.cx, e.getKey(), e.getValue());
                default:
                    throw new AssertionError();
                }
            }
            mapIter.setIterator(null);
            return null;
        }

        @Override
        protected Object nextValue() {
            return next(this);
        }

        @Override
        protected boolean callForEachRemainingValue() {
            return true;
        }

        @Override
        protected void forEachRemainingValue(Consumer<? super Object> action) {
            MapIteratorObject mapIter = getScriptObject();
            Iterator<Entry<Object, Object>> iter = mapIter.getIterator();
            if (iter == null) {
                return;
            }
            while (iter.hasNext()) {
                Entry<Object, Object> e = iter.next();
                switch (mapIter.getIterationKind()) {
                case Key:
                    action.accept(e.getKey());
                    break;
                case Value:
                    action.accept(e.getValue());
                    break;
                case KeyValue:
                    action.accept(CreateArrayFromList(cx, e.getKey(), e.getValue()));
                    break;
                default:
                    throw new AssertionError();
                }
            }
            mapIter.setIterator(null);
        }
    }

    private static final class SetScriptIterator extends BuiltinScriptIterator<SetIteratorObject> {
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
        protected SetIteratorObject createIteratorObject() {
            return SetIteratorPrototype.CreateSetIterator(cx, iterator, SetIteratorObject.SetIterationKind.Value);
        }

        @Override
        protected Intrinsics getIntrinsic() {
            return Intrinsics.SetIteratorPrototype;
        }

        @Override
        protected Object nextValue() {
            if (hasScriptObject()) {
                // Call the SetIterator implementation if the iterator object has escaped.
                return SetIteratorScriptIterator.next(this);
            }
            if (iterator.hasNext()) {
                Entry<Object, Void> e = iterator.next();
                return e.getKey();
            }
            return null;
        }

        @Override
        protected void forEachRemainingValue(Consumer<? super Object> action) {
            while (iterator.hasNext()) {
                Entry<Object, Void> e = iterator.next();
                action.accept(e.getKey());
            }
        }
    }

    private static final class SetIteratorScriptIterator extends BuiltinScriptIterator<SetIteratorObject> {
        SetIteratorScriptIterator(ExecutionContext cx, SetIteratorObject setIterator) {
            super(cx, setIterator);
        }

        static boolean isBuiltinIterator(ExecutionContext cx, SetIteratorObject setIterator) {
            Property iterProp = findIteratorProperty(setIterator);
            return iterProp != null && isBuiltinIterator(cx, setIterator, iterProp.getValue());
        }

        static boolean isBuiltinIterator(ExecutionContext cx, SetIteratorObject setIterator, Object method) {
            // Test 1: Is setIterator[Symbol.iterator] == %IteratorPrototype%[Symbol.iterator]?
            if (IteratorPrototype.isBuiltinIterator(cx.getRealm(), method)) {
                // Test 2: Is %SetIteratorPrototype%.next the built-in next method?
                Property iterNext = findNextProperty(setIterator);
                return iterNext != null && SetIteratorPrototype.isBuiltinNext(cx.getRealm(), iterNext.getValue());
            }
            return false;
        }

        static Object next(BuiltinScriptIterator<SetIteratorObject> builtinIter) {
            SetIteratorObject setIter = builtinIter.getScriptObject();
            Iterator<Entry<Object, Void>> iter = setIter.getIterator();
            if (iter == null) {
                return null;
            }
            if (iter.hasNext()) {
                Entry<Object, Void> e = iter.next();
                switch (setIter.getIterationKind()) {
                case Key:
                case Value:
                    return e.getKey();
                case KeyValue:
                    return CreateArrayFromList(builtinIter.cx, e.getKey(), e.getKey());
                default:
                    throw new AssertionError();
                }
            }
            setIter.setIterator(null);
            return null;
        }

        @Override
        protected Object nextValue() {
            return next(this);
        }

        @Override
        protected boolean callForEachRemainingValue() {
            return true;
        }

        @Override
        protected void forEachRemainingValue(Consumer<? super Object> action) {
            SetIteratorObject setIter = getScriptObject();
            Iterator<Entry<Object, Void>> iter = setIter.getIterator();
            if (iter == null) {
                return;
            }
            while (iter.hasNext()) {
                Entry<Object, Void> e = iter.next();
                switch (setIter.getIterationKind()) {
                case Key:
                case Value:
                    action.accept(e.getKey());
                    break;
                case KeyValue:
                    action.accept(CreateArrayFromList(cx, e.getKey(), e.getKey()));
                    break;
                default:
                    throw new AssertionError();
                }
            }
            setIter.setIterator(null);
        }
    }

    private static final class GeneratorScriptIterator extends BuiltinScriptIterator<GeneratorObject> {
        GeneratorScriptIterator(ExecutionContext cx, GeneratorObject generator) {
            super(cx, generator);
        }

        static boolean isBuiltinIterator(ExecutionContext cx, GeneratorObject generator) {
            Property iterProp = findIteratorProperty(generator);
            return iterProp != null && isBuiltinIterator(cx, generator, iterProp.getValue());
        }

        static boolean isBuiltinIterator(ExecutionContext cx, GeneratorObject generator, Object method) {
            // Test 1: Is generator[Symbol.iterator] == %IteratorPrototype%[Symbol.iterator]?
            if (IteratorPrototype.isBuiltinIterator(cx.getRealm(), method)) {
                // Test 2: Is %GeneratorPrototype%.next the built-in next method?
                Property iterNext = findNextProperty(generator);
                return iterNext != null && GeneratorPrototype.isBuiltinNext(cx.getRealm(), iterNext.getValue());
            }
            return false;
        }

        @Override
        public ScriptObject nextIterResultValue(Object value) {
            GeneratorObject gen = getScriptObject();
            if (gen.getState() != GeneratorObject.GeneratorState.Completed) {
                return GeneratorResume(cx, gen, value, "Generator.prototype.next");
            }
            return null;
        }

        @Override
        protected Object nextValue() {
            GeneratorObject gen = getScriptObject();
            if (gen.getState() != GeneratorObject.GeneratorState.Completed) {
                // TODO: Remove iterator result boxing.
                ScriptObject result = GeneratorResume(cx, gen, UNDEFINED, "Generator.prototype.next");
                if (!IteratorComplete(cx, result)) {
                    return IteratorValue(cx, result);
                }
            }
            return null;
        }
    }

    private static final class AsyncFromSyncScriptIterator extends BuiltinScriptIterator<AsyncFromSyncIteratorObject> {
        AsyncFromSyncScriptIterator(ExecutionContext cx, AsyncFromSyncIteratorObject iteratorObject) {
            super(cx, iteratorObject);
        }

        @Override
        public ScriptObject nextIterResultValue(Object value) {
            return getScriptObject().next(cx, value);
        }

        @Override
        protected Object nextValue() {
            // Async-from-sync iterators aren't used for normal iteration.
            throw new AssertionError();
        }
    }
}

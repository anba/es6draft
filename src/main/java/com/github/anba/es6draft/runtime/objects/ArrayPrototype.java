/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.ArrayIteratorPrototype.CreateArrayIterator;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.AliasFunction;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Optional;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.StrBuilder;
import com.github.anba.es6draft.runtime.objects.ArrayIteratorObject.ArrayIterationKind;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.NativeFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>22 Indexed Collections</h1><br>
 * <h2>22.1 Array Objects</h2>
 * <ul>
 * <li>22.1.3 Properties of the Array Prototype Object
 * <li>22.1.4 Properties of Array Instances
 * </ul>
 */
public final class ArrayPrototype extends ArrayObject implements Initializable {
    /**
     * Constructs a new Array prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public ArrayPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
        createProperties(realm, this, FlatMapFlattenProperties.class);
    }

    /**
     * Marker class for {@code Array.prototype.values}.
     */
    private static final class ArrayPrototypeValues {
    }

    /**
     * Returns {@code true} if <var>values</var> is the built-in {@code Array.prototype.values} function for the
     * requested realm.
     * 
     * @param realm
     *            the function realm
     * @param values
     *            the values function
     * @return {@code true} if <var>values</var> is the built-in {@code Array.prototype.values} function
     */
    public static boolean isBuiltinValues(Realm realm, Object values) {
        return NativeFunction.isNative(realm, values, ArrayPrototypeValues.class);
    }

    private static final long ARRAY_LENGTH_LIMIT = 0x1F_FFFF_FFFF_FFFFL;
    private static final boolean NO_ARRAY_OPTIMIZATION = false;
    private static final int MIN_SPARSE_LENGTH = 100; // Arbitrarily chosen limit
    private static final int MAX_PROTO_DEPTH = 10; // Arbitrarily chosen limit

    private enum IterationKind {
        DenseOwnKeys, SparseOwnKeys, SparseKeys, Slow;

        boolean isSparse() {
            return this == SparseOwnKeys || this == SparseKeys;
        }

        Keyiterator forward(OrdinaryObject array, long from, long to) {
            // assert 0 <= from && from <= to;
            switch (this) {
            case SparseKeys:
                return new ForwardSparseIter(from, to, arrayKeysWithProto(array, from, to));
            case SparseOwnKeys:
                return new ForwardSparseIter(from, to, arrayOwnKeys(array, from, to));
            case Slow:
            case DenseOwnKeys:
            default:
                throw new AssertionError();
            }
        }

        Keyiterator reverse(OrdinaryObject array, long from, long to) {
            // assert 0 <= from && from <= to;
            switch (this) {
            case SparseKeys:
                return new ReverseSparseIter(from, to, arrayKeysWithProto(array, from, to));
            case SparseOwnKeys:
                return new ReverseSparseIter(from, to, arrayOwnKeys(array, from, to));
            case Slow:
            case DenseOwnKeys:
            default:
                throw new AssertionError();
            }
        }
    }

    private static IterationKind iterationKind(ScriptObject object, long length) {
        return iterationKind(object, object, length);
    }

    private static IterationKind iterationKind(ScriptObject target, ScriptObject source, long length) {
        if (NO_ARRAY_OPTIMIZATION) {
            return IterationKind.Slow;
        }
        if (length < MIN_SPARSE_LENGTH) {
            return IterationKind.Slow;
        }
        if (!(target instanceof OrdinaryObject) || ((OrdinaryObject) target).hasSpecialIndexedProperties()) {
            return IterationKind.Slow;
        }
        if (source instanceof ArrayObject) {
            return iterationKind((ArrayObject) source, length);
        }
        if (source instanceof TypedArrayObject) {
            return iterationKind((TypedArrayObject) source);
        }
        if (source instanceof OrdinaryObject) {
            return iterationKind((OrdinaryObject) source, length);
        }
        return IterationKind.Slow;
    }

    private static IterationKind iterationKind(ArrayObject array, long length) {
        if (array.isDenseArray()) {
            return IterationKind.DenseOwnKeys;
        }
        if (array.hasIndexedAccessors()) {
            return IterationKind.Slow;
        }
        return iterationKindForSparse(array, length);
    }

    private static IterationKind iterationKind(OrdinaryObject arrayLike, long length) {
        if (arrayLike.hasSpecialIndexedProperties()) {
            return IterationKind.Slow;
        }
        if (arrayLike.isDenseArray(length)) {
            return IterationKind.DenseOwnKeys;
        }
        if (arrayLike.hasIndexedAccessors()) {
            return IterationKind.Slow;
        }
        return iterationKindForSparse(arrayLike, length);
    }

    private static IterationKind iterationKind(TypedArrayObject typedArray) {
        if (typedArray.getBuffer().isDetached()) {
            return IterationKind.Slow;
        }
        return IterationKind.DenseOwnKeys;
    }

    private static IterationKind iterationKindForSparse(OrdinaryObject arrayLike, long length) {
        IterationKind iteration = IterationKind.SparseOwnKeys;
        int protoDepth = 0;
        long indexed = 0;
        for (OrdinaryObject object = arrayLike;;) {
            indexed += object.getIndexedSize();
            ScriptObject prototype = object.getPrototype();
            if (prototype == null) {
                break;
            }
            if (!(prototype instanceof OrdinaryObject)) {
                return IterationKind.Slow;
            }
            object = (OrdinaryObject) prototype;
            if (object.hasSpecialIndexedProperties()) {
                return IterationKind.Slow;
            }
            if (object.hasIndexedProperties()) {
                if (object.hasIndexedAccessors()) {
                    return IterationKind.Slow;
                }
                iteration = IterationKind.SparseKeys;
            }
            if (++protoDepth == MAX_PROTO_DEPTH) {
                return IterationKind.Slow;
            }
        }
        double density = indexed / (double) length;
        if (density > 0.75) {
            return IterationKind.Slow;
        }
        return iteration;
    }

    private static long[] arrayOwnKeys(OrdinaryObject array, long from, long to) {
        return array.indices(from, to);
    }

    private static long[] arrayKeysWithProto(OrdinaryObject array, long from, long to) {
        long[] indices = array.indices(from, to);
        for (ScriptObject prototype = array.getPrototype(); prototype != null;) {
            assert prototype instanceof OrdinaryObject : "Wrong class " + prototype.getClass();
            OrdinaryObject proto = (OrdinaryObject) prototype;
            if (proto.hasIndexedProperties()) {
                long[] protoIndices = proto.indices(from, to);
                long[] newIndices = new long[indices.length + protoIndices.length];
                System.arraycopy(indices, 0, newIndices, 0, indices.length);
                System.arraycopy(protoIndices, 0, newIndices, indices.length, protoIndices.length);
                indices = newIndices;
            }
            prototype = proto.getPrototype();
        }
        Arrays.sort(indices);
        return indices;
    }

    private static abstract class Keyiterator {
        protected final long from;
        protected final long to;
        protected final long[] keys;
        protected final int length;
        protected int index;
        protected long lastKey;

        Keyiterator(long from, long to, long[] keys, int index, long lastKey) {
            this.from = from;
            this.to = to;
            this.keys = keys;
            this.length = keys.length;
            this.index = index;
            this.lastKey = lastKey;
        }

        final int size() {
            return keys.length;
        }

        final long peek() {
            assert 0 <= index && index < length;
            return keys[index];
        }

        abstract boolean hasNext();

        abstract long next();

        abstract boolean containsNext(long needle);
    }

    private static final class ForwardSparseIter extends Keyiterator {
        ForwardSparseIter(long from, long to, long[] keys) {
            super(from, to, keys, 0, from - 1);
        }

        @Override
        boolean hasNext() {
            for (; index < length; ++index) {
                long key = keys[index];
                if (key != lastKey) {
                    assert lastKey < key && (from <= key && key < to);
                    return true;
                }
            }
            return false;
        }

        @Override
        long next() {
            assert index < length;
            return lastKey = keys[index++];
        }

        @Override
        boolean containsNext(long needle) {
            for (int i = index; i < length; ++i) {
                if (keys[i] == needle) {
                    return true;
                }
                if (keys[i] > needle) {
                    break;
                }
            }
            return false;
        }
    }

    private static final class ReverseSparseIter extends Keyiterator {
        ReverseSparseIter(long from, long to, long[] keys) {
            super(from, to, keys, keys.length - 1, to);
        }

        @Override
        boolean hasNext() {
            for (; index >= 0; --index) {
                long key = keys[index];
                if (key != lastKey) {
                    assert key < lastKey && (from <= key && key < to);
                    return true;
                }
            }
            return false;
        }

        @Override
        long next() {
            assert index >= 0;
            return lastKey = keys[index--];
        }

        @Override
        boolean containsNext(long needle) {
            for (int i = index; i >= 0; --i) {
                if (keys[i] == needle) {
                    return true;
                }
                if (keys[i] < needle) {
                    break;
                }
            }
            return false;
        }
    }

    /**
     * 22.1.3 Properties of the Array Prototype Object
     */
    public enum Properties {
        ;

        private static long ToArrayIndex(ExecutionContext cx, Object index, long length) {
            long relativeIndex = (long) ToNumber(cx, index); // ToInteger
            if (relativeIndex < 0) {
                return Math.max(length + relativeIndex, 0);
            }
            return Math.min(relativeIndex, length);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 22.1.3.2 Array.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Array;

        /**
         * 22.1.3.28 Array.prototype.toString ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the string representation
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            ScriptObject array = ToObject(cx, thisValue);
            /* step 2 */
            Object func = Get(cx, array, "join");
            /* step 3 */
            if (!IsCallable(func)) {
                func = cx.getIntrinsic(Intrinsics.ObjProto_toString);
            }
            /* step 4 */
            return ((Callable) func).call(cx, array);
        }

        /**
         * 22.1.3.27 Array.prototype.toLocaleString ( [ reserved1 [ , reserved2 ] ] )<br>
         * 13.4.1 Array.prototype.toLocaleString([locales [, options ]])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param locales
         *            the optional locales array
         * @param options
         *            the optional options object
         * @return the locale specific string representation
         */
        @Function(name = "toLocaleString", arity = 0)
        public static Object toLocaleString(ExecutionContext cx, Object thisValue, Object locales, Object options) {
            /* step 1 */
            ScriptObject array = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, array, "length"));
            /* step 3 */
            // FIXME: spec issue - retrieve list separator from locale?
            String separator = cx.getRealm().getListSeparator();
            /* step 4 */
            if (len == 0) {
                return "";
            }
            /* step 5 */
            Object firstElement = Get(cx, array, 0);
            /* step 6 */
            StrBuilder r = new StrBuilder(cx);
            if (!Type.isUndefinedOrNull(firstElement)) {
                r.append(ToString(cx, Invoke(cx, firstElement, "toLocaleString", locales, options)));
            }
            /* steps 7-8 */
            for (long k = 1; k < len; ++k) {
                r.append(separator);
                Object nextElement = Get(cx, array, k);
                if (!Type.isUndefinedOrNull(nextElement)) {
                    r.append(ToString(cx, Invoke(cx, nextElement, "toLocaleString", locales, options)));
                }
            }
            /* step 9 */
            return r.toString();
        }

        /**
         * 22.1.3.1 Array.prototype.concat ( ...arguments )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param items
         *            the new array elements
         * @return the concatenated array object
         */
        @Function(name = "concat", arity = 1)
        public static Object concat(ExecutionContext cx, Object thisValue, Object... items) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            ScriptObject a = ArraySpeciesCreate(cx, o, 0);
            /* step 3 */
            long n = 0;
            /* step 4 */
            Object[] allItems = new Object[items.length + 1];
            allItems[0] = o;
            System.arraycopy(items, 0, allItems, 1, items.length);
            /* step 5 */
            for (Object item : allItems) {
                /* step 5.a (not applicable) */
                /* step 5.b */
                boolean spreadable = IsConcatSpreadable(cx, item);
                if (spreadable) {
                    ScriptObject e = (ScriptObject) item;
                    /* step 5.c.ii */
                    long len = ToLength(cx, Get(cx, e, "length"));
                    /* step 5.c.iii */
                    if (n + len > ARRAY_LENGTH_LIMIT) {
                        throw newTypeError(cx, Messages.Key.InvalidArrayLength);
                    }
                    /* steps 5.c.i, 5.c.iv */
                    IterationKind iteration = iterationKind(a, e, len);
                    if (iteration.isSparse()) {
                        for (Keyiterator keys = iteration.forward((OrdinaryObject) e, 0, len); keys.hasNext();) {
                            long k = keys.next();
                            Object subElement = Get(cx, e, k);
                            CreateDataPropertyOrThrow(cx, a, n + k, subElement);
                        }
                        n += len;
                    } else {
                        for (long k = 0; k < len; ++k, ++n) {
                            long p = k;
                            boolean exists = HasProperty(cx, e, p);
                            if (exists) {
                                Object subElement = Get(cx, e, p);
                                CreateDataPropertyOrThrow(cx, a, n, subElement);
                            }
                        }
                    }
                } else {
                    /* step 5.d */
                    if (n >= ARRAY_LENGTH_LIMIT) {
                        throw newTypeError(cx, Messages.Key.InvalidArrayLength);
                    }
                    CreateDataPropertyOrThrow(cx, a, n++, item);
                }
            }
            /* step 6 */
            assert n <= ARRAY_LENGTH_LIMIT;
            Set(cx, a, "length", n, true);
            /* step 7 */
            return a;
        }

        /**
         * 22.1.3.1.1 Runtime Semantics: IsConcatSpreadable ( O )
         * 
         * @param cx
         *            the execution context
         * @param o
         *            the object to test
         * @return {@code true} if the object is spreadable
         */
        public static boolean IsConcatSpreadable(ExecutionContext cx, Object o) {
            /* step 1 */
            if (!Type.isObject(o)) {
                return false;
            }
            ScriptObject object = Type.objectValue(o);
            /* step 2 */
            Object spreadable = Get(cx, object, BuiltinSymbol.isConcatSpreadable.get());
            /* step 3 */
            if (!Type.isUndefined(spreadable)) {
                return ToBoolean(spreadable);
            }
            /* step 4 */
            return IsArray(cx, object);
        }

        /**
         * 22.1.3.13 Array.prototype.join (separator)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param separator
         *            the separator string
         * @return the result string
         */
        @Function(name = "join", arity = 1)
        public static Object join(ExecutionContext cx, Object thisValue, Object separator) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* steps 3-4 */
            String sep = Type.isUndefined(separator) ? "," : ToFlatString(cx, separator);
            /* step 5 */
            if (len == 0) {
                return "";
            }
            /* step 6 */
            Object element0 = Get(cx, o, 0);
            /* step 7 */
            StrBuilder r = new StrBuilder(cx);
            if (!Type.isUndefinedOrNull(element0)) {
                r.append(ToString(cx, element0));
            }
            /* steps 8-9 */
            IterationKind iteration = iterationKind(o, len);
            if (iteration.isSparse()) {
                joinSparse(cx, (OrdinaryObject) o, len, sep, r, iteration);
            } else {
                for (long k = 1; k < len; ++k) {
                    /* step 9.a */
                    r.append(sep);
                    /* step 9.b */
                    Object element = Get(cx, o, k);
                    /* steps 9.c-d */
                    if (!Type.isUndefinedOrNull(element)) {
                        r.append(ToString(cx, element));
                    }
                }
            }
            /* step 10 */
            return r.toString();
        }

        private static void joinSparse(ExecutionContext cx, OrdinaryObject o, long length, String sep, StrBuilder r,
                IterationKind iteration) {
            final boolean hasSeparator = !sep.isEmpty();
            if (hasSeparator) {
                long estimated = length * sep.length();
                if (estimated >= Integer.MAX_VALUE || length > Long.MAX_VALUE / sep.length()) {
                    throw newInternalError(cx, Messages.Key.OutOfMemory);
                }
            }
            long lastKey = 0;
            objectElement: {
                for (Keyiterator iter = iteration.forward(o, 1, length); iter.hasNext();) {
                    long k = iter.next();
                    // Add leading separator and fill holes if separator is not the empty string.
                    if (hasSeparator) {
                        for (long start = lastKey; start < k; ++start) {
                            r.append(sep);
                        }
                    }
                    lastKey = k;

                    Object element = Get(cx, o, k);
                    if (!Type.isUndefinedOrNull(element)) {
                        r.append(ToString(cx, element));
                        // Side-effects may have invalidated array keys.
                        if (Type.isObject(element)) {
                            break objectElement;
                        }
                    }
                }
                // Fill trailing holes if separator is not the empty string.
                if (hasSeparator) {
                    for (long start = lastKey + 1; start < length; ++start) {
                        r.append(sep);
                    }
                }
                return;
            }
            // Trailing elements after object type element.
            for (long k = lastKey + 1; k < length; ++k) {
                r.append(sep);
                Object element = Get(cx, o, k);
                if (!Type.isUndefinedOrNull(element)) {
                    r.append(ToString(cx, element));
                }
            }
        }

        /**
         * 22.1.3.17 Array.prototype.pop ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the popped array element
         */
        @Function(name = "pop", arity = 0)
        public static Object pop(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* steps 3-4 */
            if (len == 0) {
                /* step 3 */
                /* step 3.a */
                Set(cx, o, "length", 0, true);
                /* step 3.b */
                return UNDEFINED;
            } else {
                /* step 4 */
                assert len > 0;
                /* step 4.a */
                long newLen = len - 1;
                /* step 4.b */
                long index = newLen;
                /* step 4.c */
                Object element = Get(cx, o, index);
                /* step 4.d */
                DeletePropertyOrThrow(cx, o, index);
                /* step 4.e */
                Set(cx, o, "length", newLen, true);
                /* step 4.f */
                return element;
            }
        }

        /**
         * 22.1.3.18 Array.prototype.push ( ...items )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param items
         *            the new array elements
         * @return the new array length
         */
        @Function(name = "push", arity = 1)
        public static Object push(ExecutionContext cx, Object thisValue, Object... items) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* step 3 (not applicable) */
            /* steps 4-5 */
            if (len + items.length > ARRAY_LENGTH_LIMIT) {
                throw newTypeError(cx, Messages.Key.InvalidArrayLength);
            }
            /* step 6 */
            for (Object e : items) {
                /* step 6.a (not applicable) */
                /* step 6.b */
                Set(cx, o, len, e, true);
                /* step 6.c */
                len += 1;
            }
            /* step 7 */
            assert len <= ARRAY_LENGTH_LIMIT;
            Set(cx, o, "length", len, true);
            /* step 8 */
            return len;
        }

        /**
         * 22.1.3.21 Array.prototype.reverse ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return this array object
         */
        @Function(name = "reverse", arity = 0)
        public static Object reverse(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* steps 3-5 */
            IterationKind iteration = iterationKind(o, len);
            if (iteration.isSparse()) {
                reverseSparse(cx, (OrdinaryObject) o, len, iteration);
            } else {
                /* step 3 */
                long middle = len / 2L;
                /* steps 4-5 */
                for (long lower = 0; lower != middle; ++lower) {
                    /* step 5.a */
                    long upper = len - lower - 1;
                    /* step 5.b */
                    long upperP = upper;
                    /* step 5.c */
                    long lowerP = lower;
                    /* step 5.d */
                    boolean lowerExists = HasProperty(cx, o, lowerP);
                    /* step 5.e */
                    Object lowerValue = lowerExists ? Get(cx, o, lowerP) : null;
                    /* step 5.f */
                    boolean upperExists = HasProperty(cx, o, upperP);
                    /* step 5.g */
                    Object upperValue = upperExists ? Get(cx, o, upperP) : null;
                    /* steps 5.h-k */
                    if (lowerExists && upperExists) {
                        Set(cx, o, lowerP, upperValue, true);
                        Set(cx, o, upperP, lowerValue, true);
                    } else if (!lowerExists && upperExists) {
                        Set(cx, o, lowerP, upperValue, true);
                        DeletePropertyOrThrow(cx, o, upperP);
                    } else if (lowerExists && !upperExists) {
                        DeletePropertyOrThrow(cx, o, lowerP);
                        Set(cx, o, upperP, lowerValue, true);
                    } else {
                        // no action required
                    }
                }
            }
            /* step 6 */
            return o;
        }

        private static void reverseSparse(ExecutionContext cx, OrdinaryObject o, long length, IterationKind iteration) {
            long middle = length / 2L;
            Keyiterator lowerIter = iteration.forward(o, 0, middle);
            Keyiterator upperIter = iteration.reverse(o, length - middle, length);

            while (lowerIter.hasNext() && upperIter.hasNext()) {
                long lower = lowerIter.peek();
                long upper = (length - 1) - upperIter.peek();

                if (lower == upper) {
                    long lowerP = lowerIter.next();
                    long upperP = upperIter.next();

                    Object lowerValue = Get(cx, o, lowerP);
                    Object upperValue = Get(cx, o, upperP);
                    Set(cx, o, lowerP, upperValue, true);
                    Set(cx, o, upperP, lowerValue, true);
                } else if (lower < upper) {
                    long lowerP = lowerIter.next();
                    long upperP = (length - 1) - lower;

                    Object lowerValue = Get(cx, o, lowerP);
                    DeletePropertyOrThrow(cx, o, lowerP);
                    Set(cx, o, upperP, lowerValue, true);
                } else {
                    long upperP = upperIter.next();
                    long lowerP = upper;

                    Object upperValue = Get(cx, o, upperP);
                    Set(cx, o, lowerP, upperValue, true);
                    DeletePropertyOrThrow(cx, o, upperP);
                }
            }

            while (lowerIter.hasNext()) {
                long lowerP = lowerIter.next();
                long upperP = (length - 1) - lowerP;

                Object lowerValue = Get(cx, o, lowerP);
                DeletePropertyOrThrow(cx, o, lowerP);
                Set(cx, o, upperP, lowerValue, true);
            }

            while (upperIter.hasNext()) {
                long upperP = upperIter.next();
                long lowerP = (length - 1) - upperP;

                Object upperValue = Get(cx, o, upperP);
                Set(cx, o, lowerP, upperValue, true);
                DeletePropertyOrThrow(cx, o, upperP);
            }
        }

        /**
         * 22.1.3.22 Array.prototype.shift ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the shifted array element
         */
        @Function(name = "shift", arity = 0)
        public static Object shift(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* step 3 */
            if (len == 0) {
                Set(cx, o, "length", 0, true);
                return UNDEFINED;
            }
            assert len > 0;
            /* step 4 */
            Object first = Get(cx, o, 0);
            /* steps 5-6 */
            IterationKind iteration = iterationKind(o, len);
            if (iteration.isSparse()) {
                shiftSparse(cx, (OrdinaryObject) o, len, iteration);
            } else {
                for (long k = 1; k < len; ++k) {
                    /* step 6.a */
                    long from = k;
                    /* step 6.b */
                    long to = k - 1;
                    /* step 6.c */
                    boolean fromPresent = HasProperty(cx, o, from);
                    /* steps 6.d-e */
                    if (fromPresent) {
                        Object fromVal = Get(cx, o, from);
                        Set(cx, o, to, fromVal, true);
                    } else {
                        DeletePropertyOrThrow(cx, o, to);
                    }
                }
            }
            /* step 7 */
            DeletePropertyOrThrow(cx, o, len - 1);
            /* step 8 */
            Set(cx, o, "length", len - 1, true);
            /* step 9 */
            return first;
        }

        private static void shiftSparse(ExecutionContext cx, OrdinaryObject o, long length, IterationKind iteration) {
            Keyiterator iter = iteration.forward(o, 1, length);
            if (iter.hasNext() && iter.peek() != 1) {
                DeletePropertyOrThrow(cx, o, 0);
            }
            while (iter.hasNext()) {
                long k = iter.next();

                long from = k;
                long to = k - 1;
                Object fromVal = Get(cx, o, from);
                Set(cx, o, to, fromVal, true);

                long replacement = k + 1;
                if (replacement < length && !iter.containsNext(replacement)) {
                    DeletePropertyOrThrow(cx, o, from);
                }
            }
        }

        /**
         * 22.1.3.23 Array.prototype.slice (start, end)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param start
         *            the start position
         * @param end
         *            the end position
         * @return the new array object
         */
        @Function(name = "slice", arity = 2)
        public static Object slice(ExecutionContext cx, Object thisValue, Object start, Object end) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* steps 3-4 */
            long k = ToArrayIndex(cx, start, len);
            /* steps 5-6 */
            long finall = Type.isUndefined(end) ? len : ToArrayIndex(cx, end, len);
            /* step 7 */
            long count = Math.max(finall - k, 0);
            /* step 8 */
            ScriptObject a = ArraySpeciesCreate(cx, o, count);
            /* steps 9-10 */
            long n;
            IterationKind iteration = iterationKind(a, o, len);
            if (iteration.isSparse()) {
                sliceSparse(cx, (OrdinaryObject) a, k, finall, (OrdinaryObject) o, iteration);
                n = count;
            } else {
                n = 0;
                for (; k < finall; ++k, ++n) {
                    long pk = k;
                    boolean kpresent = HasProperty(cx, o, pk);
                    if (kpresent) {
                        Object kvalue = Get(cx, o, pk);
                        CreateDataPropertyOrThrow(cx, a, n, kvalue);
                    }
                }
            }
            /* step 11 */
            Set(cx, a, "length", n, true);
            /* step 12 */
            return a;
        }

        private static void sliceSparse(ExecutionContext cx, OrdinaryObject a, long k, long finall, OrdinaryObject o,
                IterationKind iteration) {
            for (Keyiterator iter = iteration.forward(o, k, finall); iter.hasNext();) {
                long pk = iter.next();

                long n = pk - k;
                Object kvalue = Get(cx, o, pk);
                CreateDataPropertyOrThrow(cx, a, n, kvalue);
            }
        }

        /**
         * 22.1.3.24.1 Runtime Semantics: SortCompare( x, y )
         */
        private static final class DefaultComparator implements Comparator<Object> {
            private final ExecutionContext cx;

            DefaultComparator(ExecutionContext cx) {
                this.cx = cx;
            }

            @Override
            public int compare(Object o1, Object o2) {
                /* steps 1-4 (not applicable) */
                /* step 5 */
                String x = ToFlatString(cx, o1);
                /* step 6 */
                String y = ToFlatString(cx, o2);
                /* steps 7-11 */
                return x.compareTo(y);
            }
        }

        /**
         * 22.1.3.24.1 Runtime Semantics: SortCompare( x, y )
         */
        private static final class FunctionComparator implements Comparator<Object> {
            private final ExecutionContext cx;
            private final Callable comparefn;

            FunctionComparator(ExecutionContext cx, Callable comparefn) {
                this.cx = cx;
                this.comparefn = comparefn;
            }

            @Override
            public int compare(Object o1, Object o2) {
                /* steps 1-3, 5-11 (not applicable) */
                /* step 4 */
                double c = ToNumber(cx, comparefn.call(cx, UNDEFINED, o1, o2));
                return (c < 0 ? -1 : c > 0 ? 1 : 0);
            }
        }

        private static void sortElements(ExecutionContext cx, ArrayList<Object> elements, Callable comparefn) {
            Comparator<Object> comparator;
            if (comparefn != null) {
                comparator = new FunctionComparator(cx, (Callable) comparefn);
            } else {
                comparator = new DefaultComparator(cx);
            }
            try {
                Collections.sort(elements, comparator);
            } catch (IllegalArgumentException e) {
                // User-defined comparator functions may return inconsistent comparison results,
                // and those will trigger this exception in the Collections.sort() method:
                // `IllegalArgumentException: Comparison method violates its general contract!`
                // If that happens, just ignore the Java exception and stop the sort operation.
            }
        }

        /**
         * 22.1.3.25 Array.prototype.sort (comparefn)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param comparefn
         *            the comparator function
         * @return this array object
         */
        @Function(name = "sort", arity = 1)
        public static Object sort(ExecutionContext cx, Object thisValue, Object comparefn) {
            /* step 1 */
            Callable compareFunction = null;
            if (!Type.isUndefined(comparefn)) {
                if (!IsCallable(comparefn)) {
                    throw newTypeError(cx, Messages.Key.NotCallable);
                }
                compareFunction = (Callable) comparefn;
            }
            /* step 2 */
            ScriptObject obj = ToObject(cx, thisValue);
            /* step 3 */
            long len = ToLength(cx, Get(cx, obj, "length"));

            // return if array is empty or has only one element
            if (len <= 1) {
                return obj;
            }
            IterationKind iteration = iterationKind(obj, len);
            if (iteration.isSparse()) {
                sortSparse(cx, (OrdinaryObject) obj, len, compareFunction, iteration);
            } else {
                // handle OOM early
                if (len > Integer.MAX_VALUE) {
                    throw newInternalError(cx, Messages.Key.OutOfMemory);
                }

                // collect elements
                int length = (int) len;
                int emptyCount = 0;
                int undefCount = 0;
                ArrayList<Object> elements = new ArrayList<>(Math.min(length, 1024));
                for (int i = 0; i < length; ++i) {
                    long index = i;
                    if (HasProperty(cx, obj, index)) {
                        Object e = Get(cx, obj, index);
                        if (!Type.isUndefined(e)) {
                            elements.add(e);
                        } else {
                            undefCount += 1;
                        }
                    } else {
                        emptyCount += 1;
                    }
                }

                // sort elements
                int count = elements.size();
                if (count > 1) {
                    sortElements(cx, elements, compareFunction);
                }

                // and finally set sorted elements
                for (int i = 0, offset = 0; i < count; ++i) {
                    int p = offset + i;
                    Set(cx, obj, p, elements.get(i), true);
                }
                for (int i = 0, offset = count; i < undefCount; ++i) {
                    int p = offset + i;
                    Set(cx, obj, p, UNDEFINED, true);
                }
                for (int i = 0, offset = count + undefCount; i < emptyCount; ++i) {
                    int p = offset + i;
                    DeletePropertyOrThrow(cx, obj, p);
                }
            }
            return obj;
        }

        private static void sortSparse(ExecutionContext cx, OrdinaryObject obj, long length, Callable comparefn,
                IterationKind iteration) {
            // collect elements
            Keyiterator collectIter = iteration.forward(obj, 0, length);
            int undefCount = 0;
            ArrayList<Object> elements = new ArrayList<>(Math.min(collectIter.size(), 1024));
            while (collectIter.hasNext()) {
                long index = collectIter.next();
                Object e = Get(cx, obj, index);
                if (!Type.isUndefined(e)) {
                    elements.add(e);
                } else {
                    undefCount += 1;
                }
            }

            // sort elements
            int count = elements.size();
            if (count > 1) {
                sortElements(cx, elements, comparefn);
            }

            // and finally set sorted elements
            for (int i = 0, offset = 0; i < count; ++i) {
                int p = offset + i;
                Set(cx, obj, p, elements.get(i), true);
            }
            for (int i = 0, offset = count; i < undefCount; ++i) {
                int p = offset + i;
                Set(cx, obj, p, UNDEFINED, true);
            }
            // User-defined actions in comparefn may have invalidated sparse-array property
            IterationKind iterationDelete = iterationKind(obj, length);
            if (iterationDelete.isSparse()) {
                Keyiterator deleteIter = iterationDelete.forward(obj, count + undefCount, length);
                while (deleteIter.hasNext()) {
                    long p = deleteIter.next();
                    DeletePropertyOrThrow(cx, obj, p);
                }
            } else {
                for (long i = count + undefCount; i < length; ++i) {
                    long p = i;
                    DeletePropertyOrThrow(cx, obj, p);
                }
            }
        }

        /**
         * 22.1.3.26 Array.prototype.splice (start, deleteCount, ...items )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param start
         *            the start index
         * @param deleteCount
         *            the delete count
         * @param items
         *            the new array elements
         * @return the deleted array elements
         */
        @Function(name = "splice", arity = 2)
        public static Object splice(ExecutionContext cx, Object thisValue,
                @Optional(Optional.Default.NONE) Object start, @Optional(Optional.Default.NONE) Object deleteCount,
                Object... items) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* steps 3-4 */
            long actualStart = start != null ? ToArrayIndex(cx, start, len) : 0;
            /* steps 5-7 */
            int insertCount;
            long actualDeleteCount;
            if (start == null) {
                insertCount = 0;
                actualDeleteCount = 0;
            } else if (deleteCount == null) {
                insertCount = 0;
                actualDeleteCount = len - actualStart;
            } else {
                insertCount = items.length;
                long dc = (long) ToNumber(cx, deleteCount); // ToInteger
                actualDeleteCount = Math.min(Math.max(dc, 0), len - actualStart);
            }
            /* step 8 */
            if (len + insertCount - actualDeleteCount > ARRAY_LENGTH_LIMIT) {
                throw newTypeError(cx, Messages.Key.InvalidArrayLength);
            }
            /* step 9 */
            ScriptObject a = ArraySpeciesCreate(cx, o, actualDeleteCount);
            /* steps 10-11 */
            IterationKind iterationCopy = iterationKind(a, o, actualDeleteCount);
            if (iterationCopy.isSparse()) {
                spliceSparseCopy(cx, (OrdinaryObject) a, (OrdinaryObject) o, actualStart, actualDeleteCount,
                        iterationCopy);
            } else {
                for (long k = 0; k < actualDeleteCount; ++k) {
                    /* step 11.a */
                    long from = actualStart + k;
                    /* step 11.b */
                    boolean fromPresent = HasProperty(cx, o, from);
                    /* step 11.c */
                    if (fromPresent) {
                        Object fromValue = Get(cx, o, from);
                        CreateDataPropertyOrThrow(cx, a, k, fromValue);
                    }
                }
            }
            /* step 12 */
            Set(cx, a, "length", actualDeleteCount, true);
            /* step 13 (not applicable) */
            /* step 14 */
            int itemCount = items.length;
            /* steps 15-16 */
            if (itemCount < actualDeleteCount) {
                /* steps 15.a-b */
                IterationKind iterationMove = iterationKind(o, len);
                if (iterationMove.isSparse()) {
                    spliceSparseMoveLeft(cx, (OrdinaryObject) o, len, actualStart, actualDeleteCount, itemCount,
                            iterationMove);
                } else {
                    for (long k = actualStart; k < (len - actualDeleteCount); ++k) {
                        long from = k + actualDeleteCount;
                        long to = k + itemCount;
                        boolean fromPresent = HasProperty(cx, o, from);
                        if (fromPresent) {
                            Object fromValue = Get(cx, o, from);
                            Set(cx, o, to, fromValue, true);
                        } else {
                            DeletePropertyOrThrow(cx, o, to);
                        }
                    }
                }
                /* steps 15.c-d */
                IterationKind iterationDelete = iterationKind(o, len);
                if (iterationDelete.isSparse()) {
                    spliceSparseDelete(cx, (OrdinaryObject) o, len, actualDeleteCount, itemCount, iterationDelete);
                } else {
                    for (long k = len; k > (len - actualDeleteCount + itemCount); --k) {
                        DeletePropertyOrThrow(cx, o, k - 1);
                    }
                }
            } else if (itemCount > actualDeleteCount) {
                /* steps 16.a-b */
                IterationKind iterationMove = iterationKind(o, len);
                if (iterationMove.isSparse()) {
                    spliceSparseMoveRight(cx, (OrdinaryObject) o, len, actualStart, actualDeleteCount, itemCount,
                            iterationMove);
                } else {
                    for (long k = (len - actualDeleteCount); k > actualStart; --k) {
                        long from = k + actualDeleteCount - 1;
                        long to = k + itemCount - 1;
                        boolean fromPresent = HasProperty(cx, o, from);
                        if (fromPresent) {
                            Object fromValue = Get(cx, o, from);
                            Set(cx, o, to, fromValue, true);
                        } else {
                            DeletePropertyOrThrow(cx, o, to);
                        }
                    }
                }
            }
            /* step 17 */
            long k = actualStart;
            /* step 18 */
            for (int i = 0; i < itemCount; ++k, ++i) {
                Object e = items[i];
                Set(cx, o, k, e, true);
            }
            /* step 19 */
            assert len - actualDeleteCount + itemCount <= ARRAY_LENGTH_LIMIT;
            Set(cx, o, "length", len - actualDeleteCount + itemCount, true);
            /* step 20 */
            return a;
        }

        private static void spliceSparseMoveRight(ExecutionContext cx, OrdinaryObject o, long length, long start,
                long deleteCount, int itemCount, IterationKind iteration) {
            assert 0 <= deleteCount && deleteCount <= (length - start) : "actualDeleteCount=" + deleteCount;
            assert start >= 0 && (itemCount > 0 && itemCount > deleteCount);

            long targetRangeStart = start + itemCount;
            long targetRangeEnd = length;
            Keyiterator iterTarget = iteration.reverse(o, targetRangeStart, targetRangeEnd);

            long sourceRangeStart = start + deleteCount;
            long sourceRangeEnd = length;
            Keyiterator iterSource = iteration.reverse(o, sourceRangeStart, sourceRangeEnd);

            while (iterTarget.hasNext() && iterSource.hasNext()) {
                long toRel = iterTarget.peek() - itemCount;
                long fromRel = iterSource.peek() - deleteCount;
                if (toRel == fromRel) {
                    long from = iterSource.next();
                    long to = iterTarget.next();
                    Object fromValue = Get(cx, o, from);
                    Set(cx, o, to, fromValue, true);
                } else if (toRel < fromRel) {
                    long from = iterSource.next();
                    long actualTo = from - deleteCount + itemCount;
                    Object fromValue = Get(cx, o, from);
                    Set(cx, o, actualTo, fromValue, true);
                } else {
                    long to = iterTarget.next();
                    DeletePropertyOrThrow(cx, o, to);
                }
            }

            while (iterTarget.hasNext()) {
                long to = iterTarget.next();
                DeletePropertyOrThrow(cx, o, to);
            }

            while (iterSource.hasNext()) {
                long from = iterSource.next();
                long actualTo = from - deleteCount + itemCount;
                Object fromValue = Get(cx, o, from);
                Set(cx, o, actualTo, fromValue, true);
            }
        }

        private static void spliceSparseMoveLeft(ExecutionContext cx, OrdinaryObject o, long length, long start,
                long deleteCount, int itemCount, IterationKind iteration) {
            assert 0 < deleteCount && deleteCount <= (length - start) : "actualDeleteCount=" + deleteCount;
            assert (itemCount < deleteCount);
            assert start >= 0 && itemCount >= 0;
            long moveAmount = (length - start) - deleteCount;
            assert moveAmount >= 0;

            long targetRangeStart = start + itemCount;
            long targetRangeEnd = targetRangeStart + moveAmount;
            assert targetRangeEnd < length;
            Keyiterator iterTarget = iteration.forward(o, targetRangeStart, targetRangeEnd);

            long sourceRangeStart = start + deleteCount;
            long sourceRangeEnd = sourceRangeStart + moveAmount;
            assert sourceRangeEnd == length;
            Keyiterator iterSource = iteration.forward(o, sourceRangeStart, sourceRangeEnd);

            while (iterTarget.hasNext() && iterSource.hasNext()) {
                long toRel = iterTarget.peek() - itemCount;
                long fromRel = iterSource.peek() - deleteCount;
                if (toRel == fromRel) {
                    long to = iterTarget.next();
                    long from = iterSource.next();
                    Object fromValue = Get(cx, o, from);
                    Set(cx, o, to, fromValue, true);
                } else if (toRel < fromRel) {
                    long to = iterTarget.next();
                    DeletePropertyOrThrow(cx, o, to);
                } else {
                    long from = iterSource.next();
                    long actualTo = from - deleteCount + itemCount;
                    Object fromValue = Get(cx, o, from);
                    Set(cx, o, actualTo, fromValue, true);
                }
            }

            while (iterTarget.hasNext()) {
                long to = iterTarget.next();
                DeletePropertyOrThrow(cx, o, to);
            }

            while (iterSource.hasNext()) {
                long from = iterSource.next();
                long actualTo = from - deleteCount + itemCount;
                Object fromValue = Get(cx, o, from);
                Set(cx, o, actualTo, fromValue, true);
            }
        }

        private static void spliceSparseCopy(ExecutionContext cx, OrdinaryObject a, OrdinaryObject o, long start,
                long deleteCount, IterationKind iteration) {
            long copyEnd = start + deleteCount;
            for (Keyiterator iter = iteration.forward(o, start, copyEnd); iter.hasNext();) {
                long from = iter.next();
                Object fromValue = Get(cx, o, from);
                CreateDataPropertyOrThrow(cx, a, from - start, fromValue);
            }
        }

        private static void spliceSparseDelete(ExecutionContext cx, OrdinaryObject o, long length, long deleteCount,
                long itemCount, IterationKind iteration) {
            long delStart = length - deleteCount + itemCount;
            for (Keyiterator iter = iteration.reverse(o, delStart, length); iter.hasNext();) {
                long k = iter.next();
                DeletePropertyOrThrow(cx, o, k);
            }
        }

        /**
         * 22.1.3.29 Array.prototype.unshift ( ...items )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param items
         *            the new array elements
         * @return the new array length
         */
        @Function(name = "unshift", arity = 1)
        public static Object unshift(ExecutionContext cx, Object thisValue, Object... items) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* step 3 */
            int argCount = items.length;
            /* step 4 */
            if (argCount > 0) {
                /* step 4.a */
                if (len + argCount > ARRAY_LENGTH_LIMIT) {
                    throw newTypeError(cx, Messages.Key.InvalidArrayLength);
                }
                /* steps 4.b-c */
                IterationKind iteration = iterationKind(o, len);
                if (iteration.isSparse()) {
                    unshiftSparse(cx, (OrdinaryObject) o, len, argCount, iteration);
                } else {
                    for (long k = len; k > 0; --k) {
                        long from = k - 1;
                        long to = k + argCount - 1;
                        boolean fromPresent = HasProperty(cx, o, from);
                        if (fromPresent) {
                            Object fromValue = Get(cx, o, from);
                            Set(cx, o, to, fromValue, true);
                        } else {
                            DeletePropertyOrThrow(cx, o, to);
                        }
                    }
                }
                /* steps 4.d-f */
                for (int j = 0; j < items.length; ++j) {
                    Object e = items[j];
                    Set(cx, o, j, e, true);
                }
            }
            /* step 5 */
            assert len + argCount <= ARRAY_LENGTH_LIMIT;
            Set(cx, o, "length", len + argCount, true);
            /* step 6 */
            return len + argCount;
        }

        private static void unshiftSparse(ExecutionContext cx, OrdinaryObject o, long length, int argCount,
                IterationKind iteration) {
            Keyiterator iter = iteration.reverse(o, 0, length);
            int deleteFirst = 0, deleteLast = 0;
            // TODO: alloc Math.min(iter.size, argCount) + ring buffer?
            long[] keysToDelete = new long[iter.size()];
            while (iter.hasNext()) {
                long k = iter.next();
                while (deleteLast > deleteFirst && keysToDelete[deleteFirst] > k) {
                    DeletePropertyOrThrow(cx, o, keysToDelete[deleteFirst++] + argCount);
                }

                long from = k;
                long to = k + argCount;
                Object fromValue = Get(cx, o, from);
                Set(cx, o, to, fromValue, true);

                long replacement = k - argCount;
                if (replacement >= 0 && !iter.containsNext(replacement)) {
                    keysToDelete[deleteLast++] = replacement;
                }
            }
            while (deleteLast > deleteFirst) {
                DeletePropertyOrThrow(cx, o, keysToDelete[deleteFirst++] + argCount);
            }
        }

        /**
         * 22.1.3.12 Array.prototype.indexOf ( searchElement [ , fromIndex ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param searchElement
         *            the search element
         * @param fromIndex
         *            the optional start index
         * @return the result index
         */
        @Function(name = "indexOf", arity = 1)
        public static Object indexOf(ExecutionContext cx, Object thisValue, Object searchElement, Object fromIndex) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* step 3 */
            if (len == 0) {
                return -1;
            }
            /* step 4 */
            long n = (long) ToNumber(cx, fromIndex); // ToInteger
            /* step 5 */
            if (n >= len) {
                return -1;
            }
            /* steps 6-7 */
            long k;
            if (n >= 0) {
                k = n;
            } else {
                k = len + n;
                if (k < 0) {
                    k = 0;
                }
            }
            /* step 8 */
            for (; k < len; ++k) {
                /* step 8.a */
                boolean kpresent = HasProperty(cx, o, k);
                /* step 8.b */
                if (kpresent) {
                    Object elementk = Get(cx, o, k);
                    boolean same = StrictEqualityComparison(searchElement, elementk);
                    if (same) {
                        return k;
                    }
                }
            }
            /* step 9 */
            return -1;
        }

        /**
         * 22.1.3.15 Array.prototype.lastIndexOf ( searchElement [ , fromIndex ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param searchElement
         *            the search element
         * @param fromIndex
         *            the optional start index
         * @return the result index
         */
        @Function(name = "lastIndexOf", arity = 1)
        public static Object lastIndexOf(ExecutionContext cx, Object thisValue, Object searchElement,
                @Optional(Optional.Default.NONE) Object fromIndex) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* step 3 */
            if (len == 0) {
                return -1;
            }
            /* step 4 */
            long n;
            if (fromIndex != null) {
                n = (long) ToNumber(cx, fromIndex); // ToInteger
            } else {
                n = len - 1;
            }
            /* steps 5-6 */
            long k;
            if (n >= 0) {
                k = Math.min(n, len - 1);
            } else {
                k = len + n;
            }
            /* step 7 */
            for (; k >= 0; --k) {
                /* step 7.a */
                boolean kpresent = HasProperty(cx, o, k);
                /* step 7.b */
                if (kpresent) {
                    Object elementk = Get(cx, o, k);
                    boolean same = StrictEqualityComparison(searchElement, elementk);
                    if (same) {
                        return k;
                    }
                }
            }
            /* step 8 */
            return -1;
        }

        /**
         * 22.1.3.5 Array.prototype.every ( callbackfn [ , thisArg] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param callbackfn
         *            the callback function
         * @param thisArg
         *            the optional this-argument for the callback function
         * @return {@code true} if every element matches
         */
        @Function(name = "every", arity = 1)
        public static Object every(ExecutionContext cx, Object thisValue, Object callbackfn, Object thisArg) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* step 3 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 4 (omitted) */
            /* steps 5-6 */
            for (long k = 0; k < len; ++k) {
                /* step 6.a */
                long pk = k;
                /* step 6.b */
                boolean kpresent = HasProperty(cx, o, pk);
                /* step 6.c */
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    boolean testResult = ToBoolean(callback.call(cx, thisArg, kvalue, k, o));
                    if (!testResult) {
                        return false;
                    }
                }
            }
            /* step 7 */
            return true;
        }

        /**
         * 22.1.3.24 Array.prototype.some ( callbackfn [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param callbackfn
         *            the callback function
         * @param thisArg
         *            the optional this-argument for the callback function
         * @return {@code true} if some elements match
         */
        @Function(name = "some", arity = 1)
        public static Object some(ExecutionContext cx, Object thisValue, Object callbackfn, Object thisArg) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* step 3 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 4 (omitted) */
            /* steps 5-6 */
            for (long k = 0; k < len; ++k) {
                /* step 6.a */
                long pk = k;
                /* step 6.b */
                boolean kpresent = HasProperty(cx, o, pk);
                /* step 6.c */
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    boolean testResult = ToBoolean(callback.call(cx, thisArg, kvalue, k, o));
                    if (testResult) {
                        return true;
                    }
                }
            }
            /* step 7 */
            return false;
        }

        /**
         * 22.1.3.10 Array.prototype.forEach ( callbackfn [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param callbackfn
         *            the callback function
         * @param thisArg
         *            the optional this-argument for the callback function
         * @return the undefined value
         */
        @Function(name = "forEach", arity = 1)
        public static Object forEach(ExecutionContext cx, Object thisValue, Object callbackfn, Object thisArg) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* step 3 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 4 (omitted) */
            /* steps 5-6 */
            for (long k = 0; k < len; ++k) {
                /* step 6.a */
                long pk = k;
                /* step 6.b */
                boolean kpresent = HasProperty(cx, o, pk);
                /* step 6.c */
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    callback.call(cx, thisArg, kvalue, k, o);
                }
            }
            /* step 7 */
            return UNDEFINED;
        }

        /**
         * 22.1.3.16 Array.prototype.map ( callbackfn [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param callbackfn
         *            the callback function
         * @param thisArg
         *            the optional this-argument for the callback function
         * @return the mapped value
         */
        @Function(name = "map", arity = 1)
        public static Object map(ExecutionContext cx, Object thisValue, Object callbackfn, Object thisArg) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* step 3 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 4 (omitted) */
            /* step 5 */
            ScriptObject a = ArraySpeciesCreate(cx, o, len);
            /* steps 6-7 */
            for (long k = 0; k < len; ++k) {
                /* step 7.a */
                long pk = k;
                /* step 7.b */
                boolean kpresent = HasProperty(cx, o, pk);
                /* step 7.c */
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    Object mappedValue = callback.call(cx, thisArg, kvalue, k, o);
                    CreateDataPropertyOrThrow(cx, a, pk, mappedValue);
                }
            }
            /* step 8 */
            return a;
        }

        /**
         * 22.1.3.7 Array.prototype.filter ( callbackfn [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param callbackfn
         *            the callback function
         * @param thisArg
         *            the optional this-argument for the callback function
         * @return the filtered value
         */
        @Function(name = "filter", arity = 1)
        public static Object filter(ExecutionContext cx, Object thisValue, Object callbackfn, Object thisArg) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* step 3 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 4 (omitted) */
            /* step 5 */
            ScriptObject a = ArraySpeciesCreate(cx, o, 0);
            /* steps 6-7 */
            for (long k = 0, to = 0; k < len; ++k) {
                /* step 7.a */
                long pk = k;
                /* step 7.b */
                boolean kpresent = HasProperty(cx, o, pk);
                /* step 7.c */
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    boolean selected = ToBoolean(callback.call(cx, thisArg, kvalue, k, o));
                    if (selected) {
                        CreateDataPropertyOrThrow(cx, a, to, kvalue);
                        to += 1;
                    }
                }
            }
            /* step 8 */
            return a;
        }

        /**
         * 22.1.3.19 Array.prototype.reduce ( callbackfn [ , initialValue ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param callbackfn
         *            the callback function
         * @param initialValue
         *            the initial value
         * @return the reduced value
         */
        @Function(name = "reduce", arity = 1)
        public static Object reduce(ExecutionContext cx, Object thisValue, Object callbackfn,
                @Optional(Optional.Default.NONE) Object initialValue) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* step 3 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 4 */
            if (len == 0 && initialValue == null) {
                throw newTypeError(cx, Messages.Key.ReduceInitialValue);
            }
            /* step 5 */
            long k = 0;
            /* steps 6-7 */
            Object accumulator = null;
            if (initialValue != null) {
                /* step 6.a */
                accumulator = initialValue;
            } else {
                /* step 7.a */
                boolean kpresent = false;
                /* step 7.b */
                for (; !kpresent && k < len; ++k) {
                    long pk = k;
                    kpresent = HasProperty(cx, o, pk);
                    if (kpresent) {
                        accumulator = Get(cx, o, pk);
                    }
                }
                /* step 7.c */
                if (!kpresent) {
                    throw newTypeError(cx, Messages.Key.ReduceInitialValue);
                }
            }
            /* step 8 */
            for (; k < len; ++k) {
                /* step 8.a */
                long pk = k;
                /* step 8.b */
                boolean kpresent = HasProperty(cx, o, pk);
                /* step 8.c */
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    accumulator = callback.call(cx, UNDEFINED, accumulator, kvalue, k, o);
                }
            }
            /* step 9 */
            return accumulator;
        }

        /**
         * 22.1.3.20 Array.prototype.reduceRight ( callbackfn [ , initialValue ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param callbackfn
         *            the callback function
         * @param initialValue
         *            the initial value
         * @return the reduced value
         */
        @Function(name = "reduceRight", arity = 1)
        public static Object reduceRight(ExecutionContext cx, Object thisValue, Object callbackfn,
                @Optional(Optional.Default.NONE) Object initialValue) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* step 3 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 4 */
            if (len == 0 && initialValue == null) {
                throw newTypeError(cx, Messages.Key.ReduceInitialValue);
            }
            /* step 5 */
            long k = len - 1;
            /* steps 6-7 */
            Object accumulator = null;
            if (initialValue != null) {
                /* step 6.a */
                accumulator = initialValue;
            } else {
                /* step 7.a */
                boolean kpresent = false;
                /* step 7.b */
                for (; !kpresent && k >= 0; --k) {
                    long pk = k;
                    kpresent = HasProperty(cx, o, pk);
                    if (kpresent) {
                        accumulator = Get(cx, o, pk);
                    }
                }
                /* step 7.c */
                if (!kpresent) {
                    throw newTypeError(cx, Messages.Key.ReduceInitialValue);
                }
            }
            /* step 8 */
            for (; k >= 0; --k) {
                /* step 8.a */
                long pk = k;
                /* step 8.b */
                boolean kpresent = HasProperty(cx, o, pk);
                /* step 8.c */
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    accumulator = callback.call(cx, UNDEFINED, accumulator, kvalue, k, o);
                }
            }
            /* step 9 */
            return accumulator;
        }

        /**
         * 22.1.3.8 Array.prototype.find ( predicate [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param predicate
         *            the predicate function
         * @param thisArg
         *            the optional this-argument for the predicate function
         * @return the result value
         */
        @Function(name = "find", arity = 1)
        public static Object find(ExecutionContext cx, Object thisValue, Object predicate, Object thisArg) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* step 3 */
            if (!IsCallable(predicate)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable pred = (Callable) predicate;
            /* step 4 (omitted) */
            /* steps 5-6 */
            for (long k = 0; k < len; ++k) {
                /* step 6.a */
                long pk = k;
                /* step 6.b */
                Object kvalue = Get(cx, o, pk);
                /* step 6.c */
                boolean testResult = ToBoolean(pred.call(cx, thisArg, kvalue, k, o));
                /* step 6.d */
                if (testResult) {
                    return kvalue;
                }
            }
            /* step 7 */
            return UNDEFINED;
        }

        /**
         * 22.1.3.9 Array.prototype.findIndex ( predicate [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param predicate
         *            the predicate function
         * @param thisArg
         *            the optional this-argument for the predicate function
         * @return the result index
         */
        @Function(name = "findIndex", arity = 1)
        public static Object findIndex(ExecutionContext cx, Object thisValue, Object predicate, Object thisArg) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* step 3 */
            if (!IsCallable(predicate)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable pred = (Callable) predicate;
            /* step 4 (omitted) */
            /* steps 5-6 */
            for (long k = 0; k < len; ++k) {
                /* step 6.a */
                long pk = k;
                /* step 6.b */
                Object kvalue = Get(cx, o, pk);
                /* step 6.c */
                boolean testResult = ToBoolean(pred.call(cx, thisArg, kvalue, k, o));
                /* step 6.d */
                if (testResult) {
                    return k;
                }
            }
            /* step 7 */
            return -1;
        }

        /**
         * 22.1.3.4 Array.prototype.entries ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the entries iterator
         */
        @Function(name = "entries", arity = 0)
        public static Object entries(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            return CreateArrayIterator(cx, o, ArrayIterationKind.KeyValue);
        }

        /**
         * 22.1.3.14 Array.prototype.keys ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the keys iterator
         */
        @Function(name = "keys", arity = 0)
        public static Object keys(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            return CreateArrayIterator(cx, o, ArrayIterationKind.Key);
        }

        /**
         * 22.1.3.30 Array.prototype.values ( )<br>
         * 22.1.3.31 Array.prototype [ @@iterator ] ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the values iterator
         */
        @Function(name = "values", arity = 0, nativeId = ArrayPrototypeValues.class)
        @AliasFunction(name = "[Symbol.iterator]", symbol = BuiltinSymbol.iterator)
        public static Object values(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            return CreateArrayIterator(cx, o, ArrayIterationKind.Value);
        }

        /**
         * 22.1.3.32 Array.prototype [ @@unscopables ]
         * 
         * @param cx
         *            the execution context
         * @return the unscopables object
         */
        @Value(name = "[Symbol.unscopables]", symbol = BuiltinSymbol.unscopables,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object unscopables(ExecutionContext cx) {
            /* step 1 */
            OrdinaryObject blackList = ObjectCreate(cx, (ScriptObject) null);
            /* steps 2-8 */
            boolean status = true;
            status &= CreateDataProperty(cx, blackList, "copyWithin", true);
            status &= CreateDataProperty(cx, blackList, "entries", true);
            status &= CreateDataProperty(cx, blackList, "fill", true);
            status &= CreateDataProperty(cx, blackList, "find", true);
            status &= CreateDataProperty(cx, blackList, "findIndex", true);
            status &= CreateDataProperty(cx, blackList, "includes", true);
            status &= CreateDataProperty(cx, blackList, "keys", true);
            status &= CreateDataProperty(cx, blackList, "values", true);
            /* step 10 */
            assert status;
            /* step 11 */
            return blackList;
        }

        /**
         * 22.1.3.6 Array.prototype.fill (value [ , start [ , end ] ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the fill value
         * @param start
         *            the start index
         * @param end
         *            the end index
         * @return this typed array object
         */
        @Function(name = "fill", arity = 1)
        public static Object fill(ExecutionContext cx, Object thisValue, Object value, Object start, Object end) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* steps 3-4 */
            long k = ToArrayIndex(cx, start, len);
            /* steps 5-6 */
            long finall = Type.isUndefined(end) ? len : ToArrayIndex(cx, end, len);
            /* step 7 */
            for (; k < finall; ++k) {
                /* step 7.a */
                long pk = k;
                /* step 7.b */
                Set(cx, o, pk, value, true);
            }
            /* step 8 */
            return o;
        }

        /**
         * 22.1.3.3 Array.prototype.copyWithin (target, start [ , end ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param target
         *            the target index
         * @param start
         *            the start index
         * @param end
         *            the end index
         * @return this typed array object
         */
        @Function(name = "copyWithin", arity = 2)
        public static Object copyWithin(ExecutionContext cx, Object thisValue, Object target, Object start,
                Object end) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* steps 3-4 */
            long to = ToArrayIndex(cx, target, len);
            /* steps 5-6 */
            long from = ToArrayIndex(cx, start, len);
            /* steps 7-8 */
            long finall = Type.isUndefined(end) ? len : ToArrayIndex(cx, end, len);
            /* step 9 */
            long count = Math.min(finall - from, len - to);
            /* steps 10-11 */
            long direction;
            if (from < to && to < from + count) {
                direction = -1;
                from = from + count - 1;
                to = to + count - 1;
            } else {
                direction = 1;
            }
            /* step 12 */
            for (; count > 0; --count) {
                /* steps 12.a-b */
                long fromKey = from;
                long toKey = to;
                /* step 12.c */
                boolean fromPresent = HasProperty(cx, o, fromKey);
                /* steps 12.d-e */
                if (fromPresent) {
                    Object fromVal = Get(cx, o, fromKey);
                    Set(cx, o, toKey, fromVal, true);
                } else {
                    DeletePropertyOrThrow(cx, o, toKey);
                }
                /* steps 12.f-g */
                from += direction;
                to += direction;
            }
            /* step 13 */
            return o;
        }

        /**
         * 22.1.3.11 Array.prototype.includes ( searchElement [ , fromIndex ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param searchElement
         *            the search element
         * @param fromIndex
         *            the optional start index
         * @return the result index
         */
        @Function(name = "includes", arity = 1)
        public static Object includes(ExecutionContext cx, Object thisValue, Object searchElement, Object fromIndex) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* step 3 */
            if (len == 0) {
                return false;
            }
            /* step 4 */
            long n = (long) ToNumber(cx, fromIndex); // ToInteger
            /* steps 5-6 */
            long k;
            if (n >= 0) {
                k = n;
            } else {
                k = len + n;
                if (k < 0) {
                    k = 0;
                }
            }
            /* step 7 */
            for (; k < len; ++k) {
                /* step 7.a */
                Object element = Get(cx, o, k);
                /* step 10.b */
                if (SameValueZero(searchElement, element)) {
                    return true;
                }
            }
            /* step 8 */
            return false;
        }
    }

    /**
     * Extension: Array.prototype.flat{Map,ten}
     */
    @CompatibilityExtension(CompatibilityOption.ArrayPrototypeFlatMapFlatten)
    public enum FlatMapFlattenProperties {
        ;

        /**
         * Array.prototype.flatMap ( mapperFunction [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param mapperFunction
         *            the mapper function
         * @param thisArg
         *            the optional this-argument for the callback function
         * @return the flattened array
         */
        @Function(name = "flatMap", arity = 1)
        public static Object flatMap(ExecutionContext cx, Object thisValue, Object mapperFunction, Object thisArg) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long sourceLen = ToLength(cx, Get(cx, o, "length"));
            /* step 3 */
            if (!IsCallable(mapperFunction)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable mapper = (Callable) mapperFunction;
            /* step 4 (omitted) */
            /* step 5 */
            ScriptObject a = ArraySpeciesCreate(cx, o, 0);
            /* step 6 */
            FlattenIntoArray(cx, a, o, sourceLen, 0, 1, mapper, thisArg);
            /* step 7 */
            return a;
        }

        /**
         * Array.prototype.flatten( [ depth ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param depth
         *            the optional flatten depth
         * @return the flattened array
         */
        @Function(name = "flatten", arity = 0)
        public static Object flatten(ExecutionContext cx, Object thisValue, Object depth) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            long sourceLen = ToLength(cx, Get(cx, o, "length"));
            /* step 3 */
            long depthNum = 1;
            /* step 4 */
            if (!Type.isUndefined(depth)) {
                depthNum = (long) ToNumber(cx, depth); // ToInteger
            }
            /* step 5 */
            ScriptObject a = ArraySpeciesCreate(cx, o, 0);
            /* step 6 */
            FlattenIntoArray(cx, a, o, sourceLen, 0, depthNum);
            /* step 7 */
            return a;
        }

        /**
         * FlattenIntoArray(target, source, sourceLen, start, depth [ , mapperFunction, thisArg ])
         * 
         * @param cx
         *            the execution context
         * @param target
         *            the target array
         * @param source
         *            the source array
         * @param sourceLen
         *            the source array's lenght
         * @param start
         *            the start index
         * @param depth
         *            the maximum depth
         * @return the assigned last index
         */
        private static long FlattenIntoArray(ExecutionContext cx, ScriptObject target, ScriptObject source,
                long sourceLen, long start, long depth) {
            return FlattenIntoArray(cx, target, source, sourceLen, start, depth, null, null);
        }

        /**
         * FlattenIntoArray(target, source, sourceLen, start, depth [ , mapperFunction, thisArg ])
         * 
         * @param cx
         *            the execution context
         * @param target
         *            the target array
         * @param source
         *            the source array
         * @param sourceLen
         *            the source array's lenght
         * @param start
         *            the start index
         * @param depth
         *            the maximum depth
         * @param mapperFunction
         *            the mapper function
         * @param thisArg
         *            the this-argument for the mapper function
         * @return the assigned last index
         */
        private static long FlattenIntoArray(ExecutionContext cx, ScriptObject target, ScriptObject source,
                long sourceLen, long start, long depth, Callable mapperFunction, Object thisArg) {
            /* step 1 */
            long targetIndex = start;
            /* steps 2-3 */
            for (long sourceIndex = 0; sourceIndex < sourceLen; ++sourceIndex) {
                /* step 3.a (not applicable) */
                /* step 3.b */
                boolean exists = HasProperty(cx, source, sourceIndex);
                /* step 3.c */
                if (exists) {
                    /* step 3.c.i */
                    Object element = Get(cx, source, sourceIndex);
                    /* step 3.c.ii */
                    if (mapperFunction != null) {
                        element = Call(cx, mapperFunction, thisArg, element, sourceIndex, source);
                    }
                    /* steps 3.c.iii-iv */
                    boolean shouldFlatten = depth > 0 && IsArray(cx, element);
                    /* steps 3.c.v-vi */
                    if (shouldFlatten) {
                        /* step 3.c.v.1 */
                        long elementLen = ToLength(cx, Get(cx, Type.objectValue(element), "length"));
                        /* step 3.c.v.2 */
                        targetIndex = FlattenIntoArray(cx, target, Type.objectValue(element), elementLen, targetIndex,
                                depth - 1);
                    } else {
                        /* step 3.c.vi.1 */
                        if (targetIndex >= ARRAY_LENGTH_LIMIT) {
                            throw newTypeError(cx, Messages.Key.InvalidArrayLength);
                        }
                        /* step 3.c.vi.2 */
                        CreateDataPropertyOrThrow(cx, target, targetIndex, element);
                        /* step 3.c.vi.3 */
                        targetIndex += 1;
                    }
                }
                /* step 3.d (omitted) */
            }
            /* step 4 */
            return targetIndex;
        }
    }
}

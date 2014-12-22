/**
 * Copyright (c) 2012-2014 André Bargull
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
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArraySpeciesCreate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.*;
import com.github.anba.es6draft.runtime.internal.Properties.AliasFunction;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Optional;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.ArrayIteratorPrototype.ArrayIterationKind;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBufferObject;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayObject;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayPrototypePrototype;
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
public final class ArrayPrototype extends OrdinaryObject implements Initializable {
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
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
        createProperties(cx, this, AdditionalProperties.class);
    }

    /**
     * Marker class for {@code Array.prototype.values}.
     */
    private static final class ArrayPrototypeValues {
    }

    public static boolean isBuiltinValues(Object next) {
        return next instanceof NativeFunction
                && ((NativeFunction) next).getId() == ArrayPrototypeValues.class;
    }

    private static final boolean NO_ARRAY_OPTIMIZATION = false;
    private static final int MIN_SPARSE_LENGTH = 100; // Arbitrarily chosen limit
    private static final int MAX_PROTO_DEPTH = 10; // Arbitrarily chosen limit

    private enum IterationKind {
        DenseOwnKeys, SparseOwnKeys, InheritedKeys, TypedArray, Slow;

        public boolean isSparse() {
            return this == SparseOwnKeys || this == InheritedKeys;
        }

        public boolean isInherited() {
            return this == InheritedKeys;
        }
    }

    private static IterationKind iterationKind(ScriptObject object, long length) {
        return iterationKind(object, object, length);
    }

    private static IterationKind iterationKind(ScriptObject target, ScriptObject source, long length) {
        if (NO_ARRAY_OPTIMIZATION) {
            return IterationKind.Slow;
        }
        if (length == 0) {
            return IterationKind.Slow;
        }
        if (length < MIN_SPARSE_LENGTH) {
            return IterationKind.Slow;
        }
        if (!(target instanceof OrdinaryObject)
                || ((OrdinaryObject) target).hasSpecialIndexedProperties()) {
            return IterationKind.Slow;
        }
        if (source instanceof ArrayObject) {
            return iterationKind((ArrayObject) source);
        }
        if (source instanceof TypedArrayObject) {
            return iterationKind((TypedArrayObject) source);
        }
        if (source instanceof OrdinaryObject) {
            return iterationKind((OrdinaryObject) source, length);
        }
        return IterationKind.Slow;
    }

    private static IterationKind iterationKind(ArrayObject array) {
        if (array.isDenseArray()) {
            return IterationKind.DenseOwnKeys;
        }
        if (array.hasIndexedAccessors()) {
            return IterationKind.Slow;
        }
        return iterationKindForSparse(array);
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
        return iterationKindForSparse(arrayLike);
    }

    private static IterationKind iterationKind(TypedArrayObject typedArray) {
        ArrayBufferObject buffer = typedArray.getBuffer();
        if (buffer == null || buffer.isDetached()) {
            return IterationKind.Slow;
        }
        return IterationKind.TypedArray;
    }

    private static IterationKind iterationKindForSparse(OrdinaryObject arrayLike) {
        IterationKind iteration = IterationKind.SparseOwnKeys;
        int protoDepth = 0;
        for (OrdinaryObject object = arrayLike;;) {
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
                iteration = IterationKind.InheritedKeys;
            }
            if (++protoDepth == MAX_PROTO_DEPTH) {
                return IterationKind.Slow;
            }
        }
        return iteration;
    }

    private static long[] arrayKeys(OrdinaryObject array, long from, long to, boolean inherited) {
        if (inherited) {
            return inheritedKeys(array, from, to);
        }
        return array.indices(from, to);
    }

    private static long[] inheritedKeys(OrdinaryObject array, long from, long to) {
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

    private static ForwardIter forwardIter(OrdinaryObject array, long from, long to,
            boolean inherited) {
        return new ForwardIter(from, to, arrayKeys(array, from, to, inherited), inherited);
    }

    private static ReverseIter reverseIterator(OrdinaryObject array, long from, long to,
            boolean inherited) {
        return new ReverseIter(from, to, arrayKeys(array, from, to, inherited), inherited);
    }

    private static abstract class KeyIter {
        protected final long from;
        protected final long to;
        protected final long[] keys;
        protected final boolean inherited;
        protected final int length;
        protected int index;
        protected long lastKey;

        KeyIter(long from, long to, long[] keys, boolean inherited, int index, long lastKey) {
            this.from = from;
            this.to = to;
            this.keys = keys;
            this.inherited = inherited;
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
    }

    private static final class ForwardIter extends KeyIter {
        ForwardIter(long from, long to, long[] keys, boolean inherited) {
            super(from, to, keys, inherited, 0, from - 1);
        }

        boolean hasNext() {
            for (; index < length; ++index) {
                long key = keys[index];
                if (key != lastKey) {
                    assert lastKey < key && (from <= key && key < to);
                    return true;
                }
                assert inherited;
            }
            return false;
        }

        long next() {
            assert index < length;
            return lastKey = keys[index++];
        }

        boolean contains(long needle) {
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

    private static final class ReverseIter extends KeyIter {
        ReverseIter(long from, long to, long[] keys, boolean inherited) {
            super(from, to, keys, inherited, keys.length - 1, to);
        }

        boolean hasNext() {
            for (; index >= 0; --index) {
                long key = keys[index];
                if (key != lastKey) {
                    assert key < lastKey && (from <= key && key < to);
                    return true;
                }
                assert inherited;
            }
            return false;
        }

        long next() {
            assert index >= 0;
            return lastKey = keys[index--];
        }

        boolean contains(long needle) {
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

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 22.1.3.2 Array.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Array;

        /**
         * 22.1.3.27 Array.prototype.toString ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the string representation
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            ScriptObject array = ToObject(cx, thisValue);
            /* steps 3-4 */
            Object func = Get(cx, array, "join");
            /* step 5 */
            if (!IsCallable(func)) {
                func = cx.getIntrinsic(Intrinsics.ObjProto_toString);
            }
            /* step 6 */
            return ((Callable) func).call(cx, array);
        }

        /**
         * 22.1.3.26 Array.prototype.toLocaleString ( [ reserved1 [ , reserved2 ] ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the locale specific string representation
         */
        @Function(name = "toLocaleString", arity = 0)
        public static Object toLocaleString(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            ScriptObject array = ToObject(cx, thisValue);
            /* step 3 */
            Object arrayLen = Get(cx, array, "length");
            /* steps 4-5 */
            long len = ToLength(cx, arrayLen);
            /* step 6 */
            String separator = cx.getRealm().getListSeparator();
            /* step 7 */
            if (len == 0) {
                return "";
            }
            /* steps 8-9 */
            Object firstElement = Get(cx, array, "0");
            /* steps 10-11 */
            StringBuilder r = new StringBuilder();
            if (Type.isUndefinedOrNull(firstElement)) {
                r.append("");
            } else {
                r.append(ToString(cx, Invoke(cx, firstElement, "toLocaleString")));
            }
            /* steps 12-13 */
            for (long k = 1; k < len; ++k) {
                Object nextElement = Get(cx, array, k);
                if (Type.isUndefinedOrNull(nextElement)) {
                    r.append(separator).append("");
                } else {
                    r.append(separator).append(
                            ToString(cx, Invoke(cx, nextElement, "toLocaleString")));
                }
            }
            /* step 14 */
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
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* steps 3-4 */
            ScriptObject a = ArraySpeciesCreate(cx, o, 0);
            /* step 5 */
            long n = 0;
            /* step 6 */
            Object[] allItems = new Object[items.length + 1];
            allItems[0] = o;
            System.arraycopy(items, 0, allItems, 1, items.length);
            /* step 7 */
            for (Object item : allItems) {
                boolean spreadable = IsConcatSpreadable(cx, item);
                if (spreadable) {
                    ScriptObject e = (ScriptObject) item;
                    /* step 7.d.ii */
                    Object lenVal = Get(cx, e, "length");
                    /* steps 7.d.iii-7.d.iv */
                    long len = ToLength(cx, lenVal);
                    IterationKind iteration = iterationKind(a, e, len);
                    // Optimization: Sparse Array objects
                    if (iteration.isSparse()) {
                        concatSpread(cx, (OrdinaryObject) a, n, (OrdinaryObject) e, len,
                                iteration.isInherited());
                        n += len;
                        continue;
                    }
                    // Optimization: TypedArray objects
                    if (iteration == IterationKind.TypedArray) {
                        concatSpread(cx, (OrdinaryObject) a, n, (TypedArrayObject) e, len);
                        n += len;
                        continue;
                    }
                    /* steps 7.d.i, 7.d.v */
                    for (long k = 0; k < len; ++k, ++n) {
                        long p = k;
                        boolean exists = HasProperty(cx, e, p);
                        if (exists) {
                            Object subElement = Get(cx, e, p);
                            CreateDataPropertyOrThrow(cx, a, n, subElement);
                        }
                    }
                } else {
                    /* step 7.e */
                    CreateDataPropertyOrThrow(cx, a, n++, item);
                }
            }
            /* steps 8-9 */
            // TODO: handle 2^53-1 limit
            Put(cx, a, "length", n, true);
            /* step 10 */
            return a;
        }

        private static void concatSpread(ExecutionContext cx, OrdinaryObject a, long n,
                OrdinaryObject e, long length, boolean inherited) {
            for (ForwardIter iter = forwardIter(e, 0, length, inherited); iter.hasNext();) {
                long k = iter.next();
                Object subElement = Get(cx, e, k);
                CreateDataPropertyOrThrow(cx, a, n + k, subElement);
            }
        }

        private static void concatSpread(ExecutionContext cx, OrdinaryObject a, long n,
                TypedArrayObject e, long length) {
            assert length > 0;
            long actualLength = Math.min(length, e.getArrayLength());
            for (long k = 0; k < actualLength; ++k) {
                Object subElement = Get(cx, e, k);
                CreateDataPropertyOrThrow(cx, a, n + k, subElement);
            }
        }

        /**
         * 22.1.3.3.1 IsConcatSpreadable (O) Abstract Operation
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
            /* steps 2-3 */
            Object spreadable = Get(cx, object, BuiltinSymbol.isConcatSpreadable.get());
            /* step 4 */
            if (!Type.isUndefined(spreadable)) {
                return ToBoolean(spreadable);
            }
            /* step 5 */
            return IsArray(cx, object);
        }

        /**
         * 22.1.3.12 Array.prototype.join (separator)
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
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            /* steps 6-14 */
            return join(cx, o, len, separator);
        }

        /**
         * 22.1.3.12 Array.prototype.join (separator)
         * 
         * @param cx
         *            the execution context
         * @param o
         *            the script object
         * @param len
         *            the length value
         * @param separator
         *            the separator string
         * @return the result string
         * @see ArrayPrototype.Properties#join(ExecutionContext, Object, Object)
         * @see TypedArrayPrototypePrototype.Properties#join(ExecutionContext, Object, Object)
         */
        public static String join(ExecutionContext cx, ScriptObject o, long len, Object separator) {
            /* steps 1-5 (not applicable) */
            /* step 6 */
            if (Type.isUndefined(separator)) {
                separator = ",";
            }
            /* step 7 */
            String sep = ToFlatString(cx, separator);
            /* step 8 */
            if (len == 0) {
                return "";
            }
            /* step 9 */
            Object element0 = Get(cx, o, 0);
            /* steps 10-11 */
            StringBuilder r = new StringBuilder();
            if (Type.isUndefinedOrNull(element0)) {
                r.append("");
            } else {
                r.append(ToString(cx, element0));
            }
            IterationKind iteration = iterationKind(o, len);
            if (iteration.isSparse()) {
                /* steps 12-13 (Optimization: Sparse Array objects) */
                joinSparse(cx, (OrdinaryObject) o, len, sep, r, iteration.isInherited());
            } else {
                /* steps 12-13 */
                for (long k = 1; k < len; ++k) {
                    Object element = Get(cx, o, k);
                    if (Type.isUndefinedOrNull(element)) {
                        r.append(sep).append("");
                    } else {
                        r.append(sep).append(ToString(cx, element));
                    }
                }
            }
            /* step 14 */
            return r.toString();
        }

        private static void joinSparse(ExecutionContext cx, OrdinaryObject o, long length,
                String sep, StringBuilder r, boolean inherited) {
            final boolean hasSeparator = !sep.isEmpty();
            if (hasSeparator) {
                long estimated = length * sep.length();
                if (estimated >= Integer.MAX_VALUE || length > Long.MAX_VALUE / sep.length()) {
                    throw newInternalError(cx, Messages.Key.OutOfMemory);
                }
            }
            long lastKey = 0;
            objectElement: {
                for (ForwardIter iter = forwardIter(o, 1, length, inherited); iter.hasNext();) {
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
                Object element = Get(cx, o, k);
                r.append(sep);
                if (!Type.isUndefinedOrNull(element)) {
                    r.append(ToString(cx, element));
                }
            }
        }

        /**
         * 22.1.3.16 Array.prototype.pop ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the popped array element
         */
        @Function(name = "pop", arity = 0)
        public static Object pop(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            if (len == 0) {
                /* step 6 */
                Put(cx, o, "length", 0, true);
                return UNDEFINED;
            } else {
                /* step 7 */
                assert len > 0;
                long newLen = len - 1;
                long index = newLen;
                Object element = Get(cx, o, index);
                DeletePropertyOrThrow(cx, o, index);
                Put(cx, o, "length", newLen, true);
                return element;
            }
        }

        /**
         * 22.1.3.17 Array.prototype.push ( ...items )
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
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long n = ToLength(cx, lenVal);
            /* steps 6-7 */
            for (Object e : items) {
                Put(cx, o, n, e, true);
                n += 1;
            }
            /* steps 8-9 */
            // TODO: handle 2^53-1 limit
            Put(cx, o, "length", n, true);
            /* step 10 */
            return n;
        }

        /**
         * 22.1.3.20 Array.prototype.reverse ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return this array object
         */
        @Function(name = "reverse", arity = 0)
        public static Object reverse(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            /* steps 6-9 */
            return reverse(cx, o, len);
        }

        /**
         * 22.1.3.20 Array.prototype.reverse ( )
         * 
         * @param cx
         *            the execution context
         * @param o
         *            the script object
         * @param len
         *            the length value
         * @return this array object
         * @see ArrayPrototype.Properties#reverse(ExecutionContext, Object)
         * @see TypedArrayPrototypePrototype.Properties#reverse(ExecutionContext, Object)
         */
        public static ScriptObject reverse(ExecutionContext cx, ScriptObject o, long len) {
            /* steps 1-5 (not applicable) */
            IterationKind iteration = iterationKind(o, len);
            if (iteration.isSparse()) {
                /* steps 6-8 (Optimization: Sparse Array objects) */
                reverseSparse(cx, (OrdinaryObject) o, len, iteration.isInherited());
            } else {
                /* step 6 */
                long middle = len / 2L;
                /* steps 7-8 */
                for (long lower = 0; lower != middle; ++lower) {
                    long upper = len - lower - 1;
                    long upperP = upper;
                    long lowerP = lower;
                    boolean lowerExists = HasProperty(cx, o, lowerP);
                    Object lowerValue = lowerExists ? Get(cx, o, lowerP) : null;
                    boolean upperExists = HasProperty(cx, o, upperP);
                    Object upperValue = upperExists ? Get(cx, o, upperP) : null;
                    if (lowerExists && upperExists) {
                        Put(cx, o, lowerP, upperValue, true);
                        Put(cx, o, upperP, lowerValue, true);
                    } else if (!lowerExists && upperExists) {
                        Put(cx, o, lowerP, upperValue, true);
                        DeletePropertyOrThrow(cx, o, upperP);
                    } else if (lowerExists && !upperExists) {
                        DeletePropertyOrThrow(cx, o, lowerP);
                        Put(cx, o, upperP, lowerValue, true);
                    } else {
                        // no action required
                    }
                }
            }
            /* step 9 */
            return o;
        }

        private static void reverseSparse(ExecutionContext cx, OrdinaryObject o, long length,
                boolean inherited) {
            long middle = length / 2L;
            ForwardIter lowerIter = forwardIter(o, 0, middle, inherited);
            ReverseIter upperIter = reverseIterator(o, length - middle, length, inherited);

            while (lowerIter.hasNext() && upperIter.hasNext()) {
                long lower = lowerIter.peek();
                long upper = (length - 1) - upperIter.peek();

                if (lower == upper) {
                    long lowerP = lowerIter.next();
                    long upperP = upperIter.next();

                    Object lowerValue = Get(cx, o, lowerP);
                    Object upperValue = Get(cx, o, upperP);
                    Put(cx, o, lowerP, upperValue, true);
                    Put(cx, o, upperP, lowerValue, true);
                } else if (lower < upper) {
                    long lowerP = lowerIter.next();
                    long upperP = (length - 1) - lower;

                    Object lowerValue = Get(cx, o, lowerP);
                    DeletePropertyOrThrow(cx, o, lowerP);
                    Put(cx, o, upperP, lowerValue, true);
                } else {
                    long upperP = upperIter.next();
                    long lowerP = upper;

                    Object upperValue = Get(cx, o, upperP);
                    Put(cx, o, lowerP, upperValue, true);
                    DeletePropertyOrThrow(cx, o, upperP);
                }
            }

            while (lowerIter.hasNext()) {
                long lowerP = lowerIter.next();
                long upperP = (length - 1) - lowerP;

                Object lowerValue = Get(cx, o, lowerP);
                DeletePropertyOrThrow(cx, o, lowerP);
                Put(cx, o, upperP, lowerValue, true);
            }

            while (upperIter.hasNext()) {
                long upperP = upperIter.next();
                long lowerP = (length - 1) - upperP;

                Object upperValue = Get(cx, o, upperP);
                Put(cx, o, lowerP, upperValue, true);
                DeletePropertyOrThrow(cx, o, upperP);
            }
        }

        /**
         * 22.1.3.21 Array.prototype.shift ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the shifted array element
         */
        @Function(name = "shift", arity = 0)
        public static Object shift(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            /* step 6 */
            if (len == 0) {
                Put(cx, o, "length", 0, true);
                return UNDEFINED;
            }
            assert len > 0;
            /* steps 7-8 */
            Object first = Get(cx, o, 0);
            IterationKind iteration = iterationKind(o, len);
            if (iteration.isSparse()) {
                /* steps 9-10 (Optimization: Sparse Array objects) */
                shiftSparse(cx, (OrdinaryObject) o, len, iteration.isInherited());
            } else {
                /* steps 9-10 */
                for (long k = 1; k < len; ++k) {
                    long from = k;
                    long to = k - 1;
                    boolean fromPresent = HasProperty(cx, o, from);
                    if (fromPresent) {
                        Object fromVal = Get(cx, o, from);
                        Put(cx, o, to, fromVal, true);
                    } else {
                        DeletePropertyOrThrow(cx, o, to);
                    }
                }
            }
            /* steps 11-12 */
            DeletePropertyOrThrow(cx, o, len - 1);
            /* steps 13-14 */
            Put(cx, o, "length", len - 1, true);
            /* step 15 */
            return first;
        }

        private static void shiftSparse(ExecutionContext cx, OrdinaryObject o, long length,
                boolean inherited) {
            ForwardIter iter = forwardIter(o, 1, length, inherited);
            if (iter.hasNext() && iter.peek() != 1) {
                DeletePropertyOrThrow(cx, o, 0);
            }
            while (iter.hasNext()) {
                long k = iter.next();

                long from = k;
                long to = k - 1;
                Object fromVal = Get(cx, o, from);
                Put(cx, o, to, fromVal, true);

                long replacement = k + 1;
                if (replacement < length && !iter.contains(replacement)) {
                    DeletePropertyOrThrow(cx, o, from);
                }
            }
        }

        /**
         * 22.1.3.22 Array.prototype.slice (start, end)
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
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            /* steps 6-7 */
            double relativeStart = ToInteger(cx, start);
            /* step 8 */
            long k;
            if (relativeStart < 0) {
                k = (long) Math.max(len + relativeStart, 0);
            } else {
                k = (long) Math.min(relativeStart, len);
            }
            /* steps 9-10 */
            double relativeEnd;
            if (Type.isUndefined(end)) {
                relativeEnd = len;
            } else {
                relativeEnd = ToInteger(cx, end);
            }
            /* step 11 */
            long finall;
            if (relativeEnd < 0) {
                finall = (long) Math.max(len + relativeEnd, 0);
            } else {
                finall = (long) Math.min(relativeEnd, len);
            }
            /* step 12 */
            long count = Math.max(finall - k, 0);
            /* steps 13-14 */
            ScriptObject a = ArraySpeciesCreate(cx, o, count);
            long n;
            IterationKind iteration = iterationKind(a, o, len);
            if (iteration.isSparse()) {
                /* steps 15-16 (Optimization: Sparse Array objects) */
                sliceSparse(cx, (OrdinaryObject) a, k, finall, (OrdinaryObject) o,
                        iteration.isInherited());
                n = count;
            } else {
                n = 0;
                /* steps 15-16 */
                for (; k < finall; ++k, ++n) {
                    long pk = k;
                    boolean kpresent = HasProperty(cx, o, pk);
                    if (kpresent) {
                        Object kvalue = Get(cx, o, pk);
                        CreateDataPropertyOrThrow(cx, a, n, kvalue);
                    }
                }
            }
            /* steps 17-18 */
            Put(cx, a, "length", n, true);
            /* step 19 */
            return a;
        }

        private static void sliceSparse(ExecutionContext cx, OrdinaryObject a, long k, long finall,
                OrdinaryObject o, boolean inherited) {
            for (ForwardIter iter = forwardIter(o, k, finall, inherited); iter.hasNext();) {
                long pk = iter.next();

                long n = pk - k;
                Object kvalue = Get(cx, o, pk);
                CreateDataPropertyOrThrow(cx, a, n, kvalue);
            }
        }

        /**
         * 22.1.3.24.1 Runtime Semantics: SortCompare Abstract Operation
         */
        private static final class DefaultComparator implements Comparator<Object> {
            private final ExecutionContext cx;

            DefaultComparator(ExecutionContext cx) {
                this.cx = cx;
            }

            @Override
            public int compare(Object o1, Object o2) {
                String x = ToFlatString(cx, o1);
                String y = ToFlatString(cx, o2);
                return x.compareTo(y);
            }
        }

        /**
         * 22.1.3.24.1 Runtime Semantics: SortCompare Abstract Operation
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
                double c = ToNumber(cx, comparefn.call(cx, UNDEFINED, o1, o2));
                return (c < 0 ? -1 : c > 0 ? 1 : 0);
            }
        }

        private static void sortElements(ExecutionContext cx, ArrayList<Object> elements,
                Object comparefn) {
            Comparator<Object> comparator;
            if (!Type.isUndefined(comparefn)) {
                if (!IsCallable(comparefn)) {
                    throw newTypeError(cx, Messages.Key.NotCallable);
                }
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
         * 22.1.3.24 Array.prototype.sort (comparefn)
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
            ScriptObject obj = ToObject(cx, thisValue);
            /* step 2 */
            Object lenValue = Get(cx, obj, "length");
            /* steps 3-4 */
            long len = ToLength(cx, lenValue);

            IterationKind iteration = iterationKind(obj, len);
            if (iteration.isSparse()) {
                sortSparse(cx, (OrdinaryObject) obj, len, comparefn, iteration.isInherited());
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
                    sortElements(cx, elements, comparefn);
                }

                // and finally set sorted elements
                for (int i = 0, offset = 0; i < count; ++i) {
                    int p = offset + i;
                    Put(cx, obj, p, elements.get(i), true);
                }
                for (int i = 0, offset = count; i < undefCount; ++i) {
                    int p = offset + i;
                    Put(cx, obj, p, UNDEFINED, true);
                }
                for (int i = 0, offset = count + undefCount; i < emptyCount; ++i) {
                    int p = offset + i;
                    DeletePropertyOrThrow(cx, obj, p);
                }
            }
            return obj;
        }

        private static void sortSparse(ExecutionContext cx, OrdinaryObject obj, long length,
                Object comparefn, boolean inherited) {
            // collect elements
            ForwardIter collectIter = forwardIter((OrdinaryObject) obj, 0, length, inherited);
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
                Put(cx, obj, p, elements.get(i), true);
            }
            for (int i = 0, offset = count; i < undefCount; ++i) {
                int p = offset + i;
                Put(cx, obj, p, UNDEFINED, true);
            }
            // User-defined actions in comparefn may have invalidated sparse-property
            IterationKind iterationDelete = iterationKind(obj, length);
            if (iterationDelete.isSparse()) {
                for (ForwardIter deleteIter = forwardIter((OrdinaryObject) obj, count + undefCount,
                        length, iterationDelete.isInherited()); deleteIter.hasNext();) {
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
         * 22.1.3.25 Array.prototype.splice (start, deleteCount, ...items )
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
                @Optional(Optional.Default.NONE) Object start,
                @Optional(Optional.Default.NONE) Object deleteCount, Object... items) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            /* steps 6-7 */
            double relativeStart = (start != null ? ToInteger(cx, start) : 0);
            /* step 8 */
            long actualStart;
            if (relativeStart < 0) {
                actualStart = (long) Math.max(len + relativeStart, 0);
            } else {
                actualStart = (long) Math.min(relativeStart, len);
            }
            /* steps 9-11 */
            long actualDeleteCount;
            if (start == null) {
                actualDeleteCount = 0;
            } else if (deleteCount == null) {
                actualDeleteCount = len - actualStart;
            } else {
                double dc = ToInteger(cx, deleteCount);
                actualDeleteCount = (long) Math.min(Math.max(dc, 0), len - actualStart);
            }
            /* steps 12-13 */
            ScriptObject a = ArraySpeciesCreate(cx, o, actualDeleteCount);
            IterationKind iterationCopy = iterationKind(a, o, actualDeleteCount);
            if (iterationCopy.isSparse()) {
                /* steps 14-15 */
                spliceSparseCopy(cx, (OrdinaryObject) a, (OrdinaryObject) o, actualStart,
                        actualDeleteCount, iterationCopy.isInherited());
            } else {
                /* steps 14-15 */
                for (long k = 0; k < actualDeleteCount; ++k) {
                    long from = actualStart + k;
                    boolean fromPresent = HasProperty(cx, o, from);
                    if (fromPresent) {
                        Object fromValue = Get(cx, o, from);
                        CreateDataPropertyOrThrow(cx, a, k, fromValue);
                    }
                }
            }
            /* steps 16-17 */
            Put(cx, a, "length", actualDeleteCount, true);
            /* steps 18-19 */
            int itemCount = items.length;
            if (itemCount < actualDeleteCount) {
                /* step 20 */
                IterationKind iterationMove = iterationKind(o, len);
                if (iterationMove.isSparse()) {
                    /* steps 20.a-20.b */
                    spliceSparseMoveLeft(cx, (OrdinaryObject) o, len, actualStart,
                            actualDeleteCount, itemCount, iterationMove.isInherited());
                } else {
                    /* steps 20.a-20.b */
                    for (long k = actualStart; k < (len - actualDeleteCount); ++k) {
                        long from = k + actualDeleteCount;
                        long to = k + itemCount;
                        boolean fromPresent = HasProperty(cx, o, from);
                        if (fromPresent) {
                            Object fromValue = Get(cx, o, from);
                            Put(cx, o, to, fromValue, true);
                        } else {
                            DeletePropertyOrThrow(cx, o, to);
                        }
                    }
                }
                IterationKind iterationDelete = iterationKind(o, len);
                if (iterationDelete.isSparse()) {
                    /* steps 20.c-20.d */
                    spliceSparseDelete(cx, (OrdinaryObject) o, len, actualDeleteCount, itemCount,
                            iterationDelete.isInherited());
                } else {
                    /* steps 20.c-20.d */
                    for (long k = len; k > (len - actualDeleteCount + itemCount); --k) {
                        DeletePropertyOrThrow(cx, o, k - 1);
                    }
                }
            } else if (itemCount > actualDeleteCount) {
                /* step 21 */
                IterationKind iterationMove = iterationKind(o, len);
                if (iterationMove.isSparse()) {
                    /* step 21 */
                    spliceSparseMoveRight(cx, (OrdinaryObject) o, len, actualStart,
                            actualDeleteCount, itemCount, iterationMove.isInherited());
                } else {
                    /* step 21 */
                    for (long k = (len - actualDeleteCount); k > actualStart; --k) {
                        long from = k + actualDeleteCount - 1;
                        long to = k + itemCount - 1;
                        boolean fromPresent = HasProperty(cx, o, from);
                        if (fromPresent) {
                            Object fromValue = Get(cx, o, from);
                            Put(cx, o, to, fromValue, true);
                        } else {
                            DeletePropertyOrThrow(cx, o, to);
                        }
                    }
                }
            }
            /* step 22 */
            long k = actualStart;
            /* step 23 */
            for (int i = 0; i < itemCount; ++k, ++i) {
                Object e = items[i];
                Put(cx, o, k, e, true);
            }
            /* steps 24-25 */
            // TODO: handle 2^53-1 limit
            Put(cx, o, "length", len - actualDeleteCount + itemCount, true);
            /* step 26 */
            return a;
        }

        private static void spliceSparseMoveRight(ExecutionContext cx, OrdinaryObject o,
                long length, long start, long deleteCount, int itemCount, boolean inherited) {
            assert 0 <= deleteCount && deleteCount <= (length - start) : "actualDeleteCount="
                    + deleteCount;
            assert start >= 0 && (itemCount > 0 && itemCount > deleteCount);

            long targetRangeStart = start + itemCount;
            long targetRangeEnd = length;
            ReverseIter iterTarget = reverseIterator(o, targetRangeStart, targetRangeEnd, inherited);

            long sourceRangeStart = start + deleteCount;
            long sourceRangeEnd = length;
            ReverseIter iterSource = reverseIterator(o, sourceRangeStart, sourceRangeEnd, inherited);

            while (iterTarget.hasNext() && iterSource.hasNext()) {
                long toRel = iterTarget.peek() - itemCount;
                long fromRel = iterSource.peek() - deleteCount;
                if (toRel == fromRel) {
                    long from = iterSource.next();
                    long to = iterTarget.next();
                    Object fromValue = Get(cx, o, from);
                    Put(cx, o, to, fromValue, true);
                } else if (toRel < fromRel) {
                    long from = iterSource.next();
                    long actualTo = from - deleteCount + itemCount;
                    Object fromValue = Get(cx, o, from);
                    Put(cx, o, actualTo, fromValue, true);
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
                Put(cx, o, actualTo, fromValue, true);
            }
        }

        private static void spliceSparseMoveLeft(ExecutionContext cx, OrdinaryObject o,
                long length, long start, long deleteCount, int itemCount, boolean inherited) {
            assert 0 < deleteCount && deleteCount <= (length - start) : "actualDeleteCount="
                    + deleteCount;
            assert (itemCount < deleteCount);
            assert start >= 0 && itemCount >= 0;
            long moveAmount = (length - start) - deleteCount;
            assert moveAmount >= 0;

            long targetRangeStart = start + itemCount;
            long targetRangeEnd = targetRangeStart + moveAmount;
            assert targetRangeEnd < length;
            ForwardIter iterTarget = forwardIter(o, targetRangeStart, targetRangeEnd, inherited);

            long sourceRangeStart = start + deleteCount;
            long sourceRangeEnd = sourceRangeStart + moveAmount;
            assert sourceRangeEnd == length;
            ForwardIter iterSource = forwardIter(o, sourceRangeStart, sourceRangeEnd, inherited);

            while (iterTarget.hasNext() && iterSource.hasNext()) {
                long toRel = iterTarget.peek() - itemCount;
                long fromRel = iterSource.peek() - deleteCount;
                if (toRel == fromRel) {
                    long to = iterTarget.next();
                    long from = iterSource.next();
                    Object fromValue = Get(cx, o, from);
                    Put(cx, o, to, fromValue, true);
                } else if (toRel < fromRel) {
                    long to = iterTarget.next();
                    DeletePropertyOrThrow(cx, o, to);
                } else {
                    long from = iterSource.next();
                    long actualTo = from - deleteCount + itemCount;
                    Object fromValue = Get(cx, o, from);
                    Put(cx, o, actualTo, fromValue, true);
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
                Put(cx, o, actualTo, fromValue, true);
            }
        }

        private static void spliceSparseCopy(ExecutionContext cx, OrdinaryObject a,
                OrdinaryObject o, long start, long deleteCount, boolean inherited) {
            long copyEnd = start + deleteCount;
            for (ForwardIter iter = forwardIter(o, start, copyEnd, inherited); iter.hasNext();) {
                long from = iter.next();
                Object fromValue = Get(cx, o, from);
                CreateDataPropertyOrThrow(cx, a, from - start, fromValue);
            }
        }

        private static void spliceSparseDelete(ExecutionContext cx, OrdinaryObject o, long length,
                long deleteCount, long itemCount, boolean inherited) {
            long delStart = length - deleteCount + itemCount;
            for (ReverseIter iter = reverseIterator(o, delStart, length, inherited); iter.hasNext();) {
                long k = iter.next();
                DeletePropertyOrThrow(cx, o, k);
            }
        }

        /**
         * 22.1.3.28 Array.prototype.unshift ( ...items )
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
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            /* step 6 */
            int argCount = items.length;
            /* step 7 */
            if (argCount > 0) {
                IterationKind iteration = iterationKind(o, len);
                if (iteration.isSparse()) {
                    /* steps 7.a-7.b (Optimization: Sparse Array objects) */
                    unshiftSparse(cx, (OrdinaryObject) o, len, argCount, iteration.isInherited());
                } else {
                    /* steps 7.a-7.b */
                    for (long k = len; k > 0; --k) {
                        long from = k - 1;
                        long to = k + argCount - 1;
                        boolean fromPresent = HasProperty(cx, o, from);
                        if (fromPresent) {
                            Object fromValue = Get(cx, o, from);
                            Put(cx, o, to, fromValue, true);
                        } else {
                            DeletePropertyOrThrow(cx, o, to);
                        }
                    }
                }
                /* steps 7.c-7.e */
                for (int j = 0; j < items.length; ++j) {
                    Object e = items[j];
                    Put(cx, o, j, e, true);
                }
            }
            /* steps 8-9 */
            // TODO: handle 2^53-1 limit
            Put(cx, o, "length", len + argCount, true);
            /* step 10 */
            return len + argCount;
        }

        private static void unshiftSparse(ExecutionContext cx, OrdinaryObject o, long length,
                int argCount, boolean inherited) {
            ReverseIter iter = reverseIterator(o, 0, length, inherited);
            int deleteFirst = 0, deleteLast = 0;
            long[] keysToDelete = new long[iter.size()];
            while (iter.hasNext()) {
                long k = iter.next();
                while (deleteLast > deleteFirst && keysToDelete[deleteFirst] > k) {
                    DeletePropertyOrThrow(cx, o, keysToDelete[deleteFirst++] + argCount);
                }

                long from = k;
                long to = k + argCount;
                Object fromValue = Get(cx, o, from);
                Put(cx, o, to, fromValue, true);

                long replacement = k - argCount;
                if (replacement >= 0 && !iter.contains(replacement)) {
                    keysToDelete[deleteLast++] = replacement;
                }
            }
            while (deleteLast > deleteFirst) {
                DeletePropertyOrThrow(cx, o, keysToDelete[deleteFirst++] + argCount);
            }
        }

        /**
         * 22.1.3.11 Array.prototype.indexOf ( searchElement [ , fromIndex ] )
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
        public static Object indexOf(ExecutionContext cx, Object thisValue, Object searchElement,
                @Optional(Optional.Default.NONE) Object fromIndex) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            /* steps 6-13 */
            return indexOf(cx, o, len, searchElement, fromIndex);
        }

        /**
         * 22.1.3.11 Array.prototype.indexOf ( searchElement [ , fromIndex ] )
         * 
         * @param cx
         *            the execution context
         * @param o
         *            the script object
         * @param len
         *            the length value
         * @param searchElement
         *            the search element
         * @param fromIndex
         *            the optional start index
         * @return the result index
         * @see ArrayPrototype.Properties#indexOf(ExecutionContext, Object, Object, Object)
         * @see TypedArrayPrototypePrototype.Properties#indexOf(ExecutionContext, Object, Object,
         *      Object)
         */
        public static long indexOf(ExecutionContext cx, ScriptObject o, long len,
                Object searchElement, Object fromIndex) {
            /* steps 1-5 (not applicable) */
            /* step 6 */
            if (len == 0) {
                return -1;
            }
            /* steps 7-8 */
            long n;
            if (fromIndex != null) {
                n = (long) ToInteger(cx, fromIndex);
            } else {
                n = 0;
            }
            /* step 9 */
            if (n >= len) {
                return -1;
            }
            /* steps 10-11 */
            long k;
            if (n >= 0) {
                k = n;
            } else {
                k = len - Math.abs(n);
                if (k < 0) {
                    k = 0;
                }
            }
            /* step 12 */
            for (; k < len; ++k) {
                long pk = k;
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object elementk = Get(cx, o, pk);
                    boolean same = StrictEqualityComparison(searchElement, elementk);
                    if (same) {
                        return k;
                    }
                }
            }
            /* step 13 */
            return -1;
        }

        /**
         * 22.1.3.14 Array.prototype.lastIndexOf ( searchElement [ , fromIndex ] )
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
        public static Object lastIndexOf(ExecutionContext cx, Object thisValue,
                Object searchElement, @Optional(Optional.Default.NONE) Object fromIndex) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            /* steps 6-12 */
            return lastIndexOf(cx, o, len, searchElement, fromIndex);
        }

        /**
         * 22.1.3.14 Array.prototype.lastIndexOf ( searchElement [ , fromIndex ] )
         * 
         * @param cx
         *            the execution context
         * @param o
         *            the script object
         * @param len
         *            the length value
         * @param searchElement
         *            the search element
         * @param fromIndex
         *            the optional start index
         * @return the result index
         * @see ArrayPrototype.Properties#lastIndexOf(ExecutionContext, Object, Object, Object)
         * @see TypedArrayPrototypePrototype.Properties#lastIndexOf(ExecutionContext, Object,
         *      Object, Object)
         */
        public static long lastIndexOf(ExecutionContext cx, ScriptObject o, long len,
                Object searchElement, Object fromIndex) {
            /* steps 1-5 (not applicable) */
            /* step 6 */
            if (len == 0) {
                return -1;
            }
            /* steps 7-8 */
            long n;
            if (fromIndex != null) {
                n = (long) ToInteger(cx, fromIndex);
            } else {
                n = len - 1;
            }
            /* steps 9-10 */
            long k;
            if (n >= 0) {
                k = Math.min(n, len - 1);
            } else {
                k = len - Math.abs(n);
            }
            /* step 11 */
            for (; k >= 0; --k) {
                long pk = k;
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object elementk = Get(cx, o, pk);
                    boolean same = StrictEqualityComparison(searchElement, elementk);
                    if (same) {
                        return k;
                    }
                }
            }
            /* step 12 */
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
        public static Object every(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            /* steps 6-10 */
            return every(cx, o, len, callbackfn, thisArg);
        }

        /**
         * 22.1.3.5 Array.prototype.every ( callbackfn [ , thisArg] )
         * 
         * @param cx
         *            the execution context
         * @param o
         *            the script object
         * @param len
         *            the length value
         * @param callbackfn
         *            the callback function
         * @param thisArg
         *            the optional this-argument for the callback function
         * @return {@code true} if every element matches
         * @see ArrayPrototype.Properties#every(ExecutionContext, Object, Object, Object)
         * @see TypedArrayPrototypePrototype.Properties#every(ExecutionContext, Object, Object,
         *      Object)
         */
        public static boolean every(ExecutionContext cx, ScriptObject o, long len,
                Object callbackfn, Object thisArg) {
            /* steps 1-5 (not applicable) */
            /* step 6 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 7 (omitted) */
            /* steps 8-9 */
            for (long k = 0; k < len; ++k) {
                long pk = k;
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    Object testResult = callback.call(cx, thisArg, kvalue, k, o);
                    if (!ToBoolean(testResult)) {
                        return false;
                    }
                }
            }
            /* step 10 */
            return true;
        }

        /**
         * 22.1.3.23 Array.prototype.some ( callbackfn [ , thisArg ] )
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
        public static Object some(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            /* steps 6-10 */
            return some(cx, o, len, callbackfn, thisArg);
        }

        /**
         * 22.1.3.23 Array.prototype.some ( callbackfn [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param o
         *            the script object
         * @param len
         *            the length value
         * @param callbackfn
         *            the callback function
         * @param thisArg
         *            the optional this-argument for the callback function
         * @return {@code true} if some elements match
         * @see ArrayPrototype.Properties#some(ExecutionContext, Object, Object, Object)
         * @see TypedArrayPrototypePrototype.Properties#some(ExecutionContext, Object, Object,
         *      Object)
         */
        public static boolean some(ExecutionContext cx, ScriptObject o, long len,
                Object callbackfn, Object thisArg) {
            /* steps 1-5 (not applicable) */
            /* step 6 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 7 (omitted) */
            /* steps 8-9 */
            for (long k = 0; k < len; ++k) {
                long pk = k;
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    Object testResult = callback.call(cx, thisArg, kvalue, k, o);
                    if (ToBoolean(testResult)) {
                        return true;
                    }
                }
            }
            /* step 10 */
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
        public static Object forEach(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            /* steps 6-10 */
            return forEach(cx, o, len, callbackfn, thisArg);
        }

        /**
         * 22.1.3.10 Array.prototype.forEach ( callbackfn [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param o
         *            the script object
         * @param len
         *            the length value
         * @param callbackfn
         *            the callback function
         * @param thisArg
         *            the optional this-argument for the callback function
         * @return the undefined value
         * @see ArrayPrototype.Properties#forEach(ExecutionContext, Object, Object, Object)
         * @see TypedArrayPrototypePrototype.Properties#forEach(ExecutionContext, Object, Object,
         *      Object)
         */
        public static Object forEach(ExecutionContext cx, ScriptObject o, long len,
                Object callbackfn, Object thisArg) {
            /* steps 1-5 (not applicable) */
            /* step 6 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 7 (omitted) */
            /* steps 8-9 */
            for (long k = 0; k < len; ++k) {
                long pk = k;
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    callback.call(cx, thisArg, kvalue, k, o);
                }
            }
            /* step 10 */
            return UNDEFINED;
        }

        /**
         * 22.1.3.15 Array.prototype.map ( callbackfn [ , thisArg ] )
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
        public static Object map(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            /* step 6 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 7 (omitted) */
            /* steps 8-9 */
            ScriptObject a = ArraySpeciesCreate(cx, o, len);
            /* steps 10-11 */
            for (long k = 0; k < len; ++k) {
                long pk = k;
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    Object mappedValue = callback.call(cx, thisArg, kvalue, k, o);
                    CreateDataPropertyOrThrow(cx, a, pk, mappedValue);
                }
            }
            /* step 12 */
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
        public static Object filter(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            /* step 6 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 7 (omitted) */
            /* steps 8-9 */
            ScriptObject a = ArraySpeciesCreate(cx, o, 0);
            /* steps 10-12 */
            for (long k = 0, to = 0; k < len; ++k) {
                long pk = k;
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    Object selected = callback.call(cx, thisArg, kvalue, k, o);
                    if (ToBoolean(selected)) {
                        CreateDataPropertyOrThrow(cx, a, to, kvalue);
                        to += 1;
                    }
                }
            }
            /* step 13 */
            return a;
        }

        /**
         * 22.1.3.18 Array.prototype.reduce ( callbackfn [ , initialValue ] )
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
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            /* steps 6-12 */
            return reduce(cx, o, len, callbackfn, initialValue);
        }

        /**
         * 22.1.3.18 Array.prototype.reduce ( callbackfn [ , initialValue ] )
         * 
         * @param cx
         *            the execution context
         * @param o
         *            the script object
         * @param len
         *            the length value
         * @param callbackfn
         *            the callback function
         * @param initialValue
         *            the initial value
         * @return the reduced value
         * @see ArrayPrototype.Properties#reduce(ExecutionContext, Object, Object, Object)
         * @see TypedArrayPrototypePrototype.Properties#reduce(ExecutionContext, Object, Object,
         *      Object)
         */
        public static Object reduce(ExecutionContext cx, ScriptObject o, long len,
                Object callbackfn, Object initialValue) {
            /* steps 1-5 (not applicable) */
            /* step 6 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 7 */
            if (len == 0 && initialValue == null) {
                throw newTypeError(cx, Messages.Key.ReduceInitialValue);
            }
            /* step 8 */
            long k = 0;
            /* steps 9-10 */
            Object accumulator = null;
            if (initialValue != null) {
                accumulator = initialValue;
            } else {
                boolean kpresent = false;
                for (; !kpresent && k < len; ++k) {
                    long pk = k;
                    kpresent = HasProperty(cx, o, pk);
                    if (kpresent) {
                        accumulator = Get(cx, o, pk);
                    }
                }
                if (!kpresent) {
                    throw newTypeError(cx, Messages.Key.ReduceInitialValue);
                }
            }
            /* step 11 */
            for (; k < len; ++k) {
                long pk = k;
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    accumulator = callback.call(cx, UNDEFINED, accumulator, kvalue, k, o);
                }
            }
            /* step 12 */
            return accumulator;
        }

        /**
         * 22.1.3.19 Array.prototype.reduceRight ( callbackfn [ , initialValue ] )
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
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            /* steps 6-12 */
            return reduceRight(cx, o, len, callbackfn, initialValue);
        }

        /**
         * 22.1.3.19 Array.prototype.reduceRight ( callbackfn [ , initialValue ] )
         * 
         * @param cx
         *            the execution context
         * @param o
         *            the script object
         * @param len
         *            the length value
         * @param callbackfn
         *            the callback function
         * @param initialValue
         *            the initial value
         * @return the reduced value
         * @see ArrayPrototype.Properties#reduceRight(ExecutionContext, Object, Object, Object)
         * @see TypedArrayPrototypePrototype.Properties#reduceRight(ExecutionContext, Object,
         *      Object, Object)
         */
        public static Object reduceRight(ExecutionContext cx, ScriptObject o, long len,
                Object callbackfn, Object initialValue) {
            /* steps 1-5 (not applicable) */
            /* step 6 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 7 */
            if (len == 0 && initialValue == null) {
                throw newTypeError(cx, Messages.Key.ReduceInitialValue);
            }
            /* step 8 */
            long k = len - 1;
            /* steps 9-10 */
            Object accumulator = null;
            if (initialValue != null) {
                accumulator = initialValue;
            } else {
                boolean kpresent = false;
                for (; !kpresent && k >= 0; --k) {
                    long pk = k;
                    kpresent = HasProperty(cx, o, pk);
                    if (kpresent) {
                        accumulator = Get(cx, o, pk);
                    }
                }
                if (!kpresent) {
                    throw newTypeError(cx, Messages.Key.ReduceInitialValue);
                }
            }
            /* step 11 */
            for (; k >= 0; --k) {
                long pk = k;
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    accumulator = callback.call(cx, UNDEFINED, accumulator, kvalue, k, o);
                }
            }
            /* step 12 */
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
        public static Object find(ExecutionContext cx, Object thisValue, Object predicate,
                Object thisArg) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            /* steps 6-10 */
            return find(cx, o, len, predicate, thisArg);
        }

        /**
         * 22.1.3.8 Array.prototype.find ( predicate [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param o
         *            the script object
         * @param len
         *            the length value
         * @param predicate
         *            the predicate function
         * @param thisArg
         *            the optional this-argument for the predicate function
         * @return the result value
         * @see ArrayPrototype.Properties#find(ExecutionContext, Object, Object, Object)
         * @see TypedArrayPrototypePrototype.Properties#find(ExecutionContext, Object, Object,
         *      Object)
         */
        public static Object find(ExecutionContext cx, ScriptObject o, long len, Object predicate,
                Object thisArg) {
            /* steps 1-5 (not applicable) */
            /* step 6 */
            if (!IsCallable(predicate)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable pred = (Callable) predicate;
            /* step 7 (omitted) */
            /* steps 8-9 */
            for (long k = 0; k < len; ++k) {
                long pk = k;
                Object kvalue = Get(cx, o, pk);
                Object testResult = pred.call(cx, thisArg, kvalue, k, o);
                if (ToBoolean(testResult)) {
                    return kvalue;
                }
            }
            /* step 10 */
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
        public static Object findIndex(ExecutionContext cx, Object thisValue, Object predicate,
                Object thisArg) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            /* steps 6-10 */
            return findIndex(cx, o, len, predicate, thisArg);
        }

        /**
         * 22.1.3.9 Array.prototype.findIndex ( predicate [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param o
         *            the script object
         * @param len
         *            the length value
         * @param predicate
         *            the predicate function
         * @param thisArg
         *            the optional this-argument for the predicate function
         * @return the result index
         * @see ArrayPrototype.Properties#findIndex(ExecutionContext, Object, Object, Object)
         * @see TypedArrayPrototypePrototype.Properties#findIndex(ExecutionContext, Object, Object,
         *      Object)
         */
        public static long findIndex(ExecutionContext cx, ScriptObject o, long len,
                Object predicate, Object thisArg) {
            /* steps 1-5 (not applicable) */
            /* step 6 */
            if (!IsCallable(predicate)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable pred = (Callable) predicate;
            /* step 7 (omitted) */
            /* steps 8-9 */
            for (long k = 0; k < len; ++k) {
                long pk = k;
                Object kvalue = Get(cx, o, pk);
                Object testResult = pred.call(cx, thisArg, kvalue, k, o);
                if (ToBoolean(testResult)) {
                    return k;
                }
            }
            /* step 10 */
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
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            return CreateArrayIterator(cx, o, ArrayIterationKind.KeyValue);
        }

        /**
         * 22.1.3.13 Array.prototype.keys ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the keys iterator
         */
        @Function(name = "keys", arity = 0)
        public static Object keys(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            return CreateArrayIterator(cx, o, ArrayIterationKind.Key);
        }

        /**
         * 22.1.3.29 Array.prototype.values ( )<br>
         * 22.1.3.30 Array.prototype [ @@iterator ] ( )
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
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            return CreateArrayIterator(cx, o, ArrayIterationKind.Value);
        }

        /**
         * 22.1.3.31 Array.prototype [ @@unscopables ]
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
            status &= CreateDataProperty(cx, blackList, "keys", true);
            status &= CreateDataProperty(cx, blackList, "values", true);
            /* step 9 */
            assert status;
            /* step 10 */
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
        public static Object fill(ExecutionContext cx, Object thisValue, Object value,
                Object start, Object end) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            /* steps 6-13 */
            return fill(cx, o, len, value, start, end);
        }

        /**
         * 22.1.3.6 Array.prototype.fill (value [ , start [ , end ] ] )
         * 
         * @param cx
         *            the execution context
         * @param o
         *            the script object
         * @param len
         *            the length value
         * @param value
         *            the fill value
         * @param start
         *            the start index
         * @param end
         *            the end index
         * @return this typed array object
         * @see ArrayPrototype.Properties#fill(ExecutionContext, Object, Object, Object, Object)
         * @see TypedArrayPrototypePrototype.Properties#fill(ExecutionContext, Object, Object,
         *      Object, Object)
         */
        public static ScriptObject fill(ExecutionContext cx, ScriptObject o, long len,
                Object value, Object start, Object end) {
            /* steps 1-5 (not applicable) */
            /* steps 6-7 */
            double relativeStart = ToInteger(cx, start);
            /* step 8 */
            long k;
            if (relativeStart < 0) {
                k = (long) Math.max((len + relativeStart), 0);
            } else {
                k = (long) Math.min(relativeStart, len);
            }
            /* steps 9-10 */
            double relativeEnd = Type.isUndefined(end) ? len : ToInteger(cx, end);
            /* step 11 */
            long finall;
            if (relativeEnd < 0) {
                finall = (long) Math.max((len + relativeEnd), 0);
            } else {
                finall = (long) Math.min(relativeEnd, len);
            }
            /* step 12 */
            for (; k < finall; ++k) {
                long pk = k;
                Put(cx, o, pk, value, true);
            }
            /* step 13 */
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
        public static Object copyWithin(ExecutionContext cx, Object thisValue, Object target,
                Object start, Object end) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToLength(cx, lenVal);
            /* steps 6-20 */
            return copyWithin(cx, o, len, target, start, end);
        }

        /**
         * 22.1.3.3 Array.prototype.copyWithin (target, start [ , end ] )
         * 
         * @param cx
         *            the execution context
         * @param o
         *            the script object
         * @param len
         *            the length value
         * @param target
         *            the target index
         * @param start
         *            the start index
         * @param end
         *            the end index
         * @return this typed array object
         * @see ArrayPrototype.Properties#copyWithin(ExecutionContext, Object, Object, Object,
         *      Object)
         * @see TypedArrayPrototypePrototype.Properties#copyWithin(ExecutionContext, Object, Object,
         *      Object, Object)
         */
        public static ScriptObject copyWithin(ExecutionContext cx, ScriptObject o, long len,
                Object target, Object start, Object end) {
            /* steps 1-5 (not applicable) */
            /* steps 6-7 */
            double relativeTarget = ToInteger(cx, target);
            /* step 8 */
            long to;
            if (relativeTarget < 0) {
                to = (long) Math.max((len + relativeTarget), 0);
            } else {
                to = (long) Math.min(relativeTarget, len);
            }
            /* steps 9-10 */
            double relativeStart = ToInteger(cx, start);
            /* step 11 */
            long from;
            if (relativeStart < 0) {
                from = (long) Math.max((len + relativeStart), 0);
            } else {
                from = (long) Math.min(relativeStart, len);
            }
            /* steps 12-13 */
            double relativeEnd = Type.isUndefined(end) ? len : ToInteger(cx, end);
            /* step 14 */
            long finall;
            if (relativeEnd < 0) {
                finall = (long) Math.max((len + relativeEnd), 0);
            } else {
                finall = (long) Math.min(relativeEnd, len);
            }
            /* step 15 */
            long count = Math.min(finall - from, len - to);
            /* steps 16-17 */
            long direction;
            if (from < to && to < from + count) {
                direction = -1;
                from = from + count - 1;
                to = to + count - 1;
            } else {
                direction = 1;
            }
            /* step 18 */
            for (; count > 0; --count) {
                long fromKey = from;
                long toKey = to;
                boolean fromPresent = HasProperty(cx, o, fromKey);
                if (fromPresent) {
                    Object fromVal = Get(cx, o, fromKey);
                    Put(cx, o, toKey, fromVal, true);
                } else {
                    DeletePropertyOrThrow(cx, o, toKey);
                }
                from += direction;
                to += direction;
            }
            /* step 19 */
            return o;
        }
    }

    /**
     * Proposed ECMAScript 7 additions
     */
    @CompatibilityExtension(CompatibilityOption.ArrayIncludes)
    public enum AdditionalProperties {
        ;

        /**
         * Array.prototype.includes ( searchElement [ , fromIndex ] )
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
        public static Object includes(ExecutionContext cx, Object thisValue, Object searchElement,
                Object fromIndex) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* steps 3-4 */
            long len = ToLength(cx, Get(cx, o, "length"));
            /* step 5 */
            if (len == 0) {
                return false;
            }
            /* steps 6-7 */
            long n = (long) ToInteger(cx, fromIndex);
            /* steps 8-9 */
            long k;
            if (n >= 0) {
                k = n;
            } else {
                k = len + n;
                if (k < 0) {
                    k = 0;
                }
            }
            /* step 10 */
            for (; k < len; ++k) {
                /* steps 10.a-b */
                Object element = Get(cx, o, k);
                /* step 10.c */
                if (SameValueZero(searchElement, element)) {
                    return true;
                }
            }
            /* step 11 */
            return false;
        }
    }
}

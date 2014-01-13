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
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.strictEqualityComparison;
import static com.github.anba.es6draft.runtime.objects.ArrayIteratorPrototype.CreateArrayIterator;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.AliasFunction;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Optional;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.ArrayIteratorPrototype.ArrayIterationKind;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayPrototypePrototype;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>22 Indexed Collections</h1><br>
 * <h2>22.1 Array Objects</h2>
 * <ul>
 * <li>22.1.3 Properties of the Array Prototype Object
 * <li>22.1.4 Properties of Array Instances
 * </ul>
 */
public class ArrayPrototype extends OrdinaryObject implements Initialisable {
    public ArrayPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    private static Realm getConstructorRealm(Constructor constructor) {
        if (constructor instanceof FunctionObject) {
            return ((FunctionObject) constructor).getRealm();
        } else if (constructor instanceof BuiltinFunction) {
            return ((BuiltinFunction) constructor).getRealm();
        } else {
            return null;
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
         * 22.1.3.26 Array.prototype.toLocaleString ( )
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
                Object nextElement = Get(cx, array, ToString(k));
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
         * 22.1.3.1 Array.prototype.concat ( [ item1 [ , item2 [ , ... ] ] ] )
         */
        @Function(name = "concat", arity = 1)
        public static Object concat(ExecutionContext cx, Object thisValue, Object... items) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            ScriptObject a = null;
            /* step 4 */
            if (o instanceof ExoticArray) {
                Object c = Get(cx, o, "constructor");
                if (IsConstructor(c) && getConstructorRealm((Constructor) c) == cx.getRealm()) {
                    a = ((Constructor) c).construct(cx, 0);
                }
            }
            /* steps 5-6 */
            if (a == null) {
                a = ArrayCreate(cx, 0);
            }
            /* step 7 */
            long n = 0;
            /* step 8 */
            Object[] allItems = new Object[items.length + 1];
            allItems[0] = o;
            System.arraycopy(items, 0, allItems, 1, items.length);
            /* step 9 */
            for (Object item : allItems) {
                boolean spreadable = IsConcatSpreadable(cx, item);
                if (spreadable) {
                    ScriptObject e = (ScriptObject) item;
                    Object lenVal = Get(cx, e, "length");
                    long len = ToLength(cx, lenVal);
                    for (long k = 0; k < len; ++k, ++n) {
                        String p = ToString(k);
                        boolean exists = HasProperty(cx, e, p);
                        if (exists) {
                            Object subElement = Get(cx, e, p);
                            CreateDataPropertyOrThrow(cx, a, ToString(n), subElement);
                        }
                    }
                } else {
                    CreateDataPropertyOrThrow(cx, a, ToString(n++), item);
                }
            }
            /* steps 10-11 */
            Put(cx, a, "length", n, true);
            /* step 12 */
            return a;
        }

        /**
         * 22.1.3.3.1 IsConcatSpreadable (O) Abstract Operation
         */
        public static boolean IsConcatSpreadable(ExecutionContext cx, Object o) {
            /* step 1 */
            if (!Type.isObject(o)) {
                return false;
            }
            /* steps 2-3 */
            Object spreadable = Get(cx, Type.objectValue(o), BuiltinSymbol.isConcatSpreadable.get());
            /* step 4 */
            if (!Type.isUndefined(spreadable)) {
                return ToBoolean(spreadable);
            }
            /* step 5 */
            if (o instanceof ExoticArray) {
                return true;
            }
            /* step 6 */
            return false;
        }

        /**
         * 22.1.3.12 Array.prototype.join (separator)
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
            Object element0 = Get(cx, o, "0");
            /* steps 10-11 */
            StringBuilder r = new StringBuilder();
            if (Type.isUndefinedOrNull(element0)) {
                r.append("");
            } else {
                r.append(ToString(cx, element0));
            }
            /* steps 12-13 */
            for (long k = 1; k < len; ++k) {
                Object element = Get(cx, o, ToString(k));
                if (Type.isUndefinedOrNull(element)) {
                    r.append(sep).append("");
                } else {
                    r.append(sep).append(ToString(cx, element));
                }
            }
            /* step 14 */
            return r.toString();
        }

        /**
         * 22.1.3.16 Array.prototype.pop ( )
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
                String index = ToString(newLen);
                Object element = Get(cx, o, index);
                DeletePropertyOrThrow(cx, o, index);
                Put(cx, o, "length", newLen, true);
                return element;
            }
        }

        /**
         * 22.1.3.17 Array.prototype.push ( [ item1 [ , item2 [ , ... ] ] ] )
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
                Put(cx, o, ToString(n), e, true);
                n += 1;
            }
            /* steps 8-9 */
            Put(cx, o, "length", n, true);
            /* step 10 */
            return n;
        }

        /**
         * 22.1.3.20 Array.prototype.reverse ( )
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
         * @see ArrayPrototype.Properties#reverse(ExecutionContext, Object)
         * @see TypedArrayPrototypePrototype.Properties#reverse(ExecutionContext, Object)
         */
        public static ScriptObject reverse(ExecutionContext cx, ScriptObject o, long len) {
            /* steps 1-5 (not applicable) */
            /* step 6 */
            long middle = len / 2L;
            /* steps 7-8 */
            for (long lower = 0; lower != middle; ++lower) {
                long upper = len - lower - 1;
                String upperP = ToString(upper);
                String lowerP = ToString(lower);
                Object lowerValue = Get(cx, o, lowerP);
                Object upperValue = Get(cx, o, upperP);
                boolean lowerExists = HasProperty(cx, o, lowerP);
                boolean upperExists = HasProperty(cx, o, upperP);
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
            /* step 9 */
            return o;
        }

        /**
         * 22.1.3.21 Array.prototype.shift ( )
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
            /* steps 7-8 */
            Object first = Get(cx, o, "0");
            /* steps 9-10 */
            for (long k = 1; k < len; ++k) {
                String from = ToString(k);
                String to = ToString(k - 1);
                boolean fromPresent = HasProperty(cx, o, from);
                if (fromPresent) {
                    Object fromVal = Get(cx, o, from);
                    Put(cx, o, to, fromVal, true);
                } else {
                    DeletePropertyOrThrow(cx, o, to);
                }
            }
            /* steps 11-12 */
            DeletePropertyOrThrow(cx, o, ToString(len - 1));
            /* steps 13-14 */
            Put(cx, o, "length", len - 1, true);
            /* step 15 */
            return first;
        }

        /**
         * 22.1.3.22 Array.prototype.slice (start, end)
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
            /* step 13 */
            ScriptObject a = null;
            /* step 14 */
            if (o instanceof ExoticArray) {
                Object c = Get(cx, o, "constructor");
                if (IsConstructor(c) && getConstructorRealm((Constructor) c) == cx.getRealm()) {
                    a = ((Constructor) c).construct(cx, count);
                }
            }
            /* steps 15-16 */
            if (a == null) {
                a = ArrayCreate(cx, count);
            }
            /* steps 17-18 */
            long n = 0;
            for (; k < finall; ++k, ++n) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    CreateDataPropertyOrThrow(cx, a, ToString(n), kvalue);
                }
            }
            /* steps 19-20 */
            Put(cx, a, "length", n, true);
            /* step 21 */
            return a;
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
                double c = ToInteger(cx, comparefn.call(cx, UNDEFINED, o1, o2));
                return (c == 0 ? 0 : c < 0 ? -1 : 1);
            }
        }

        /**
         * 22.1.3.24 Array.prototype.sort (comparefn)
         */
        @Function(name = "sort", arity = 1)
        public static Object sort(ExecutionContext cx, Object thisValue, Object comparefn) {
            /* step 1 */
            ScriptObject obj = ToObject(cx, thisValue);
            /* step 2 */
            Object lenValue = Get(cx, obj, "length");
            /* steps 3-4 */
            long len = ToLength(cx, lenValue);

            // handle OOM early
            if (len > Integer.MAX_VALUE) {
                throw newInternalError(cx, Messages.Key.OutOfMemory);
            }

            // collect elements
            int length = (int) len;
            int emptyCount = 0;
            int undefCount = 0;
            List<Object> elements = new ArrayList<>(Math.min(length, 1024));
            for (int i = 0; i < length; ++i) {
                String index = ToString(i);
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
                    // `IllegalArgumentException: Comparison method violates its general contract!`
                    // just ignore this exception...
                }
            }

            // and finally set sorted elements
            for (int i = 0, offset = 0; i < count; ++i) {
                String p = ToString(offset + i);
                Put(cx, obj, p, elements.get(i), true);
            }
            for (int i = 0, offset = count; i < undefCount; ++i) {
                String p = ToString(offset + i);
                Put(cx, obj, p, UNDEFINED, true);
            }
            for (int i = 0, offset = count + undefCount; i < emptyCount; ++i) {
                DeletePropertyOrThrow(cx, obj, ToString(offset + i));
            }

            return obj;
        }

        /**
         * 22.1.3.25 Array.prototype.splice (start, deleteCount [ , item1 [ , item2 [ , ... ] ] ] )
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
            /* step 12 */
            ScriptObject a = null;
            /* step 13 */
            if (o instanceof ExoticArray) {
                Object c = Get(cx, o, "constructor");
                if (IsConstructor(c) && getConstructorRealm((Constructor) c) == cx.getRealm()) {
                    a = ((Constructor) c).construct(cx, actualDeleteCount);
                }
            }
            /* steps 14-15 */
            if (a == null) {
                a = ArrayCreate(cx, actualDeleteCount);
            }
            /* steps 16-17 */
            for (long k = 0; k < actualDeleteCount; ++k) {
                String from = ToString(actualStart + k);
                boolean fromPresent = HasProperty(cx, o, from);
                if (fromPresent) {
                    Object fromValue = Get(cx, o, from);
                    CreateDataPropertyOrThrow(cx, a, ToString(k), fromValue);
                }
            }
            /* steps 18-19 */
            Put(cx, a, "length", actualDeleteCount, true);
            /* steps 20-21 */
            int itemCount = items.length;
            if (itemCount < actualDeleteCount) {
                /* step 22 */
                for (long k = actualStart; k < (len - actualDeleteCount); ++k) {
                    String from = ToString(k + actualDeleteCount);
                    String to = ToString(k + itemCount);
                    boolean fromPresent = HasProperty(cx, o, from);
                    if (fromPresent) {
                        Object fromValue = Get(cx, o, from);
                        Put(cx, o, to, fromValue, true);
                    } else {
                        DeletePropertyOrThrow(cx, o, to);
                    }
                }
                for (long k = len; k > (len - actualDeleteCount + itemCount); --k) {
                    DeletePropertyOrThrow(cx, o, ToString(k - 1));
                }
            } else if (itemCount > actualDeleteCount) {
                /* step 23 */
                for (long k = (len - actualDeleteCount); k > actualStart; --k) {
                    String from = ToString(k + actualDeleteCount - 1);
                    String to = ToString(k + itemCount - 1);
                    boolean fromPresent = HasProperty(cx, o, from);
                    if (fromPresent) {
                        Object fromValue = Get(cx, o, from);
                        Put(cx, o, to, fromValue, true);
                    } else {
                        DeletePropertyOrThrow(cx, o, to);
                    }
                }
            }
            /* step 24 */
            long k = actualStart;
            /* step 25 */
            for (int i = 0; i < itemCount; ++k, ++i) {
                Object e = items[i];
                Put(cx, o, ToString(k), e, true);
            }
            /* steps 26-27 */
            Put(cx, o, "length", len - actualDeleteCount + itemCount, true);
            /* step 28 */
            return a;
        }

        /**
         * 22.1.3.28 Array.prototype.unshift ( [ item1 [ , item2 [ , ... ] ] ] )
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
            /* steps 7-8 */
            for (long k = len; k > 0; --k) {
                String from = ToString(k - 1);
                String to = ToString(k + argCount - 1);
                boolean fromPresent = HasProperty(cx, o, from);
                if (fromPresent) {
                    Object fromValue = Get(cx, o, from);
                    Put(cx, o, to, fromValue, true);
                } else {
                    DeletePropertyOrThrow(cx, o, to);
                }
            }
            /* steps 9-11 */
            for (int j = 0; j < items.length; ++j) {
                Object e = items[j];
                Put(cx, o, ToString(j), e, true);
            }
            /* steps 12-13 */
            Put(cx, o, "length", len + argCount, true);
            /* step 14 */
            return len + argCount;
        }

        /**
         * 22.1.3.11 Array.prototype.indexOf ( searchElement [ , fromIndex ] )
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
         * @see ArrayPrototype.Properties#indexOf(ExecutionContext, Object, Object, Object)
         * @see TypedArrayPrototypePrototype.Properties#indexOf(ExecutionContext, Object, Object,
         *      Object)
         */
        public static Object indexOf(ExecutionContext cx, ScriptObject o, long len,
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
                String pk = ToString(k);
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object elementk = Get(cx, o, pk);
                    boolean same = strictEqualityComparison(searchElement, elementk);
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
                String pk = ToString(k);
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object elementk = Get(cx, o, pk);
                    boolean same = strictEqualityComparison(searchElement, elementk);
                    if (same) {
                        return k;
                    }
                }
            }
            /* step 12 */
            return -1;
        }

        /**
         * 22.1.3.5 Array.prototype.every ( callbackfn, thisArg = undefined )
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
         * 22.1.3.5 Array.prototype.every ( callbackfn, thisArg = undefined )
         * 
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
                String pk = ToString(k);
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
         * 22.1.3.23 Array.prototype.some ( callbackfn, thisArg = undefined )
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
         * 22.1.3.23 Array.prototype.some ( callbackfn, thisArg = undefined )
         * 
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
                String pk = ToString(k);
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
         * 22.1.3.10 Array.prototype.forEach ( callbackfn, thisArg = undefined )
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
         * 22.1.3.10 Array.prototype.forEach ( callbackfn, thisArg = undefined )
         * 
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
                String pk = ToString(k);
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
         * 22.1.3.15 Array.prototype.map ( callbackfn, thisArg = undefined )
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
            /* step 8 */
            ScriptObject a = null;
            /* step 9 */
            if (o instanceof ExoticArray) {
                Object c = Get(cx, o, "constructor");
                if (IsConstructor(c) && getConstructorRealm((Constructor) c) == cx.getRealm()) {
                    a = ((Constructor) c).construct(cx, len);
                }
            }
            /* steps 10-11 */
            if (a == null) {
                a = ArrayCreate(cx, len);
            }
            /* steps 12-13 */
            for (long k = 0; k < len; ++k) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    Object mappedValue = callback.call(cx, thisArg, kvalue, k, o);
                    CreateDataPropertyOrThrow(cx, a, pk, mappedValue);
                }
            }
            /* step 14 */
            return a;
        }

        /**
         * 22.1.3.7 Array.prototype.filter ( callbackfn, thisArg = undefined )
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
            /* step 8 */
            ScriptObject a = null;
            /* step 9 */
            if (o instanceof ExoticArray) {
                Object c = Get(cx, o, "constructor");
                if (IsConstructor(c) && getConstructorRealm((Constructor) c) == cx.getRealm()) {
                    a = ((Constructor) c).construct(cx, 0);
                }
            }
            /* steps 10-11 */
            if (a == null) {
                a = ArrayCreate(cx, 0);
            }
            /* steps 12-14 */
            for (long k = 0, to = 0; k < len; ++k) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    Object selected = callback.call(cx, thisArg, kvalue, k, o);
                    if (ToBoolean(selected)) {
                        CreateDataPropertyOrThrow(cx, a, ToString(to), kvalue);
                        to += 1;
                    }
                }
            }
            /* step 15 */
            return a;
        }

        /**
         * 22.1.3.18 Array.prototype.reduce ( callbackfn [ , initialValue ] )
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
                    String pk = ToString(k);
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
                String pk = ToString(k);
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
                    String pk = ToString(k);
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
                String pk = ToString(k);
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
         * 22.1.3.8 Array.prototype.find ( predicate, thisArg = undefined )
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
         * 22.1.3.8 Array.prototype.find ( predicate, thisArg = undefined )
         * 
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
                String pk = ToString(k);
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    Object testResult = pred.call(cx, thisArg, kvalue, k, o);
                    if (ToBoolean(testResult)) {
                        return kvalue;
                    }
                }
            }
            /* step 10 */
            return UNDEFINED;
        }

        /**
         * 22.1.3.9 Array.prototype.findIndex ( predicate [ , thisArg ] )
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
                String pk = ToString(k);
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    Object testResult = pred.call(cx, thisArg, kvalue, k, o);
                    if (ToBoolean(testResult)) {
                        return k;
                    }
                }
            }
            /* step 10 */
            return -1;
        }

        /**
         * 22.1.3.4 Array.prototype.entries ( )
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
         */
        @Function(name = "values", arity = 0)
        @AliasFunction(name = "[Symbol.iterator]", symbol = BuiltinSymbol.iterator)
        public static Object values(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            return CreateArrayIterator(cx, o, ArrayIterationKind.Value);
        }

        /**
         * 22.1.3.31 Array.prototype [ @@unscopables ]
         */
        @Value(name = "[Symbol.unscopables]", symbol = BuiltinSymbol.unscopables,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object unscopables(ExecutionContext cx) {
            /* step 1 */
            ScriptObject blackList = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            /* steps 2-8 */
            boolean status = true;
            status &= CreateDataProperty(cx, blackList, "find", true);
            status &= CreateDataProperty(cx, blackList, "findIndex", true);
            status &= CreateDataProperty(cx, blackList, "fill", true);
            status &= CreateDataProperty(cx, blackList, "copyWithin", true);
            status &= CreateDataProperty(cx, blackList, "entries", true);
            status &= CreateDataProperty(cx, blackList, "keys", true);
            status &= CreateDataProperty(cx, blackList, "values", true);
            /* step 9 */
            assert status;
            /* step 10 */
            return blackList;
        }

        /**
         * 22.1.3.6 Array.prototype.fill (value, start = 0, end = this.length)
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
         * 22.1.3.6 Array.prototype.fill (value, start = 0, end = this.length)
         * 
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
                String pk = ToString(k);
                Put(cx, o, pk, value, true);
            }
            /* step 13 */
            return o;
        }

        /**
         * 22.1.3.3 Array.prototype.copyWithin (target, start, end = this.length)
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
         * 22.1.3.3 Array.prototype.copyWithin (target, start, end = this.length)
         * 
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
                String fromKey = ToString(from);
                String toKey = ToString(to);
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
}

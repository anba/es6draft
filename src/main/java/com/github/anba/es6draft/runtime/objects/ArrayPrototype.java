/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
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
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Optional;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.ArrayIteratorPrototype.ArrayIterationKind;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.4 Array Objects</h2>
 * <ul>
 * <li>15.4.3 Properties of the Array Prototype Object
 * <li>15.4.4 Properties of Array Instances
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

    /**
     * 15.4.3 Properties of the Array Prototype Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.4.3.1 Array.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Array;

        /**
         * 15.4.3.2 Array.prototype.toString ( )
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
         * 15.4.3.3 Array.prototype.toLocaleString ( )
         */
        @Function(name = "toLocaleString", arity = 0)
        public static Object toLocaleString(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            ScriptObject array = ToObject(cx, thisValue);
            /* step 2 */
            Object arrayLen = Get(cx, array, "length");
            /* step 3 */
            long len = ToUint32(cx, arrayLen);
            /* step 4 */
            String separator = cx.getRealm().getListSeparator();
            /* step 5 */
            if (len == 0) {
                return "";
            }
            /* step 8, 10 (step 9 not applicable) */
            Object firstElement = Get(cx, array, "0");
            /* step 11-12 */
            StringBuilder r = new StringBuilder();
            if (Type.isUndefinedOrNull(firstElement)) {
                r.append("");
            } else {
                r.append(ToString(cx, Invoke(cx, firstElement, "toLocaleString")));
            }
            /* step 13-14 */
            for (long k = 1; k < len; ++k) {
                Object nextElement = Get(cx, array, ToString(k));
                if (Type.isUndefinedOrNull(nextElement)) {
                    r.append(separator).append("");
                } else {
                    r.append(separator).append(
                            ToString(cx, Invoke(cx, nextElement, "toLocaleString")));
                }
            }
            /* step 15 */
            return r.toString();
        }

        /**
         * 15.4.3.4 Array.prototype.concat ( [ item1 [ , item2 [ , ... ] ] ] )
         */
        @Function(name = "concat", arity = 1)
        public static Object concat(ExecutionContext cx, Object thisValue, Object... items) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            ScriptObject a = null;
            /* step 3 */
            if (o instanceof ExoticArray) {
                Object c = Get(cx, o, "constructor");
                if (IsConstructor(c)) {
                    ScriptObject newObj = ((Constructor) c).construct(cx, 0);
                    a = ToObject(cx, newObj);
                }
            }
            /* steps 4-5 */
            if (a == null) {
                a = ArrayCreate(cx, 0);
            }
            /* step 6 */
            long n = 0;
            /* step 7 */
            Object[] allItems = new Object[items.length + 1];
            allItems[0] = o;
            System.arraycopy(items, 0, allItems, 1, items.length);
            /* step 8 */
            for (Object item : allItems) {
                boolean spreadable = IsConcatSpreadable(cx, item);
                if (spreadable) {
                    ScriptObject e = (ScriptObject) item;
                    long len = ToUint32(cx, Get(cx, e, "length"));
                    for (long k = 0; k < len; ++k, ++n) {
                        String p = ToString(k);
                        boolean exists = HasProperty(cx, e, p);
                        if (exists) {
                            Object subElement = Get(cx, e, p);
                            a.defineOwnProperty(cx, ToString(n), new PropertyDescriptor(subElement,
                                    true, true, true));
                        }
                    }
                } else {
                    a.defineOwnProperty(cx, ToString(n++), new PropertyDescriptor(item, true, true,
                            true));
                }
            }
            /* steps 9-10 */
            Put(cx, a, "length", n, true);
            /* step 11 */
            return a;
        }

        /**
         * 15.4.3.4.1 IsConcatSpreadable (O) Abstract Operation
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
         * 15.4.3.5 Array.prototype.join (separator)
         */
        @Function(name = "join", arity = 1)
        public static Object join(ExecutionContext cx, Object thisValue, Object separator) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToUint32(cx, lenVal);
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
         * 15.4.3.6 Array.prototype.pop ( )
         */
        @Function(name = "pop", arity = 0)
        public static Object pop(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToUint32(cx, lenVal);
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
         * 15.4.3.7 Array.prototype.push ( [ item1 [ , item2 [ , ... ] ] ] )
         */
        @Function(name = "push", arity = 1)
        public static Object push(ExecutionContext cx, Object thisValue, Object... items) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long n = ToUint32(cx, lenVal);
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
         * 15.4.3.8 Array.prototype.reverse ( )
         */
        @Function(name = "reverse", arity = 0)
        public static Object reverse(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* step 4-5 */
            long len = ToUint32(cx, lenVal);
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
         * 15.4.3.9 Array.prototype.shift ( )
         */
        @Function(name = "shift", arity = 0)
        public static Object shift(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* step 4-5 */
            long len = ToUint32(cx, lenVal);
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
         * 15.4.3.10 Array.prototype.slice (start, end)
         */
        @Function(name = "slice", arity = 2)
        public static Object slice(ExecutionContext cx, Object thisValue, Object start, Object end) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            ScriptObject a = ArrayCreate(cx, 0);
            /* step 4 */
            Object lenVal = Get(cx, o, "length");
            /* step 5-6 */
            long len = ToUint32(cx, lenVal);
            /* step 7-8 */
            double relativeStart = ToInteger(cx, start);
            /* step 9 */
            long k;
            if (relativeStart < 0) {
                k = (long) Math.max(len + relativeStart, 0);
            } else {
                k = (long) Math.min(relativeStart, len);
            }
            /* steps 10-11 */
            double relativeEnd;
            if (Type.isUndefined(end)) {
                relativeEnd = len;
            } else {
                relativeEnd = ToInteger(cx, end);
            }
            /* step 12 */
            long finall;
            if (relativeEnd < 0) {
                finall = (long) Math.max(len + relativeEnd, 0);
            } else {
                finall = (long) Math.min(relativeEnd, len);
            }
            /* steps 13-14 */
            long n = 0;
            for (; k < finall; ++k, ++n) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    String p = ToString(n);
                    boolean status = CreateOwnDataProperty(cx, a, p, kvalue);
                    if (!status) {
                        throw throwTypeError(cx, Messages.Key.PropertyNotCreatable, p);
                    }
                }
            }
            /* steps 15-16 */
            Put(cx, a, "length", n, true);
            /* step 17 */
            return a;
        }

        private static class DefaultComparator implements Comparator<Object> {
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

        private static class FunctionComparator implements Comparator<Object> {
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
         * 15.4.3.11 Array.prototype.sort (comparefn)
         */
        @Function(name = "sort", arity = 1)
        public static Object sort(ExecutionContext cx, Object thisValue, Object comparefn) {
            ScriptObject obj = ToObject(cx, thisValue);
            long len = ToUint32(cx, Get(cx, obj, "length"));

            int emptyCount = 0;
            int undefCount = 0;
            List<Object> elements = new ArrayList<>((int) Math.min(len, 1024));
            for (int i = 0; i < len; ++i) {
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

            int count = elements.size();
            if (count > 1) {
                Comparator<Object> comparator;
                if (!Type.isUndefined(comparefn)) {
                    if (!IsCallable(comparefn)) {
                        throw throwTypeError(cx, Messages.Key.NotCallable);
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
         * 15.4.3.12 Array.prototype.splice (start, deleteCount [ , item1 [ , item2 [ , ... ] ] ] )
         */
        @Function(name = "splice", arity = 2)
        public static Object splice(ExecutionContext cx, Object thisValue,
                @Optional(Optional.Default.NONE) Object start,
                @Optional(Optional.Default.NONE) Object deleteCount, Object... items) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            ScriptObject a = ArrayCreate(cx, 0);
            /* step 4 */
            Object lenVal = Get(cx, o, "length");
            /* steps 5-6 */
            long len = ToUint32(cx, lenVal);
            /* steps 7-8 */
            double relativeStart = (start != null ? ToInteger(cx, start) : 0);
            /* step 9 */
            long actualStart;
            if (relativeStart < 0) {
                actualStart = (long) Math.max(len + relativeStart, 0);
            } else {
                actualStart = (long) Math.min(relativeStart, len);
            }
            /* step 10 */
            // TODO: track spec, https://bugs.ecmascript.org/show_bug.cgi?id=429
            long actualDeleteCount;
            if (start != null && deleteCount == null) {
                actualDeleteCount = (len - actualStart);
            } else {
                double del = (deleteCount != null ? Math.max(ToInteger(cx, deleteCount), 0) : 0);
                actualDeleteCount = (long) Math.min(del, len - actualStart);
            }
            /* steps 11-12 */
            for (long k = 0; k < actualDeleteCount; ++k) {
                String from = ToString(actualStart + k);
                boolean fromPresent = HasProperty(cx, o, from);
                if (fromPresent) {
                    Object fromValue = Get(cx, o, from);
                    a.defineOwnProperty(cx, ToString(k), new PropertyDescriptor(fromValue, true,
                            true, true));
                }
            }
            /* steps 13-14 */
            Put(cx, a, "length", actualDeleteCount, true);

            /* steps 15-16 */
            int itemCount = items.length;
            if (itemCount < actualDeleteCount) {
                /* step 17 */
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
                /* step 18 */
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
            /* step 19 */
            long k = actualStart;
            /* step 20 */
            for (int i = 0; i < itemCount; ++k, ++i) {
                Object e = items[i];
                Put(cx, o, ToString(k), e, true);
            }
            /* steps 21-22 */
            Put(cx, o, "length", len - actualDeleteCount + itemCount, true);
            /* step 23 */
            return a;
        }

        /**
         * 15.4.3.13 Array.prototype.unshift ( [ item1 [ , item2 [ , ... ] ] ] )
         */
        @Function(name = "unshift", arity = 1)
        public static Object unshift(ExecutionContext cx, Object thisValue, Object... items) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToUint32(cx, lenVal);
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
         * 15.4.3.14 Array.prototype.indexOf ( searchElement [ , fromIndex ] )
         */
        @Function(name = "indexOf", arity = 1)
        public static Object indexOf(ExecutionContext cx, Object thisValue, Object searchElement,
                @Optional(Optional.Default.NONE) Object fromIndex) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToUint32(cx, lenVal);
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
                k = (long) (len - Math.abs(n));
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
         * 15.4.3.15 Array.prototype.lastIndexOf ( searchElement [ , fromIndex ] )
         */
        @Function(name = "lastIndexOf", arity = 1)
        public static Object lastIndexOf(ExecutionContext cx, Object thisValue,
                Object searchElement, @Optional(Optional.Default.NONE) Object fromIndex) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToUint32(cx, lenVal);
            /* step 6 */
            if (len == 0) {
                return -1;
            }
            /* steps 7-8 */
            long n;
            if (fromIndex != null) {
                n = (long) ToInteger(cx, fromIndex);
            } else {
                n = (long) (len - 1);
            }
            /* steps 9-10 */
            long k;
            if (n >= 0) {
                k = (long) Math.min(n, len - 1);
            } else {
                k = (long) (len - Math.abs(n));
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
         * 15.4.3.16 Array.prototype.every ( callbackfn [ , thisArg ] )
         */
        @Function(name = "every", arity = 1)
        public static Object every(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToUint32(cx, lenVal);
            /* step 6 */
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
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
         * 15.4.3.17 Array.prototype.some ( callbackfn [ , thisArg ] )
         */
        @Function(name = "some", arity = 1)
        public static Object some(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToUint32(cx, lenVal);
            /* step 6 */
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
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
         * 15.4.3.18 Array.prototype.forEach ( callbackfn [ , thisArg ] )
         */
        @Function(name = "forEach", arity = 1)
        public static Object forEach(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToUint32(cx, lenVal);
            /* step 6 */
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
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
         * 15.4.3.19 Array.prototype.map ( callbackfn [ , thisArg ] )
         */
        @Function(name = "map", arity = 1)
        public static Object map(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToUint32(cx, lenVal);
            /* step 6 */
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 7 (omitted) */
            /* step 8 */
            ScriptObject a = ArrayCreate(cx, len);
            /* steps 9-10 */
            for (long k = 0; k < len; ++k) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    Object mappedValue = callback.call(cx, thisArg, kvalue, k, o);
                    a.defineOwnProperty(cx, pk, new PropertyDescriptor(mappedValue, true, true,
                            true));
                }
            }
            /* step 11 */
            return a;
        }

        /**
         * 15.4.3.20 Array.prototype.filter ( callbackfn [ , thisArg ] )
         */
        @Function(name = "filter", arity = 1)
        public static Object filter(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToUint32(cx, lenVal);
            /* step 6 */
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 7 (omitted) */
            /* step 8 */
            ScriptObject a = ArrayCreate(cx, 0);
            /* steps 9-11 */
            for (long k = 0, to = 0; k < len; ++k) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    Object selected = callback.call(cx, thisArg, kvalue, k, o);
                    if (ToBoolean(selected)) {
                        a.defineOwnProperty(cx, ToString(to), new PropertyDescriptor(kvalue, true,
                                true, true));
                        to += 1;
                    }
                }
            }
            /* step 12 */
            return a;
        }

        /**
         * 15.4.3.21 Array.prototype.reduce ( callbackfn [ , initialValue ] )
         */
        @Function(name = "reduce", arity = 1)
        public static Object reduce(ExecutionContext cx, Object thisValue, Object callbackfn,
                @Optional(Optional.Default.NONE) Object initialValue) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToUint32(cx, lenVal);
            /* step 6 */
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 7 */
            if (len == 0 && initialValue == null) {
                throw throwTypeError(cx, Messages.Key.ReduceInitialValue);
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
                    throw throwTypeError(cx, Messages.Key.ReduceInitialValue);
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
         * 15.4.3.22 Array.prototype.reduceRight ( callbackfn [ , initialValue ] )
         */
        @Function(name = "reduceRight", arity = 1)
        public static Object reduceRight(ExecutionContext cx, Object thisValue, Object callbackfn,
                @Optional(Optional.Default.NONE) Object initialValue) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToUint32(cx, lenVal);
            /* step 6 */
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 7 */
            if (len == 0 && initialValue == null) {
                throw throwTypeError(cx, Messages.Key.ReduceInitialValue);
            }
            /* step 8 */
            long k = (len - 1);
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
                    throw throwTypeError(cx, Messages.Key.ReduceInitialValue);
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
         * 15.4.3.23 Array.prototype.find ( predicate, thisArg = undefined )
         */
        @Function(name = "find", arity = 1)
        public static Object find(ExecutionContext cx, Object thisValue, Object predicate,
                Object thisArg) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToUint32(cx, lenVal);
            /* step 6 */
            if (!IsCallable(predicate)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
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
         * 15.4.3.24 Array.prototype.findIndex ( predicate [ , thisArg ] )
         */
        @Function(name = "findIndex", arity = 1)
        public static Object findIndex(ExecutionContext cx, Object thisValue, Object predicate,
                Object thisArg) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Object lenVal = Get(cx, o, "length");
            /* steps 4-5 */
            long len = ToUint32(cx, lenVal);
            /* step 6 */
            if (!IsCallable(predicate)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
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
         * 15.4.3.25 Array.prototype.entries ( )
         */
        @Function(name = "entries", arity = 0)
        public static Object entries(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            return CreateArrayIterator(cx, o, ArrayIterationKind.KeyValue);
        }

        /**
         * 15.4.3.26 Array.prototype.keys ( )
         */
        @Function(name = "keys", arity = 0)
        public static Object keys(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            return CreateArrayIterator(cx, o, ArrayIterationKind.Key);
        }

        /**
         * 15.4.3.27 Array.prototype.values ( )<br>
         * 15.4.3.28 Array.prototype [ @@iterator ] ( )
         */
        @Function(name = "values", arity = 0)
        @AliasFunction(name = "@@iterator", symbol = BuiltinSymbol.iterator)
        public static Object values(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            return CreateArrayIterator(cx, o, ArrayIterationKind.Value);
        }
    }
}

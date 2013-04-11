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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Optional;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.ArrayIteratorPrototype.ArrayIterationKind;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
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
 * <li>15.4.4 Properties of the Array Prototype Object
 * <li>15.4.5 Properties of Array Instances
 * </ul>
 */
public class ArrayPrototype extends OrdinaryObject implements Initialisable {
    public ArrayPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);

        // 15.4.4.26 Array.prototype.@@iterator ( )
        defineOwnProperty(cx, BuiltinSymbol.iterator.get(),
                new PropertyDescriptor(Get(cx, this, "values"), true, false, true));
    }

    /**
     * 15.4.4 Properties of the Array Prototype Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.4.4.1 Array.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Array;

        /**
         * 15.4.4.2 Array.prototype.toString ( )
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            ScriptObject array = ToObject(cx, thisValue);
            Object func = Get(cx, array, "join");
            if (!IsCallable(func)) {
                func = cx.getIntrinsic(Intrinsics.ObjProto_toString);
            }
            return ((Callable) func).call(cx, array);
        }

        /**
         * 15.4.4.3 Array.prototype.toLocaleString ( )
         */
        @Function(name = "toLocaleString", arity = 0)
        public static Object toLocaleString(ExecutionContext cx, Object thisValue) {
            ScriptObject array = ToObject(cx, thisValue);
            Object arrayLen = Get(cx, array, "length");
            long len = ToUint32(cx, arrayLen);
            String separator = ",";
            if (len == 0) {
                return "";
            }
            StringBuilder r = new StringBuilder();
            Object firstElement = Get(cx, array, "0");
            if (Type.isUndefinedOrNull(firstElement)) {
                r.append("");
            } else {
                r.append(ToString(cx, Invoke(cx, firstElement, "toLocaleString")));
            }
            for (long k = 1; k < len; ++k) {
                Object nextElement = Get(cx, array, ToString(k));
                if (Type.isUndefinedOrNull(nextElement)) {
                    r.append(separator).append("");
                } else {
                    r.append(separator).append(
                            ToString(cx, Invoke(cx, nextElement, "toLocaleString")));
                }
            }
            return r.toString();
        }

        /**
         * 15.4.4.4 Array.prototype.concat ( [ item1 [ , item2 [ , ... ] ] ] )
         */
        @Function(name = "concat", arity = 1)
        public static Object concat(ExecutionContext cx, Object thisValue, Object... items) {
            ScriptObject o = ToObject(cx, thisValue);
            ScriptObject a = ArrayCreate(cx, 0);
            long n = 0;
            int itemsLength = items.length;
            items = Arrays.copyOf(items, itemsLength + 1, Object[].class);
            System.arraycopy(items, 0, items, 1, itemsLength);
            items[0] = o;
            for (Object item : items) {
                if (item instanceof ExoticArray) {
                    assert item instanceof ScriptObject;
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
            Put(cx, a, "length", n, true);
            return a;
        }

        /**
         * 15.4.4.5 Array.prototype.join (separator)
         */
        @Function(name = "join", arity = 1)
        public static Object join(ExecutionContext cx, Object thisValue, Object separator) {
            ScriptObject o = ToObject(cx, thisValue);
            Object lenVal = Get(cx, o, "length");
            long len = ToUint32(cx, lenVal);
            if (Type.isUndefined(separator)) {
                separator = ",";
            }
            String sep = ToFlatString(cx, separator);
            if (len == 0) {
                return "";
            }
            StringBuilder r = new StringBuilder();
            Object element0 = Get(cx, o, "0");
            if (Type.isUndefinedOrNull(element0)) {
                r.append("");
            } else {
                r.append(ToString(cx, element0));
            }
            for (long k = 1; k < len; ++k) {
                Object element = Get(cx, o, ToString(k));
                if (Type.isUndefinedOrNull(element)) {
                    r.append(sep).append("");
                } else {
                    r.append(sep).append(ToString(cx, element));
                }
            }
            return r.toString();
        }

        /**
         * 15.4.4.6 Array.prototype.pop ( )
         */
        @Function(name = "pop", arity = 0)
        public static Object pop(ExecutionContext cx, Object thisValue) {
            ScriptObject o = ToObject(cx, thisValue);
            Object lenVal = Get(cx, o, "length");
            long len = ToUint32(cx, lenVal);
            if (len == 0) {
                Put(cx, o, "length", 0, true);
                return UNDEFINED;
            } else {
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
         * 15.4.4.7 Array.prototype.push ( [ item1 [ , item2 [ , ... ] ] ] )
         */
        @Function(name = "push", arity = 1)
        public static Object push(ExecutionContext cx, Object thisValue, Object... items) {
            ScriptObject o = ToObject(cx, thisValue);
            Object lenVal = Get(cx, o, "length");
            long n = ToUint32(cx, lenVal);
            for (Object e : items) {
                Put(cx, o, ToString(n), e, true);
                n += 1;
            }
            Put(cx, o, "length", n, true);
            return n;
        }

        /**
         * 15.4.4.8 Array.prototype.reverse ( )
         */
        @Function(name = "reverse", arity = 0)
        public static Object reverse(ExecutionContext cx, Object thisValue) {
            ScriptObject o = ToObject(cx, thisValue);
            Object lenVal = Get(cx, o, "length");
            long len = ToUint32(cx, lenVal);
            long middle = len / 2L;
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
            return o;
        }

        /**
         * 15.4.4.9 Array.prototype.shift ( )
         */
        @Function(name = "shift", arity = 0)
        public static Object shift(ExecutionContext cx, Object thisValue) {
            ScriptObject o = ToObject(cx, thisValue);
            Object lenVal = Get(cx, o, "length");
            long len = ToUint32(cx, lenVal);
            if (len == 0) {
                Put(cx, o, "length", 0, true);
                return UNDEFINED;
            }
            Object first = Get(cx, o, "0");
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
            DeletePropertyOrThrow(cx, o, ToString(len - 1));
            Put(cx, o, "length", len - 1, true);
            return first;
        }

        /**
         * 15.4.4.10 Array.prototype.slice (start, end)
         */
        @Function(name = "slice", arity = 2)
        public static Object slice(ExecutionContext cx, Object thisValue, Object start, Object end) {
            ScriptObject o = ToObject(cx, thisValue);
            ScriptObject a = ArrayCreate(cx, 0);
            Object lenVal = Get(cx, o, "length");
            long len = ToUint32(cx, lenVal);
            double relativeStart = ToInteger(cx, start);
            long k;
            if (relativeStart < 0) {
                k = (long) Math.max(len + relativeStart, 0);
            } else {
                k = (long) Math.min(relativeStart, len);
            }
            double relativeEnd;
            if (Type.isUndefined(end)) {
                relativeEnd = len;
            } else {
                relativeEnd = ToInteger(cx, end);
            }
            long finall;
            if (relativeEnd < 0) {
                finall = (long) Math.max(len + relativeEnd, 0);
            } else {
                finall = (long) Math.min(relativeEnd, len);
            }
            long n = 0;
            for (; k < finall; ++k, ++n) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    String p = ToString(n);
                    boolean status = CreateOwnDataProperty(cx, a, p, kvalue);
                    if (!status) {
                        // FIXME: spec bug? (Assert instead of throw TypeError?) (Bug 1402)
                        throw throwTypeError(cx, Messages.Key.PropertyNotCreatable, p);
                    }
                }
            }
            // FIXME: spec bug (call to Put does make sense with the supplied args) (Bug 1402)
            Put(cx, a, "length", n, true);
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
         * 15.4.4.11 Array.prototype.sort (comparefn)
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
                Collections.sort(elements, comparator);
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
         * 15.4.4.12 Array.prototype.splice (start, deleteCount [ , item1 [ , item2 [ , ... ] ] ] )
         */
        @Function(name = "splice", arity = 2)
        public static Object splice(ExecutionContext cx, Object thisValue, Object start,
                Object deleteCount, Object... items) {
            ScriptObject o = ToObject(cx, thisValue);
            ScriptObject a = ArrayCreate(cx, 0);
            Object lenVal = Get(cx, o, "length");
            long len = ToUint32(cx, lenVal);
            double relativeStart = ToInteger(cx, start);
            long actualStart;
            if (relativeStart < 0) {
                actualStart = (long) Math.max(len + relativeStart, 0);
            } else {
                actualStart = (long) Math.min(relativeStart, len);
            }
            long actualDeleteCount = (long) Math.min(Math.max(ToInteger(cx, deleteCount), 0), len
                    - actualStart);
            for (long k = 0; k < actualDeleteCount; ++k) {
                String from = ToString(actualStart + k);
                boolean fromPresent = HasProperty(cx, o, from);
                if (fromPresent) {
                    Object fromValue = Get(cx, o, from);
                    a.defineOwnProperty(cx, ToString(k), new PropertyDescriptor(fromValue, true,
                            true, true));
                }
            }
            Put(cx, a, "length", actualDeleteCount, true);

            int itemCount = items.length;
            if (itemCount < actualDeleteCount) {
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
            long k = actualStart;
            for (int i = 0; i < itemCount; ++k, ++i) {
                Object e = items[i];
                Put(cx, o, ToString(k), e, true);
            }
            Put(cx, o, "length", len - actualDeleteCount + itemCount, true);
            return a;
        }

        /**
         * 15.4.4.13 Array.prototype.unshift ( [ item1 [ , item2 [ , ... ] ] ] )
         */
        @Function(name = "unshift", arity = 1)
        public static Object unshift(ExecutionContext cx, Object thisValue, Object... items) {
            ScriptObject o = ToObject(cx, thisValue);
            Object lenVal = Get(cx, o, "length");
            long len = ToUint32(cx, lenVal);
            int argCount = items.length;
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
            for (int j = 0; j < items.length; ++j) {
                Object e = items[j];
                Put(cx, o, ToString(j), e, true);
            }
            Put(cx, o, "length", len + argCount, true);
            return len + argCount;
        }

        /**
         * 15.4.4.14 Array.prototype.indexOf ( searchElement [ , fromIndex ] )
         */
        @Function(name = "indexOf", arity = 1)
        public static Object indexOf(ExecutionContext cx, Object thisValue, Object searchElement,
                @Optional(Optional.Default.NONE) Object fromIndex) {
            ScriptObject o = ToObject(cx, thisValue);
            Object lenVal = Get(cx, o, "length");
            long len = ToUint32(cx, lenVal);
            if (len == 0) {
                return -1;
            }
            long n;
            if (fromIndex != null) {
                n = (long) ToInteger(cx, fromIndex);
            } else {
                n = 0;
            }
            if (n >= len) {
                return -1;
            }
            long k;
            if (n >= 0) {
                k = n;
            } else {
                k = (long) (len - Math.abs(n));
                if (k < 0) {
                    k = 0;
                }
            }
            for (; k < len; ++k) {
                boolean kpresent = HasProperty(cx, o, ToString(k));
                if (kpresent) {
                    Object elementk = Get(cx, o, ToString(k));
                    boolean same = strictEqualityComparison(searchElement, elementk);
                    if (same) {
                        return k;
                    }
                }
            }
            return -1;
        }

        /**
         * 15.4.4.15 Array.prototype.lastIndexOf ( searchElement [ , fromIndex ] )
         */
        @Function(name = "lastIndexOf", arity = 1)
        public static Object lastIndexOf(ExecutionContext cx, Object thisValue,
                Object searchElement, @Optional(Optional.Default.NONE) Object fromIndex) {
            ScriptObject o = ToObject(cx, thisValue);
            Object lenVal = Get(cx, o, "length");
            long len = ToUint32(cx, lenVal);
            if (len == 0) {
                return -1;
            }
            long n;
            if (fromIndex != null) {
                n = (long) ToInteger(cx, fromIndex);
            } else {
                n = (long) (len - 1);
            }
            long k;
            if (n >= 0) {
                k = (long) Math.min(n, len - 1);
            } else {
                k = (long) (len - Math.abs(n));
            }
            for (; k >= 0; --k) {
                boolean kpresent = HasProperty(cx, o, ToString(k));
                if (kpresent) {
                    Object elementk = Get(cx, o, ToString(k));
                    boolean same = strictEqualityComparison(searchElement, elementk);
                    if (same) {
                        return k;
                    }
                }
            }
            return -1;
        }

        /**
         * 15.4.4.16 Array.prototype.every ( callbackfn [ , thisArg ] )
         */
        @Function(name = "every", arity = 1)
        public static Object every(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            ScriptObject o = ToObject(cx, thisValue);
            Object lenVal = Get(cx, o, "length");
            long len = ToUint32(cx, lenVal);
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
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
            return true;
        }

        /**
         * 15.4.4.17 Array.prototype.some ( callbackfn [ , thisArg ] )
         */
        @Function(name = "some", arity = 1)
        public static Object some(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            ScriptObject o = ToObject(cx, thisValue);
            Object lenVal = Get(cx, o, "length");
            long len = ToUint32(cx, lenVal);
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
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
            return false;
        }

        /**
         * 15.4.4.18 Array.prototype.forEach ( callbackfn [ , thisArg ] )
         */
        @Function(name = "forEach", arity = 1)
        public static Object forEach(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            ScriptObject o = ToObject(cx, thisValue);
            Object lenVal = Get(cx, o, "length");
            long len = ToUint32(cx, lenVal);
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            for (long k = 0; k < len; ++k) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    callback.call(cx, thisArg, kvalue, k, o);
                }
            }
            return UNDEFINED;
        }

        /**
         * 15.4.4.19 Array.prototype.map ( callbackfn [ , thisArg ] )
         */
        @Function(name = "map", arity = 1)
        public static Object map(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            ScriptObject o = ToObject(cx, thisValue);
            Object lenVal = Get(cx, o, "length");
            long len = ToUint32(cx, lenVal);
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            ScriptObject a = ArrayCreate(cx, len);
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
            return a;
        }

        /**
         * 15.4.4.20 Array.prototype.filter ( callbackfn [ , thisArg ] )
         */
        @Function(name = "filter", arity = 1)
        public static Object filter(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            ScriptObject o = ToObject(cx, thisValue);
            Object lenVal = Get(cx, o, "length");
            long len = ToUint32(cx, lenVal);
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            ScriptObject a = ArrayCreate(cx, 0);
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
            return a;
        }

        /**
         * 15.4.4.21 Array.prototype.reduce ( callbackfn [ , initialValue ] )
         */
        @Function(name = "reduce", arity = 1)
        public static Object reduce(ExecutionContext cx, Object thisValue, Object callbackfn,
                @Optional(Optional.Default.NONE) Object initialValue) {
            ScriptObject o = ToObject(cx, thisValue);
            Object lenVal = Get(cx, o, "length");
            long len = ToUint32(cx, lenVal);
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            if (len == 0 && initialValue == null) {
                throw throwTypeError(cx, Messages.Key.ReduceInitialValue);
            }
            long k = 0;
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
            for (; k < len; ++k) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    accumulator = callback.call(cx, UNDEFINED, accumulator, kvalue, k, o);
                }
            }
            return accumulator;
        }

        /**
         * 15.4.4.22 Array.prototype.reduceRight ( callbackfn [ , initialValue ] )
         */
        @Function(name = "reduceRight", arity = 1)
        public static Object reduceRight(ExecutionContext cx, Object thisValue, Object callbackfn,
                @Optional(Optional.Default.NONE) Object initialValue) {
            ScriptObject o = ToObject(cx, thisValue);
            Object lenVal = Get(cx, o, "length");
            long len = ToUint32(cx, lenVal);
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            if (len == 0 && initialValue == null) {
                throw throwTypeError(cx, Messages.Key.ReduceInitialValue);
            }
            long k = (len - 1);
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
            for (; k >= 0; --k) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    accumulator = callback.call(cx, UNDEFINED, accumulator, kvalue, k, o);
                }
            }
            return accumulator;
        }

        /**
         * 15.4.4.23 Array.prototype.items ( )
         */
        @Function(name = "items", arity = 0)
        public static Object items(ExecutionContext cx, Object thisValue) {
            ScriptObject o = ToObject(cx, thisValue);
            return CreateArrayIterator(cx, o, ArrayIterationKind.KeyValue);
        }

        /**
         * 15.4.4.24 Array.prototype.keys ( )
         */
        @Function(name = "keys", arity = 0)
        public static Object keys(ExecutionContext cx, Object thisValue) {
            ScriptObject o = ToObject(cx, thisValue);
            return CreateArrayIterator(cx, o, ArrayIterationKind.Key);
        }

        /**
         * 15.4.4.25 Array.prototype.values ( )
         */
        @Function(name = "values", arity = 0)
        public static Object values(ExecutionContext cx, Object thisValue) {
            ScriptObject o = ToObject(cx, thisValue);
            return CreateArrayIterator(cx, o, ArrayIterationKind.Value);
        }

        /**
         * 15.4.4.x Array.prototype.find ( predicate [ , thisArg ] )
         */
        @Function(name = "find", arity = 1)
        public static Object find(ExecutionContext cx, Object thisValue, Object predicate,
                Object thisArg) {
            ScriptObject o = ToObject(cx, thisValue);
            Object lenVal = Get(cx, o, "length");
            long len = ToUint32(cx, lenVal);
            // FIXME: spec bug (IsCallable() check should occur before return) (bug 1342)
            if (!IsCallable(predicate)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            Callable pred = (Callable) predicate;
            if (len == 0) {
                return UNDEFINED;
            }
            for (long k = 0; k < len; ++k) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    Object result = pred.call(cx, thisArg, kvalue, k, o);
                    if (ToBoolean(result)) {
                        return kvalue;
                    }
                }
            }
            return UNDEFINED;
        }

        /**
         * 15.4.4.x Array.prototype.findIndex ( predicate [ , thisArg ] )
         */
        @Function(name = "findIndex", arity = 1)
        public static Object findIndex(ExecutionContext cx, Object thisValue, Object predicate,
                Object thisArg) {
            ScriptObject o = ToObject(cx, thisValue);
            Object lenVal = Get(cx, o, "length");
            long len = ToUint32(cx, lenVal);
            // FIXME: spec bug (IsCallable() check should occur before return) (bug 1342)
            if (!IsCallable(predicate)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            Callable pred = (Callable) predicate;
            if (len == 0) {
                return -1;
            }
            for (long k = 0; k < len; ++k) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(cx, o, pk);
                if (kpresent) {
                    Object kvalue = Get(cx, o, pk);
                    Object result = pred.call(cx, thisArg, kvalue, k, o);
                    if (ToBoolean(result)) {
                        return k;
                    }
                }
            }
            return -1;
        }
    }
}

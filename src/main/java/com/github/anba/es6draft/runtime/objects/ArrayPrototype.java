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
public class ArrayPrototype extends OrdinaryObject implements ScriptObject, Initialisable {
    public ArrayPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);

        // 15.4.4.26 Array.prototype.@@iterator ( )
        defineOwnProperty(realm, BuiltinSymbol.iterator.get(),
                new PropertyDescriptor(Get(realm, this, "values"), true, false, true));
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
        public static Object toString(Realm realm, Object thisValue) {
            ScriptObject array = ToObject(realm, thisValue);
            Object func = Get(realm, array, "join");
            if (!IsCallable(func)) {
                func = realm.getIntrinsic(Intrinsics.ObjProto_toString);
            }
            return ((Callable) func).call(array);
        }

        /**
         * 15.4.4.3 Array.prototype.toLocaleString ( )
         */
        @Function(name = "toLocaleString", arity = 0)
        public static Object toLocaleString(Realm realm, Object thisValue) {
            ScriptObject array = ToObject(realm, thisValue);
            Object arrayLen = Get(realm, array, "length");
            long len = ToUint32(realm, arrayLen);
            String separator = ",";
            if (len == 0) {
                return "";
            }
            StringBuilder r = new StringBuilder();
            Object firstElement = Get(realm, array, "0");
            if (Type.isUndefinedOrNull(firstElement)) {
                r.append("");
            } else {
                r.append(ToString(realm, Invoke(realm, firstElement, "toLocaleString")));
            }
            for (long k = 1; k < len; ++k) {
                Object nextElement = Get(realm, array, ToString(k));
                if (Type.isUndefinedOrNull(nextElement)) {
                    r.append(separator).append("");
                } else {
                    r.append(separator).append(
                            ToString(realm, Invoke(realm, nextElement, "toLocaleString")));
                }
            }
            return r.toString();
        }

        /**
         * 15.4.4.4 Array.prototype.concat ( [ item1 [ , item2 [ , ... ] ] ] )
         */
        @Function(name = "concat", arity = 1)
        public static Object concat(Realm realm, Object thisValue, Object... items) {
            ScriptObject o = ToObject(realm, thisValue);
            ScriptObject a = ArrayCreate(realm, 0);
            long n = 0;
            int itemsLength = items.length;
            items = Arrays.copyOf(items, itemsLength + 1, Object[].class);
            System.arraycopy(items, 0, items, 1, itemsLength);
            items[0] = o;
            for (Object item : items) {
                if (item instanceof ExoticArray) {
                    assert item instanceof ScriptObject;
                    ScriptObject e = (ScriptObject) item;
                    long len = ToUint32(realm, Get(realm, e, "length"));
                    for (long k = 0; k < len; ++k, ++n) {
                        String p = ToString(k);
                        boolean exists = HasProperty(realm, e, p);
                        if (exists) {
                            Object subElement = Get(realm, e, p);
                            a.defineOwnProperty(realm, ToString(n), new PropertyDescriptor(
                                    subElement, true, true, true));
                        }
                    }
                } else {
                    a.defineOwnProperty(realm, ToString(n++), new PropertyDescriptor(item, true,
                            true, true));
                }
            }
            Put(realm, a, "length", n, true);
            return a;
        }

        /**
         * 15.4.4.5 Array.prototype.join (separator)
         */
        @Function(name = "join", arity = 1)
        public static Object join(Realm realm, Object thisValue, Object separator) {
            ScriptObject o = ToObject(realm, thisValue);
            Object lenVal = Get(realm, o, "length");
            long len = ToUint32(realm, lenVal);
            if (Type.isUndefined(separator)) {
                separator = ",";
            }
            String sep = ToFlatString(realm, separator);
            if (len == 0) {
                return "";
            }
            StringBuilder r = new StringBuilder();
            Object element0 = Get(realm, o, "0");
            if (Type.isUndefinedOrNull(element0)) {
                r.append("");
            } else {
                r.append(ToString(realm, element0));
            }
            for (long k = 1; k < len; ++k) {
                Object element = Get(realm, o, ToString(k));
                if (Type.isUndefinedOrNull(element)) {
                    r.append(sep).append("");
                } else {
                    r.append(sep).append(ToString(realm, element));
                }
            }
            return r.toString();
        }

        /**
         * 15.4.4.6 Array.prototype.pop ( )
         */
        @Function(name = "pop", arity = 0)
        public static Object pop(Realm realm, Object thisValue) {
            ScriptObject o = ToObject(realm, thisValue);
            Object lenVal = Get(realm, o, "length");
            long len = ToUint32(realm, lenVal);
            if (len == 0) {
                Put(realm, o, "length", 0, true);
                return UNDEFINED;
            } else {
                assert len > 0;
                long newLen = len - 1;
                String index = ToString(newLen);
                Object element = Get(realm, o, index);
                DeletePropertyOrThrow(realm, o, index);
                Put(realm, o, "length", newLen, true);
                return element;
            }
        }

        /**
         * 15.4.4.7 Array.prototype.push ( [ item1 [ , item2 [ , ... ] ] ] )
         */
        @Function(name = "push", arity = 1)
        public static Object push(Realm realm, Object thisValue, Object... items) {
            ScriptObject o = ToObject(realm, thisValue);
            Object lenVal = Get(realm, o, "length");
            long n = ToUint32(realm, lenVal);
            for (Object e : items) {
                Put(realm, o, ToString(n), e, true);
                n += 1;
            }
            Put(realm, o, "length", n, true);
            return n;
        }

        /**
         * 15.4.4.8 Array.prototype.reverse ( )
         */
        @Function(name = "reverse", arity = 0)
        public static Object reverse(Realm realm, Object thisValue) {
            ScriptObject o = ToObject(realm, thisValue);
            Object lenVal = Get(realm, o, "length");
            long len = ToUint32(realm, lenVal);
            long middle = len / 2L;
            for (long lower = 0; lower != middle; ++lower) {
                long upper = len - lower - 1;
                String upperP = ToString(upper);
                String lowerP = ToString(lower);
                Object lowerValue = Get(realm, o, lowerP);
                Object upperValue = Get(realm, o, upperP);
                boolean lowerExists = HasProperty(realm, o, lowerP);
                boolean upperExists = HasProperty(realm, o, upperP);
                if (lowerExists && upperExists) {
                    Put(realm, o, lowerP, upperValue, true);
                    Put(realm, o, upperP, lowerValue, true);
                } else if (!lowerExists && upperExists) {
                    Put(realm, o, lowerP, upperValue, true);
                    DeletePropertyOrThrow(realm, o, upperP);
                } else if (lowerExists && !upperExists) {
                    DeletePropertyOrThrow(realm, o, lowerP);
                    Put(realm, o, upperP, lowerValue, true);
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
        public static Object shift(Realm realm, Object thisValue) {
            ScriptObject o = ToObject(realm, thisValue);
            Object lenVal = Get(realm, o, "length");
            long len = ToUint32(realm, lenVal);
            if (len == 0) {
                Put(realm, o, "length", 0, true);
                return UNDEFINED;
            }
            Object first = Get(realm, o, "0");
            for (long k = 1; k < len; ++k) {
                String from = ToString(k);
                String to = ToString(k - 1);
                boolean fromPresent = HasProperty(realm, o, from);
                if (fromPresent) {
                    Object fromVal = Get(realm, o, from);
                    Put(realm, o, to, fromVal, true);
                } else {
                    DeletePropertyOrThrow(realm, o, to);
                }
            }
            DeletePropertyOrThrow(realm, o, ToString(len - 1));
            Put(realm, o, "length", len - 1, true);
            return first;
        }

        /**
         * 15.4.4.10 Array.prototype.slice (start, end)
         */
        @Function(name = "slice", arity = 2)
        public static Object slice(Realm realm, Object thisValue, Object start, Object end) {
            ScriptObject o = ToObject(realm, thisValue);
            ScriptObject a = ArrayCreate(realm, 0);
            Object lenVal = Get(realm, o, "length");
            long len = ToUint32(realm, lenVal);
            double relativeStart = ToInteger(realm, start);
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
                relativeEnd = ToInteger(realm, end);
            }
            long finall;
            if (relativeEnd < 0) {
                finall = (long) Math.max(len + relativeEnd, 0);
            } else {
                finall = (long) Math.min(relativeEnd, len);
            }
            for (long n = 0; k < finall; ++k, ++n) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(realm, o, pk);
                if (kpresent) {
                    Object kvalue = Get(realm, o, pk);
                    String p = ToString(n);
                    boolean status = CreateOwnDataProperty(realm, a, p, kvalue);
                    if (!status) {
                        // FIXME: spec bug? (Assert instead of throw TypeError?)
                        throw throwTypeError(realm, Messages.Key.PropertyNotCreatable, p);
                    }
                }
            }
            // FIXME: spec bug (call to Put does make sense with the supplied args)
            // Put(realm, a, "length", finall, true);
            return a;
        }

        private static class DefaultComparator implements Comparator<Object> {
            private final Realm realm;

            DefaultComparator(Realm realm) {
                this.realm = realm;
            }

            @Override
            public int compare(Object o1, Object o2) {
                String x = ToFlatString(realm, o1);
                String y = ToFlatString(realm, o2);
                return x.compareTo(y);
            }
        }

        private static class FunctionComparator implements Comparator<Object> {
            private final Realm realm;
            private final Callable comparefn;

            FunctionComparator(Realm realm, Callable comparefn) {
                this.realm = realm;
                this.comparefn = comparefn;
            }

            @Override
            public int compare(Object o1, Object o2) {
                double c = ToInteger(realm, comparefn.call(UNDEFINED, o1, o2));
                return (c == 0 ? 0 : c < 0 ? -1 : 1);
            }
        }

        /**
         * 15.4.4.11 Array.prototype.sort (comparefn)
         */
        @Function(name = "sort", arity = 1)
        public static Object sort(Realm realm, Object thisValue, Object comparefn) {
            ScriptObject obj = ToObject(realm, thisValue);
            long len = ToUint32(realm, Get(realm, obj, "length"));

            int emptyCount = 0;
            int undefCount = 0;
            List<Object> elements = new ArrayList<>((int) Math.min(len, 1024));
            for (int i = 0; i < len; ++i) {
                String index = ToString(i);
                if (HasProperty(realm, obj, index)) {
                    Object e = Get(realm, obj, index);
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
                        throw throwTypeError(realm, Messages.Key.NotCallable);
                    }
                    comparator = new FunctionComparator(realm, (Callable) comparefn);
                } else {
                    comparator = new DefaultComparator(realm);
                }
                Collections.sort(elements, comparator);
            }

            for (int i = 0, offset = 0; i < count; ++i) {
                String p = ToString(offset + i);
                if (!obj.set(realm, p, elements.get(i), obj)) {
                    throw throwTypeError(realm, Messages.Key.PropertyNotModifiable, p);
                }
            }
            for (int i = 0, offset = count; i < undefCount; ++i) {
                String p = ToString(offset + i);
                if (!obj.set(realm, p, UNDEFINED, obj)) {
                    throw throwTypeError(realm, Messages.Key.PropertyNotModifiable, p);
                }
            }
            for (int i = 0, offset = count + undefCount; i < emptyCount; ++i) {
                DeletePropertyOrThrow(realm, obj, ToString(offset + i));
            }

            return obj;
        }

        /**
         * 15.4.4.12 Array.prototype.splice (start, deleteCount [ , item1 [ , item2 [ , ... ] ] ] )
         */
        @Function(name = "splice", arity = 2)
        public static Object splice(Realm realm, Object thisValue, Object start,
                Object deleteCount, Object... items) {
            ScriptObject o = ToObject(realm, thisValue);
            ScriptObject a = ArrayCreate(realm, 0);
            Object lenVal = Get(realm, o, "length");
            long len = ToUint32(realm, lenVal);
            double relativeStart = ToInteger(realm, start);
            long actualStart;
            if (relativeStart < 0) {
                actualStart = (long) Math.max(len + relativeStart, 0);
            } else {
                actualStart = (long) Math.min(relativeStart, len);
            }
            long actualDeleteCount = (long) Math.min(Math.max(ToInteger(realm, deleteCount), 0),
                    len - actualStart);
            for (long k = 0; k < actualDeleteCount; ++k) {
                String from = ToString(actualStart + k);
                boolean fromPresent = HasProperty(realm, o, from);
                if (fromPresent) {
                    Object fromValue = Get(realm, o, from);
                    a.defineOwnProperty(realm, ToString(k), new PropertyDescriptor(fromValue, true,
                            true, true));
                }
            }
            Put(realm, a, "length", actualDeleteCount, true);

            int itemCount = items.length;
            if (itemCount < actualDeleteCount) {
                for (long k = actualStart; k < (len - actualDeleteCount); ++k) {
                    String from = ToString(k + actualDeleteCount);
                    String to = ToString(k + itemCount);
                    boolean fromPresent = HasProperty(realm, o, from);
                    if (fromPresent) {
                        Object fromValue = Get(realm, o, from);
                        Put(realm, o, to, fromValue, true);
                    } else {
                        DeletePropertyOrThrow(realm, o, to);
                    }
                }
                for (long k = len; k > (len - actualDeleteCount + itemCount); --k) {
                    DeletePropertyOrThrow(realm, o, ToString(k - 1));
                }
            } else if (itemCount > actualDeleteCount) {
                for (long k = (len - actualDeleteCount); k > actualStart; --k) {
                    String from = ToString(k + actualDeleteCount - 1);
                    String to = ToString(k + itemCount - 1);
                    boolean fromPresent = HasProperty(realm, o, from);
                    if (fromPresent) {
                        Object fromValue = Get(realm, o, from);
                        Put(realm, o, to, fromValue, true);
                    } else {
                        DeletePropertyOrThrow(realm, o, to);
                    }
                }
            }
            long k = actualStart;
            for (int i = 0; i < itemCount; ++k, ++i) {
                Object e = items[i];
                Put(realm, o, ToString(k), e, true);
            }
            Put(realm, o, "length", len - actualDeleteCount + itemCount, true);
            return a;
        }

        /**
         * 15.4.4.13 Array.prototype.unshift ( [ item1 [ , item2 [ , ... ] ] ] )
         */
        @Function(name = "unshift", arity = 1)
        public static Object unshift(Realm realm, Object thisValue, Object... items) {
            ScriptObject o = ToObject(realm, thisValue);
            Object lenVal = Get(realm, o, "length");
            long len = ToUint32(realm, lenVal);
            int argCount = items.length;
            for (long k = len; k > 0; --k) {
                String from = ToString(k - 1);
                String to = ToString(k + argCount - 1);
                boolean fromPresent = HasProperty(realm, o, from);
                if (fromPresent) {
                    Object fromValue = Get(realm, o, from);
                    Put(realm, o, to, fromValue, true);
                } else {
                    DeletePropertyOrThrow(realm, o, to);
                }
            }
            for (int j = 0; j < items.length; ++j) {
                Object e = items[j];
                Put(realm, o, ToString(j), e, true);
            }
            Put(realm, o, "length", len + argCount, true);
            return len + argCount;
        }

        /**
         * 15.4.4.14 Array.prototype.indexOf ( searchElement [ , fromIndex ] )
         */
        @Function(name = "indexOf", arity = 1)
        public static Object indexOf(Realm realm, Object thisValue, Object searchElement,
                @Optional(Optional.Default.NONE) Object fromIndex) {
            ScriptObject o = ToObject(realm, thisValue);
            Object lenVal = Get(realm, o, "length");
            long len = ToUint32(realm, lenVal);
            if (len == 0) {
                return -1;
            }
            long n;
            if (fromIndex != null) {
                n = (long) ToInteger(realm, fromIndex);
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
                boolean kpresent = HasProperty(realm, o, ToString(k));
                if (kpresent) {
                    Object elementk = Get(realm, o, ToString(k));
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
        public static Object lastIndexOf(Realm realm, Object thisValue, Object searchElement,
                @Optional(Optional.Default.NONE) Object fromIndex) {
            ScriptObject o = ToObject(realm, thisValue);
            Object lenVal = Get(realm, o, "length");
            long len = ToUint32(realm, lenVal);
            if (len == 0) {
                return -1;
            }
            long n;
            if (fromIndex != null) {
                n = (long) ToInteger(realm, fromIndex);
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
                boolean kpresent = HasProperty(realm, o, ToString(k));
                if (kpresent) {
                    Object elementk = Get(realm, o, ToString(k));
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
        public static Object every(Realm realm, Object thisValue, Object callbackfn, Object thisArg) {
            ScriptObject o = ToObject(realm, thisValue);
            Object lenVal = Get(realm, o, "length");
            long len = ToUint32(realm, lenVal);
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(realm, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            for (long k = 0; k < len; ++k) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(realm, o, pk);
                if (kpresent) {
                    Object kvalue = Get(realm, o, pk);
                    Object testResult = callback.call(thisArg, kvalue, k, o);
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
        public static Object some(Realm realm, Object thisValue, Object callbackfn, Object thisArg) {
            ScriptObject o = ToObject(realm, thisValue);
            Object lenVal = Get(realm, o, "length");
            long len = ToUint32(realm, lenVal);
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(realm, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            for (long k = 0; k < len; ++k) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(realm, o, pk);
                if (kpresent) {
                    Object kvalue = Get(realm, o, pk);
                    Object testResult = callback.call(thisArg, kvalue, k, o);
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
        public static Object forEach(Realm realm, Object thisValue, Object callbackfn,
                Object thisArg) {
            ScriptObject o = ToObject(realm, thisValue);
            Object lenVal = Get(realm, o, "length");
            long len = ToUint32(realm, lenVal);
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(realm, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            for (long k = 0; k < len; ++k) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(realm, o, pk);
                if (kpresent) {
                    Object kvalue = Get(realm, o, pk);
                    callback.call(thisArg, kvalue, k, o);
                }
            }
            return UNDEFINED;
        }

        /**
         * 15.4.4.19 Array.prototype.map ( callbackfn [ , thisArg ] )
         */
        @Function(name = "map", arity = 1)
        public static Object map(Realm realm, Object thisValue, Object callbackfn, Object thisArg) {
            ScriptObject o = ToObject(realm, thisValue);
            Object lenVal = Get(realm, o, "length");
            long len = ToUint32(realm, lenVal);
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(realm, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            ScriptObject a = ArrayCreate(realm, len);
            for (long k = 0; k < len; ++k) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(realm, o, pk);
                if (kpresent) {
                    Object kvalue = Get(realm, o, pk);
                    Object mappedValue = callback.call(thisArg, kvalue, k, o);
                    a.defineOwnProperty(realm, pk, new PropertyDescriptor(mappedValue, true, true,
                            true));
                }
            }
            return a;
        }

        /**
         * 15.4.4.20 Array.prototype.filter ( callbackfn [ , thisArg ] )
         */
        @Function(name = "filter", arity = 1)
        public static Object filter(Realm realm, Object thisValue, Object callbackfn, Object thisArg) {
            ScriptObject o = ToObject(realm, thisValue);
            Object lenVal = Get(realm, o, "length");
            long len = ToUint32(realm, lenVal);
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(realm, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            ScriptObject a = ArrayCreate(realm, 0);
            for (long k = 0, to = 0; k < len; ++k) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(realm, o, pk);
                if (kpresent) {
                    Object kvalue = Get(realm, o, pk);
                    Object selected = callback.call(thisArg, kvalue, k, o);
                    if (ToBoolean(selected)) {
                        a.defineOwnProperty(realm, ToString(to), new PropertyDescriptor(kvalue,
                                true, true, true));
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
        public static Object reduce(Realm realm, Object thisValue, Object callbackfn,
                @Optional(Optional.Default.NONE) Object initialValue) {
            ScriptObject o = ToObject(realm, thisValue);
            Object lenVal = Get(realm, o, "length");
            long len = ToUint32(realm, lenVal);
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(realm, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            if (len == 0 && initialValue == null) {
                throw throwTypeError(realm, Messages.Key.ReduceInitialValue);
            }
            long k = 0;
            Object accumulator = null;
            if (initialValue != null) {
                accumulator = initialValue;
            } else {
                boolean kpresent = false;
                for (; !kpresent && k < len; ++k) {
                    String pk = ToString(k);
                    kpresent = HasProperty(realm, o, pk);
                    if (kpresent) {
                        accumulator = Get(realm, o, pk);
                    }
                }
                if (!kpresent) {
                    throw throwTypeError(realm, Messages.Key.ReduceInitialValue);
                }
            }
            for (; k < len; ++k) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(realm, o, pk);
                if (kpresent) {
                    Object kvalue = Get(realm, o, pk);
                    accumulator = callback.call(UNDEFINED, accumulator, kvalue, k, o);
                }
            }
            return accumulator;
        }

        /**
         * 15.4.4.22 Array.prototype.reduceRight ( callbackfn [ , initialValue ] )
         */
        @Function(name = "reduceRight", arity = 1)
        public static Object reduceRight(Realm realm, Object thisValue, Object callbackfn,
                @Optional(Optional.Default.NONE) Object initialValue) {
            ScriptObject o = ToObject(realm, thisValue);
            Object lenVal = Get(realm, o, "length");
            long len = ToUint32(realm, lenVal);
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(realm, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            if (len == 0 && initialValue == null) {
                throw throwTypeError(realm, Messages.Key.ReduceInitialValue);
            }
            long k = (len - 1);
            Object accumulator = null;
            if (initialValue != null) {
                accumulator = initialValue;
            } else {
                boolean kpresent = false;
                for (; !kpresent && k >= 0; --k) {
                    String pk = ToString(k);
                    kpresent = HasProperty(realm, o, pk);
                    if (kpresent) {
                        accumulator = Get(realm, o, pk);
                    }
                }
                if (!kpresent) {
                    throw throwTypeError(realm, Messages.Key.ReduceInitialValue);
                }
            }
            for (; k >= 0; --k) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(realm, o, pk);
                if (kpresent) {
                    Object kvalue = Get(realm, o, pk);
                    accumulator = callback.call(UNDEFINED, accumulator, kvalue, k, o);
                }
            }
            return accumulator;
        }

        /**
         * 15.4.4.23 Array.prototype.items ( )
         */
        @Function(name = "items", arity = 0)
        public static Object items(Realm realm, Object thisValue) {
            ScriptObject o = ToObject(realm, thisValue);
            return CreateArrayIterator(realm, o, ArrayIterationKind.KeyValue);
        }

        /**
         * 15.4.4.24 Array.prototype.keys ( )
         */
        @Function(name = "keys", arity = 0)
        public static Object keys(Realm realm, Object thisValue) {
            ScriptObject o = ToObject(realm, thisValue);
            return CreateArrayIterator(realm, o, ArrayIterationKind.Key);
        }

        /**
         * 15.4.4.25 Array.prototype.values ( )
         */
        @Function(name = "values", arity = 0)
        public static Object values(Realm realm, Object thisValue) {
            ScriptObject o = ToObject(realm, thisValue);
            return CreateArrayIterator(realm, o, ArrayIterationKind.Value);
        }

        /**
         * 15.4.4.x Array.prototype.find ( predicate [ , thisArg ] )
         */
        @Function(name = "find", arity = 1)
        public static Object find(Realm realm, Object thisValue, Object predicate, Object thisArg) {
            ScriptObject o = ToObject(realm, thisValue);
            Object lenVal = Get(realm, o, "length");
            long len = ToUint32(realm, lenVal);
            // FIXME: spec bug (IsCallable() check should occur before return)
            if (!IsCallable(predicate)) {
                throw throwTypeError(realm, Messages.Key.NotCallable);
            }
            Callable pred = (Callable) predicate;
            if (len == 0) {
                return UNDEFINED;
            }
            for (long k = 0; k < len; ++k) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(realm, o, pk);
                if (kpresent) {
                    Object kvalue = Get(realm, o, pk);
                    Object result = pred.call(thisArg, kvalue, k, o);
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
        public static Object findIndex(Realm realm, Object thisValue, Object predicate,
                Object thisArg) {
            ScriptObject o = ToObject(realm, thisValue);
            Object lenVal = Get(realm, o, "length");
            long len = ToUint32(realm, lenVal);
            // FIXME: spec bug (IsCallable() check should occur before return)
            if (!IsCallable(predicate)) {
                throw throwTypeError(realm, Messages.Key.NotCallable);
            }
            Callable pred = (Callable) predicate;
            if (len == 0) {
                return -1;
            }
            for (long k = 0; k < len; ++k) {
                String pk = ToString(k);
                boolean kpresent = HasProperty(realm, o, pk);
                if (kpresent) {
                    Object kvalue = Get(realm, o, pk);
                    Object result = pred.call(thisArg, kvalue, k, o);
                    if (ToBoolean(result)) {
                        return k;
                    }
                }
            }
            return -1;
        }
    }
}

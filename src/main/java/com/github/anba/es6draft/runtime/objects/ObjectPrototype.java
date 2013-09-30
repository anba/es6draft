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
import static com.github.anba.es6draft.runtime.types.Null.NULL;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArguments;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;
import com.github.anba.es6draft.runtime.types.builtins.ExoticBoundFunction;
import com.github.anba.es6draft.runtime.types.builtins.ExoticProxy;
import com.github.anba.es6draft.runtime.types.builtins.ExoticString;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>19 Fundamental Objects</h1><br>
 * <h2>19.1 Object Objects</h2>
 * <ul>
 * <li>19.1.4 Properties of the Object Prototype Object
 * <li>19.1.5 Properties of Object Instances
 * </ul>
 */
public class ObjectPrototype extends OrdinaryObject implements Initialisable {
    public ObjectPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        createProperties(this, cx, AdditionalProperties.class);
    }

    public enum Properties {
        ;

        @Prototype
        public static final ScriptObject __proto__ = null;

        /**
         * 19.1.4.1 Object.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Object;

        /**
         * 19.1.4.6 Object.prototype.toString ( )
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            if (Type.isUndefined(thisValue)) {
                return "[object Undefined]";
            }
            /* step 2 */
            if (Type.isNull(thisValue)) {
                return "[object Null]";
            }
            /* step 3 */
            ScriptObject o = ToObject(cx, thisValue);
            /* steps 4-15 */
            String builtinTag;
            if (o instanceof ExoticArray) {
                builtinTag = "Array";
            } else if (o instanceof ExoticString) {
                builtinTag = "String";
            } else if (o instanceof ExoticProxy) {
                builtinTag = "Proxy";
            } else if (o instanceof ExoticArguments) {
                builtinTag = "Arguments";
            } else if (o instanceof FunctionObject || o instanceof BuiltinFunction
                    || o instanceof ExoticBoundFunction) {
                builtinTag = "Function";
            } else if (o instanceof ErrorObject) {
                builtinTag = "Error";
            } else if (o instanceof BooleanObject) {
                builtinTag = "Boolean";
            } else if (o instanceof NumberObject) {
                builtinTag = "Number";
            } else if (o instanceof DateObject) {
                builtinTag = "Date";
            } else if (o instanceof RegExpObject) {
                builtinTag = "RegExp";
            } else if (o instanceof MathObject) {
                builtinTag = "Math";
            } else if (o instanceof JSONObject) {
                builtinTag = "JSON";
            } else {
                builtinTag = "Object";
            }
            /* steps 15-16 */
            boolean hasTag = HasProperty(cx, o, BuiltinSymbol.toStringTag.get());
            /* steps 17-18 */
            String tag;
            if (!hasTag) {
                tag = builtinTag;
            } else {
                try {
                    Object ttag = Get(cx, o, BuiltinSymbol.toStringTag.get());
                    if (Type.isString(ttag)) {
                        tag = Type.stringValue(ttag).toString();
                    } else {
                        tag = "???";
                    }
                } catch (ScriptException e) {
                    tag = "???";
                }
                // FIXME: spec bug? (censor 'Object' again, but see Bug 1148) (Bug 1408/1459)
                if (censoredNames.contains(tag) && !builtinTag.equals(tag)) {
                    tag = "~" + tag;
                }
            }
            /* step 19 */
            return "[object " + tag + "]";
        }

        private static final Set<String> censoredNames;
        static {
            List<String> names = Arrays.asList("Arguments", "Array", "Boolean", "Date", "Error",
                    "Function", "JSON", "Math", "Number", "RegExp", "String");
            censoredNames = new HashSet<>(names);
        }

        /**
         * 19.1.4.5 Object.prototype.toLocaleString ( )
         */
        @Function(name = "toLocaleString", arity = 0)
        public static Object toLocaleString(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            return Invoke(cx, thisValue, "toString");
        }

        /**
         * 19.1.4.7 Object.prototype.valueOf ( )
         */
        @Function(name = "valueOf", arity = 0)
        public static Object valueOf(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            return o;
        }

        /**
         * 19.1.4.2 Object.prototype.hasOwnProperty (V)
         */
        @Function(name = "hasOwnProperty", arity = 1)
        public static Object hasOwnProperty(ExecutionContext cx, Object thisValue, Object v) {
            /* steps 1-2 */
            Object p = ToPropertyKey(cx, v);
            /* steps 3-4 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 5 */
            if (p instanceof String) {
                return HasOwnProperty(cx, o, (String) p);
            } else {
                return HasOwnProperty(cx, o, (Symbol) p);
            }
        }

        /**
         * 19.1.4.3 Object.prototype.isPrototypeOf (V)
         */
        @Function(name = "isPrototypeOf", arity = 1)
        public static Object isPrototypeOf(ExecutionContext cx, Object thisValue, Object v) {
            /* step 1 */
            if (!Type.isObject(v)) {
                return false;
            }
            ScriptObject _v = Type.objectValue(v);
            /* steps 2-3 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 4 */
            for (;;) {
                _v = _v.getPrototypeOf(cx);
                if (_v == null) {
                    return false;
                }
                if (SameValue(o, _v)) {
                    return true;
                }
            }
        }

        /**
         * 19.1.4.4 Object.prototype.propertyIsEnumerable (V)
         */
        @Function(name = "propertyIsEnumerable", arity = 1)
        public static Object propertyIsEnumerable(ExecutionContext cx, Object thisValue, Object v) {
            /* steps 1-2 */
            String p = ToFlatString(cx, v);
            /* steps 3-4 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 5 */
            Property desc = o.getOwnProperty(cx, p);
            /* step 6 */
            if (desc == null) {
                return false;
            }
            /* step 7 */
            return desc.isEnumerable();
        }
    }

    /**
     * B.2.2 Additional Properties of the Object.prototype Object
     */
    @CompatibilityExtension(CompatibilityOption.ObjectPrototype)
    public enum AdditionalProperties {
        ;

        /**
         * B.2.2.1 Object.prototype.__proto__<br>
         * B.2.2.1.1 get Object.prototype.__proto__
         */
        @Accessor(name = "__proto__", type = Accessor.Type.Getter)
        public static Object getPrototype(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            ScriptObject p = o.getPrototypeOf(cx);
            return p != null ? p : NULL;
        }

        /**
         * B.2.2.1 Object.prototype.__proto__<br>
         * B.2.2.1.2 set Object.prototype.__proto__
         */
        @Accessor(name = "__proto__", type = Accessor.Type.Setter)
        public static Object setPrototype(ExecutionContext cx, Object thisValue, Object proto) {
            /* steps 1-2 */
            Object o = CheckObjectCoercible(cx, thisValue);
            /* step 3 */
            if (!(Type.isNull(proto) || Type.isObject(proto))) {
                return proto;
            }
            /* step 4 */
            if (!Type.isObject(o)) {
                return proto;
            }
            /* steps 5-6 */
            ScriptObject obj = Type.objectValue(o);
            ScriptObject p = Type.isObject(proto) ? Type.objectValue(proto) : null;
            boolean status = obj.setPrototypeOf(cx, p);
            /* step 7 */
            if (!status) {
                // provide better error messages for ordinary objects
                if (obj instanceof OrdinaryObject) {
                    if (!obj.isExtensible(cx)) {
                        throw throwTypeError(cx, Messages.Key.NotExtensible);
                    }
                    throw throwTypeError(cx, Messages.Key.CyclicProto);
                }
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            /* step 8 */
            return proto;
        }
    }
}

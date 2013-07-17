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
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

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
import com.github.anba.es6draft.runtime.types.builtins.ExoticString;
import com.github.anba.es6draft.runtime.types.builtins.ExoticSymbol;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.2 Object Objects</h2>
 * <ul>
 * <li>15.2.4 Properties of the Object Prototype Object
 * <li>15.2.5 Properties of Object Instances
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
         * 15.2.4.1 Object.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Object;

        /**
         * 15.2.4.2 Object.prototype.toString ( )
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            if (Type.isUndefined(thisValue)) {
                return "[object Undefined]";
            }
            if (Type.isNull(thisValue)) {
                return "[object Null]";
            }
            ScriptObject o = ToObject(cx, thisValue);
            if (o instanceof ExoticSymbol) {
                return "[object Symbol]";
            }
            String builtinTag;
            if (o instanceof ExoticArray) {
                builtinTag = "Array";
            } else if (o instanceof ExoticString) {
                builtinTag = "String";
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
            String tag;
            boolean hasTag = HasProperty(cx, o, BuiltinSymbol.toStringTag.get());
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
            return "[object " + tag + "]";
        }

        private static final Set<String> censoredNames;
        static {
            List<String> names = Arrays.asList("Arguments", "Array", "Boolean", "Date", "Error",
                    "Function", "JSON", "Math", "Number", "RegExp", "String");
            censoredNames = new HashSet<>(names);
        }

        /**
         * 15.2.4.3 Object.prototype.toLocaleString ( )
         */
        @Function(name = "toLocaleString", arity = 0)
        public static Object toLocaleString(ExecutionContext cx, Object thisValue) {
            return Invoke(cx, thisValue, "toString");
        }

        /**
         * 15.2.4.4 Object.prototype.valueOf ( )
         */
        @Function(name = "valueOf", arity = 0)
        public static Object valueOf(ExecutionContext cx, Object thisValue) {
            ScriptObject o = ToObject(cx, thisValue);
            return o;
        }

        /**
         * 15.2.4.5 Object.prototype.hasOwnProperty (V)
         */
        @Function(name = "hasOwnProperty", arity = 1)
        public static Object hasOwnProperty(ExecutionContext cx, Object thisValue, Object v) {
            Object p = ToPropertyKey(cx, v);
            ScriptObject o = ToObject(cx, thisValue);
            if (p instanceof String) {
                return o.hasOwnProperty(cx, (String) p);
            } else {
                return o.hasOwnProperty(cx, (Symbol) p);
            }
        }

        /**
         * 15.2.4.6 Object.prototype.isPrototypeOf (V)
         */
        @Function(name = "isPrototypeOf", arity = 1)
        public static Object isPrototypeOf(ExecutionContext cx, Object thisValue, Object v) {
            if (!Type.isObject(v)) {
                return false;
            }
            ScriptObject w = Type.objectValue(v);
            ScriptObject o = ToObject(cx, thisValue);
            for (;;) {
                w = w.getInheritance(cx);
                if (w == null) {
                    return false;
                }
                if (o == w) {
                    return true;
                }
            }
        }

        /**
         * 15.2.4.7 Object.prototype.propertyIsEnumerable (V)
         */
        @Function(name = "propertyIsEnumerable", arity = 1)
        public static Object propertyIsEnumerable(ExecutionContext cx, Object thisValue, Object v) {
            String p = ToFlatString(cx, v);
            ScriptObject o = ToObject(cx, thisValue);
            Property desc = o.getOwnProperty(cx, p);
            if (desc == null) {
                return false;
            }
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
         * get Object.prototype.__proto__
         */
        @Accessor(name = "__proto__", type = Accessor.Type.Getter)
        public static Object getPrototype(ExecutionContext cx, Object thisValue) {
            ScriptObject o = ToObject(cx, thisValue);
            ScriptObject p = o.getInheritance(cx);
            return (p != null ? p : NULL);
        }

        /**
         * B.2.2.1 Object.prototype.__proto__<br>
         * set Object.prototype.__proto__
         */
        @Accessor(name = "__proto__", type = Accessor.Type.Setter)
        public static Object setPrototype(ExecutionContext cx, Object thisValue, Object p) {
            ScriptObject o = ToObject(cx, thisValue);
            if (!IsExtensible(cx, o)) {
                throwTypeError(cx, Messages.Key.NotExtensible);
            }
            boolean status = true;
            if (Type.isNull(p)) {
                status = o.setInheritance(cx, null);
            } else if (Type.isObject(p)) {
                status = o.setInheritance(cx, Type.objectValue(p));
            }
            if (!status) {
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            return UNDEFINED;
        }
    }
}

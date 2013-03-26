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

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
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
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.2 Object Objects</h2>
 * <ul>
 * <li>15.2.4 Properties of the Object Prototype Object
 * <li>15.2.5 Properties of Object Instances
 * </ul>
 */
public class ObjectPrototype extends OrdinaryObject implements ScriptObject, Initialisable {
    public ObjectPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
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
        public static Object toString(Realm realm, Object thisValue) {
            if (Type.isUndefined(thisValue)) {
                return "[object Undefined]";
            }
            if (Type.isNull(thisValue)) {
                return "[object Null]";
            }
            ScriptObject o = ToObject(realm, thisValue);
            if (o instanceof Symbol) {
                return "[object Symbol]";
            }
            String builtinTag;
            if (o instanceof ExoticArray) {
                builtinTag = "Array";
            } else if (o instanceof ExoticString) {
                builtinTag = "String";
            } else if (o instanceof ExoticArguments) {
                builtinTag = "Arguments";
            } else if (o instanceof OrdinaryFunction || o instanceof OrdinaryGenerator
                    || o instanceof BuiltinFunction || o instanceof ExoticBoundFunction) {
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
            boolean hasTag = HasProperty(realm, o, BuiltinSymbol.toStringTag.get());
            if (!hasTag) {
                tag = builtinTag;
            } else {
                try {
                    Object ttag = Get(realm, o, BuiltinSymbol.toStringTag.get());
                    if (Type.isString(ttag)) {
                        tag = Type.stringValue(ttag).toString();
                    } else {
                        tag = "???";
                    }
                } catch (ScriptException e) {
                    tag = "???";
                }
                // FIXME: spec bug? (censor 'Object' again, but see Bug 1148)
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
        public static Object toLocaleString(Realm realm, Object thisValue) {
            return Invoke(realm, thisValue, "toString");
        }

        /**
         * 15.2.4.4 Object.prototype.valueOf ( )
         */
        @Function(name = "valueOf", arity = 0)
        public static Object valueOf(Realm realm, Object thisValue) {
            ScriptObject o = ToObject(realm, thisValue);
            return o;
        }

        /**
         * 15.2.4.5 Object.prototype.hasOwnProperty (V)
         */
        @Function(name = "hasOwnProperty", arity = 1)
        public static Object hasOwnProperty(Realm realm, Object thisValue, Object v) {
            Object p = ToPropertyKey(realm, v);
            ScriptObject o = ToObject(realm, thisValue);
            if (p instanceof String) {
                return o.hasOwnProperty(realm, (String) p);
            } else {
                return o.hasOwnProperty(realm, (Symbol) p);
            }
        }

        /**
         * 15.2.4.6 Object.prototype.isPrototypeOf (V)
         */
        @Function(name = "isPrototypeOf", arity = 1)
        public static Object isPrototypeOf(Realm realm, Object thisValue, Object v) {
            if (!Type.isObject(v)) {
                return false;
            }
            ScriptObject w = Type.objectValue(v);
            ScriptObject o = ToObject(realm, thisValue);
            for (;;) {
                w = w.getPrototype(realm);
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
        public static Object propertyIsEnumerable(Realm realm, Object thisValue, Object v) {
            String p = ToFlatString(realm, v);
            ScriptObject o = ToObject(realm, thisValue);
            Property desc = o.getOwnProperty(realm, p);
            if (desc == null) {
                return false;
            }
            return desc.isEnumerable();
        }

        /**
         * B.3.1.1 Object.prototype.__proto__
         */
        @Accessor(name = "__proto__", type = Accessor.Type.Getter)
        public static Object getPrototype(Realm realm, Object thisValue) {
            ScriptObject o = ToObject(realm, thisValue);
            ScriptObject p = o.getPrototype(realm);
            return (p != null ? p : NULL);
        }

        /**
         * B.3.1.1 Object.prototype.__proto__
         */
        @Accessor(name = "__proto__", type = Accessor.Type.Setter)
        public static Object setPrototype(Realm realm, Object thisValue, Object p) {
            ScriptObject o = ToObject(realm, thisValue);
            if (!IsExtensible(realm, o)) {
                throwTypeError(realm, Messages.Key.NotExtensible);
            }
            if (Type.isNull(p)) {
                o.setPrototype(realm, null);
            } else if (Type.isObject(p)) {
                o.setPrototype(realm, Type.objectValue(p));
            }
            return UNDEFINED;
        }
    }
}

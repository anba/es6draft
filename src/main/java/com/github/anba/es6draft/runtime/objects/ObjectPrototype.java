/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.date.DateObject;
import com.github.anba.es6draft.runtime.objects.number.NumberObject;
import com.github.anba.es6draft.runtime.objects.text.RegExpObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ArgumentsObject;
import com.github.anba.es6draft.runtime.types.builtins.LegacyArgumentsObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.github.anba.es6draft.runtime.types.builtins.StringObject;

/**
 * <h1>19 Fundamental Objects</h1><br>
 * <h2>19.1 Object Objects</h2>
 * <ul>
 * <li>19.1.3 Properties of the Object Prototype Object
 * <li>19.1.4 Properties of Object Instances
 * </ul>
 */
public final class ObjectPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Object prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public ObjectPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
        createProperties(realm, this, AdditionalProperties.class);
    }

    public enum Properties {
        ;

        @Prototype
        public static final ScriptObject __proto__ = null;

        /**
         * 19.1.3.1 Object.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Object;

        /**
         * 19.1.3.6 Object.prototype.toString ( )
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
            if (Type.isUndefined(thisValue)) {
                return "[object Undefined]";
            }
            /* step 2 */
            if (Type.isNull(thisValue)) {
                return "[object Null]";
            }
            /* step 3 */
            ScriptObject o = ToObject(cx, thisValue);
            /* steps 4-5 */
            boolean isArray = IsArray(cx, o);
            /* steps 6-15 */
            String builtinTag;
            if (isArray) {
                builtinTag = "Array";
            } else if (o instanceof StringObject) {
                builtinTag = "String";
            } else if (o instanceof ArgumentsObject || o instanceof LegacyArgumentsObject) {
                builtinTag = "Arguments";
            } else if (o instanceof Callable) {
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
            } else {
                builtinTag = "Object";
            }
            /* steps 16-17 */
            Object ttag = Get(cx, o, BuiltinSymbol.toStringTag.get());
            /* step 18 */
            String tag;
            if (!Type.isString(ttag)) {
                tag = builtinTag;
            } else {
                tag = Type.stringValue(ttag).toString();
            }
            /* step 19 */
            return "[object " + tag + "]";
        }

        /**
         * 19.1.3.5 Object.prototype.toLocaleString ( [ reserved1 [ , reserved2 ] ] )
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
            return Invoke(cx, thisValue, "toString");
        }

        /**
         * 19.1.3.7 Object.prototype.valueOf ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the object value
         */
        @Function(name = "valueOf", arity = 0)
        public static Object valueOf(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            return ToObject(cx, thisValue);
        }

        /**
         * 19.1.3.2 Object.prototype.hasOwnProperty (V)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param v
         *            the property key
         * @return {@code true} if the property is present
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
         * 19.1.3.3 Object.prototype.isPrototypeOf (V)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param v
         *            the script object
         * @return {@code true} if the this-value was found in the prototype chain of <var>v</var>
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
                if (o == _v) {
                    return true;
                }
            }
        }

        /**
         * 19.1.3.4 Object.prototype.propertyIsEnumerable (V)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param v
         *            the property key
         * @return {@code true} if the property is enumerable
         */
        @Function(name = "propertyIsEnumerable", arity = 1)
        public static Object propertyIsEnumerable(ExecutionContext cx, Object thisValue, Object v) {
            /* steps 1-2 */
            Object p = ToPropertyKey(cx, v);
            /* steps 3-4 */
            ScriptObject o = ToObject(cx, thisValue);
            /* steps 5-6 */
            Property desc;
            if (p instanceof String) {
                desc = o.getOwnProperty(cx, (String) p);
            } else {
                desc = o.getOwnProperty(cx, (Symbol) p);
            }
            /* step 7 */
            if (desc == null) {
                return false;
            }
            /* step 8 */
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
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the prototype object
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
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param proto
         *            the new prototype object
         * @return the prototype object
         */
        @Accessor(name = "__proto__", type = Accessor.Type.Setter)
        public static Object setPrototype(ExecutionContext cx, Object thisValue, Object proto) {
            /* steps 1-2 */
            Object o = RequireObjectCoercible(cx, thisValue);
            /* step 3 */
            if (!Type.isObjectOrNull(proto)) {
                return UNDEFINED;
            }
            /* step 4 */
            if (!Type.isObject(o)) {
                return UNDEFINED;
            }
            /* steps 5-6 */
            ScriptObject obj = Type.objectValue(o);
            boolean status = obj.setPrototypeOf(cx, Type.objectValueOrNull(proto));
            /* step 7 */
            if (!status) {
                // provide better error messages for ordinary objects
                if (obj instanceof OrdinaryObject) {
                    if (!obj.isExtensible(cx)) {
                        throw newTypeError(cx, Messages.Key.NotExtensible);
                    }
                    throw newTypeError(cx, Messages.Key.CyclicProto);
                }
                throw newTypeError(cx, Messages.Key.ObjectSetPrototypeFailed);
            }
            /* step 8 */
            return UNDEFINED;
        }
    }
}

/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.AccessorPropertyDescriptor;
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
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ImmutablePrototypeObject;
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
public final class ObjectPrototype extends ImmutablePrototypeObject implements Initializable {
    /**
     * Constructs a new Object prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public ObjectPrototype(Realm realm) {
        super(realm, null);
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
            /* step 4 */
            boolean isArray = IsArray(cx, o);
            /* steps 5-14 (not applicable) */
            /* step 15 */
            Object ttag = Get(cx, o, BuiltinSymbol.toStringTag.get());
            /* step 16 */
            String tag;
            if (!Type.isString(ttag)) {
                tag = isArray ? "Array" : o.className();
            } else {
                tag = Type.stringValue(ttag).toString();
            }
            /* step 17 */
            return StringObject.validateLength(cx, "[object " + tag + "]");
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
            /* step 1 */
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
            /* step 1 */
            Object p = ToPropertyKey(cx, v);
            /* step 2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            return HasOwnProperty(cx, o, p);
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
            /* step 2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            for (;;) {
                /* step 3.a */
                _v = _v.getPrototypeOf(cx);
                /* step 3.b */
                if (_v == null) {
                    return false;
                }
                /* step 3.c */
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
            /* step 1 */
            Object p = ToPropertyKey(cx, v);
            /* step 2 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 3 */
            Property desc = o.getOwnProperty(cx, p);
            /* step 4 */
            if (desc == null) {
                return false;
            }
            /* step 5 */
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
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
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
         * @return the {@code undefined} value
         */
        @Accessor(name = "__proto__", type = Accessor.Type.Setter)
        public static Object setPrototype(ExecutionContext cx, Object thisValue, Object proto) {
            /* step 1 */
            Object o = RequireObjectCoercible(cx, thisValue);
            /* step 2 */
            if (!Type.isObjectOrNull(proto)) {
                return UNDEFINED;
            }
            /* step 3 */
            if (!Type.isObject(o)) {
                return UNDEFINED;
            }
            /* step 4 */
            ScriptObject obj = Type.objectValue(o);
            boolean status = obj.setPrototypeOf(cx, Type.objectValueOrNull(proto));
            /* step 5 */
            if (!status) {
                // provide better error messages for ordinary objects
                if (obj instanceof OrdinaryObject && !(obj instanceof ImmutablePrototypeObject)) {
                    if (!obj.isExtensible(cx)) {
                        throw newTypeError(cx, Messages.Key.NotExtensible);
                    }
                    throw newTypeError(cx, Messages.Key.CyclicProto);
                }
                throw newTypeError(cx, Messages.Key.ObjectSetPrototypeFailed);
            }
            /* step 6 */
            return UNDEFINED;
        }

        /**
         * B.2.2.2 Object.prototype.__defineGetter__ (P, getter)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param p
         *            the property key
         * @param getter
         *            the getter function
         * @return the {@code undefined} value
         */
        @Function(name = "__defineGetter__", arity = 2)
        public static Object __defineGetter__(ExecutionContext cx, Object thisValue, Object p, Object getter) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            if (!IsCallable(getter)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            /* step 3 */
            PropertyDescriptor desc = AccessorPropertyDescriptor((Callable) getter, null, true, true);
            /* step 4 */
            Object key = ToPropertyKey(cx, p);
            /* step 5 */
            DefinePropertyOrThrow(cx, o, key, desc);
            /* step 6 */
            return UNDEFINED;
        }

        /**
         * B.2.2.3 Object.prototype.__defineSetter__ (P, setter)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param p
         *            the property key
         * @param setter
         *            the setter function
         * @return the {@code undefined} value
         */
        @Function(name = "__defineSetter__", arity = 2)
        public static Object __defineSetter__(ExecutionContext cx, Object thisValue, Object p, Object setter) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            if (!IsCallable(setter)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            /* step 3 */
            PropertyDescriptor desc = AccessorPropertyDescriptor(null, (Callable) setter, true, true);
            /* step 4 */
            Object key = ToPropertyKey(cx, p);
            /* step 5 */
            DefinePropertyOrThrow(cx, o, key, desc);
            /* step 6 */
            return UNDEFINED;
        }

        /**
         * B.2.2.4 Object.prototype.__lookupGetter__ (P)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param p
         *            the property key
         * @return the getter function or {@code undefined} if not found
         */
        @Function(name = "__lookupGetter__", arity = 1)
        public static Object __lookupGetter__(ExecutionContext cx, Object thisValue, Object p) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            Object key = ToPropertyKey(cx, p);
            /* step 3 */
            for (;;) {
                /* step 3.a */
                Property desc = o.getOwnProperty(cx, key);
                /* step 3.b */
                if (desc != null) {
                    /* step 3.b.i */
                    if (desc.isAccessorDescriptor()) {
                        return desc.getGetter() != null ? desc.getGetter() : UNDEFINED;
                    }
                    /* step 3.b.ii */
                    return UNDEFINED;
                }
                /* step 3.c */
                o = o.getPrototypeOf(cx);
                /* step 3.d */
                if (o == null) {
                    return UNDEFINED;
                }
            }
        }

        /**
         * B.2.2.5 Object.prototype.__lookupSetter__ (P)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param p
         *            the property key
         * @return the setter function or {@code undefined} if not found
         */
        @Function(name = "__lookupSetter__", arity = 1)
        public static Object __lookupSetter__(ExecutionContext cx, Object thisValue, Object p) {
            /* step 1 */
            ScriptObject o = ToObject(cx, thisValue);
            /* step 2 */
            Object key = ToPropertyKey(cx, p);
            /* step 3 */
            for (;;) {
                /* step 3.a */
                Property desc = o.getOwnProperty(cx, key);
                /* step 3.b */
                if (desc != null) {
                    /* step 3.b.i */
                    if (desc.isAccessorDescriptor()) {
                        return desc.getSetter() != null ? desc.getSetter() : UNDEFINED;
                    }
                    /* step 3.b.ii */
                    return UNDEFINED;
                }
                /* step 3.c */
                o = o.getPrototypeOf(cx);
                /* step 3.d */
                if (o == null) {
                    return UNDEFINED;
                }
            }
        }
    }
}

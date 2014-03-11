/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.Construct;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>22 Indexed Collections</h1><br>
 * <h2>22.2 TypedArray Objects</h2>
 * <ul>
 * <li>22.2.4 The TypedArray Constructors
 * <li>22.2.5 Properties of the TypedArray Constructors
 * </ul>
 */
public final class TypedArrayConstructor extends BuiltinConstructor implements Initialisable {
    /** [[ElementType]] */
    private final ElementType elementType;

    public TypedArrayConstructor(Realm realm, ElementType elementType) {
        super(realm, elementType.getConstructorName());
        this.elementType = elementType;
    }

    /** [[ElementType]] */
    public ElementType getElementType() {
        return elementType;
    }

    @Override
    public void initialise(ExecutionContext cx) {
        switch (elementType) {
        case Int8:
            createProperties(this, cx, Properties_Int8Array.class);
            break;
        case Uint8:
            createProperties(this, cx, Properties_Uint8Array.class);
            break;
        case Uint8C:
            createProperties(this, cx, Properties_Uint8Clamped.class);
            break;
        case Int16:
            createProperties(this, cx, Properties_Int16Array.class);
            break;
        case Uint16:
            createProperties(this, cx, Properties_Uint16Array.class);
            break;
        case Int32:
            createProperties(this, cx, Properties_Int32Array.class);
            break;
        case Uint32:
            createProperties(this, cx, Properties_Uint32Array.class);
            break;
        case Float32:
            createProperties(this, cx, Properties_Float32Array.class);
            break;
        case Float64:
            createProperties(this, cx, Properties_Float64Array.class);
            break;
        default:
            throw new IllegalStateException();
        }
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 22.2.4.1 TypedArray (...args)
     */
    @Override
    public TypedArrayObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* step 1 */
        Object obj = thisValue;
        /* steps 2-3 */
        if (!(obj instanceof TypedArrayObject)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        TypedArrayObject array = (TypedArrayObject) obj;
        /* step 4 */
        if (array.getElementType() != null) {
            throw newTypeError(calleeContext, Messages.Key.InitialisedObject);
        }
        /* step 5 */
        array.setElementType(getElementType());
        /* steps 6-7 */
        Realm realmF = calleeContext.getRealm();
        /* step 8 */
        ScriptObject super_ = realmF.getIntrinsic(Intrinsics.TypedArray);
        assert super_ instanceof TypedArrayConstructorPrototype;
        /* steps 9-10 */
        return ((TypedArrayConstructorPrototype) super_).call(calleeContext, thisValue, args);
    }

    /**
     * 22.2.4.2 new TypedArray (...args)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_Int8Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = ElementType.Int8.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.Int8ArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Int8.size();
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_Uint8Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = ElementType.Uint8.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.Uint8ArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Uint8.size();
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_Uint8Clamped {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = ElementType.Uint8C.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.Uint8ClampedArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Uint8C.size();
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_Int16Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = ElementType.Int16.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.Int16ArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Int16.size();
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_Uint16Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = ElementType.Uint16.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.Uint16ArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Uint16.size();
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_Int32Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = ElementType.Int32.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.Int32ArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Int32.size();
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_Uint32Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = ElementType.Uint32.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.Uint32ArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Uint32.size();
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_Float32Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = ElementType.Float32.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.Float32ArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Float32.size();
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_Float64Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = ElementType.Float64.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.Float64ArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Float64.size();
    }
}

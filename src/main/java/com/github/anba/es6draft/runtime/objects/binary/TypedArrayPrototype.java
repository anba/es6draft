/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>22 Indexed Collections</h1><br>
 * <h2>22.2 TypedArray Objects</h2>
 * <ul>
 * <li>22.2.6 Properties of the TypedArray Prototype Object
 * </ul>
 */
public final class TypedArrayPrototype extends OrdinaryObject implements Initializable {
    private final ElementType elementType;

    /**
     * Constructs a new TypedArray prototype object.
     * 
     * @param realm
     *            the realm object
     * @param elementType
     *            the typed array element type
     */
    public TypedArrayPrototype(Realm realm, ElementType elementType) {
        super(realm);
        this.elementType = elementType;
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, propertiesForType(elementType));
    }

    private static Class<?> propertiesForType(ElementType elementType) {
        switch (elementType) {
        case Int8:
            return Properties_Int8Array.class;
        case Uint8:
            return Properties_Uint8Array.class;
        case Uint8C:
            return Properties_Uint8Clamped.class;
        case Int16:
            return Properties_Int16Array.class;
        case Uint16:
            return Properties_Uint16Array.class;
        case Int32:
            return Properties_Int32Array.class;
        case Uint32:
            return Properties_Uint32Array.class;
        case BigInt64:
            return Properties_BigInt64Array.class;
        case BigUint64:
            return Properties_BigUint64Array.class;
        case Float32:
            return Properties_Float32Array.class;
        case Float64:
            return Properties_Float64Array.class;
        default:
            throw new AssertionError();
        }
    }

    /**
     * 22.2.6 Properties of the TypedArray Prototype Object
     */
    public enum Properties_Int8Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArrayPrototype;

        /**
         * 22.2.6.2 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Int8Array;

        /**
         * 22.2.6.1 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Int8.size();
    }

    /**
     * 22.2.6 Properties of the TypedArray Prototype Object
     */
    public enum Properties_Uint8Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArrayPrototype;

        /**
         * 22.2.6.2 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Uint8Array;

        /**
         * 22.2.6.1 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Uint8.size();
    }

    /**
     * 22.2.6 Properties of the TypedArray Prototype Object
     */
    public enum Properties_Uint8Clamped {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArrayPrototype;

        /**
         * 22.2.6.2 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Uint8ClampedArray;

        /**
         * 22.2.6.1 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Uint8C.size();
    }

    /**
     * 22.2.6 Properties of the TypedArray Prototype Object
     */
    public enum Properties_Int16Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArrayPrototype;

        /**
         * 22.2.6.2 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Int16Array;

        /**
         * 22.2.6.1 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Int16.size();
    }

    /**
     * 22.2.6 Properties of the TypedArray Prototype Object
     */
    public enum Properties_Uint16Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArrayPrototype;

        /**
         * 22.2.6.2 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Uint16Array;

        /**
         * 22.2.6.1 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Uint16.size();
    }

    /**
     * 22.2.6 Properties of the TypedArray Prototype Object
     */
    public enum Properties_Int32Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArrayPrototype;

        /**
         * 22.2.6.2 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Int32Array;

        /**
         * 22.2.6.1 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Int32.size();
    }

    /**
     * 22.2.6 Properties of the TypedArray Prototype Object
     */
    public enum Properties_Uint32Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArrayPrototype;

        /**
         * 22.2.6.2 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Uint32Array;

        /**
         * 22.2.6.1 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Uint32.size();
    }

    /**
     * 22.2.6 Properties of the TypedArray Prototype Object
     */
    public enum Properties_Float32Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArrayPrototype;

        /**
         * 22.2.6.2 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Float32Array;

        /**
         * 22.2.6.1 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Float32.size();
    }

    /**
     * 22.2.6 Properties of the TypedArray Prototype Object
     */
    public enum Properties_Float64Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArrayPrototype;

        /**
         * 22.2.6.2 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Float64Array;

        /**
         * 22.2.6.1 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Float64.size();
    }

    /**
     * 22.2.6 Properties of the TypedArray Prototype Object
     */
    public enum Properties_BigInt64Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArrayPrototype;

        /**
         * 22.2.6.2 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.BigInt64Array;

        /**
         * 22.2.6.1 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.BigInt64.size();
    }

    /**
     * 22.2.6 Properties of the TypedArray Prototype Object
     */
    public enum Properties_BigUint64Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArrayPrototype;

        /**
         * 22.2.6.2 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.BigUint64Array;

        /**
         * 22.2.6.1 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.BigUint64.size();
    }
}

/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import com.github.anba.es6draft.runtime.ExecutionContext;
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
    private final ElementType elementKind;

    /**
     * Constructs a new TypedArray prototype object.
     * 
     * @param realm
     *            the realm object
     * @param elementKind
     *            the typed array element type
     */
    public TypedArrayPrototype(Realm realm, ElementType elementKind) {
        super(realm);
        this.elementKind = elementKind;
    }

    @Override
    public void initialize(ExecutionContext cx) {
        switch (elementKind) {
        case Int8:
            createProperties(cx, this, Properties_Int8Array.class);
            break;
        case Uint8:
            createProperties(cx, this, Properties_Uint8Array.class);
            break;
        case Uint8C:
            createProperties(cx, this, Properties_Uint8Clamped.class);
            break;
        case Int16:
            createProperties(cx, this, Properties_Int16Array.class);
            break;
        case Uint16:
            createProperties(cx, this, Properties_Uint16Array.class);
            break;
        case Int32:
            createProperties(cx, this, Properties_Int32Array.class);
            break;
        case Uint32:
            createProperties(cx, this, Properties_Uint32Array.class);
            break;
        case Float32:
            createProperties(cx, this, Properties_Float32Array.class);
            break;
        case Float64:
            createProperties(cx, this, Properties_Float64Array.class);
            break;
        default:
            throw new IllegalStateException();
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
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
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
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
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
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
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
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
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
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
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
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
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
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
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
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
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
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Float64.size();
    }
}

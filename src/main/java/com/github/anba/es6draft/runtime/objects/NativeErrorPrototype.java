/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.NativeErrorConstructor.ErrorType;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.11 Error Objects</h2>
 * <ul>
 * <li>15.11.7 NativeError Object Structure
 * <ul>
 * <li>15.11.7.4 Properties of the NativeError Prototype Objects
 * </ul>
 * </ul>
 */
public class NativeErrorPrototype extends OrdinaryObject implements ScriptObject, Initialisable {
    private final ErrorType type;

    public NativeErrorPrototype(Realm realm, ErrorType type) {
        super(realm);
        this.type = type;
    }

    @Override
    public void initialise(Realm realm) {
        switch (type) {
        case EvalError:
            createProperties(this, realm, EvalErrorPrototypeProperties.class);
            break;
        case RangeError:
            createProperties(this, realm, RangeErrorPrototypeProperties.class);
            break;
        case ReferenceError:
            createProperties(this, realm, ReferenceErrorPrototypeProperties.class);
            break;
        case SyntaxError:
            createProperties(this, realm, SyntaxErrorPrototypeProperties.class);
            break;
        case TypeError:
            createProperties(this, realm, TypeErrorPrototypeProperties.class);
            break;
        case URIError:
            createProperties(this, realm, URIErrorPrototypeProperties.class);
            break;
        case InternalError:
            createProperties(this, realm, InternalErrorPrototypeProperties.class);
            break;
        default:
            throw new IllegalStateException();
        }
    }

    /**
     * 15.11.7.4 Properties of the NativeError Prototype Objects
     */
    public enum EvalErrorPrototypeProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ErrorPrototype;

        /**
         * 15.11.7.4.1 NativeError.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.EvalError;

        /**
         * 15.11.7.4.2 NativeError.prototype.name
         */
        @Value(name = "name")
        public static final String name = "EvalError";

        /**
         * 15.11.7.4.3 NativeError.prototype.message
         */
        @Value(name = "message")
        public static final String message = "";
    }

    /**
     * 15.11.7.4 Properties of the NativeError Prototype Objects
     */
    public enum RangeErrorPrototypeProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ErrorPrototype;

        /**
         * 15.11.7.4.1 NativeError.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.RangeError;

        /**
         * 15.11.7.4.2 NativeError.prototype.name
         */
        @Value(name = "name")
        public static final String name = "RangeError";

        /**
         * 15.11.7.4.3 NativeError.prototype.message
         */
        @Value(name = "message")
        public static final String message = "";
    }

    /**
     * 15.11.7.4 Properties of the NativeError Prototype Objects
     */
    public enum ReferenceErrorPrototypeProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ErrorPrototype;

        /**
         * 15.11.7.4.1 NativeError.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.ReferenceError;

        /**
         * 15.11.7.4.2 NativeError.prototype.name
         */
        @Value(name = "name")
        public static final String name = "ReferenceError";

        /**
         * 15.11.7.4.3 NativeError.prototype.message
         */
        @Value(name = "message")
        public static final String message = "";
    }

    /**
     * 15.11.7.4 Properties of the NativeError Prototype Objects
     */
    public enum SyntaxErrorPrototypeProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ErrorPrototype;

        /**
         * 15.11.7.4.1 NativeError.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.SyntaxError;

        /**
         * 15.11.7.4.2 NativeError.prototype.name
         */
        @Value(name = "name")
        public static final String name = "SyntaxError";

        /**
         * 15.11.7.4.3 NativeError.prototype.message
         */
        @Value(name = "message")
        public static final String message = "";
    }

    /**
     * 15.11.7.4 Properties of the NativeError Prototype Objects
     */
    public enum TypeErrorPrototypeProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ErrorPrototype;

        /**
         * 15.11.7.4.1 NativeError.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.TypeError;

        /**
         * 15.11.7.4.2 NativeError.prototype.name
         */
        @Value(name = "name")
        public static final String name = "TypeError";

        /**
         * 15.11.7.4.3 NativeError.prototype.message
         */
        @Value(name = "message")
        public static final String message = "";
    }

    /**
     * 15.11.7.4 Properties of the NativeError Prototype Objects
     */
    public enum URIErrorPrototypeProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ErrorPrototype;

        /**
         * 15.11.7.4.1 NativeError.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.URIError;

        /**
         * 15.11.7.4.2 NativeError.prototype.name
         */
        @Value(name = "name")
        public static final String name = "URIError";

        /**
         * 15.11.7.4.3 NativeError.prototype.message
         */
        @Value(name = "message")
        public static final String message = "";
    }

    /**
     * 15.11.7.4 Properties of the NativeError Prototype Objects
     */
    public enum InternalErrorPrototypeProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ErrorPrototype;

        /**
         * 15.11.7.4.1 NativeError.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.InternalError;

        /**
         * 15.11.7.4.2 NativeError.prototype.name
         */
        @Value(name = "name")
        public static final String name = "InternalError";

        /**
         * 15.11.7.4.3 NativeError.prototype.message
         */
        @Value(name = "message")
        public static final String message = "";
    }
}

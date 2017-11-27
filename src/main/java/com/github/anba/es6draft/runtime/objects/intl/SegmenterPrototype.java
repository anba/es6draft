/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.intl.SegmentIteratorPrototype.CreateSegmentIterator;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>Segmenter Objects</h1>
 * <ul>
 * <li>Properties of the Intl.Segmenter Prototype Object
 * </ul>
 */
public final class SegmenterPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Segmenter prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public SegmenterPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * Properties of the Intl.Segmenter Prototype Object
     */
    public enum Properties {
        ;

        private static SegmenterObject thisSegmenterObject(ExecutionContext cx, Object value, String method) {
            if (value instanceof SegmenterObject) {
                return (SegmenterObject) value;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * Intl.Segmenter.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Intl_Segmenter;

        /**
         * Intl.Segmenter.prototype.segment( string )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param string
         *            the string value
         * @return the segmenter iterator object
         */
        @Function(name = "segment", arity = 1)
        public static Object segment(ExecutionContext cx, Object thisValue, Object string) {
            /* steps 1-2 */
            SegmenterObject segmenter = thisSegmenterObject(cx, thisValue, "Intl.Segmenter.prototype.segment");
            /* step 3 */
            String str = ToFlatString(cx, string);
            /* step 4 */
            return CreateSegmentIterator(cx, segmenter, str);
        }

        /**
         * Intl.Segmenter.prototype.resolvedOptions ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the resolved options object
         */
        @Function(name = "resolvedOptions", arity = 0)
        public static Object resolvedOptions(ExecutionContext cx, Object thisValue) {
            SegmenterObject segmenter = thisSegmenterObject(cx, thisValue, "Intl.Segmenter.prototype.resolvedOptions");
            OrdinaryObject object = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            CreateDataProperty(cx, object, "locale", segmenter.getLocale());
            CreateDataProperty(cx, object, "granularity", segmenter.getGranularity());
            if (segmenter.getStrictness() != null) {
                CreateDataProperty(cx, object, "strictness", segmenter.getStrictness());
            }
            return object;
        }
    }
}

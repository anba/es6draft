/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.text;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.text.RegExpPrototype.RegExpExec;
import static com.github.anba.es6draft.runtime.types.Null.NULL;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Extension: String.prototype.matchAll
 */
public final class RegExpStringIteratorPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new RegExp String Iterator prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public RegExpStringIteratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * MatchAllIterator ( R, O )
     * 
     * @param cx
     *            the execution context
     * @param r
     *            the regular expression object
     * @param o
     *            the string value
     * @return the new regexp string iterator
     */
    public static OrdinaryObject MatchAllIterator(ExecutionContext cx, Object r, Object o) {
        /* step 1 */
        if (!IsRegExp(cx, r)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        ScriptObject rObj = Type.objectValue(r);
        /* step 2 */
        String s = ToFlatString(cx, o);
        /* step 3 */
        Constructor constructor = SpeciesConstructor(cx, rObj, Intrinsics.RegExp);
        /* step 4 */
        String flags = ToFlatString(cx, Get(cx, rObj, "flags"));

        // FIXME: spec bug - global flag not added.
        if (flags.indexOf('g') < 0) {
            flags = flags + "g";
        }

        /* step 5 */
        ScriptObject matcher = constructor.construct(cx, rObj, flags);
        /* step 6 */
        long lastIndex = ToLength(cx, Get(cx, rObj, "lastIndex"));
        /* step 7 */
        Set(cx, matcher, "lastIndex", lastIndex, true);
        /* step 8 */
        return CreateRegExpStringIterator(cx, matcher, s);
    }

    /**
     * CreateRegExpStringIterator( regexp, string )
     * 
     * @param cx
     *            the execution context
     * @param regexp
     *            the regular expression object
     * @param string
     *            the string value
     * @return the new regexp string iterator
     */
    public static OrdinaryObject CreateRegExpStringIterator(ExecutionContext cx, ScriptObject regexp, String string) {
        /* step 1 (implicit) */
        /* steps 2-7 */
        return new RegExpStringIteratorObject(cx.getRealm(), regexp, string,
                cx.getIntrinsic(Intrinsics.RegExpStringIteratorPrototype));
    }

    /**
     * The %RegExpStringIteratorPrototype% Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.IteratorPrototype;

        /**
         * %RegExpStringIteratorPrototype%.next( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the next iterator result object
         */
        @Function(name = "next", arity = 0)
        public static Object next(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            if (!(thisValue instanceof RegExpStringIteratorObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleThis, "%RegExpStringIteratorPrototype%.next",
                        Type.of(thisValue).toString());
            }
            RegExpStringIteratorObject iterator = (RegExpStringIteratorObject) thisValue;
            /* step 4 */
            if (iterator.isDone()) {
                return CreateIterResultObject(cx, NULL, true);
            }
            /* step 5 */
            ScriptObject regexp = iterator.getIteratedRegExp();
            /* step 6 */
            String string = iterator.getIteratedString();
            /* step 7 */
            ScriptObject match = RegExpExec(cx, regexp, string);
            /* step 8 */
            if (match == null) {
                /* step 8.a */
                iterator.setDone(true);
                /* step 8.b */
                return CreateIterResultObject(cx, NULL, true);
            }
            /* steps 9.a-b */
            long previousIndex = iterator.getPreviousIndex();
            /* step 9.c */
            long index = ToLength(cx, Get(cx, match, "index"));
            /* step 9.d */
            if (previousIndex == index) {
                /* step 9.d.i */
                iterator.setDone(true);
                /* step 9.d.ii */
                return CreateIterResultObject(cx, NULL, true);
            } else {
                /* step 9.e.i */
                iterator.setPreviousIndex(index);
                /* step 9.e.ii */
                return CreateIterResultObject(cx, match, false);
            }
        }

        /**
         * %RegExpStringIteratorPrototype% [@@toStringTag]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "RegExp String Iterator";
    }
}

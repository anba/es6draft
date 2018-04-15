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
import static com.github.anba.es6draft.runtime.objects.text.RegExpConstructor.RegExpCreate;
import static com.github.anba.es6draft.runtime.objects.text.RegExpPrototype.RegExpExec;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

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
        String s = ToFlatString(cx, o);
        /* step 2 */
        ScriptObject matcher;
        boolean global, fullUnicode;
        if (IsRegExp(cx, r)) {
            ScriptObject rObj = Type.objectValue(r);
            /* step 2.a */
            Constructor constructor = SpeciesConstructor(cx, rObj, Intrinsics.RegExp);
            /* step 2.b */
            String flags = ToFlatString(cx, Get(cx, rObj, "flags"));
            /* step 2.c */
            matcher = constructor.construct(cx, rObj, flags);
            /* step 2.d */
            global = ToBoolean(Get(cx, matcher, "global"));
            /* step 2.e */
            fullUnicode = ToBoolean(Get(cx, matcher, "unicode"));
            /* step 2.f */
            long lastIndex = ToLength(cx, Get(cx, rObj, "lastIndex"));
            /* step 2.g */
            Set(cx, matcher, "lastIndex", lastIndex, true);
        } else {
            /* step 3.a */
            String flags = "g";
            /* step 3.b */
            RegExpObject matcherRx = RegExpCreate(cx, r, flags);
            matcher = matcherRx;
            /* step 3.c */
            if (!IsRegExp(cx, matcher)) {
                throw newTypeError(cx, Messages.Key.InvalidRegExpArgument);
            }
            /* step 3.d */
            global = true;
            /* step 3.e */
            fullUnicode = false;
            /* step 3.f */
            if (!StrictEqualityComparison(matcherRx.getLastIndex().getValue(), 0)) {
                throw newTypeError(cx, Messages.Key.InvalidRegExpArgument);
            }
        }
        /* step 4 */
        return CreateRegExpStringIterator(cx, matcher, s, global, fullUnicode);
    }

    /**
     * CreateRegExpStringIterator( regexp, string, global, fullUnicode )
     * 
     * @param cx
     *            the execution context
     * @param regexp
     *            the regular expression object
     * @param string
     *            the string value
     * @param global
     *            the global flag
     * @param fullUnicode
     *            the unicode flag
     * @return the new regexp string iterator
     */
    public static OrdinaryObject CreateRegExpStringIterator(ExecutionContext cx, ScriptObject regexp, String string,
            boolean global, boolean fullUnicode) {
        /* step 1 (implicit) */
        /* steps 2-7 */
        return new RegExpStringIteratorObject(cx.getRealm(), regexp, string, global, fullUnicode,
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
                return CreateIterResultObject(cx, UNDEFINED, true);
            }
            /* step 5 */
            ScriptObject regexp = iterator.getIteratedRegExp();
            /* step 6 */
            String string = iterator.getIteratedString();
            /* step 7 */
            boolean global = iterator.isGlobal();
            /* step 8 */
            boolean fullUnicode = iterator.isUnicode();
            /* step 9 */
            ScriptObject match = RegExpExec(cx, regexp, string);
            /* step 10 */
            if (match == null) {
                /* step 10.a */
                iterator.setDone(true);
                /* step 10.b */
                return CreateIterResultObject(cx, UNDEFINED, true);
            } else {
                /* steps 11.a-b */
                if (global) {
                    /* step 11.a.i */
                    CharSequence matchStr = ToString(cx, Get(cx, match, 0));
                    /* step 11.a.ii */
                    if (matchStr.length() == 0) {
                        long thisIndex = ToLength(cx, Get(cx, regexp, "lastIndex"));
                        long nextIndex = RegExpPrototype.AdvanceStringIndex(string, thisIndex, fullUnicode);
                        Set(cx, regexp, "lastIndex", nextIndex, true);
                    }
                    /* step 11.a.iii */
                    return CreateIterResultObject(cx, match, false);
                } else {
                    /* step 11.b */
                    /* step 11.b.i */
                    iterator.setDone(true);
                    /* step 11.b.ii */
                    return CreateIterResultObject(cx, match, false);
                }
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

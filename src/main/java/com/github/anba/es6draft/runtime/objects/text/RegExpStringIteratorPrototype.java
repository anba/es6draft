/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.text;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateIterResultObject;
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
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
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
    public static OrdinaryObject CreateRegExpStringIterator(ExecutionContext cx, RegExpObject regexp, String string) {
        /* step 1 (FIXME: spec bug - invalid assertion) */
        /* step 2 (FIXME: spec bug - invalid assertion) */
        /* step 3 (not applicable) */
        /* steps 4-7 */
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
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            RegExpStringIteratorObject iterator = (RegExpStringIteratorObject) thisValue;
            /* step 4 */
            RegExpObject regexp = iterator.getIteratedRegExp();
            /* step 5 */
            String string = iterator.getIteratedString();
            /* step 6 */
            // FIXME: spec bug - missing ReturnIfAbrupt
            ScriptObject match = RegExpExec(cx, regexp, string);
            /* steps 7-8 */
            if (match == null) {
                /* step 7 */
                return CreateIterResultObject(cx, NULL, true);
            } else {
                /* step 8 */
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

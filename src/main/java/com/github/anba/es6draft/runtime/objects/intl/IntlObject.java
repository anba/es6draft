/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>8 The Intl Object</h1>
 */
public class IntlObject extends OrdinaryObject implements ScriptObject, Initialisable {
    public IntlObject(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
    }

    /**
     * 8.1 Properties of the Intl Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        @Value(name = "Collator")
        public static final Intrinsics Collator = Intrinsics.Intl_Collator;

        @Value(name = "NumberFormat")
        public static final Intrinsics NumberFormat = Intrinsics.Intl_NumberFormat;

        @Value(name = "DateTimeFormat")
        public static final Intrinsics DateTimeFormat = Intrinsics.Intl_DateTimeFormat;
    }
}

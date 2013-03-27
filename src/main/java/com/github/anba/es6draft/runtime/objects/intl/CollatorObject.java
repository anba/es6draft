/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>10 Collator Objects</h1>
 * <ul>
 * <li>10.4 Properties of Intl.Collator Instances
 * </ul>
 */
public class CollatorObject extends OrdinaryObject {
    /**
     * [[initializedIntlObject]]
     */
    private boolean initialized;

    public CollatorObject(Realm realm) {
        super(realm);
    }

    /**
     * [[initializedIntlObject]]
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * [[initializedIntlObject]]
     */
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
}

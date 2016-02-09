/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.text;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Extension: String.prototype.matchAll
 */
public final class RegExpStringIteratorObject extends OrdinaryObject {
    /** [[IteratingRegExp]] */
    private final RegExpObject iteratedRegExp;

    /** [[IteratedString]] */
    private final String iteratedString;

    RegExpStringIteratorObject(Realm realm, RegExpObject regexp, String string, ScriptObject prototype) {
        super(realm);
        this.iteratedRegExp = regexp;
        this.iteratedString = string;
        setPrototype(prototype);
    }

    /**
     * [[IteratingRegExp]]
     * 
     * @return the iterated regexp
     */
    public RegExpObject getIteratedRegExp() {
        return iteratedRegExp;
    }

    /**
     * [[IteratedString]]
     * 
     * @return the iterated string
     */
    public String getIteratedString() {
        return iteratedString;
    }
}

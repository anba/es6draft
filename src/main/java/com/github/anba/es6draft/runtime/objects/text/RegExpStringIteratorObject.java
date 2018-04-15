/**
 * Copyright (c) André Bargull
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
    private final ScriptObject iteratedRegExp;

    /** [[IteratedString]] */
    private final String iteratedString;

    /** [[Global]] */
    private boolean global;

    /** [[Unicode]] */
    private boolean unicode;

    /** [[Done]] */
    private boolean done;

    RegExpStringIteratorObject(Realm realm, ScriptObject regexp, String string, boolean global, boolean unicode,
            ScriptObject prototype) {
        super(realm, prototype);
        this.iteratedRegExp = regexp;
        this.iteratedString = string;
        this.global = global;
        this.unicode = unicode;
        this.done = false;
    }

    /**
     * [[IteratingRegExp]]
     * 
     * @return the iterated regexp
     */
    public ScriptObject getIteratedRegExp() {
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

    /**
     * [[Global]]
     * 
     * @return the global flag
     */
    public boolean isGlobal() {
        return global;
    }

    /**
     * [[Unicode]]
     * 
     * @return the unicode flag
     */
    public boolean isUnicode() {
        return unicode;
    }

    /**
     * [[Done]]
     * 
     * @return the done flag
     */
    public boolean isDone() {
        return done;
    }

    /**
     * [[Done]]
     * 
     * @param done
     *            the new done value
     */
    public void setDone(boolean done) {
        this.done = done;
    }
}

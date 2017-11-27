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
 * 21.1.5.3 Properties of String Iterator Instances
 */
public final class StringIteratorObject extends OrdinaryObject {
    /** [[IteratedString]] */
    private String iteratedString;

    /** [[StringIteratorNextIndex]] */
    private int nextIndex;

    StringIteratorObject(Realm realm, String string, ScriptObject prototype) {
        this(realm, string, 0, prototype);
    }

    StringIteratorObject(Realm realm, String string, int index, ScriptObject prototype) {
        super(realm, prototype);
        this.iteratedString = string;
        this.nextIndex = index;
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
     * [[IteratedString]]
     * 
     * @param iteratedString
     *            the iterated string
     */
    public void setIteratedString(String iteratedString) {
        this.iteratedString = iteratedString;
    }

    /**
     * [[StringIteratorNextIndex]]
     * 
     * @return the next string index
     */
    public int getNextIndex() {
        return nextIndex;
    }

    /**
     * [[StringIteratorNextIndex]]
     * 
     * @param nextIndex
     *            the next string index
     */
    public void setNextIndex(int nextIndex) {
        this.nextIndex = nextIndex;
    }

    @Override
    public String toString() {
        return String.format("%s, iteratedString=%s, nextIndex=%d", super.toString(), iteratedString, nextIndex);
    }
}

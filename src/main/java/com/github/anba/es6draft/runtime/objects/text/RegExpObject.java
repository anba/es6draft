/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.text;

import com.github.anba.es6draft.regexp.RegExpMatcher;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>21 Text Processing</h1><br>
 * <h2>21.2 RegExp (Regular Expression) Objects</h2>
 * <ul>
 * <li>21.2.6 Properties of RegExp Instances
 * </ul>
 */
public final class RegExpObject extends OrdinaryObject {
    /** [[OriginalSource]] */
    private String originalSource = null;

    /** [[OriginalFlags]] */
    private String originalFlags = null;

    /** [[RegExpMatcher]] */
    private RegExpMatcher regExpMatcher = null;

    public RegExpObject(Realm realm) {
        super(realm);
    }

    protected void initialise(String originalSource, String originalFlags, RegExpMatcher matcher) {
        this.originalSource = originalSource;
        this.originalFlags = originalFlags;
        this.regExpMatcher = matcher;
    }

    protected boolean isInitialised() {
        return regExpMatcher != null;
    }

    /**
     * [[OriginalSource]]
     * 
     * @return the regular expression source
     */
    public String getOriginalSource() {
        assert originalSource != null;
        return originalSource;
    }

    /**
     * [[OriginalFlags]]
     * 
     * @return the regular expression flags
     */
    public String getOriginalFlags() {
        assert originalFlags != null;
        return originalFlags;
    }

    /**
     * [[RegExpMatcher]]
     * 
     * @return the regular expression matcher object
     */
    public RegExpMatcher getRegExpMatcher() {
        assert regExpMatcher != null;
        return regExpMatcher;
    }
}

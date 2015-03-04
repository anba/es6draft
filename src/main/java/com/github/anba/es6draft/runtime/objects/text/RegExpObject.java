/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.text;

import java.util.Objects;

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
    private String originalSource;

    /** [[OriginalFlags]] */
    private String originalFlags;

    /** [[RegExpMatcher]] */
    private RegExpMatcher regExpMatcher;

    /**
     * Constructs a new RegExp object.
     * 
     * @param realm
     *            the realm object
     */
    public RegExpObject(Realm realm) {
        super(realm);
    }

    void initialize(String originalSource, String originalFlags, RegExpMatcher matcher) {
        this.originalSource = originalSource;
        this.originalFlags = originalFlags;
        this.regExpMatcher = matcher;
    }

    /**
     * [[OriginalSource]]
     * 
     * @return the regular expression source
     */
    public String getOriginalSource() {
        return originalSource;
    }

    /**
     * [[OriginalFlags]]
     * 
     * @return the regular expression flags
     */
    public String getOriginalFlags() {
        return originalFlags;
    }

    /**
     * [[RegExpMatcher]]
     * 
     * @return the regular expression matcher object
     */
    public RegExpMatcher getRegExpMatcher() {
        return regExpMatcher;
    }

    @Override
    public String toString() {
        return String.format("%s, matcher={%s}", super.toString(), Objects.toString(regExpMatcher));
    }
}

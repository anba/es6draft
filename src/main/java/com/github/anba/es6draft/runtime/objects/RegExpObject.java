/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import java.util.BitSet;
import java.util.regex.Pattern;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.10 RegExp (Regular Expression) Objects</h2>
 * <ul>
 * <li>15.10.7 Properties of RegExp Instances
 * </ul>
 */
public class RegExpObject extends OrdinaryObject implements Scriptable {
    private String pattern;
    private String flags;
    private BitSet negativeLAGroups;

    /**
     * [[Match]]
     */
    private Pattern match;

    public RegExpObject(Realm realm) {
        super(realm);
        /* uninitialised regexp object */
    }

    public RegExpObject(Realm realm, String pattern, String flags, Pattern match,
            BitSet negativeLAGroups) {
        super(realm);
        initialise(pattern, flags, match, negativeLAGroups);
    }

    protected void initialise(String pattern, String flags, Pattern match, BitSet negativeLAGroups) {
        this.pattern = pattern;
        this.flags = flags;
        this.match = match;
        this.negativeLAGroups = negativeLAGroups;
    }

    protected boolean isInitialised() {
        return match != null;
    }

    protected String getPattern() {
        assert pattern != null;
        return pattern;
    }

    protected String getFlags() {
        assert flags != null;
        return flags;
    }

    protected BitSet getNegativeLookaheadGroups() {
        assert negativeLAGroups != null;
        return negativeLAGroups;
    }

    /**
     * [[Match]]
     */
    public Pattern getMatch() {
        assert match != null;
        return match;
    }

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        return BuiltinBrand.BuiltinRegExp;
    }
}

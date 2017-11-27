/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.text;

import java.util.Objects;

import com.github.anba.es6draft.regexp.RegExpMatcher;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.ScriptObject;
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
    private int flags;

    /** [[RegExpMatcher]] */
    private RegExpMatcher regExpMatcher;

    /** [[Realm]] */
    private final Realm realm;

    /** [[LegacyFeaturesEnabled]] */
    private final boolean legacyFeaturesEnabled;

    private final Property lastIndex;

    /**
     * Constructs a new RegExp object.
     * 
     * @param realm
     *            the realm object
     * @param legacyFeaturesEnabled
     *            the legacyFeaturesEnabled flag
     * @param prototype
     *            the prototype object
     */
    public RegExpObject(Realm realm, boolean legacyFeaturesEnabled, ScriptObject prototype) {
        super(realm, prototype);
        this.realm = realm;
        this.legacyFeaturesEnabled = legacyFeaturesEnabled;
        infallibleDefineOwnProperty("lastIndex", lastIndex = new Property(0, true, false, false));
    }

    void initialize(String originalSource, String originalFlags, RegExpMatcher matcher) {
        this.originalSource = originalSource;
        this.originalFlags = originalFlags;
        this.flags = Flags.from(originalFlags);
        this.regExpMatcher = matcher;
    }

    enum Flags {
        Global, IgnoreCase, Multiline, Sticky, Unicode, DotAll;

        int mask() {
            return 1 << ordinal();
        }

        boolean isSet(int flags) {
            return (flags & mask()) != 0;
        }

        static Flags of(char c) {
            switch (c) {
            case 'g':
                return Global;
            case 'i':
                return IgnoreCase;
            case 'm':
                return Multiline;
            case 's':
                return DotAll;
            case 'u':
                return Unicode;
            case 'y':
                return Sticky;
            default:
                throw new AssertionError();
            }
        }

        static int from(String flags) {
            int f = 0;
            for (int i = 0; i < flags.length(); ++i) {
                f |= of(flags.charAt(i)).mask();
            }
            return f;
        }
    }

    boolean isSet(Flags flag) {
        return flag.isSet(flags);
    }

    Property getLastIndex() {
        return lastIndex;
    }

    Realm getRealm() {
        return realm;
    }

    boolean isLegacyFeaturesEnabled() {
        return legacyFeaturesEnabled;
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
    public String className() {
        return "RegExp";
    }

    @Override
    public String toString() {
        return String.format("%s, matcher={%s}", super.toString(), Objects.toString(regExpMatcher));
    }
}

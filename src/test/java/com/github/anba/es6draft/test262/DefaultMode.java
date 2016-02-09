/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

/**
 * Test mode enumeration.
 */
enum DefaultMode {
    Strict, NonStrict, Both;

    static DefaultMode forName(String name) {
        switch (name) {
        case "strict":
            return Strict;
        case "non_strict":
            return NonStrict;
        case "both":
            return Both;
        default:
            throw new IllegalArgumentException(name);
        }
    }
}

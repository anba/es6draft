/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse,
} = Assert;

// 21.2.2.8.2 Canonicalize: Incorrect statement in note
// https://bugs.ecmascript.org/show_bug.cgi?id=2933

// LATIN SMALL LETTER LONG S (U+017F) matches a-z (A-Z) only if unicode flag is enabled
assertFalse(/[a-z]/i.test("\u017F"));
assertFalse(/[A-Z]/i.test("\u017F"));

assertTrue(/[a-z]/iu.test("\u017F"));
assertTrue(/[A-Z]/iu.test("\u017F"));


// KELVIN SIGN S (U+212A) matches a-z (A-Z) only if unicode flag is enabled
assertFalse(/[a-z]/i.test("\u212A"));
assertFalse(/[A-Z]/i.test("\u212A"));

assertTrue(/[a-z]/iu.test("\u212A"));
assertTrue(/[A-Z]/iu.test("\u212A"));


// LATIN CAPITAL LETTER I WITH DOT ABOVE (U+0130) does not match a-z (A-Z) even if unicode flag is enabled
assertFalse(/[a-z]/i.test("\u0130"));
assertFalse(/[A-Z]/i.test("\u0130"));

assertFalse(/[a-z]/iu.test("\u0130"));
assertFalse(/[A-Z]/iu.test("\u0130"));


// LATIN SMALL LETTER DOTLESS I (U+0131) does not match a-z (A-Z) even if unicode flag is enabled
assertFalse(/[a-z]/i.test("\u0131"));
assertFalse(/[A-Z]/i.test("\u0131"));

assertFalse(/[a-z]/iu.test("\u0131"));
assertFalse(/[A-Z]/iu.test("\u0131"));

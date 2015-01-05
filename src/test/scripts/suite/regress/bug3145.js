/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse,
} = Assert;

// 21.2.2.8.2: clarify whether this affects `\w` and `\W` or not
// https://bugs.ecmascript.org/show_bug.cgi?id=3145

// LATIN SMALL LETTER LONG S (U+017F) matches \w (and \W) if unicode flag is enabled
assertFalse(/\w/i.test("\u017F"));
assertTrue(/\w/iu.test("\u017F"));
assertTrue(/\W/i.test("\u017F"));
assertTrue(/\W/iu.test("\u017F"));


// KELVIN SIGN S (U+212A) matches \w (and \W) if unicode flag is enabled
assertFalse(/\w/i.test("\u212A"));
assertTrue(/\w/iu.test("\u212A"));
assertTrue(/\W/i.test("\u212A"));
assertTrue(/\W/iu.test("\u212A"));


// LATIN CAPITAL LETTER I WITH DOT ABOVE (U+0130) does not match \w even if unicode flag is enabled
assertFalse(/\w/i.test("\u0130"));
assertFalse(/\w/iu.test("\u0130"));
assertTrue(/\W/i.test("\u0130"));
assertTrue(/\W/iu.test("\u0130"));


// LATIN SMALL LETTER DOTLESS I (U+0131) does not match \w even if unicode flag is enabled
assertFalse(/\w/i.test("\u0130"));
assertFalse(/\w/iu.test("\u0130"));
assertTrue(/\W/i.test("\u0130"));
assertTrue(/\W/iu.test("\u0130"));


// LATIN CAPITAL LETTER S (U+0053) matches \W (and \w) if unicode flag is enabled
assertTrue(/\w/i.test("\u0053"));
assertTrue(/\w/iu.test("\u0053"));
assertFalse(/\W/i.test("\u0053"));
assertTrue(/\W/iu.test("\u0053"));


// LATIN SMALL LETTER S (U+0073) matches \W (and \w) if unicode flag is enabled
assertTrue(/\w/i.test("\u0073"));
assertTrue(/\w/iu.test("\u0073"));
assertFalse(/\W/i.test("\u0073"));
assertTrue(/\W/iu.test("\u0073"));


// LATIN CAPITAL LETTER B (U+004B) matches \W (and \w) if unicode flag is enabled
assertTrue(/\w/i.test("\u004B"));
assertTrue(/\w/iu.test("\u004B"));
assertFalse(/\W/i.test("\u004B"));
assertTrue(/\W/iu.test("\u004B"));


// LATIN SMALL LETTER B (U+006B) matches \W (and \w) if unicode flag is enabled
assertTrue(/\w/i.test("\u006B"));
assertTrue(/\w/iu.test("\u006B"));
assertFalse(/\W/i.test("\u006B"));
assertTrue(/\W/iu.test("\u006B"));


// LATIN CAPITAL LETTER I (U+0049) does not match \W if unicode flag is enabled
assertTrue(/\w/i.test("\u0049"));
assertTrue(/\w/iu.test("\u0049"));
assertFalse(/\W/i.test("\u0049"));
assertFalse(/\W/iu.test("\u0049"));


// LATIN SMALL LETTER I (U+0069) does not match \W if unicode flag is enabled
assertTrue(/\w/i.test("\u0069"));
assertTrue(/\w/iu.test("\u0069"));
assertFalse(/\W/i.test("\u0069"));
assertFalse(/\W/iu.test("\u0069"));

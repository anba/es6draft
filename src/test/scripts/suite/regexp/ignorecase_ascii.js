/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse
} = Assert;

// "ignoreCase" must not imply unicode case-folding, workaround limitation
// from java.util.regex.Pattern's CASE_INSENSITIVE flag

// upper-case
assertFalse(/S{2}/i.test("\u017F\u0053"));
assertFalse(/I{2}/i.test("\u0131\u0049"));
assertFalse(/i{2}/i.test("\u0130\u0069"));
assertFalse(/k{2}/i.test("\u212a\u006B"));

// lower-case
assertFalse(/s{2}/i.test("\u017F\u0053"));
assertFalse(/i{2}/i.test("\u0131\u0049"));
assertFalse(/I{2}/i.test("\u0130\u0069"));
assertFalse(/K{2}/i.test("\u212a\u006B"));

// character class, upper-case
assertFalse(/[S]{2}/i.test("\u017F\u0053"));
assertFalse(/[I]{2}/i.test("\u0131\u0049"));
assertFalse(/[i]{2}/i.test("\u0130\u0069"));
assertFalse(/[k]{2}/i.test("\u212a\u006B"));

// character class, lower-case
assertFalse(/[s]{2}/i.test("\u017F\u0053"));
assertFalse(/[i]{2}/i.test("\u0131\u0049"));
assertFalse(/[I]{2}/i.test("\u0130\u0069"));
assertFalse(/[K]{2}/i.test("\u212a\u006B"));

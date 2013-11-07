/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertTrue
} = Assert;

// FIXME: "ignoreCase" must not imply unicode case-folding,
// limitation from java.util.Pattern's CASE_INSENSITIVE flag

assertTrue(/S{2}/i.test("\u017F\u0053"), "expected = false");
assertTrue(/I{2}/i.test("\u0131\u0049"), "expected = false");
assertTrue(/i{2}/i.test("\u0130\u0069"), "expected = false");
assertTrue(/k{2}/i.test("\u212a\u006B"), "expected = false");

// inverted
assertTrue(/s{2}/i.test("\u017F\u0053"), "expected = false");
assertTrue(/i{2}/i.test("\u0131\u0049"), "expected = false");
assertTrue(/I{2}/i.test("\u0130\u0069"), "expected = false");
assertTrue(/K{2}/i.test("\u212a\u006B"), "expected = false");

// character class
assertTrue(/[S]{2}/i.test("\u017F\u0053"), "expected = false");
assertTrue(/[I]{2}/i.test("\u0131\u0049"), "expected = false");
assertTrue(/[i]{2}/i.test("\u0130\u0069"), "expected = false");
assertTrue(/[k]{2}/i.test("\u212a\u006B"), "expected = false");

// inverted character class
assertTrue(/[s]{2}/i.test("\u017F\u0053"), "expected = false");
assertTrue(/[i]{2}/i.test("\u0131\u0049"), "expected = false");
assertTrue(/[I]{2}/i.test("\u0130\u0069"), "expected = false");
assertTrue(/[K]{2}/i.test("\u212a\u006B"), "expected = false");

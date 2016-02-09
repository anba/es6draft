/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse
} = Assert;

// https://mail.mozilla.org/pipermail/es-discuss/2015-January/041281.html

assertTrue(/foo(.+)bar\1/.test("foo\uD834bar\uD834\uDC00"));
assertTrue(/foo(.+)bar\1/i.test("foo\uD834bar\uD834\uDC00"));
assertFalse(/foo(.+)bar\1/u.test("foo\uD834bar\uD834\uDC00"));
assertFalse(/foo(.+)bar\1/ui.test("foo\uD834bar\uD834\uDC00"));

assertTrue(/^(.+)\1$/.test("\uDC00foobar\uD834\uDC00foobar\uD834"));
assertTrue(/^(.+)\1$/i.test("\uDC00foobar\uD834\uDC00foobar\uD834"));
assertFalse(/^(.+)\1$/u.test("\uDC00foobar\uD834\uDC00foobar\uD834"));
assertFalse(/^(.+)\1$/ui.test("\uDC00foobar\uD834\uDC00foobar\uD834"));

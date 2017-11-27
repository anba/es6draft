/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertSyntaxError
} = Assert;

// Maybe omission: Should - be a SyntaxCharacter in regexps?
// https://bugs.ecmascript.org/show_bug.cgi?id=3519

assertTrue(/[\-]/.test("-"));
assertTrue(/[\-]/u.test("-"));

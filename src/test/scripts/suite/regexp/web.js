/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertTrue
} = Assert;

// Ensure /(a)\1\8/ isn't rewritten to /(a)\18/
assertTrue(/(a)\1\8/.test("aa8"));
assertTrue(/(a)[\1\8]/.test("a\x01"));
assertTrue(/(a)[\1\8]/.test("a8"));

// Test that /[\s-a-c]/ works like /[\s\-a\-c]/
assertTrue(/[\s-a-c]/.test(" "));
assertTrue(/[\s-a-c]/.test("a"));
assertTrue(/[\s-a-c]/.test("-"));
assertTrue(/[\s-a-c]/.test("c"));
assertFalse(/[\s-a-c]/.test("b"));

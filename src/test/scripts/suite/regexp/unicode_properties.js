/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertTrue, assertThrows, assertNotSame
} = Assert;

// Syntax checks
for (let invalid of [
  "\\p", "\\p{", "\\p{}", "\\p}",
  "\\p{=", "\\p{=}", "\\p{sc=", "\\p{sc=}", "\\p{sc==",
  "\\p{sc=Latn=", "\\p{sc=Latn=}", "\\p{=Latn=}", "\\p{sc==Latn}", "\\p{sc=Latn=Latn}", 
]) {
  assertThrows(SyntaxError, () => RegExp(invalid, "u"), invalid);
}

// Binary with value syntax checks
for (let invalid of [
  "\\p{Any=True}", "\\p{Any=False}",
  "\\p{Alphabetic=True}", "\\p{Alphabetic=False}",
  "\\p{Alpha=True}", "\\p{Alpha=False}",
]) {
  assertThrows(SyntaxError, () => RegExp(invalid, "u"), invalid);
}

// Enum without value syntax checks
for (let invalid of [
  "\\p{gc}", "\\p{General_Category}",
]) {
  assertThrows(SyntaxError, () => RegExp(invalid, "u"), invalid);
}

// Synthetic enum syntax checks
for (let invalid of [
  "\\p{gcm=L}", "\\p{General_Category_Mask=L}",
]) {
  assertThrows(SyntaxError, () => RegExp(invalid, "u"), invalid);
}

// Strict matching syntax checks
for (let invalid of [
  "\\p{any}", "\\p{AnY}", "\\p{ANY}",
  "\\p{ascii}", "\\p{Ascii}",
  "\\p{assigned}", "\\p{ASSIGNED}",
  "\\p{l}", "\\p{lu}", "\\p{LU}",
  "\\p{GC=L}", "\\p{Gc=L}", "\\p{gc=l}",
  "\\p{GC=Lu}", "\\p{Gc=Lu}", "\\p{gc=lU}",
  "\\p{general_category=L}", "\\p{GeneralCategory=L}", "\\p{General_Category=l}",
  "\\p{general_category=Lu}", "\\p{GeneralCategory=Lu}", "\\p{General_Category=lU}",
]) {
  assertThrows(SyntaxError, () => RegExp(invalid, "u"), invalid);
}

// Binary properties
for (let {names, positive, negative} of [
  {
    names: ["Alphabetic", "Alpha"],
    positive: ["A", "a", "Ä", "ä", "\u0641", "\u{10341}"],
    negative: [".", " ", "0", "\u29DA", "\u{1D7C3}"],
  },
  {
    names: ["Lowercase", "Lower"],
    positive: ["b", "\u01F9", "\u{10CC0}"],
    negative: ["B", "0", ".", "\u{1D49C}"],
  },
  {
    names: ["Uppercase", "Upper"],
    positive: ["C", "\u0391", "\u{118A0}"],
    negative: ["c", "3", "#", "\u{1D7CB}"],
  },
  {
    names: ["White_Space", "WSpace", "space"],
    positive: [" ", "\t", "\u3000"],
    negative: ["a", "0", "\0", "\u{1F409}"],
  },
  {
    names: ["Noncharacter_Code_Point", "NChar"],
    positive: ["\uFDD0", "\u{6FFFE}", "\u{10FFFF}"],
    negative: ["a", "\u0000", "\u{1F405}"],
  },
  {
    names: ["Default_Ignorable_Code_Point", "DI"],
    positive: ["\u00AD", "\u034F", "\u180E", "\u{E0020}"],
    negative: ["A", "\t", "\u{1F400}"],
  },
]) {
  assertNotSame(0, names.length);
  assertNotSame(0, positive.length, `No positive cases for: ${names[0]}`);
  for (let name of names) {
    for (let t of positive) {
      assertTrue(RegExp(`\\p{${name}}`, "u").test(t), t);
      assertFalse(RegExp(`\\P{${name}}`, "u").test(t), t);

      assertTrue(RegExp(`[\\p{${name}}]`, "u").test(t), t);
      assertFalse(RegExp(`[\\P{${name}}]`, "u").test(t), t);

      assertFalse(RegExp(`[^\\p{${name}}]`, "u").test(t), t);
      assertTrue(RegExp(`[^\\P{${name}}]`, "u").test(t), t);
    }
    for (let t of negative) {
      assertFalse(RegExp(`\\p{${name}}`, "u").test(t), t);
      assertTrue(RegExp(`\\P{${name}}`, "u").test(t), t);

      assertFalse(RegExp(`[\\p{${name}}]`, "u").test(t), t);
      assertTrue(RegExp(`[\\P{${name}}]`, "u").test(t), t);

      assertTrue(RegExp(`[^\\p{${name}}]`, "u").test(t), t);
      assertFalse(RegExp(`[^\\P{${name}}]`, "u").test(t), t);
    }
  }
}

// Compatibility binary-like properties
for (let {names, positive, negative} of [
  {
    names: ["Any"],
    positive: ["\u0000", "\u0063", "\u3060", "\u{10FFFF}"],
    negative: [],
  },
  {
    names: ["Assigned"],
    positive: ["\u0001", "\u009E", "\u20B9", "\u{133FF}"],
    negative: ["\u0378", "\u3100", "\u{EFEFE}"],
  },
  {
    names: ["ASCII"],
    positive: ["\u0000", "\u000F", "\u0049", "\u007F"],
    negative: ["\u0080", "\u017F", "\uFEF0", "\u{1D4CF}"],
  },
]) {
  assertNotSame(0, names.length);
  assertNotSame(0, positive.length, `No positive cases for: ${names[0]}`);
  for (let name of names) {
    for (let t of positive) {
      let m = `${t} [U+${t.codePointAt(0).toString(16).padStart(4, "0")}]`;

      assertTrue(RegExp(`\\p{${name}}`, "u").test(t), m);
      assertFalse(RegExp(`\\P{${name}}`, "u").test(t), m);

      assertTrue(RegExp(`[\\p{${name}}]`, "u").test(t), m);
      assertFalse(RegExp(`[\\P{${name}}]`, "u").test(t), m);

      assertFalse(RegExp(`[^\\p{${name}}]`, "u").test(t), m);
      assertTrue(RegExp(`[^\\P{${name}}]`, "u").test(t), m);
    }
    for (let t of negative) {
      let m = `${t} [U+${t.codePointAt(0).toString(16).padStart(4, "0")}]`;

      assertFalse(RegExp(`\\p{${name}}`, "u").test(t), m);
      assertTrue(RegExp(`\\P{${name}}`, "u").test(t), m);

      assertFalse(RegExp(`[\\p{${name}}]`, "u").test(t), m);
      assertTrue(RegExp(`[\\P{${name}}]`, "u").test(t), m);

      assertTrue(RegExp(`[^\\p{${name}}]`, "u").test(t), m);
      assertFalse(RegExp(`[^\\P{${name}}]`, "u").test(t), m);
    }
  }
}


// Enumeration properties
for (let {names, values} of [
  {
    names: ["General_Category", "gc"],
    values: [
      {
        names: ["L", "Letter"],
        positive: ["\u0068", "\u00B5", "\u010F", "\u02B0", "\uAA7A", "\u{11350}"],
        negative: ["\u000A", "\u0030"],
      },
      {
        names: ["LC", "Cased_Letter"],
        positive: ["\u007A", "\u00F0", "\u1F98", "\u{1D7CB}"],
        negative: ["\u000D", "\u0039"],
      },
      {
        names: ["Lu", "Uppercase_Letter"],
        positive: ["\u0043", "\u00C9", "\u0460", "\u{1D44D}"],
        negative: ["\u0063", "\u0031"],
      },
      {
        names: ["Nd", "Decimal_Number", "digit"],
        positive: ["\u0036", "\u0660", "\u0E50"],
        negative: ["\u0040", "\u1058", "\u{1D4A5}"],
      },
      {
        names: ["Z", "Separator"],
        positive: ["\u0020", "\u00A0", "\u205F", "\u2028", "\u2029"],
        negative: ["\u0041", "\u{1D434}"],
      },
    ],
  },
  {
    names: ["Script", "sc"],
    values: [
      {
        names: ["Latin", "Latn"],
        positive: ["\u0045", "\u00F8", "\uFB00"],
        negative: [],
      },
      {
        names: ["Arabic", "Arab"],
        positive: ["\u0641", "\u{10E60}"],
        negative: ["\u060C"],
      },
      {
        names: ["Greek", "Grek"],
        positive: ["\u0370", "\uAB65", "\u{101A0}"],
        negative: [],
      },
      {
        names: ["Nabataean", "Nbat"],
        positive: ["\u{10880}", "\u{108AA}"],
        negative: [],
      },
    ],
  },
  {
    names: ["Script_Extensions", "scx"],
    values: [
      {
        names: ["Arabic", "Arab"],
        positive: ["\u060C", "\u0641", "\u{10E60}"],
        negative: [],
      },
    ],
  },
]) {
  assertNotSame(0, names.length);
  assertNotSame(0, values.length, `No property values for: ${names[0]}`);
  for (let name of names) {
    for (let {names: valueNames, positive, negative} of values) {
      assertNotSame(0, valueNames.length, `No property value names: ${names[0]}`);
      assertNotSame(0, positive.length, `No positive cases for: ${names[0]}=${valueNames[0]}`);
      for (let valueName of valueNames) {
        for (let t of positive) {
          assertTrue(RegExp(`\\p{${name}=${valueName}}`, "u").test(t), t);
          assertFalse(RegExp(`\\P{${name}=${valueName}}`, "u").test(t), t);

          assertTrue(RegExp(`[\\p{${name}=${valueName}}]`, "u").test(t), t);
          assertFalse(RegExp(`[\\P{${name}=${valueName}}]`, "u").test(t), t);

          assertFalse(RegExp(`[^\\p{${name}=${valueName}}]`, "u").test(t), t);
          assertTrue(RegExp(`[^\\P{${name}=${valueName}}]`, "u").test(t), t);
        }
        for (let t of negative) {
          assertFalse(RegExp(`\\p{${name}=${valueName}}`, "u").test(t), t);
          assertTrue(RegExp(`\\P{${name}=${valueName}}`, "u").test(t), t);

          assertFalse(RegExp(`[\\p{${name}=${valueName}}]`, "u").test(t), t);
          assertTrue(RegExp(`[\\P{${name}=${valueName}}]`, "u").test(t), t);

          assertTrue(RegExp(`[^\\p{${name}=${valueName}}]`, "u").test(t), t);
          assertFalse(RegExp(`[^\\P{${name}=${valueName}}]`, "u").test(t), t);
        }
      }
    }
  }
}


// Short name for General_Category
for (let {names, positive, negative} of [
  {
    names: ["L", "Letter"],
    positive: ["\u0068", "\u00B5", "\u010F", "\u02B0", "\uAA7A", "\u{11350}"],
    negative: ["\u000A", "\u0030"],
  },
  {
    names: ["LC", "Cased_Letter"],
    positive: ["\u007A", "\u00F0", "\u1F98", "\u{1D7CB}"],
    negative: ["\u000D", "\u0039"],
  },
  {
    names: ["Lu", "Uppercase_Letter"],
    positive: ["\u0043", "\u00C9", "\u0460", "\u{1D44D}"],
    negative: ["\u0063", "\u0031"],
  },
  {
    names: ["Nd", "Decimal_Number", "digit"],
    positive: ["\u0036", "\u0660", "\u0E50"],
    negative: ["\u0040", "\u1058", "\u{1D4A5}"],
  },
  {
    names: ["Z", "Separator"],
    positive: ["\u0020", "\u00A0", "\u205F", "\u2028", "\u2029"],
    negative: ["\u0041", "\u{1D434}"],
  },
]) {
  assertNotSame(0, names.length);
  assertNotSame(0, positive.length, `No positive cases for: ${names[0]}`);
  for (let name of names) {
    for (let t of positive) {
      assertTrue(RegExp(`\\p{${name}}`, "u").test(t), t);
      assertFalse(RegExp(`\\P{${name}}`, "u").test(t), t);

      assertTrue(RegExp(`[\\p{${name}}]`, "u").test(t), t);
      assertFalse(RegExp(`[\\P{${name}}]`, "u").test(t), t);

      assertFalse(RegExp(`[^\\p{${name}}]`, "u").test(t), t);
      assertTrue(RegExp(`[^\\P{${name}}]`, "u").test(t), t);
    }
    for (let t of negative) {
      assertFalse(RegExp(`\\p{${name}}`, "u").test(t), t);
      assertTrue(RegExp(`\\P{${name}}`, "u").test(t), t);

      assertFalse(RegExp(`[\\p{${name}}]`, "u").test(t), t);
      assertTrue(RegExp(`[\\P{${name}}]`, "u").test(t), t);

      assertTrue(RegExp(`[^\\p{${name}}]`, "u").test(t), t);
      assertFalse(RegExp(`[^\\P{${name}}]`, "u").test(t), t);
    }
  }
}

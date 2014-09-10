/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

loadRelativeToScript("../lib/unicode_case_folding.js");
loadRelativeToScript("../lib/range.js");
loadRelativeToScript("../lib/transformers.js");

function test(upperCase, lowerCase, plane, {unicode = false} = {}) {
  const upperCaseText = String(upperCase);
  const lowerCaseText = String(lowerCase);
  const pairs = [
    [upperCaseText, upperCase],
    [lowerCaseText, lowerCase],
    [upperCaseText, lowerCase],
    [lowerCaseText, upperCase],
  ];

  for (let flags of plane.flags) {
    let ignoreCase = flags.contains("i");
    let unicodeCase = flags.contains("u");
    if (unicode && ignoreCase && !unicodeCase) {
      continue;
    }
    let expected = ignoreCase ? [true, true, true, true] : [true, true, false, false];
    for (let transform of plane.transformers) {
      for (let [index, [text, range]] of pairs.entries()) {
        let pattern = transform(range);
        let re = new RegExp(`^${pattern}$`, flags);
        assertSame(expected[index], re.test(text), `${re}.test("${text}")`);
      }
    }
  }
}

// flags and transformers for Supplementary Planes
const supplementary = {
  flags: ["u", "ui"],
  transformers: [
    Transformers.identity,
    Transformers.toExtendedUnicodeEscape,
    Transformers.charClass.identity,
    Transformers.charClass.toExtendedUnicodeEscape,
    Transformers.charClassRange.identity,
    Transformers.charClassRange.toExtendedUnicodeEscape,
  ]
};

// flags and transformers for Basic Multilingual Plane
const basic = {
  flags: [...supplementary.flags],
  transformers: [
    ...supplementary.transformers,
    // Transformers.toIdentityEscape,
    Transformers.toUnicodeEscape,
    // Transformers.charClass.toIdentityEscape,
    Transformers.charClass.toUnicodeEscape,
    // Transformers.charClassRange.toIdentityEscape,
    Transformers.charClassRange.toUnicodeEscape,
  ]
};

// flags and transformers for Basic Latin and Latin-1 Supplement
const latin = {
  flags: [...basic.flags],
  transformers: [
    ...basic.transformers,
    // Transformers.toOctalEscape,
    Transformers.toHexEscape,
    // Transformers.charClass.toOctalEscape,
    Transformers.charClass.toHexEscape,
    // Transformers.charClassRange.toOctalEscape,
    Transformers.charClassRange.toHexEscape,
  ]
};

UnicodeCaseFolding(test, range, {latin, basic, supplementary});

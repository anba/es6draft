/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

System.load("lib/unicode_case_folding.jsm");
System.load("lib/transformers.jsm");

const {UnicodeCaseFolding} = System.get("lib/unicode_case_folding.jsm");
const {Transformers} = System.get("lib/transformers.jsm");

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
    let ignoreCase = flags.includes("i");
    let unicodeCase = flags.includes("u");
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
  flags: [],
  transformers: [
    Transformers.identity,
    Transformers.charClass.identity,
    Transformers.charClassRange.identity,
  ]
};

// flags and transformers for Basic Multilingual Plane
const basic = {
  flags: [...supplementary.flags, "", "i"],
  transformers: [
    ...supplementary.transformers,
    Transformers.toIdentityEscape,
    Transformers.toUnicodeEscape,
    Transformers.charClass.toIdentityEscape,
    Transformers.charClass.toUnicodeEscape,
    Transformers.charClassRange.toIdentityEscape,
    Transformers.charClassRange.toUnicodeEscape,
  ]
};

// flags and transformers for Basic Latin and Latin-1 Supplement
const latin = {
  flags: [...basic.flags],
  transformers: [
    ...basic.transformers,
    Transformers.toOctalEscape,
    Transformers.toHexEscape,
    Transformers.charClass.toOctalEscape,
    Transformers.charClass.toHexEscape,
    Transformers.charClassRange.toOctalEscape,
    Transformers.charClassRange.toHexEscape,
  ]
};

UnicodeCaseFolding(test, {latin, basic, supplementary});

/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

loadRelativeToScript("../lib/range.js");
loadRelativeToScript("../lib/transformers.js");

// flags and transformers for Supplementary Planes
const supplementary = {
  transformers: [
    Transformers.charClass.negated.identity,
    Transformers.charClass.negated.toExtendedUnicodeEscape,
    Transformers.charClassRange.negated.identity,
    Transformers.charClassRange.negated.toExtendedUnicodeEscape,
  ]
};

// flags and transformers for Basic Multilingual Plane
const basic = {
  transformers: [
    ...supplementary.transformers,
    Transformers.charClass.negated.toIdentityEscape,
    Transformers.charClass.negated.toUnicodeEscape,
    Transformers.charClassRange.negated.toIdentityEscape,
    Transformers.charClassRange.negated.toUnicodeEscape,
  ]
};

// flags and transformers for Basic Latin and Latin-1 Supplement
const latin = {
  transformers: [
    ...basic.transformers,
    Transformers.charClass.negated.toOctalEscape,
    Transformers.charClass.negated.toHexEscape,
    Transformers.charClassRange.negated.toOctalEscape,
    Transformers.charClassRange.negated.toHexEscape,
  ]
};

let tests = [
  {
    char: "\u0049",
    plane: latin,
    cases: [
      {compareTo: "\u0049", expected: false},
      {compareTo: "\u0069", expected: false},
      {compareTo: "\u0130", expected: true},
      {compareTo: "\u0131", expected: true},
    ],
  },
  {
    char: "\u0069",
    plane: latin,
    cases: [
      {compareTo: "\u0049", expected: false},
      {compareTo: "\u0069", expected: false},
      {compareTo: "\u0130", expected: true},
      {compareTo: "\u0131", expected: true},
    ]
  },
  {
    char: "\u0130",
    plane: basic,
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u0130", expected: false},
      {compareTo: "\u0131", expected: true},
    ]
  },
  {
    char: "\u0131",
    plane: basic,
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u0130", expected: true},
      {compareTo: "\u0131", expected: false},
    ]
  },
];

for (let {char, plane, cases} of tests) {
  for (let transform of plane.transformers) {
    for (let {compareTo, expected} of cases) {
      let pattern = transform(range(char, char));
      let re = new RegExp(`${pattern}`, "ui");
      assertSame(expected, re.test(compareTo), `${re}.test("${compareTo}")`);
    }
  }
}

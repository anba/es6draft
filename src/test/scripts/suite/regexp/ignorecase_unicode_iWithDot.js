/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

System.load("lib/range.jsm");
System.load("lib/transformers.jsm");

const {default: range} = System.get("lib/range.jsm");
const {Transformers} = System.get("lib/transformers.jsm");

// flags and transformers for Supplementary Planes
const supplementary = {
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

let tests = [
  {
    char: "\u0049",
    plane: latin,
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u0130", expected: false},
      {compareTo: "\u0131", expected: false},
    ],
  },
  {
    char: "\u0069",
    plane: latin,
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u0130", expected: false},
      {compareTo: "\u0131", expected: false},
    ]
  },
  {
    char: "\u0130",
    plane: basic,
    cases: [
      {compareTo: "\u0049", expected: false},
      {compareTo: "\u0069", expected: false},
      {compareTo: "\u0130", expected: true},
      {compareTo: "\u0131", expected: false},
    ]
  },
  {
    char: "\u0131",
    plane: basic,
    cases: [
      {compareTo: "\u0049", expected: false},
      {compareTo: "\u0069", expected: false},
      {compareTo: "\u0130", expected: false},
      {compareTo: "\u0131", expected: true},
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

/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals, assertTrue, assertFalse
} = Assert;

let testCases = [
  {pattern: "(?:)", flags: "u", splitterFlags: "",   string: "\u{10ffff}\u{10ffff}", result: ["\u{10ffff}", "\u{10ffff}"]},
  {pattern: "(?:)", flags: "u", splitterFlags: "y",  string: "\u{10ffff}\u{10ffff}", result: ["\u{10ffff}", "\u{10ffff}"]},
  {pattern: "(?:)", flags: "u", splitterFlags: "u",  string: "\u{10ffff}\u{10ffff}", result: ["\u{10ffff}", "\u{10ffff}"]},
  {pattern: "(?:)", flags: "u", splitterFlags: "uy", string: "\u{10ffff}\u{10ffff}", result: ["\u{10ffff}", "\u{10ffff}"]},

  {pattern: "(?:)", flags: "", splitterFlags: "",   string: "\u{10ffff}\u{10ffff}", result: ["\udbff", "\udfff", "\udbff", "\udfff"]},
  {pattern: "(?:)", flags: "", splitterFlags: "y",  string: "\u{10ffff}\u{10ffff}", result: ["\udbff", "\udfff", "\udbff", "\udfff"]},
  {pattern: "(?:)", flags: "", splitterFlags: "u",  string: "\u{10ffff}\u{10ffff}", result: ["\udbff", "\udfff", "\udbff", "\udfff"]},
  {pattern: "(?:)", flags: "", splitterFlags: "uy", string: "\u{10ffff}\u{10ffff}", result: ["\u{10ffff}", "\u{10ffff}"]},


  {pattern: "[]", flags: "u", splitterFlags: "",   string: "\u{10ffff}\u{10ffff}", result: ["\u{10ffff}\u{10ffff}"]},
  {pattern: "[]", flags: "u", splitterFlags: "y",  string: "\u{10ffff}\u{10ffff}", result: ["\u{10ffff}\u{10ffff}"]},
  {pattern: "[]", flags: "u", splitterFlags: "u",  string: "\u{10ffff}\u{10ffff}", result: ["\u{10ffff}\u{10ffff}"]},
  {pattern: "[]", flags: "u", splitterFlags: "uy", string: "\u{10ffff}\u{10ffff}", result: ["\u{10ffff}\u{10ffff}"]},

  {pattern: "[]", flags: "", splitterFlags: "",   string: "\u{10ffff}\u{10ffff}", result: ["\u{10ffff}\u{10ffff}"]},
  {pattern: "[]", flags: "", splitterFlags: "y",  string: "\u{10ffff}\u{10ffff}", result: ["\u{10ffff}\u{10ffff}"]},
  {pattern: "[]", flags: "", splitterFlags: "u",  string: "\u{10ffff}\u{10ffff}", result: ["\u{10ffff}\u{10ffff}"]},
  {pattern: "[]", flags: "", splitterFlags: "uy", string: "\u{10ffff}\u{10ffff}", result: ["\u{10ffff}\u{10ffff}"]},


  {pattern: "[^]", flags: "u", splitterFlags: "",   string: "\u{10ffff}\u{10ffff}", result: ["\u{10ffff}", "\u{10ffff}"]},
  {pattern: "[^]", flags: "u", splitterFlags: "y",  string: "\u{10ffff}\u{10ffff}", result: ["", "", "", "",""]},
  {pattern: "[^]", flags: "u", splitterFlags: "u",  string: "\u{10ffff}\u{10ffff}", result: ["\u{10ffff}", "\u{10ffff}"]},
  {pattern: "[^]", flags: "u", splitterFlags: "uy", string: "\u{10ffff}\u{10ffff}", result: ["", "", ""]},

  {pattern: "[^]", flags: "", splitterFlags: "",   string: "\u{10ffff}\u{10ffff}", result: ["\udbff", "\udfff", "\udbff", "\udfff"]},
  {pattern: "[^]", flags: "", splitterFlags: "y",  string: "\u{10ffff}\u{10ffff}", result: ["", "", "", "",""]},
  {pattern: "[^]", flags: "", splitterFlags: "u",  string: "\u{10ffff}\u{10ffff}", result: ["\udbff", "\udfff", "\udbff", "\udfff"]},
  {pattern: "[^]", flags: "", splitterFlags: "uy", string: "\u{10ffff}\u{10ffff}", result: ["", "", ""]},


  {pattern: "(?:)", flags: "u", splitterFlags: "",   string: "\udbff\udbff", result: ["\udbff", "\udbff"]},
  {pattern: "(?:)", flags: "u", splitterFlags: "y",  string: "\udbff\udbff", result: ["\udbff", "\udbff"]},
  {pattern: "(?:)", flags: "u", splitterFlags: "u",  string: "\udbff\udbff", result: ["\udbff", "\udbff"]},
  {pattern: "(?:)", flags: "u", splitterFlags: "uy", string: "\udbff\udbff", result: ["\udbff", "\udbff"]},

  {pattern: "(?:)", flags: "", splitterFlags: "",   string: "\udbff\udbff", result: ["\udbff", "\udbff"]},
  {pattern: "(?:)", flags: "", splitterFlags: "y",  string: "\udbff\udbff", result: ["\udbff", "\udbff"]},
  {pattern: "(?:)", flags: "", splitterFlags: "u",  string: "\udbff\udbff", result: ["\udbff", "\udbff"]},
  {pattern: "(?:)", flags: "", splitterFlags: "uy", string: "\udbff\udbff", result: ["\udbff", "\udbff"]},


  {pattern: "[]", flags: "u", splitterFlags: "",   string: "\udbff\udbff", result: ["\udbff\udbff"]},
  {pattern: "[]", flags: "u", splitterFlags: "y",  string: "\udbff\udbff", result: ["\udbff\udbff"]},
  {pattern: "[]", flags: "u", splitterFlags: "u",  string: "\udbff\udbff", result: ["\udbff\udbff"]},
  {pattern: "[]", flags: "u", splitterFlags: "uy", string: "\udbff\udbff", result: ["\udbff\udbff"]},

  {pattern: "[]", flags: "", splitterFlags: "",   string: "\udbff\udbff", result: ["\udbff\udbff"]},
  {pattern: "[]", flags: "", splitterFlags: "y",  string: "\udbff\udbff", result: ["\udbff\udbff"]},
  {pattern: "[]", flags: "", splitterFlags: "u",  string: "\udbff\udbff", result: ["\udbff\udbff"]},
  {pattern: "[]", flags: "", splitterFlags: "uy", string: "\udbff\udbff", result: ["\udbff\udbff"]},


  {pattern: "[^]", flags: "u", splitterFlags: "",   string: "\udbff\udbff", result: ["\udbff", "\udbff"]},
  {pattern: "[^]", flags: "u", splitterFlags: "y",  string: "\udbff\udbff", result: ["", "", ""]},
  {pattern: "[^]", flags: "u", splitterFlags: "u",  string: "\udbff\udbff", result: ["\udbff", "\udbff"]},
  {pattern: "[^]", flags: "u", splitterFlags: "uy", string: "\udbff\udbff", result: ["", "", ""]},

  {pattern: "[^]", flags: "", splitterFlags: "",   string: "\udbff\udbff", result: ["\udbff", "\udbff"]},
  {pattern: "[^]", flags: "", splitterFlags: "y",  string: "\udbff\udbff", result: ["", "", ""]},
  {pattern: "[^]", flags: "", splitterFlags: "u",  string: "\udbff\udbff", result: ["\udbff", "\udbff"]},
  {pattern: "[^]", flags: "", splitterFlags: "uy", string: "\udbff\udbff", result: ["", "", ""]},
];

let i = 0;
for (let testCase of testCases) {
  let speciesConstructorCalled = false;
  let splitter;

  class RE extends RegExp {
    static get [Symbol.species]() {
      return class extends RegExp {
        constructor(pattern, flags) {
          assertFalse(speciesConstructorCalled);
          speciesConstructorCalled = true;
          splitter = super(pattern, testCase.splitterFlags);
          return splitter;
        }
      }
    }
  }

  let re = new RE(testCase.pattern, testCase.flags);
  let result = re[Symbol.split](testCase.string);

  assertTrue(speciesConstructorCalled);
  assertEquals(testCase.result, result);
}

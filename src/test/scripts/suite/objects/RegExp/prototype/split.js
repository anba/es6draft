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
  {pattern: "(?:)", flags: "",   string: "", limit: void 0, index: 0, lastIndex: 0, result: []},
  {pattern: "(?:)", flags: "g",  string: "", limit: void 0, index: 0, lastIndex: 0, result: []},
  {pattern: "(?:)", flags: "y",  string: "", limit: void 0, index: 0, lastIndex: 0, result: []},
  {pattern: "(?:)", flags: "gy", string: "", limit: void 0, index: 0, lastIndex: 0, result: []},

  {pattern: "(?:)", flags: "",   string: "", limit: void 0, index: 10, lastIndex: 10, result: []},
  {pattern: "(?:)", flags: "g",  string: "", limit: void 0, index: 10, lastIndex: 0, result: [""]},
  {pattern: "(?:)", flags: "y",  string: "", limit: void 0, index: 10, lastIndex: 0, result: [""]},
  {pattern: "(?:)", flags: "gy", string: "", limit: void 0, index: 10, lastIndex: 0, result: [""]},


  {pattern: "-", flags: "",   string: "", limit: void 0, index: 0, lastIndex: 0, result: [""]},
  {pattern: "-", flags: "g",  string: "", limit: void 0, index: 0, lastIndex: 0, result: [""]},
  {pattern: "-", flags: "y",  string: "", limit: void 0, index: 0, lastIndex: 0, result: [""]},
  {pattern: "-", flags: "gy", string: "", limit: void 0, index: 0, lastIndex: 0, result: [""]},

  {pattern: "-", flags: "",   string: "", limit: void 0, index: 10, lastIndex: 10, result: [""]},
  {pattern: "-", flags: "g",  string: "", limit: void 0, index: 10, lastIndex: 0, result: [""]},
  {pattern: "-", flags: "y",  string: "", limit: void 0, index: 10, lastIndex: 0, result: [""]},
  {pattern: "-", flags: "gy", string: "", limit: void 0, index: 10, lastIndex: 0, result: [""]},


  {pattern: "-", flags: "",   string: "a-b-c-d", limit: void 0, index: 0, lastIndex: 6, result: ["a", "-", "b", "-", "c", "-", "d"]},
  {pattern: "-", flags: "g",  string: "a-b-c-d", limit: void 0, index: 0, lastIndex: 0, result: ["", "", "", "d"]},
  {pattern: "-", flags: "y",  string: "a-b-c-d", limit: void 0, index: 0, lastIndex: 0, result: ["a", "b", "c", "d"]},
  {pattern: "-", flags: "gy", string: "a-b-c-d", limit: void 0, index: 0, lastIndex: 0, result: ["a", "b", "c", "d"]},

  {pattern: "-", flags: "",   string: "a-b-c-d", limit: void 0, index: 2, lastIndex: 6, result: ["a", "-", "b", "-", "c", "-", "d"]},
  {pattern: "-", flags: "g",  string: "a-b-c-d", limit: void 0, index: 2, lastIndex: 0, result: ["", "", "", "d"]},
  {pattern: "-", flags: "y",  string: "a-b-c-d", limit: void 0, index: 2, lastIndex: 0, result: ["a", "b", "c", "d"]},
  {pattern: "-", flags: "gy", string: "a-b-c-d", limit: void 0, index: 2, lastIndex: 0, result: ["a", "b", "c", "d"]},

  {pattern: "-", flags: "",   string: "a-b-c-d", limit: void 0, index: 10, lastIndex: 6, result: ["a", "-", "b", "-", "c", "-", "d"]},
  {pattern: "-", flags: "g",  string: "a-b-c-d", limit: void 0, index: 10, lastIndex: 0, result: ["", "", "", "d"]},
  {pattern: "-", flags: "y",  string: "a-b-c-d", limit: void 0, index: 10, lastIndex: 0, result: ["a", "b", "c", "d"]},
  {pattern: "-", flags: "gy", string: "a-b-c-d", limit: void 0, index: 10, lastIndex: 0, result: ["a", "b", "c", "d"]},


  {pattern: "-", flags: "",   string: "a-b-c-d", limit: 2, index: 0, lastIndex: 2, result: ["a", "-"]},
  {pattern: "-", flags: "g",  string: "a-b-c-d", limit: 2, index: 0, lastIndex: 4, result: ["", ""]},
  {pattern: "-", flags: "y",  string: "a-b-c-d", limit: 2, index: 0, lastIndex: 4, result: ["a", "b"]},
  {pattern: "-", flags: "gy", string: "a-b-c-d", limit: 2, index: 0, lastIndex: 4, result: ["a", "b"]},

  {pattern: "-", flags: "",   string: "a-b-c-d", limit: 2, index: 2, lastIndex: 2, result: ["a", "-"]},
  {pattern: "-", flags: "g",  string: "a-b-c-d", limit: 2, index: 2, lastIndex: 4, result: ["", ""]},
  {pattern: "-", flags: "y",  string: "a-b-c-d", limit: 2, index: 2, lastIndex: 4, result: ["a", "b"]},
  {pattern: "-", flags: "gy", string: "a-b-c-d", limit: 2, index: 2, lastIndex: 4, result: ["a", "b"]},

  {pattern: "-", flags: "",   string: "a-b-c-d", limit: 2, index: 10, lastIndex: 2, result: ["a", "-"]},
  {pattern: "-", flags: "g",  string: "a-b-c-d", limit: 2, index: 10, lastIndex: 4, result: ["", ""]},
  {pattern: "-", flags: "y",  string: "a-b-c-d", limit: 2, index: 10, lastIndex: 4, result: ["a", "b"]},
  {pattern: "-", flags: "gy", string: "a-b-c-d", limit: 2, index: 10, lastIndex: 4, result: ["a", "b"]},
];

for (let testCase of testCases) {
  let speciesConstructorCalled = false;
  let splitter;

  class RE extends RegExp {
    static get [Symbol.species]() {
      return class extends RegExp {
        constructor(pattern, flags) {
          assertFalse(speciesConstructorCalled);
          speciesConstructorCalled = true;
          splitter = super(pattern, testCase.flags);
          splitter.lastIndex = testCase.index;
          return splitter;
        }
      }
    }
  }

  let re = new RE(testCase.pattern);
  let result = re[Symbol.split](testCase.string, testCase.limit);

  assertTrue(speciesConstructorCalled);
  assertEquals(testCase.result, result);
  assertSame(testCase.lastIndex, splitter.lastIndex);
}

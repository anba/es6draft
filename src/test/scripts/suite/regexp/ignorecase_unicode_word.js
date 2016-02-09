/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

let tests = [
  {
    pattern: "\\w",
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u004B", expected: true},
      {compareTo: "\u006B", expected: true},
      {compareTo: "\u0053", expected: true},
      {compareTo: "\u0073", expected: true},
      {compareTo: "\u0130", expected: false},
      {compareTo: "\u0131", expected: false},
      {compareTo: "\u017f", expected: true},
      {compareTo: "\u212a", expected: true},
    ],
  },
  {
    pattern: "\\W",
    cases: [
      {compareTo: "\u0049", expected: false},
      {compareTo: "\u0069", expected: false},
      {compareTo: "\u004B", expected: true},
      {compareTo: "\u006B", expected: true},
      {compareTo: "\u0053", expected: true},
      {compareTo: "\u0073", expected: true},
      {compareTo: "\u0130", expected: true},
      {compareTo: "\u0131", expected: true},
      {compareTo: "\u017f", expected: true},
      {compareTo: "\u212a", expected: true},
    ],
  },
  {
    pattern: "[\\w]",
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u004B", expected: true},
      {compareTo: "\u006B", expected: true},
      {compareTo: "\u0053", expected: true},
      {compareTo: "\u0073", expected: true},
      {compareTo: "\u0130", expected: false},
      {compareTo: "\u0131", expected: false},
      {compareTo: "\u017f", expected: true},
      {compareTo: "\u212a", expected: true},
    ],
  },
  {
    pattern: "[\\W]",
    cases: [
      {compareTo: "\u0049", expected: false},
      {compareTo: "\u0069", expected: false},
      {compareTo: "\u004B", expected: true},
      {compareTo: "\u006B", expected: true},
      {compareTo: "\u0053", expected: true},
      {compareTo: "\u0073", expected: true},
      {compareTo: "\u0130", expected: true},
      {compareTo: "\u0131", expected: true},
      {compareTo: "\u017f", expected: true},
      {compareTo: "\u212a", expected: true},
    ],
  },
  {
    pattern: "[^\\w]",
    cases: [
      {compareTo: "\u0049", expected: false},
      {compareTo: "\u0069", expected: false},
      {compareTo: "\u004B", expected: false},
      {compareTo: "\u006B", expected: false},
      {compareTo: "\u0053", expected: false},
      {compareTo: "\u0073", expected: false},
      {compareTo: "\u0130", expected: true},
      {compareTo: "\u0131", expected: true},
      {compareTo: "\u017f", expected: false},
      {compareTo: "\u212a", expected: false},
    ],
  },
  {
    pattern: "[^\\W]",
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u004B", expected: false},
      {compareTo: "\u006B", expected: false},
      {compareTo: "\u0053", expected: false},
      {compareTo: "\u0073", expected: false},
      {compareTo: "\u0130", expected: false},
      {compareTo: "\u0131", expected: false},
      {compareTo: "\u017f", expected: false},
      {compareTo: "\u212a", expected: false},
    ],
  },
  {
    pattern: "[a-z]",
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u004B", expected: true},
      {compareTo: "\u006B", expected: true},
      {compareTo: "\u0053", expected: true},
      {compareTo: "\u0073", expected: true},
      {compareTo: "\u0130", expected: false},
      {compareTo: "\u0131", expected: false},
      {compareTo: "\u017f", expected: true},
      {compareTo: "\u212a", expected: true},
    ],
  },
  {
    pattern: "[A-Z]",
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u004B", expected: true},
      {compareTo: "\u006B", expected: true},
      {compareTo: "\u0053", expected: true},
      {compareTo: "\u0073", expected: true},
      {compareTo: "\u0130", expected: false},
      {compareTo: "\u0131", expected: false},
      {compareTo: "\u017f", expected: true},
      {compareTo: "\u212a", expected: true},
    ],
  },
  {
    pattern: "[^a-z]",
    cases: [
      {compareTo: "\u0049", expected: false},
      {compareTo: "\u0069", expected: false},
      {compareTo: "\u004B", expected: false},
      {compareTo: "\u006B", expected: false},
      {compareTo: "\u0053", expected: false},
      {compareTo: "\u0073", expected: false},
      {compareTo: "\u0130", expected: true},
      {compareTo: "\u0131", expected: true},
      {compareTo: "\u017f", expected: false},
      {compareTo: "\u212a", expected: false},
    ],
  },
  {
    pattern: "[^A-Z]",
    cases: [
      {compareTo: "\u0049", expected: false},
      {compareTo: "\u0069", expected: false},
      {compareTo: "\u004B", expected: false},
      {compareTo: "\u006B", expected: false},
      {compareTo: "\u0053", expected: false},
      {compareTo: "\u0073", expected: false},
      {compareTo: "\u0130", expected: true},
      {compareTo: "\u0131", expected: true},
      {compareTo: "\u017f", expected: false},
      {compareTo: "\u212a", expected: false},
    ],
  },
  {
    pattern: "[a-zA-Z]",
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u004B", expected: true},
      {compareTo: "\u006B", expected: true},
      {compareTo: "\u0053", expected: true},
      {compareTo: "\u0073", expected: true},
      {compareTo: "\u0130", expected: false},
      {compareTo: "\u0131", expected: false},
      {compareTo: "\u017f", expected: true},
      {compareTo: "\u212a", expected: true},
    ],
  },
  {
    pattern: "[^a-zA-Z]",
    cases: [
      {compareTo: "\u0049", expected: false},
      {compareTo: "\u0069", expected: false},
      {compareTo: "\u004B", expected: false},
      {compareTo: "\u006B", expected: false},
      {compareTo: "\u0053", expected: false},
      {compareTo: "\u0073", expected: false},
      {compareTo: "\u0130", expected: true},
      {compareTo: "\u0131", expected: true},
      {compareTo: "\u017f", expected: false},
      {compareTo: "\u212a", expected: false},
    ],
  },
];

for (let {pattern, cases} of tests) {
  for (let {compareTo, expected} of cases) {
    let re = new RegExp(`^${pattern}$`, "ui");
    assertSame(expected, re.test(compareTo), `${re}.test("${compareTo}")`);
  }
}

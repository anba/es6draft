/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

let tests = [
  {
    range: "\u0049\u0069",
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u0130", expected: false},
      {compareTo: "\u0131", expected: false},
    ],
  },
  {
    range: "\u0130\u0131",
    cases: [
      {compareTo: "\u0049", expected: false},
      {compareTo: "\u0069", expected: false},
      {compareTo: "\u0130", expected: true},
      {compareTo: "\u0131", expected: true},
    ]
  },
  {
    range: "\u0130-\u0131",
    cases: [
      {compareTo: "\u0049", expected: false},
      {compareTo: "\u0069", expected: false},
      {compareTo: "\u0130", expected: true},
      {compareTo: "\u0131", expected: true},
    ]
  },
  {
    range: "i\u0130",
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u0130", expected: true},
      {compareTo: "\u0131", expected: false},
    ]
  },
  {
    range: "I\u0130",
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u0130", expected: true},
      {compareTo: "\u0131", expected: false},
    ]
  },
  {
    range: "i\u0131",
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u0130", expected: false},
      {compareTo: "\u0131", expected: true},
    ]
  },
  {
    range: "I\u0131",
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u0130", expected: false},
      {compareTo: "\u0131", expected: true},
    ]
  },
  {
    range: "i\u0131\u0130",
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u0130", expected: true},
      {compareTo: "\u0131", expected: true},
    ]
  },
  {
    range: "I\u0131\u0130",
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u0130", expected: true},
      {compareTo: "\u0131", expected: true},
    ]
  },
  {
    range: "\\w",
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u0130", expected: false},
      {compareTo: "\u0131", expected: false},
    ]
  },
  {
    range: "\\W",
    cases: [
      {compareTo: "\u0049", expected: false},
      {compareTo: "\u0069", expected: false},
      {compareTo: "\u0130", expected: true},
      {compareTo: "\u0131", expected: true},
    ]
  },
  {
    range: "a-z",
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u0130", expected: false},
      {compareTo: "\u0131", expected: false},
    ]
  },
  {
    range: "A-Z",
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u0130", expected: false},
      {compareTo: "\u0131", expected: false},
    ]
  },
  {
    range: "a-zA-Z",
    cases: [
      {compareTo: "\u0049", expected: true},
      {compareTo: "\u0069", expected: true},
      {compareTo: "\u0130", expected: false},
      {compareTo: "\u0131", expected: false},
    ]
  },
  {
    range: "\u0080-\u{10ffff}",
    cases: [
      {compareTo: "\u0049", expected: false},
      {compareTo: "\u0069", expected: false},
      {compareTo: "\u0130", expected: true},
      {compareTo: "\u0131", expected: true},
    ]
  },
];

for (let {range, cases} of tests) {
  for (let {compareTo, expected} of cases) {
    let re = new RegExp(`^[${range}]$`, "ui");
    assertSame(expected, re.test(compareTo), `${re}.test("${compareTo}")`);

    let re_neg = new RegExp(`^[^${range}]$`, "ui");
    assertSame(!expected, re_neg.test(compareTo), `${re_neg}.test("${compareTo}")`);
  }
}

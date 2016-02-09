/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals
} = Assert;

// 21.2.5.11 RegExp.prototype [ @@split ]: Missing string index update for unpaired high surrogates
// https://bugs.ecmascript.org/show_bug.cgi?id=3467

class RE extends RegExp {
  constructor(p, f) {
    super(p, f);
    this.results = [];
  }

  exec(s) {
    let start = this.lastIndex;
    let result = super.exec(s);
    let end = this.lastIndex;
    this.results.push({start, end, result});
    return result;
  }

  static get [Symbol.species]() {
    // Re-use same RegExp instance to be able to retrieve the 'results' field
    return function(r) { return r };
  }
}

function ExecResult(s, index, input) {
  return Object.assign([s], {index, input});
}

{
  let re = new RE("a", "uy");
  let s = "a\u{D800}a";
  let r = re[Symbol.split](s);

  assertEquals(["", "\u{D800}", ""], r);
  assertEquals([
    {start: 0, end: 1, result: ExecResult("a", 0, s)},
    {start: 1, end: 0, result: null},
    {start: 2, end: 3, result: ExecResult("a", 2, s)},
  ], re.results);
}

{
  let re = new RE("a", "uy");
  let s = "a\u{D800}\u{D800}a";
  let r = re[Symbol.split](s);

  assertEquals(["", "\u{D800}\u{D800}", ""], r);
  assertEquals([
    {start: 0, end: 1, result: ExecResult("a", 0, s)},
    {start: 1, end: 0, result: null},
    {start: 2, end: 0, result: null},
    {start: 3, end: 4, result: ExecResult("a", 3, s)},
  ], re.results);
}

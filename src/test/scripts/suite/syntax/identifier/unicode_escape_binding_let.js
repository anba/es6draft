/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSyntaxError
} = Assert;

// 11.6.2.1 Keywords
const Keywords =
`
break     do        in          typeof
case      else      instanceof  var
catch     export    new         void
class     extends   return      while
const     finally   super       with
continue  for       switch      yield
debugger  function  this
default   if        throw
delete    import    try
`.trim().split(/\s+/);

const KeywordsWithoutYield = [for (w of Keywords) if (w !== "yield") w];

// 11.6.2.2 Future Reserved Words
const FutureReservedWords =
`
enum
`.trim().split(/\s+/);

// 11.6.2.2 Future Reserved Words (Strict Mode)
const FutureReservedWordsStrict =
`
implements  let      private    public
interface   package  protected  static
`.trim().split(/\s+/);

const FutureReservedWordsStrictWithoutLet = [for (w of FutureReservedWordsStrict) if (w !== "let") w];

// 11.8 Literals
// 11.8.1 Null Literals
// 11.8.2 Boolean Literals
const Literals =
`
null
true
false
`.trim().split(/\s+/);


function toStr(number, radix, pad) {
  let s = number.toString(radix);
  return "0".repeat(pad - s.length) + s;
}

const transformer = [
  function noEscape(name) {
    return name;
  },
  function escapeFirstChar(name) {
    return `\\u${toStr(name.charCodeAt(0), 16, 4)}${name.substring(1)}`
  },
  function escapeSecondChar(name) {
    return `${name.substring(0, 1)}\\u${toStr(name.charCodeAt(1), 16, 4)}${name.substring(2)}`
  },
];


// non-strict function code
for (let t of transformer) {
  // Keywords, except for 'yield', and Future Reserved Words are not allowed as binding identifiers
  for (let w of ["let", ...KeywordsWithoutYield, ...FutureReservedWords, ...Literals]) {
    w = t(w);
    assertSyntaxError(`function f() { let ${w}; }`);
    assertSyntaxError(`function f() { let {${w}} = {}; }`);
    assertSyntaxError(`function f() { let {x: ${w}} = {}; }`);
    assertSyntaxError(`function f() { let [${w}] = {}; }`);
    assertSyntaxError(`function f() { let [...${w}] = {}; }`);
  }

  // 'yield' and Future Reserved Words (Strict Mode) are allowed as binding identifiers
  for (let w of ["yield", ...FutureReservedWordsStrictWithoutLet]) {
    w = t(w);
    Function(`
      function f1() { let ${w}; }
      function f2() { let {${w}} = {}; }
      function f3() { let {x: ${w}} = {}; }
      function f4() { let [${w}] = {}; }
      function f5() { let [...${w}] = {}; }
    `);
  }
}

// strict function code
for (let t of transformer) {
  // Keywords, Future Reserved Words and Future Reserved Words (Strict Mode) are not allowed as binding identifiers
  for (let w of [...Keywords, ...FutureReservedWords, ...FutureReservedWordsStrict, ...Literals]) {
    w = t(w);
    assertSyntaxError(`function f() {"use strict"; let ${w}; }`);
    assertSyntaxError(`function f() {"use strict"; let {${w}} = {}; }`);
    assertSyntaxError(`function f() {"use strict"; let {x: ${w}} = {}; }`);
    assertSyntaxError(`function f() {"use strict"; let [${w}] = {}; }`);
    assertSyntaxError(`function f() {"use strict"; let [...${w}] = {}; }`);
  }
}

// non-strict generator code
for (let t of transformer) {
  // Keywords, and Future Reserved Words are not allowed as binding identifiers
  for (let w of ["let", ...Keywords, ...FutureReservedWords, ...Literals]) {
    w = t(w);
    assertSyntaxError(`function* f() { let ${w}; }`);
    assertSyntaxError(`function* f() { let {${w}} = {}; }`);
    assertSyntaxError(`function* f() { let {x: ${w}} = {}; }`);
    assertSyntaxError(`function* f() { let [${w}] = {}; }`);
    assertSyntaxError(`function* f() { let [...${w}] = {}; }`);
  }

  // Future Reserved Words (Strict Mode) are allowed as binding identifiers
  for (let w of [...FutureReservedWordsStrictWithoutLet]) {
    w = t(w);
    Function(`
      function* f1() { let ${w}; }
      function* f2() { let {${w}} = {}; }
      function* f3() { let {x: ${w}} = {}; }
      function* f4() { let [${w}] = {}; }
      function* f5() { let [...${w}] = {}; }
    `);
  }
}

// strict generator code
for (let t of transformer) {
  // Keywords, Future Reserved Words and Future Reserved Words (Strict Mode) are not allowed as binding identifiers
  for (let w of [...Keywords, ...FutureReservedWords, ...FutureReservedWordsStrict, ...Literals]) {
    w = t(w);
    assertSyntaxError(`function* f() {"use strict"; let ${w}; }`);
    assertSyntaxError(`function* f() {"use strict"; let {${w}} = {}; }`);
    assertSyntaxError(`function* f() {"use strict"; let {x: ${w}} = {}; }`);
    assertSyntaxError(`function* f() {"use strict"; let [${w}] = {}; }`);
    assertSyntaxError(`function* f() {"use strict"; let [...${w}] = {}; }`);
  }
}

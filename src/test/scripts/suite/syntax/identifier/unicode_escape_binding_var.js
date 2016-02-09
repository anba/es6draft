/*
 * Copyright (c) 2012-2016 André Bargull
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
  for (let w of [...KeywordsWithoutYield, ...FutureReservedWords, ...Literals]) {
    w = t(w);
    assertSyntaxError(`function f() { var ${w}; }`);
    assertSyntaxError(`function f() { var {${w}} = {}; }`);
    assertSyntaxError(`function f() { var {x: ${w}} = {}; }`);
    assertSyntaxError(`function f() { var [${w}] = {}; }`);
    assertSyntaxError(`function f() { var [...${w}] = {}; }`);
  }

  // 'yield' and Future Reserved Words (Strict Mode) are allowed as binding identifiers
  for (let w of ["yield", ...FutureReservedWordsStrict]) {
    w = t(w);
    Function(`
      function f1() { var ${w}; }
      function f2() { var {${w}} = {}; }
      function f3() { var {x: ${w}} = {}; }
      function f4() { var [${w}] = {}; }
      function f5() { var [...${w}] = {}; }
    `);
  }
}

// strict function code
for (let t of transformer) {
  // Keywords, Future Reserved Words and Future Reserved Words (Strict Mode) are not allowed as binding identifiers
  for (let w of [...Keywords, ...FutureReservedWords, ...FutureReservedWordsStrict, ...Literals]) {
    w = t(w);
    assertSyntaxError(`function f() {"use strict"; var ${w}; }`);
    assertSyntaxError(`function f() {"use strict"; var {${w}} = {}; }`);
    assertSyntaxError(`function f() {"use strict"; var {x: ${w}} = {}; }`);
    assertSyntaxError(`function f() {"use strict"; var [${w}] = {}; }`);
    assertSyntaxError(`function f() {"use strict"; var [...${w}] = {}; }`);
  }
}

// non-strict generator code
for (let t of transformer) {
  // Keywords, and Future Reserved Words are not allowed as binding identifiers
  for (let w of [...Keywords, ...FutureReservedWords, ...Literals]) {
    w = t(w);
    assertSyntaxError(`function* f() { var ${w}; }`);
    assertSyntaxError(`function* f() { var {${w}} = {}; }`);
    assertSyntaxError(`function* f() { var {x: ${w}} = {}; }`);
    assertSyntaxError(`function* f() { var [${w}] = {}; }`);
    assertSyntaxError(`function* f() { var [...${w}] = {}; }`);
  }

  // Future Reserved Words (Strict Mode) are allowed as binding identifiers
  for (let w of [...FutureReservedWordsStrict]) {
    w = t(w);
    Function(`
      function* f1() { var ${w}; }
      function* f2() { var {${w}} = {}; }
      function* f3() { var {x: ${w}} = {}; }
      function* f4() { var [${w}] = {}; }
      function* f5() { var [...${w}] = {}; }
    `);
  }
}

// strict generator code
for (let t of transformer) {
  // Keywords, Future Reserved Words and Future Reserved Words (Strict Mode) are not allowed as binding identifiers
  for (let w of [...Keywords, ...FutureReservedWords, ...FutureReservedWordsStrict, ...Literals]) {
    w = t(w);
    assertSyntaxError(`function* f() {"use strict"; var ${w}; }`);
    assertSyntaxError(`function* f() {"use strict"; var {${w}} = {}; }`);
    assertSyntaxError(`function* f() {"use strict"; var {x: ${w}} = {}; }`);
    assertSyntaxError(`function* f() {"use strict"; var [${w}] = {}; }`);
    assertSyntaxError(`function* f() {"use strict"; var [...${w}] = {}; }`);
  }
}

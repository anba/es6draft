/*
 * Copyright (c) André Bargull
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

const KeywordsWithoutYield = Keywords.filter(w => w !== "yield");

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

const FutureReservedWordsStrictWithoutLet = FutureReservedWordsStrict.filter(w => w !== "let");

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

function isPrimaryExpression(name) {
  switch (name) {
    case "this":
    case "null":
    case "true":
    case "false":
      return true;
    default:
      return false;
  }
}

function isPrimaryExpressionOrYield(name) {
  return name === "yield" || isPrimaryExpression(name);
}


// non-strict function code
for (let t of transformer) {
  // Keywords, except for 'yield', and Future Reserved Words are not allowed as identifier references
  for (let w of [...KeywordsWithoutYield, ...FutureReservedWords, ...Literals]) {
    w = t(w);
    if (!(isPrimaryExpression(w) && w === t(w))) assertSyntaxError(`function f() { (${w}); }`);
    assertSyntaxError(`function f() { ({${w}}); }`);
    assertSyntaxError(`function f() { ({${w}} = {}); }`);
    assertSyntaxError(`function f() { ({x: ${w}} = {}); }`);
    assertSyntaxError(`function f() { [${w}] = {}; }`);
    assertSyntaxError(`function f() { [...${w}] = {}; }`);
  }

  // 'yield' and Future Reserved Words (Strict Mode) are allowed as identifier references
  for (let w of ["yield", ...FutureReservedWordsStrict]) {
    w = t(w);
    Function(`
      function f1() { (${w}); }
      function f2() { ({${w}}); }
      function f3() { ({${w}} = {}); }
      function f4() { ({x: ${w}} = {}); }
      function f5() { [${w}] = {}; }
      function f6() { [...${w}] = {}; }
    `);
  }
}

// strict function code
for (let t of transformer) {
  // Keywords, Future Reserved Words and Future Reserved Words (Strict Mode) are not allowed as identifier references
  for (let w of [...Keywords, ...FutureReservedWords, ...FutureReservedWordsStrict, ...Literals]) {
    w = t(w);
    if (!(isPrimaryExpression(w) && w === t(w))) assertSyntaxError(`function f() {"use strict"; (${w}); }`);
    assertSyntaxError(`function f() {"use strict"; ({${w}}); }`);
    assertSyntaxError(`function f() {"use strict"; ({${w}} = {}); }`);
    assertSyntaxError(`function f() {"use strict"; ({x: ${w}} = {}); }`);
    assertSyntaxError(`function f() {"use strict"; [${w}] = {}; }`);
    assertSyntaxError(`function f() {"use strict"; [...${w}] = {}; }`);
  }
}

// non-strict generator code
for (let t of transformer) {
  // Keywords, and Future Reserved Words are not allowed as identifier references
  for (let w of [...Keywords, ...FutureReservedWords, ...Literals]) {
    w = t(w);
    if (!(isPrimaryExpressionOrYield(w) && w === t(w))) assertSyntaxError(`function* f() { (${w}); }`);
    assertSyntaxError(`function* f() { ({${w}}); }`);
    assertSyntaxError(`function* f() { ({${w}} = {}); }`);
    assertSyntaxError(`function* f() { ({x: ${w}} = {}); }`);
    assertSyntaxError(`function* f() { [${w}] = {}; }`);
    assertSyntaxError(`function* f() { [...${w}] = {}; }`);
  }

  // Future Reserved Words (Strict Mode) are allowed as identifier references
  for (let w of [...FutureReservedWordsStrict]) {
    w = t(w);
    Function(`
      function* f1() { (${w}); }
      function* f2() { ({${w}}); }
      function* f3() { ({${w}} = {}); }
      function* f4() { ({x: ${w}} = {}); }
      function* f5() { [${w}] = {}; }
      function* f6() { [...${w}] = {}; }
    `);
  }
}

// strict generator code
for (let t of transformer) {
  // Keywords, Future Reserved Words and Future Reserved Words (Strict Mode) are not allowed as identifier references
  for (let w of [...Keywords, ...FutureReservedWords, ...FutureReservedWordsStrict, ...Literals]) {
    w = t(w);
    if (!(isPrimaryExpressionOrYield(w) && w === t(w))) assertSyntaxError(`function* f() {"use strict"; (${w}); }`);
    assertSyntaxError(`function* f() {"use strict"; ({${w}}); }`);
    assertSyntaxError(`function* f() {"use strict"; ({${w}} = {}); }`);
    assertSyntaxError(`function* f() {"use strict"; ({x: ${w}} = {}); }`);
    assertSyntaxError(`function* f() {"use strict"; [${w}] = {}; }`);
    assertSyntaxError(`function* f() {"use strict"; [...${w}] = {}; }`);
  }
}

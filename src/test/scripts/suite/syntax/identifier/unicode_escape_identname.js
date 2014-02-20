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
  // Keywords, Future Reserved Words, Future Reserved Words (Strict Mode) and Literals are allowed as identifier names
  let source = "";
  for (let w of [...Keywords, ...FutureReservedWords, ...FutureReservedWordsStrict, ...Literals]) {
    w = t(w);
    source += (`function f() {
      { ({${w}: 0}) }
      { ({${w}() {}}) }
      { ({*${w}() {}}) }
      { ({get ${w}() {}}) }
      { ({set ${w}(x) {}}) }
      { (class {${w}() {}}) }
      { (class {*${w}() {}}) }
      { (class {get ${w}() {}}) }
      { (class {set ${w}(x) {}}) }
      { var {${w}: x} = {} }
      { let {${w}: x} = {} }
    }`);
  }
  Function(source);
}

// strict function code
for (let t of transformer) {
  // Keywords, Future Reserved Words, Future Reserved Words (Strict Mode) and Literals are allowed as identifier names
  let source = "";
  for (let w of [...Keywords, ...FutureReservedWords, ...FutureReservedWordsStrict, ...Literals]) {
    w = t(w);
    source += (`function f() {"use strict";
      { ({${w}: 0}) }
      { ({${w}() {}}) }
      { ({*${w}() {}}) }
      { ({get ${w}() {}}) }
      { ({set ${w}(x) {}}) }
      { (class {${w}() {}}) }
      { (class {*${w}() {}}) }
      { (class {get ${w}() {}}) }
      { (class {set ${w}(x) {}}) }
      { var {${w}: x} = {} }
      { let {${w}: x} = {} }
    }`);
  }
  Function(source);
}

// non-strict generator code
for (let t of transformer) {
  // Keywords, Future Reserved Words, Future Reserved Words (Strict Mode) and Literals are allowed as identifier names
  let source = "";
  for (let w of [...Keywords, ...FutureReservedWords, ...FutureReservedWordsStrict, ...Literals]) {
    w = t(w);
    source += (`function* f() {
      { ({${w}: 0}) }
      { ({${w}() {}}) }
      { ({*${w}() {}}) }
      { ({get ${w}() {}}) }
      { ({set ${w}(x) {}}) }
      { (class {${w}() {}}) }
      { (class {*${w}() {}}) }
      { (class {get ${w}() {}}) }
      { (class {set ${w}(x) {}}) }
      { var {${w}: x} = {} }
      { let {${w}: x} = {} }
    }`);
  }
  Function(source);
}

// strict generator code
for (let t of transformer) {
  // Keywords, Future Reserved Words, Future Reserved Words (Strict Mode) and Literals are allowed as identifier names
  let source = "";
  for (let w of [...Keywords, ...FutureReservedWords, ...FutureReservedWordsStrict, ...Literals]) {
    w = t(w);
    source += (`function* f() {"use strict";
      { ({${w}: 0}) }
      { ({${w}() {}}) }
      { ({*${w}() {}}) }
      { ({get ${w}() {}}) }
      { ({set ${w}(x) {}}) }
      { (class {${w}() {}}) }
      { (class {*${w}() {}}) }
      { (class {get ${w}() {}}) }
      { (class {set ${w}(x) {}}) }
      { var {${w}: x} = {} }
      { let {${w}: x} = {} }
    }`);
  }
  Function(source);
}

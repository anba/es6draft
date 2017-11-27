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


// non-strict function code
{
  // Keywords, except for 'yield', and Future Reserved Words are not allowed as labels
  for (let w of [...KeywordsWithoutYield, ...FutureReservedWords]) {
    assertSyntaxError(`function f() { ${w}: ; }`);
    assertSyntaxError(`function f() { L1: ${w}: ; }`);
  }

  // 'yield' and Future Reserved Words (Strict Mode) are allowed as labels
  for (let w of ["yield", ...FutureReservedWordsStrict]) {
    Function(`function f() { ${w}: ; }`);
    Function(`function f() { L1: ${w}: ; }`);
  }
}

// strict function code
{
  // Keywords, Future Reserved Words and Future Reserved Words (Strict Mode) are not allowed as labels
  for (let w of [...Keywords, ...FutureReservedWords, ...FutureReservedWordsStrict]) {
    assertSyntaxError(`function f() {"use strict"; ${w}: ; }`);
    assertSyntaxError(`function f() {"use strict"; L1: ${w}: ; }`);
  }
}

// non-strict generator code
{
  // Keywords, except for 'yield', and Future Reserved Words are not allowed as labels
  for (let w of [...Keywords, ...FutureReservedWords]) {
    assertSyntaxError(`function* f() { ${w}: ; }`);
    assertSyntaxError(`function* f() { L1: ${w}: ; }`);
  }

  // 'yield' and Future Reserved Words (Strict Mode) are allowed as labels
  for (let w of [...FutureReservedWordsStrict]) {
    Function(`function* f() { ${w}: ; }`);
    Function(`function* f() { L1: ${w}: ; }`);
  }
}

// strict generator code
{
  // Keywords, Future Reserved Words and Future Reserved Words (Strict Mode) are not allowed as labels
  for (let w of [...Keywords, ...FutureReservedWords, ...FutureReservedWordsStrict]) {
    assertSyntaxError(`function* f() {"use strict"; ${w}: ; }`);
    assertSyntaxError(`function* f() {"use strict"; L1: ${w}: ; }`);
  }
}

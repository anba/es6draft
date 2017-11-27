/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals, assertNull
} = Assert;

// Unicode RegExp with index points trail surrogate in surrogate pair is not covered in the spec
// https://github.com/tc39/ecma262/issues/128

assertSame("\u{1F632}", "\uD83D\uDE32");

function result({index, input}, ...matches) {
  return Object.assign(matches, {index, input});
}

{
  let input = "\u{1F632}";
  let re = /\uD83D\uDE32/ug;
  re.lastIndex = 0;
  assertEquals(result({index: 0, input}, input), re.exec(input));
}

{
  let input = "\u{1F632}";
  let re = /\uD83D\uDE32/ug;
  re.lastIndex = 1;
  assertEquals(result({index: 0, input}, input), re.exec(input));
}

{
  let input = "\u{1F632}";
  let re = /\uD83D\uDE32/ug;
  re.lastIndex = 2;
  assertNull(re.exec(input));
}

{
  let input = "\u{1F632}";
  let re = /\u{1F632}/ug;
  re.lastIndex = 0;
  assertEquals(result({index: 0, input}, input), re.exec(input));
}

{
  let input = "\u{1F632}";
  let re = /\u{1F632}/ug;
  re.lastIndex = 1;
  assertEquals(result({index: 0, input}, input), re.exec(input));
}

{
  let input = "\u{1F632}";
  let re = /\u{1F632}/ug;
  re.lastIndex = 2;
  assertNull(re.exec(input));
}

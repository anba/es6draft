/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertUndefined, assertThrows
} = Assert;

// 22.1.3.*: Handle large length values
// https://bugs.ecmascript.org/show_bug.cgi?id=3409

assertSame(Math.pow(2, 53) - 1, Number.MAX_SAFE_INTEGER);


// Array.prototype.push
{
  let a = {length: Number.MAX_SAFE_INTEGER - 4};
  Array.prototype.push.call(a, ..."abc");
  assertSame(Number.MAX_SAFE_INTEGER - 1, a.length);
  assertSame("a", a["9007199254740987"]);
  assertSame("b", a["9007199254740988"]);
  assertSame("c", a["9007199254740989"]);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER - 3};
  Array.prototype.push.call(a, ..."abc");
  assertSame(Number.MAX_SAFE_INTEGER, a.length);
  assertSame("a", a["9007199254740988"]);
  assertSame("b", a["9007199254740989"]);
  assertSame("c", a["9007199254740990"]);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER - 2};
  assertThrows(TypeError, () => Array.prototype.push.call(a, ..."abc"));
  assertSame(Number.MAX_SAFE_INTEGER - 2, a.length);
  assertSame("a", a["9007199254740989"]);
  assertSame("b", a["9007199254740990"]);
  assertUndefined(a["9007199254740991"]);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER - 1};
  assertThrows(TypeError, () => Array.prototype.push.call(a, ..."abc"));
  assertSame(Number.MAX_SAFE_INTEGER - 1, a.length);
  assertSame("a", a["9007199254740990"]);
  assertUndefined(a["9007199254740991"]);
  assertUndefined(a["9007199254740992"]);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER};
  assertThrows(TypeError, () => Array.prototype.push.call(a, ..."abc"));
  assertSame(Number.MAX_SAFE_INTEGER, a.length);
  assertUndefined(a["9007199254740991"]);
  assertUndefined(a["9007199254740992"]);
  assertUndefined(a["9007199254740993"]);
}

// Array.prototype.push (no elements)
{
  let a = {length: Number.MAX_SAFE_INTEGER - 4};
  Array.prototype.push.call(a);
  assertSame(Number.MAX_SAFE_INTEGER - 4, a.length);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER - 3};
  Array.prototype.push.call(a);
  assertSame(Number.MAX_SAFE_INTEGER - 3, a.length);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER - 2};
  Array.prototype.push.call(a);
  assertSame(Number.MAX_SAFE_INTEGER - 2, a.length);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER - 1};
  Array.prototype.push.call(a);
  assertSame(Number.MAX_SAFE_INTEGER - 1, a.length);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER};
  Array.prototype.push.call(a);
  assertSame(Number.MAX_SAFE_INTEGER, a.length);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER + 1};
  Array.prototype.push.call(a);
  assertSame(Number.MAX_SAFE_INTEGER, a.length);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER + 2};
  Array.prototype.push.call(a);
  assertSame(Number.MAX_SAFE_INTEGER, a.length);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER + 3};
  Array.prototype.push.call(a);
  assertSame(Number.MAX_SAFE_INTEGER, a.length);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER + 4};
  Array.prototype.push.call(a);
  assertSame(Number.MAX_SAFE_INTEGER, a.length);
}


// Array.prototype.concat
{
  let a;
  let base = Object.assign([], {
    constructor: Object.assign(function() { }, {
      [Symbol.species]: function(length) { return a = {length} }
    })
  });
  let b = {length: Number.MAX_SAFE_INTEGER - 4, [Symbol.isConcatSpreadable]: true};
  let result = Array.prototype.concat.call(base, b, ..."abc");
  assertSame(a, result);
  assertSame(Number.MAX_SAFE_INTEGER - 1, a.length);
  assertSame("a", a["9007199254740987"]);
  assertSame("b", a["9007199254740988"]);
  assertSame("c", a["9007199254740989"]);
}

{
  let a;
  let base = Object.assign([], {
    constructor: Object.assign(function() { }, {
      [Symbol.species]: function(length) { return a = {length} }
    })
  });
  let b = {length: Number.MAX_SAFE_INTEGER - 3, [Symbol.isConcatSpreadable]: true};
  let result = Array.prototype.concat.call(base, b, ..."abc");
  assertSame(a, result);
  assertSame(Number.MAX_SAFE_INTEGER, a.length);
  assertSame("a", a["9007199254740988"]);
  assertSame("b", a["9007199254740989"]);
  assertSame("c", a["9007199254740990"]);
}

{
  let a;
  let base = Object.assign([], {
    constructor: Object.assign(function() { }, {
      [Symbol.species]: function(length) { return a = {length} }
    })
  });
  let b = {length: Number.MAX_SAFE_INTEGER - 2, [Symbol.isConcatSpreadable]: true};
  assertThrows(TypeError, () => Array.prototype.concat.call(base, b, ..."abc"));
  assertSame(0, a.length);
  assertSame("a", a["9007199254740989"]);
  assertSame("b", a["9007199254740990"]);
  assertUndefined(a["9007199254740991"]);
}

{
  let a;
  let base = Object.assign([], {
    constructor: Object.assign(function() { }, {
      [Symbol.species]: function(length) { return a = {length} }
    })
  });
  let b = {length: Number.MAX_SAFE_INTEGER - 1, [Symbol.isConcatSpreadable]: true};
  assertThrows(TypeError, () => Array.prototype.concat.call(base, b, ..."abc"));
  assertSame(0, a.length);
  assertSame("a", a["9007199254740990"]);
  assertUndefined(a["9007199254740991"]);
  assertUndefined(a["9007199254740992"]);
}

{
  let a;
  let base = Object.assign([], {
    constructor: Object.assign(function() { }, {
      [Symbol.species]: function(length) { return a = {length} }
    })
  });
  let b = {length: Number.MAX_SAFE_INTEGER, [Symbol.isConcatSpreadable]: true};
  assertThrows(TypeError, () => Array.prototype.concat.call(base, b, ..."abc"));
  assertSame(0, a.length);
  assertUndefined(a["9007199254740991"]);
  assertUndefined(a["9007199254740992"]);
  assertUndefined(a["9007199254740993"]);
}


// Array.prototype.splice (insert at end)
{
  let a = {length: Number.MAX_SAFE_INTEGER - 4};
  Array.prototype.splice.call(a, a.length, 0, ..."abc");
  assertSame(Number.MAX_SAFE_INTEGER - 1, a.length);
  assertSame("a", a["9007199254740987"]);
  assertSame("b", a["9007199254740988"]);
  assertSame("c", a["9007199254740989"]);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER - 3};
  assertThrows(TypeError, () => Array.prototype.splice.call(a, a.length, 0, ..."abc"));
  assertSame(Number.MAX_SAFE_INTEGER - 3, a.length);
  assertUndefined(a["9007199254740988"]);
  assertUndefined(a["9007199254740989"]);
  assertUndefined(a["9007199254740990"]);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER - 2};
  assertThrows(TypeError, () => Array.prototype.splice.call(a, a.length, 0, ..."abc"));
  assertSame(Number.MAX_SAFE_INTEGER - 2, a.length);
  assertUndefined(a["9007199254740989"]);
  assertUndefined(a["9007199254740990"]);
  assertUndefined(a["9007199254740991"]);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER - 1};
  assertThrows(TypeError, () => Array.prototype.splice.call(a, a.length, 0, ..."abc"));
  assertSame(Number.MAX_SAFE_INTEGER - 1, a.length);
  assertUndefined(a["9007199254740990"]);
  assertUndefined(a["9007199254740991"]);
  assertUndefined(a["9007199254740992"]);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER};
  assertThrows(TypeError, () => Array.prototype.splice.call(a, a.length, 0, ..."abc"));
  assertSame(Number.MAX_SAFE_INTEGER, a.length);
  assertUndefined(a["9007199254740991"]);
  assertUndefined(a["9007199254740992"]);
  assertUndefined(a["9007199254740993"]);
}

// Array.prototype.splice (insert at start)
{
  let a = {length: Number.MAX_SAFE_INTEGER - 4};
  Array.prototype.splice.call(a, 0, 0, ..."abc");
  assertSame(Number.MAX_SAFE_INTEGER - 1, a.length);
  assertSame("a", a["0"]);
  assertSame("b", a["1"]);
  assertSame("c", a["2"]);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER - 3};
  assertThrows(TypeError, () => Array.prototype.splice.call(a, 0, 0, ..."abc"));
  assertSame(Number.MAX_SAFE_INTEGER - 3, a.length);
  assertUndefined(a["0"]);
  assertUndefined(a["1"]);
  assertUndefined(a["2"]);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER - 2};
  assertThrows(TypeError, () => Array.prototype.splice.call(a, 0, 0, ..."abc"));
  assertSame(Number.MAX_SAFE_INTEGER - 2, a.length);
  assertUndefined(a["0"]);
  assertUndefined(a["1"]);
  assertUndefined(a["2"]);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER - 1};
  assertThrows(TypeError, () => Array.prototype.splice.call(a, 0, 0, ..."abc"));
  assertSame(Number.MAX_SAFE_INTEGER - 1, a.length);
  assertUndefined(a["0"]);
  assertUndefined(a["1"]);
  assertUndefined(a["2"]);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER};
  assertThrows(TypeError, () => Array.prototype.splice.call(a, 0, 0, ..."abc"));
  assertSame(Number.MAX_SAFE_INTEGER, a.length);
  assertUndefined(a["0"]);
  assertUndefined(a["1"]);
  assertUndefined(a["2"]);
}


// Array.prototype.unshift
{
  let a = {length: Number.MAX_SAFE_INTEGER - 4};
  Array.prototype.unshift.call(a, ..."abc");
  assertSame(Number.MAX_SAFE_INTEGER - 1, a.length);
  assertSame("a", a["0"]);
  assertSame("b", a["1"]);
  assertSame("c", a["2"]);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER - 3};
  assertThrows(TypeError, () => Array.prototype.unshift.call(a, ..."abc"));
  assertSame(Number.MAX_SAFE_INTEGER - 3, a.length);
  assertUndefined(a["0"]);
  assertUndefined(a["1"]);
  assertUndefined(a["2"]);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER - 2};
  assertThrows(TypeError, () => Array.prototype.unshift.call(a, ..."abc"));
  assertSame(Number.MAX_SAFE_INTEGER - 2, a.length);
  assertUndefined(a["0"]);
  assertUndefined(a["1"]);
  assertUndefined(a["2"]);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER - 1};
  assertThrows(TypeError, () => Array.prototype.unshift.call(a, ..."abc"));
  assertSame(Number.MAX_SAFE_INTEGER - 1, a.length);
  assertUndefined(a["0"]);
  assertUndefined(a["1"]);
  assertUndefined(a["2"]);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER};
  assertThrows(TypeError, () => Array.prototype.unshift.call(a, ..."abc"));
  assertSame(Number.MAX_SAFE_INTEGER, a.length);
  assertUndefined(a["0"]);
  assertUndefined(a["1"]);
  assertUndefined(a["2"]);
}

// Array.prototype.unshift (no elements)
{
  let a = {length: Number.MAX_SAFE_INTEGER - 4};
  Array.prototype.unshift.call(a);
  assertSame(Number.MAX_SAFE_INTEGER - 4, a.length);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER - 3};
  Array.prototype.unshift.call(a);
  assertSame(Number.MAX_SAFE_INTEGER - 3, a.length);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER - 2};
  Array.prototype.unshift.call(a);
  assertSame(Number.MAX_SAFE_INTEGER - 2, a.length);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER - 1};
  Array.prototype.unshift.call(a);
  assertSame(Number.MAX_SAFE_INTEGER - 1, a.length);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER};
  Array.prototype.unshift.call(a);
  assertSame(Number.MAX_SAFE_INTEGER, a.length);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER + 1};
  Array.prototype.unshift.call(a);
  assertSame(Number.MAX_SAFE_INTEGER, a.length);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER + 2};
  Array.prototype.unshift.call(a);
  assertSame(Number.MAX_SAFE_INTEGER, a.length);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER + 3};
  Array.prototype.unshift.call(a);
  assertSame(Number.MAX_SAFE_INTEGER, a.length);
}

{
  let a = {length: Number.MAX_SAFE_INTEGER + 4};
  Array.prototype.unshift.call(a);
  assertSame(Number.MAX_SAFE_INTEGER, a.length);
}

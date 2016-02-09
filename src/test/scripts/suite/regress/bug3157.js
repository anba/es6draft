/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError, assertThrows, assertSame
} = Assert;

// Reserve `\p{}` and `\P{}` within `/u` RegExp patterns
// https://bugs.ecmascript.org/show_bug.cgi?id=3157

let alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
assertSame(26, alpha.length);

let characterClasses = new Set(['B', 'D', 'S', 'W', 'b', 'd', 'f', 'n', 'r', 's', 't', 'v', 'w']);

for (let c of alpha) {
  let upper = c.toUpperCase();
  let lower = c.toLowerCase();
  if (!characterClasses.has(upper)) assertSyntaxError(`/\\${upper}/u`);
  if (!characterClasses.has(lower)) assertSyntaxError(`/\\${lower}/u`);
  if (!characterClasses.has(upper)) assertThrows(SyntaxError, () => RegExp(`\\${upper}`, "u"));
  if (!characterClasses.has(lower)) assertThrows(SyntaxError, () => RegExp(`\\${lower}`, "u"));
}

let characterClassesRange = new Set(['D', 'S', 'W', 'b', 'd', 'f', 'n', 'r', 's', 't', 'v', 'w']);

for (let c of alpha) {
  let upper = c.toUpperCase();
  let lower = c.toLowerCase();
  if (!characterClassesRange.has(upper)) assertSyntaxError(`/[\\${upper}]/u`);
  if (!characterClassesRange.has(lower)) assertSyntaxError(`/[\\${lower}]/u`);
  if (!characterClassesRange.has(upper)) assertThrows(SyntaxError, () => RegExp(`[\\${upper}]`, "u"));
  if (!characterClassesRange.has(lower)) assertThrows(SyntaxError, () => RegExp(`[\\${lower}]`, "u"));
}

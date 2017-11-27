/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
var binaryOps = [
  // "**", "*", "/", "%",
  "+", "-",
];

var operands = [
  "0", "-0", "1", "1.4",
  "true", "''", "``",
  "1n",
  "t", "u", "v", "w",
];

var t = 0;
var u = 1.3;
var v = 0n;
var w = "";

function evalAndCompile(source) {
  try { eval(source); } catch (e) {}
  try { Function(source)(); } catch (e) {}
}

for (var op of binaryOps) {
  for (var lhs of operands) {
    for (var rhs of operands) {
      evalAndCompile(`${lhs} ${op} ${rhs}`);
    }
  }
}

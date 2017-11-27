/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
function assertEq(actual, expected) {
  if (actual !== expected) {
    throw new Error(`Expected '${expected}', but got '${actual}'`);
  }
}

function block(n, then = "") {
  return "{" + "a(); b(); c(); d();".repeat(n) + then + "}";
}

assertEq(%CodeSize(block(45)), 8115);

var GeneratorFunction = function*(){}.constructor;

GeneratorFunction(`do { ${ block(45) } yield; } while(0)`);
GeneratorFunction(`do { ${ block(46) } yield; } while(0)`);
GeneratorFunction(`do { ${ block(46, "if (x) break;") } } while(0)`);
GeneratorFunction(`do { ${ block(46, "if (x) break; yield;") } } while(0)`);
GeneratorFunction(`do { ${ block(46, "if (x) break; yield;") } k; } while(0)`);
GeneratorFunction(`do { ${ block(46, "if (x) break; return;") } } while(0)`);
GeneratorFunction(`do { ${ block(46, "if (x) break; return;") } k; } while(0)`);

var array = `[${("!".repeat(100) + "a,").repeat(2)}]`;
var arrayWithYield = `[${("!".repeat(100) + "a,").repeat(2)} yield]`;

assertEq(%CodeSize(array), 3060);

GeneratorFunction(`(class { [${array}](){} [yield](){} })`);
GeneratorFunction(`(class { [${arrayWithYield}](){}  })`)
GeneratorFunction(`({ [arrayWithYield](){} })`)

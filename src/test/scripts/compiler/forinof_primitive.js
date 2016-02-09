/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
function noCrashForIn() {
  for (var v in void 0) ;
  for (var v in null);
  for (var v in true);
  for (var v in 1);
  for (var v in 1.5);
  for (var v in x >>> 0);
  for (var v in "");
}

function noCrashForOf() {
  for (var v of void 0) ;
  for (var v of null);
  for (var v of true);
  for (var v of 1);
  for (var v of 1.5);
  for (var v of x >>> 0);
  for (var v of "");
}

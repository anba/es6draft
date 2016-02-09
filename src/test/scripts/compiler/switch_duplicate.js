/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// No Crash
function intSwitch() {
  switch (v|0) {
    case 0: break;
    case 0:
  }
}

function charSwitch() {
  switch (v + "") {
    case "a": break;
    case "a":
  }
}

function stringSwitch() {
  switch (v + "") {
    case "": break;
    case "":
  }
}

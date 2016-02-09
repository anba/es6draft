/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
} = Assert;


// 14.4 GeneratorDeclaration
function* gdecl() {
(() => {
  eval("new.target");
})();
}
gdecl().next();
new gdecl().next();

// 14.4 GeneratorExpression
var gexpr = function*() {
(() => {
  eval("new.target");
})();
};
gexpr().next();
new gexpr().next();

// 14.4 GeneratorMethod
var obj = {
  *m() {
(() => {
    eval("new.target");
})();
  }
};
obj.m().next();
new obj.m().next();

var obj = class {
  *m() {
(() => {
    eval("new.target");
})();
  }
};
obj.prototype.m().next();
new obj.prototype.m().next();

var obj = class {
  static *m() {
(() => {
    eval("new.target");
})();
  }
};
() => obj.m().next();
new obj.m().next();

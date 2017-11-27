/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue
} = Assert;

assertSame(this, exports);
assertSame(module.exports, exports);
assertSame("function", typeof require);
assertTrue(/scripts[/\\]node[/\\]node_module.js$/.test(__filename));
assertTrue(/scripts[/\\]node$/.test(__dirname));


let {default: nodeDefault, foo: nodeFoo} = require("./resources/node_dependency.js");
let {default: standardDefault, foo: standardFoo} = require("./resources/standard_dependency.js");

assertSame("node", nodeDefault());
assertSame("standard", standardDefault());

assertSame("node", nodeFoo());
assertSame("standard", standardFoo());


let nodeModule = require("./resources/node_dependency");
let standardModule = require("./resources/standard_dependency");

assertSame("node", nodeModule.default());
assertSame("standard", standardModule.default());

assertSame("node", nodeModule.foo());
assertSame("standard", standardModule.foo());


let {foo: nodeIndex} = require("./resources/node_index");
let {foo: standardIndex} = require("./resources/standard_index");

assertSame("node-index", nodeIndex());
assertSame("standard-index", standardIndex());


let {foo: nodePackage} = require("./resources/node_package");
let {foo: standardPackage} = require("./resources/standard_package");

assertSame("node-package", nodePackage());
assertSame("standard-package", standardPackage());


let {main: nodePackageMain} = require("./resources/node_package/package");
let {main: standardPackageMain} = require("./resources/node_package/package");

assertSame("main.js", nodePackageMain);
assertSame("main.js", standardPackageMain);

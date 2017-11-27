/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

import nodeDefault, {foo as nodeFoo} from "./resources/node_dependency.js";
import standardDefault, {foo as standardFoo} from "./resources/standard_dependency.js";

assertSame("node", nodeDefault());
assertSame("standard", standardDefault());

assertSame("node", nodeFoo());
assertSame("standard", standardFoo());


import* as nodeModule from "./resources/node_dependency";
import* as standardModule from "./resources/standard_dependency";

assertSame("node", nodeModule.default());
assertSame("standard", standardModule.default());

assertSame("node", nodeModule.foo());
assertSame("standard", standardModule.foo());


import {foo as nodeIndex} from "./resources/node_index";
import {foo as standardIndex} from "./resources/standard_index";

assertSame("node-index", nodeIndex());
assertSame("standard-index", standardIndex());


import {foo as nodePackage} from "./resources/node_package";
import {foo as standardPackage} from "./resources/standard_package";

assertSame("node-package", nodePackage());
assertSame("standard-package", standardPackage());


import {main as nodePackageMain} from "./resources/node_package/package";
import {main as standardPackageMain} from "./resources/node_package/package";

assertSame("main.js", nodePackageMain);
assertSame("main.js", standardPackageMain);

/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

import defaultExpr from "./resources/export_default_expression.jsm";
import defaultFunDecl from "./resources/export_default_function_declaration.jsm";
import defaultGenDecl from "./resources/export_default_generator_declaration.jsm";
import defaultNamedFunDecl from "./resources/export_default_named_function_declaration.jsm";
import defaultNamedGenDecl from "./resources/export_default_named_generator_declaration.jsm";

assertSame("default-export-expression", defaultExpr);

assertSame("default", defaultFunDecl.name);
assertSame("default-export-function-declaration", defaultFunDecl());

assertSame("default", defaultGenDecl.name);
assertSame("default-export-generator-declaration", defaultGenDecl().next().value);

assertSame("fnamed", defaultNamedFunDecl.name);
assertSame("default-export-named-function-declaration", defaultNamedFunDecl());

assertSame("gnamed", defaultNamedGenDecl.name);
assertSame("default-export-named-generator-declaration", defaultNamedGenDecl().next().value);

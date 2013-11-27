/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertDataProperty
} = Assert;

function assertFunctionName(f, name) {
  return assertDataProperty(f, "name", {value: name, writable: false, enumerable: false, configurable: true});
}

function assertMethodName(o, pk, name) {
  let desc = Object.getOwnPropertyDescriptor(o, pk);
  let m = 'value' in desc ? desc.value : desc.get !== void 0 ? desc.get : desc.set;
  return assertFunctionName(m, name);
}

// method definition in class (prototype)
{
  const symbolWithoutDescription = Symbol();
  const symbolWithDescription = Symbol("desc");
  const symbolWithEmptyDescription = Symbol("");

  // 14.3.9  Runtime Semantics: PropertyDefinitionEvaluation
  // - MethodDefinition : PropertyName (StrictFormalParameters ) { FunctionBody }
  // - MethodDefinition : get PropertyName ( ) { FunctionBody }
  // - MethodDefinition : set PropertyName ( PropertySetParameterList ) { FunctionBody }
  // 14.4.13 Runtime Semantics: PropertyDefinitionEvaluation
  // - GeneratorMethod : * PropertyName ( StrictFormalParameters ) { FunctionBody }
  let m0 = class {m(){}};
  assertMethodName(m0.prototype, "m", "m");

  let g0 = class {get g(){}};
  assertMethodName(g0.prototype, "g", "get g");

  let s0 = class {set s(_){}};
  assertMethodName(s0.prototype, "s", "set s");

  let gm0 = class {*gm(){}};
  assertMethodName(gm0.prototype, "gm", "gm");

  let m1 = class {["m"](){}};
  assertMethodName(m1.prototype, "m", "m");

  let g1 = class {get ["g"](){}};
  assertMethodName(g1.prototype, "g", "get g");

  let s1 = class {set ["s"](_){}};
  assertMethodName(s1.prototype, "s", "set s");

  let gm1 = class {*["gm"](){}};
  assertMethodName(gm1.prototype, "gm", "gm");

  let m2 = class {[symbolWithoutDescription](){}};
  assertMethodName(m2.prototype, symbolWithoutDescription, "");

  let g2 = class {get [symbolWithoutDescription](){}};
  assertMethodName(g2.prototype, symbolWithoutDescription, "");

  let s2 = class {set [symbolWithoutDescription](_){}};
  assertMethodName(s2.prototype, symbolWithoutDescription, "");

  let gm2 = class {*[symbolWithoutDescription](){}};
  assertMethodName(gm2.prototype, symbolWithoutDescription, "");

  let m3 = class {[symbolWithDescription](){}};
  assertMethodName(m3.prototype, symbolWithDescription, "[desc]");

  let g3 = class {get [symbolWithDescription](){}};
  assertMethodName(g3.prototype, symbolWithDescription, "get [desc]");

  let s3 = class {set [symbolWithDescription](_){}};
  assertMethodName(s3.prototype, symbolWithDescription, "set [desc]");

  let gm3 = class {*[symbolWithDescription](){}};
  assertMethodName(gm3.prototype, symbolWithDescription, "[desc]");

  let m4 = class {[symbolWithEmptyDescription](){}};
  assertMethodName(m4.prototype, symbolWithEmptyDescription, "[]");

  let g4 = class {get [symbolWithEmptyDescription](){}};
  assertMethodName(g4.prototype, symbolWithEmptyDescription, "get []");

  let s4 = class {set [symbolWithEmptyDescription](_){}};
  assertMethodName(s4.prototype, symbolWithEmptyDescription, "set []");

  let gm4 = class {*[symbolWithEmptyDescription](){}};
  assertMethodName(gm4.prototype, symbolWithEmptyDescription, "[]");
}

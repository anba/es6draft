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

// method definition in class (static)
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
  let m0 = class {static m(){}};
  assertMethodName(m0, "m", "m");

  let g0 = class {static get g(){}};
  assertMethodName(g0, "g", "get g");

  let s0 = class {static set s(_){}};
  assertMethodName(s0, "s", "set s");

  let gm0 = class {static *gm(){}};
  assertMethodName(gm0, "gm", "gm");

  let m1 = class {static ["m"](){}};
  assertMethodName(m1, "m", "m");

  let g1 = class {static get ["g"](){}};
  assertMethodName(g1, "g", "get g");

  let s1 = class {static set ["s"](_){}};
  assertMethodName(s1, "s", "set s");

  let gm1 = class {static *["gm"](){}};
  assertMethodName(gm1, "gm", "gm");

  let m2 = class {static [symbolWithoutDescription](){}};
  assertMethodName(m2, symbolWithoutDescription, "");

  let g2 = class {static get [symbolWithoutDescription](){}};
  assertMethodName(g2, symbolWithoutDescription, "");

  let s2 = class {static set [symbolWithoutDescription](_){}};
  assertMethodName(s2, symbolWithoutDescription, "");

  let gm2 = class {static *[symbolWithoutDescription](){}};
  assertMethodName(gm2, symbolWithoutDescription, "");

  let m3 = class {static [symbolWithDescription](){}};
  assertMethodName(m3, symbolWithDescription, "[desc]");

  let g3 = class {static get [symbolWithDescription](){}};
  assertMethodName(g3, symbolWithDescription, "get [desc]");

  let s3 = class {static set [symbolWithDescription](_){}};
  assertMethodName(s3, symbolWithDescription, "set [desc]");

  let gm3 = class {static *[symbolWithDescription](){}};
  assertMethodName(gm3, symbolWithDescription, "[desc]");

  let m4 = class {static [symbolWithEmptyDescription](){}};
  assertMethodName(m4, symbolWithEmptyDescription, "[]");

  let g4 = class {static get [symbolWithEmptyDescription](){}};
  assertMethodName(g4, symbolWithEmptyDescription, "get []");

  let s4 = class {static set [symbolWithEmptyDescription](_){}};
  assertMethodName(s4, symbolWithEmptyDescription, "set []");

  let gm4 = class {static *[symbolWithEmptyDescription](){}};
  assertMethodName(gm4, symbolWithEmptyDescription, "[]");
}

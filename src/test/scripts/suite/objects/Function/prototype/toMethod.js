/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertBuiltinFunction,
  assertSame,
  assertNotSame,
  assertThrows,
  assertFalse,
} = Assert;


/* 19.2.3.5 Function.prototype.toMethod (superBinding, methodName = undefined) */

assertBuiltinFunction(Function.prototype.toMethod, "toMethod", 1);

const ToMethod = Function.prototype.toMethod.call.bind(Function.prototype.toMethod);

// Function.prototype.toMethod() overwrites .constructor with rebound .constructor from source (explicit constructor)
{
  let log = "";
  class SourceBase {
    constructor() { log += "[SourceBase]"; }
  }
  class Source extends SourceBase {
    constructor() { super(); log += "[Source]"; }
  }
  class TargetBase {
    constructor() { log += "[TargetBase]"; }
  }
  class Target extends TargetBase {
    constructor() { super(); log += "[Target]"; }
  }

  assertSame(Source, Source.prototype.constructor);
  assertSame(Target, Target.prototype.constructor);

  log = "";  
  assertSame(Source, (new Source).constructor);
  assertSame("[SourceBase][Source]", log);

  log = "";  
  assertSame(Target, (new Target).constructor);
  assertSame("[TargetBase][Target]", log);

  Target.prototype.constructor = ToMethod(Source.prototype.constructor, Target.prototype);
  assertNotSame(Target, Target.prototype.constructor);

  log = "";
  assertNotSame(Target, (new Target).constructor);
  assertSame("[TargetBase][Target]", log);

  log = "";
  assertNotSame(Target, (new Target.prototype.constructor).constructor);
  assertSame("[TargetBase][Source]", log);
}

// Function.prototype.toMethod() overwrites .constructor with rebound .constructor from source (implicit constructor)
{
  let log = "";
  class SourceBase {
    constructor() { log += "[SourceBase]"; }
  }
  class Source extends SourceBase {
  }
  class TargetBase {
    constructor() { log += "[TargetBase]"; }
  }
  class Target extends TargetBase {
  }

  assertSame(Source, Source.prototype.constructor);
  assertSame(Target, Target.prototype.constructor);

  log = "";  
  assertSame(Source, (new Source).constructor);
  assertSame("[SourceBase]", log);

  log = "";  
  assertSame(Target, (new Target).constructor);
  assertSame("[TargetBase]", log);

  Target.prototype.constructor = ToMethod(Source.prototype.constructor, Target.prototype);
  assertNotSame(Target, Target.prototype.constructor);

  log = "";
  assertNotSame(Target, (new Target).constructor);
  assertSame("[TargetBase]", log);

  log = "";
  assertNotSame(Target, (new Target.prototype.constructor).constructor);
  assertSame("[TargetBase]", log);
}

// Function.prototype.toMethod() rebinds [[HomeObject]]
{
  let log = "";
  class SourceBase {
    fn() { log += "[SourceBase]"; }
  }
  class Source extends SourceBase {
    fn() { super(); log += "[Source]"; }
  }
  class TargetBase {
    fn() { log += "[TargetBase]"; }
  }
  class Target extends TargetBase {
  }

  log = "";  
  (new Source).fn();
  assertSame("[SourceBase][Source]", log);

  log = "";  
  (new Target).fn();
  assertSame("[TargetBase]", log);

  Target.prototype.fn = ToMethod(Source.prototype.fn, Target.prototype);

  log = "";
  (new Target).fn();
  assertSame("[TargetBase][Source]", log);
}

// Function.prototype.toMethod() copies internal slots ([[Prototype]]) (1)
{
  class Source {
    fn() { super(); }
  }
  class Target { }

  class XFunction extends Function { }

  Object.setPrototypeOf(Source.prototype.fn, XFunction.prototype);
  assertSame(Object.getPrototypeOf(Source.prototype.fn), XFunction.prototype);

  Target.prototype.fn = ToMethod(Source.prototype.fn, Target.prototype);

  assertNotSame(Source.prototype.fn, Target.prototype.fn);
  assertSame(Object.getPrototypeOf(Source.prototype.fn), Object.getPrototypeOf(Target.prototype.fn));
}

// Function.prototype.toMethod() copies internal slots ([[Prototype]]) (2)
{
  class Source {
    fn() { super(); }
  }
  class Target { }

  Object.setPrototypeOf(Source.prototype.fn, null);
  assertSame(Object.getPrototypeOf(Source.prototype.fn), null);

  Target.prototype.fn = ToMethod(Source.prototype.fn, Target.prototype);

  assertNotSame(Source.prototype.fn, Target.prototype.fn);
  assertSame(Object.getPrototypeOf(Source.prototype.fn), Object.getPrototypeOf(Target.prototype.fn));
}

// Function.prototype.toMethod() copies internal slots ([[Extensible]])
{
  class Source {
    fn() { super(); }
  }
  class Target { }

  Object.preventExtensions(Source.prototype.fn);
  assertFalse(Object.isExtensible(Source.prototype.fn));

  Target.prototype.fn = ToMethod(Source.prototype.fn, Target.prototype);

  assertNotSame(Source.prototype.fn, Target.prototype.fn);
  assertFalse(Object.isExtensible(Target.prototype.fn));
}

// Function.prototype.toMethod() copies internal slots ([[Realm]]) (1)
{
  const foreignRealm = new Reflect.Realm();
  assertNotSame(foreignRealm.global.TypeError, TypeError);

  class Source {
    fn() { [](); super(); }
  }
  let Target = foreignRealm.eval(`
    class Target { }
    Target;
  `);

  Target.prototype.fn = ToMethod(Source.prototype.fn, Target.prototype);

  // Source is from current realm!
  assertThrows(() => (new Target).fn(), TypeError);
}

// Function.prototype.toMethod() copies internal slots ([[Realm]]) (2)
{
  const foreignRealm = new Reflect.Realm();
  assertNotSame(foreignRealm.global.TypeError, TypeError);

  let Source = foreignRealm.eval(`
    class Source {
      fn() { [](); super(); }
    }
    Source;
  `);
  class Target { }

  Target.prototype.fn = ToMethod(Source.prototype.fn, Target.prototype);

  // Source is from another realm!
  assertThrows(() => (new Target).fn(), foreignRealm.global.TypeError);
}

// Function.prototype.toMethod() copies internal slots (%ThrowTypeError% in 'caller' and 'arguments') (1)
{
  const foreignRealm = new Reflect.Realm();
  const ThrowTypeError = Object.getOwnPropertyDescriptor(function(){"use strict"}, "caller").get;

  class Source {
    fn() { super(); }
  }
  let Target = foreignRealm.eval(`
    class Target { }
    Target;
  `);

  Target.prototype.fn = ToMethod(Source.prototype.fn, Target.prototype);

  // .caller and .arguments are copied like just like other properties
  assertSame(ThrowTypeError, Object.getOwnPropertyDescriptor(Target.prototype.fn, "caller").get);
  assertSame(ThrowTypeError, Object.getOwnPropertyDescriptor(Target.prototype.fn, "caller").set);
  assertSame(ThrowTypeError, Object.getOwnPropertyDescriptor(Target.prototype.fn, "arguments").get);
  assertSame(ThrowTypeError, Object.getOwnPropertyDescriptor(Target.prototype.fn, "arguments").set);
}

// Function.prototype.toMethod() copies internal slots (%ThrowTypeError% in 'caller' and 'arguments') (2)
{
  const foreignRealm = new Reflect.Realm();
  const ThrowTypeError = Object.getOwnPropertyDescriptor(function(){"use strict"}, "caller").get;
  const ForeignThrowTypeError = Object.getOwnPropertyDescriptor(foreignRealm.eval(`(function(){"use strict"})`), "caller").get;

  assertNotSame(ThrowTypeError, ForeignThrowTypeError);

  let Source = foreignRealm.eval(`
    class Source {
      fn() { super(); }
    }
    Source;
  `);
  class Target { }

  Target.prototype.fn = ToMethod(Source.prototype.fn, Target.prototype);

  // .caller and .arguments are copied like just like other properties
  assertSame(ForeignThrowTypeError, Object.getOwnPropertyDescriptor(Target.prototype.fn, "caller").get);
  assertSame(ForeignThrowTypeError, Object.getOwnPropertyDescriptor(Target.prototype.fn, "caller").set);
  assertSame(ForeignThrowTypeError, Object.getOwnPropertyDescriptor(Target.prototype.fn, "arguments").get);
  assertSame(ForeignThrowTypeError, Object.getOwnPropertyDescriptor(Target.prototype.fn, "arguments").set);
}

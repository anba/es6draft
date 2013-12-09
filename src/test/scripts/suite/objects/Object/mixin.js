/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertBuiltinFunction,
  assertSame,
  assertNotSame,
  assertThrows,
} = Assert;


/* 19.1.2.15  Object.mixin ( target, source ) */

assertBuiltinFunction(Object.mixin, "mixin", 2);

// Object.mixin() overwrites .constructor with rebound .constructor from source (explicit constructor)
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

  Object.mixin(Target.prototype, Source.prototype);
  assertNotSame(Target, Target.prototype.constructor);

  log = "";
  assertNotSame(Target, (new Target).constructor);
  assertSame("[TargetBase][Target]", log);

  log = "";
  assertNotSame(Target, (new Target.prototype.constructor).constructor);
  assertSame("[TargetBase][Source]", log);
}

// Object.mixin() overwrites .constructor with rebound .constructor from source (implicit constructor)
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

  Object.mixin(Target.prototype, Source.prototype);
  assertNotSame(Target, Target.prototype.constructor);

  log = "";
  assertNotSame(Target, (new Target).constructor);
  assertSame("[TargetBase]", log);

  log = "";
  assertNotSame(Target, (new Target.prototype.constructor).constructor);
  assertSame("[TargetBase]", log);
}

// Object.mixin() rebinds [[HomeObject]]
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

  Object.mixin(Target.prototype, Source.prototype);

  log = "";
  (new Target).fn();
  assertSame("[TargetBase][Source]", log);
}

// Object.mixin() copies internal slots ([[Prototype]]) (1)
{
  class SourceBase {
    fn() { }
  }
  class Source extends SourceBase {
    fn() { super(); }
  }
  class TargetBase {
    fn() { }
  }
  class Target extends TargetBase { }

  class XFunction extends Function { }

  Object.setPrototypeOf(Source.prototype.fn, XFunction.prototype);
  assertNotSame(Object.getPrototypeOf(Target.prototype.fn), XFunction.prototype);

  Object.mixin(Target.prototype, Source.prototype);

  assertNotSame(Source.prototype.fn, Target.prototype.fn);
  assertSame(Object.getPrototypeOf(Source.prototype.fn), Object.getPrototypeOf(Target.prototype.fn));
}

// Object.mixin() copies internal slots ([[Prototype]]) (2)
{
  class SourceBase {
    fn() { }
  }
  class Source extends SourceBase {
    fn() { super(); }
  }
  class TargetBase {
    fn() { }
  }
  class Target extends TargetBase { }

  Object.setPrototypeOf(Source.prototype.fn, null);
  assertNotSame(Object.getPrototypeOf(Target.prototype.fn), null);

  Object.mixin(Target.prototype, Source.prototype);

  assertNotSame(Source.prototype.fn, Target.prototype.fn);
  assertSame(Object.getPrototypeOf(Source.prototype.fn), Object.getPrototypeOf(Target.prototype.fn));
}

// Object.mixin() copies internal slots ([[Realm]]) (1)
{
  const foreignRealm = new Realm();
  assertNotSame(foreignRealm.global.TypeError, TypeError);

  class Source {
    fn() { [](); super(); }
  }
  let Target = foreignRealm.eval(`
    class Target { }
    Target;
  `);

  Object.mixin(Target.prototype, Source.prototype);

  // Source is from current realm!
  assertThrows(() => (new Target).fn(), TypeError);
}

// Object.mixin() copies internal slots ([[Realm]]) (2)
{
  const foreignRealm = new Realm();
  assertNotSame(foreignRealm.global.TypeError, TypeError);

  let Source = foreignRealm.eval(`
    class Source {
      fn() { [](); super(); }
    }
    Source;
  `);
  class Target { }

  Object.mixin(Target.prototype, Source.prototype);

  // Source is from another realm!
  assertThrows(() => (new Target).fn(), foreignRealm.global.TypeError);
}

// Object.mixin() copies internal slots (%ThrowTypeError% in 'caller' and 'arguments') (1)
{
  const foreignRealm = new Realm();
  const ThrowTypeError = Object.getOwnPropertyDescriptor(function(){"use strict"}, "caller").get;

  class Source {
    fn() { super(); }
  }
  let Target = foreignRealm.eval(`
    class Target { }
    Target;
  `);

  Object.mixin(Target.prototype, Source.prototype);

  // .caller and .arguments are copied like just like other properties
  assertSame(ThrowTypeError, Object.getOwnPropertyDescriptor(Target.prototype.fn, "caller").get);
  assertSame(ThrowTypeError, Object.getOwnPropertyDescriptor(Target.prototype.fn, "caller").set);
  assertSame(ThrowTypeError, Object.getOwnPropertyDescriptor(Target.prototype.fn, "arguments").get);
  assertSame(ThrowTypeError, Object.getOwnPropertyDescriptor(Target.prototype.fn, "arguments").set);
}

// Object.mixin() copies internal slots (%ThrowTypeError% in 'caller' and 'arguments') (2)
{
  const foreignRealm = new Realm();
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

  Object.mixin(Target.prototype, Source.prototype);

  // .caller and .arguments are copied like just like other properties
  assertSame(ForeignThrowTypeError, Object.getOwnPropertyDescriptor(Target.prototype.fn, "caller").get);
  assertSame(ForeignThrowTypeError, Object.getOwnPropertyDescriptor(Target.prototype.fn, "caller").set);
  assertSame(ForeignThrowTypeError, Object.getOwnPropertyDescriptor(Target.prototype.fn, "arguments").get);
  assertSame(ForeignThrowTypeError, Object.getOwnPropertyDescriptor(Target.prototype.fn, "arguments").set);
}

/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertThrows, fail
} = Assert;

// Reference type and implementation reality
// https://bugs.ecmascript.org/show_bug.cgi?id=4379

class DummyError { }

for (let undefinedOrNull of [undefined, null]) {
  assertThrows(DummyError, () => {
    let base = undefinedOrNull;
    let prop = () => { throw new DummyError(); };
    base[prop()]++;
  });

  assertThrows(TypeError, () => {
    let base = undefinedOrNull;
    let prop = {
      toString() {
        fail `property key evaluated`;
      }
    };
    base[prop]++;
  });

  assertThrows(DummyError, () => {
    let base = undefinedOrNull;
    let prop = () => { throw new DummyError(); };
    let expr = () => fail `right-hand side expression evaluated`;
    base[prop()] = expr();
  });

  assertThrows(TypeError, () => {
    let base = undefinedOrNull;
    let prop = {
      toString() {
        fail `property key evaluated`;
      }
    };
    let expr = () => fail `right-hand side expression evaluated`;
    base[prop] = expr();
  });

  assertThrows(DummyError, () => {
    let base = undefinedOrNull;
    let prop = () => { throw new DummyError(); };
    let expr = () => fail `right-hand side expression evaluated`;
    base[prop()] += expr();
  });

  assertThrows(TypeError, () => {
    let base = undefinedOrNull;
    let prop = {
      toString() {
        fail `property key evaluated`;
      }
    };
    let expr = () => fail `right-hand side expression evaluated`;
    base[prop] += expr();
  });
}

assertThrows(DummyError, () => {
  let base = {};
  let prop = {
    toString() {
      throw new DummyError();
    }
  };
  let expr = () => fail `right-hand side expression evaluated`;
  base[prop] = expr();
});

assertThrows(DummyError, () => {
  let base = {};
  let prop = {
    toString() {
      throw new DummyError();
    }
  };
  let expr = () => fail `right-hand side expression evaluated`;
  base[prop] += expr();
});

{
  let propKeyEvaluated = false;
  let base = {};
  let prop = {
    toString() {
      assertFalse(propKeyEvaluated);
      propKeyEvaluated = true;
      return 1;
    }
  };
  base[prop]++;
}

{
  let propKeyEvaluated = false;
  let base = {};
  let prop = {
    toString() {
      assertFalse(propKeyEvaluated);
      propKeyEvaluated = true;
      return "";
    }
  };
  let expr = () => 0;
  base[prop] = expr();
}

{
  let propKeyEvaluated = false;
  let base = {};
  let prop = {
    toString() {
      assertFalse(propKeyEvaluated);
      propKeyEvaluated = true;
      return "";
    }
  };
  let expr = () => 0;
  base[prop] += expr();
}

/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, fail
} = Assert;

// 12.14.5.2, 13.2.2.4, 13.2.3.6, 13.2.3.7: Missing ReturnIfAbrupt after ResolveBinding
// https://bugs.ecmascript.org/show_bug.cgi?id=3624

class Err extends Error {}

function MakeProxy(id) {
  return new Proxy({}, {
    has(t, pk) {
      if (pk === id) {
        throw new Err;
      }
      return Reflect.has(t, pk);
    },
    set(t, pk, r) {
      if (pk === id) {
        fail `unexpected [[Set]] on ${id}`;
      }
      return Reflect.set(t, pk, r);
    }
  });
}

// 12.14.5.2 Runtime Semantics: DestructuringAssignmentEvaluation
// AssignmentProperty : IdentifierReference Initializer_opt
assertThrows(Err, () => {
  var foo;
  with (MakeProxy("foo")) {
    ({foo} = {});
  }
});

// 13.2.2.4 Runtime Semantics: Evaluation
// VariableDeclaration : BindingIdentifier Initializer
assertThrows(Err, () => {
  with (MakeProxy("foo")) {
    var foo = 0;
  }
});

// 13.2.3.6 Runtime Semantics: IteratorBindingInitialization
// SingleNameBinding : BindingIdentifier Initializer_opt
assertThrows(Err, () => {
  with (MakeProxy("foo")) {
    var [foo] = [];
  }
});

// 13.2.3.6 Runtime Semantics: IteratorBindingInitialization
// BindingRestElement : ... BindingIdentifier
assertThrows(Err, () => {
  with (MakeProxy("foo")) {
    var [...foo] = [];
  }
});

// 13.2.3.7 Runtime Semantics: KeyedBindingInitialization
// SingleNameBinding : BindingIdentifier Initializer_opt
assertThrows(Err, () => {
  with (MakeProxy("foo")) {
    var {foo} = {};
  }
});

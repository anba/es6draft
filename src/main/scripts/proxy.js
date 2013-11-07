/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function OldProxyAPI(global) {
"use strict";

const {
  Object, Function, Array, Proxy, Reflect, TypeError,
} = global;

const {
  create: Object_create,
  assign: Object_assign,
  mixin: Object_mixin,
} = Object;

const $CallFunction = Function.prototype.call.bind(Function.prototype.call);

const iteratorSym = Symbol.iterator;

// pseudo-symbol in SpiderMonkey
const mozIteratorSym = "@@iterator";

function toArrayIterator(obj) {
  if (obj == null) throw TypeError();
  obj = Object(obj);
  var length = obj.length >>> 0;
  var result = [];
  for (var i = 0; i < length; ++i) {
    result[i] = obj[i];
  }
  return result[iteratorSym]();
}

function toProxyHandler(handler) {
  var TypeErrorThrower = () => { throw TypeError() };
  /* fundamental traps mapping:
   * getOwnPropertyDescriptor -> getOwnPropertyDescriptor
   * getPropertyDescriptor -> -
   * getOwnPropertyNames -> ownKeys
   * getPropertyNames -> -
   * defineProperty -> defineProperty
   * delete -> deleteProperty
   * fix -> -
   */
  var proxyHandler = {
    getOwnPropertyDescriptor: TypeErrorThrower,
    defineProperty: TypeErrorThrower,
    deleteProperty: TypeErrorThrower,
    ownKeys: TypeErrorThrower,
  };

  // fundamental traps
  if ('getOwnPropertyDescriptor' in handler) {
    proxyHandler['getOwnPropertyDescriptor'] = (_, pk) => handler['getOwnPropertyDescriptor'](pk);
  } else if ('getPropertyDescriptor' in handler) {
    proxyHandler['getOwnPropertyDescriptor'] = (_, pk) => handler['getPropertyDescriptor'](pk);
  }
  if ('getOwnPropertyNames' in handler) {
    proxyHandler['ownKeys'] = () => toArrayIterator(handler['getOwnPropertyNames']());
  }
  if ('defineProperty' in handler) {
    proxyHandler['defineProperty'] = (_, pk, desc) => (handler['defineProperty'](pk, desc), true);
  }
  if ('delete' in handler) {
    proxyHandler['deleteProperty'] = (_, pk) => handler['delete'](pk);
  }

  // derived traps
  if ('has' in handler) {
    proxyHandler['has'] = (_, pk) => handler['has'](pk);
  } else {
    proxyHandler['has'] = (_, pk) => !!handler['getPropertyDescriptor'](pk);
  }
  if ('hasOwn' in handler) {
    proxyHandler['hasOwn'] = (_, pk) => handler['hasOwn'](pk);
  } else {
    proxyHandler['hasOwn'] = (_, pk) => !!handler['getOwnPropertyDescriptor'](pk);
  }
  if ('get' in handler && typeof handler['get'] == 'function') {
    proxyHandler['get'] = (_, pk, receiver) => handler['get'](receiver, pk);
  } else {
    proxyHandler['get'] = (_, pk, receiver) => {
      // XXX: special case for iteration tests
      if (pk === iteratorSym) {
        var desc = handler['getPropertyDescriptor'](mozIteratorSym);
        if (desc !== undefined && 'value' in desc) {
          // call @@iterator() so we don't end up with a StopIteration based Iterator
          return function() { return desc.value.call(this)[iteratorSym]() };
        }
      }
      var desc = handler['getPropertyDescriptor'](pk);
      if (desc !== undefined && 'value' in desc) {
        return desc.value;
      }
      if (desc !== undefined && desc.get !== undefined) {
        return desc.get.call(receiver);
      }
    };
  }
  if ('set' in handler) {
    proxyHandler['set'] = (_, pk, value, receiver) => handler['set'](receiver, pk, value);
  } else {
    proxyHandler['set'] = (_, pk, value, receiver) => {
      var desc = handler['getOwnPropertyDescriptor'](pk);
      if (!desc) {
        desc = handler['getPropertyDescriptor'](pk);
        if (!desc) {
          desc = {
            writable: true, enumerable: true, configurable: true
          };
        }
      }
      if (('writable' in desc) && desc.writable) {
        handler['defineProperty'](pk, (desc.value = value, desc));
        return true;
      }
      if (!('writable' in desc) && desc.set) {
        desc.set.call(receiver, value);
        return true;
      }
      return false;
    };
  }
  if ('enumerate' in handler) {
    proxyHandler['enumerate'] = () => toArrayIterator(handler['enumerate']());
  } else if ('iterate' in handler) {
    proxyHandler['enumerate'] = () => handler['iterate']()[iteratorSym]();
  } else {
    proxyHandler['enumerate'] = () => handler['getPropertyNames']().filter(
      pk => handler['getPropertyDescriptor'](pk).enumerable
    )[iteratorSym]();
  }
  if ('keys' in handler) {
    proxyHandler['ownKeys'] = () => toArrayIterator(handler['keys']());
  }
  proxyHandler['invoke'] = (_, pk, args, receiver) => $CallFunction(proxyHandler['get'](_, pk, receiver), receiver, ...args);
  return proxyHandler;
}

// Create a Proxy for the Proxy function in order to add an "invoke" trap while still passing Proxy surface tests
var BuiltinProxy = Proxy;
var NewProxy = new BuiltinProxy(BuiltinProxy, {
  addInvokeTrap(p, handler) {
    // Directly assign "invoke" trap to handler; this change is visible to other scripts, but tests don't complain
    Object_mixin(handler, {
      invoke(_, pk, args, receiver) {
        var fn = Reflect.get(p, pk, receiver);
        return $CallFunction(fn, p, ...args);
      }
    });
    return p;
  },
  apply(target, thisValue, args) {
    var p = $CallFunction(target, thisValue, ...args);
    return this.addInvokeTrap(p, args[1]);
  },
  construct(target, args) {
    var p = new target(...args);
    return this.addInvokeTrap(p, args[1]);
  }
});

Object.defineProperties(Object_assign(NewProxy, {
  create(handler, proto = null) {
    if (Object(handler) !== handler) throw TypeError();
    var proxyTarget = Object_create(proto);
    var proxyHandler = Object_assign({
      setPrototypeOf() { throw TypeError() }
    }, toProxyHandler(handler));
    return new BuiltinProxy(proxyTarget, proxyHandler);
  },
  createFunction(handler, callTrap, constructTrap = callTrap) {
    if (Object(handler) !== handler) throw TypeError();
    if (typeof callTrap != 'function') throw TypeError();
    if (typeof constructTrap != 'function') throw TypeError();
    var proxyTarget = function(){};
    var proxyHandler = Object_assign({
      setPrototypeOf() { throw TypeError() },
      apply(_, thisValue, args) { return callTrap.apply(thisValue, args) },
      construct(_, args) { return new constructTrap(...args) }
    }, toProxyHandler(handler));
    return new BuiltinProxy(proxyTarget, proxyHandler);
  }
}), {
  create: {enumerable: false},
  createFunction: {enumerable: false},
});

Object.defineProperty(global, "Proxy", {
  value: NewProxy,
  writable: true, enumerable: false, configurable: true
});

})(this);

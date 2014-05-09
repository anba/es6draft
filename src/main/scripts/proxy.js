/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function OldProxyAPI() {
"use strict";

const global = %GlobalObject();

const {
  Object, Function, Array, Proxy, Reflect, Symbol, TypeError,
} = global;

const {
  create: Object_create,
  assign: Object_assign,
} = Object;

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
    proxyHandler['ownKeys'] = () => handler['getOwnPropertyNames']();
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
    proxyHandler['ownKeys'] = () => handler['keys']();
  }
  return proxyHandler;
}

Object.defineProperties(Object_assign(Proxy, {
  create(handler, proto = null) {
    if (Object(handler) !== handler) throw TypeError();
    var proxyTarget = Object_create(proto);
    var proxyHandler = Object_assign({
      setPrototypeOf() { throw TypeError() }
    }, toProxyHandler(handler));
    return new Proxy(proxyTarget, proxyHandler);
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
    return new Proxy(proxyTarget, proxyHandler);
  }
}), {
  create: {enumerable: false},
  createFunction: {enumerable: false},
});

// Enable creating proxies without `new` for tests
const newProxy = new Proxy(Proxy, {
  apply(target, thisValue, args) {
    return new target(...args);
  }
});

// FIXME: Cannot replace intrinsic because intrinsics are restricted to ordinary objects...
// %SetIntrinsic("Proxy", newProxy);
global.Proxy = newProxy;

})();

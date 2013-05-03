/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function JSLegacyExtensions(global) {
"use strict";

const Object = global.Object,
      Function = global.Function,
      Array = global.Array,
      String = global.String,
      Boolean = global.Boolean,
      Number = global.Number,
      Math = global.Math,
      Date = global.Date,
      RegExp = global.RegExp,
      Error = global.Error,
      TypeError = global.TypeError,
      JSON = global.JSON,
      Proxy = global.Proxy,
      Map = global.Map,
      Set = global.Set,
      WeakMap = global.WeakMap,
      Reflect = global.Reflect;

const Object_defineProperty = Object.defineProperty,
      Object_getOwnPropertyDescriptor = Object.getOwnPropertyDescriptor,
      Object_getOwnPropertyNames = Object.getOwnPropertyNames,
      Object_hasOwnProperty = Function.prototype.call.bind(Object.prototype.hasOwnProperty),
      Object_keys = Object.keys;

Object.defineProperty(global, getSym("@@toStringTag"), {
  value: "global", writable: true, enumerable: false, configurable: true
});

Object.defineProperties(Object.assign(Object.prototype, {
  __defineGetter__(name, getter) {
    var obj = (this != null ? Object(this) : global);
    Object_defineProperty(obj, name, {get: getter, enumerable: true, configurable: true});
  },
  __defineSetter__(name, setter) {
    var obj = (this != null ? Object(this) : global);
    Object_defineProperty(obj, name, {set: setter, enumerable: true, configurable: true});
  },
  __lookupGetter__(name) {
    var p = this;
    do {
      var desc = Object_getOwnPropertyDescriptor(p, name);
      if (desc && desc.get) return desc.get;
    } while ((p = p.__proto__));
  },
  __lookupSetter__(name) {
    var p = this;
    do {
      var desc = Object_getOwnPropertyDescriptor(p, name);
      if (desc && desc.set) return desc.set;
    } while ((p = p.__proto__));
  }
}), {
  __defineGetter__: {enumerable: false},
  __defineSetter__: {enumerable: false},
  __lookupGetter__: {enumerable: false},
  __lookupSetter__: {enumerable: false},
});

const String_prototype_replace = String.prototype.replace;

Object.defineProperties(Object.assign(String.prototype, {
  trimLeft() {
    return String_prototype_replace.call(this, /^\s+/, "");
  },
  trimRight() {
    return String_prototype_replace.call(this, /\s+$/, "");
  },
}), {
  trimLeft: {enumerable: false},
  trimRight: {enumerable: false},
});

function toProxyHandler(handler) {
  var TypeErrorThrower = () => { throw TypeError() };
  var proxyHandler = {
    getOwnPropertyDescriptor: TypeErrorThrower,
    getPropertyDescriptor: TypeErrorThrower,
    getOwnPropertyNames: TypeErrorThrower,
    getPropertyNames: TypeErrorThrower,
    defineProperty: TypeErrorThrower,
    delete: TypeErrorThrower,
    ownKeys: TypeErrorThrower,
  };

  // fundamental traps
  if ('getOwnPropertyDescriptor' in handler) {
    proxyHandler['getOwnPropertyDescriptor'] = (_, pk) => handler['getOwnPropertyDescriptor'](pk);
  }
  if (!('getOwnPropertyDescriptor' in handler) && 'getPropertyDescriptor' in handler) {
    proxyHandler['getOwnPropertyDescriptor'] = (_, pk) => handler['getPropertyDescriptor'](pk);
  }
  if ('getOwnPropertyNames' in handler) {
    proxyHandler['ownKeys'] = () => Array.from(handler['getOwnPropertyNames']()).values();
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
  if ('get' in handler) {
    proxyHandler['get'] = (_, pk, receiver) => handler['get'](receiver, pk);
  } else {
    proxyHandler['get'] = (_, pk, receiver) => {
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
    proxyHandler['enumerate'] = () => Array.from(handler['enumerate']()).values();
  } else if ('iterate' in handler) {
    proxyHandler['enumerate'] = () => handler['iterate']();
  } else {
    proxyHandler['enumerate'] = () => handler['getPropertyNames'].filter(
      pk => handler['getPropertyDescriptor'](pk).enumerable
    ).values();
  }
  if ('keys' in handler) {
    proxyHandler['ownKeys'] = () => Array.from(handler['keys']()).values();
  }
  return proxyHandler;
}

Object.defineProperties(Object.assign(Proxy, {
  create(handler, proto = null) {
    if (Object(handler) !== handler) throw TypeError();
    var proxyTarget = Object.create(proto);
    var proxyHandler = Object.assign({
      setPrototypeOf() { throw TypeError() }
    }, toProxyHandler(handler));
    return new Proxy(proxyTarget, proxyHandler);
  },
  createFunction(handler, callTrap, constructTrap = callTrap) {
    if (Object(handler) !== handler) throw TypeError();
    if (typeof callTrap != 'function') throw TypeError();
    if (typeof constructTrap != 'function') throw TypeError();
    var proxyTarget = function(){};
    var proxyHandler = Object.assign({
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

})(this);

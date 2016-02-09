/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function OldProxyAPI() {
"use strict";

const global = %GlobalObject();

const {
  Object, Proxy, Symbol, TypeError, Reflect,
} = global;

const {
  create: Object_create,
  assign: Object_assign,
  keys: Object_keys,
} = Object;

const {
  getPrototypeOf: Reflect_getPrototypeOf,
  setPrototypeOf: Reflect_setPrototypeOf,
  isExtensible: Reflect_isExtensible,
  defineProperty: Reflect_defineProperty,
  apply: Reflect_apply,
  construct: Reflect_construct,
} = Reflect;

function toArray(obj) {
  if (obj == null) throw TypeError();
  obj = Object(obj);
  var length = obj.length >>> 0;
  var result = [];
  for (var i = 0; i < length; ++i) {
    result[i] = obj[i];
  }
  return result;
}

function NormalizeAndCompletePropertyDescriptor(obj) {
  if (obj == null) {
    return;
  }
  if (Object(obj) !== obj) {
    throw TypeError();
  }
  var enumerable, configurable, value, writable, get, set;
  var isDataProperty = false, isAccessorProperty = false;
  enumerable = 'enumerable' in obj ? !!obj.enumerable : false;
  configurable = 'configurable' in obj ? !!obj.configurable : false;
  if ('value' in obj) {
    isDataProperty = true;
    value = obj.value;
  }
  if ('writable' in obj) {
    isDataProperty = true;
    writable = !!obj.writable;
  } else {
    writable = false;
  }
  if ('get' in obj) {
    isAccessorProperty = true;
    get = obj.get;
    if (typeof get !== 'function' && get !== void 0) {
      throw TypeError();
    }
  }
  if ('set' in obj) {
    isAccessorProperty = true;
    set = obj.set;
    if (typeof set !== 'function' && set !== void 0) {
      throw TypeError();
    }
  }
  if (isDataProperty && isAccessorProperty) {
    throw TypeError();
  }
  if (isDataProperty) {
    return {__proto__: null, value, writable, enumerable, configurable};
  } else {
    return {__proto__: null, get, set, enumerable, configurable};
  }
}

function toProxyHandler(handler, callTrap = void 0, constructTrap = void 0) {
  return {
    __proto__: null,
    getPrototypeOf(target) {
      return Reflect_getPrototypeOf(target);
    },
    setPrototypeOf(target, p) {
      return Reflect_setPrototypeOf(target, p);
    },
    isExtensible(target) {
      return Reflect_isExtensible(target);
    },
    preventExtensions(target) {
      return false;
    },
    getOwnPropertyDescriptor(target, propertyKey) {
      var trapGetOwnPropertyDescriptor = handler['getOwnPropertyDescriptor'];
      if (typeof trapGetOwnPropertyDescriptor === 'function') {
        return Reflect_apply(trapGetOwnPropertyDescriptor, handler, [propertyKey]);
      }
      // Some tests only define the 'getPropertyDescriptor' trap.
      return handler['getPropertyDescriptor'](propertyKey);
    },
    defineProperty(target, propertyKey, desc) {
      handler['defineProperty'](propertyKey, desc);
      return true;
    },
    has(target, propertyKey) {
      var trapHas = handler['has'];
      if (typeof trapHas === 'function') {
        return Reflect_apply(trapHas, handler, [propertyKey]);
      }
      // Derived trap
      return handler['getPropertyDescriptor'](propertyKey) != null;
    },
    get(target, propertyKey, receiver) {
      var trapGet = handler['get'];
      if (typeof trapGet === 'function') {
        return Reflect_apply(trapGet, handler, [receiver, propertyKey]);
      }
      // Derived trap
      var desc = handler['getPropertyDescriptor'](propertyKey);
      desc = NormalizeAndCompletePropertyDescriptor(desc);
      if (desc !== void 0) {
        if ('value' in desc) {
          return desc.value;
        } else if (desc.get) {
          return Reflect_apply(desc.get, receiver, []);
        }
      }
    },
    set(target, propertyKey, value, receiver) {
      var trapSet = handler['set'];
      if (typeof trapSet === 'function') {
        return Reflect_apply(trapSet, handler, [receiver, propertyKey, value]);
      }
      // Derived trap
      var desc = handler['getOwnPropertyDescriptor'](propertyKey);
      desc = NormalizeAndCompletePropertyDescriptor(desc);
      if (desc) {
        if ('writable' in desc) {
          if (desc.writable) {
            handler['defineProperty'](propertyKey, {value});
            return true;
          }
        } else if (desc.set) {
          Reflect_apply(desc.set, receiver, [value]);
          return true;
        }
        return false;
      }
      desc = handler['getPropertyDescriptor'](propertyKey);
      desc = NormalizeAndCompletePropertyDescriptor(desc);
      if (desc) {
        if ('writable' in desc) {
          if (!desc.writable) {
            return false;
          }
        } else {
          if (desc.set) {
            Reflect_apply(desc.set, receiver, [value]);
            return true;
          }
          return false;
        }
      }
      if (!Reflect_isExtensible(receiver)) {
        return false;
      }
      // handler['defineProperty'](propertyKey, {
      //   value,
      //   writable: true,
      //   enumerable: true,
      //   configurable: true,
      // });
      // return true;
      return Reflect_defineProperty(receiver, propertyKey, {
        value,
        writable: true,
        enumerable: true,
        configurable: true,
      });
    },
    deleteProperty(target, propertyKey) {
      return handler['delete'](propertyKey);
    },
    enumerate(target) {
      // non-standard 'iterate' trap
      var trapIterate = handler['iterate'];
      if (typeof trapIterate === 'function') {
        return Reflect_apply(trapIterate, handler, [])[Symbol.iterator]();
      }
      // standard trap 'enumerate'
      var trapEnumerate = handler['enumerate'];
      if (typeof trapEnumerate === 'function') {
        return toArray(Reflect_apply(trapEnumerate, handler, []))[Symbol.iterator]();
      }
      // Derived trap
      var trapGetPropertyNames = handler['getPropertyNames'];
      if (typeof trapGetPropertyNames === 'function') {
        var names = Reflect_apply(trapGetPropertyNames, handler, []);
        var result = [];
        for (var i = 0, j = 0, len = names.length >>> 0; i < len; ++i) {
          var name = %ToString(names[i]);
          var desc = handler['getPropertyDescriptor'](name);
          desc = NormalizeAndCompletePropertyDescriptor(desc);
          if (desc && desc.enumerable) {
            result[j++] = name;
          }
        }
        return result[Symbol.iterator]();
      }
      // Non-standard derived trap
      var result = toArray(this.keys());
      for (var object = target; (object = Reflect_getPrototypeOf(object));) {
        result = [...result, ...Object_keys(object)];
      }
      return result[Symbol.iterator]();
    },
    keys(target) {
      var trapKeys = handler['keys'];
      if (typeof trapKeys === 'function') {
        return Reflect_apply(trapKeys, handler, []);
      }
      // Derived trap
      var names = handler['getOwnPropertyNames']();
      var result = [];
      for (var i = 0, j = 0, len = names.length >>> 0; i < len; ++i) {
        var name = %ToString(names[i]);
        var desc = handler['getOwnPropertyDescriptor'](name);
        desc = NormalizeAndCompletePropertyDescriptor(desc);
        if (desc && desc.enumerable) {
          result[j++] = name;
        }
      }
      return result;
    },
    ownKeys(target) {
      var trapKeys = handler['keys'];
      if (typeof trapKeys === 'function') {
        return Reflect_apply(trapKeys, handler, []);
      }
      return handler['getOwnPropertyNames']();
    },
    apply(target, thisValue, args) {
      return Reflect_apply(callTrap, thisValue, args);
    },
    construct(target, args, newTarget) {
      return Reflect_construct(constructTrap, args, newTarget);
    },
  };
}

Object.defineProperties(Object_assign(Proxy, {
  create(handler, proto = null) {
    if (Object(handler) !== handler) throw TypeError();
    var proxyTarget = Object_create(proto);
    var proxyHandler = toProxyHandler(handler);
    return new Proxy(proxyTarget, proxyHandler);
  },
  createFunction(handler, callTrap, constructTrap = callTrap) {
    if (Object(handler) !== handler) throw TypeError();
    if (typeof callTrap !== 'function') throw TypeError();
    if (typeof constructTrap !== 'function') throw TypeError();
    var proxyTarget = function(){};
    var proxyHandler = toProxyHandler(handler, callTrap, constructTrap);
    return new Proxy(proxyTarget, proxyHandler);
  }
}), {
  create: {enumerable: false},
  createFunction: {enumerable: false},
});

})();

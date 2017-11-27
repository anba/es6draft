/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
id: ...
info: Ensure IsArray is executed before Get(o, @@toStringTag).
description: >
  ...
features: [Symbol.toStringTag, Proxy]
---*/

var arrayProxyNoStringTag = Proxy.revocable([], {
  get: function(target, propertyKey, receiver) {
    assert.sameValue(propertyKey, Symbol.toStringTag);
    arrayProxyNoStringTag.revoke();
    return;
  }
});
assert.sameValue(Object.prototype.toString.call(arrayProxyNoStringTag.proxy), "[object Array]");

var arrayProxyWithStringTag = Proxy.revocable([], {
  get: function(target, propertyKey, receiver) {
    assert.sameValue(propertyKey, Symbol.toStringTag);
    arrayProxyWithStringTag.revoke();
    return "CustomArray";
  }
});
assert.sameValue(Object.prototype.toString.call(arrayProxyWithStringTag.proxy), "[object CustomArray]");


var objectProxyNoStringTag = Proxy.revocable({}, {
  get: function(target, propertyKey, receiver) {
    assert.sameValue(propertyKey, Symbol.toStringTag);
    objectProxyNoStringTag.revoke();
    return;
  }
});
assert.sameValue(Object.prototype.toString.call(objectProxyNoStringTag.proxy), "[object Object]");

var objectProxyWithStringTag = Proxy.revocable({}, {
  get: function(target, propertyKey, receiver) {
    assert.sameValue(propertyKey, Symbol.toStringTag);
    objectProxyWithStringTag.revoke();
    return "CustomObject";
  }
});
assert.sameValue(Object.prototype.toString.call(objectProxyWithStringTag.proxy), "[object CustomObject]");


var functionProxyNoStringTag = Proxy.revocable(function(){}, {
  get: function(target, propertyKey, receiver) {
    assert.sameValue(propertyKey, Symbol.toStringTag);
    functionProxyNoStringTag.revoke();
    return;
  }
});
assert.sameValue(Object.prototype.toString.call(functionProxyNoStringTag.proxy), "[object Function]");

var functionProxyWithStringTag = Proxy.revocable(function(){}, {
  get: function(target, propertyKey, receiver) {
    assert.sameValue(propertyKey, Symbol.toStringTag);
    functionProxyWithStringTag.revoke();
    return "CustomFunction";
  }
});
assert.sameValue(Object.prototype.toString.call(functionProxyWithStringTag.proxy), "[object CustomFunction]");

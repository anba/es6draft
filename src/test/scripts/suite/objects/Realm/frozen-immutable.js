/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue,
} = Assert;

const root = Reflect.Realm.immutableRoot();

const rootObjects = [
  {object: root, path: "root"},
  {object: root.global, path: "root.global"},
  {object: root.eval(`(function*(){}).constructor`), path: "GeneratorFunction"},
  {object: root.eval(`(async function(){}).constructor`), path: "AsyncFunction"},
  {object: root.eval(`(async function*(){}).constructor`), path: "AsyncGenerator"},
  {object: root.eval(`Object.getPrototypeOf([][Symbol.iterator]())`), path: "ArrayIterator"},
  {object: root.eval(`Object.getPrototypeOf((new Map)[Symbol.iterator]())`), path: "MapIterator"},
  {object: root.eval(`Object.getPrototypeOf((new Set)[Symbol.iterator]())`), path: "SetIterator"},
];

for (let {object, path} of visitAll(rootObjects)) {
  assertTrue(Object.isFrozen(object), path);
}

function* visitAll(list) {
  const visited = new Set();

  for (let {object, path} of list) {
    yield* visit(object, path);
  }

  function* visit(object, path) {
    if (visited.has(object)) {
      return;
    }
    visited.add(object);
    yield {object, path};
    let proto = Reflect.getPrototypeOf(object);
    if (proto) {
      yield* visit(proto, `${path}.__proto__`);
    }
    for (let k of Reflect.ownKeys(object)) {
      let desc = Reflect.getOwnPropertyDescriptor(object, k);
      if ("value" in desc) {
        let value = desc.value;
        if (typeof value === "function" || (typeof value === "object" && value !== null)) {
          yield* visit(value, `${path}[${String(k)}]`);
        }
      } else {
        if (desc.get) {
          yield* visit(desc.get, `${path}[get ${String(k)}]`);
        }
        if (desc.set) {
          yield* visit(desc.set, `${path}[set ${String(k)}]`);
        }
      }
    }
  }
}

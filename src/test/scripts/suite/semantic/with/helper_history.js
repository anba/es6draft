/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

function HasBindingFail({hidden, object}, property) {
  return [
    {name: "getOwnPropertyDescriptor", target: object, property: property, result: void 0},
    {name: "getPrototypeOf", target: object, result: Object.getPrototypeOf(hidden)},
  ];
}

function HasBindingSuccess({hidden, object}, property) {
  return [
    {name: "getOwnPropertyDescriptor", target: object, property: property, result: Object.getOwnPropertyDescriptor(hidden, property)},
    {name: "getOwnPropertyDescriptor", target: object, property: Symbol.unscopables, result: Object.getOwnPropertyDescriptor(hidden, Symbol.unscopables)},
  ];
}

function GetBindingValueFail({hidden, object}, property) {
  return [
    {name: "getOwnPropertyDescriptor", target: object, property: property, result: void 0},
    {name: "getPrototypeOf", target: object, result: Object.getPrototypeOf(hidden)},
  ];
}

function GetBindingValueSuccess({hidden, object}, property) {
  return [
    {name: "getOwnPropertyDescriptor", target: object, property: property, result: Object.getOwnPropertyDescriptor(hidden, property)},
    {name: "getOwnPropertyDescriptor", target: object, property: Symbol.unscopables, result: Object.getOwnPropertyDescriptor(hidden, Symbol.unscopables)},
  ];
}

function GetValue({hidden, object}, property, value) {
  return [
    {name: "get", target: object, property: property, receiver: object, result: value},
  ];
}

function BindingNotIntercepted({hidden, object}, {hidden: blackListHidden, object: blackListObject}, property) {
  return [
    {name: "get", target: object, property: Symbol.unscopables, receiver: object, result: blackListObject},
    {name: "getOwnPropertyDescriptor", target: blackListObject, property: property, result: Object.getOwnPropertyDescriptor(blackListHidden, property)},
  ];
}

function BindingIntercepted({hidden, object}, {hidden: blackListHidden, object: blackListObject}, property) {
  return [
    {name: "get", target: object, property: Symbol.unscopables, receiver: object, result: blackListObject},
    {name: "getOwnPropertyDescriptor", target: blackListObject, property: property, result: Object.getOwnPropertyDescriptor(blackListHidden, property)},
    {name: "getPrototypeOf", target: object, result: Object.getPrototypeOf(hidden)},
  ];
}

function WithLookup(history, hidden, fn) {
  let object = Recorder.watch(hidden, history);
  try { fn(object); } finally { Recorder.unwatch(object); }
  return {hidden, object};
}

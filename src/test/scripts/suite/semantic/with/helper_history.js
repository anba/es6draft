/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
function HasBindingFail({hidden, object}, property) {
  return [
    {name: "has", target: object, property: property, result: false},
  ];
}

function HasBindingSuccess({hidden, object}, property) {
  return [
    {name: "has", target: object, property: property, result: true},
    {name: "get", target: object, property: Symbol.unscopables, receiver: object, result: hidden[Symbol.unscopables]},
  ];
}

function GetBindingValueFail({hidden, object}, property) {
  return [
    {name: "has", target: object, property: property, result: false},
  ];
}

function GetBindingValueSuccess({hidden, object}, property) {
  return [
    {name: "has", target: object, property: property, result: true},
  ];
}

function GetValue({hidden, object}, property, value) {
  return [
    {name: "get", target: object, property: property, receiver: object, result: value},
  ];
}

function BindingNotIntercepted({hidden, object}, {hidden: blackListHidden, object: blackListObject}, property) {
  return [
    {name: "get", target: blackListObject, property: property, receiver: blackListObject, result: void 0},
  ];
}

function BindingIntercepted({hidden, object}, {hidden: blackListHidden, object: blackListObject}, property) {
  return [
    {name: "get", target: blackListObject, property: property, receiver: blackListObject, result: blackListHidden[property]},
  ];
}

function WithLookup(history, hidden, fn) {
  let object = Recorder.watch(hidden, history);
  try { fn(object); } finally { Recorder.unwatch(object); }
  return {hidden, object};
}

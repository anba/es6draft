/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals
} = Assert;

// 8.1.1.2.4 InitializeBinding: Unreachable method ?
// https://bugs.ecmascript.org/show_bug.cgi?id=3759

let recording = [];
let startRecording = false;
let realm = new Reflect.Realm(Object.create(null), new Proxy({}, {
  get(t, pk, r) {
    if (startRecording) {
      recording.push(pk);
    }
  }
}));
startRecording = true;
realm.eval("var x = 0");
startRecording = false;

const canDeclareGlobalVar = [
  "getOwnPropertyDescriptor",
  "isExtensible",
];
const createGlobalVarBinding = [
  "getOwnPropertyDescriptor",
  "isExtensible",
  "defineProperty",
  "set",
  "getOwnPropertyDescriptor",
  "defineProperty",
];
const putValue = [
  "has",
  "set",
  "getOwnPropertyDescriptor",
  "defineProperty",
];

assertEquals([...canDeclareGlobalVar, ...createGlobalVarBinding, ...putValue], recording);

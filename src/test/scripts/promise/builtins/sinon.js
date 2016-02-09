/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
"use strict";

/**
 * Compatibility layer for sinon test API
 */

const {ok: assert, strictEqual} = require("assert");

const callRecords = new WeakMap();
let callCount = 0;

function createFunction(f, onCall) {
  let p = new Proxy(f, {
    apply(target, thisArg, argList) {
      callRecords.get(p).push({id: callCount++, target, thisArg, argList});
      return onCall(target, thisArg, argList);
    }
  });
  callRecords.set(p, []);
  return p;
}

Object.assign(exports, {
  stub() {
    let action = () => {};
    let p = createFunction(() => {}, (_, ...rest) => Reflect.apply(action, ...rest));
    return Object.assign(p, {
      returns(v) {
        action = () => { return v };
        return this;
      },
      throws(v) {
        action = () => { throw v };
        return this;
      }
    });
  },
  spy(f = () => {}) {
    return createFunction(f, Reflect.apply);
  },
  match: {
    same(expected) {
      return actual => (expected === actual);
    }
  },
  assert: {
    callOrder(f, ...more) {
      more.reduce((current, next) => {
        let currentRecords = callRecords.get(current);
        assert(currentRecords.length);
        let nextRecords = callRecords.get(next);
        if (nextRecords.length) {
          let {id: firstCall} = currentRecords[0];
          let {id: lastCall} = nextRecords[nextRecords.length - 1];
          assert(firstCall < lastCall);
        }
        return next;
      }, f);
    },
    calledWith(f, ...matchers) {
      callRecords.get(f).some(({argList}) =>
        argList.length === matchers.length && argList.every((arg, i) => matchers[i](arg))
      ) || assert(false);
    },
    notCalled(f) {
      strictEqual(callRecords.get(f).length, 0);
    }
  }
});

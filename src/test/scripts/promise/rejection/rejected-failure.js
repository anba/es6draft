/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

{
  class P extends Promise {
    constructor(exec) {
      let pending;
      super((resolve, reject) => {
        exec(resolve, reject);
        pending = {resolve, reject};
      });
      Object.assign(this, pending);
    }
  }

  let pending;
  let p1 = new P((resolve, reject) => { pending = {resolve, reject} });
  let p2 = p1.then();
  pending.reject(new Error("initial error"));

  let error = new Error("override error");
  p2.reject(error);
  p2.catch(e => {
    if (e !== error) {
      throw e;
    }
  });
}

triggerGC();

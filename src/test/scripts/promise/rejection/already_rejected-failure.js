/*
 * Copyright (c) André Bargull
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

  let p1 = P.reject(new Error("initial error"));
  let p2 = p1.then();

  let error = new Error("override error");
  p2.reject(error);
  p2.catch(e => {
    if (e !== error) {
      throw e;
    }
  });
}

triggerGC();

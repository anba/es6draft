/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

{
  class P extends Promise {
    constructor(exec) {
      super((resolve, reject) => {
        exec(resolve, reject);
        Object.assign(this, {resolve, reject});
      });
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

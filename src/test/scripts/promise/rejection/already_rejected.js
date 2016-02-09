/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

{
  let p1 = Promise.reject(new Error("rejected promise 1"));
  let p2 = p1.catch(() => {});
}

{
  let p1 = Promise.reject(new Error("rejected promise 2"));
  let p2 = p1.then(() => {});
  let p3 = p2.catch(() => {});
}

{
  let p1 = Promise.reject(new Error("rejected promise 3"));
  let p2 = p1.then(() => {});
  let p3 = p1.catch(() => {});
}

triggerGC();

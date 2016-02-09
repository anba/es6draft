/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

{
  let pending;
  let p1 = new Promise((resolve, reject) => { pending = {resolve, reject} });
  let p2 = p1.then(() => {});
  let p3 = p2.then(() => {});
  pending.reject(new Error);
}

triggerGC();

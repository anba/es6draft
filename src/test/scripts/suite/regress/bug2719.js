/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

System.load("lib/promises.jsm");
const {
  reportFailure
} = System.get("lib/promises.jsm");

// 7.5.3 PromiseOf: Apply same optimization as in Promise.resolve?
// https://bugs.ecmascript.org/show_bug.cgi?id=2719

{
  let log = "";

  Promise.resolve()
    .then(() => log += "|turn1")
    .then(() => log += "|turn2")
    .then(() => log += "|turn3")
    .then(() => log += "|turn4")
    // .then(() => assertSame("|start|turn1|fast-resolve|turn2|turn3|slow-resolve|translate:123|turn4", log))
    .then(() => assertSame("|start|turn1|fast-resolve|turn2|turn3|slow-resolve|turn4", log))
    .catch(reportFailure);

  Promise.resolve(Promise.resolve()).then(() => log += "|fast-resolve");
  (class extends Promise {}).resolve(Promise.resolve()).then(() => log += "|slow-resolve");

  // const StopTranslate = {};
  //
  // let loader = new Reflect.Loader({
  //   translate({source}) {
  //     log += "|translate:" + source;
  //     throw StopTranslate;
  //   }
  // });
  //
  // let source = Promise.resolve("123");
  // loader.module(source)
  //   .then(v => fail `module() fulfilled with ${v}`, e => assertSame(StopTranslate, e))
  //   .catch(reportFailure);

  log += "|start";
}

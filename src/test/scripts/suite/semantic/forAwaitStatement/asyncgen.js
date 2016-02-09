/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals, assertInstanceOf, fail
} = Assert;

System.load("lib/promises.jsm");
const {
  reportFailure
} = System.get("lib/promises.jsm");

{
  async function* producer() {
    yield 1;
    yield 2;
  }
  async function consumer() {
    var values = [];
    for await (var v of producer()) values.push(v);
    return values;
  }
  consumer().then(v => assertEquals([1, 2], v)).catch(reportFailure);
}

{
  async function* producer() {
    yield await Promise.resolve(3);
    yield await Promise.resolve(4);
  }
  async function consumer() {
    var values = [];
    for await (var v of producer()) values.push(v);
    return values;
  }
  consumer().then(v => assertEquals([3, 4], v)).catch(reportFailure);
}

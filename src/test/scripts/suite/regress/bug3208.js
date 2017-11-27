/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals
} = Assert;

// String.prototype.substr/slice language mistreats -0 as start
// https://bugs.ecmascript.org/show_bug.cgi?id=3208

assertSame("abc", "abc".slice(-0));
assertSame("a", "abc".slice(-0, 1));
assertSame("ab", "abc".slice(-0, -1));
assertSame("", "abc".slice(-0, -0));
assertSame("", "abc".slice(1, -0));
assertSame("", "abc".slice(-1, -0));

assertSame("abc", "abc".substr(-0));
assertSame("a", "abc".substr(-0, 1));
assertSame("", "abc".substr(-0, -1));

assertEquals([..."abc"], [..."abc"].copyWithin(-0, -0));
assertEquals([..."abc"], [..."abc"].copyWithin(-0, -0, -0));
assertEquals([..."aab"], [..."abc"].copyWithin(1, -0));
assertEquals([..."bcc"], [..."abc"].copyWithin(-0, 1));

assertEquals([..."aaa"], Array(3).fill("a", -0));
assertEquals(Array(3), Array(3).fill("a", -0, -0));
assertEquals(Array(3), Array(3).fill("a", 1, -0));

assertEquals([..."abc"], [..."abc"].slice(-0));
assertEquals([..."a"], [..."abc"].slice(-0, 1));
assertEquals([..."ab"], [..."abc"].slice(-0, -1));
assertEquals([...""], [..."abc"].slice(-0, -0));
assertEquals([...""], [..."abc"].slice(1, -0));
assertEquals([...""], [..."abc"].slice(-1, -0));

assertEquals([..."abc"], [..."abc"].splice(-0));
assertEquals([..."a"], [..."abc"].splice(-0, 1));
assertEquals([...""], [..."abc"].splice(-0, -0));
assertEquals([...""], [..."abc"].splice(-0, -1));

class ASCIIString extends ArrayBuffer {
  toString() {
    return new Uint8Array(this).reduce((s, c) => s + String.fromCharCode(c), "");
  }

  toArray() {
    return new class extends Uint8Array {
      toString() {
        return ASCIIString.prototype.toString.call(this.buffer);
      }
    }(this);
  }

  static from(string) {
    let s = String(string);
    let buffer = new this(s.length);
    new Uint8Array(buffer).set(Array.prototype.map.call(s, c => c.charCodeAt(0)));
    return buffer;
  }
}

assertSame("abc", ASCIIString.from("abc").slice(-0).toString());
assertSame("a", ASCIIString.from("abc").slice(-0, 1).toString());
assertSame("ab", ASCIIString.from("abc").slice(-0, -1).toString());
assertSame("", ASCIIString.from("abc").slice(-0, -0).toString());
assertSame("", ASCIIString.from("abc").slice(1, -0).toString());
assertSame("", ASCIIString.from("abc").slice(-1, -0).toString());

assertSame("abc", ASCIIString.from("abc").toArray().slice(-0).toString());
assertSame("a", ASCIIString.from("abc").toArray().slice(-0, 1).toString());
assertSame("ab", ASCIIString.from("abc").toArray().slice(-0, -1).toString());
assertSame("", ASCIIString.from("abc").toArray().slice(-0, -0).toString());
assertSame("", ASCIIString.from("abc").toArray().slice(1, -0).toString());
assertSame("", ASCIIString.from("abc").toArray().slice(-1, -0).toString());

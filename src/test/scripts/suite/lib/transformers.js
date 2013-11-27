/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const Transformers = (() => {
  function toStr(number, radix, pad) {
    let s = number.toString(radix);
    return "0".repeat(pad - s.length) + s;
  }

  function rangeLength(range) {
    return ~~((range.end - range.start) / range.increment) + 1;
  }

  function identity(range) {
    return String(range);
  }

  function toCharClass(pattern, range) {
    return `[${pattern}]{${rangeLength(range)}}`;
  }

  function toNegatedCharClass(pattern, range) {
    return `[^${pattern}]{${rangeLength(range)}}`;
  }

  function toCharClassRange(t, range) {
    return `[${t(range.startR())}-${t(range.endR())}]{${rangeLength(range)}}`;
  }

  function toNegatedCharClassRange(t, range) {
    return `[^${t(range.startR())}-${t(range.endR())}]{${rangeLength(range)}}`;
  }

  function toIdentityEscape(range) {
    let result = "";
    for (let cp of String(range)) {
      if ((cp >= '0' && cp <= '9') || (cp >= 'A' && cp <= 'Z') || (cp >= 'a' && cp <= 'z')) {
        result += cp;
      } else {
        result += `\\${cp}`;
      }
    }
    return result;
  }

  function toOctalEscape(range) {
    let result = "";
    for (let cp of range) {
      result += `\\0${toStr(cp, 8, 3)}`;
    }
    return result;
  }

  function toHexEscape(range) {
    let result = "";
    for (let cp of range) {
      result += `\\x${toStr(cp, 16, 2)}`;
    }
    return result;
  }

  function toUnicodeEscape(range) {
    let result = "";
    for (let cp of range) {
      result += `\\u${toStr(cp, 16, 4)}`;
    }
    return result;
  }

  function toExtendedUnicodeEscape(range) {
    let result = "";
    for (let cp of range) {
      result += `\\u{${cp.toString(16)}}`;
    }
    return result;
  }

  const combine = (f, g) => (...args) => f(g(...args), ...args);
  const combine2 = (f, g) => (...args) => f(g, ...args);

  return {
    identity, toIdentityEscape, toOctalEscape, toHexEscape, toUnicodeEscape, toExtendedUnicodeEscape,
    charClass: {
      identity: combine(toCharClass, identity),
      toIdentityEscape: combine(toCharClass, toIdentityEscape),
      toOctalEscape: combine(toCharClass, toOctalEscape),
      toHexEscape: combine(toCharClass, toHexEscape),
      toUnicodeEscape: combine(toCharClass, toUnicodeEscape),
      toExtendedUnicodeEscape: combine(toCharClass, toExtendedUnicodeEscape),
      negated: {
        identity: combine(toNegatedCharClass, identity),
        toIdentityEscape: combine(toNegatedCharClass, toIdentityEscape),
        toOctalEscape: combine(toNegatedCharClass, toOctalEscape),
        toHexEscape: combine(toNegatedCharClass, toHexEscape),
        toUnicodeEscape: combine(toNegatedCharClass, toUnicodeEscape),
        toExtendedUnicodeEscape: combine(toNegatedCharClass, toExtendedUnicodeEscape),
      },
    },
    charClassRange: {
      identity: combine2(toCharClassRange, identity),
      toIdentityEscape: combine2(toCharClassRange, toIdentityEscape),
      toOctalEscape: combine2(toCharClassRange, toOctalEscape),
      toHexEscape: combine2(toCharClassRange, toHexEscape),
      toUnicodeEscape: combine2(toCharClassRange, toUnicodeEscape),
      toExtendedUnicodeEscape: combine2(toCharClassRange, toExtendedUnicodeEscape),
      negated: {
        identity: combine2(toNegatedCharClassRange, identity),
        toIdentityEscape: combine2(toNegatedCharClassRange, toIdentityEscape),
        toOctalEscape: combine2(toNegatedCharClassRange, toOctalEscape),
        toHexEscape: combine2(toNegatedCharClassRange, toHexEscape),
        toUnicodeEscape: combine2(toNegatedCharClassRange, toUnicodeEscape),
        toExtendedUnicodeEscape: combine2(toNegatedCharClassRange, toExtendedUnicodeEscape),
      },
    },
  };
})();

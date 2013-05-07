/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function ArrayStringStatics(global) {
"use strict";

const Object = global.Object,
      Array = global.Array,
      String = global.String;

Object.defineProperties(Object.assign(Array, {
  join(array, $1, ...more) {
    return Array.prototype.join.call(array, $1, ...more);
  },
  reverse(array, ...more) {
    return Array.prototype.reverse.call(array, ...more);
  },
  sort(array, $1, ...more) {
    return Array.prototype.sort.call(array, $1, ...more);
  },
  push(array, $1, ...more) {
    if (arguments.length <= 1) {
      return Array.prototype.push.call(array);
    }
    return Array.prototype.push.call(array, ...[$1, ...more]);
  },
  pop(array, ...more) {
    return Array.prototype.pop.call(array, ...more);
  },
  shift(array, ...more) {
    return Array.prototype.shift.call(array, ...more);
  },
  unshift(array, $1, ...more) {
    if (arguments.length <= 1) {
      return Array.prototype.unshift.call(array);
    }
    return Array.prototype.unshift.call(array, ...[$1, ...more]);
  },
  splice(array, $1, $2, ...more) {
    return Array.prototype.splice.call(array, $1, $2, ...more);
  },
  concat(array, $1, ...more) {
    if (arguments.length <= 1) {
      return Array.prototype.concat.call(array);
    }
    return Array.prototype.concat.call(array, ...[$1, ...more]);
  },
  slice(array, $1, $2, ...more) {
    return Array.prototype.slice.call(array, $1, $2, ...more);
  },
  filter(array, $1, ...more) {
    return Array.prototype.filter.call(array, $1, ...more);
  },
  lastIndexOf(array, $1, ...more) {
    return Array.prototype.lastIndexOf.call(array, $1, ...more);
  },
  indexOf(array, $1, ...more) {
    return Array.prototype.indexOf.call(array, $1, ...more);
  },
  forEach(array, $1, ...more) {
    return Array.prototype.forEach.call(array, $1, ...more);
  },
  map(array, $1, ...more) {
    return Array.prototype.map.call(array, $1, ...more);
  },
  every(array, $1, ...more) {
    return Array.prototype.every.call(array, $1, ...more);
  },
  some(array, $1, ...more) {
    return Array.prototype.some.call(array, $1, ...more);
  },
  reduce(array, $1, ...more) {
    return Array.prototype.reduce.call(array, $1, ...more);
  },
  reduceRight(array, $1, ...more) {
    return Array.prototype.reduceRight.call(array, $1, ...more);
  },
}), {
  join: {enumerable: false},
  reverse: {enumerable: false},
  sort: {enumerable: false},
  push: {enumerable: false},
  pop: {enumerable: false},
  shift: {enumerable: false},
  unshift: {enumerable: false},
  splice: {enumerable: false},
  concat: {enumerable: false},
  slice: {enumerable: false},
  filter: {enumerable: false},
  lastIndexOf: {enumerable: false},
  indexOf: {enumerable: false},
  forEach: {enumerable: false},
  map: {enumerable: false},
  every: {enumerable: false},
  some: {enumerable: false},
  reduce: {enumerable: false},
  reduceRight: {enumerable: false},
});

Object.defineProperties(Object.assign(String, {
  quote(string, ...more) {
    return String.prototype.quote.call(string, ...more);
  },
  substring(string, $1, $2, ...more) {
    return String.prototype.substring.call(string, $1, $2, ...more);
  },
  toLowerCase(string, ...more) {
    return String.prototype.toLowerCase.call(string, ...more);
  },
  toUpperCase(string, ...more) {
    return String.prototype.toUpperCase.call(string, ...more);
  },
  charAt(string, $1, ...more) {
    return String.prototype.charAt.call(string, $1, ...more);
  },
  charCodeAt(string, $1, ...more) {
    return String.prototype.charCodeAt.call(string, $1, ...more);
  },
  contains(string, $1, ...more) {
    return String.prototype.contains.call(string, $1, ...more);
  },
  indexOf(string, $1, ...more) {
    return String.prototype.indexOf.call(string, $1, ...more);
  },
  lastIndexOf(string, $1, ...more) {
    return String.prototype.lastIndexOf.call(string, $1, ...more);
  },
  startsWith(string, $1, ...more) {
    return String.prototype.startsWith.call(string, $1, ...more);
  },
  endsWith(string, $1, ...more) {
    return String.prototype.endsWith.call(string, $1, ...more);
  },
  trim(string, ...more) {
    return String.prototype.trim.call(string, ...more);
  },
  trimLeft(string, ...more) {
    return String.prototype.trimLeft.call(string, ...more);
  },
  trimRight(string, ...more) {
    return String.prototype.trimRight.call(string, ...more);
  },
  toLocaleLowerCase(string, ...more) {
    return String.prototype.toLocaleLowerCase.call(string, ...more);
  },
  toLocaleUpperCase(string, ...more) {
    return String.prototype.toLocaleUpperCase.call(string, ...more);
  },
  localeCompare(string, $1, ...more) {
    return String.prototype.localeCompare.call(string, $1, ...more);
  },
  match(string, $1, ...more) {
    return String.prototype.match.call(string, $1, ...more);
  },
  search(string, $1, ...more) {
    return String.prototype.search.call(string, $1, ...more);
  },
  replace(string, $1, $2, ...more) {
    return String.prototype.replace.call(string, $1, $2, ...more);
  },
  split(string, $1, $2, ...more) {
    return String.prototype.split.call(string, $1, $2, ...more);
  },
  substr(string, $1, $2, ...more) {
    return String.prototype.substr.call(string, $1, $2, ...more);
  },
  concat(string, $1, ...more) {
    if (arguments.length <= 1) {
      return String.prototype.concat.call(string);
    }
    return String.prototype.concat.call(string, ...[$1, ...more]);
  },
  slice(string, $1, $2, ...more) {
    return String.prototype.slice.call(string, $1, $2, ...more);
  },
}), {
  quote: {enumerable: false},
  substring: {enumerable: false},
  toLowerCase: {enumerable: false},
  toUpperCase: {enumerable: false},
  charAt: {enumerable: false},
  charCodeAt: {enumerable: false},
  contains: {enumerable: false},
  indexOf: {enumerable: false},
  lastIndexOf: {enumerable: false},
  startsWith: {enumerable: false},
  endsWith: {enumerable: false},
  trim: {enumerable: false},
  trimLeft: {enumerable: false},
  trimRight: {enumerable: false},
  toLocaleLowerCase: {enumerable: false},
  toLocaleUpperCase: {enumerable: false},
  localeCompare: {enumerable: false},
  match: {enumerable: false},
  search: {enumerable: false},
  replace: {enumerable: false},
  split: {enumerable: false},
  substr: {enumerable: false},
  concat: {enumerable: false},
  slice: {enumerable: false},
});

})(this);

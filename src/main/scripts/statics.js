/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function ArrayStringStatics() {
"use strict";

const global = %GlobalObject();

const {
  Object, Array, String
} = global;

{ /* Array statics */

const {
  join: Array_prototype_join,
  reverse: Array_prototype_reverse,
  sort: Array_prototype_sort,
  push: Array_prototype_push,
  pop: Array_prototype_pop,
  shift: Array_prototype_shift,
  unshift: Array_prototype_unshift,
  splice: Array_prototype_splice,
  concat: Array_prototype_concat,
  slice: Array_prototype_slice,
  filter: Array_prototype_filter,
  lastIndexOf: Array_prototype_lastIndexOf,
  indexOf: Array_prototype_indexOf,
  forEach: Array_prototype_forEach,
  map: Array_prototype_map,
  every: Array_prototype_every,
  some: Array_prototype_some,
  reduce: Array_prototype_reduce,
  reduceRight: Array_prototype_reduceRight,
} = Array.prototype;

Object.defineProperties(Object.assign(Array, {
  join(array, $1, ...more) {
    return %CallFunction(Array_prototype_join, array, $1, ...more);
  },
  reverse(array, ...more) {
    return %CallFunction(Array_prototype_reverse, array, ...more);
  },
  sort(array, $1, ...more) {
    return %CallFunction(Array_prototype_sort, array, $1, ...more);
  },
  push(array, $1, ...more) {
    if (arguments.length <= 1) {
      return %CallFunction(Array_prototype_push, array);
    }
    return %CallFunction(Array_prototype_push, array, ...[$1, ...more]);
  },
  pop(array, ...more) {
    return %CallFunction(Array_prototype_pop, array, ...more);
  },
  shift(array, ...more) {
    return %CallFunction(Array_prototype_shift, array, ...more);
  },
  unshift(array, $1, ...more) {
    if (arguments.length <= 1) {
      return %CallFunction(Array_prototype_unshift, array);
    }
    return %CallFunction(Array_prototype_unshift, array, ...[$1, ...more]);
  },
  splice(array, $1, $2, ...more) {
    return %CallFunction(Array_prototype_splice, array, $1, $2, ...more);
  },
  concat(array, $1, ...more) {
    if (arguments.length <= 1) {
      return %CallFunction(Array_prototype_concat, array);
    }
    return %CallFunction(Array_prototype_concat, array, ...[$1, ...more]);
  },
  slice(array, $1, $2, ...more) {
    return %CallFunction(Array_prototype_slice, array, $1, $2, ...more);
  },
  filter(array, $1, ...more) {
    return %CallFunction(Array_prototype_filter, array, $1, ...more);
  },
  lastIndexOf(array, $1, ...more) {
    return %CallFunction(Array_prototype_lastIndexOf, array, $1, ...more);
  },
  indexOf(array, $1, ...more) {
    return %CallFunction(Array_prototype_indexOf, array, $1, ...more);
  },
  forEach(array, $1, ...more) {
    return %CallFunction(Array_prototype_forEach, array, $1, ...more);
  },
  map(array, $1, ...more) {
    return %CallFunction(Array_prototype_map, array, $1, ...more);
  },
  every(array, $1, ...more) {
    return %CallFunction(Array_prototype_every, array, $1, ...more);
  },
  some(array, $1, ...more) {
    return %CallFunction(Array_prototype_some, array, $1, ...more);
  },
  reduce(array, $1, ...more) {
    return %CallFunction(Array_prototype_reduce, array, $1, ...more);
  },
  reduceRight(array, $1, ...more) {
    return %CallFunction(Array_prototype_reduceRight, array, $1, ...more);
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
}

{ /* String statics */

const {
  quote: String_prototype_quote,
  substring: String_prototype_substring,
  toLowerCase: String_prototype_toLowerCase,
  toUpperCase: String_prototype_toUpperCase,
  charAt: String_prototype_charAt,
  charCodeAt: String_prototype_charCodeAt,
  contains: String_prototype_contains,
  indexOf: String_prototype_indexOf,
  lastIndexOf: String_prototype_lastIndexOf,
  startsWith: String_prototype_startsWith,
  endsWith: String_prototype_endsWith,
  trim: String_prototype_trim,
  trimLeft: String_prototype_trimLeft,
  trimRight: String_prototype_trimRight,
  toLocaleLowerCase: String_prototype_toLocaleLowerCase,
  toLocaleUpperCase: String_prototype_toLocaleUpperCase,
  localeCompare: String_prototype_localeCompare,
  match: String_prototype_match,
  search: String_prototype_search,
  replace: String_prototype_replace,
  split: String_prototype_split,
  substr: String_prototype_substr,
  concat: String_prototype_concat,
  slice: String_prototype_slice,
} = String.prototype;

Object.defineProperties(Object.assign(String, {
  quote(string, ...more) {
    return %CallFunction(String_prototype_quote, string, ...more);
  },
  substring(string, $1, $2, ...more) {
    return %CallFunction(String_prototype_substring, string, $1, $2, ...more);
  },
  toLowerCase(string, ...more) {
    return %CallFunction(String_prototype_toLowerCase, string, ...more);
  },
  toUpperCase(string, ...more) {
    return %CallFunction(String_prototype_toUpperCase, string, ...more);
  },
  charAt(string, $1, ...more) {
    return %CallFunction(String_prototype_charAt, string, $1, ...more);
  },
  charCodeAt(string, $1, ...more) {
    return %CallFunction(String_prototype_charCodeAt, string, $1, ...more);
  },
  contains(string, $1, ...more) {
    return %CallFunction(String_prototype_contains, string, $1, ...more);
  },
  indexOf(string, $1, ...more) {
    return %CallFunction(String_prototype_indexOf, string, $1, ...more);
  },
  lastIndexOf(string, $1, ...more) {
    return %CallFunction(String_prototype_lastIndexOf, string, $1, ...more);
  },
  startsWith(string, $1, ...more) {
    return %CallFunction(String_prototype_startsWith, string, $1, ...more);
  },
  endsWith(string, $1, ...more) {
    return %CallFunction(String_prototype_endsWith, string, $1, ...more);
  },
  trim(string, ...more) {
    return %CallFunction(String_prototype_trim, string, ...more);
  },
  trimLeft(string, ...more) {
    return %CallFunction(String_prototype_trimLeft, string, ...more);
  },
  trimRight(string, ...more) {
    return %CallFunction(String_prototype_trimRight, string, ...more);
  },
  toLocaleLowerCase(string, ...more) {
    return %CallFunction(String_prototype_toLocaleLowerCase, string, ...more);
  },
  toLocaleUpperCase(string, ...more) {
    return %CallFunction(String_prototype_toLocaleUpperCase, string, ...more);
  },
  localeCompare(string, $1, ...more) {
    return %CallFunction(String_prototype_localeCompare, string, $1, ...more);
  },
  match(string, $1, ...more) {
    return %CallFunction(String_prototype_match, string, $1, ...more);
  },
  search(string, $1, ...more) {
    return %CallFunction(String_prototype_search, string, $1, ...more);
  },
  replace(string, $1, $2, ...more) {
    return %CallFunction(String_prototype_replace, string, $1, $2, ...more);
  },
  split(string, $1, $2, ...more) {
    return %CallFunction(String_prototype_split, string, $1, $2, ...more);
  },
  substr(string, $1, $2, ...more) {
    return %CallFunction(String_prototype_substr, string, $1, $2, ...more);
  },
  concat(string, $1, ...more) {
    if (arguments.length <= 1) {
      return %CallFunction(String_prototype_concat, string);
    }
    return %CallFunction(String_prototype_concat, string, ...[$1, ...more]);
  },
  slice(string, $1, $2, ...more) {
    return %CallFunction(String_prototype_slice, string, $1, $2, ...more);
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
}

})();

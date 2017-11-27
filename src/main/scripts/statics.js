/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function ArrayStringStatics() {
"use strict";

const Array = %Intrinsic("Array");
const String = %Intrinsic("String");

/* Array statics */
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

%CreateMethodProperties(Array, {
  join(array, separator) {
    return %CallFunction(Array_prototype_join, array, separator);
  },
  reverse(array) {
    return %CallFunction(Array_prototype_reverse, array);
  },
  sort(array, comparefn) {
    return %CallFunction(Array_prototype_sort, array, comparefn);
  },
  push(array, item, ...items) {
    if (arguments.length <= 1) {
      return %CallFunction(Array_prototype_push, array);
    }
    return %CallFunction(Array_prototype_push, array, item, ...items);
  },
  pop(array) {
    return %CallFunction(Array_prototype_pop, array);
  },
  shift(array) {
    return %CallFunction(Array_prototype_shift, array);
  },
  unshift(array, item, ...items) {
    if (arguments.length <= 1) {
      return %CallFunction(Array_prototype_unshift, array);
    }
    return %CallFunction(Array_prototype_unshift, array, item, ...items);
  },
  splice(array, start, deleteCount, ...items) {
    if (arguments.length <= 1) {
      return %CallFunction(Array_prototype_splice, array);
    }
    if (arguments.length <= 2) {
      return %CallFunction(Array_prototype_splice, array, start);
    }
    if (arguments.length <= 3) {
      return %CallFunction(Array_prototype_splice, array, start, deleteCount);
    }
    return %CallFunction(Array_prototype_splice, array, start, deleteCount, ...items);
  },
  concat(array, arg, ...args) {
    if (arguments.length <= 1) {
      return %CallFunction(Array_prototype_concat, array);
    }
    return %CallFunction(Array_prototype_concat, array, arg, ...args);
  },
  slice(array, start, end) {
    return %CallFunction(Array_prototype_slice, array, start, end);
  },
  lastIndexOf(array, searchElement, fromIndex = void 0) {
    if (arguments.length <= 2) {
      return %CallFunction(Array_prototype_lastIndexOf, array, searchElement);
    }
    return %CallFunction(Array_prototype_lastIndexOf, array, searchElement, fromIndex);
  },
  indexOf(array, searchElement, fromIndex = void 0) {
    return %CallFunction(Array_prototype_indexOf, array, searchElement, fromIndex);
  },
  forEach(array, callbackfn, thisArg = void 0) {
    return %CallFunction(Array_prototype_forEach, array, callbackfn, thisArg);
  },
  map(array, callbackfn, thisArg = void 0) {
    return %CallFunction(Array_prototype_map, array, callbackfn, thisArg);
  },
  filter(array, callbackfn, thisArg = void 0) {
    return %CallFunction(Array_prototype_filter, array, callbackfn, thisArg);
  },
  every(array, callbackfn, thisArg = void 0) {
    return %CallFunction(Array_prototype_every, array, callbackfn, thisArg);
  },
  some(array, callbackfn, thisArg = void 0) {
    return %CallFunction(Array_prototype_some, array, callbackfn, thisArg);
  },
  reduce(array, callbackfn, initialValue = void 0) {
    if (arguments.length <= 2) {
      return %CallFunction(Array_prototype_reduce, array, callbackfn);
    }
    return %CallFunction(Array_prototype_reduce, array, callbackfn, initialValue);
  },
  reduceRight(array, callbackfn, initialValue = void 0) {
    if (arguments.length <= 2) {
      return %CallFunction(Array_prototype_reduceRight, array, callbackfn);
    }
    return %CallFunction(Array_prototype_reduceRight, array, callbackfn, initialValue);
  },
});

/* String statics */
const {
  substring: String_prototype_substring,
  toLowerCase: String_prototype_toLowerCase,
  toUpperCase: String_prototype_toUpperCase,
  charAt: String_prototype_charAt,
  charCodeAt: String_prototype_charCodeAt,
  includes: String_prototype_includes,
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
  normalize: String_prototype_normalize,
  localeCompare: String_prototype_localeCompare,
  match: String_prototype_match,
  search: String_prototype_search,
  replace: String_prototype_replace,
  split: String_prototype_split,
  substr: String_prototype_substr,
  concat: String_prototype_concat,
  slice: String_prototype_slice,
} = String.prototype;

%CreateMethodProperties(String, {
  substring(string, start, end) {
    return %CallFunction(String_prototype_substring, string, start, end);
  },
  toLowerCase(string) {
    return %CallFunction(String_prototype_toLowerCase, string);
  },
  toUpperCase(string) {
    return %CallFunction(String_prototype_toUpperCase, string);
  },
  charAt(string, pos) {
    return %CallFunction(String_prototype_charAt, string, pos);
  },
  charCodeAt(string, pos) {
    return %CallFunction(String_prototype_charCodeAt, string, pos);
  },
  includes(string, searchString, position = void 0) {
    return %CallFunction(String_prototype_includes, string, searchString, position);
  },
  contains(string, searchString, position = void 0) {
    return %CallFunction(String_prototype_contains, string, searchString, position);
  },
  indexOf(string, searchString, position = void 0) {
    return %CallFunction(String_prototype_indexOf, string, searchString, position);
  },
  lastIndexOf(string, searchString, position = void 0) {
    return %CallFunction(String_prototype_lastIndexOf, string, searchString, position);
  },
  startsWith(string, searchString, position = void 0) {
    return %CallFunction(String_prototype_startsWith, string, searchString, position);
  },
  endsWith(string, searchString, position = void 0) {
    return %CallFunction(String_prototype_endsWith, string, searchString, position);
  },
  trim(string) {
    return %CallFunction(String_prototype_trim, string);
  },
  trimLeft(string) {
    return %CallFunction(String_prototype_trimLeft, string);
  },
  trimRight(string) {
    return %CallFunction(String_prototype_trimRight, string);
  },
  toLocaleLowerCase(string, locales = void 0) {
    return %CallFunction(String_prototype_toLocaleLowerCase, string, locales);
  },
  toLocaleUpperCase(string, locales = void 0) {
    return %CallFunction(String_prototype_toLocaleUpperCase, string, locales);
  },
  normalize(string, form = void 0) {
    return %CallFunction(String_prototype_normalize, string, form);
  },
  localeCompare(string, that, locales = void 0, options = void 0) {
    return %CallFunction(String_prototype_localeCompare, string, that, locales, options);
  },
  match(string, regexp) {
    return %CallFunction(String_prototype_match, string, regexp);
  },
  search(string, regexp) {
    return %CallFunction(String_prototype_search, string, regexp);
  },
  replace(string, searchValue, replaceValue) {
    return %CallFunction(String_prototype_replace, string, searchValue, replaceValue);
  },
  split(string, separator, limit) {
    return %CallFunction(String_prototype_split, string, separator, limit);
  },
  substr(string, start, length) {
    return %CallFunction(String_prototype_substr, string, start, length);
  },
  concat(string, arg, ...args) {
    if (arguments.length <= 1) {
      return %CallFunction(String_prototype_concat, string);
    }
    return %CallFunction(String_prototype_concat, string, arg, ...args);
  },
  slice(string, start, end) {
    return %CallFunction(String_prototype_slice, string, start, end);
  },
});

})();

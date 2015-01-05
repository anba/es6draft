/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function StringExtensions() {
"use strict";

const global = %GlobalObject();

const {
  Object, String, RegExp
} = global;

const {
  match: String_prototype_match,
  replace: String_prototype_replace,
  search: String_prototype_search,
} = String.prototype;

const specialCharsRE = /[|^$\\()[\]{}.?*+]/g;

function ToFlatPattern(p) {
  return %RegExpReplace(specialCharsRE, p, "\\$&");
}

/*
 * Add support to specify regular expression flags
 */
Object.defineProperties(Object.assign(String.prototype, {
  match(regexp, flags = void 0) {
    if (typeof regexp == 'string' && flags !== void 0) {
      regexp = RegExp(regexp, flags);
    }
    return %CallFunction(String_prototype_match, this, regexp);
  },
  search(regexp, flags = void 0) {
    if (typeof regexp == 'string' && flags !== void 0) {
      regexp = RegExp(regexp, flags);
    }
    return %CallFunction(String_prototype_search, this, regexp);
  },
  replace(searchValue, replaceValue, flags = void 0) {
    if (typeof searchValue == 'string' && flags !== void 0) {
      searchValue = RegExp(ToFlatPattern(searchValue), flags);
    }
    return %CallFunction(String_prototype_replace, this, searchValue, replaceValue);
  },
}), {
  match: {enumerable: false},
  search: {enumerable: false},
  replace: {enumerable: false},
});

})();

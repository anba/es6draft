/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function StringExtensions(global) {
"use strict";

const Object = global.Object,
      String = global.String,
      RegExp = global.RegExp;

const String_prototype_match = String.prototype.match,
      String_prototype_search = String.prototype.search,
      String_prototype_replace = String.prototype.replace;

Object.defineProperties(Object.assign(String.prototype, {
  match(regexp, flags) {
    if (typeof regexp == 'string' && flags !== void 0) {
      regexp = new RegExp(regexp, flags);
    }
    return String_prototype_match.call(this, regexp);
  },
  search(regexp, flags) {
    if (typeof regexp == 'string' && flags !== void 0) {
      regexp = new RegExp(regexp, flags);
    }
    return String_prototype_search.call(this, regexp);
  },
  replace(searchValue, replaceValue, flags) {
    if (typeof searchValue == 'string' && flags !== void 0) {
      searchValue = new RegExp(searchValue, flags);
    }
    return String_prototype_replace.call(this, searchValue, replaceValue);
  },
}), {
  match: {enumerable: false},
  search: {enumerable: false},
  replace: {enumerable: false},
});


})(this);

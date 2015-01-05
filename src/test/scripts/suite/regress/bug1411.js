/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// B.2.2: CreateHTML() does not append the string to the output
// https://bugs.ecmascript.org/show_bug.cgi?id=1411

let methods = {
  anchor: {tag: "a", attribute: "name"},
  big: {tag: "big"},
  blink: {tag: "blink"},
  bold: {tag: "b"},
  fixed: {tag: "tt"},
  fontcolor: {tag: "font", attribute: "color"},
  fontsize: {tag: "font", attribute: "size"},
  italics: {tag: "i"},
  link: {tag: "a", attribute: "href"},
  small: {tag: "small"},
  strike: {tag: "strike"},
  sub: {tag: "sub"},
  sup: {tag: "sup"},
};

for (let name of Object.keys(methods)) {
  let s = "abc", v = "def";
  let r = s[name](v);
  let {tag, attribute = ""} = methods[name];
  if (attribute === "") {
    assertSame(`<${tag}>${s}</${tag}>`, r);
  } else {
    assertSame(`<${tag} ${attribute}="${v}">${s}</${tag}>`, r);
  }
}

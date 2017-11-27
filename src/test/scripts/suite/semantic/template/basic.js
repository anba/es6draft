/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotSame, assertTrue, assertFalse,
  assertSyntaxError, assertDataProperty, assertBuiltinFunction,
  assertThrows
} = Assert;

// unterminated template literal
assertSyntaxError("`");
assertSyntaxError("`${");
assertSyntaxError("`${}");
assertSyntaxError("`${1}");
assertSyntaxError("`${1 + 2}");


// invalid template substitution
assertSyntaxError("`${`");
assertSyntaxError("`${}`");


// character escape sequence (single escape character)
assertSame("\'", `\'`);
assertSame("\"", `\"`);
assertSame("\\", `\\`);
assertSame("\b", `\b`);
assertSame("\f", `\f`);
assertSame("\n", `\n`);
assertSame("\r", `\r`);
assertSame("\t", `\t`);
assertSame("\v", `\v`);
assertSame("\r\n", `\r\n`);


// character escape sequence (non escape character)
assertSame("\0", eval("`\\" + String.fromCharCode(0) + "`"));
assertSame("$", `\$`);
assertSame(".", `\.`);
assertSame("A", `\A`);
assertSame("a", `\a`);


// digit escape sequence
assertSame("\0", `\0`);
assertSyntaxError("`\\1`");
assertSyntaxError("`\\2`");
assertSyntaxError("`\\3`");
assertSyntaxError("`\\4`");
assertSyntaxError("`\\5`");
assertSyntaxError("`\\6`");
assertSyntaxError("`\\7`");
assertSyntaxError("`\\8`");
assertSyntaxError("`\\9`");


// hex escape sequence
assertSyntaxError("`\\x`");
assertSyntaxError("`\\x0`");
assertSyntaxError("`\\x0Z`");
assertSyntaxError("`\\xZ`");

assertSame("\0", `\x00`);
assertSame("$", `\x24`);
assertSame(".", `\x2E`);
assertSame("A", `\x41`);
assertSame("a", `\x61`);
assertSame("AB", `\x41B`);
assertSame(String.fromCharCode(0xFF), `\xFF`);


// unicode escape sequence
assertSame("\0", `\u0000`);
assertSame("$", `\u0024`);
assertSame(".", `\u002E`);
assertSame("A", `\u0041`);
assertSame("a", `\u0061`);
assertSame("AB", `\u0041B`);
assertSame(String.fromCharCode(0xFFFF), `\uFFFF`);

assertSame("\0", `\u{0000}`);
assertSame("$", `\u{0024}`);
assertSame(".", `\u{002E}`);
assertSame("A", `\u{0041}`);
assertSame("a", `\u{0061}`);
assertSame("AB", `\u{0041}B`);
assertSame(String.fromCharCode(0xFFFF), `\u{FFFF}`);

assertSame("\0", `\u{0}`);
assertSame("$", `\u{24}`);
assertSame(".", `\u{2E}`);
assertSame("A", `\u{41}`);
assertSame("a", `\u{61}`);
assertSame(String.fromCharCode(0x41B), `\u{41B}`);

assertSame(String.fromCodePoint(0x10000), `\u{10000}`);
assertSame(String.fromCodePoint(0x10FFFF), `\u{10FFFF}`);
assertSyntaxError("`\\u{110000}`");


// line continuation
assertSame("", eval("`\\\n`"))
assertSame("", eval("`\\\r`"))
assertSame("", eval("`\\\u2028`"))
assertSame("", eval("`\\\u2029`"))


// source character
assertSame("", ``);
assertSame("`", `\``);
assertSame("$", `$`);
assertSame("$$", `$$`);
assertSame("$$}", `$$}`);


// simple variable substitution
var foo = "FOO", bar = "BAR";
assertSame("baz", `baz`);
assertSame(foo, `${foo}`);
assertSame("pre" + foo, `pre${foo}`);
assertSame(foo + "post", `${foo}post`);
assertSame("pre" + foo + "post", `pre${foo}post`);
assertSame(foo + bar, `${foo}${bar}`);
assertSame(foo + "-" + bar, `${foo}-${bar}`);


// expression substitution
assertSame("1", `${1}`);
assertSame("12", `${1}${2}`);
assertSame("3", `${1 + 2}`);
assertSame("2", `${1, 2}`);


// perform ToString() for each substitution
assertSame("ToString", `${{toString(){ return "ToString" }}}`);
assertSame("[object Object]", `${{valueOf(){ return "ValueOf" }}}`);
assertSame("ValueOf", `${{valueOf(){ return "ValueOf" }, __proto__: null}}`);
assertThrows(TypeError, () => `${{__proto__: null}}`);

assertSame("ToString", `${{toString(){ return "ToString" }, valueOf(){ return "ValueOf" }}}`);


var records = [];
function recordingHandler(siteObj) {
  var subs = [].splice.call(arguments, 1);
  records.push({siteObj: siteObj, subs: subs});
  return recordingHandler;
}

function cooked(siteObj) {
  return siteObj[0];
}

function raw(siteObj) {
  return siteObj.raw[0];
}

recordingHandler``;
for (var i = 0; i < 2; ++i)
  recordingHandler``;
assertSame(3, records.length);

assertSame(records[0].siteObj, records[1].siteObj);
assertSame(records[1].siteObj, records[2].siteObj);

assertTrue("raw" in records[0].siteObj);

assertTrue(Array.isArray(records[0].siteObj));

assertTrue(Array.isArray(records[0].siteObj.raw));

assertSame(1, records[0].siteObj.length);
assertSame("", records[0].siteObj[0]);

assertSame(1, records[0].siteObj.raw.length);
assertSame("", records[0].siteObj.raw[0]);

assertTrue(Object.isFrozen(records[0].siteObj.raw));

assertTrue(Object.isFrozen(records[0].siteObj));

// Runtime Semantics: ArgumentListEvaluation
assertSame(0, records[0].subs.length);


// same call-site object for different handlers
var a = [
  function(siteObj){ return siteObj },
  function(siteObj){ return siteObj },
].map(fn => fn``);
assertSame(a[0], a[1]);

// Same call-site in function
function templInFunctionWithLoop() {
  const id = x => x;
  let r = [];
  for (let i = 0; i < 2; ++i) {
    r.push(id``);
  }
  assertSame(2, r.length);
  assertSame(r[0], r[1]);
}
templInFunctionWithLoop();


records.length = 0;
recordingHandler`${foo}`;

assertSame(1, records[0].subs.length);
assertSame(foo, records[0].subs[0]);
assertSame(2, records[0].siteObj.length);
assertSame("", records[0].siteObj[0]);
assertSame("", records[0].siteObj[1]);
assertSame(2, records[0].siteObj.raw.length);
assertSame("", records[0].siteObj.raw[0]);
assertSame("", records[0].siteObj.raw[1]);


records.length = 0;
recordingHandler`a${foo}`;

assertSame(1, records[0].subs.length);
assertSame(foo, records[0].subs[0]);
assertSame(2, records[0].siteObj.length);
assertSame("a", records[0].siteObj[0]);
assertSame("", records[0].siteObj[1]);
assertSame(2, records[0].siteObj.raw.length);
assertSame("a", records[0].siteObj.raw[0]);
assertSame("", records[0].siteObj.raw[1]);


records.length = 0;
recordingHandler`${foo}b`;

assertSame(1, records[0].subs.length);
assertSame(foo, records[0].subs[0]);
assertSame(2, records[0].siteObj.length);
assertSame("", records[0].siteObj[0]);
assertSame("b", records[0].siteObj[1]);
assertSame(2, records[0].siteObj.raw.length);
assertSame("", records[0].siteObj.raw[0]);
assertSame("b", records[0].siteObj.raw[1]);


records.length = 0;
recordingHandler`a${foo}b`;

assertSame(1, records[0].subs.length);
assertSame(foo, records[0].subs[0]);
assertSame(2, records[0].siteObj.length);
assertSame("a", records[0].siteObj[0]);
assertSame("b", records[0].siteObj[1]);
assertSame(2, records[0].siteObj.raw.length);
assertSame("a", records[0].siteObj.raw[0]);
assertSame("b", records[0].siteObj.raw[1]);


// LineTerminatorSequence
assertSame("\n\n\u2028\u2029", eval("cooked`\n\r\u2028\u2029`", {cooked: cooked}));
assertSame("\n\n\u2028\u2029", eval("raw`\n\r\u2028\u2029`", {raw: raw}));

// LineContinuation
assertSame("", eval("cooked`\\\n\\\r\\\u2028\\\u2029`", {cooked: cooked}));
assertSame("\\\n\\\n\\\u2028\\\u2029", eval("raw`\\\n\\\r\\\u2028\\\u2029`", {raw: raw}));

// Escape Sequences
assertSame("\n\r\u2028\u2029", cooked`\n\r\u2028\u2029`);
assertSame("\\n\\r\\u2028\\u2029", raw`\n\r\u2028\u2029`);

assertSame("\0", cooked`\0`);
assertSame("\\0", raw`\0`);

assertSame("\0", cooked`\x00`);
assertSame("\\x00", raw`\x00`);

assertSame("\0", cooked`\u0000`);
assertSame("\\u0000", raw`\u0000`);

assertSame("\0", cooked`\u{0}`);
assertSame("\\u{0}", raw`\u{0}`);

assertSame("\'\"\\\b\f\n\r\t\v", cooked`\'\"\\\b\f\n\r\t\v`);
assertSame("\\'\\\"\\\\\\b\\f\\n\\r\\t\\v", raw`\'\"\\\b\f\n\r\t\v`);

assertSame("\r\n", cooked`\r\n`);
assertSame("\\r\\n", raw`\r\n`);


// MemberExpression, CallExpression, NewExpression

var handler = {rec: recordingHandler, get(){ return recordingHandler }};
handler.self = handler;

records.length = 0;
var nums = 0;
nums += 1; handler.rec``;
nums += 1; handler['rec']``;
nums += 1; handler.get()``;
nums += 1; handler['get']()``;
nums += 1; handler.self.rec``;
nums += 1; handler.self['rec']``;
nums += 1; handler.self.get()``;
nums += 1; handler.self['get']()``;
nums += 1; (1,handler.rec)``;
nums += 2; recordingHandler````;
nums += 2; handler.rec````;
assertSame(nums, records.length);


assertTrue(new (() => Object)`` instanceof Object);
assertSame("A", (() => ({a: "A"}))``['a']);
assertSame("A", (() => ({a: "A"}))``.a);
assertSame("A", (() => () => "A")``());


// nested template
assertSame("baz", `${`baz`}`);
assertSame(foo, `${`${foo}`}`);
assertSame("<[" + foo + "]>", `<${`[${foo}]`}>`);

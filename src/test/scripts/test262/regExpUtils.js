// Replace definitions from harness/regExpUtils.js

function buildString({ loneCodePoints, ranges }) {
  return $buildRegExpUnicodeString(loneCodePoints, ranges);
}

function testPropertyEscapes(regex, string, expression) {
  if (!regex.test(string)) {
    for (const symbol of string) {
      const hex = symbol
        .codePointAt(0)
        .toString(16)
        .toUpperCase()
        .padStart(6, "0");
      assert(
        regex.test(symbol),
        `\`${ expression }\` should match U+${ hex } (\`${ symbol }\`)`
      );
    }
  }
}

function matchValidator(expectedEntries, expectedIndex, expectedInput) {
  return function(match) {
    assert.compareArray(match, expectedEntries, 'Match entries');
    assert.sameValue(match.index, expectedIndex, 'Match index');
    assert.sameValue(match.input, expectedInput, 'Match input');
  }
}

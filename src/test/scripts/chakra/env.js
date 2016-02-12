/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
{
const global = this;
const std_String = String;

$MESSAGE = null;
$PRINT = print;

WScript = {
  lineCount: 0,
  nextMessage(sep) {
    var message = $MESSAGE();
    if (message === null) {
      return null;
    }
    for (var i = 0; i < sep.length; ++i) {
      var m = $MESSAGE();
      if (m === null) {
        break;
      }
      message += sep[i] + m;
    }
    WScript.lineCount += 1 + i;
    return message;
  },
  Validate(actual) {
    var sep = [];
    for (var i = 0;;) {
      var cr = actual.indexOf('\r', i);
      var nl = actual.indexOf('\n', i);
      if (cr !== -1 && (nl === -1 || cr < nl)) {
        if (false && cr + 1 === nl) {
          i = cr + 2;
          sep.push("\r\n");
        } else {
          i = cr + 1;
          sep.push("\r");
        }
      } else if (nl !== -1) {
        i = nl + 1;
        sep.push("\n");
      } else {
        break;
      }
    }
    if (actual.indexOf('\0') !== -1) {
      actual = actual.substring(0, actual.indexOf('\0'));
    }
    var expected = WScript.nextMessage(sep);
    if (expected === null) {
      var err = new Error(`Expected <EOF>, but got: '${actual}'`);
      $async_enqueueTask(() => { throw err; });
      return;
    }
    var lc = WScript.lineCount;
    if (expected !== actual) {
      // Avoid comparing error messages, assume the correct error was thrown when the error type matches.
      var nativeErrors = ["EvalError", "RangeError", "ReferenceError", "SyntaxError", "TypeError", "URIError"];
      for (var i = 0; i < nativeErrors.length; ++i) {
        if (expected.startsWith(nativeErrors[i])) {
          if (!actual.startsWith(nativeErrors[i])) {
            var err = new Error(`${lc}: Expected: '${expected}', but got: '${actual}'`);
            $async_enqueueTask(() => { throw err; });
          }
          return;
        }
      }
      // Report the error in the next event loop turn, otherwise it messes up the test framework.
      var err = new Error(`${lc} Expected: '${expected}', but got: '${actual}' `);
      $async_enqueueTask(() => { throw err; });
    }
  },
  Echo(...args) {
    var message = "";
    for (var i = 0; i < args.length; ++i) {
      message += std_String(args[i]);
      if (i + 1 < args.length) {
        message += " ";
      }
    }
    if ($MESSAGE) {
      return WScript.Validate(message);
    }
    $PRINT(message);
  },
  Arguments: [],
  LoadScriptFile(path, type) {
    "use strict";
    path = std_String(path);
    path = path.replace(/\\/g, "/");
    if (type !== void 0 && type !== "self") {
      throw new Error(`Unsupported type: ${type}`);
    }
    if (path.endsWith("UnitTestFramework.js") && installTestFrameworkHooks) {
      installTestFrameworkHooks();
      installTestFrameworkHooks = null;
    }
    // Tail-call required for relative-to-script semantics.
    return loadRelativeToScript(path);
  }
};

print = WScript.Echo;

function installTestFrameworkHooks() {
  let testRunnerObj;
  Object.defineProperty(global, "testRunner", {
    get() {
      return testRunnerObj;
    },
    set(testRunner) {
      if (!$MESSAGE) {
        // Replace testRunner.runTest() so it directly throws the exception.
        testRunner.runTest = function runTest(testIndex, testName, testBody) {
          "use strict";
          return testBody();
        };
      }
      testRunnerObj = testRunner;
    },
    configurable: false
  });

  let assertObj;
  Object.defineProperty(global, "assert", {
    get() {
      return assertObj;
    },
    set(assert) {
      // Remove the "expectedErrorMessage" parameter in order to avoid comparing implementation
      // specific error messages.
      const originalThrows = assert.throws;
      assert.throws = function throws(testFunction, expectedException, message, expectedErrorMessage) {
        "use strict";
        return originalThrows(testFunction, expectedException, message);
      };
      assertObj = assert;
    },
    configurable: false
  });
}

}

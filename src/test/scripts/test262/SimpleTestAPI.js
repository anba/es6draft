// Replace definitions from harness/sta.js

class Test262Error extends Error {
  get name() {
    return "Test262Error";
  }
}

function $ERROR(message) {
  throw new Test262Error(message);
}

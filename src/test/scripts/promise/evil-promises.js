"use strict";

describe("Evil promises should not be able to break invariants", function () {
    specify("resolving to a promise that calls onFulfilled twice", function (done) {
        var evilPromise = Promise.resolve();
        evilPromise.then = function (f) {
            f(1);
            f(2);
        };

        var calledAlready = false;
        var resolvedToEvil = OrdinaryConstruct(Promise, [function (resolve) { resolve(evilPromise); }]);
        resolvedToEvil.then(function (value) {
            assert.strictEqual(calledAlready, false);
            calledAlready = true;
            assert.strictEqual(value, 1);
        })
        .then(done, done);
    });
});

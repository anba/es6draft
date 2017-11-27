// Increase the maximum allowed time difference because we're running multiple tests in parallel, which can result in
// larger timeouts compared to single-threaded test runners.
var $ATOMICS_MAX_TIME_EPSILON = 1000;

/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, fail
} = Assert;

const True = () => true;
const False = () => false;
const Throw = (v = null) => { throw v; };

// Empty try-catch-finally blocks
assertSame(void 0, eval(`"init"; try { } catch (e) { }`));
assertSame(void 0, eval(`"init"; try { } catch (e) { } finally { }`));
assertSame(void 0, eval(`"init"; try { } finally { }`));

// Try block without abrupt completion
assertSame("try", eval(`"init"; try { "try"; } finally { }`));
assertSame("try", eval(`"init"; try { "try"; } finally { "finally"; }`));
assertSame("try", eval(`"init"; try { "try"; } catch (e) { } finally { }`));
assertSame("try", eval(`"init"; try { "try"; } catch (e) { } finally { "finally"; }`));

// Catch block without abrupt completion
assertSame("catch", eval(`"init"; try { "try"; throw null; } catch (e) { "catch"; } finally { }`));
assertSame("catch", eval(`"init"; try { "try"; throw null; } catch (e) { "catch"; } finally { "finally"; }`));

// Try block with abrupt completion
assertSame(void 0, eval(`"init"; L: try { break L; } finally { }`));
assertSame("try", eval(`"init"; L: try { "try"; break L; } finally { }`));
assertSame(void 0, eval(`"init"; L: try { break L; } finally { "finally"; }`));
assertSame("try", eval(`"init"; L: try { "try"; break L; } finally { "finally"; }`));

// Finally block with abrupt completion
assertSame("finally", eval(`L: try { "try"; } finally { "finally"; break L; }`));

// TryStatement : try Block Catch
// (1) try completes with throw completion.
// (A) catch completes with value
assertSame("catch", eval(`"init"; try { "try"; Throw(); } catch (e) { "catch"; }`));
assertSame("catch", eval(`"init"; L: try { "try"; Throw(); } catch (e) { "catch"; break L; }`));
assertSame("catch", eval(`"init"; while (True()) try { "try"; Throw(); } catch (e) { "catch"; break; }`));
assertSame("catch", eval(`"init"; do try { "try"; Throw(); } catch (e) { "catch"; continue; } while(False());`));
// (B) catch completes with empty
assertSame(void 0, eval(`"init"; try { "try"; Throw(); } catch (e) { }`));
assertSame(void 0, eval(`"init"; L: try { "try"; Throw(); } catch (e) { break L; }`));
assertSame(void 0, eval(`"init"; while (True()) try { "try"; Throw(); } catch (e) { break; }`));
assertSame(void 0, eval(`"init"; do try { "try"; Throw(); } catch (e) { continue; } while(False());`));

// (2) try completes with normal completion.
// (A) try completes with value
assertSame("try", eval(`"init"; try { "try"; } catch (e) { fail("unreachable"); }`));
assertSame("try", eval(`"init"; L: try { "try"; break L; } catch (e) { fail("unreachable"); }`));
assertSame("try", eval(`"init"; while (True()) try { "try"; break; } catch (e) { fail("unreachable"); }`));
assertSame("try", eval(`"init"; do try { "try"; continue; } catch (e) { fail("unreachable");} while(False());`));
// (B) try completes with empty
assertSame(void 0, eval(`"init"; try { } catch (e) { fail("unreachable"); }`));
assertSame(void 0, eval(`"init"; L: try { break L; } catch (e) { fail("unreachable"); }`));
assertSame(void 0, eval(`"init"; while (True()) try { break; } catch (e) { fail("unreachable"); }`));
assertSame(void 0, eval(`"init"; do try { continue; } catch (e) { fail("unreachable"); } while(False());`));


// TryStatement : try Block Finally
// (1) finally completes with normal completion
// (A) finally completes with value
assertSame("try", eval(`"init"; try { "try"; } finally { "finally"; }`));
assertSame("try", eval(`"init"; L: try { "try"; break L; } finally { "finally"; }`));
assertSame("try", eval(`"init"; while (True()) try { "try"; break; } finally { "finally"; }`));
assertSame("try", eval(`"init"; do try { "try"; continue; } finally { "finally"; } while(False());`));
assertSame("try", eval(`"init"; try { "try"; } finally { L: try { "finally"; } finally { break L; } }`));
assertSame("try", eval(`"init"; try { try { "try"; } finally { L: try { "finally"; } finally { break L; } } } finally { }`));
// (B) finally completes with empty
assertSame("try", eval(`"init"; try { "try"; } finally { }`));
assertSame("try", eval(`"init"; L: try { "try"; break L; } finally { }`));
assertSame("try", eval(`"init"; while (True()) try { "try"; break; } finally { }`));
assertSame("try", eval(`"init"; do try { "try"; continue; } finally { } while(False());`));
assertSame("try", eval(`"init"; try { "try"; } finally { L: try { } finally { break L; } }`));
assertSame("try", eval(`"init"; try { try { "try"; } finally { L: try { } finally { break L; } } } finally { }`));

// (2) finally completes with abrupt completion
// (A) finally completes with value
assertSame("finally", eval(`"init"; L: try { "try"; } finally { "finally"; break L; }`));
assertSame("finally", eval(`"init"; while (True()) try { "try"; } finally { "finally"; break; }`));
assertSame("finally", eval(`"init"; do try { "try"; } finally { "finally"; continue; } while(False());`));
assertSame("finally", eval(`"init"; L: try { "try"; break L; } finally { "finally"; break L; }`));
assertSame("finally", eval(`"init"; while (True()) try { "try"; break; } finally { "finally"; break; }`));
assertSame("finally", eval(`"init"; do try { "try"; continue; } finally { "finally"; continue; } while(False());`));
// (B) finally completes with empty
assertSame(void 0, eval(`"init"; L: try { "try"; } finally { break L; }`));
assertSame(void 0, eval(`"init"; while (True()) try { "try"; } finally { break; }`));
assertSame(void 0, eval(`"init"; do try { "try"; } finally { continue; } while(False());`));
assertSame(void 0, eval(`"init"; L: try { "try"; break L; } finally { break L; }`));
assertSame(void 0, eval(`"init"; while (True()) try { "try"; break; } finally { break; }`));
assertSame(void 0, eval(`"init"; do try { "try"; continue; } finally { continue; } while(False());`));

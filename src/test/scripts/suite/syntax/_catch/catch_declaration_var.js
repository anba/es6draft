/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// Catch variable is BindingIdentifier
Function(`try {} catch (e) { var e = [] }`);
Function(`try {} catch (e) { var [e] = [] }`);
Function(`try {} catch (e) { var {e} = [] }`);


// Catch variable is ArrayBindingPattern
assertSyntaxError(`try {} catch ([e]) { var e = [] }`);
assertSyntaxError(`try {} catch ([e]) { var [e] = [] }`);
assertSyntaxError(`try {} catch ([e]) { var {e} = [] }`);


// Catch variable is ObjectBindingPattern
assertSyntaxError(`try {} catch ({e}) { var e = [] }`);
assertSyntaxError(`try {} catch ({e}) { var [e] = [] }`);
assertSyntaxError(`try {} catch ({e}) { var {e} = [] }`);

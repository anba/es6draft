/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// Catch variable is BindingIdentifier
Function(`try {} catch (e) { for(var e = [];;) ; }`);
Function(`try {} catch (e) { for(var [e] = [];;) ; }`);
Function(`try {} catch (e) { for(var {e} = [];;) ; }`);


// Catch variable is ArrayBindingPattern
assertSyntaxError(`try {} catch ([e]) { for(var e = [];;) ; }`);
assertSyntaxError(`try {} catch ([e]) { for(var [e] = [];;) ; }`);
assertSyntaxError(`try {} catch ([e]) { for(var {e} = [];;) ; }`);


// Catch variable is ObjectBindingPattern
assertSyntaxError(`try {} catch ({e}) { for(var e = [];;) ; }`);
assertSyntaxError(`try {} catch ({e}) { for(var [e] = [];;) ; }`);
assertSyntaxError(`try {} catch ({e}) { for(var {e} = [];;) ; }`);

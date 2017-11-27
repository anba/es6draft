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
assertSyntaxError(`try {} catch (e) { for(var e of []) ; }`);
assertSyntaxError(`try {} catch (e) { for(var [e] of []) ; }`);
assertSyntaxError(`try {} catch (e) { for(var {e} of []) ; }`);


// Catch variable is ArrayBindingPattern
assertSyntaxError(`try {} catch ([e]) { for(var e of []) ; }`);
assertSyntaxError(`try {} catch ([e]) { for(var [e] of []) ; }`);
assertSyntaxError(`try {} catch ([e]) { for(var {e} of []) ; }`);


// Catch variable is ObjectBindingPattern
assertSyntaxError(`try {} catch ({e}) { for(var e of []) ; }`);
assertSyntaxError(`try {} catch ({e}) { for(var [e] of []) ; }`);
assertSyntaxError(`try {} catch ({e}) { for(var {e} of []) ; }`);

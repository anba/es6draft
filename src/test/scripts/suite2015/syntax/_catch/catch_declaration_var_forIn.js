/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
} = Assert;

// Catch variable is BindingIdentifier
Function(`try {} catch (e) { for(var e in []) ; }`);
Function(`try {} catch (e) { for(var [e] in []) ; }`);
Function(`try {} catch (e) { for(var {e} in []) ; }`);


// Catch variable is ArrayBindingPattern
Function(`try {} catch ([e]) { for(var e in []) ; }`);
Function(`try {} catch ([e]) { for(var [e] in []) ; }`);
Function(`try {} catch ([e]) { for(var {e} in []) ; }`);


// Catch variable is ObjectBindingPattern
Function(`try {} catch ({e}) { for(var e in []) ; }`);
Function(`try {} catch ({e}) { for(var [e] in []) ; }`);
Function(`try {} catch ({e}) { for(var {e} in []) ; }`);

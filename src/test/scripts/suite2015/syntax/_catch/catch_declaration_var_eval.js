/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
} = Assert;

// Catch variable is BindingIdentifier
try { throw []; } catch (e) { eval(`var e = []`); }
try { throw []; } catch (e) { eval(`var [e] = []`); }
try { throw []; } catch (e) { eval(`var {e} = []`); }


// Catch variable is ArrayBindingPattern
try { throw []; } catch ([e]) { eval(`var e = []`); }
try { throw []; } catch ([e]) { eval(`var [e] = []`); }
try { throw []; } catch ([e]) { eval(`var {e} = []`); }


// Catch variable is ObjectBindingPattern
try { throw []; } catch ({e}) { eval(`var e = []`); }
try { throw []; } catch ({e}) { eval(`var [e] = []`); }
try { throw []; } catch ({e}) { eval(`var {e} = []`); }

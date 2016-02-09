/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
} = Assert;

// Catch variable is BindingIdentifier
try { throw []; } catch (e) { eval(`let e = []`); }
try { throw []; } catch (e) { eval(`let [e] = []`); }
try { throw []; } catch (e) { eval(`let {e} = []`); }

try { throw []; } catch (e) { eval(`const e = []`); }
try { throw []; } catch (e) { eval(`const [e] = []`); }
try { throw []; } catch (e) { eval(`const {e} = []`); }

try { throw []; } catch (e) { eval(`class e {}`); }


// Catch variable is ArrayBindingPattern
try { throw []; } catch ([e]) { eval(`let e = []`); }
try { throw []; } catch ([e]) { eval(`let [e] = []`); }
try { throw []; } catch ([e]) { eval(`let {e} = []`); }

try { throw []; } catch ([e]) { eval(`const e = []`); }
try { throw []; } catch ([e]) { eval(`const [e] = []`); }
try { throw []; } catch ([e]) { eval(`const {e} = []`); }

try { throw []; } catch (e) { eval(`class e {}`); }


// Catch variable is ObjectBindingPattern
try { throw []; } catch ({e}) { eval(`let e = []`); }
try { throw []; } catch ({e}) { eval(`let [e] = []`); }
try { throw []; } catch ({e}) { eval(`let {e} = []`); }

try { throw []; } catch ({e}) { eval(`const e = []`); }
try { throw []; } catch ({e}) { eval(`const [e] = []`); }
try { throw []; } catch ({e}) { eval(`const {e} = []`); }

try { throw []; } catch (e) { eval(`class e {}`); }

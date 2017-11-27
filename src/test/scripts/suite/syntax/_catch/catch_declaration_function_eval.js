/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// Catch variable is BindingIdentifier
try { throw []; } catch (e) { eval(`function e(){}`); }
assertThrows(SyntaxError, () => { try { throw []; } catch (e) { eval(`function* e(){}`); } });


// Catch variable is ArrayBindingPattern
assertThrows(SyntaxError, () => { try { throw []; } catch ([e]) { eval(`function e(){}`); } });
assertThrows(SyntaxError, () => { try { throw []; } catch ([e]) { eval(`function* e(){}`); } });


// Catch variable is ObjectBindingPattern
assertThrows(SyntaxError, () => { try { throw []; } catch ({e}) { eval(`function e(){}`); } });
assertThrows(SyntaxError, () => { try { throw []; } catch ({e}) { eval(`function* e(){}`); } });

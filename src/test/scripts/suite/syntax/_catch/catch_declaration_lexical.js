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
assertSyntaxError(`try {} catch (e) { let e = [] }`);
assertSyntaxError(`try {} catch (e) { let [e] = [] }`);
assertSyntaxError(`try {} catch (e) { let {e} = [] }`);

assertSyntaxError(`try {} catch (e) { const e = [] }`);
assertSyntaxError(`try {} catch (e) { const [e] = [] }`);
assertSyntaxError(`try {} catch (e) { const {e} = [] }`);

assertSyntaxError(`try {} catch (e) { class e {} }`);


// Catch variable is ArrayBindingPattern
assertSyntaxError(`try {} catch ([e]) { let e = [] }`);
assertSyntaxError(`try {} catch ([e]) { let [e] = [] }`);
assertSyntaxError(`try {} catch ([e]) { let {e} = [] }`);

assertSyntaxError(`try {} catch ([e]) { const e = [] }`);
assertSyntaxError(`try {} catch ([e]) { const [e] = [] }`);
assertSyntaxError(`try {} catch ([e]) { const {e} = [] }`);

assertSyntaxError(`try {} catch (e) { class e {} }`);


// Catch variable is ObjectBindingPattern
assertSyntaxError(`try {} catch ({e}) { let e = [] }`);
assertSyntaxError(`try {} catch ({e}) { let [e] = [] }`);
assertSyntaxError(`try {} catch ({e}) { let {e} = [] }`);

assertSyntaxError(`try {} catch ({e}) { const e = [] }`);
assertSyntaxError(`try {} catch ({e}) { const [e] = [] }`);
assertSyntaxError(`try {} catch ({e}) { const {e} = [] }`);

assertSyntaxError(`try {} catch (e) { class e {} }`);

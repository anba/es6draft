/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertTrue
} = Assert;

assertTrue("await" in {\u0061wait: 0});
assertTrue("async" in {\u0061sync: 0});

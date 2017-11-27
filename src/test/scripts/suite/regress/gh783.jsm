/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
} = Assert;

import {x, y} from './resources/gh783_1.jsm';

assertSame(x, 1);
assertSame(y, 1);

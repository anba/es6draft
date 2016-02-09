/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
import {
  assertSame
} from "../suite/lib/assert.jsm";

import* as all from "./resources/export_json_entry";

assertSame("bar", all.Foo);

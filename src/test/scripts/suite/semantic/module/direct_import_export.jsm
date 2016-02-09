/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
import {assertSame} from "lib/assert.jsm";

import { value } from "./resources/direct_export_import_single.jsm";

assertSame("abc", value);

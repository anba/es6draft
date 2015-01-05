/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
import {assertEquals} from "lib/assert.jsm";

// TODO: Replace with Reflect.global when available
const global = System.global;

import log from "./resources/export_set_global_property_logger.jsm";
import "./resources/export_set_global_property1.jsm";
import "./resources/export_set_global_property2.jsm";

assertEquals([
  {property1: "property1"},
  {property2: "property2"},
], log);

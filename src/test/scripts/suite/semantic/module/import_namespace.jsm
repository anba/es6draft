/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
import {assertSame, assertEquals} from "lib/assert.jsm";

import * as modSingleExport from "./resources/export_single.jsm";
assertEquals(["value"], Object.getOwnPropertyNames(modSingleExport));
assertEquals(["value"], [...modSingleExport[Symbol.iterator]()]);

import * as modMultiExport from "./resources/export_multi.jsm";
assertEquals(["value1", "value2"], Object.getOwnPropertyNames(modMultiExport));
assertEquals(["value1", "value2"], [...modMultiExport[Symbol.iterator]()]);

import * as modMultiExportNotSorted from "./resources/export_multi_not_sorted.jsm";
assertEquals(["A_export", "B_export"], Object.getOwnPropertyNames(modMultiExportNotSorted));
assertEquals(["A_export", "B_export"], [...modMultiExportNotSorted[Symbol.iterator]()]);

import * as modDefaultExprExport from "./resources/export_default_expression.jsm";
assertEquals(["default"], Object.getOwnPropertyNames(modDefaultExprExport));
assertEquals(["default"], [...modDefaultExprExport[Symbol.iterator]()]);

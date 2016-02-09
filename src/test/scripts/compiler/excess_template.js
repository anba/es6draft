/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// excess template literal
Function("`" + "${1}".repeat(0xff) + "`");
Function("`" + "${1}".repeat(0xfff) + "`");
Function("`" + "${1}".repeat(0xffff) + "`");

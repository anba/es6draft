/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
id: sec-names-and-keywords
info: Test grandfathered characters of ID_Continue.
description: >
  Grandfathered characters (Other_ID_Start + Other_ID_Continue)
---*/

// Other_ID_Start (Unicode 4.0)
var a℘; // U+2118
var a℮; // U+212E
var a゛; // U+309B
var a゜; // U+309C

// Other_ID_Start (Unicode 9.0)
var aᢅ; // U+1885
var aᢆ; // U+1886

// Other_ID_Continue (Unicode 4.1)
var a፩; // U+1369
var a፪; // U+136A
var a፫; // U+136B
var a፬; // U+136C
var a፭; // U+136D
var a፮; // U+136E
var a፯; // U+136F
var a፰; // U+1370
var a፱; // U+1371

// Other_ID_Continue (Unicode 5.1)
var a·; // U+00B7
var a·; // U+0387

// Other_ID_Continue (Unicode 6.0)
var a᧚; // U+19DA

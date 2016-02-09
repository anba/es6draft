/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 20.3.1.9 Local Time: local time computation at DST
// https://bugs.ecmascript.org/show_bug.cgi?id=4007

// Zone NAME                GMTOFF  RULES
//      America/Los_Angeles -8:00   US
// Rule NAME  FROM  TO  TYPE  IN  ON      AT    SAVE
//      US    2007  max -     Mar Sun>=8  2:00  1:00

assertSame(1425801600000, new Date(2015, 3 - 1, 8, 0, 0, 0, 0).getTime());
assertSame(1425805200000, new Date(2015, 3 - 1, 8, 1, 0, 0, 0).getTime());
assertSame(1425808800000, new Date(2015, 3 - 1, 8, 2, 0, 0, 0).getTime());
assertSame(1425808800000, new Date(2015, 3 - 1, 8, 3, 0, 0, 0).getTime());
assertSame(1425812400000, new Date(2015, 3 - 1, 8, 4, 0, 0, 0).getTime());

assertSame(1425808800000 + 5 * 60 * 1000, new Date(2015, 3 - 1, 8, 2, 5, 0, 0).getTime());
assertSame(1425808800000 + 5 * 60 * 1000, new Date(2015, 3 - 1, 8, 3, 5, 0, 0).getTime());

assertSame(1425808800000 - 1, new Date(2015, 3 - 1, 8, 2, 0, 0, -1).getTime());
assertSame(1425808800000 + 0, new Date(2015, 3 - 1, 8, 2, 0, 0, +0).getTime());
assertSame(1425808800000 + 1, new Date(2015, 3 - 1, 8, 2, 0, 0, +1).getTime());

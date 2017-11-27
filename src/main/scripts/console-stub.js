/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
{
    const global = %GlobalProperties();
    const PRINT = typeof print === "function" ? print : () => {};

    global.console = {
        log(...args) {
            %CallFunction(PRINT, null, ...args);
        }
    };
}

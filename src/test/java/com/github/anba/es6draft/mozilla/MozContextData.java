/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.mozilla;

import com.github.anba.es6draft.repl.functions.AtomicsTestFunctions;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;

/**
*
*/
final class MozContextData extends RuntimeContext.Data implements AtomicsTestFunctions.MailboxProvider {
    private final AtomicsTestFunctions.Mailbox mailbox = new AtomicsTestFunctions.Mailbox();

    @Override
    public AtomicsTestFunctions.Mailbox getMailbox() {
        return mailbox;
    }
}

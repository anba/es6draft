/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.Test;

import com.github.anba.es6draft.runtime.internal.Messages;

/**
 *
 */
public final class MessagesTest {
    @Test
    public void runTest() {
        Messages messages = Messages.create(Locale.ROOT);
        ResourceBundle resourceBundle = messages.getResourceBundle();
        for (Messages.Key key : Messages.Key.values()) {
            assertTrue(String.format("key %s [%s] not found", key, key.getId()),
                    resourceBundle.containsKey(key.getId()));
        }
    }
}

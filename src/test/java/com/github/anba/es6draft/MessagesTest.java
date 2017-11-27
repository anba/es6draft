/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

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
        Set<String> keySet = Arrays.stream(Messages.Key.values()).map(Messages.Key::getId).collect(Collectors.toSet());
        for (String key : Collections.list(resourceBundle.getKeys())) {
            assertTrue(String.format("unused message key %s", key), keySet.contains(key));
        }
    }
}

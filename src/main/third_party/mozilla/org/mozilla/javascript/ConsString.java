/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * <p>This class represents a string composed of two components, each of which
 * may be a <code>java.lang.String</code> or another ConsString.</p>
 *
 * <p>This string representation is optimized for concatenation using the "+"
 * operator. Instead of immediately copying both components to a new character
 * array, ConsString keeps references to the original components and only
 * converts them to a String if either toString() is called or a certain depth
 * level is reached.</p>
 *
 * <p>Note that instances of this class are only immutable if both parts are
 * immutable, i.e. either Strings or ConsStrings that are ultimately composed
 * of Strings.</p>
 *
 * <p>Both the name and the concept are borrowed from V8.</p>
 */
public final class ConsString implements CharSequence {
    private final int length;
    private int depth;
    private CharSequence s1, s2;

    public ConsString(CharSequence str1, CharSequence str2) {
        int length = 0, depth = 1;
        if (str1 instanceof ConsString) {
            ConsString s = (ConsString) str1;
            length += s.length;
            depth += s.depth;
            if (s.depth == 0) {
                // Directly access string if ConsString is flattened
                str1 = s.s1;
            }
        } else {
            length += ((String) str1).length();
        }
        if (str2 instanceof ConsString) {
            ConsString s = (ConsString) str2;
            length += s.length;
            depth += s.depth;
            if (s.depth == 0) {
                // Directly access string if ConsString is flattened
                str2 = s.s1;
            }
        } else {
            length += ((String) str2).length();
        }
        this.length = length;
        this.depth = depth;
        this.s1 = str1;
        this.s2 = str2;
        // Don't let it grow too deep, can cause stack overflows
        if (depth > 200 && str1 instanceof ConsString && str2 instanceof ConsString) {
            flatten();
        }
    }

    @Override
    public String toString() {
        return depth == 0 ? (String)s1 : flatten();
    }

    private String flatten() {
        if (depth > 0) {
            s1 = flatten(this);
            s2 = "";
            depth = 0;
        }
        return (String) s1;
    }

    private static String flatten(ConsString s) {
        char[] ca = new char[s.length()];
        appendTo(s, ca, 0);
        return new String(ca);
    }

    private static void appendTo(ConsString s, char[] ca, int offset) {
        for (;;) {
            // Flattened ConsString or both parts are simple Strings, just append and return
            if (s.depth <= 1) {
                String s1 = (String) s.s1, s2 = (String) s.s2;
                appendTo(s1, ca, offset);
                appendTo(s2, ca, offset + s1.length());
                return;
            }
            // At least one part is a ConsString
            if (s.s1 instanceof String) {
                // Left is String and right is ConsString, append left and continue with right
                String s1 = (String) s.s1;
                s = (ConsString) s.s2;
                appendTo(s1, ca, offset);
                offset += s1.length();
            } else if (s.s2 instanceof String) {
                // Left is ConsString and right is String, append right and continue with left
                String s2 = (String) s.s2;
                s = (ConsString) s.s1;
                appendTo(s2, ca, offset + s.length());
            } else {
                // Both are ConsStrings, descend into less deeper one and continue with the other
                ConsString s1 = (ConsString) s.s1, s2 = (ConsString) s.s2;
                if (s1.depth < s2.depth) {
                    s = s2;
                    appendTo(s1, ca, offset);
                    offset += s1.length();
                } else {
                    s = s1;
                    appendTo(s2, ca, offset + s.length());
                }
            }
        }
    }

    private static void appendTo(String s, char[] ca, int offset) {
        s.getChars(0, s.length(), ca, offset);
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        return toString().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return toString().substring(start, end);
    }

}

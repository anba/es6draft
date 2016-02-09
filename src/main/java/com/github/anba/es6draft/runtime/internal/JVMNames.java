/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

/**
 * @see <a href="https://blogs.oracle.com/jrose/entry/symbolic_freedom_in_the_vm">symbolic freedom
 *      in the VM</a>
 */
public final class JVMNames {
    private JVMNames() {
    }

    /**
     * Adds the given prefix/suffix pair to the bytecode name, neither prefix nor suffix may contain
     * characters which require additional escapes.
     * 
     * @param name
     *            the bytecode name
     * @param prefix
     *            the new prefix
     * @param suffix
     *            the new suffix
     * @return the concatenated string
     */
    public static String addPrefixSuffix(String name, String prefix, String suffix) {
        assert name.length() >= 2 : "expected bytecode name";
        assert prefix.isEmpty() || toBytecodeName(prefix) == prefix : "prefix needs escapes";
        assert suffix.isEmpty() || toBytecodeName(suffix) == suffix : "suffix needs escapes";
        StringBuilder sb = new StringBuilder(2 + name.length() + prefix.length() + suffix.length());
        if (name.charAt(0) != '\\' || prefix.isEmpty()) {
            // simple concat if name is not mangled or prefix is empty
            sb.append(prefix).append(name);
        } else {
            // add \= indicator before adding prefix
            sb.append("\\=").append(prefix);
            if (name.charAt(1) == '=') {
                // \= indicator was already present, append remaining characters
                sb.append(name, 2, name.length());
            } else {
                sb.append(name);
            }
        }
        // add suffix and return result
        return sb.append(suffix).toString();
    }

    /**
     * Returns the corresponding bytecode name of the input string.
     * 
     * @param n
     *            the unescaped input string
     * @return the escaped bytecode name
     */
    public static String toBytecodeName(String n) {
        int length = n.length();
        if (length == 0) {
            return "\\=";
        }
        NO_ESCAPE: {
            for (int i = 0; i < length; ++i) {
                char c = n.charAt(i);
                if (c == '\\' && i + 1 < length) {
                    char d = n.charAt(i + 1);
                    if ((i == 0 && d == '=') || unescape(d) != d) {
                        // case 1: accidental escape
                        break NO_ESCAPE;
                    }
                }
                if (c != '\\' && escape(c) != c) {
                    // case 2: dangerous character
                    break NO_ESCAPE;
                }
            }
            return n;
        }
        StringBuilder sb = new StringBuilder(length + 8);
        for (int i = 0; i < length; ++i) {
            char c = n.charAt(i);
            if (c == '\\' && i + 1 < length) {
                char d = n.charAt(i + 1);
                if ((i == 0 && d == '=') || unescape(d) != d) {
                    // case 1: accidental escape
                    sb.append('\\').append(escape(c));
                    continue;
                }
            }
            if (c != '\\' && escape(c) != c) {
                // case 2: dangerous character
                sb.append('\\').append(escape(c));
                continue;
            }
            sb.append(c);
        }
        // case 3: prepend \= if first char is not already \
        if (sb.charAt(0) != '\\') {
            sb.insert(0, "\\=");
        }
        return sb.toString();
    }

    /**
     * Returns the base name of the supplied bytecode name.
     * 
     * @param n
     *            the bytecode name
     * @return the unescaped base name
     */
    public static String fromBytecodeName(String n) {
        assert n.length() > 0;
        if (n.charAt(0) != '\\') {
            // mangled names always start with \
            return n;
        }
        int length = n.length();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            char c = n.charAt(i);
            if (c == '\\' && i + 1 < length) {
                char d = n.charAt(i + 1);
                if (i == 0 && d == '=') {
                    // no output for \= prefix
                    i += 1;
                    continue;
                }
                if (unescape(d) != d) {
                    sb.append(unescape(d));
                    i += 1;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static char escape(char c) {
        switch (c) {
        case '/':
            return '|';
        case '.':
            return ',';
        case ';':
            return '?';
        case '$':
            return '%';
        case '<':
            return '^';
        case '>':
            return '_';
        case '[':
            return '{';
        case ']':
            return '}';
        case ':':
            return '!';
        case '\\':
            return '-';
        default:
            return c;
        }
    }

    private static char unescape(char c) {
        switch (c) {
        case '|':
            return '/';
        case ',':
            return '.';
        case '?':
            return ';';
        case '%':
            return '$';
        case '^':
            return '<';
        case '_':
            return '>';
        case '{':
            return '[';
        case '}':
            return ']';
        case '!':
            return ':';
        case '-':
            return '\\';
        default:
            return c;
        }
    }
}

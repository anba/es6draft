/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import static org.joni.constants.MetaChar.INEFFECTIVE_META_CHAR;

import org.joni.Option;
import org.joni.Syntax;
import org.joni.Syntax.MetaCharTable;
import org.joni.constants.SyntaxProperties;

/**
 * Joni {@link Syntax} configuration
 */
final class JoniSyntax implements SyntaxProperties {
    private JoniSyntax() {
    }

    // - OP_ESC_W_WORD, OP_ESC_S_WHITE_SPACE and OP_ESC_D_DIGIT are not enabled, because they
    // require an ASCII-compatible encoding
    // - OP_ESC_OCTAL3 is not enabled, because it's semantics are not compatible
    private static final int op = OP_DOT_ANYCHAR | OP_ASTERISK_ZERO_INF | OP_PLUS_ONE_INF
            | OP_QMARK_ZERO_ONE | OP_BRACE_INTERVAL | OP_VBAR_ALT | OP_LPAREN_SUBEXP
            | OP_ESC_AZ_BUF_ANCHOR | OP_DECIMAL_BACKREF | OP_BRACKET_CC | OP_ESC_B_WORD_BOUND
            | OP_LINE_ANCHOR | OP_QMARK_NON_GREEDY | OP_ESC_CONTROL_CHARS | OP_ESC_C_CONTROL
            | OP_ESC_X_HEX2;

    // - OP2_OPTION_ECMASCRIPT is not enabled, because it breaks nested repeats
    private static final int op2 = OP2_QMARK_GROUP_EFFECT | OP2_OPTION_PERL | OP2_ESC_V_VTAB
            | OP2_ESC_U_HEX4 | OP2_ESC_P_BRACE_CHAR_PROPERTY;

    private static final int behaviour = BACKSLASH_ESCAPE_IN_CC | ALLOW_EMPTY_RANGE_IN_CC
            | DIFFERENT_LEN_ALT_LOOK_BEHIND;

    public static final Syntax ECMAScript = new Syntax(op, op2, behaviour, Option.NONE,
            new MetaCharTable('\\', /* esc */
            INEFFECTIVE_META_CHAR, /* anychar '.' */
            INEFFECTIVE_META_CHAR, /* anytime '*' */
            INEFFECTIVE_META_CHAR, /* zero or one time '?' */
            INEFFECTIVE_META_CHAR, /* one or more time '+' */
            INEFFECTIVE_META_CHAR /* anychar anytime */
            ));
}

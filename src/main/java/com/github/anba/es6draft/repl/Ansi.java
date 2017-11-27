/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

/**
 *
 */
final class Ansi {
    // https://en.wikipedia.org/wiki/ANSI_escape_code

    private enum Color {
        Black(0), Red(1), Green(2), Yellow(3), Blue(4), Magenta(5), Cyan(6), White(7);

        final int offset;

        private Color(int offset) {
            this.offset = offset;
        }
    }

    enum Attribute {
        // @formatter:off
        Reset(0),
        Bold(1),
        Underline(4),
        Negative(7),
        NormalIntensity(22),
        UnderlineNone(24),
        Positive(27),
        TextColorBlack(30 + Color.Black.offset),
        TextColorRed(30 + Color.Red.offset),
        TextColorGreen(30 + Color.Green.offset),
        TextColorYellow(30 + Color.Yellow.offset),
        TextColorBlue(30 + Color.Blue.offset),
        TextColorMagenta(30 + Color.Magenta.offset),
        TextColorCyan(30 + Color.Cyan.offset),
        TextColorWhite(30 + Color.White.offset),
        DefaultTextColor(39),
        BackgroundColorBlack(40 + Color.Black.offset),
        BackgroundColorRed(40 + Color.Red.offset),
        BackgroundColorGreen(40 + Color.Green.offset),
        BackgroundColorYellow(40 + Color.Yellow.offset),
        BackgroundColorBlue(40 + Color.Blue.offset),
        BackgroundColorMagenta(40 + Color.Magenta.offset),
        BackgroundColorCyan(40 + Color.Cyan.offset),
        BackgroundColorWhite(40 + Color.White.offset),
        DefaultBackgroundColor(49),
        TextColorHiBlack(90 + Color.Black.offset),
        TextColorHiRed(90 + Color.Red.offset),
        TextColorHiGreen(90 + Color.Green.offset),
        TextColorHiYellow(90 + Color.Yellow.offset),
        TextColorHiBlue(90 + Color.Blue.offset),
        TextColorHiMagenta(90 + Color.Magenta.offset),
        TextColorHiCyan(90 + Color.Cyan.offset),
        TextColorHiWhite(90 + Color.White.offset),
        BackgroundColorHiBlack(100 + Color.Black.offset),
        BackgroundColorHiRed(100 + Color.Red.offset),
        BackgroundColorHiGreen(100 + Color.Green.offset),
        BackgroundColorHiYellow(100 + Color.Yellow.offset),
        BackgroundColorHiBlue(100 + Color.Blue.offset),
        BackgroundColorHiMagenta(100 + Color.Magenta.offset),
        BackgroundColorHiCyan(100 + Color.Cyan.offset),
        BackgroundColorHiWhite(100 + Color.White.offset),
        ;
        // @formatter:on

        private final int code;

        private Attribute(int code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return "\u001B[" + code + "m";
        }

        public String and(Attribute other) {
            return "\u001B[" + code + ";" + other.code + "m";
        }
    }
}

/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import java.util.Arrays;

/**
 * <h1>Unicode Locale Data Markup Language (LDML)</h1>
 * <p>
 * Date Format Patterns
 * <ul>
 * <li>Date Field Symbol Table
 * </ul>
 * <p>
 * Version: 29
 * 
 * @see <a href="http://unicode.org/reports/tr35/tr35-dates.html#Date_Format_Patterns">Date&nbsp;Format&nbsp;Patterns
 *      </a>
 * @see <a href="http://unicode.org/reports/tr35/tr35-dates.html#Date_Field_Symbol_Table">Date&nbsp;Field&nbsp;Symbol&
 *      nbsp;Table</a>
 */
final class DateFieldSymbolTable {
    private DateFieldSymbolTable() {
    }

    static final class Skeleton {
        private final char[] symbols;
        private final FieldWeight[] fieldWeights;

        private Skeleton(char[] symbols, FieldWeight[] weights) {
            this.symbols = symbols;
            this.fieldWeights = weights;
        }

        /**
         * Parses a skeleton pattern and returns the corresponding skeleton object.
         * 
         * @param skeleton
         *            the skeleton pattern
         * @return the skeleton object for the skeleton
         * 
         * @see <a href="http://unicode.org/reports/tr35/tr35-dates.html#availableFormats_appendItems">Date Format
         *      Patterns</a>
         */
        public static Skeleton fromSkeleton(String skeleton) {
            char[] symbols = new char[DateField.LENGTH];
            FieldWeight[] weights = new FieldWeight[DateField.LENGTH];
            for (int i = 0, len = skeleton.length(); i < len;) {
                char sym = skeleton.charAt(i++);
                assert ('A' <= sym && sym <= 'Z') || ('a' <= sym && sym <= 'z');
                int length = 1;
                for (; i < len && skeleton.charAt(i) == sym; ++i) {
                    length += 1;
                }
                DateField field = DateField.forSymbol(sym);
                FieldWeight weight = field.getWeight(sym, length);
                int index = field.ordinal();
                if (symbols[index] != 0) {
                    throw new IllegalArgumentException();
                }
                symbols[index] = sym;
                weights[index] = weight;
            }
            return new Skeleton(symbols, weights);
        }

        /**
         * Parses a format pattern and returns the corresponding skeleton.
         * 
         * @param pattern
         *            the format pattern
         * @return the skeleton for the pattern
         * 
         * @see <a href="http://unicode.org/reports/tr35/tr35-dates.html#Date_Format_Patterns">Date Format Patterns</a>
         */
        public static Skeleton fromPattern(String pattern) {
            char[] symbols = new char[DateField.LENGTH];
            FieldWeight[] weights = new FieldWeight[DateField.LENGTH];
            boolean quote = false;
            for (int i = 0, len = pattern.length(); i < len;) {
                char sym = pattern.charAt(i++);
                if (sym == '\'') {
                    if (i < len && pattern.charAt(i) == '\'') {
                        i += 1;
                    } else {
                        quote = !quote;
                    }
                    continue;
                }
                if (quote || !(('A' <= sym && sym <= 'Z') || ('a' <= sym && sym <= 'z'))) {
                    continue;
                }
                int length = 1;
                for (; i < len && pattern.charAt(i) == sym; ++i) {
                    length += 1;
                }
                DateField field = DateField.forSymbol(sym);
                FieldWeight weight = field.getWeight(sym, length);
                int index = field.ordinal();
                if (symbols[index] != 0) {
                    throw new IllegalArgumentException();
                }
                symbols[index] = sym;
                weights[index] = weight;
            }
            return new Skeleton(symbols, weights);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != Skeleton.class) {
                return false;
            }
            Skeleton other = (Skeleton) obj;
            return Arrays.equals(symbols, other.symbols) && Arrays.equals(fieldWeights, other.fieldWeights);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(fieldWeights);
            result = prime * result + Arrays.hashCode(symbols);
            return result;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (DateField df : DateField.values()) {
                int index = df.ordinal();
                df.append(sb, fieldWeights[index], symbols[index]);
            }
            return sb.toString();
        }

        public char getSymbol(DateField field) {
            return symbols[field.ordinal()];
        }

        public FieldWeight getWeight(DateField field) {
            return fieldWeights[field.ordinal()];
        }

        public boolean has(DateField field) {
            return getWeight(field) != null;
        }

        public boolean isSingleton() {
            boolean found = false;
            for (int i = 0; i < DateField.LENGTH; ++i) {
                if (fieldWeights[i] != null) {
                    if (found != false) {
                        return false;
                    }
                    found = true;
                }
            }
            return found;
        }

        public boolean isHour12() {
            char hour = getSymbol(DateField.Hour);
            return (hour == 'h' || hour == 'K');
        }

        public String hourCycle() {
            char hour = getSymbol(DateField.Hour);
            switch (hour) {
            case 'h':
                return "h12";
            case 'H':
                return "h23";
            case 'k':
                return "h24";
            case 'K':
                return "h11";
            default:
                throw new AssertionError();
            }
        }

        public Skeleton hour12() {
            assert has(DateField.Hour) && !isHour12();
            int hourIndex = DateField.Hour.ordinal();
            char[] symbols = this.symbols.clone();
            symbols[hourIndex] = (char) (symbols[hourIndex] ^ 0x20);
            return new Skeleton(symbols, fieldWeights);
        }

        public Skeleton hour24() {
            assert has(DateField.Hour) && isHour12();
            int hourIndex = DateField.Hour.ordinal();
            char[] symbols = this.symbols.clone();
            symbols[hourIndex] = (char) (symbols[hourIndex] ^ 0x20);
            return new Skeleton(symbols, fieldWeights);
        }

        public boolean isDate() {
            if (has(DateField.Year) || has(DateField.Month) || has(DateField.Day) || has(DateField.Era)) {
                return true;
            }
            return has(DateField.Weekday) && !isTime();
        }

        public boolean isTime() {
            return has(DateField.Hour) || has(DateField.Minute) || has(DateField.Second) || has(DateField.Timezone);
        }
    }

    enum FieldWeight {
        TwoDigit(1, 2), Numeric(2, 1), Narrow(3, 5), Short(4, 3), Long(5, 4);

        // relative index per abstract operation BasicFormatMatcher, step 11.c.vii.1
        private final int index;
        // length in output string
        private final int length;

        private FieldWeight(int index, int length) {
            this.index = index;
            this.length = length;
        }

        public int index() {
            return index;
        }

        public int length() {
            return length;
        }

        @Override
        public String toString() {
            switch (this) {
            case TwoDigit:
                return "2-digit";
            case Numeric:
                return "numeric";
            case Narrow:
                return "narrow";
            case Short:
                return "short";
            case Long:
                return "long";
            default:
                throw new AssertionError();
            }
        }

        public static FieldWeight forName(String name) {
            if (name == null) {
                return null;
            }
            switch (name) {
            case "numeric":
                return Numeric;
            case "2-digit":
                return TwoDigit;
            case "short":
                return Short;
            case "long":
                return Long;
            case "narrow":
                return Narrow;
            default:
                throw new IllegalArgumentException(name);
            }
        }
    }

    enum DateField {
        Era('G') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                assert symbol == 'G';
                if (symbol == 'G') {
                    if (count >= 1 && count <= 3) {
                        return FieldWeight.Short;
                    }
                    if (count == 4) {
                        return FieldWeight.Long;
                    }
                    if (count == 5) {
                        return FieldWeight.Narrow;
                    }
                }
                throw new IllegalArgumentException();
            }
        },
        Year('y') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                if (symbol == 'U') {
                    if (count >= 1 && count <= 3) {
                        return FieldWeight.Short;
                    }
                    if (count == 4) {
                        return FieldWeight.Long;
                    }
                    if (count == 5) {
                        return FieldWeight.Narrow;
                    }
                } else if (symbol == 'y' || symbol == 'Y' || symbol == 'u' || symbol == 'r') {
                    if (count == 2) {
                        return FieldWeight.TwoDigit;
                    }
                    if (count >= 1) {
                        return FieldWeight.Numeric;
                    }
                }
                throw new IllegalArgumentException();
            }
        },
        Quarter('Q') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                if (symbol == 'Q' || symbol == 'q') {
                    if (count >= 1 && count <= 2) {
                        return FieldWeight.Numeric;
                    }
                    if (count == 3) {
                        return FieldWeight.Short;
                    }
                    if (count == 4) {
                        return FieldWeight.Long;
                    }
                    if (count == 5) {
                        return FieldWeight.Narrow;
                    }
                }
                throw new IllegalArgumentException();
            }
        },
        Month('M') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                if (symbol == 'M' || symbol == 'L') {
                    if (count == 1) {
                        return FieldWeight.Numeric;
                    }
                    if (count == 2) {
                        return FieldWeight.TwoDigit;
                    }
                    if (count == 3) {
                        return FieldWeight.Short;
                    }
                    if (count == 4) {
                        return FieldWeight.Long;
                    }
                    if (count == 5) {
                        return FieldWeight.Narrow;
                    }
                }
                throw new IllegalArgumentException();
            }
        },
        Week('w') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                if (symbol == 'w') {
                    if (count == 1) {
                        return FieldWeight.Numeric;
                    }
                    if (count == 2) {
                        return FieldWeight.TwoDigit;
                    }
                } else if (symbol == 'W') {
                    if (count == 1) {
                        return FieldWeight.Numeric;
                    }
                }
                throw new IllegalArgumentException();
            }
        },
        Day('d') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                if (symbol == 'd') {
                    if (count == 1) {
                        return FieldWeight.Numeric;
                    }
                    if (count == 2) {
                        return FieldWeight.TwoDigit;
                    }
                } else if (symbol == 'D') {
                    if (count >= 1 && count <= 3) {
                        return FieldWeight.Numeric;
                    }
                } else if (symbol == 'F') {
                    if (count == 1) {
                        return FieldWeight.Numeric;
                    }
                } else if (symbol == 'g') {
                    if (count >= 1) {
                        return FieldWeight.Numeric;
                    }
                }
                throw new IllegalArgumentException();
            }
        },
        Weekday('E') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                if (symbol == 'E') {
                    if (count >= 1 && count <= 3) {
                        return FieldWeight.Short;
                    }
                    if (count == 4) {
                        return FieldWeight.Long;
                    }
                    if (count >= 5 && count <= 6) {
                        return FieldWeight.Narrow;
                    }
                } else if (symbol == 'e') {
                    if (count >= 1 && count <= 2) {
                        return FieldWeight.Numeric;
                    }
                    if (count == 3) {
                        return FieldWeight.Short;
                    }
                    if (count == 4) {
                        return FieldWeight.Long;
                    }
                    if (count >= 5 && count <= 6) {
                        return FieldWeight.Narrow;
                    }
                } else if (symbol == 'c') {
                    if (count == 1) {
                        return FieldWeight.Numeric;
                    }
                    if (count == 3) {
                        return FieldWeight.Short;
                    }
                    if (count == 4) {
                        return FieldWeight.Long;
                    }
                    if (count >= 5 && count <= 6) {
                        return FieldWeight.Narrow;
                    }
                }
                throw new IllegalArgumentException();
            }
        },
        Period('a') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                if (symbol == 'a' || symbol == 'b' || symbol == 'B') {
                    if (count >= 1 && count <= 3) {
                        return FieldWeight.Short;
                    }
                    if (count == 4) {
                        return FieldWeight.Long;
                    }
                    if (count == 5) {
                        return FieldWeight.Narrow;
                    }
                }
                throw new IllegalArgumentException();
            }
        },
        Hour('j') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                if (symbol == 'h' || symbol == 'H' || symbol == 'K' || symbol == 'k') {
                    if (count == 1) {
                        return FieldWeight.Numeric;
                    }
                    if (count == 2) {
                        return FieldWeight.TwoDigit;
                    }
                }
                throw new IllegalArgumentException();
            }

            @Override
            public void append(StringBuilder sb, FieldWeight weight, Boolean hour12) {
                append(sb, weight, hour12 != null ? hour12 ? 'h' : 'H' : symbol);
            }
        },
        Minute('m') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                if (symbol == 'm') {
                    if (count == 1) {
                        return FieldWeight.Numeric;
                    }
                    if (count == 2) {
                        return FieldWeight.TwoDigit;
                    }
                }
                throw new IllegalArgumentException();
            }
        },
        Second('s') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                if (symbol == 's') {
                    if (count == 1) {
                        return FieldWeight.Numeric;
                    }
                    if (count == 2) {
                        return FieldWeight.TwoDigit;
                    }
                } else if (symbol == 'S' || symbol == 'A') {
                    if (count >= 1) {
                        return FieldWeight.Numeric;
                    }
                }
                throw new IllegalArgumentException();
            }
        },
        Timezone('z') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                if (symbol == 'z') {
                    if (count >= 1 && count <= 3) {
                        return FieldWeight.Short;
                    }
                    if (count == 4) {
                        return FieldWeight.Long;
                    }
                } else if (symbol == 'Z') {
                    if (count >= 1 && count <= 3) {
                        return FieldWeight.Short;
                    }
                    if (count >= 4 && count <= 5) {
                        return FieldWeight.Long;
                    }
                } else if (symbol == 'O' || symbol == 'v') {
                    if (count == 1) {
                        return FieldWeight.Short;
                    }
                    if (count == 4) {
                        return FieldWeight.Long;
                    }
                } else if (symbol == 'V') {
                    if (count >= 1 && count <= 3) {
                        return FieldWeight.Short;
                    }
                    if (count == 4) {
                        return FieldWeight.Long;
                    }
                } else if (symbol == 'X' || symbol == 'x') {
                    if (count >= 1 && count <= 3) {
                        return FieldWeight.Short;
                    }
                    if (count >= 4 && count <= 5) {
                        return FieldWeight.Long;
                    }
                }
                throw new IllegalArgumentException();
            }
        };

        static final int LENGTH = values().length;

        protected final char symbol;

        private DateField(char symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            switch (this) {
            case Era:
                return "era";
            case Year:
                return "year";
            case Quarter:
                return "quarter";
            case Month:
                return "month";
            case Week:
                return "week";
            case Day:
                return "day";
            case Weekday:
                return "weekday";
            case Period:
                return "period";
            case Hour:
                return "hour";
            case Minute:
                return "minute";
            case Second:
                return "second";
            case Timezone:
                return "timeZoneName";
            default:
                throw new AssertionError();
            }
        }

        final void append(StringBuilder sb, FieldWeight weight, char c) {
            if (weight != null) {
                int n = weight.length();
                sb.ensureCapacity(sb.length() + n);
                for (int i = 0; i < n; ++i) {
                    sb.append(c);
                }
            }
        }

        public abstract FieldWeight getWeight(char symbol, int count);

        public void append(StringBuilder sb, FieldWeight weight, Boolean option) {
            throw new UnsupportedOperationException();
        }

        public final void append(StringBuilder sb, FieldWeight weight) {
            append(sb, weight, symbol);
        }

        public static DateField forSymbol(char symbol) {
            switch (symbol) {
            case 'G':
                return Era;
            case 'y':
            case 'Y':
            case 'u':
            case 'U':
            case 'r':
                return Year;
            case 'Q':
            case 'q':
                return Quarter;
            case 'M':
            case 'L':
                return Month;
            case 'w':
            case 'W':
                return Week;
            case 'd':
            case 'D':
            case 'F':
            case 'g':
                return Day;
            case 'E':
            case 'e':
            case 'c':
                return Weekday;
            case 'a':
            case 'b':
            case 'B':
                return Period;
            case 'h':
            case 'H':
            case 'K':
            case 'k':
                return Hour;
            case 'm':
                return Minute;
            case 's':
            case 'S':
            case 'A':
                return Second;
            case 'z':
            case 'Z':
            case 'O':
            case 'v':
            case 'V':
            case 'X':
            case 'x':
                return Timezone;
            case 'l':
            case 'j':
            case 'J':
            case 'C':
            default:
                throw new IllegalArgumentException(Character.toString(symbol));
            }
        }
    }
}

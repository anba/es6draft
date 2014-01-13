/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

/**
 * @see <a
 *      href="http://unicode.org/reports/tr35/tr35-dates.html#Date_Field_Symbol_Table">Date_Field_Symbol_Table</a>
 */
final class DateFieldSymbolTable {
    private DateFieldSymbolTable() {
    }

    static final class Skeleton {
        private final char[] symbols;
        private final FieldWeight[] fieldWeights;
        private final boolean hour12;

        public Skeleton(String skeleton) {
            char symbols[] = new char[DateField.LENGTH];
            FieldWeight weights[] = new FieldWeight[DateField.LENGTH];
            boolean hour12 = false;
            for (int i = 0, len = skeleton.length(); i < len;) {
                char sym = skeleton.charAt(i++);
                int length = 1;
                for (; i < len && skeleton.charAt(i) == sym; ++i) {
                    length += 1;
                }
                DateField field = DateField.forSymbol(sym);
                FieldWeight weight = field.getWeight(sym, length);
                assert weight != FieldWeight.Invalid;
                symbols[field.ordinal()] = sym;
                weights[field.ordinal()] = weight;
                if (field == DateField.Hour) {
                    hour12 = (sym == 'h' || sym == 'K');
                }
            }
            this.symbols = symbols;
            this.fieldWeights = weights;
            this.hour12 = hour12;
        }

        public boolean has(DateField field) {
            return fieldWeights[field.ordinal()] != null;
        }

        public char getSymbol(DateField field) {
            return symbols[field.ordinal()];
        }

        public FieldWeight getWeight(DateField field) {
            return fieldWeights[field.ordinal()];
        }

        public boolean isHour12() {
            return hour12;
        }

        public boolean isDate() {
            return has(DateField.Year) || has(DateField.Month) || has(DateField.Day);
        }

        public boolean isTime() {
            return has(DateField.Hour) || has(DateField.Minute) || has(DateField.Second);
        }
    }

    enum FieldWeight {
        TwoDigit(1, 2), Numeric(2, 1), Narrow(3, 5), Short(4, 3), Long(5, 4), Invalid(-1, -1);

        // relative weight per abstract operation BasicFormatMatcher, step 11.c.vii.1
        private final int weight;
        // length in output string
        private final int length;

        private FieldWeight(int weight, int length) {
            this.weight = weight;
            this.length = length;
        }

        public int length() {
            return length;
        }

        public int weight() {
            return weight;
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
            case Invalid:
                return "<invalid>";
            default:
                throw new IllegalStateException();
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
                if (count >= 1 && count <= 3) {
                    return FieldWeight.Short;
                }
                if (count == 4) {
                    return FieldWeight.Long;
                }
                if (count == 5) {
                    return FieldWeight.Narrow;
                }
                return FieldWeight.Invalid;
            }
        },
        Year('y') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                assert symbol == 'y' || symbol == 'Y' || symbol == 'u' || symbol == 'U';
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
                } else {
                    if (count == 2) {
                        return FieldWeight.TwoDigit;
                    }
                    if (count >= 1) {
                        return FieldWeight.Numeric;
                    }
                }
                return FieldWeight.Invalid;
            }
        },
        Quarter('Q') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                assert symbol == 'Q' || symbol == 'q';
                if (count >= 1 && count <= 2) {
                    return FieldWeight.Numeric;
                }
                if (count == 3) {
                    return FieldWeight.Short;
                }
                if (count == 4) {
                    return FieldWeight.Long;
                }
                return FieldWeight.Invalid;
            }
        },
        Month('M') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                assert symbol == 'M' || symbol == 'L';
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
                return FieldWeight.Invalid;
            }
        },
        Week('w') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                assert symbol == 'w' || symbol == 'W';
                if (symbol == 'w') {
                    if (count >= 1 && count <= 2) {
                        return FieldWeight.Numeric;
                    }
                } else {
                    if (count == 1) {
                        return FieldWeight.Numeric;
                    }
                }
                return FieldWeight.Invalid;
            }
        },
        Day('d') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                assert symbol == 'd' || symbol == 'D' || symbol == 'F' || symbol == 'g';
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
                } else {
                    if (count >= 1) {
                        return FieldWeight.Numeric;
                    }
                }
                return FieldWeight.Invalid;
            }
        },
        Weekday('E') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                assert symbol == 'E' || symbol == 'e' || symbol == 'c';
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
                } else {
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
                return FieldWeight.Invalid;
            }
        },
        Period('a') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                assert symbol == 'a';
                if (count == 1) {
                    return FieldWeight.Short;
                }
                return FieldWeight.Invalid;
            }
        },
        Hour('j') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                assert symbol == 'h' || symbol == 'H' || symbol == 'K' || symbol == 'k';
                if (count == 1) {
                    return FieldWeight.Numeric;
                }
                if (count == 2) {
                    return FieldWeight.TwoDigit;
                }
                return FieldWeight.Invalid;
            }

            @Override
            public void append(StringBuilder sb, String weight, Boolean hour12) {
                char c = (hour12 != null ? hour12 ? 'h' : 'H' : symbol);
                FieldWeight fw = FieldWeight.forName(weight);
                for (int i = (fw != null ? fw.length() : 0); i != 0; --i) {
                    sb.append(c);
                }
            }
        },
        Minute('m') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                assert symbol == 'm';
                if (count == 1) {
                    return FieldWeight.Numeric;
                }
                if (count == 2) {
                    return FieldWeight.TwoDigit;
                }
                return FieldWeight.Invalid;
            }
        },
        Second('s') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                assert symbol == 's' || symbol == 'S' || symbol == 'A';
                if (symbol == 's') {
                    if (count == 1) {
                        return FieldWeight.Numeric;
                    }
                    if (count == 2) {
                        return FieldWeight.TwoDigit;
                    }
                } else {
                    if (count >= 1) {
                        return FieldWeight.Numeric;
                    }
                }
                return FieldWeight.Invalid;
            }
        },
        Timezone('z') {
            @Override
            public FieldWeight getWeight(char symbol, int count) {
                assert symbol == 'z' || symbol == 'Z' || symbol == 'O' || symbol == 'v'
                        || symbol == 'V' || symbol == 'X' || symbol == 'x';
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
                } else {
                    if (count >= 1 && count <= 3) {
                        return FieldWeight.Short;
                    }
                    if (count >= 4 && count <= 5) {
                        return FieldWeight.Long;
                    }
                }
                return FieldWeight.Invalid;
            }
        };

        public static final int LENGTH = values().length;

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
                throw new IllegalStateException();
            }
        }

        public abstract FieldWeight getWeight(char symbol, int count);

        public void append(StringBuilder sb, String weight, Boolean option) {
            throw new UnsupportedOperationException();
        }

        public void append(StringBuilder sb, String weight) {
            FieldWeight fw = FieldWeight.forName(weight);
            for (int i = (fw != null ? fw.length() : 0); i != 0; --i) {
                sb.append(symbol);
            }
        }

        public static DateField forSymbol(char symbol) {
            switch (symbol) {
            case 'G':
                return Era;
            case 'y':
            case 'Y':
            case 'u':
            case 'U':
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
            default:
                throw new IllegalArgumentException(Character.toString(symbol));
            }
        }
    }
}

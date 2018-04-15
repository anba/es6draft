/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.CreateIterResultObject;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToIndex;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.ibm.icu.text.BreakIterator;

/**
 * <h1>Segmenter Objects</h1><br>
 * <h2>Segment Iterators</h2>
 * <ul>
 * <li>%SegmentIteratorPrototype%
 * </ul>
 */
public final class SegmentIteratorPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Segment Iterator prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public SegmentIteratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * CreateSegmentIterator ( segmenter, string )
     * 
     * @param cx
     *            the execution context
     * @param segmenter
     *            the segmenter object
     * @param string
     *            the string value
     * @return the new segment iterator
     */
    public static OrdinaryObject CreateSegmentIterator(ExecutionContext cx, SegmenterObject segmenter, String string) {
        /* steps 1-5 */
        return new SegmentIteratorObject(cx.getRealm(), segmenter, string,
                cx.getIntrinsic(Intrinsics.Intl_SegmentIteratorPrototype));
    }

    private enum Direction {
        Forwards, Backwards
    }

    /**
     * Constants for line break rule limits.
     * 
     * @see ICU4C ULineBreakTag in common/unicode/ubrk.h
     */
    private enum LineBreakTag {
        ;
        static final int SOFT = 0;
        static final int SOFT_LIMIT = 100;
        static final int HARD = 100;
        static final int HARD_LIMIT = 200;
    }

    /**
     * Constants for sentence break rule limits.
     * 
     * @see ICU4C USentenceBreakTag in common/unicode/ubrk.h
     */
    private enum SentenceBreakTag {
        ;
        static final int TERM = 0;
        static final int TERM_LIMIT = 100;
        static final int SEP = 100;
        static final int SEP_LIMIT = 200;
    }

    /**
     * AdvanceSegmentIterator ( iterator, direction )
     * 
     * @param iterator
     *            the segment iterator object
     * @param direction
     *            the direction kind
     * @return {@code true} if iterator has hit the end of the string, otherwise {@code false}
     */
    public static boolean AdvanceSegmentIterator(SegmentIteratorObject iterator, Direction direction) {
        /* step 1 */
        BreakIterator breakIterator = iterator.getBreakIterator();
        /* step 2 */
        String string = iterator.getString();
        /* step 3 */
        int position = iterator.getPosition();
        /* step 4 */
        if ((direction == Direction.Forwards && position >= string.length())
                || (direction == Direction.Backwards && position <= 0)) {
            return true;
        }
        /* step 5 */
        int result;
        if (direction == Direction.Forwards) {
            result = breakIterator.following(position);
        } else {
            assert direction == Direction.Backwards;
            result = breakIterator.preceding(position);
        }
        /* step 6 */
        String breakType = null;
        if (result != BreakIterator.DONE) {
            switch (iterator.getGranularity()) {
            case "grapheme":
                // Always undefined.
                break;
            case "word": {
                int ruleStatus = breakIterator.getRuleStatus();
                if (BreakIterator.WORD_NONE <= ruleStatus && ruleStatus < BreakIterator.WORD_NONE_LIMIT) {
                    breakType = "none";
                } else if (BreakIterator.WORD_NUMBER <= ruleStatus && ruleStatus < BreakIterator.WORD_NUMBER_LIMIT) {
                    breakType = "word";
                } else if (BreakIterator.WORD_LETTER <= ruleStatus && ruleStatus < BreakIterator.WORD_LETTER_LIMIT) {
                    breakType = "word";
                } else if (BreakIterator.WORD_KANA <= ruleStatus && ruleStatus < BreakIterator.WORD_KANA_LIMIT) {
                    breakType = "word";
                } else if (BreakIterator.WORD_IDEO <= ruleStatus && ruleStatus < BreakIterator.WORD_IDEO_LIMIT) {
                    breakType = "word";
                }
                break;
            }
            case "line": {
                int ruleStatus = breakIterator.getRuleStatus();
                if (LineBreakTag.SOFT <= ruleStatus && ruleStatus < LineBreakTag.SOFT_LIMIT) {
                    breakType = "soft";
                } else if (LineBreakTag.HARD <= ruleStatus && ruleStatus < LineBreakTag.HARD_LIMIT) {
                    breakType = "hard";
                }
                break;
            }
            case "sentence": {
                int ruleStatus = breakIterator.getRuleStatus();
                if (SentenceBreakTag.TERM <= ruleStatus && ruleStatus < SentenceBreakTag.TERM_LIMIT) {
                    breakType = "term";
                } else if (SentenceBreakTag.SEP <= ruleStatus && ruleStatus < SentenceBreakTag.SEP_LIMIT) {
                    breakType = "sep";
                }
                break;
            }
            default:
                throw new AssertionError();
            }
        }
        iterator.setBreakType(breakType);
        /* step 7 */
        iterator.setPosition(breakIterator.current());
        /* step 8 */
        return false;
    }

    /**
     * %SegmentIteratorPrototype%
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.IteratorPrototype;

        /**
         * %SegmentIteratorPrototype%.next( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the next iterator result object
         */
        @Function(name = "next", arity = 0)
        public static Object next(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            if (!(thisValue instanceof SegmentIteratorObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleThis, "%SegmentIteratorPrototype%.next",
                        Type.of(thisValue).toString());
            }
            SegmentIteratorObject iterator = (SegmentIteratorObject) thisValue;
            /* step 3 */
            int previousPosition = iterator.getPosition();
            /* step 4 */
            boolean done = AdvanceSegmentIterator(iterator, Direction.Forwards);
            /* step 5 */
            if (done) {
                return CreateIterResultObject(cx, UNDEFINED, true);
            }
            /* step 6 */
            int newPosition = iterator.getPosition();
            /* step 7 */
            String string = iterator.getString();
            /* step 8 */
            String segment = string.substring(previousPosition, newPosition);
            /* step 9 */
            Object breakType = iterator.getBreakType() != null ? iterator.getBreakType() : UNDEFINED;
            /* step 10 */
            OrdinaryObject result = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            /* step 11 */
            CreateDataProperty(cx, result, "segment", segment);
            /* step 12 */
            CreateDataProperty(cx, result, "breakType", breakType);
            /* step 13 */
            return CreateIterResultObject(cx, result, false);
        }

        /**
         * %SegmentIteratorPrototype%.following( [ from ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param from
         *            the optional string start index
         * @return the next iterator result object
         */
        @Function(name = "following", arity = 0)
        public static Object following(ExecutionContext cx, Object thisValue, Object from) {
            /* steps 1-2 */
            if (!(thisValue instanceof SegmentIteratorObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleThis, "%SegmentIteratorPrototype%.following",
                        Type.of(thisValue).toString());
            }
            SegmentIteratorObject iterator = (SegmentIteratorObject) thisValue;
            /* step 3 */
            if (!Type.isUndefined(from)) {
                /* step 3.a */
                long fromPos = ToIndex(cx, from);
                /* step 3.b */
                if (fromPos >= iterator.getString().length()) {
                    throw newRangeError(cx, Messages.Key.IntlInvalidStringIndex);
                }
                /* step 3.c */
                iterator.setPosition((int) fromPos);
            }
            /* step 4 */
            return AdvanceSegmentIterator(iterator, Direction.Forwards);
        }

        /**
         * %SegmentIteratorPrototype%.preceding( [ from ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param from
         *            the optional string start index
         * @return the next iterator result object
         */
        @Function(name = "preceding", arity = 0)
        public static Object preceding(ExecutionContext cx, Object thisValue, Object from) {
            /* steps 1-2 */
            if (!(thisValue instanceof SegmentIteratorObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleThis, "%SegmentIteratorPrototype%.preceding",
                        Type.of(thisValue).toString());
            }
            SegmentIteratorObject iterator = (SegmentIteratorObject) thisValue;
            /* step 3 */
            if (!Type.isUndefined(from)) {
                /* step 3.a */
                long fromPos = ToIndex(cx, from);
                /* step 3.b */
                if (fromPos > iterator.getString().length() || fromPos == 0) {
                    throw newRangeError(cx, Messages.Key.IntlInvalidStringIndex);
                }
                /* step 3.c */
                iterator.setPosition((int) fromPos);
            }
            /* step 4 */
            return AdvanceSegmentIterator(iterator, Direction.Backwards);
        }

        /**
         * get %SegmentIteratorPrototype%.position
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the current position
         */
        @Accessor(name = "position", type = Accessor.Type.Getter)
        public static Object position(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            if (!(thisValue instanceof SegmentIteratorObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleThis, "get %SegmentIteratorPrototype%.position",
                        Type.of(thisValue).toString());
            }
            SegmentIteratorObject iterator = (SegmentIteratorObject) thisValue;
            /* step 3 */
            return iterator.getPosition();
        }

        /**
         * get %SegmentIteratorPrototype%.breakType
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the current position
         */
        @Accessor(name = "breakType", type = Accessor.Type.Getter)
        public static Object breakType(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            if (!(thisValue instanceof SegmentIteratorObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleThis, "get %SegmentIteratorPrototype%.breakType",
                        Type.of(thisValue).toString());
            }
            SegmentIteratorObject iterator = (SegmentIteratorObject) thisValue;
            /* step 3 */
            return iterator.getBreakType() != null ? iterator.getBreakType() : UNDEFINED;
        }
    }
}

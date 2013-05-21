/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.util.Arrays;
import java.util.Collection;

/**
 * Java8 compatibility shim
 * 
 */
public final class Functional {
    private Functional() {
    }

    private static final Predicate<Object> TRUE = new Predicate<Object>() {
        @Override
        public boolean test(Object t) {
            return true;
        }
    };

    /**
     * The predicate which always returns {@code true}
     */
    @SuppressWarnings("unchecked")
    private static <T> Predicate<T> alwaysTrue() {
        return (Predicate<T>) TRUE;
    }

    /**
     * Predicate interface
     */
    public static interface Predicate<T> {
        boolean test(T t);
    }

    /**
     * Single-arity function interface
     */
    public static interface Function<T, R> {
        R apply(T t);
    }

    /**
     * Two-arity function interface
     */
    public static interface BiFunction<T, U, R> {
        R apply(T t, U u);
    }

    /**
     * Returns a new {@link Iterable} for the input rest-argument
     */
    @SafeVarargs
    public static <T> Iterable<T> iterable(T... rest) {
        return Arrays.asList(rest);
    }

    /**
     * Applies the mapper on the input (lazy operation)
     */
    public static <T, U> Iterable<U> map(Iterable<T> base, Function<? super T, ? extends U> mapper) {
        return new $FilterMap<>(base, Functional.<T> alwaysTrue(), mapper);
    }

    /**
     * Filters and maps the input (lazy operation)
     */
    public static <T, U> Iterable<U> filterMap(Iterable<T> base, Predicate<? super T> predicate,
            Function<? super T, ? extends U> mapper) {
        return new $FilterMap<>(base, predicate, mapper);
    }

    /**
     * Adds all entries into the specified collection
     */
    public static <C extends Collection<? super T>, T> C intoCollection(Iterable<T> itr,
            C collection) {
        for (T value : itr) {
            collection.add(value);
        }
        return collection;
    }

    /**
     * {@link FilterMap} based on {@link Predicate} and {@link Mapper}
     */
    private static final class $FilterMap<T, U> extends FilterMap<T, U> {
        private final Predicate<? super T> predicate;
        private final Function<? super T, ? extends U> mapper;

        public $FilterMap(Iterable<T> base, Predicate<? super T> predicate,
                Function<? super T, ? extends U> mapper) {
            super(base);
            this.predicate = predicate;
            this.mapper = mapper;
        }

        @Override
        protected boolean filter(T value) {
            return predicate.test(value);
        }

        @Override
        protected U map(T value) {
            return mapper.apply(value);
        }
    }
}

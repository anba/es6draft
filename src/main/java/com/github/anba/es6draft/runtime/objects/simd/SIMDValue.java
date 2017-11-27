/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.simd;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.github.anba.es6draft.runtime.AbstractOperations;

/**
 * The SIMD value.
 */
public final class SIMDValue {
    /** [[SIMDTypeDescriptor]] */
    private final SIMDType type;

    /** [[SIMDElements]] */
    private final Object elements;

    /**
     * Creates a new SIMD value.
     * 
     * @param type
     *            the SIMD type descriptor
     * @param elements
     *            the SIMD elements
     */
    SIMDValue(SIMDType type, Object elements) {
        assert elements.getClass() == classForSIMD(type);
        this.type = type;
        this.elements = elements;
    }

    private static Class<?> classForSIMD(SIMDType type) {
        switch (type) {
        case Float64x2:
        case Float32x4:
            return double[].class;
        case Int32x4:
        case Int16x8:
        case Int8x16:
        case Uint32x4:
        case Uint16x8:
        case Uint8x16:
            return int[].class;
        case Bool64x2:
        case Bool32x4:
        case Bool16x8:
        case Bool8x16:
            return boolean[].class;
        default:
            throw new AssertionError();
        }
    }

    /**
     * [[SIMDTypeDescriptor]]
     * 
     * @return the SIMD type descriptor
     */
    public SIMDType getType() {
        return type;
    }

    /**
     * [[SIMDElements]]
     * 
     * @return the SIMD elements
     */
    public Object getElements() {
        return elements;
    }

    /**
     * Returns the SIMD elements as a {@code double[]} array.
     * 
     * @return the SIMD elements
     * @throws ClassCastException
     *             if SIMD type is not a floating point type
     */
    public double[] asDouble() {
        return (double[]) elements;
    }

    /**
     * Returns the SIMD elements as an {@code int[]} array.
     * 
     * @return the SIMD elements
     * @throws ClassCastException
     *             if SIMD type is not an integer type
     */
    public int[] asInt() {
        return (int[]) elements;
    }

    /**
     * Returns the SIMD elements as a {@code boolean[]} array.
     * 
     * @return the SIMD elements
     * @throws ClassCastException
     *             if SIMD type is not a boolean type
     */
    public boolean[] asBoolean() {
        return (boolean[]) elements;
    }

    @Override
    public boolean equals(Object obj) {
        // equals() implements SameValue semantics.
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != SIMDValue.class) {
            return false;
        }
        SIMDValue other = (SIMDValue) obj;
        if (type != other.type) {
            return false;
        }
        if (elements instanceof double[]) {
            return Arrays.equals((double[]) elements, (double[]) other.elements);
        }
        if (elements instanceof int[]) {
            return Arrays.equals((int[]) elements, (int[]) other.elements);
        }
        return Arrays.equals((boolean[]) elements, (boolean[]) other.elements);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + type.hashCode();
        if (elements instanceof double[]) {
            result = prime * result + Arrays.hashCode((double[]) elements);
        } else if (elements instanceof int[]) {
            result = prime * result + Arrays.hashCode((int[]) elements);
        } else {
            result = prime * result + Arrays.hashCode((boolean[]) elements);
        }
        return result;
    }

    @Override
    public String toString() {
        // String representation per SIMD ToString() specification.
        Stream<String> content;
        if (elements instanceof double[]) {
            content = Arrays.stream((double[]) elements).mapToObj(AbstractOperations::ToString);
        } else if (elements instanceof int[]) {
            if (type == SIMDType.Uint32x4) {
                content = Arrays.stream((int[]) elements).mapToLong(Integer::toUnsignedLong)
                        .mapToObj(AbstractOperations::ToString);
            } else {
                content = Arrays.stream((int[]) elements).mapToObj(AbstractOperations::ToString);
            }
        } else {
            content = booleanStream((boolean[]) elements).map(b -> Boolean.toString(b));
        }
        return content.collect(Collectors.joining(", ", "SIMD." + type.name() + "(", ")"));
    }

    private static Stream<Boolean> booleanStream(boolean[] array) {
        Spliterator<Boolean> spliterator = Spliterators.spliterator(new Iterator<Boolean>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                return index < array.length;
            }

            @Override
            public Boolean next() {
                if (index >= array.length) {
                    throw new NoSuchElementException();
                }
                return array[index++];
            }
        }, array.length, Spliterator.ORDERED | Spliterator.IMMUTABLE);
        return StreamSupport.stream(spliterator, false);
    }
}

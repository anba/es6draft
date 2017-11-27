/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.language;

import static com.github.anba.es6draft.runtime.AbstractOperations.GetIterator;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;

import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.stream.IntStream;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.internal.ScriptIterators;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.ArgumentsObject;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * 
 */
public final class ArrayOperations {
    private ArrayOperations() {
    }

    /**
     * 12.14.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
     * <p>
     * 13.2.3.5 Runtime Semantics: IteratorBindingInitialization
     * 
     * @param iterator
     *            the iterator
     * @param cx
     *            the execution context
     * @return the array with the remaining elements from <var>iterator</var>
     */
    public static ArrayObject createRestArray(Iterator<?> iterator, ExecutionContext cx) {
        ArrayObject result = ArrayCreate(cx, 0);
        for (int n = 0; iterator.hasNext();) {
            defineProperty(result, n++, iterator.next());
        }
        return result;
    }

    /**
     * 12.2.4.1 Array Literal
     * <p>
     * 12.2.4.1.2 Runtime Semantics: Array Accumulation
     * <ul>
     * <li>ElementList : Elision<span><sub>opt</sub></span> AssignmentExpression
     * <li>ElementList : ElementList , Elision<span><sub>opt</sub></span> AssignmentExpression
     * </ul>
     * 
     * @param array
     *            the array object
     * @param nextIndex
     *            the array index
     * @param value
     *            the array element value
     */
    public static void defineProperty(ArrayObject array, int nextIndex, Object value) {
        // Inlined: CreateDataProperty(array, ToString(ToUint32(nextIndex)), value);
        array.insert(nextIndex, value);
    }

    /**
     * 12.2.4.1 Array Literal
     * <p>
     * 12.2.4.1.3 Runtime Semantics: Evaluation
     * 
     * @param array
     *            the array object
     * @param length
     *            the array length value
     */
    public static void defineLength(ArrayObject array, int length) {
        // Set(cx, array, "length", length, false);
        array.setLengthUnchecked(length);
    }

    /**
     * 12.2.4.1 Array Literal
     * <p>
     * 12.2.4.1.2 Runtime Semantics: Array Accumulation
     * <ul>
     * <li>SpreadElement : ... AssignmentExpression
     * </ul>
     * 
     * @param array
     *            the array object
     * @param nextIndex
     *            the array index
     * @param spreadObj
     *            the spread element
     * @param cx
     *            the execution context
     * @return the next array index
     */
    public static int spreadElement(ArrayObject array, int nextIndex, Object spreadObj, ExecutionContext cx) {
        if (isSpreadableObject(spreadObj)) {
            int next = spreadElement(array, nextIndex, (OrdinaryObject) spreadObj, cx);
            if (next >= 0) {
                return next;
            }
        } else if (spreadObj instanceof TypedArrayObject) {
            int next = spreadElement(array, nextIndex, (TypedArrayObject) spreadObj, cx);
            if (next >= 0) {
                return next;
            }
        }
        /* steps 1-2 (cf. generated code) */
        /* steps 3-5 */
        PrimitiveIterator.OfInt nextIndexIterator = IntStream.range(nextIndex, Integer.MAX_VALUE).iterator();
        ScriptIterator<?> iterator = GetIterator(cx, spreadObj);
        iterator.forEachRemaining(v -> defineProperty(array, nextIndexIterator.nextInt(), v));
        return nextIndexIterator.nextInt();
    }

    private static boolean isSpreadableObject(Object object) {
        return object instanceof ArrayObject || object instanceof ArgumentsObject
                || object.getClass() == OrdinaryObject.class;
    }

    private static int spreadElement(ArrayObject array, int nextIndex, OrdinaryObject object, ExecutionContext cx) {
        long length = object.getLength();
        long newLength = nextIndex + length;
        if (0 <= length && newLength <= Integer.MAX_VALUE
                && ScriptIterators.isBuiltinArrayIterator(cx, object, length)) {
            array.insertFrom(nextIndex, object, length);
            return (int) newLength;
        }
        return -1;
    }

    private static int spreadElement(ArrayObject array, int nextIndex, TypedArrayObject object, ExecutionContext cx) {
        long length = object.getLength();
        long newLength = nextIndex + length;
        if (0 <= length && newLength <= Integer.MAX_VALUE && ScriptIterators.isBuiltinTypedArrayIterator(cx, object)) {
            array.insertFrom(cx, nextIndex, object);
            return (int) newLength;
        }
        return -1;
    }
}

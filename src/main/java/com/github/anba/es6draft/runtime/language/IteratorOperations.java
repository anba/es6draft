/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.language;

import static com.github.anba.es6draft.runtime.AbstractOperations.GetIterator;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.HashSet;
import java.util.Iterator;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.internal.SimpleIterator;
import com.github.anba.es6draft.runtime.objects.async.iteration.AsyncGeneratorAbstractOperations;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * 
 */
public final class IteratorOperations {
    private IteratorOperations() {
    }

    /**
     * 12.15.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
     * <p>
     * 13.3.3.6 Runtime Semantics: IteratorBindingInitialization
     * 
     * @param iterator
     *            the iterator
     */
    public static void iteratorNextAndIgnore(Iterator<?> iterator) {
        if (iterator.hasNext()) {
            iterator.next();
        }
    }

    /**
     * 12.15.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
     * <p>
     * 13.3.3.6 Runtime Semantics: IteratorBindingInitialization
     * 
     * @param iterator
     *            the iterator
     * @return the next iterator result, or undefined it is already exhausted
     */
    public static Object iteratorNextOrUndefined(Iterator<?> iterator) {
        return iterator.hasNext() ? iterator.next() : UNDEFINED;
    }

    /**
     * 13.7.5 The for-in and for-of Statements
     * <ul>
     * <li>13.7.5.12 Runtime Semantics: ForIn/OfHeadEvaluation ( TDZnames, expr, iterationKind, labelSet)
     * </ul>
     * 
     * @param value
     *            the object to enumerate
     * @param cx
     *            the execution context
     * @return the keys enumerator
     */
    public static Iterator<String> enumerate(Object value, ExecutionContext cx) {
        /* step 6.b */
        ScriptObject obj = ToObject(cx, value);
        /* step 6.c */
        return new EnumeratePropertiesIterator(cx, obj);
    }

    /**
     * 13.7.5.15 EnumerateObjectProperties (O)
     * 
     * @param cx
     *            the execution context
     * @param object
     *            the script object
     * @return the keys enumerator
     */
    public static Iterator<String> EnumerateObjectProperties(ExecutionContext cx, ScriptObject object) {
        return new EnumeratePropertiesIterator(cx, object);
    }

    /**
     * 13.7.5.15 EnumerateObjectProperties (O)
     */
    // FIXME: spec bug - use of term `prototype object` not always correct in EnumerateObjectProperties description.
    // FIXME: spec issue - add note about abrupt completions and forwarding to `for-in` statement code?
    private static final class EnumeratePropertiesIterator extends SimpleIterator<String> {
        private final HashSet<String> visitedKeys = new HashSet<>();
        private final ExecutionContext cx;
        private ScriptObject obj;
        private Iterator<String> keys;

        EnumeratePropertiesIterator(ExecutionContext cx, ScriptObject obj) {
            this.cx = cx;
            this.obj = obj;
            this.keys = obj.ownEnumerablePropertyKeys(cx);
        }

        @Override
        protected String findNext() {
            HashSet<String> visitedKeys = this.visitedKeys;
            ExecutionContext cx = this.cx;
            for (ScriptObject obj = this.obj; obj != null;) {
                for (Iterator<String> keys = this.keys; keys.hasNext();) {
                    String key = keys.next();
                    ScriptObject.Enumerability e = obj.isEnumerableOwnProperty(cx, key);
                    if (e != ScriptObject.Enumerability.Deleted) {
                        if (visitedKeys.add(key) && e == ScriptObject.Enumerability.Enumerable) {
                            return key;
                        }
                    }
                }
                this.obj = obj = obj.getPrototypeOf(cx);
                if (obj != null) {
                    this.keys = obj.ownEnumerablePropertyKeys(cx);
                } else {
                    this.keys = null;
                }
            }
            return null;
        }
    }

    /**
     * 13.7.5 The for-in and for-of Statements
     * <ul>
     * <li>13.7.5.12 Runtime Semantics: ForIn/OfHeadEvaluation ( TDZnames, expr, iterationKind, labelSet)
     * </ul>
     * <p>
     * 12.15.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
     * <p>
     * 13.3.3.6 Runtime Semantics: IteratorBindingInitialization
     * 
     * @param value
     *            the object to iterate
     * @param cx
     *            the execution context
     * @return the object iterator
     */
    public static ScriptIterator<?> iterate(Object value, ExecutionContext cx) {
        /* step 7 */
        return GetIterator(cx, value);
    }

    /**
     * 13.7.5 The for-in and for-of Statements<br>
     * Extension: 'for-await' statement
     * <p>
     * 13.7.5.12 Runtime Semantics: ForIn/OfHeadEvaluation ( TDZnames, expr, iterationKind, labelSet)
     * 
     * @param value
     *            the object to enumerate
     * @param cx
     *            the execution context
     * @return the async iterator
     */
    public static ScriptIterator<?> asyncIterate(Object value, ExecutionContext cx) {
        return AsyncGeneratorAbstractOperations.GetAsyncIterator(cx, value);
    }
}

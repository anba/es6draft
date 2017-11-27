/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import java.util.Collections;
import java.util.Iterator;

import com.github.anba.es6draft.runtime.internal.InlineArrayList;

/**
 * 
 */
final class VariableScope implements Iterable<Variable<?>> {
    private final InlineArrayList<Variable<?>> variables = new InlineArrayList<>();
    final VariableScope parent;
    final int firstSlot;
    final org.objectweb.asm.Label start = new org.objectweb.asm.Label();
    final org.objectweb.asm.Label end = new org.objectweb.asm.Label();

    VariableScope(VariableScope parent, int firstSlot) {
        this.parent = parent;
        this.firstSlot = firstSlot;
    }

    <T> Variable<T> add(String name, Type type, int slot) {
        Variable<T> variable = new Variable<>(name, type, slot);
        variables.add(variable);
        return variable;
    }

    @Override
    public Iterator<Variable<?>> iterator() {
        if (variables.isEmpty()) {
            return Collections.emptyIterator();
        }
        return variables.iterator();
    }
}

/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 
 */
public final class VariablesSnapshot implements Iterable<Variable<?>> {
    private final BitSet variables;
    private final BitSet active;
    private final Type[] types;

    VariablesSnapshot(BitSet variables, BitSet active, Type[] types) {
        this.variables = variables;
        this.active = active;
        this.types = types;
    }

    BitSet getVariables() {
        return variables;
    }

    BitSet getActive() {
        return active;
    }

    Type[] getTypes() {
        return types;
    }

    private Variable<?> getVariable(int slot) {
        assert 0 <= slot && slot <= types.length : String.format("slot=%d not in [%d, %d]", slot,
                0, types.length);
        assert variables.get(slot) && types[slot] != null && types[slot] != Type.RESERVED : String
                .format("slot=%d, used=%b, type=%s", slot, variables.get(slot), types[slot]);
        return new Variable<>(null, types[slot], slot);
    }

    private int getNextInitializedSlot(int slot) {
        assert 0 <= slot && slot <= types.length : String.format("slot=%d not in [%d, %d]", slot,
                0, types.length);
        assert variables.get(slot) && active.get(slot) && types[slot] != null
                && types[slot] != Type.RESERVED : String.format(
                "slot=%d, used=%b, active=%b, type=%s", slot, variables.get(slot),
                active.get(slot), types[slot]);
        return active.nextSetBit(slot + types[slot].getSize());
    }

    /**
     * Returns the number of initialized variables.
     * 
     * @return the total variable count
     */
    public int getSize() {
        return active.cardinality();
    }

    @Override
    public Iterator<Variable<?>> iterator() {
        return new SnapshotIterator();
    }

    private final class SnapshotIterator implements Iterator<Variable<?>> {
        private int slot = 0;

        @Override
        public boolean hasNext() {
            return slot != -1;
        }

        @Override
        public Variable<?> next() {
            if (slot == -1) {
                throw new NoSuchElementException();
            }
            Variable<?> v = getVariable(slot);
            slot = getNextInitializedSlot(slot);
            return v;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}

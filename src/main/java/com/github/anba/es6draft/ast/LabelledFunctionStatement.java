/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.EnumSet;
import java.util.Set;

/**
 * <h1>Annex B</h1><br>
 * <h2>B.3 Other Additional Features</h2>
 * <ul>
 * <li>B.3.2 Labelled Function Declarations
 * </ul>
 * 
 * Statement type enclosing a {@link FunctionDeclaration}.
 */
public final class LabelledFunctionStatement extends Statement implements AbruptNode {
    private final Set<String> labelSet;
    private final HoistableDeclaration function;

    public LabelledFunctionStatement(long beginPosition, long endPosition, Set<String> labelSet,
            HoistableDeclaration function) {
        super(beginPosition, endPosition);
        this.labelSet = labelSet;
        this.function = function;
    }

    @Override
    public Set<String> getLabelSet() {
        return labelSet;
    }

    @Override
    public EnumSet<Abrupt> getAbrupt() {
        return EnumSet.noneOf(Abrupt.class);
    }

    public HoistableDeclaration getFunction() {
        return function;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> int accept(IntNodeVisitor<V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> void accept(VoidNodeVisitor<V> visitor, V value) {
        visitor.visit(this, value);
    }
}

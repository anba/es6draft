/**
 * 
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>11 Expressions</h1><br>
 * <h2>11.1 Primary Expressions</h2><br>
 * <h3>11.1.4 Array Initialiser</h3>
 * <ul>
 * <li>11.1.4.2 Array Comprehension
 * </ul>
 */
public class LegacyComprehension extends Comprehension implements ScopedNode {
    private BlockScope scope;

    public LegacyComprehension(BlockScope scope, List<ComprehensionQualifier> list,
            Expression expression) {
        super(list, expression);
        this.scope = scope;
    }

    @Override
    public BlockScope getScope() {
        return scope;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}

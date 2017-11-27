/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.runtime.objects.Eval;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.3 Left-Hand-Side Expressions</h2>
 * <ul>
 * <li>12.3.4 Function Calls
 * </ul>
 */
public final class CallExpression extends Expression {
    private final Expression base;
    private final List<Expression> arguments;
    private final Set<Eval.EvalFlags> evalFlags;

    public CallExpression(long beginPosition, long endPosition, Expression base, List<Expression> arguments,
            Set<Eval.EvalFlags> evalFlags) {
        super(beginPosition, endPosition);
        this.base = base;
        this.arguments = arguments;
        this.evalFlags = evalFlags;
    }

    /**
     * Returns the function call's base expression.
     * 
     * @return the callee expression
     */
    public Expression getBase() {
        // TODO: Rename to 'callee'?
        return base;
    }

    /**
     * Returns the list of arguments.
     * 
     * @return the arguments
     */
    public List<Expression> getArguments() {
        return arguments;
    }

    /**
     * Returns the set of {@code eval} flags.
     * 
     * @return the {@code eval} flags or the empty set if this call expression is not a direct eval call
     */
    public Set<Eval.EvalFlags> getEvalFlags() {
        return evalFlags;
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

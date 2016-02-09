/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.synthetic;

import java.util.Collections;
import java.util.List;

import com.github.anba.es6draft.ast.IntNodeVisitor;
import com.github.anba.es6draft.ast.ModuleItem;
import com.github.anba.es6draft.ast.NodeVisitor;
import com.github.anba.es6draft.ast.Statement;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.ast.VoidNodeVisitor;

/**
 * List of {@link StatementListItem}s as an external Java method.
 */
public final class StatementListMethod extends Statement {
    private final List<? extends ModuleItem> statements;

    public StatementListMethod(ModuleItem statement) {
        super(statement.getBeginPosition(), statement.getEndPosition());
        this.statements = Collections.singletonList(statement);
    }

    public StatementListMethod(List<? extends ModuleItem> statements) {
        super(first(statements).getBeginPosition(), last(statements).getEndPosition());
        this.statements = statements;
    }

    public List<? extends ModuleItem> getStatements() {
        return statements;
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

    private static ModuleItem first(List<? extends ModuleItem> elements) {
        assert !elements.isEmpty();
        return elements.get(0);
    }

    private static ModuleItem last(List<? extends ModuleItem> elements) {
        assert !elements.isEmpty();
        return elements.get(elements.size() - 1);
    }
}

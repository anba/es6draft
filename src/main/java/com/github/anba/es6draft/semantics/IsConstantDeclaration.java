/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.semantics;

import com.github.anba.es6draft.ast.ClassDeclaration;
import com.github.anba.es6draft.ast.FunctionDeclaration;
import com.github.anba.es6draft.ast.GeneratorDeclaration;
import com.github.anba.es6draft.ast.LexicalDeclaration;
import com.github.anba.es6draft.ast.LexicalDeclaration.Type;
import com.github.anba.es6draft.ast.NodeVisitor;

/**
 * Static Semantics: IsConstantDeclaration
 */
class IsConstantDeclaration extends StaticSemanticsVisitor<Boolean, Void> {
    static final NodeVisitor<Boolean, Void> INSTANCE = new IsConstantDeclaration();

    @Override
    public Boolean visit(LexicalDeclaration node, Void value) {
        if (node.getType() == Type.Const) {
            return true;
        } else {
            assert node.getType() == Type.Let;
            return false;
        }
    }

    @Override
    public Boolean visit(FunctionDeclaration node, Void value) {
        return false;
    }

    @Override
    public Boolean visit(GeneratorDeclaration node, Void value) {
        return false;
    }

    @Override
    public Boolean visit(ClassDeclaration node, Void value) {
        return false;
    }
}

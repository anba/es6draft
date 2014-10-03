/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.GeneratorComprehension;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.2 Primary Expression</h2>
 * <ul>
 * <li>12.2.7 Generator Comprehensions
 * </ul>
 */
final class GeneratorComprehensionGenerator extends ComprehensionGenerator {
    private GeneratorComprehensionGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    /**
     * 12.2.7.2 Runtime Semantics: Evaluation
     */
    static void EvaluateGeneratorComprehension(CodeGenerator codegen, GeneratorComprehension node,
            ExpressionVisitor mv) {
        node.getComprehension().accept(new GeneratorComprehensionGenerator(codegen), mv);
    }

    /**
     * 12.2.4.2.3 Runtime Semantics: ComprehensionEvaluation
     * <p>
     * ComprehensionTail : AssignmentExpression
     */
    @Override
    protected Void visit(Expression node, ExpressionVisitor mv) {
        /* steps 1-3 */
        expressionBoxedValue(node, mv);
        /* step 4 (not applicable) */
        /* steps 5-8 */
        yield(node, mv);
        mv.pop();

        return null;
    }
}

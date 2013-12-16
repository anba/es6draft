/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.GeneratorComprehension;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.1 Primary Expressions</h2>
 * <ul>
 * <li>12.1.7 Generator Comprehensions
 * </ul>
 */
final class GeneratorComprehensionGenerator extends ComprehensionGenerator {
    private static final class Methods {
        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_yield = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "yield",
                Type.getMethodType(Types.Object, Types.Object, Types.ExecutionContext));
    }

    private boolean initialised = false;

    GeneratorComprehensionGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    /**
     * 12.1.7.2 Runtime Semantics: Evaluation
     */
    @Override
    public Void visit(GeneratorComprehension node, ExpressionVisitor mv) {
        if (initialised) {
            // nested generator comprehension
            return visit((Expression) node, mv);
        }
        this.initialised = true;

        node.getComprehension().accept(this, mv);

        return null;
    }

    /**
     * 12.1.4.2.3 Runtime Semantics: ComprehensionEvaluation
     * <p>
     * ComprehensionTail : AssignmentExpression
     */
    @Override
    protected Void visit(Expression node, ExpressionVisitor mv) {
        assert initialised : "generator-comprehension generator not initialised";

        expressionBoxedValue(node, mv);
        mv.lineInfo(node);
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_yield);
        mv.pop();

        return null;
    }
}

/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

import static java.util.Collections.singletonList;

import java.util.List;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.synthetic.ElementAccessorValue;
import com.github.anba.es6draft.ast.synthetic.ExpressionMethod;
import com.github.anba.es6draft.ast.synthetic.IdentifierReferenceValue;
import com.github.anba.es6draft.ast.synthetic.PropertyAccessorValue;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.SpreadArrayLiteral;
import com.github.anba.es6draft.ast.synthetic.SpreadElementMethod;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;
import com.github.anba.es6draft.ast.synthetic.SuperExpressionValue;

/**
 * Returns the estimated byte code size for a {@link Node}
 */
final class CodeSizeVisitor implements IntNodeVisitor<CodeSizeHandler> {
    public int startAnalyze(Node node, List<? extends Node> children, CodeSizeHandler value) {
        return analyze(node, children, 0, 0, value);
    }

    public int startAnalyze(Node node, CodeSizeHandler value) {
        return node.accept(this, value);
    }

    private void submit(TopLevelNode<?> node, List<? extends Node> children, CodeSizeHandler value) {
        value.submit(node, children);
    }

    private int reportSize(Node node, int size, CodeSizeHandler value) {
        return value.reportSize(node, size);
    }

    private int analyze(Node node, Node child, int nodeSize, CodeSizeHandler value) {
        int size = nodeSize;
        if (child != null) {
            size += child.accept(this, value);
        }
        return reportSize(node, size, value);
    }

    private int analyze(Node node, Node left, Node right, int nodeSize, CodeSizeHandler value) {
        int size = nodeSize;
        if (left != null) {
            size += left.accept(this, value);
        }
        if (right != null) {
            size += right.accept(this, value);
        }
        return reportSize(node, size, value);
    }

    private int analyze(Node node, Node left, Node middle, Node right, int nodeSize,
            CodeSizeHandler value) {
        int size = nodeSize;
        if (left != null) {
            size += left.accept(this, value);
        }
        if (middle != null) {
            size += middle.accept(this, value);
        }
        if (right != null) {
            size += right.accept(this, value);
        }
        return reportSize(node, size, value);
    }

    private int analyze(Node node, Node left, Node middle, Node right, Node extra, int nodeSize,
            CodeSizeHandler value) {
        int size = nodeSize;
        if (left != null) {
            size += left.accept(this, value);
        }
        if (middle != null) {
            size += middle.accept(this, value);
        }
        if (right != null) {
            size += right.accept(this, value);
        }
        if (extra != null) {
            size += extra.accept(this, value);
        }
        return reportSize(node, size, value);
    }

    private int analyze(Node node, Node left, Node middle, Node right,
            List<? extends Node> children, int nodeSize, int childFactor, CodeSizeHandler value) {
        int size = nodeSize + childFactor * children.size();
        if (left != null) {
            size += left.accept(this, value);
        }
        if (middle != null) {
            size += middle.accept(this, value);
        }
        if (right != null) {
            size += right.accept(this, value);
        }
        for (Node child : children) {
            size += child.accept(this, value);
        }
        return reportSize(node, size, value);
    }

    private int analyze(Node node, List<? extends Node> children, int nodeSize, int childFactor,
            CodeSizeHandler value) {
        int size = nodeSize + childFactor * children.size();
        for (Node child : children) {
            size += child.accept(this, value);
        }
        return reportSize(node, size, value);
    }

    private int analyze(Node node, List<? extends Node> children, Node extra, int nodeSize,
            int childFactor, CodeSizeHandler value) {
        int size = nodeSize + childFactor * children.size();
        for (Node child : children) {
            size += child.accept(this, value);
        }
        if (extra != null) {
            size += extra.accept(this, value);
        }
        return reportSize(node, size, value);
    }

    private int analyze(MethodDefinition node, PropertyName child, int nodeSize,
            CodeSizeHandler value) {
        int size = nodeSize;
        if (child != null) {
            size += child.accept(this, value);
        }
        // TODO: reportSize() problematic
        // return reportSize(node, size, value);
        return size;
    }

    private int stringSize(String s) {
        if (s.length() <= 32768) {
            return 5;
        } else {
            return ((s.length() / 32768) + 1) * 10;
        }
    }

    /* ***************************************************************************************** */

    @Override
    public int visit(ArrayAssignmentPattern node, CodeSizeHandler value) {
        return analyze(node, node.getElements(), 30, 5, value);
    }

    @Override
    public int visit(ArrayBindingPattern node, CodeSizeHandler value) {
        return analyze(node, node.getElements(), 30, 5, value);
    }

    @Override
    public int visit(ArrayComprehension node, CodeSizeHandler value) {
        return analyze(node, node.getComprehension(), 25, value);
    }

    @Override
    public int visit(ArrayLiteral node, CodeSizeHandler value) {
        return analyze(node, node.getElements(), 25, 5, value);
    }

    @Override
    public int visit(ArrowFunction node, CodeSizeHandler value) {
        if (node.getExpression() != null) {
            submit(node, singletonList(node.getExpression()), value);
        } else {
            submit(node, node.getStatements(), value);
        }
        return 10;
    }

    @Override
    public int visit(AssignmentElement node, CodeSizeHandler value) {
        return analyze(node, node.getTarget(), node.getInitializer(), 25, value);
    }

    @Override
    public int visit(AssignmentExpression node, CodeSizeHandler value) {
        return analyze(node, node.getLeft(), node.getRight(), 10, value);
    }

    @Override
    public int visit(AssignmentProperty node, CodeSizeHandler value) {
        return analyze(node, node.getPropertyName(), node.getTarget(), node.getInitializer(), 25,
                value);
    }

    @Override
    public int visit(AssignmentRestElement node, CodeSizeHandler value) {
        return analyze(node, node.getTarget(), 25, value);
    }

    @Override
    public int visit(AsyncArrowFunction node, CodeSizeHandler value) {
        if (node.getExpression() != null) {
            submit(node, singletonList(node.getExpression()), value);
        } else {
            submit(node, node.getStatements(), value);
        }
        return 10;
    }

    @Override
    public int visit(AsyncFunctionDeclaration node, CodeSizeHandler value) {
        submit(node, node.getStatements(), value);
        return 0;
    }

    @Override
    public int visit(AsyncFunctionExpression node, CodeSizeHandler value) {
        submit(node, node.getStatements(), value);
        return 10;
    }

    @Override
    public int visit(AwaitExpression node, CodeSizeHandler value) {
        return analyze(node, node.getExpression(), 150, value);
    }

    @Override
    public int visit(BinaryExpression node, CodeSizeHandler value) {
        return analyze(node, node.getLeft(), node.getRight(), 20, value);
    }

    @Override
    public int visit(BindingElement node, CodeSizeHandler value) {
        return analyze(node, node.getBinding(), node.getInitializer(), 25, value);
    }

    @Override
    public int visit(BindingElision node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public int visit(BindingIdentifier node, CodeSizeHandler value) {
        return 15;
    }

    @Override
    public int visit(BindingProperty node, CodeSizeHandler value) {
        return analyze(node, node.getPropertyName(), node.getBinding(), node.getInitializer(), 25,
                value);
    }

    @Override
    public int visit(BindingRestElement node, CodeSizeHandler value) {
        return analyze(node, node.getBindingIdentifier(), 25, value);
    }

    @Override
    public int visit(BlockStatement node, CodeSizeHandler value) {
        return analyze(node, node.getStatements(), 15, 0, value);
    }

    @Override
    public int visit(BooleanLiteral node, CodeSizeHandler value) {
        return 5;
    }

    @Override
    public int visit(BreakStatement node, CodeSizeHandler value) {
        return 5;
    }

    @Override
    public int visit(CallExpression node, CodeSizeHandler value) {
        return analyze(node, node.getArguments(), node.getBase(), 30, 5, value);
    }

    @Override
    public int visit(CallSpreadElement node, CodeSizeHandler value) {
        return analyze(node, node.getExpression(), 10, value);
    }

    @Override
    public int visit(CatchNode node, CodeSizeHandler value) {
        return analyze(node, node.getCatchParameter(), node.getCatchBlock(), 35, value);
    }

    @Override
    public int visit(ClassDeclaration node, CodeSizeHandler value) {
        return analyze(node, node.getMethods(), node.getHeritage(), 50, 10, value);
    }

    @Override
    public int visit(ClassExpression node, CodeSizeHandler value) {
        return analyze(node, node.getMethods(), node.getHeritage(), 50, 10, value);
    }

    @Override
    public int visit(CommaExpression node, CodeSizeHandler value) {
        return analyze(node, node.getOperands(), 5, 5, value);
    }

    @Override
    public int visit(Comprehension node, CodeSizeHandler value) {
        return analyze(node, node.getList(), node.getExpression(), 50, 0, value);
    }

    @Override
    public int visit(ComprehensionFor node, CodeSizeHandler value) {
        return analyze(node, node.getBinding(), node.getExpression(), 40, value);
    }

    @Override
    public int visit(ComprehensionIf node, CodeSizeHandler value) {
        return analyze(node, node.getTest(), 15, value);
    }

    @Override
    public int visit(ComputedPropertyName node, CodeSizeHandler value) {
        return analyze(node, node.getExpression(), 15, value);
    }

    @Override
    public int visit(ConditionalExpression node, CodeSizeHandler value) {
        return analyze(node, node.getTest(), node.getThen(), node.getOtherwise(), 20, value);
    }

    @Override
    public int visit(ContinueStatement node, CodeSizeHandler value) {
        return 5;
    }

    @Override
    public int visit(DebuggerStatement node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public int visit(DoWhileStatement node, CodeSizeHandler value) {
        return analyze(node, node.getTest(), node.getStatement(), 25, value);
    }

    @Override
    public int visit(ElementAccessor node, CodeSizeHandler value) {
        return analyze(node, node.getBase(), node.getElement(), 10, value);
    }

    @Override
    public int visit(ElementAccessorValue node, CodeSizeHandler value) {
        return analyze(node, node.getBase(), node.getElement(), 10, value);
    }

    @Override
    public int visit(Elision node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public int visit(EmptyStatement node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public int visit(ExportDeclaration node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public int visit(ExportSpecifier node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public int visit(ExportsClause node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public int visit(ExpressionMethod node, CodeSizeHandler value) {
        return 5;
    }

    @Override
    public int visit(ExpressionStatement node, CodeSizeHandler value) {
        return analyze(node, node.getExpression(), 5, value);
    }

    @Override
    public int visit(ForEachStatement node, CodeSizeHandler value) {
        return analyze(node, node.getHead(), node.getExpression(), node.getStatement(), 50, value);
    }

    @Override
    public int visit(ForInStatement node, CodeSizeHandler value) {
        return analyze(node, node.getHead(), node.getExpression(), node.getStatement(), 50, value);
    }

    @Override
    public int visit(FormalParameterList node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public int visit(ForOfStatement node, CodeSizeHandler value) {
        return analyze(node, node.getHead(), node.getExpression(), node.getStatement(), 50, value);
    }

    @Override
    public int visit(ForStatement node, CodeSizeHandler value) {
        return analyze(node, node.getHead(), node.getTest(), node.getStep(), node.getStatement(),
                30, value);
    }

    @Override
    public int visit(FunctionDeclaration node, CodeSizeHandler value) {
        submit(node, node.getStatements(), value);
        return 0;
    }

    @Override
    public int visit(FunctionExpression node, CodeSizeHandler value) {
        submit(node, node.getStatements(), value);
        return 10;
    }

    @Override
    public int visit(GeneratorComprehension node, CodeSizeHandler value) {
        submit(node, singletonList(node.getComprehension()), value);
        return 10;
    }

    @Override
    public int visit(GeneratorDeclaration node, CodeSizeHandler value) {
        submit(node, node.getStatements(), value);
        return 0;
    }

    @Override
    public int visit(GeneratorExpression node, CodeSizeHandler value) {
        submit(node, node.getStatements(), value);
        return 10;
    }

    @Override
    public int visit(GuardedCatchNode node, CodeSizeHandler value) {
        return analyze(node, node.getCatchParameter(), node.getGuard(), node.getCatchBlock(), 45,
                value);
    }

    @Override
    public int visit(IdentifierName node, CodeSizeHandler value) {
        return 5;
    }

    @Override
    public int visit(IdentifierReference node, CodeSizeHandler value) {
        return 10;
    }

    @Override
    public int visit(IdentifierReferenceValue node, CodeSizeHandler value) {
        return 10;
    }

    @Override
    public int visit(IfStatement node, CodeSizeHandler value) {
        return analyze(node, node.getTest(), node.getOtherwise(), node.getThen(), 10, value);
    }

    @Override
    public int visit(ImportDeclaration node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public int visit(ImportSpecifier node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public int visit(ImportClause node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public int visit(LabelledStatement node, CodeSizeHandler value) {
        return analyze(node, node.getStatement(), 15, value);
    }

    @Override
    public int visit(LegacyComprehension node, CodeSizeHandler value) {
        return analyze(node, node.getList(), node.getExpression(), 50, 0, value);
    }

    @Override
    public int visit(LegacyComprehensionFor node, CodeSizeHandler value) {
        return analyze(node, node.getBinding(), node.getExpression(), 40, value);
    }

    @Override
    public int visit(LegacyGeneratorDeclaration node, CodeSizeHandler value) {
        return visit((GeneratorDeclaration) node, value);
    }

    @Override
    public int visit(LegacyGeneratorExpression node, CodeSizeHandler value) {
        return visit((GeneratorExpression) node, value);
    }

    @Override
    public int visit(LetExpression node, CodeSizeHandler value) {
        return analyze(node, node.getBindings(), node.getExpression(), 25, 5, value);
    }

    @Override
    public int visit(LetStatement node, CodeSizeHandler value) {
        return analyze(node, node.getBindings(), node.getStatement(), 25, 5, value);
    }

    @Override
    public int visit(LexicalBinding node, CodeSizeHandler value) {
        return analyze(node, node.getBinding(), node.getInitializer(), 20, value);
    }

    @Override
    public int visit(LexicalDeclaration node, CodeSizeHandler value) {
        return analyze(node, node.getElements(), 0, 0, value);
    }

    @Override
    public int visit(MethodDefinition node, CodeSizeHandler value) {
        submit(node, node.getStatements(), value);
        return analyze(node, node.getPropertyName(), 10, value);
    }

    @Override
    public int visit(Module node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public int visit(ModuleImport node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public int visit(NativeCallExpression node, CodeSizeHandler value) {
        return analyze(node, node.getArguments(), node.getBase(), 30, 5, value);
    }

    @Override
    public int visit(NewExpression node, CodeSizeHandler value) {
        return analyze(node, node.getArguments(), node.getExpression(), 15, 5, value);
    }

    @Override
    public int visit(NullLiteral node, CodeSizeHandler value) {
        return 5;
    }

    @Override
    public int visit(NumericLiteral node, CodeSizeHandler value) {
        return 5;
    }

    @Override
    public int visit(ObjectAssignmentPattern node, CodeSizeHandler value) {
        return analyze(node, node.getProperties(), 20, 5, value);
    }

    @Override
    public int visit(ObjectBindingPattern node, CodeSizeHandler value) {
        return analyze(node, node.getProperties(), 20, 5, value);
    }

    @Override
    public int visit(ObjectLiteral node, CodeSizeHandler value) {
        return analyze(node, node.getProperties(), 15, 5, value);
    }

    @Override
    public int visit(PropertyAccessor node, CodeSizeHandler value) {
        return analyze(node, node.getBase(), 10, value);
    }

    @Override
    public int visit(PropertyAccessorValue node, CodeSizeHandler value) {
        return analyze(node, node.getBase(), 10, value);
    }

    @Override
    public int visit(PropertyDefinitionsMethod node, CodeSizeHandler value) {
        // don't descend into synthetic nodes
        return 10;
    }

    @Override
    public int visit(PropertyNameDefinition node, CodeSizeHandler value) {
        return 15;
    }

    @Override
    public int visit(PropertyValueDefinition node, CodeSizeHandler value) {
        return analyze(node, node.getPropertyName(), node.getPropertyValue(), 5, value);
    }

    @Override
    public int visit(RegularExpressionLiteral node, CodeSizeHandler value) {
        return 10;
    }

    @Override
    public int visit(ReturnStatement node, CodeSizeHandler value) {
        return analyze(node, node.getExpression(), 10, value);
    }

    @Override
    public int visit(Script node, CodeSizeHandler value) {
        throw new IllegalStateException();
    }

    @Override
    public int visit(SpreadArrayLiteral node, CodeSizeHandler value) {
        // don't descend into synthetic nodes
        return 10;
    }

    @Override
    public int visit(SpreadElement node, CodeSizeHandler value) {
        return analyze(node, node.getExpression(), 10, value);
    }

    @Override
    public int visit(SpreadElementMethod node, CodeSizeHandler value) {
        // don't descend into synthetic nodes
        return 10;
    }

    @Override
    public int visit(StatementListMethod node, CodeSizeHandler value) {
        // don't descend into synthetic nodes
        return 15;
    }

    @Override
    public int visit(StringLiteral node, CodeSizeHandler value) {
        return stringSize(node.getValue());
    }

    @Override
    public int visit(SuperExpression node, CodeSizeHandler value) {
        switch (node.getType()) {
        case PropertyAccessor:
            return 10;
        case ElementAccessor:
            return analyze(node, node.getExpression(), 15, value);
        case CallExpression:
            return analyze(node, node.getArguments(), 25, 5, value);
        case NewExpression:
            return 10;
        default:
            throw new IllegalStateException();
        }
    }

    @Override
    public int visit(SuperExpressionValue node, CodeSizeHandler value) {
        return visit((SuperExpression) node, value);
    }

    @Override
    public int visit(SwitchClause node, CodeSizeHandler value) {
        return analyze(node, node.getStatements(), node.getExpression(), 15, 0, value);
    }

    @Override
    public int visit(SwitchStatement node, CodeSizeHandler value) {
        return analyze(node, node.getClauses(), node.getExpression(), 100, 0, value);
    }

    @Override
    public int visit(TemplateCallExpression node, CodeSizeHandler value) {
        return analyze(node, node.getBase(), node.getTemplate(), 30, value);
    }

    @Override
    public int visit(TemplateCharacters node, CodeSizeHandler value) {
        return stringSize(node.getValue()) + stringSize(node.getRawValue());
    }

    @Override
    public int visit(TemplateLiteral node, CodeSizeHandler value) {
        return analyze(node, node.getElements(), 25, 10, value);
    }

    @Override
    public int visit(ThisExpression node, CodeSizeHandler value) {
        return 10;
    }

    @Override
    public int visit(ThrowStatement node, CodeSizeHandler value) {
        return analyze(node, node.getExpression(), 10, value);
    }

    @Override
    public int visit(TryStatement node, CodeSizeHandler value) {
        return analyze(node, node.getTryBlock(), node.getCatchNode(), node.getFinallyBlock(),
                node.getGuardedCatchNodes(), 40, 20, value);
    }

    @Override
    public int visit(UnaryExpression node, CodeSizeHandler value) {
        return analyze(node, node.getOperand(), 20, value);
    }

    @Override
    public int visit(VariableDeclaration node, CodeSizeHandler value) {
        return analyze(node, node.getBinding(), node.getInitializer(), 15, value);
    }

    @Override
    public int visit(VariableStatement node, CodeSizeHandler value) {
        return analyze(node, node.getElements(), 0, 0, value);
    }

    @Override
    public int visit(WhileStatement node, CodeSizeHandler value) {
        return analyze(node, node.getTest(), node.getStatement(), 25, value);
    }

    @Override
    public int visit(WithStatement node, CodeSizeHandler value) {
        return analyze(node, node.getExpression(), node.getStatement(), 25, value);
    }

    @Override
    public int visit(YieldExpression node, CodeSizeHandler value) {
        if (node.isDelegatedYield()) {
            return analyze(node, node.getExpression(), 300, value);
        }
        return analyze(node, node.getExpression(), 150, value);
    }
}

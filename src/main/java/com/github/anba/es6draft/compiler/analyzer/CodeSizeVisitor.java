/**
 * Copyright (c) 2012-2013 André Bargull
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
import com.github.anba.es6draft.ast.synthetic.IdentifierValue;
import com.github.anba.es6draft.ast.synthetic.PropertyAccessorValue;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;
import com.github.anba.es6draft.ast.synthetic.SpreadArrayLiteral;
import com.github.anba.es6draft.ast.synthetic.SpreadElementMethod;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;
import com.github.anba.es6draft.ast.synthetic.SuperExpressionValue;

/**
 * Returns the estimated byte code size for a {@link Node}
 */
class CodeSizeVisitor implements NodeVisitor<Integer, CodeSizeHandler> {
    public CodeSizeVisitor() {
    }

    public int startAnalyze(Node node, List<? extends Node> children, CodeSizeHandler value) {
        return analyze(node, children, 0, 0, value);
    }

    public int startAnalyze(Node node, CodeSizeHandler value) {
        return node.accept(this, value);
    }

    private void submit(Node node, List<? extends Node> children, CodeSizeHandler value) {
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

    private int analyze(Node node, List<? extends Node> children1, List<? extends Node> children2,
            Node extra, int nodeSize, int childFactor, CodeSizeHandler value) {
        int size = nodeSize + childFactor * children1.size() + childFactor * children2.size();
        for (Node child : children1) {
            size += child.accept(this, value);
        }
        for (Node child : children2) {
            size += child.accept(this, value);
        }
        if (extra != null) {
            size += extra.accept(this, value);
        }
        return reportSize(node, size, value);
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
    public Integer visit(ArrayAssignmentPattern node, CodeSizeHandler value) {
        return analyze(node, node.getElements(), 30, 5, value);
    }

    @Override
    public Integer visit(ArrayBindingPattern node, CodeSizeHandler value) {
        return analyze(node, node.getElements(), 30, 5, value);
    }

    @Override
    public Integer visit(ArrayComprehension node, CodeSizeHandler value) {
        return analyze(node, node.getComprehension(), 25, value);
    }

    @Override
    public Integer visit(ArrayLiteral node, CodeSizeHandler value) {
        return analyze(node, node.getElements(), 25, 5, value);
    }

    @Override
    public Integer visit(ArrowFunction node, CodeSizeHandler value) {
        return 10;
    }

    @Override
    public Integer visit(AssignmentElement node, CodeSizeHandler value) {
        return analyze(node, node.getTarget(), node.getInitialiser(), 25, value);
    }

    @Override
    public Integer visit(AssignmentExpression node, CodeSizeHandler value) {
        return analyze(node, node.getLeft(), node.getRight(), 10, value);
    }

    @Override
    public Integer visit(AssignmentProperty node, CodeSizeHandler value) {
        return analyze(node, node.getTarget(), node.getInitialiser(), 25, value);
    }

    @Override
    public Integer visit(AssignmentRestElement node, CodeSizeHandler value) {
        return analyze(node, node.getTarget(), 25, value);
    }

    @Override
    public Integer visit(BinaryExpression node, CodeSizeHandler value) {
        return analyze(node, node.getLeft(), node.getRight(), 20, value);
    }

    @Override
    public Integer visit(BindingElement node, CodeSizeHandler value) {
        return analyze(node, node.getBinding(), node.getInitialiser(), 25, value);
    }

    @Override
    public Integer visit(BindingElision node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public Integer visit(BindingIdentifier node, CodeSizeHandler value) {
        return 15;
    }

    @Override
    public Integer visit(BindingProperty node, CodeSizeHandler value) {
        return analyze(node, node.getBinding(), node.getInitialiser(), 25, value);
    }

    @Override
    public Integer visit(BindingRestElement node, CodeSizeHandler value) {
        return analyze(node, node.getBindingIdentifier(), 25, value);
    }

    @Override
    public Integer visit(BlockStatement node, CodeSizeHandler value) {
        return analyze(node, node.getStatements(), 15, 0, value);
    }

    @Override
    public Integer visit(BooleanLiteral node, CodeSizeHandler value) {
        return 5;
    }

    @Override
    public Integer visit(BreakStatement node, CodeSizeHandler value) {
        return 5;
    }

    @Override
    public Integer visit(CallExpression node, CodeSizeHandler value) {
        return analyze(node, node.getArguments(), node.getBase(), 25, 5, value);
    }

    @Override
    public Integer visit(CallSpreadElement node, CodeSizeHandler value) {
        return analyze(node, node.getExpression(), 10, value);
    }

    @Override
    public Integer visit(CatchNode node, CodeSizeHandler value) {
        return analyze(node, node.getCatchParameter(), node.getCatchBlock(), 35, value);
    }

    @Override
    public Integer visit(ClassDeclaration node, CodeSizeHandler value) {
        return analyze(node, node.getPrototypeMethods(), node.getStaticMethods(),
                node.getHeritage(), 50, 10, value);
    }

    @Override
    public Integer visit(ClassExpression node, CodeSizeHandler value) {
        return analyze(node, node.getPrototypeMethods(), node.getStaticMethods(),
                node.getHeritage(), 50, 10, value);
    }

    @Override
    public Integer visit(CommaExpression node, CodeSizeHandler value) {
        return analyze(node, node.getOperands(), 5, 5, value);
    }

    @Override
    public Integer visit(Comprehension node, CodeSizeHandler value) {
        return analyze(node, node.getList(), node.getExpression(), 50, 0, value);
    }

    @Override
    public Integer visit(ComprehensionFor node, CodeSizeHandler value) {
        return analyze(node, node.getBinding(), node.getExpression(), 40, value);
    }

    @Override
    public Integer visit(ComprehensionIf node, CodeSizeHandler value) {
        return analyze(node, node.getTest(), 15, value);
    }

    @Override
    public Integer visit(ConditionalExpression node, CodeSizeHandler value) {
        return analyze(node, node.getTest(), node.getThen(), node.getOtherwise(), 20, value);
    }

    @Override
    public Integer visit(ContinueStatement node, CodeSizeHandler value) {
        return 5;
    }

    @Override
    public Integer visit(DebuggerStatement node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public Integer visit(DoWhileStatement node, CodeSizeHandler value) {
        return analyze(node, node.getTest(), node.getStatement(), 25, value);
    }

    @Override
    public Integer visit(ElementAccessor node, CodeSizeHandler value) {
        return analyze(node, node.getBase(), node.getElement(), 10, value);
    }

    @Override
    public Integer visit(ElementAccessorValue node, CodeSizeHandler value) {
        return analyze(node, node.getBase(), node.getElement(), 10, value);
    }

    @Override
    public Integer visit(Elision node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public Integer visit(EmptyStatement node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public Integer visit(ExportDeclaration node, CodeSizeHandler value) {
        return analyze(node, node.getDeclaration(), node.getExportSpecifierSet(),
                node.getExpression(), node.getVariableStatement(), 0, value);
    }

    @Override
    public Integer visit(ExportSpecifier node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public Integer visit(ExportSpecifierSet node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public Integer visit(ExpressionMethod node, CodeSizeHandler value) {
        return 5;
    }

    @Override
    public Integer visit(ExpressionStatement node, CodeSizeHandler value) {
        return analyze(node, node.getExpression(), 5, value);
    }

    @Override
    public Integer visit(ForEachStatement node, CodeSizeHandler value) {
        return analyze(node, node.getHead(), node.getExpression(), node.getStatement(), 50, value);
    }

    @Override
    public Integer visit(ForInStatement node, CodeSizeHandler value) {
        return analyze(node, node.getHead(), node.getExpression(), node.getStatement(), 50, value);
    }

    @Override
    public Integer visit(FormalParameterList node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public Integer visit(ForOfStatement node, CodeSizeHandler value) {
        return analyze(node, node.getHead(), node.getExpression(), node.getStatement(), 50, value);
    }

    @Override
    public Integer visit(ForStatement node, CodeSizeHandler value) {
        return analyze(node, node.getHead(), node.getTest(), node.getStep(), node.getStatement(),
                30, value);
    }

    @Override
    public Integer visit(FunctionDeclaration node, CodeSizeHandler value) {
        submit(node, node.getStatements(), value);
        return 0;
    }

    @Override
    public Integer visit(FunctionExpression node, CodeSizeHandler value) {
        submit(node, node.getStatements(), value);
        return 10;
    }

    @Override
    public Integer visit(GeneratorComprehension node, CodeSizeHandler value) {
        submit(node, singletonList(node.getComprehension()), value);
        return 10;
    }

    @Override
    public Integer visit(GeneratorDeclaration node, CodeSizeHandler value) {
        submit(node, node.getStatements(), value);
        return 0;
    }

    @Override
    public Integer visit(GeneratorExpression node, CodeSizeHandler value) {
        submit(node, node.getStatements(), value);
        return 10;
    }

    @Override
    public Integer visit(GuardedCatchNode node, CodeSizeHandler value) {
        return analyze(node, node.getCatchParameter(), node.getGuard(), node.getCatchBlock(), 45,
                value);
    }

    @Override
    public Integer visit(Identifier node, CodeSizeHandler value) {
        return 10;
    }

    @Override
    public Integer visit(IdentifierValue node, CodeSizeHandler value) {
        return 10;
    }

    @Override
    public Integer visit(IfStatement node, CodeSizeHandler value) {
        return analyze(node, node.getTest(), node.getOtherwise(), node.getThen(), 10, value);
    }

    @Override
    public Integer visit(ImportDeclaration node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public Integer visit(ImportSpecifier node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public Integer visit(ImportSpecifierSet node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public Integer visit(LabelledStatement node, CodeSizeHandler value) {
        return 15;
    }

    @Override
    public Integer visit(LetExpression node, CodeSizeHandler value) {
        return analyze(node, node.getBindings(), node.getExpression(), 25, 5, value);
    }

    @Override
    public Integer visit(LetStatement node, CodeSizeHandler value) {
        return analyze(node, node.getBindings(), node.getStatement(), 25, 5, value);
    }

    @Override
    public Integer visit(LexicalBinding node, CodeSizeHandler value) {
        return analyze(node, node.getInitialiser(), 5, value);
    }

    @Override
    public Integer visit(LexicalDeclaration node, CodeSizeHandler value) {
        return analyze(node, node.getElements(), 0, 0, value);
    }

    @Override
    public Integer visit(MethodDefinition node, CodeSizeHandler value) {
        submit(node, node.getStatements(), value);
        return 10;
    }

    @Override
    public Integer visit(ModuleDeclaration node, CodeSizeHandler value) {
        return 0;
    }

    @Override
    public Integer visit(NewExpression node, CodeSizeHandler value) {
        return analyze(node, node.getArguments(), node.getExpression(), 15, 5, value);
    }

    @Override
    public Integer visit(NullLiteral node, CodeSizeHandler value) {
        return 5;
    }

    @Override
    public Integer visit(NumericLiteral node, CodeSizeHandler value) {
        return 5;
    }

    @Override
    public Integer visit(ObjectAssignmentPattern node, CodeSizeHandler value) {
        return analyze(node, node.getProperties(), 20, 5, value);
    }

    @Override
    public Integer visit(ObjectBindingPattern node, CodeSizeHandler value) {
        return analyze(node, node.getProperties(), 20, 5, value);
    }

    @Override
    public Integer visit(ObjectLiteral node, CodeSizeHandler value) {
        return analyze(node, node.getProperties(), 15, 5, value);
    }

    @Override
    public Integer visit(PropertyAccessor node, CodeSizeHandler value) {
        return analyze(node, node.getBase(), 10, value);
    }

    @Override
    public Integer visit(PropertyAccessorValue node, CodeSizeHandler value) {
        return analyze(node, node.getBase(), 10, value);
    }

    @Override
    public Integer visit(PropertyDefinitionsMethod node, CodeSizeHandler value) {
        // don't descend into synthetic nodes
        return 10;
    }

    @Override
    public Integer visit(PropertyNameDefinition node, CodeSizeHandler value) {
        return 15;
    }

    @Override
    public Integer visit(PropertyValueDefinition node, CodeSizeHandler value) {
        return analyze(node, node.getPropertyValue(), 5, value);
    }

    @Override
    public Integer visit(RegularExpressionLiteral node, CodeSizeHandler value) {
        return 10;
    }

    @Override
    public Integer visit(ReturnStatement node, CodeSizeHandler value) {
        return analyze(node, node.getExpression(), 10, value);
    }

    @Override
    public Integer visit(Script node, CodeSizeHandler value) {
        throw new IllegalStateException();
    }

    @Override
    public Integer visit(SpreadArrayLiteral node, CodeSizeHandler value) {
        // don't descend into synthetic nodes
        return 10;
    }

    @Override
    public Integer visit(SpreadElement node, CodeSizeHandler value) {
        return analyze(node, node.getExpression(), 10, value);
    }

    @Override
    public Integer visit(SpreadElementMethod node, CodeSizeHandler value) {
        // don't descend into synthetic nodes
        return 10;
    }

    @Override
    public Integer visit(StatementListMethod node, CodeSizeHandler value) {
        return 15;
    }

    @Override
    public Integer visit(StringLiteral node, CodeSizeHandler value) {
        return stringSize(node.getValue());
    }

    @Override
    public Integer visit(SuperExpression node, CodeSizeHandler value) {
        if (node.getName() != null) {
            return 10;
        } else if (node.getExpression() != null) {
            return analyze(node, node.getExpression(), 15, value);
        } else if (node.getArguments() != null) {
            return analyze(node, node.getArguments(), 25, 5, value);
        } else {
            return 10;
        }
    }

    @Override
    public Integer visit(SuperExpressionValue node, CodeSizeHandler value) {
        return visit((SuperExpression) node, value);
    }

    @Override
    public Integer visit(SwitchClause node, CodeSizeHandler value) {
        return analyze(node, node.getStatements(), node.getExpression(), 15, 0, value);
    }

    @Override
    public Integer visit(SwitchStatement node, CodeSizeHandler value) {
        return analyze(node, node.getClauses(), node.getExpression(), 100, 0, value);
    }

    @Override
    public Integer visit(TemplateCallExpression node, CodeSizeHandler value) {
        return analyze(node, node.getBase(), node.getTemplate(), 25, value);
    }

    @Override
    public Integer visit(TemplateCharacters node, CodeSizeHandler value) {
        return stringSize(node.getValue()) + stringSize(node.getRawValue());
    }

    @Override
    public Integer visit(TemplateLiteral node, CodeSizeHandler value) {
        return analyze(node, node.getElements(), 20, 10, value);
    }

    @Override
    public Integer visit(ThisExpression node, CodeSizeHandler value) {
        return 10;
    }

    @Override
    public Integer visit(ThrowStatement node, CodeSizeHandler value) {
        return analyze(node, node.getExpression(), 10, value);
    }

    @Override
    public Integer visit(TryStatement node, CodeSizeHandler value) {
        return analyze(node, node.getTryBlock(), node.getCatchNode(), node.getFinallyBlock(),
                node.getGuardedCatchNodes(), 35, 15, value);
    }

    @Override
    public Integer visit(UnaryExpression node, CodeSizeHandler value) {
        return analyze(node, node.getOperand(), 20, value);
    }

    @Override
    public Integer visit(VariableDeclaration node, CodeSizeHandler value) {
        return analyze(node, node.getInitialiser(), 5, value);
    }

    @Override
    public Integer visit(VariableStatement node, CodeSizeHandler value) {
        return analyze(node, node.getElements(), 0, 0, value);
    }

    @Override
    public Integer visit(WhileStatement node, CodeSizeHandler value) {
        return analyze(node, node.getTest(), node.getStatement(), 25, value);
    }

    @Override
    public Integer visit(WithStatement node, CodeSizeHandler value) {
        return analyze(node, node.getExpression(), node.getStatement(), 25, value);
    }

    @Override
    public Integer visit(YieldExpression node, CodeSizeHandler value) {
        return analyze(node, node.getExpression(), 15, value);
    }
}

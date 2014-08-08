/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

import static java.util.Collections.singletonList;

import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.Map.Entry;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;

/**
 * Handles statements which are not at top-level.
 */
abstract class NestedSubMethod<NODE extends Node> extends SubMethod<NODE> {
    static final class StatementSubMethod extends NestedSubMethod<Statement> {
        @Override
        int processNode(Statement node, int oldSize) {
            return super.visitNested(node, oldSize);
        }
    }

    static final class SwitchClauseSubMethod extends NestedSubMethod<SwitchClause> {
        @Override
        int processNode(SwitchClause node, int oldSize) {
            return super.visitNested(node, oldSize);
        }
    }

    private static final class StatementElement extends NodeElement<StatementListItem> {
        static final StatementElement EMPTY = new StatementElement(ExportState.Empty, null, 0, -1);

        StatementElement(ExportState state, StatementListItem node, int size, int index) {
            super(state, node, size, index);
        }

        void maybeExport(List<StatementListItem> statements, int size) {
            assert state == ExportState.NotExported && size < MAX_STATEMENT_SIZE;
            update(ExportState.MaybeExported, new StatementListMethod(statements), size);
        }

        @Override
        protected final StatementListItem createReplacement() {
            return new StatementListMethod(singletonList(node));
        }

        @Override
        protected final int getReplacementSize() {
            return STMT_METHOD_SIZE;
        }

        static List<StatementElement> from(List<StatementListItem> statements,
                Map<StatementListItem, Integer> codeSizes) {
            List<StatementElement> list = new ArrayList<>(statements.size());
            for (int i = 0, len = statements.size(); i < len; i++) {
                StatementListItem stmt = statements.get(i);
                int size = codeSizes.get(stmt);
                list.add(new StatementElement(ExportState.NotExported, stmt, size, i));
            }
            return list;
        }
    }

    private int visitNested(NODE node, int oldSize) {
        // need to handle break/continue statements, i.e.
        /* 
         * while (test()) {
         *   <statements_1>
         *   break; // <-- can't move this to submethod, would lose label context
         *   <statements_2>
         * }
         */

        // re-compute all sizes
        MemorizingHandler memorizinghandler = new MemorizingHandler();
        new CodeSizeVisitor().startAnalyze(node, memorizinghandler);
        Map<StatementListItem, Integer> codeSizes = memorizinghandler.codeSizes;

        // find exportable statements
        FindExportableStatement findExport = new FindExportableStatement(codeSizes);
        node.accept(findExport, new ArrayDeque<Node>());
        List<StatementListItem> exportable = findExport.exportable;
        List<Node> parents = findExport.parents;

        List<StatementElement> elements = StatementElement.from(exportable, codeSizes);
        int accSize = oldSize;

        // replace single big elements with method-statements
        PriorityQueue<StatementElement> pq = new PriorityQueue<>(elements);
        while (!pq.isEmpty() && pq.peek().size > MAX_STATEMENT_SIZE) {
            StatementElement element = pq.remove();
            accSize += element.export();
        }

        if (accSize > MAX_STATEMENT_SIZE) {
            // if statement size still too large, try to export more statements, possibly by
            // compacting sibling elements
            for (int i = 0, len = elements.size(); i < len; i++) {
                StatementElement element = elements.get(i);
                if (element.state != ExportState.NotExported) {
                    continue;
                }
                assert element.node == exportable.get(i);
                Node parent = parents.get(i);
                if (i + 1 < len && parents.get(i + 1) == parent) {
                    // siblings, try to compact
                    if (parent instanceof BlockStatement) {
                        tryCompactSibling(elements, (BlockStatement) parent, i);
                    } else if (parent instanceof SwitchClause) {
                        tryCompactSibling(elements, (SwitchClause) parent, i);
                    }
                }
            }

            pq = new PriorityQueue<>(elements);
            while (!pq.isEmpty() && accSize > MAX_STATEMENT_SIZE) {
                StatementElement element = pq.remove();
                if (element.state == ExportState.Exported || element.state == ExportState.Empty) {
                    // or rather break..?
                    continue;
                }
                accSize += element.export();
            }
        }

        // update all entries which were marked as exported
        StatementUpdater updater = new StatementUpdater();
        for (int i = 0, len = elements.size(); i < len; i++) {
            StatementElement element = elements.get(i);
            if (element.state != ExportState.Exported) {
                continue;
            }
            Node parent = parents.get(i);
            StatementListItem sourceElement = exportable.get(i);
            StatementListMethod targetElement = (StatementListMethod) element.node;
            assert sourceElement != targetElement : sourceElement.getClass() + ", "
                    + targetElement.getClass();
            Entry<StatementListItem, StatementListMethod> entry = new SimpleEntry<>(sourceElement,
                    targetElement);
            parent.accept(updater, entry);
        }

        return validateSize(node);
    }

    private boolean tryCompactSibling(List<StatementElement> list, BlockStatement parent, int index) {
        return tryCompactSibling(list, parent.getStatements(), index);
    }

    private boolean tryCompactSibling(List<StatementElement> list, SwitchClause parent, int index) {
        return tryCompactSibling(list, parent.getStatements(), index);
    }

    private boolean tryCompactSibling(List<StatementElement> elements,
            List<StatementListItem> statements, int index) {
        StatementElement element = elements.get(index);
        int startIndex = statements.indexOf(element.node);
        assert startIndex != -1;
        int endIndex = startIndex + 1;
        int accSize = element.size;

        for (int i = index + 1; endIndex < statements.size() && i < elements.size(); ++endIndex, ++i) {
            StatementListItem statement = statements.get(endIndex);
            StatementElement elem = elements.get(i);
            if (statement == elem.node && accSize + elem.size < MAX_STATEMENT_SIZE) {
                accSize += elem.size;
            } else {
                break;
            }
        }
        if (startIndex + 1 == endIndex) {
            return false;
        }

        // compact multiple siblings and mark the next elements in list as empty
        List<StatementListItem> siblings = statements.subList(startIndex, endIndex);
        element.maybeExport(new ArrayList<>(siblings), accSize);
        Collections.fill(elements.subList(index + 1, index + siblings.size()),
                StatementElement.EMPTY);

        return true;
    }

    private static final class StatementUpdater extends
            DefaultVoidNodeVisitor<Entry<StatementListItem, StatementListMethod>> {
        private List<StatementListItem> updateStatements(List<StatementListItem> statements,
                Entry<StatementListItem, StatementListMethod> entry) {
            List<StatementListItem> newStatements = new ArrayList<>(statements);
            int index = newStatements.indexOf(entry.getKey());
            assert index != -1;

            StatementListMethod targetElement = entry.getValue();
            int range = targetElement.getStatements().size();
            if (range == 1) {
                newStatements.set(index, targetElement);
            } else {
                newStatements.subList(index, index + range).clear();
                newStatements.add(index, targetElement);
            }

            return newStatements;
        }

        @Override
        protected void visit(Node node, Entry<StatementListItem, StatementListMethod> entry) {
            throw new IllegalStateException("unhandled node: " + node.getClass());
        }

        @Override
        protected void visit(Statement node, Entry<StatementListItem, StatementListMethod> entry) {
            throw new IllegalStateException("unhandled statement: " + node.getClass());
        }

        @Override
        public void visit(BlockStatement node, Entry<StatementListItem, StatementListMethod> entry) {
            node.setStatements(updateStatements(node.getStatements(), entry));
        }

        @Override
        public void visit(IfStatement node, Entry<StatementListItem, StatementListMethod> entry) {
            if (entry.getKey() == node.getThen()) {
                node.setThen(entry.getValue());
            } else {
                assert entry.getKey() == node.getOtherwise();
                node.setOtherwise(entry.getValue());
            }
        }

        @Override
        public void visit(SwitchClause node, Entry<StatementListItem, StatementListMethod> entry) {
            node.setStatements(updateStatements(node.getStatements(), entry));
        }
    }

    @SuppressWarnings("serial")
    private static final class RangeArrayList<E> extends ArrayList<E> {
        public void replaceRange(E element, int fromIndex, int toIndex) {
            if (fromIndex == toIndex) {
                add(fromIndex, element);
            } else {
                set(fromIndex, element);
                removeRange(fromIndex + 1, toIndex);
            }
        }
    }

    private static final class FindExportableStatement extends
            DefaultNodeVisitor<Boolean, ArrayDeque<Node>> {
        RangeArrayList<StatementListItem> exportable = new RangeArrayList<>();
        RangeArrayList<Node> parents = new RangeArrayList<>();
        HashSet<Node> nonExportable = new HashSet<>();
        Map<StatementListItem, Integer> codeSizes;

        public FindExportableStatement(Map<StatementListItem, Integer> codeSizes) {
            this.codeSizes = codeSizes;
        }

        private boolean export(StatementListItem node, ArrayDeque<Node> stack) {
            // always exportable to sub-method
            exportable.add(node);
            parents.add(stack.peek());
            return true;
        }

        private boolean neverExport(Node node) {
            // never exportable to sub-method
            return nonExportable.add(node);
        }

        private boolean neverExport(List<? extends Node> nodes) {
            // never exportable to sub-method
            return nonExportable.addAll(nodes);
        }

        private boolean isExportable(Node node) {
            return !nonExportable.contains(node);
        }

        private boolean findTarget(Statement breakOrContinue, String label, ArrayDeque<Node> stack) {
            neverExport(breakOrContinue);
            for (Iterator<Node> iter = stack.iterator(); iter.hasNext();) {
                Node stmt = iter.next();
                if (stmt instanceof BreakableStatement) {
                    if (label == null || ((BreakableStatement) stmt).getLabelSet().contains(label)) {
                        break;
                    }
                } else if (stmt instanceof LabelledStatement) {
                    if (label != null && ((LabelledStatement) stmt).getLabelSet().contains(label)) {
                        break;
                    }
                }
                neverExport(stmt);
            }

            // never exportable to sub-method
            return false;
        }

        private int push(Node node, ArrayDeque<Node> stack) {
            int startOffset = exportable.size();
            stack.push(node);
            return startOffset;
        }

        private boolean pop(Node node, ArrayDeque<Node> stack, int startOffset) {
            Node value = stack.pop();
            assert node == value;
            if (isExportable(node) && codeSizes.get(node) < MAX_SIZE) {
                // remove range [startOffset, currentOffset]
                int currentOffset = exportable.size();
                assert node instanceof StatementListItem : node.getClass();
                assert startOffset <= currentOffset && currentOffset == parents.size();
                exportable.replaceRange((StatementListItem) node, startOffset, currentOffset);
                parents.replaceRange(stack.peek(), startOffset, currentOffset);
                return true;
            }
            return false;
        }

        /* -------------------------------------------------------------------------------------- */

        private boolean visit(Node node, Node child, ArrayDeque<Node> stack) {
            int startOffset = push(node, stack);
            child.accept(this, stack);
            return pop(node, stack, startOffset);
        }

        private boolean visit(Node node, Node left, Node right, ArrayDeque<Node> stack) {
            int startOffset = push(node, stack);
            left.accept(this, stack);
            if (right != null) {
                right.accept(this, stack);
            }
            return pop(node, stack, startOffset);
        }

        private boolean visit(Node node, Node left, Node middle, Node right,
                List<? extends Node> children, ArrayDeque<Node> stack) {
            int startOffset = push(node, stack);
            left.accept(this, stack);
            if (middle != null) {
                middle.accept(this, stack);
            }
            if (right != null) {
                right.accept(this, stack);
            }
            for (Node child : children) {
                child.accept(this, stack);
            }
            return pop(node, stack, startOffset);
        }

        private boolean visit(Node node, List<? extends Node> children, ArrayDeque<Node> stack) {
            int startOffset = push(node, stack);
            for (Node child : children) {
                child.accept(this, stack);
            }
            return pop(node, stack, startOffset);
        }

        /* -------------------------------------------------------------------------------------- */

        @Override
        protected Boolean visit(Node node, ArrayDeque<Node> stack) {
            throw new IllegalStateException("unhandled node: " + node.getClass());
        }

        @Override
        protected Boolean visit(Statement node, ArrayDeque<Node> stack) {
            throw new IllegalStateException("unhandled statement: " + node.getClass());
        }

        @Override
        protected Boolean visit(Declaration node, ArrayDeque<Node> stack) {
            return export(node, stack);
        }

        @Override
        public Boolean visit(BlockStatement node, ArrayDeque<Node> stack) {
            return visit(node, node.getStatements(), stack);
        }

        @Override
        public Boolean visit(BreakStatement node, ArrayDeque<Node> stack) {
            return findTarget(node, node.getLabel(), stack);
        }

        @Override
        public Boolean visit(CatchNode node, ArrayDeque<Node> stack) {
            return node.getCatchBlock().accept(this, stack);
        }

        @Override
        public Boolean visit(ContinueStatement node, ArrayDeque<Node> stack) {
            return findTarget(node, node.getLabel(), stack);
        }

        @Override
        public Boolean visit(DebuggerStatement node, ArrayDeque<Node> stack) {
            return export(node, stack);
        }

        @Override
        public Boolean visit(DoWhileStatement node, ArrayDeque<Node> stack) {
            return visit(node, node.getStatement(), stack);
        }

        @Override
        public Boolean visit(EmptyStatement node, ArrayDeque<Node> stack) {
            return neverExport(node);
        }

        @Override
        public Boolean visit(ExpressionStatement node, ArrayDeque<Node> stack) {
            return export(node, stack);
        }

        @Override
        public Boolean visit(ForEachStatement node, ArrayDeque<Node> stack) {
            return visit(node, node.getStatement(), stack);
        }

        @Override
        public Boolean visit(ForInStatement node, ArrayDeque<Node> stack) {
            return visit(node, node.getStatement(), stack);
        }

        @Override
        public Boolean visit(ForOfStatement node, ArrayDeque<Node> stack) {
            return visit(node, node.getStatement(), stack);
        }

        @Override
        public Boolean visit(ForStatement node, ArrayDeque<Node> stack) {
            return visit(node, node.getStatement(), stack);
        }

        @Override
        public Boolean visit(GuardedCatchNode node, ArrayDeque<Node> stack) {
            return node.getCatchBlock().accept(this, stack);
        }

        @Override
        public Boolean visit(IfStatement node, ArrayDeque<Node> stack) {
            return visit(node, node.getThen(), node.getOtherwise(), stack);
        }

        @Override
        public Boolean visit(LabelledFunctionStatement node, ArrayDeque<Node> stack) {
            return visit(node, node.getFunction(), stack);
        }

        @Override
        public Boolean visit(LabelledStatement node, ArrayDeque<Node> stack) {
            return visit(node, node.getStatement(), stack);
        }

        @Override
        public Boolean visit(LetStatement node, ArrayDeque<Node> stack) {
            return visit(node, node.getStatement(), stack);
        }

        @Override
        public Boolean visit(ReturnStatement node, ArrayDeque<Node> stack) {
            return export(node, stack);
        }

        @Override
        public Boolean visit(StatementListMethod node, ArrayDeque<Node> stack) {
            // don't re-export already exported statements
            return neverExport(node);
        }

        @Override
        public Boolean visit(SwitchClause node, ArrayDeque<Node> stack) {
            // don't export individual switch-clauses
            neverExport(node);
            return visit(node, node.getStatements(), stack);
        }

        @Override
        public Boolean visit(SwitchStatement node, ArrayDeque<Node> stack) {
            return visit(node, node.getClauses(), stack);
        }

        @Override
        public Boolean visit(ThrowStatement node, ArrayDeque<Node> stack) {
            return export(node, stack);
        }

        @Override
        public Boolean visit(TryStatement node, ArrayDeque<Node> stack) {
            // don't export individual try/catch/finally blocks
            neverExport(node.getTryBlock());
            neverExport(node.getGuardedCatchNodes());
            if (node.getCatchNode() != null) {
                neverExport(node.getCatchNode().getCatchBlock());
            }
            if (node.getFinallyBlock() != null) {
                neverExport(node.getFinallyBlock());
            }
            return visit(node, node.getTryBlock(), node.getCatchNode(), node.getFinallyBlock(),
                    node.getGuardedCatchNodes(), stack);
        }

        @Override
        public Boolean visit(VariableStatement node, ArrayDeque<Node> stack) {
            return export(node, stack);
        }

        @Override
        public Boolean visit(WhileStatement node, ArrayDeque<Node> stack) {
            return visit(node, node.getStatement(), stack);
        }

        @Override
        public Boolean visit(WithStatement node, ArrayDeque<Node> stack) {
            return visit(node, node.getStatement(), stack);
        }
    }
}

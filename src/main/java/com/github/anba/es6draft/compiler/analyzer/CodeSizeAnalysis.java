/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.github.anba.es6draft.ast.*;

/**
 * Analyzes code size and possibly splits statements or expressions into sub-methods to avoid
 * compilation errors due to excess byte code size. Byte code size is limited to 64K.
 */
public class CodeSizeAnalysis implements AutoCloseable {
    private static final int MAX_SIZE = 65535;
    private static final int MAX_SIZE_ALLOWED = MAX_SIZE / 2;

    private ExecutorService executor = Executors.newFixedThreadPool(2);
    private LinkedBlockingQueue<Future<Integer>> queue = new LinkedBlockingQueue<>();

    /**
     * Start method
     */
    public void submit(Script script) {
        submit(script, script.getStatements());
        drainQueue();
    }

    /**
     * Start method
     */
    public void submit(FunctionNode function) {
        submit(function, function.getStatements());
        drainQueue();
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private void submit(TopLevelNode node, List<? extends Node> children) {
        queue.add(executor.submit(new Entry(node, children)));
    }

    private void drainQueue() {
        try {
            while (!queue.isEmpty()) {
                queue.take().get();
            }
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private class Entry implements Callable<Integer> {
        private TopLevelNode node;
        private List<? extends Node> children;

        Entry(TopLevelNode node, List<? extends Node> children) {
            this.node = node;
            this.children = children;
        }

        @Override
        public Integer call() throws Exception {
            CodeSizeVisitor visitor = new CodeSizeVisitor();
            CodeSizeHandler handler = new CodeSizeHandlerImpl(node);
            return visitor.startAnalyze(node, children, handler);
        }
    }

    private class CodeSizeHandlerImpl extends DefaultNodeVisitor<Integer, Integer> implements
            CodeSizeHandler {
        private final TopLevelNode topLevelNode;

        public CodeSizeHandlerImpl(TopLevelNode topLevelNode) {
            this.topLevelNode = topLevelNode;
        }

        @Override
        public int reportSize(Node node, int size) {
            if (size > MAX_SIZE_ALLOWED) {
                // System.out.printf("reportSize(%s, %d)%n", node, size);
                topLevelNode.setSyntheticNodes(true);
                return node.accept(this, size);
            }
            return size;
        }

        @Override
        public void submit(TopLevelNode node, List<? extends Node> children) {
            CodeSizeAnalysis.this.submit(node, children);
        }

        private <NODE extends Node> int visit(NODE node, int size, SubMethod<NODE> submethod) {
            return submethod.processNode(node, size);
        }

        @Override
        protected Integer visit(Node node, Integer size) {
            throw new IllegalStateException("unhandled node: " + node.getClass());
        }

        @Override
        public Integer visit(ArrayLiteral node, Integer size) {
            return visit(node, size, new ArrayLiteralSubMethod());
        }

        @Override
        public Integer visit(ArrowFunction node, Integer size) {
            if (node.getExpression() != null) {
                return super.visit(node, size);
            }
            return visit(node, size, new TopLevelSubMethod.FunctionSubMethod());
        }

        @Override
        public Integer visit(BinaryExpression node, Integer size) {
            return visit(node, size, new BinaryExpressionSubMethod());
        }

        @Override
        public Integer visit(CallExpression node, Integer size) {
            throw new CodeSizeException(size);
        }

        @Override
        public Integer visit(CommaExpression node, Integer size) {
            return visit(node, size, new CommaExpressionSubMethod());
        }

        @Override
        public Integer visit(FunctionDeclaration node, Integer size) {
            return visit(node, size, new TopLevelSubMethod.FunctionSubMethod());
        }

        @Override
        public Integer visit(FunctionExpression node, Integer size) {
            return visit(node, size, new TopLevelSubMethod.FunctionSubMethod());
        }

        @Override
        public Integer visit(Comprehension node, Integer size) {
            throw new CodeSizeException(size);
        }

        @Override
        public Integer visit(GeneratorDeclaration node, Integer size) {
            return visit(node, size, new TopLevelSubMethod.FunctionSubMethod());
        }

        @Override
        public Integer visit(GeneratorExpression node, Integer size) {
            return visit(node, size, new TopLevelSubMethod.FunctionSubMethod());
        }

        @Override
        public Integer visit(LegacyGeneratorDeclaration node, Integer size) {
            return visit(node, size, new TopLevelSubMethod.FunctionSubMethod());
        }

        @Override
        public Integer visit(LegacyGeneratorExpression node, Integer size) {
            return visit(node, size, new TopLevelSubMethod.FunctionSubMethod());
        }

        @Override
        public Integer visit(MethodDefinition node, Integer size) {
            return visit(node, size, new TopLevelSubMethod.FunctionSubMethod());
        }

        @Override
        public Integer visit(ObjectLiteral node, Integer size) {
            return visit(node, size, new ObjectLiteralSubMethod());
        }

        @Override
        public Integer visit(Script node, Integer size) {
            return visit(node, size, new TopLevelSubMethod.ScriptSubMethod());
        }

        @Override
        protected Integer visit(Statement node, Integer size) {
            return visit(node, size, new NestedSubMethod.StatementSubMethod());
        }

        @Override
        public Integer visit(SuperExpression node, Integer size) {
            throw new CodeSizeException(size);
        }

        @Override
        public Integer visit(SwitchClause node, Integer size) {
            return visit(node, size, new NestedSubMethod.SwitchClauseSubMethod());
        }

        @Override
        public Integer visit(TemplateLiteral node, Integer size) {
            throw new CodeSizeException(size);
        }
    }
}

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
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;

import com.github.anba.es6draft.ast.*;

/**
 * Analyzes code size and possibly splits statements or expressions into sub-methods to avoid
 * compilation errors due to excess byte code size. Byte code size is limited to 64K.
 */
public final class CodeSizeAnalysis {
    private static final int MAX_SIZE = 65535;
    private static final int MAX_SIZE_ALLOWED = MAX_SIZE / 2;

    private final ExecutorService executor;
    private final LinkedBlockingQueue<Future<Integer>> queue = new LinkedBlockingQueue<>();
    private final ReentrantLock submitLock = new ReentrantLock();
    private boolean cancelled = false;

    public CodeSizeAnalysis(ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Start method.
     * 
     * @param script
     *            the script to be analyzed
     */
    public void submit(Script script) throws CodeSizeException {
        submitAndExec(script, script.getStatements());
        drainQueue();
    }

    /**
     * Start method.
     * 
     * @param function
     *            the function node to be analyzed
     */
    public void submit(FunctionNode function) throws CodeSizeException {
        submitAndExec(function, function.getStatements());
        drainQueue();
    }

    private void submitAndExec(TopLevelNode<?> node, List<? extends Node> children) {
        // Execute the initial node on the main thread to avoid unnecessary thread creation
        new Entry(node, children).call();
    }

    private void submit(TopLevelNode<?> node, List<? extends Node> children) {
        if (!isCancelled()) {
            queue.add(executor.submit(new Entry(node, children)));
        }
    }

    private boolean isCancelled() {
        final ReentrantLock submitLock = this.submitLock;
        submitLock.lock();
        try {
            return cancelled;
        } finally {
            submitLock.unlock();
        }
    }

    private void drainQueue() {
        boolean abrupt = true;
        try {
            LinkedBlockingQueue<Future<Integer>> queue = this.queue;
            while (!queue.isEmpty()) {
                queue.take().get();
            }
            abrupt = false;
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
        } finally {
            if (abrupt) {
                purgeQueue();
            }
        }
    }

    private void purgeQueue() {
        final ReentrantLock submitLock = this.submitLock;
        submitLock.lock();
        try {
            cancelled = true;
            // Cancel pending tasks
            LinkedBlockingQueue<Future<Integer>> queue = this.queue;
            for (Future<Integer> task : queue) {
                task.cancel(true);
            }
            // Purge work queue
            ExecutorService executor = this.executor;
            if (executor instanceof ThreadPoolExecutor) {
                ((ThreadPoolExecutor) executor).purge();
            }
        } finally {
            submitLock.unlock();
        }
    }

    private final class Entry implements Callable<Integer> {
        private TopLevelNode<?> node;
        private List<? extends Node> children;

        Entry(TopLevelNode<?> node, List<? extends Node> children) {
            this.node = node;
            this.children = children;
        }

        @Override
        public Integer call() {
            CodeSizeVisitor visitor = new CodeSizeVisitor();
            CodeSizeHandler handler = new CodeSizeHandlerImpl(node);
            return visitor.startAnalyze(node, children, handler);
        }
    }

    private final class CodeSizeHandlerImpl extends DefaultNodeVisitor<Integer, Integer> implements
            CodeSizeHandler {
        private final TopLevelNode<?> topLevelNode;

        public CodeSizeHandlerImpl(TopLevelNode<?> topLevelNode) {
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
        public void submit(TopLevelNode<?> node, List<? extends Node> children) {
            CodeSizeAnalysis.this.submit(node, children);
        }

        private <NODE extends Node> int visit(NODE node, int size, SubMethod<NODE> submethod) {
            return submethod.processNode(node, size);
        }

        @Override
        protected Integer visit(Node node, Integer size) {
            throw new CodeSizeException(size);
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
        public Integer visit(AsyncFunctionDeclaration node, Integer size) {
            return visit(node, size, new TopLevelSubMethod.FunctionSubMethod());
        }

        @Override
        public Integer visit(AsyncFunctionExpression node, Integer size) {
            return visit(node, size, new TopLevelSubMethod.FunctionSubMethod());
        }

        @Override
        public Integer visit(BinaryExpression node, Integer size) {
            return visit(node, size, new BinaryExpressionSubMethod());
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
        public Integer visit(SwitchClause node, Integer size) {
            return visit(node, size, new NestedSubMethod.SwitchClauseSubMethod());
        }

        @Override
        public Integer visit(TemplateLiteral node, Integer size) {
            if (node.isTagged()) {
                return super.visit(node, size);
            }
            return visit(node, size, new TemplateLiteralSubMethod());
        }
    }
}

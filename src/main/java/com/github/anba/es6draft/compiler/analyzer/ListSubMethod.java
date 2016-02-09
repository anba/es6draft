/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Function;

import com.github.anba.es6draft.ast.Node;

/**
 * Abstract class for list-based node re-writing
 */
abstract class ListSubMethod<N extends Node> extends SubMethod<N> {
    @FunctionalInterface
    protected interface NodeElementMapper<NODE extends Node, ELEMENT> {
        ELEMENT map(NODE node, int size, int index);

        default ArrayList<ELEMENT> toList(List<NODE> nodes) {
            CodeSizeVisitor visitor = new CodeSizeVisitor();
            CodeSizeHandler handler = new EmptyHandler();
            ArrayList<ELEMENT> list = new ArrayList<>(nodes.size());
            for (int i = 0, len = nodes.size(); i < len; i++) {
                NODE node = nodes.get(i);
                int size = node.accept(visitor, handler);
                list.add(map(node, size, i));
            }
            return list;
        }
    }

    protected final <NODE extends Node, ELEMENT extends NodeElement<NODE>> List<NODE> newNodes(int oldSize,
            List<NODE> oldNodes, NodeElementMapper<NODE, ELEMENT> mapper, Conflater<ELEMENT, NODE> conflater,
            int maxElementSize, int maxAccSize, int maxConflateSize) {
        return newNodes(oldSize, oldNodes, mapper::toList, conflater, maxElementSize, maxAccSize, maxConflateSize);
    }

    protected final <NODE extends Node, ELEMENT extends NodeElement<NODE>> List<NODE> newNodes(int oldSize,
            List<NODE> oldNodes, NodeElementMapper<NODE, ELEMENT> mapper, Conflater<ELEMENT, NODE> conflater,
            int maxAccSize, int maxConflateSize) {
        return newNodes(oldSize, oldNodes, mapper::toList, conflater, maxAccSize, maxConflateSize);
    }

    protected final <NODE extends Node, ELEMENT extends NodeElement<NODE>> List<NODE> newNodes(int oldSize,
            List<NODE> oldNodes, Function<ArrayList<NODE>, ArrayList<ELEMENT>> mapper,
            Conflater<ELEMENT, NODE> conflater, int maxElementSize, int maxAccSize, int maxConflateSize) {
        ArrayList<NODE> newNodes = new ArrayList<NODE>(oldNodes);
        int accSize = replaceLarge(oldSize, newNodes, mapper, maxElementSize);
        return newNodes(accSize, newNodes, mapper, conflater, maxAccSize, maxConflateSize);
    }

    protected final <NODE extends Node, ELEMENT extends NodeElement<NODE>> List<NODE> newNodes(int oldSize,
            List<NODE> oldNodes, Function<ArrayList<NODE>, ArrayList<ELEMENT>> mapper,
            Conflater<ELEMENT, NODE> conflater, int maxAccSize, int maxConflateSize) {
        int accSize = oldSize;
        if (accSize > maxAccSize) {
            return subdivide(oldNodes, mapper, conflater, maxConflateSize);
        }
        return oldNodes;
    }

    /**
     * Replaces large elements with methods.
     * 
     * @param oldSize
     *            the old accumulated size
     * @param nodes
     *            the nodes (modified when elements are replaced)
     * @param mapper
     *            the {@code (node -> element)} mapper function
     * @param maxElementSize
     *            the maximum size per element
     * @return the new accumulated size
     */
    protected final <NODE extends Node, ELEMENT extends NodeElement<NODE>> int replaceLarge(int oldSize,
            ArrayList<NODE> nodes, Function<ArrayList<NODE>, ArrayList<ELEMENT>> mapper, int maxElementSize) {
        int accSize = oldSize;
        ArrayList<ELEMENT> elements = mapper.apply(nodes);
        PriorityQueue<ELEMENT> pq = new PriorityQueue<>(elements);
        while (!pq.isEmpty() && pq.peek().getSize() > maxElementSize) {
            // Export and update entry.
            ELEMENT element = pq.remove();
            accSize += element.export();
            nodes.set(element.getIndex(), element.getNode());
        }
        return accSize;
    }
}

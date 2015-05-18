/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.Locale;

import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.ast.Module;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.ast.scope.Scope;
import com.github.anba.es6draft.ast.scope.ScriptScope;
import com.github.anba.es6draft.compiler.assembler.SourceInfo;
import com.github.anba.es6draft.runtime.internal.Source;

/**
 *
 */
final class NodeSourceInfo {
    private NodeSourceInfo() {
    }

    static SourceInfo create(Script script, EnumSet<Compiler.Option> compilerOptions) {
        return new ScriptSourceInfo(script, compilerOptions.contains(Compiler.Option.SourceMap));
    }

    static SourceInfo create(Module module, EnumSet<Compiler.Option> compilerOptions) {
        return new ModuleSourceInfo(module, compilerOptions.contains(Compiler.Option.SourceMap));
    }

    static SourceInfo create(FunctionNode function, EnumSet<Compiler.Option> compilerOptions) {
        return new FunctionSourceInfo(function, compilerOptions.contains(Compiler.Option.SourceMap));
    }

    private static final class ScriptSourceInfo implements SourceInfo {
        private final Script script;
        private final boolean includeSourceMap;

        ScriptSourceInfo(Script script, boolean includeSourceMap) {
            this.script = script;
            this.includeSourceMap = includeSourceMap;
        }

        @Override
        public String getFileName() {
            return script.getSource().getName();
        }

        @Override
        public String getSourceMap() {
            return sourceMap(script, script.getSource(), includeSourceMap);
        }
    }

    private static final class ModuleSourceInfo implements SourceInfo {
        private final Module module;
        private final boolean includeSourceMap;

        ModuleSourceInfo(Module module, boolean includeSourceMap) {
            this.module = module;
            this.includeSourceMap = includeSourceMap;
        }

        @Override
        public String getFileName() {
            return module.getSource().getName();
        }

        @Override
        public String getSourceMap() {
            return sourceMap(module, module.getSource(), includeSourceMap);
        }
    }

    private static final class FunctionSourceInfo implements SourceInfo {
        private final FunctionNode function;
        private final boolean includeSourceMap;

        FunctionSourceInfo(FunctionNode function, boolean includeSourceMap) {
            this.function = function;
            this.includeSourceMap = includeSourceMap;
        }

        @Override
        public String getFileName() {
            // return "<Function>";
            return functionScript(function).getSource().getName();
        }

        @Override
        public String getSourceMap() {
            Script script = functionScript(function);
            return sourceMap(script, script.getSource(), includeSourceMap);
        }

        private static Script functionScript(FunctionNode function) {
            Scope enclosingScope = function.getScope().getEnclosingScope();
            assert enclosingScope instanceof ScriptScope;
            return ((ScriptScope) enclosingScope).getNode();
        }
    }

    private static String sourceMap(Node node, Source source, boolean includeSourceMap) {
        if (!includeSourceMap) {
            return null;
        }
        Path sourceFile = source.getFile();
        if (sourceFile == null) {
            // return if 'sourceFile' is not available
            return null;
        }
        Path relativePath = Paths.get("").toAbsolutePath().relativize(sourceFile);

        try (Formatter smap = new Formatter(Locale.ROOT)) {
            // Header
            // - ID
            smap.format("SMAP%n");
            // - OutputFileName
            smap.format("%s%n", source.getName());
            // - DefaultStratumId
            smap.format("Script%n");
            // Section
            // - StratumSection
            smap.format("*S Script%n");
            // - FileSection
            smap.format("*F%n");
            // -- FileInfo
            smap.format("+ 1 %s%n%s%n", sourceFile.getFileName(), relativePath);
            // - LineSection
            smap.format("*L%n");
            // -- LineInfo
            smap.format("%d#1,%d:%d%n", node.getBeginLine(), node.getEndLine(), node.getBeginLine());
            // EndSection
            smap.format("*E%n");

            return smap.toString();
        }
    }
}

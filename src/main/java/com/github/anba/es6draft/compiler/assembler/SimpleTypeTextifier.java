/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.Textifier;

/**
 * Modified {@link Textifier} to strip package names from class names.
 */
public final class SimpleTypeTextifier extends Textifier {
    public SimpleTypeTextifier() {
        super(Opcodes.ASM5);
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        this.print(pw);
        pw.flush();
        return sw.toString();
    }

    @Override
    protected Textifier createTextifier() {
        return new SimpleTypeTextifier();
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        buf.setLength(0);
        buf.append(tab2).append("INVOKEDYNAMIC").append(' ').append(name);
        appendDescriptor(METHOD_DESCRIPTOR, desc);
        if (bsmArgs.length != 0) {
            buf.append(" [");
            for (int i = 0; i < bsmArgs.length; ++i) {
                Object arg = bsmArgs[i];
                if (arg instanceof String) {
                    appendString(buf, (String) arg);
                } else if (arg instanceof Type) {
                    Type type = (Type) arg;
                    if (type.getSort() == Type.METHOD) {
                        appendDescriptor(METHOD_DESCRIPTOR, type.getDescriptor());
                    } else {
                        appendDescriptor(INTERNAL_NAME, type.getDescriptor());
                    }
                } else if (arg instanceof Handle) {
                    Handle handle = (Handle) arg;
                    appendDescriptor(HANDLE_DESCRIPTOR, getMethodDescriptor(handle.getDesc()));
                } else {
                    buf.append(arg);
                }
                if (i + 1 < bsmArgs.length) {
                    buf.append(", ");
                }
            }
            buf.append("]");
        }
        buf.append("\n");

        text.add(buf.toString());
    }

    @Override
    public void visitLdcInsn(Object cst) {
        if (cst instanceof Handle) {
            Handle handle = (Handle) cst;
            handle = new Handle(handle.getTag(), handle.getOwner(), handle.getName(),
                    getMethodDescriptor(handle.getDesc()));
            buf.setLength(0);
            buf.append(tab2).append("LDC ");
            buf.append(handle.getOwner()).append('.').append(handle.getName()).append(handle.getDesc());
            buf.append('\n');
            text.add(buf.toString());
            return;
        }
        super.visitLdcInsn(cst);
    }

    @Override
    protected void appendDescriptor(int type, String desc) {
        switch (type) {
        case INTERNAL_NAME:
            if (desc != null) {
                desc = getInternalName(desc);
            }
            break;
        case FIELD_DESCRIPTOR:
            desc = getDescriptor(desc);
            break;
        case METHOD_DESCRIPTOR:
            desc = getMethodDescriptor(desc);
            break;
        case HANDLE_DESCRIPTOR:
            desc = getMethodDescriptor(desc);
            break;
        }
        super.appendDescriptor(type, desc);
    }

    private String getDescriptor(Type type) {
        if (type.getSort() == Type.OBJECT) {
            String name = type.getInternalName();
            int index = name.lastIndexOf('/');
            return name.substring(index + 1);
        }
        if (type.getSort() == Type.ARRAY) {
            StringBuilder sb = new StringBuilder(getDescriptor(type.getElementType()));
            for (int dim = type.getDimensions(); dim > 0; --dim) {
                sb.append("[]");
            }
            return sb.toString();
        }
        return type.getClassName();
    }

    private String getInternalName(String internalName) {
        return getDescriptor(Type.getObjectType(internalName));
    }

    private String getDescriptor(String typeDescriptor) {
        return getDescriptor(Type.getType(typeDescriptor));
    }

    private String getMethodDescriptor(String methodDescriptor) {
        Type[] argumentTypes = Type.getArgumentTypes(methodDescriptor);
        Type returnType = Type.getReturnType(methodDescriptor);

        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0; i < argumentTypes.length; i++) {
            sb.append(getDescriptor(argumentTypes[i]));
            if (i + 1 < argumentTypes.length) {
                sb.append(", ");
            }
        }
        sb.append(')').append(getDescriptor(returnType));

        return sb.toString();
    }
}

package io.github.cspigot.utils.eventbusutil.writer;

import io.github.cspigot.utils.eventbusutil.util.TypeUtil;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;

import static org.objectweb.asm.Opcodes.*;

public class MethodAccessWriter extends MemberAccessWriter {

    protected static final String OBJECT_ARRAY_DESCRIPTOR = Type.getDescriptor(Object[].class);
    protected static final String CALLER_TYPE = "(L" + OBJECT_TYPE + ";)L" + OBJECT_TYPE + ";";

    protected Class returnType;
    protected Class[] parameters;

    public MethodAccessWriter(Method method) {
        super(method);
        returnType = method.getReturnType();
        parameters = method.getParameterTypes();

        visit(V1_8, ACC_PUBLIC + ACC_FINAL + ACC_SYNTHETIC, generatedName, null, OBJECT_TYPE, new String[]{ FUNCTION_TYPE });
        generateCall();
        visitEnd();
    }

    protected void generateCall() {
        MethodVisitor mv = visitMethod(ACC_PUBLIC + ACC_FINAL, FUNCTION_METHOD_NAME, CALLER_TYPE, null, null);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, OBJECT_ARRAY_DESCRIPTOR);
        mv.visitVarInsn(ASTORE, 1);
        if (!isStatic) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitInsn(ICONST_0);
            mv.visitInsn(AALOAD);
            mv.visitTypeInsn(CHECKCAST, declaringClassType);
        }
        castParams(mv);
        mv.visitMethodInsn(declaringClass.isInterface() ? INVOKEINTERFACE : isStatic ? INVOKESTATIC : INVOKEVIRTUAL, declaringClassType, name, TypeUtil.getMethodDescriptor(returnType, parameters), declaringClass.isInterface());
        if (returnType == Void.TYPE) {
            mv.visitInsn(ACONST_NULL);
        } else if (returnType.isPrimitive()) {
            TypeUtil.getPrimitive(returnType).toWrapper(mv);
        }
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    protected void castParams(MethodVisitor mv) {
        int var = isStatic ? 0 : 1;
        for (Class parameter : parameters) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn(var++);
            mv.visitInsn(AALOAD);
            if (parameter.isPrimitive()) {
                TypeUtil.Primitive primitive = TypeUtil.getPrimitive(parameter);
                mv.visitTypeInsn(CHECKCAST, Type.getInternalName(primitive.getWrapper()));
                primitive.toPrimitive(mv);
            } else {
                mv.visitTypeInsn(CHECKCAST, Type.getInternalName(parameter));
            }
        }
    }

}

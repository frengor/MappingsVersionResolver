package com.frengor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class CraftMagicNumbersVisitor extends ClassVisitor {

    private boolean methodFound;
    private String version;

    public CraftMagicNumbersVisitor(int api) {
        super(api);
    }

    public CraftMagicNumbersVisitor(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        if ("getMappingsVersion".equals(name) && "()Ljava/lang/String;".equals(desc)) {
            methodFound = true;
            return new MethodVisitor(api, mv) {
                @Override
                public void visitLdcInsn(Object value) {
                    super.visitLdcInsn(value);

                    if (version != null) {
                        throw new RuntimeException("Too many ldc instructions in getMappingsVersion()");
                    }

                    if (value instanceof String) {
                        version = (String) value;
                    } else {
                        throw new RuntimeException("Invalid loaded object found, expected string: " + value);
                    }
                }
            };
        }
        return null;
    }

    public void visitEnd() {
        if (version == null) {
            if (methodFound) {
                throw new RuntimeException("Couldn't get the mappings version from getMappingsVersion()");
            } else {
                throw new RuntimeException("Couldn't find method getMappingsVersion()");
            }
        }
    }

    public String getVersion() {
        return version;
    }
}

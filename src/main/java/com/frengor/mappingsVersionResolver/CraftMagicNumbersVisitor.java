/*
 * Copyright 2023 fren_gor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frengor.mappingsVersionResolver;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * Utility to read the mappings version from the {@code CraftMagicNumbers#getMappingsVersion()} method.
 */
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

    /**
     * Gets the resolved version, or {@code null} if either not found or the visitor hasn't run yet.
     *
     * @return The resolved version, or {@code null} if either not found or the visitor hasn't run yet.
     */
    public String getVersion() {
        return version;
    }
}

package com.evolvedmind.nero.compat;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.ProtectionDomain;
import java.util.Properties;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class CompatibilityAgent {
    public static void premain(String argument, Instrumentation instrumentation) {
        activate(argument, instrumentation, false);
    }

    public static void agentmain(String argument, Instrumentation instrumentation) {
        activate(argument, instrumentation, true);
    }

    private static synchronized void activate(String argument, Instrumentation instrumentation, boolean live) {
        if (System.getProperty("nero.compat.active") != null) return;
        if (live && !System.getProperty("nero.compat.bindings", "").isEmpty()) {
            System.err.println("NERO_COMPAT_DISABLED attachment propagation requires startup loading");
            return;
        }
        try {
            Path profilePath = Path.of(System.getProperty("nero.compat.profile", argument == null ? "" : argument));
            Properties profile = new Properties();
            try (var input = Files.newInputStream(profilePath)) {
                profile.load(input);
            }
            if (!"240".equals(profile.getProperty("revision"))) {
                throw new IllegalArgumentException("Only the qualified revision 240 profile is supported");
            }
            Path agentDirectory = Path.of(CompatibilityAgent.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getParent();
            instrumentation.appendToBootstrapClassLoaderSearch(
                new JarFile(agentDirectory.resolve("nero-compatibility-bridge.jar").toFile()));
            ModelProbe.configure(profile);
            ModelTransformer transformer = new ModelTransformer(profile);
            if (live) {
                Class<?> player = null;
                Class<?> client = null;
                for (Class<?> type : instrumentation.getAllLoadedClasses()) {
                    if (type.getName().equals(profile.getProperty("player.class", "").replace('/', '.'))) player = type;
                    if (type.getName().equals(profile.getProperty("client.class"))) client = type;
                }
                if (player == null || client == null || !instrumentation.isModifiableClass(player)
                    || !transformer.originalMatches(client.getProtectionDomain(), client.getName())
                    || !transformer.originalMatches(player.getProtectionDomain(), player.getName())) {
                    ModelProbe.disable("live client does not match the qualified profile");
                    return;
                }
                if (!ModelProbe.qualifyClient(client)) return;
                instrumentation.addTransformer(transformer, true);
                instrumentation.retransformClasses(player);
            } else {
                instrumentation.addTransformer(transformer);
            }
            System.setProperty("nero.compat.active", "0.3");
            ModelProbe.event("agent-ready", "version=0.3 revision=240 particleRendering=opt-in-isolated-plugin");
        } catch (Exception error) {
            System.err.println("NERO_COMPAT_DISABLED " + error.getClass().getSimpleName());
        }
    }

    static byte[] patch(byte[] original, String name, String descriptor) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        int[] matches = {0};
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String methodName, String methodDescriptor,
                    String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, methodName, methodDescriptor, signature, exceptions);
                if (!methodName.equals(name) || !methodDescriptor.equals(descriptor)) return delegate;
                if ((access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
                    throw new IllegalArgumentException("Model hook must be a concrete instance method");
                }
                matches[0]++;
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.ARETURN) {
                            super.visitInsn(Opcodes.DUP);
                            super.visitVarInsn(Opcodes.ALOAD, 0);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                "com/evolvedmind/nero/compat/ModelProbe", "sample",
                                "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
                        }
                        super.visitInsn(opcode);
                    }
                };
            }
        }, 0);
        if (matches[0] != 1) throw new IllegalArgumentException("Expected exactly one model hook");
        return writer.toByteArray();
    }

    static String hash(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder result = new StringBuilder();
        for (byte value : digest) result.append(String.format("%02x", value & 255));
        return result.toString();
    }

    static byte[] patchHd(byte[] original) {
        ClassReader reader=new ClassReader(original);ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
        int[] matches={0};
        reader.accept(new ClassVisitor(Opcodes.ASM9,writer) {
            public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] errors) {
                MethodVisitor delegate=super.visitMethod(access,name,desc,sig,errors);
                if(!name.equals("scenePass")||!desc.equals("()V"))return delegate;
                matches[0]++;
                return new MethodVisitor(Opcodes.ASM9,delegate) {
                    public void visitInsn(int op) {
                        if(op==Opcodes.RETURN) {
                            super.visitVarInsn(Opcodes.ALOAD,0);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,"com/evolvedmind/nero/compat/ParticleRuntime","hdScene","(Ljava/lang/Object;)V",false);
                        }
                        super.visitInsn(op);
                    }
                };
            }
        },0);
        if(matches[0]!=1)throw new IllegalArgumentException("Expected one qualified HD scene pass");
        return writer.toByteArray();
    }

    static byte[] patchScene(byte[] original) {
        ClassReader reader=new ClassReader(original);
        ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
        int[] matches={0};
        reader.accept(new ClassVisitor(Opcodes.ASM9,writer) {
            public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] errors) {
                MethodVisitor delegate=super.visitMethod(access,name,desc,sig,errors);
                if(!name.equals("dh")||!desc.equals("(IIILup;Lup;IIIZ)V"))return delegate;
                return new MethodVisitor(Opcodes.ASM9,delegate) {
                    public void visitCode() {
                        super.visitCode();
                        super.visitVarInsn(Opcodes.ALOAD,0);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC,"com/evolvedmind/nero/compat/ParticleRuntime","beginScene","(Ljava/lang/Object;)V",false);
                    }
                    public void visitMethodInsn(int op,String owner,String method,String descriptor,boolean itf) {
                        if(owner.equals("net/runelite/api/hooks/Callbacks")&&method.equals("drawScene")&&descriptor.equals("()V")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,"com/evolvedmind/nero/compat/ParticleRuntime","scene","()V",false);
                            matches[0]++;
                        }
                        super.visitMethodInsn(op,owner,method,descriptor,itf);
                    }
                };
            }
        },0);
        if(matches[0]!=3)throw new IllegalArgumentException("Unexpected native scene callbacks");
        return writer.toByteArray();
    }

    static byte[] patchAttachments(byte[] original, String className, String dataClass, String modelClass) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access,name,descriptor,signature,exceptions);
                if(className.equals(modelClass)&&name.equals("be")&&descriptor.equals("(IIIIIII)V")) {
                    return new MethodVisitor(Opcodes.ASM9,delegate) {
                        public void visitCode() {
                            super.visitCode();super.visitVarInsn(Opcodes.ALOAD,0);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,"com/evolvedmind/nero/compat/ParticleRuntime","preview","(Ljava/lang/Object;)V",false);
                        }
                    };
                }
                if ((access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) return delegate;
                boolean decode = className.equals(dataClass) && name.equals("<init>") && descriptor.equals("([B)V");
                boolean merge = name.equals("<init>") && descriptor.equals("([L"+className+";I)V");
                boolean copy = name.equals("<init>") && (descriptor.equals("(L"+className+";)V")
                    || descriptor.equals("(L"+className+";ZZZZ)V"));
                boolean returnedModel = descriptor.endsWith(")L"+modelClass+";")
                    || descriptor.endsWith(")L"+dataClass+";");
                if (!decode && !merge && !copy && !returnedModel) return delegate;
                return new MethodVisitor(Opcodes.ASM9,delegate) {
                    @Override
                    public void visitInsn(int opcode) {
                        String registry = "com/evolvedmind/nero/compat/AttachmentRegistry";
                        if (opcode == Opcodes.RETURN && (decode || merge || copy)) {
                            super.visitVarInsn(Opcodes.ALOAD,0);
                            super.visitVarInsn(Opcodes.ALOAD,1);
                            if (merge) {
                                super.visitVarInsn(Opcodes.ILOAD,2);
                                super.visitMethodInsn(Opcodes.INVOKESTATIC,registry,"merged",
                                    "(Ljava/lang/Object;Ljava/lang/Object;I)V",false);
                            } else {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC,registry,decode ? "decoded" : "inherited",
                                    decode ? "(Ljava/lang/Object;[B)V" : "(Ljava/lang/Object;Ljava/lang/Object;)V",false);
                            }
                        }
                        if (opcode == Opcodes.ARETURN && returnedModel) {
                            super.visitInsn(Opcodes.DUP);
                            super.visitVarInsn(Opcodes.ALOAD,0);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,registry,"inherited",
                                "(Ljava/lang/Object;Ljava/lang/Object;)V",false);
                        }
                        super.visitInsn(opcode);
                    }
                };
            }
        },0);
        return writer.toByteArray();
    }

    static final class ModelTransformer implements ClassFileTransformer {
        private final Properties profile;
        ModelTransformer(Properties profile) { this.profile = profile; }

        boolean originalMatches(ProtectionDomain domain, String name) throws Exception {
            if (domain == null || domain.getCodeSource() == null) return false;
            Path source = Path.of(domain.getCodeSource().getLocation().toURI());
            byte[] original;
            if (Files.isDirectory(source)) {
                original = Files.readAllBytes(source.resolve(name.replace('.', '/') + ".class"));
            } else {
                try (JarFile jar = new JarFile(source.toFile())) {
                    var entry = jar.getJarEntry(name.replace('.', '/') + ".class");
                    if (entry == null) return false;
                    try (var input = jar.getInputStream(entry)) { original = input.readAllBytes(); }
                }
            }
            return hash(original).equals(profile.getProperty("class." + name.replace('.', '/') + ".sha256"));
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> redefining,
                ProtectionDomain domain, byte[] bytes) {
            String expected = profile.getProperty("class." + className + ".sha256");
            if (expected == null) return null;
            try {
                if(className.equals("rs117/hd/renderer/zone/ZoneRenderer")) {
                    Path hdJar=Path.of(domain.getCodeSource().getLocation().toURI());
                    if(!expected.equals(hash(bytes)) || !hash(Files.readAllBytes(hdJar)).equals(profile.getProperty("artifact.hd.sha256"))) {
                        ModelProbe.event("hd-particle-hook-refused","117 HD version does not match qualified artifact");return null;
                    }
                }
                if (!expected.equals(hash(bytes))
                    && (redefining == null || !originalMatches(domain, className))) {
                    ModelProbe.disable("class fingerprint mismatch: " + className);
                    return null;
                }
                ModelProbe.event("class-qualified", className);
                if(className.equals("rs117/hd/renderer/zone/ZoneRenderer")&&ParticleRuntime.optedIn()) {
                    ModelProbe.event("hd-particle-hook-installed",className);
                    return patchHd(bytes);
                }
                if(className.equals("ez")&&ParticleRuntime.optedIn()) {
                    byte[] patched=patchScene(bytes);
                    ModelProbe.event("particle-scene-hook-installed",className);
                    return patched;
                }
                String dataClass = profile.getProperty("modeldata.class", "er");
                String modelClass = profile.getProperty("model.class", "fx");
                if (className.equals(dataClass) || className.equals(modelClass)) {
                    if (System.getProperty("nero.compat.bindings", "").isEmpty()) return null;
                    byte[] patched = patchAttachments(bytes,className,dataClass,modelClass);
                    ModelProbe.event("attachment-hooks-installed",className);
                    return patched;
                }
                boolean npc=className.equals(profile.getProperty("npc.class"));
                if (!npc && !className.equals(profile.getProperty("player.class"))) return null;
                byte[] patched = patch(bytes, profile.getProperty(npc?"npc.method":"player.method"),
                    profile.getProperty(npc?"npc.descriptor":"player.descriptor"));
                ModelProbe.event("model-hook-installed", className);
                return patched;
            } catch (Throwable error) {
                ModelProbe.disable("hook installation failed: " + error.getClass().getSimpleName());
                return null;
            }
        }
    }
}

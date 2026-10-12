import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;

/** Test harness only: constrain a copied gamepack's game sockets to our separate test server. */
public class NativeClientCopy {
 public static void main(String[] args) throws Exception {
  Path source=Path.of(args[0]),dest=Path.of(args[1]);
  if(Files.exists(dest))throw new IllegalArgumentException("Refusing to replace a test gamepack");
  String modulus=Files.readString(Path.of(args[2])).split("Modulus: ")[1].trim();
  if(!modulus.matches("[0-9a-f]{250,256}"))throw new IllegalArgumentException("Invalid public test modulus");
  int port=Integer.parseInt(args[3]);if(port<1024||port>65535)throw new IllegalArgumentException("Invalid local server port");
  if(source.toAbsolutePath().normalize().equals(dest.toAbsolutePath().normalize())||!dest.getFileName().toString().equals("nero-completionist-gamepack.jar"))throw new IllegalArgumentException("Dedicated client copy required");
  int[] rsa={0},sockets={0};
  Map<String,byte[]> entries=new LinkedHashMap<>();
  try(JarFile jar=new JarFile(source.toFile())) {
   for(var iterator=jar.entries();iterator.hasMoreElements();) {
    var entry=iterator.nextElement(); if(entry.isDirectory())continue;
    String name=entry.getName();byte[] bytes=jar.getInputStream(entry).readAllBytes();
    if(name.equals("mb.class") || name.equals("bq.class")) {
     ClassReader reader=new ClassReader(bytes);ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
     reader.accept(new ClassVisitor(Opcodes.ASM9,writer) {
      public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex) {
       return new MethodVisitor(Opcodes.ASM9,super.visitMethod(a,n,d,s,ex)) {
        public void visitLdcInsn(Object v) {
         if(name.equals("bq.class") && v instanceof String && ((String)v).matches("[0-9a-f]{250,256}")) {
          rsa[0]++;super.visitLdcInsn(modulus);
         } else super.visitLdcInsn(v);
        }
        public void visitMethodInsn(int opcode,String owner,String method,String desc,boolean itf) {
         if(name.equals("mb.class") && owner.equals("java/net/Socket") && method.equals("<init>")
              && desc.equals("(Ljava/net/InetAddress;I)V")) {
          // Discard the supplied game host/port, retaining the uninitialized Socket on the stack.
          super.visitInsn(Opcodes.POP);super.visitInsn(Opcodes.POP);
          super.visitMethodInsn(Opcodes.INVOKESTATIC,"java/net/InetAddress","getLoopbackAddress","()Ljava/net/InetAddress;",false);
          super.visitLdcInsn(port);sockets[0]++;
         }
         super.visitMethodInsn(opcode,owner,method,desc,itf);
        }
       };
      }
     },0);bytes=writer.toByteArray();
    }
    if(name.startsWith("META-INF/") && (name.endsWith(".SF")||name.endsWith(".RSA")||name.endsWith(".DSA")))continue;
    entries.put(name,bytes);
   }
  }
  if(rsa[0]!=1 || sockets[0]<1 || sockets[0]>5)throw new IllegalStateException("Unexpected gamepack patch sites");
  try(JarOutputStream out=new JarOutputStream(Files.newOutputStream(dest))) {
   for(var entry:entries.entrySet()){out.putNextEntry(new JarEntry(entry.getKey()));out.write(entry.getValue());out.closeEntry();}
  }
  System.out.println("ISOLATED_GAMEPACK sockets="+sockets[0]+" rsa="+rsa[0]+" destination="+dest);
 }
}

package com.evolvedmind.nero.compat;

public final class RuntimeCheck {
    public static void main(String[] args) throws Exception {
        if (ModelProbe.class.getClassLoader() != null) {
            throw new IllegalStateException("The model bridge must be visible to isolated client classloaders");
        }
        System.out.println("NERO_COMPAT_BOOTSTRAP_BRIDGE_VERIFIED");
        if (args.length == 0) throw new IllegalArgumentException("Supply the qualified client class names");
        for (String name : args) {
            Class<?> type = Class.forName(name, false, ClassLoader.getSystemClassLoader());
            type.getDeclaredMethods();
            System.out.println("NERO_COMPAT_LINK_VERIFIED " + name);
        }
    }
}

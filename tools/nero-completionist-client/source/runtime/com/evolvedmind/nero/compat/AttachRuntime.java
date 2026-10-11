package com.evolvedmind.nero.compat;

import com.sun.tools.attach.VirtualMachine;
import java.nio.file.Path;

public final class AttachRuntime {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Usage: PID agent.jar profile.properties");
        if (Long.parseLong(args[0]) < 1) throw new IllegalArgumentException("Invalid PID");
        VirtualMachine machine = VirtualMachine.attach(args[0]);
        try {
            machine.loadAgent(Path.of(args[1]).toAbsolutePath().toString(),
                Path.of(args[2]).toAbsolutePath().toString());
            System.out.println("NERO_COMPAT_ATTACH_COMPLETED pid=" + args[0]);
        } finally {
            machine.detach();
        }
    }
}

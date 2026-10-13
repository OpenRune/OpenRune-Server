import java.nio.file.Files;
import java.nio.file.Path;

/** Starts only the marked, separate cape test server; STOP-TEST requests normal JVM shutdown. */
public final class TestServerMain {
    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toRealPath();
        String expected = System.getenv("NERO_COMPLETIONIST_TEST_ROOT");
        if (expected == null || !root.equals(Path.of(expected).toRealPath())
                || !root.getFileName().toString().startsWith("completionist-test-server")
                || !Files.readString(root.resolve("ISOLATED-COMPLETIONIST")).equals(root.toString()))
            throw new IllegalStateException("Not a marked isolated completionist test server");
        Thread stop = new Thread(() -> {
            try { while (!Files.exists(root.resolve("STOP-TEST"))) Thread.sleep(250); System.exit(0); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); }
        }, "completionist-test-stop");
        stop.setDaemon(true); stop.start();
        org.rsmod.server.app.GameServerKt.main(new String[0]);
    }
}

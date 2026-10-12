package com.evolvedmind.nero.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;

public final class ModelProbe {
    private static volatile boolean enabled;
    private static Path log;
    private static String clientClass;
    private static Object client;
    private static Method getRevision;
    private static final AtomicLong nextSample = new AtomicLong();
    private static int samples;

    public static synchronized void configure(Properties profile) throws Exception {
        log = Path.of(System.getProperty("nero.compat.log",
            profile.getProperty("log", "nero-compat-runtime.jsonl"))).toAbsolutePath();
        Files.createDirectories(log.getParent());
        clientClass = profile.getProperty("client.class");
        AttachmentRegistry.configure(System.getProperty("nero.compat.bindings", ""));
        enabled = true;
        samples = 0;
    }

    public static synchronized void event(String kind, String detail) {
        if (log == null) return;
        try {
            if (Files.exists(log) && Files.size(log) > 2_000_000) return;
            Files.writeString(log, "{\"time\":\"" + Instant.now() + "\",\"event\":\"" + escape(kind)
                + "\",\"detail\":\"" + escape(detail) + "\"}\n",
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception error) {
            enabled = false;
        }
        System.out.println("NERO_COMPAT_" + kind.toUpperCase().replace('-', '_') + " " + detail);
    }

    public static synchronized void disable(String reason) {
        enabled = false;
        event("disabled", reason);
    }

    public static synchronized boolean qualifyClient(Class<?> type) throws Exception {
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != type) continue;
            field.setAccessible(true);
            Object candidate = field.get(null);
            if (candidate != null) { client = candidate; break; }
        }
        if (client == null) {
            disable("live client instance unavailable");
            return false;
        }
        getRevision = type.getMethod("getRevision");
        int revision = (Integer) getRevision.invoke(client);
        if (revision != 240) {
            disable("expected revision 240, observed " + revision);
            return false;
        }
        String state;
        try { state = String.valueOf(type.getMethod("getGameState").invoke(client)); }
        catch (NoSuchMethodException error) { state = "unavailable"; }
        event("client-qualified", "revision=" + revision + " state=" + state);
        return true;
    }

    public static void sample(Object model, Object player) {
        // The runtime itself requires explicit opt-in. All bound, assembled avatars
        // need this correction, including clients launched by the integration starter.
        if(enabled)
            try{AttachmentRegistry.normalizeAvatarPriorities(model);}catch(Exception e){disable("Avatar priority normalization: "+e);}
        if(enabled) ParticleRuntime.sample(model,player);
        if (!enabled || model == null || samples >= 400) return;
        long now = System.nanoTime();
        long previous = nextSample.get();
        if (now < previous || !nextSample.compareAndSet(previous, now + 250_000_000)) return;
        try {
            if (client == null) {
                Class<?> type = Class.forName(clientClass, false, player.getClass().getClassLoader());
                for (Field field : type.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) || field.getType() != type) continue;
                    field.setAccessible(true);
                    Object candidate = field.get(null);
                    if (candidate != null) { client = candidate; break; }
                }
                if (client == null) return;
                getRevision = type.getMethod("getRevision");
            }
            int revision = (Integer) getRevision.invoke(client);
            if (revision != 240) {
                disable("expected revision 240, observed " + revision);
                return;
            }
            Class<?> type = model.getClass();
            int count = (Integer) type.getMethod("getVerticesCount").invoke(model);
            int faces = (Integer) type.getMethod("getFaceCount").invoke(model);
            float[] x = (float[]) type.getMethod("getVerticesX").invoke(model);
            float[] y = (float[]) type.getMethod("getVerticesY").invoke(model);
            float[] z = (float[]) type.getMethod("getVerticesZ").invoke(model);
            if (count < 1 || count > 200_000 || faces < 0 || x.length < count
                    || y.length < count || z.length < count) throw new IllegalArgumentException("Invalid mesh");
            CRC32 fingerprint = new CRC32();
            for (int i = 0; i < count; i++) {
                for (float coordinate : new float[] {x[i], y[i], z[i]}) {
                    if (!Float.isFinite(coordinate)) throw new IllegalArgumentException("Non-finite vertex");
                    int bits = Float.floatToIntBits(coordinate);
                    fingerprint.update(bits >>> 24);
                    fingerprint.update(bits >>> 16);
                    fingerprint.update(bits >>> 8);
                    fingerprint.update(bits);
                }
            }
            synchronized (ModelProbe.class) {
                samples++;
                java.util.List<AttachmentRegistry.Anchor> anchors = AttachmentRegistry.anchors(model);
                if(samples==1 && !anchors.isEmpty()) {
                    byte[] priorities=(byte[])type.getMethod("getFaceRenderPriorities").invoke(model);
                    java.util.Map<Integer,Integer> counts=new java.util.TreeMap<>();
                    if(priorities!=null)for(byte p:priorities)counts.merge(p&255,1,Integer::sum);
                    event("avatar-priorities",counts.toString()+" capeFaceStart="+(anchors.get(0).faceId-782));
                }
                event("animated-player-model", "revision=" + revision + " vertices=" + count
                    + " faces=" + faces + " vertexCrc32=" + fingerprint.getValue()
                    + " actorToken=" + System.identityHashCode(player) + " sample=" + samples
                    + " emitterAnchors=" + anchors.size());
            }
        } catch (Throwable error) {
            disable("model observation failed: " + error.getClass().getSimpleName());
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\r", "\\r").replace("\n", "\\n");
    }
}

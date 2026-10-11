package org.rsmod.content.other.nerocurator.pack;

import com.google.gson.GsonBuilder;
import dev.openrune.filesystem.Cache;
import dev.openrune.cache.CacheDelegate;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** A bounded rebuild for an existing accepted cache; never downloads or replaces its base. */
public final class CompletionistPackTool {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected checkout root");
        Path root = Path.of(args[0]).toRealPath();
        if (!root.equals(Path.of("..").toRealPath())) throw new IllegalArgumentException("Run in the checkout's or-cache directory");
        Path raw = root.resolve("content/other/nero-completionist/pack/src/main/resources/pack/raw");
        var report = new LinkedHashMap<String,Object>();
        for (boolean serverPass : List.of(false,true)) {
            String pass = serverPass ? "server" : "live";
            Cache baseline = Cache.Companion.load(root.resolve(".data/cache/"+pass.toUpperCase()));
            Map<String,String> before;
            try { before = snapshot(baseline,serverPass); } finally { baseline.close(); }
            Cache cache = new CacheDelegate(root.resolve(".data/cache/" + pass.toUpperCase()).toString());
            try {
                var task = new CompletionistAssetsTask(raw.toFile());
                task.setRevision(240);task.setServerPass$tools(serverPass);task.init(cache);cache.update();cache.close();
                cache = Cache.Companion.load(root.resolve(".data/cache/"+pass.toUpperCase()));
                var after = snapshot(cache,serverPass);
                for (var entry : before.entrySet()) if (!entry.getValue().equals(after.get(entry.getKey()))) throw new IllegalStateException("Existing cache definition changed: " + pass + "/" + entry.getKey());
                report.put(pass,Map.of("unchangedNativeDefinitions",before.size(),"varpSentinelPreserved",!before.containsKey("2/67/-1") || after.containsKey("2/67/-1")));
            } finally { cache.close(); }
        }
        report.put("passed",true);report.put("revision",240);report.put("varpStart",63300);
        Files.writeString(root.resolve(".data/nero-completionist-pack-proof.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
        System.out.println("PASS_COMPLETIONIST_NATIVE_PACK " + report);
    }
    private static Map<String,String> snapshot(Cache cache,boolean server) throws Exception {
        var result = new LinkedHashMap<String,String>();
        for (int group : cache.archives(2)) {
            for (int id : cache.files(2,group)) {
                if ((group==10||group==59) && (id>=63000&&id<=63003 || id>=63100&&id<=63143)) continue;
                if ((group==9||group==58) && id==63010) continue;
                if (group==67 && id>=63300&&id<=63341) continue;
                byte[] bytes=cache.data(2,group,id,null);
                result.put("2/"+group+"/"+id,bytes==null?"absent":HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
            }
        }
        return result;
    }
}

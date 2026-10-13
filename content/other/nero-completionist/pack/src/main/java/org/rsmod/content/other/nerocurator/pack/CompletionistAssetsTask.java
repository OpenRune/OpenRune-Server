package org.rsmod.content.other.nerocurator.pack;

import com.google.gson.*;
import dev.openrune.cache.tools.TaskPriority;
import dev.openrune.cache.tools.tasks.CacheTask;
import dev.openrune.codec.osrs.impl.ItemServerCodec;
import dev.openrune.definition.codec.ItemCodec;
import dev.openrune.filesystem.Cache;
import dev.openrune.gamevals.GameValProvider;
import io.netty.buffer.Unpooled;
import java.io.File;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Native cache-build task: replays only the reviewed feature assets, in both cache passes. */
public final class CompletionistAssetsTask extends CacheTask {
    private final File assets;
    public CompletionistAssetsTask(File assets) { this.assets = assets; }
    @Override public TaskPriority getPriority() { return TaskPriority.VERY_LAST; }
    @Override public void init(Cache cache) {
        try { pack(cache); } catch (Exception error) { throw new IllegalStateException("Completionist cache pack failed", error); }
    }
    private void pack(Cache cache) throws Exception {
        if (getRevision() != 240) throw new IllegalStateException("Completionist assets require revision 240");
        String pass = getServerPass$tools() ? "server" : "live";
        Path root = Path.of("..").toRealPath(), directory = assets.toPath().toRealPath();
        Path cachePath = root.resolve(".data/cache/" + pass.toUpperCase());
        int varpOffset = getServerPass$tools() ? CompletionistConfigReference.varpFileOffset(cachePath) : 0;
        var manifest = JsonParser.parseString(Files.readString(directory.resolve("cache-manifest.json"))).getAsJsonObject();
        if (manifest.get("revision").getAsInt() != 240) throw new IllegalStateException("Unexpected asset revision");
        var writes = new LinkedHashMap<String, byte[]>();
        for (var entry : manifest.getAsJsonArray("writes")) {
            var asset = entry.getAsJsonObject();String key = asset.get("key").getAsString();
            if (!key.matches("(live|server)/[0-9]+/[0-9]+/[0-9]+")) throw new IllegalStateException("Invalid asset key");
            Path file = directory.resolve(asset.get("file").getAsString()).normalize().toRealPath();
            if (!file.startsWith(directory)) throw new IllegalStateException("Asset outside pack");
            byte[] bytes = Files.readAllBytes(file);
            if (!hash(bytes).equals(asset.get("sha256").getAsString())) throw new IllegalStateException("Asset checksum: " + key);
            if (key.startsWith(pass + "/") && writes.put(key, bytes) != null) throw new IllegalStateException("Duplicate asset: " + key);
        }
        // Derive stats from this build's native max variants, never from an earlier boosted target.
        var params = GameValProvider.Companion.loadIsolated(root + "/").getMappings().get("param");
        var additions = new LinkedHashMap<Integer,Integer>();
        for (String name : List.of("attack_stab","attack_slash","attack_crush","attack_magic","attack_ranged","defence_stab","defence_slash","defence_crush","defence_magic","defence_ranged","melee_strength","ranged_strength","magic_damage","item_prayer_bonus")) {
            Integer id = params.get("param." + name);if (id == null) throw new IllegalStateException("Missing bonus: " + name);
            additions.put(id, name.equals("magic_damage") ? 20 : 2);
        }
        var sources = new LinkedHashMap<Integer,Integer>();sources.put(63000,13280);sources.put(63001,13280);
        for (var entry : JsonParser.parseString(Files.readString(directory.resolve("variants.json"))).getAsJsonArray()) {
            var row = entry.getAsJsonObject();sources.put(row.get("capeId").getAsInt(), row.get("sourceId").getAsInt());
        }
        var clientCodec = new ItemCodec(240);var serverCodec = new ItemServerCodec(240, Map.of(), Map.of());
        for (var entry : sources.entrySet()) {
            int target = entry.getKey(), source = entry.getValue(), group = pass.equals("server") ? 59 : 10;
            String key = pass + "/2/" + group + "/" + target;
            byte[] base = cache.data(2,group,source,null);if (base == null) throw new IllegalStateException("Missing max source: " + source);
            byte[] bytes = writes.get(key);if (bytes == null) throw new IllegalStateException("Missing cape: " + target);
            Map<Integer,Object> desired;
            if (pass.equals("server")) {
                var nativeItem = serverCodec.loadData(source,base);var original = serverCodec.loadData(target,bytes);
                var item = original;
                desired = boost((target == 63000 || target == 63001 ? serverCodec.loadData(13342,cache.data(2,59,13342,null)) : nativeItem).getParamsRaw(), additions);if (target >= 63100) desired.put(params.get("param.death_pvp_coin_value"),569250);if (target >= 63128) desired.put(params.get("param.death_always_kept"),1);if (target == 63000 || target == 63001) desired.put(params.get("param.ammo_recovery_rate"),72);item.setParamsRaw(desired);
                item.setInterfaceOptions(nativeItem.getInterfaceOptions());
                if (target == 63126 || target == 63142) item.setInterfaceOptions(Arrays.asList(null,"Wear","Check","Empty","Destroy"));
                bytes = serverCodec.encodeToBuffer(item);
                if (!desired.equals(serverCodec.loadData(target,bytes).getParamsRaw())) throw new IllegalStateException("Server stats roundtrip");
            } else {
                desired = boost(clientCodec.loadData(target == 63000 || target == 63001 ? 13342 : source,target == 63000 || target == 63001 ? cache.data(2,10,13342,null) : base).getParams(), additions);if (target >= 63100) desired.put(params.get("param.death_pvp_coin_value"),569250);if (target >= 63128) desired.put(params.get("param.death_always_kept"),1);if (target == 63000 || target == 63001) desired.put(params.get("param.ammo_recovery_rate"),72);
                bytes = appendParams(bytes,desired);
                bytes = appendOps(bytes,clientCodec.loadData(source,base).getInterfaceOptions());
                if (target == 63126 || target == 63142) bytes = appendOps(bytes,Arrays.asList(null,"Wear","Check","Empty","Destroy"));
                if (!desired.equals(clientCodec.loadData(target,bytes).getParams())) throw new IllegalStateException("Client stats roundtrip");
            }
            writes.put(key,bytes);
        }
        // Non-wearable repair states keep the custom inventory model; bonuses and equip requirements are cleared.
        int[] originals = {63112,63100,63120,63114,63116,63118,63124,63126};
        for (int family=0;family<originals.length;family++) for (int state=0;state<3;state++) {
            int target=63200+family*3+state, group=pass.equals("server")?59:10;
            byte[] original=writes.get(pass+"/2/"+group+"/"+originals[family]);
            Map<Integer,Object> brokenParams=new LinkedHashMap<>();brokenParams.put(params.get("param.death_pvp_coin_value"),569250);
            String suffix=state==0?" (broken)":state==1?" (l) (broken)":" (l) (mangled)";
            byte[] bytes;
            if (pass.equals("server")) {
                var item=serverCodec.loadData(target,original);item.setId(target);item.setName(item.getName()+suffix);item.setWearpos1(-1);item.setWearpos2(-1);item.setWearpos3(-1);item.setParamsRaw(brokenParams);item.setInterfaceOptions(Arrays.asList(null,null,null,null,"Drop"));item.setExamine("Use this damaged Completionist cape on Perdu to repair it.");bytes=serverCodec.encodeToBuffer(item);
            } else {
                String name=clientCodec.loadData(originals[family],original).getName()+suffix;
                bytes=appendParams(original,brokenParams);bytes=appendOps(bytes,Arrays.asList(null,null,null,null,"Drop"));
                var buffer=Unpooled.buffer().writeBytes(bytes,0,bytes.length-1).writeByte(2);buffer.writeBytes(name.getBytes(java.nio.charset.Charset.forName("windows-1252"))).writeByte(0).writeByte(0);bytes=new byte[buffer.readableBytes()];buffer.readBytes(bytes);buffer.release();
            }
            writes.put(pass+"/2/"+group+"/"+target,bytes);
        }
        Path marker = root.resolve(".data/nero-completionist-source-owned.json");var owned = new LinkedHashMap<String,String>();
        for (Path path : List.of(root.resolve(".data/nero-completionist-installed.json"),marker)) if (Files.isRegularFile(path)) {
            var values = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            for (var entry : values.entrySet()) owned.put(entry.getKey(), entry.getValue().getAsString());
        }
        for (var entry : writes.entrySet()) {
            String[] key = entry.getKey().split("/");byte[] prior = cache.data(Integer.parseInt(key[1]),Integer.parseInt(key[2]),(Integer.parseInt(key[2])==67 ? Integer.parseInt(key[3])+varpOffset : Integer.parseInt(key[3])),null);
            if (prior != null && !Arrays.equals(prior,entry.getValue()) && !hash(prior).equals(owned.get(entry.getKey()))) throw new IllegalStateException("Unrelated cache asset: " + entry.getKey());
        }
        for (var entry : writes.entrySet()) {
            String[] key = entry.getKey().split("/");cache.write(Integer.parseInt(key[1]),Integer.parseInt(key[2]),(Integer.parseInt(key[2])==67 ? Integer.parseInt(key[3])+varpOffset : Integer.parseInt(key[3])),entry.getValue(),null);
            owned.put(entry.getKey(),hash(entry.getValue()));
        }
        cache.update();
        CompletionistConfigReference.restoreSignedVarpSentinel(cachePath,varpOffset);
        if (getServerPass$tools()) CompletionistComponentsKt.refreshCompletionistComponents(root,cache);
        Files.createDirectories(marker.getParent());Files.writeString(marker,new GsonBuilder().setPrettyPrinting().create().toJson(owned));
    }
    private static Map<Integer,Object> boost(Map<Integer,Object> source, Map<Integer,Integer> additions) {
        var result = new LinkedHashMap<Integer,Object>();if (source != null) result.putAll(source);
        for (var entry : additions.entrySet()) result.put(entry.getKey(), (source != null && source.get(entry.getKey()) instanceof Number n ? n.intValue() : 0) + entry.getValue());return result;
    }
    private static byte[] appendOps(byte[] source,List<String> options) {
        var buffer = Unpooled.buffer().writeBytes(source,0,source.length-1);
        for (int i=0;i<5;i++) { buffer.writeByte(35+i);String option=options.get(i);buffer.writeBytes((option==null?"":option).getBytes(java.nio.charset.Charset.forName("windows-1252")));buffer.writeByte(0); }
        buffer.writeByte(0);byte[] result = new byte[buffer.readableBytes()];buffer.readBytes(result);buffer.release();return result;
    }
    private static byte[] appendParams(byte[] source,Map<Integer,Object> params) {
        if (source[source.length-1]!=0 || params.size()>255) throw new IllegalStateException("Invalid item params");
        var buffer = Unpooled.buffer().writeBytes(source,0,source.length-1);buffer.writeByte(249).writeByte(params.size());
        for (var entry : params.entrySet()) { Object value=entry.getValue();buffer.writeByte(value instanceof String?1:0).writeMedium(entry.getKey());if(value instanceof String text) {buffer.writeBytes(text.getBytes(java.nio.charset.Charset.forName("windows-1252")));buffer.writeByte(0);}else buffer.writeInt(((Number)value).intValue()); }
        buffer.writeByte(0);byte[] result=new byte[buffer.readableBytes()];buffer.readBytes(result);buffer.release();return result;
    }
    private static String hash(byte[] bytes)throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
}

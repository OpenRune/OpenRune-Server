package com.evolvedmind.nero.verification;

import dev.openrune.filesystem.Cache;
import dev.openrune.definition.codec.TextureCodec;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Reads the actual native texture definitions/sprites; never updates a cache. */
public final class NativeCacheMaterial {
    public static void main(String[] args)throws Exception {
        Cache cache=Cache.Companion.load(Path.of(args[0]));
        try {
            for(int id:List.of(40,59)) {
                byte[] bytes=cache.data(9,0,id,null);if(bytes==null)throw new IllegalStateException("Missing native texture "+id);
                var codec=new TextureCodec(240);var builder=codec.builder(id);
                var input=io.netty.buffer.Unpooled.wrappedBuffer(bytes);
                codec.read(builder,0,input);if(input.isReadable())throw new IllegalStateException("Unconsumed native texture bytes");input.release();
                var texture=codec.build(builder);
                int direction=texture.getAnimationDirection(),speed=texture.getAnimationSpeed(),image=texture.getFileId();
                if(direction<1||direction>4||speed<1)throw new IllegalStateException("Texture is not native animated material: "+id);
                // The qualified client's um.ao is index 8; its texture provider
                // receives hc.hv as the image archive (also shared with sprites).
                byte[] pixels=cache.data(8,image,0,null);
                if(pixels==null)throw new IllegalStateException("Missing native indexed texture image "+image);
                System.out.println("PASS_NATIVE_ANIMATED_TEXTURE id="+id+" direction="+direction+" speed="+speed+" image="+image+" imageBytes="+pixels.length+" imageHeader="+HexFormat.of().formatHex(Arrays.copyOf(pixels,Math.min(8,pixels.length)))+" sha256="+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
            }
        } finally {cache.close();}
    }
}

package com.evolvedmind.nero.particles;

import com.evolvedmind.nero.compat.*;
import java.lang.reflect.*;
import java.util.*;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;

/** Real decoded-model producer -> runtime listener -> particle consumer/lifecycle. */
final class NativeParticleCheck {
    static void run(Model cape,Model ordinary)throws Exception {
        final int[] worn={63000},x={6400};
        Object world=proxy(Class.forName("net.runelite.api.WorldView"),(o,m,a)->{
            if(m.getName().equals("getTileHeights"))return new int[4][105][105];
            if(m.getName().equals("getTileSettings"))return new byte[4][105][105];
            return defaultValue(m.getReturnType());
        });
        PlayerComposition appearance=(PlayerComposition)proxy(PlayerComposition.class,(o,m,a)->m.getName().equals("getEquipmentId")?worn[0]:defaultValue(m.getReturnType()));
        Player player=(Player)proxy(Player.class,(o,m,a)->{
            switch(m.getName()) {
                case "getLocalLocation":return new LocalPoint(x[0],6400);
                case "getName":return "native_test";
                case "getPlayerComposition":return appearance;
                default:return defaultValue(m.getReturnType());
            }
        });
        Client client=(Client)proxy(Client.class,(o,m,a)->{
            if(m.getName().equals("getWorldView")||m.getName().equals("getTopLevelWorldView"))return world;
            return defaultValue(m.getReturnType());
        });
        NeroParticlePlugin plugin=new NeroParticlePlugin();set(plugin,"client",client);set(plugin,"styles",new CapeStyles(client));
        Field definitions=NeroParticlePlugin.class.getDeclaredField("definitions");definitions.setAccessible(true);
        @SuppressWarnings("unchecked") Map<Integer,ParticleSystem.Definition> defs=(Map<Integer,ParticleSystem.Definition>)definitions.get(plugin);
        ParticleSystem.Definition d=new ParticleSystem.Definition();d.minRate=d.maxRate=64;d.minLife=d.maxLife=20;d.speedChange=100;d.minSize=d.maxSize=d.endSize=16384;d.minColour=d.maxColour=d.fadeColour=0xffffffff;d.validate();defs.put(306,d);
        Method sample=NeroParticlePlugin.class.getDeclaredMethod("sample",Object.class,List.class);sample.setAccessible(true);
        Method step=NeroParticlePlugin.class.getDeclaredMethod("stepActors",double.class);step.setAccessible(true);
        Field actors=NeroParticlePlugin.class.getDeclaredField("actors");actors.setAccessible(true);
        @SuppressWarnings("unchecked") Map<Actor,Object> states=(Map<Actor,Object>)actors.get(plugin);
        ParticleRuntime.install((actor,triangles)->{try{sample.invoke(plugin,actor,triangles);}catch(Exception e){throw new IllegalStateException(e);}},()->{},()->{});
        try {
            ModelProbe.sample(cape,player);require(states.size()==1,"bound decoded model reaches particle listener");
            require(!((List<?>)step.invoke(plugin,1.)).isEmpty(),"native model emits particles");
            Object state=states.get(player);Field emitters=state.getClass().getDeclaredField("emitters");emitters.setAccessible(true);
            @SuppressWarnings("unchecked") List<ParticleSystem.Emitter> before=(List<ParticleSystem.Emitter>)emitters.get(state);float saved=before.get(0).xyz[0];
            x[0]+=128;step.invoke(plugin,1.);
            @SuppressWarnings("unchecked") List<ParticleSystem.Emitter> after=(List<ParticleSystem.Emitter>)emitters.get(state);
            require(Math.abs(after.get(0).xyz[0]-saved-128)<.01,"cached anchors follow actor movement");
            worn[0]=13280;step.invoke(plugin,1.);require(states.isEmpty(),"cape removal stops cached emission");
            worn[0]=63000;ModelProbe.sample(cape,player);ModelProbe.sample(ordinary,player);require(states.isEmpty(),"ordinary model clears old cape state");
            ModelProbe.sample(cape,player);Object cached=states.get(player);Field seen=cached.getClass().getDeclaredField("seen");seen.setAccessible(true);seen.setLong(cached,System.nanoTime()-2_000_000_000L);
            step.invoke(plugin,1.);require(states.isEmpty(),"stale offscreen emitter lease expires");
            ModelProbe.sample(cape,player);plugin.onPlayerDespawned(new net.runelite.api.events.PlayerDespawned(player));require(states.isEmpty(),"player despawn clears particles");
            System.out.println("PASS_NATIVE_PARTICLE_LIFECYCLE producerConsumer=true cachedMovement=true unequip=true ordinaryModel=true leaseExpiry=true despawn=true");
        } finally {ParticleRuntime.clear();}
    }
    private static Object proxy(Class<?> type,InvocationHandler handler){return Proxy.newProxyInstance(NativeParticleCheck.class.getClassLoader(),new Class<?>[]{type},handler);}
    private static Object defaultValue(Class<?> type){if(!type.isPrimitive())return null;if(type==boolean.class)return false;if(type==int.class)return 0;if(type==long.class)return 0L;if(type==float.class)return 0f;if(type==double.class)return 0.;if(type==byte.class)return (byte)0;if(type==short.class)return (short)0;return null;}
    private static void set(Object target,String name,Object value)throws Exception{Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);}
    private static void require(boolean passed,String message){if(!passed)throw new AssertionError(message);}
}

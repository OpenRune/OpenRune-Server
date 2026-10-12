package org.rsmod.content.completionisttest;

import java.nio.file.Files;
import java.nio.file.Path;
import dev.or2.central.account.Rights;
import kotlin.Unit;
import org.rsmod.events.EventBus;
import org.rsmod.game.entity.Player;
import org.rsmod.game.entity.player.SessionStateEvent;
import org.rsmod.plugin.scripts.PluginScript;
import org.rsmod.plugin.scripts.ScriptContext;

/** Session-only privileges in the marked test harness. This class is never included in the server JAR. */
public final class IsolatedAccessScript extends PluginScript {
    public static void requireTestRoot() {
        try {
            Path root=Path.of("").toRealPath();String expected=System.getenv("NERO_COMPLETIONIST_TEST_ROOT");
            if(expected==null||!root.equals(Path.of(expected).toRealPath())
                ||!root.getFileName().toString().equals("completionist-test-server")
                ||!Files.readString(root.resolve("ISOLATED-COMPLETIONIST")).equals(root.toString()))
                throw new IllegalStateException("Not a marked isolated completionist test root");
        } catch(java.io.IOException error) {throw new IllegalStateException("Missing isolated test marker",error);}
    }
    private static void register(EventBus bus) {
        requireTestRoot();
        bus.subscribeUnbound(SessionStateEvent.PrepareLogin.class,event->{event.getPlayer().setModLevel(Rights.ADMINISTRATOR);return Unit.INSTANCE;});
        bus.subscribeUnbound(SessionStateEvent.Login.class,event->{event.getPlayer().setModLevel(Rights.ADMINISTRATOR);return Unit.INSTANCE;});
    }
    @Override public void startup(ScriptContext context) {
        register(context.getEventBus());
        System.out.println("ISOLATED_COMPLETIONIST_TEST_RIGHTS_READY session-only admin; live accounts unchanged");
    }
    public static void main(String[] args) {
        if(args.length!=1)throw new IllegalArgumentException("Use --check or --reject");
        if(args[0].equals("--reject")) {
            try { requireTestRoot(); } catch(IllegalStateException expected) {System.out.println("PASS_TEST_RIGHTS_REJECT_UNMARKED");return;}
            throw new AssertionError("Unmarked root accepted");
        }
        EventBus bus=new EventBus();register(bus);Player p=new Player();
        bus.publish(new SessionStateEvent.PrepareLogin(p));if(p.getModLevel()!=Rights.ADMINISTRATOR)throw new AssertionError("PrepareLogin");
        p.setModLevel(Rights.NONE);bus.publish(new SessionStateEvent.Login(p));if(p.getModLevel()!=Rights.ADMINISTRATOR)throw new AssertionError("Login");
        System.out.println("PASS_TEST_RIGHTS_MARKED_LOGIN_EVENTS");
    }
}

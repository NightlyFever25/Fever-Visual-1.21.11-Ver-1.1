package fever.visual.systems.event.handlers;

import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.network.ServerConnectionEvent;
import fever.visual.systems.event.impl.player.ClientPlayerTickEvent;
import fever.visual.utility.interfaces.IMinecraft;
import net.fabricmc.loader.api.FabricLoader;

public class ServerConnectionHandler implements IMinecraft {
   public ServerConnectionHandler() {
      FeverVisual.getInstance().getEventManager().subscribe(this);
   }
}

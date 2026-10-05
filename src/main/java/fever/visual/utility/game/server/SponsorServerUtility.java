package fever.visual.utility.game.server;

import fever.visual.mixin.accessors.ServerListAccessor;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;

public final class SponsorServerUtility {
   public static final String NAME = fromCodePoints(1047, 1040, 1061, 1054, 1044, 1048, 32, 1057, 1070, 1044, 1040, 32, 1058, 1054, 1055, 32, 1050, 1054, 1055, 1048, 1071, 32, 1056, 1048, 1051, 1048, 1050, 1040);
   public static final String ADDRESS = fromCodePoints(102, 118, 46, 104, 97, 112, 112, 121, 119, 111, 114, 108, 100, 46, 112, 119);
   public static final String SPONSOR_CATEGORY = fromCodePoints(83, 112, 111, 110, 115, 111, 114, 39, 115);
   public static final String MULTIPLAYER_CATEGORY = fromCodePoints(1052, 1091, 1083, 1100, 1090, 1080, 1087, 1083, 1077, 1077, 1088);

   private SponsorServerUtility() {
   }

   public static boolean isSponsor(ServerInfo serverInfo) {
      return serverInfo != null && isSponsorAddress(serverInfo.address);
   }

   public static boolean isSponsorAddress(String address) {
      return normalize(address).equals(ADDRESS);
   }

   public static int indexOf(ServerList serverList, ServerInfo serverInfo) {
      if (serverInfo == null) {
         return -1;
      }

      for(int i = 0; i < serverList.size(); ++i) {
         if (serverList.get(i) == serverInfo || isSponsor(serverInfo) && isSponsor(serverList.get(i))) {
            return i;
         }
      }

      return -1;
   }

   public static boolean ensureFirst(ServerList serverList) {
      if (serverList == null) {
         return false;
      }

      ServerListAccessor accessor = (ServerListAccessor)serverList;
      List<ServerInfo> servers = accessor.feverVisual$getServers();
      List<ServerInfo> hiddenServers = accessor.feverVisual$getHiddenServers();
      ServerInfo sponsor = null;
      int originalSponsorIndex = -1;
      boolean changed = false;

      for(int i = 0; i < servers.size(); ++i) {
         ServerInfo serverInfo = servers.get(i);
         if (isSponsor(serverInfo)) {
            if (sponsor == null) {
               sponsor = serverInfo;
               originalSponsorIndex = i;
            } else {
               servers.remove(i--);
               changed = true;
            }
         }
      }

      for(Iterator<ServerInfo> iterator = hiddenServers.iterator(); iterator.hasNext(); ) {
         ServerInfo serverInfo = iterator.next();
         if (isSponsor(serverInfo)) {
            if (sponsor == null) {
               sponsor = serverInfo;
            }

            iterator.remove();
            changed = true;
         }
      }

      if (sponsor == null) {
         sponsor = new ServerInfo(NAME, ADDRESS, ServerInfo.ServerType.OTHER);
      }

      changed = changed || originalSponsorIndex != 0 || servers.isEmpty() || servers.get(0) != sponsor;
      if (!NAME.equals(sponsor.name)) {
         sponsor.name = NAME;
         changed = true;
      }

      if (!ADDRESS.equals(sponsor.address)) {
         sponsor.address = ADDRESS;
         changed = true;
      }

      servers.remove(sponsor);
      servers.removeIf(SponsorServerUtility::isSponsor);
      servers.add(0, sponsor);
      return changed || originalSponsorIndex != 0;
   }

   private static String normalize(String address) {
      if (address == null) {
         return "";
      }

      String normalized = address.trim().toLowerCase(Locale.ROOT);
      while(normalized.endsWith("/")) {
         normalized = normalized.substring(0, normalized.length() - 1);
      }

      return normalized;
   }

   private static String fromCodePoints(int... codePoints) {
      return new String(codePoints, 0, codePoints.length);
   }
}

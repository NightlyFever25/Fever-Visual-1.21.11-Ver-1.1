package fever.visual.utility.sounds;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MusicTrackerBridgeMain {
   private static final AtomicBoolean RUNNING = new AtomicBoolean(true);
   private static volatile IMediaSession currentSession;

   private MusicTrackerBridgeMain() {
   }

   public static void main(String[] args) {
      Thread commandThread = new Thread(MusicTrackerBridgeMain::readCommands, "fv-mediainfo-bridge-commands");
      commandThread.setDaemon(true);
      commandThread.start();

      while (RUNNING.get() && !Thread.currentThread().isInterrupted()) {
         try {
            IMediaSession selected = selectSession(MediaPlayerInfo.Instance.getMediaSessions());
            currentSession = selected;

            if (selected == null) {
               System.out.println("NONE");
            } else {
               MediaInfo media = safeMedia(selected);
               if (media == null || (media.getTitle().isEmpty() && media.getArtist().isEmpty() && !media.getPlaying())) {
                  System.out.println("NONE");
               } else {
                  System.out.println(encodeData(selected.getOwner(), media));
               }
            }
         } catch (Throwable ignored) {
            System.out.println("NONE");
         }

         System.out.flush();
         try {
            Thread.sleep(750L);
         } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            break;
         }
      }
   }

   private static void readCommands() {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
         String line;
         while ((line = reader.readLine()) != null && RUNNING.get()) {
            handleCommand(line.trim());
         }
      } catch (IOException ignored) {
      } finally {
         RUNNING.set(false);
      }
   }

   private static void handleCommand(String command) {
      IMediaSession session = currentSession;
      if (session == null || command.isEmpty()) {
         return;
      }

      try {
         switch (command) {
            case "PLAY" -> session.play();
            case "PAUSE" -> session.pause();
            case "PLAY_PAUSE" -> session.playPause();
            case "STOP" -> session.stop();
            case "NEXT" -> session.next();
            case "PREVIOUS" -> session.previous();
            default -> {
            }
         }
      } catch (Throwable ignored) {
      }
   }

   private static IMediaSession selectSession(List<IMediaSession> sessions) {
      if (sessions == null || sessions.isEmpty()) {
         return null;
      }

      IMediaSession selected = sessions.stream()
              .filter(s -> s != null)
              .max(Comparator.comparing(session -> {
                 MediaInfo media = safeMedia(session);
                 return media != null && media.getPlaying();
              }))
              .orElse(null);

      if (selected != null) {
         return selected;
      }

      for (IMediaSession session : sessions) {
         if (session != null) {
            return session;
         }
      }
      return null;
   }

   private static MediaInfo safeMedia(IMediaSession session) {
      try {
         return session.getMedia();
      } catch (Throwable ignored) {
         return null;
      }
   }

   private static String encodeData(String owner, MediaInfo media) {
      String ownerB64 = encodeString(owner == null ? "" : owner);
      String titleB64 = encodeString(media.getTitle() == null ? "" : media.getTitle());
      String artistB64 = encodeString(media.getArtist() == null ? "" : media.getArtist());
      byte[] artwork = media.getArtworkPng();
      String artworkB64 = artwork == null || artwork.length == 0 ? "" : Base64.getEncoder().encodeToString(artwork);
      return "DATA\t"
              + ownerB64 + '\t'
              + titleB64 + '\t'
              + artistB64 + '\t'
              + media.getPosition() + '\t'
              + media.getDuration() + '\t'
              + (media.getPlaying() ? "1" : "0") + '\t'
              + artworkB64;
   }

   private static String encodeString(String value) {
      return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
   }
}

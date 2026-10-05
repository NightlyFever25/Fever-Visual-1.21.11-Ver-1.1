package fever.visual.utility.sounds;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import fever.visual.FeverVisual;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.interfaces.IMinecraft;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MusicTracker implements IMinecraft {
   private static final MediaInfo EMPTY_MEDIA = new MediaInfo("", "", new byte[0], 0L, 0L, false);
   private static final long MEDIA_STALE_TIMEOUT_MS = 3000L;
   private static final long BRIDGE_RESTART_DELAY_MS = 1500L;
   private static final long BRIDGE_STABLE_UPTIME_MS = 5000L;
   private static final long BRIDGE_NATIVE_CRASH_RESTART_DELAY_MS = 30000L;
   private static final int BRIDGE_MAX_EARLY_FAILURES = 3;
   private final boolean useBridgeProcess;
   private final boolean useDirectNative;

   private final ExecutorService mediaExecutor = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "fevervisual-mediainfo");
      t.setDaemon(true);
      return t;
   });

   private volatile MediaInfo mediaInfo = EMPTY_MEDIA;
   private volatile IMediaSession session;
   private final Identifier artworkId = Identifier.of(FeverVisual.MOD_ID, "music_artwork");
   private volatile boolean artworkRegistered;
   private int lastArtworkFingerprint = Integer.MIN_VALUE;
   private final AtomicBoolean mediaPollInFlight = new AtomicBoolean(false);

   private volatile long lastMediaEventMs;
   private ColorRGBA mediaColor = ColorRGBA.WHITE;
   private final ConcurrentHashMap<Integer, Identifier> textureCache = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<Integer, ColorRGBA> colorCache = new ConcurrentHashMap<>();
   private String lyrics = "";
   private String lastTrack = "";
   private final AtomicBoolean running = new AtomicBoolean(true);
   private final AtomicBoolean bridgeStarting = new AtomicBoolean(false);
   private volatile Process bridgeProcess;
   private volatile BufferedWriter bridgeCommandWriter;
   private volatile Thread bridgeReaderThread;
   private volatile long lastBridgeStartAttemptMs;
   private volatile long lastBridgeMessageMs;
   private volatile long bridgeStartedAtMs;
   private volatile long bridgeDisabledUntilMs;
   private volatile int bridgeEarlyFailureCount;
   private volatile boolean bridgeDisabledByFailures;
   private volatile boolean bridgeEverResponded;

   public MusicTracker() {
      this.useBridgeProcess = shouldUseBridgeProcess();
      this.useDirectNative = shouldUseDirectNative(this.useBridgeProcess);
      if (!this.useBridgeProcess && !this.useDirectNative) {
         running.set(false);
         return;
      }
      if (this.useBridgeProcess) {
         this.startBridgeIfNeeded();
      }

      Thread pollThread = new Thread(() -> {
         while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
               Thread.sleep(100L);
               if (!running.get() || Thread.currentThread().isInterrupted()) {
                  break;
               }
               pollMediaSessions();
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
               break;
            } catch (Exception e) {
               if (!running.get()) {
                  break;
               }
            }
         }
      });
      pollThread.setDaemon(true);
      pollThread.setName("MusicTracker-PollThread");
      pollThread.start();
   }

   private void pollMediaSessions() {
      if (!running.get() || Thread.currentThread().isInterrupted()) {
         return;
      }

      if (this.useBridgeProcess && this.canUseBridgeProcess()) {
         this.pollBridgeProcess();
         if (!this.shouldPollDirectNativeFallback()) {
            return;
         }
      } else if (!this.useDirectNative) {
         return;
      }

      if (!mediaPollInFlight.compareAndSet(false, true)) {
         return;
      }

      mediaExecutor.execute(() -> {
         try {
            if (!this.useDirectNative || !running.get()) {
               return;
            }
            var sessions = MediaPlayerInfo.Instance.getMediaSessions();
            if (sessions == null || sessions.isEmpty()) {
               clearMediaStateIfStale();
               return;
            }

            IMediaSession current = sessions.stream()
                    .filter(s -> s != null)
                    .max(Comparator.comparing(s -> {
                       try {
                          MediaInfo info = s.getMedia();
                          return info != null && info.getPlaying();
                       } catch (Exception e) {
                          return false;
                       }
                    }))
                    .orElse(null);

            if (current == null) {
               clearMediaStateIfStale();
               return;
            }

            MediaInfo info = current.getMedia();
            if (info == null || (info.getTitle().isEmpty() && info.getArtist().isEmpty() && !info.getPlaying())) {
               clearMediaStateIfStale();
               return;
            }

            mediaInfo = info;
            session = current;
            lastMediaEventMs = System.currentTimeMillis();

            String trackId = info.getArtist() + " - " + info.getTitle();
            if (!trackId.equals(lastTrack) && running.get()) {
               lastTrack = trackId;
               lyrics = "";
               mediaExecutor.execute(() -> {
                  if (running.get()) {
                     String l = LyricsFetcher.fetchFromGenius(info.getArtist(), info.getTitle());
                     if (running.get() && l != null) {
                        lyrics = l;
                     }
                  }
               });
            }

            MediaInfo captured = info;
            mc.execute(() -> syncArtworkTexture(captured));

         } catch (Throwable e) {
            clearMediaStateIfStale();
         } finally {
            mediaPollInFlight.set(false);
         }
      });
   }

   private static boolean shouldUseBridgeProcess() {
      String property = System.getProperty("fevervisual.media.bridge.enabled");
      if (property != null) {
         return Boolean.parseBoolean(property);
      }

      String env = System.getenv("FEVERVISUAL_MEDIA_BRIDGE_ENABLED");
      if (env != null) {
         return Boolean.parseBoolean(env);
      }
      String os = System.getProperty("os.name", "").toLowerCase();
      return os.contains("windows");
   }

   private static boolean shouldUseDirectNative(boolean useBridgeProcess) {
      String property = System.getProperty("fevervisual.media.native.enabled");
      if (property != null) {
         return Boolean.parseBoolean(property);
      }

      String env = System.getenv("FEVERVISUAL_MEDIA_NATIVE_ENABLED");
      if (env != null) {
         return Boolean.parseBoolean(env);
      }

      return true;
   }

   private boolean shouldPollDirectNativeFallback() {
      if (!this.useDirectNative || this.haveActiveSession()) {
         return false;
      }

      Process process = this.bridgeProcess;
      return process == null || !process.isAlive() || !this.bridgeEverResponded;
   }

   private void pollBridgeProcess() {
      this.startBridgeIfNeeded();
      if (this.lastBridgeMessageMs > 0L && System.currentTimeMillis() - this.lastBridgeMessageMs > MEDIA_STALE_TIMEOUT_MS) {
         clearMediaStateIfStale();
      }
   }

   private void startBridgeIfNeeded() {
      if (!this.canUseBridgeProcess()) {
         return;
      }

      Process process = this.bridgeProcess;
      if (process != null && process.isAlive()) {
         return;
      }

      long now = System.currentTimeMillis();
      if (now - this.lastBridgeStartAttemptMs < BRIDGE_RESTART_DELAY_MS) {
         return;
      }

      if (!this.bridgeStarting.compareAndSet(false, true)) {
         return;
      }

      this.mediaExecutor.execute(() -> {
         try {
            if (!running.get()) {
               return;
            }
            this.lastBridgeStartAttemptMs = System.currentTimeMillis();
            this.stopBridgeProcess();

            String classpath = this.buildBridgeClasspath();
            if (classpath.isEmpty()) {
               return;
            }

            ProcessBuilder builder = new ProcessBuilder(
                    this.getJavaBinaryPath(),
                    "-XX:ErrorFile=" + this.getBridgeErrorFile(),
                    "-cp",
                    classpath,
                    MusicTrackerBridgeMain.class.getName()
            );
            builder.redirectErrorStream(true);
            Process started = builder.start();
            this.bridgeProcess = started;
            this.bridgeCommandWriter = new BufferedWriter(new OutputStreamWriter(started.getOutputStream(), StandardCharsets.UTF_8));
            this.lastBridgeMessageMs = 0L;
            this.bridgeStartedAtMs = System.currentTimeMillis();
            this.bridgeEverResponded = false;
            this.startBridgeReader(started);
         } catch (Exception ignored) {
            this.stopBridgeProcess();
         } finally {
            this.bridgeStarting.set(false);
         }
      });
   }

   private void startBridgeReader(Process process) {
      Thread reader = new Thread(() -> {
         try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running.get() && (line = bufferedReader.readLine()) != null) {
               this.lastBridgeMessageMs = System.currentTimeMillis();
               this.bridgeEverResponded = true;
               this.handleBridgeLine(line);
            }
         } catch (IOException ignored) {
         } finally {
            if (this.bridgeProcess == process) {
               long uptime = System.currentTimeMillis() - this.bridgeStartedAtMs;
               int exitCode = this.getBridgeExitCode(process);
               if (running.get()) {
                  if (exitCode != 0) {
                     if (uptime < BRIDGE_STABLE_UPTIME_MS) {
                        this.bridgeEarlyFailureCount++;
                        if (this.bridgeEarlyFailureCount >= BRIDGE_MAX_EARLY_FAILURES) {
                           this.bridgeDisabledByFailures = true;
                        }
                     } else {
                        this.bridgeEarlyFailureCount = 0;
                        this.bridgeDisabledUntilMs = System.currentTimeMillis() + BRIDGE_NATIVE_CRASH_RESTART_DELAY_MS;
                     }
                  } else if (uptime < BRIDGE_STABLE_UPTIME_MS) {
                     this.bridgeEarlyFailureCount++;
                     if (this.bridgeEarlyFailureCount >= BRIDGE_MAX_EARLY_FAILURES) {
                        this.bridgeDisabledByFailures = true;
                     }
                  } else {
                     this.bridgeEarlyFailureCount = 0;
                  }
               }
               this.stopBridgeProcess();
            }
         }
      }, "fevervisual-mediainfo-bridge-reader");
      reader.setDaemon(true);
      this.bridgeReaderThread = reader;
      reader.start();
   }

   private boolean canUseBridgeProcess() {
      if (this.bridgeDisabledByFailures) {
         return false;
      }

      return System.currentTimeMillis() >= this.bridgeDisabledUntilMs;
   }

   private void handleBridgeLine(String line) {
      if (line == null || line.isEmpty()) {
         return;
      }
      if ("NONE".equals(line)) {
         clearMediaStateIfStale();
         return;
      }

      String[] parts = line.split("\t", -1);
      if (parts.length < 8 || !"DATA".equals(parts[0])) {
         return;
      }

      try {
         String owner = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
         String title = new String(Base64.getDecoder().decode(parts[2]), StandardCharsets.UTF_8);
         String artist = new String(Base64.getDecoder().decode(parts[3]), StandardCharsets.UTF_8);
         long position = Long.parseLong(parts[4]);
         long duration = Long.parseLong(parts[5]);
         boolean playing = "1".equals(parts[6]) || "true".equalsIgnoreCase(parts[6]);
         byte[] artwork = parts[7].isEmpty() ? new byte[0] : Base64.getDecoder().decode(parts[7]);

         MediaInfo info = new MediaInfo(title, artist, artwork, position, duration, playing);
         this.mediaInfo = info;
         this.session = new BridgeMediaSession(owner);
         this.lastMediaEventMs = System.currentTimeMillis();

         String trackId = artist + " - " + title;
         if (!trackId.equals(lastTrack) && running.get()) {
            lastTrack = trackId;
            lyrics = "";
            this.mediaExecutor.execute(() -> {
               if (running.get()) {
                  String l = LyricsFetcher.fetchFromGenius(artist, title);
                  if (running.get() && l != null) {
                     lyrics = l;
                  }
               }
            });
         }

         mc.execute(() -> syncArtworkTexture(info));
      } catch (Exception ignored) {
      }
   }

   private void sendBridgeCommand(String command) {
      BufferedWriter writer = this.bridgeCommandWriter;
      Process process = this.bridgeProcess;
      if (writer == null || process == null || !process.isAlive()) {
         return;
      }

      try {
         writer.write(command);
         writer.newLine();
         writer.flush();
      } catch (IOException ignored) {
      }
   }

   private String getJavaBinaryPath() {
      String javaHome = System.getProperty("java.home");
      if (javaHome == null || javaHome.isEmpty()) {
         return "java";
      }
      Path javaPath = Path.of(javaHome, "bin", System.getProperty("os.name", "").toLowerCase().contains("windows") ? "java.exe" : "java");
      return javaPath.toString();
   }

   private String getBridgeErrorFile() {
      Path errorDir = Path.of(System.getProperty("java.io.tmpdir"), "fevervisual-media-bridge");
      try {
         Files.createDirectories(errorDir);
      } catch (IOException ignored) {
      }
      return errorDir.resolve("hs_err_pid%p.log").toString();
   }

   private int getBridgeExitCode(Process process) {
      try {
         if (!process.waitFor(50L, TimeUnit.MILLISECONDS)) {
            return 0;
         }
         return process.exitValue();
      } catch (InterruptedException ignored) {
         Thread.currentThread().interrupt();
         return 0;
      } catch (IllegalThreadStateException ignored) {
         return 0;
      }
   }

   private String buildBridgeClasspath() {
      Set<String> entries = new LinkedHashSet<>();
      String currentClasspath = System.getProperty("java.class.path", "");
      if (!currentClasspath.isEmpty()) {
         for (String entry : currentClasspath.split(java.io.File.pathSeparator)) {
            if (entry != null && !entry.isBlank()) {
               this.addBridgeClasspathEntry(entries, Path.of(entry));
            }
         }
      }

      this.addFabricLoaderClasspathEntries(entries);

      Path codeSource = this.getCodeSourcePath();
      if (codeSource != null) {
         this.addBridgeClasspathEntry(entries, codeSource);
      }

      return String.join(java.io.File.pathSeparator, entries);
   }

   private void addFabricLoaderClasspathEntries(Set<String> entries) {
      try {
         FabricLoader.getInstance().getModContainer(FeverVisual.MOD_ID).ifPresent(container -> {
            this.addModContainerClasspathEntries(entries, container);
            container.getContainingMod().ifPresent(parent -> this.addModContainerClasspathEntries(entries, parent));
         });
      } catch (Throwable ignored) {
      }
   }

   private void addModContainerClasspathEntries(Set<String> entries, ModContainer container) {
      for (Path root : container.getRootPaths()) {
         this.addBridgeClasspathEntry(entries, root);
      }

      try {
         ModOrigin origin = container.getOrigin();
         if (origin.getKind() == ModOrigin.Kind.PATH) {
            for (Path path : origin.getPaths()) {
               this.addBridgeClasspathEntry(entries, path);
            }
         }
      } catch (Throwable ignored) {
      }
   }

   private void addBridgeClasspathEntry(Set<String> entries, Path path) {
      if (path == null || !this.isDefaultFileSystemPath(path)) {
         return;
      }

      entries.add(path.toString());
      if (Files.isRegularFile(path) && path.toString().endsWith(".jar")) {
         entries.addAll(this.extractNestedBridgeLibraries(path));
      }
   }

   private boolean isDefaultFileSystemPath(Path path) {
      try {
         return path.getFileSystem().equals(FileSystems.getDefault());
      } catch (Throwable ignored) {
         return false;
      }
   }

   private Path getCodeSourcePath() {
      try {
         URL location = MusicTracker.class.getProtectionDomain().getCodeSource().getLocation();
         if (location == null) {
            return null;
         }

         return Path.of(location.toURI());
      } catch (Exception ignored) {
         return null;
      }
   }

   private Set<String> extractNestedBridgeLibraries(Path modJar) {
      Set<String> entries = new LinkedHashSet<>();
      Path outputDir = Path.of(System.getProperty("java.io.tmpdir"), "fevervisual-media-bridge");
      try {
         Files.createDirectories(outputDir);
         try (JarFile jarFile = new JarFile(modJar.toFile())) {
            Enumeration<JarEntry> jarEntries = jarFile.entries();
            while (jarEntries.hasMoreElements()) {
               JarEntry entry = jarEntries.nextElement();
               String name = entry.getName();
               if (entry.isDirectory() || !name.startsWith("META-INF/jars/") || !name.endsWith(".jar")) {
                  continue;
               }

               String fileName = name.substring(name.lastIndexOf('/') + 1);
               Path extracted = outputDir.resolve(fileName);
               long expectedSize = entry.getSize();
               if (!Files.exists(extracted) || expectedSize >= 0 && Files.size(extracted) != expectedSize) {
                  Files.copy(jarFile.getInputStream(entry), extracted, StandardCopyOption.REPLACE_EXISTING);
               }
               entries.add(extracted.toString());
            }
         }
      } catch (Exception ignored) {
      }
      return entries;
   }

   private void stopBridgeProcess() {
      BufferedWriter writer = this.bridgeCommandWriter;
      this.bridgeCommandWriter = null;
      if (writer != null) {
         try {
            writer.close();
         } catch (IOException ignored) {
         }
      }

      Process process = this.bridgeProcess;
      this.bridgeProcess = null;
      if (process != null) {
         process.destroy();
         try {
            if (!process.waitFor(200, TimeUnit.MILLISECONDS)) {
               process.destroyForcibly();
            }
         } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
         }
      }
   }

   private void clearMediaStateIfStale() {
      if (System.currentTimeMillis() - lastMediaEventMs < MEDIA_STALE_TIMEOUT_MS) {
         return;
      }

      mediaInfo = EMPTY_MEDIA;
      session = null;
      mc.execute(() -> {
         if (artworkRegistered) {
            mc.getTextureManager().destroyTexture(artworkId);
            artworkRegistered = false;
         }
         textureCache.clear();
         colorCache.clear();
      });
   }

   private static int fingerprintArtwork(MediaInfo info) {
      byte[] png = info.getArtworkPng();
      if (png != null && png.length > 0) {
         return Arrays.hashCode(png);
      }

      try {
         BufferedImage bi = info.getArtwork();
         if (bi != null && bi.getWidth() > 0 && bi.getHeight() > 0) {
            int w = bi.getWidth();
            int h = bi.getHeight();
            return Objects.hash(w, h, bi.getRGB(0, 0), bi.getRGB(w - 1, h - 1), bi.getRGB(w / 2, h / 2));
         }
      } catch (Exception ignored) {
      }

      return 0;
   }

   private static NativeImage decodeArtwork(MediaInfo info) {
      byte[] png = info.getArtworkPng();
      if (png != null && png.length > 0) {
         try {
            return NativeImage.read(new ByteArrayInputStream(png));
         } catch (Exception ignored) {
         }
      }

      try {
         BufferedImage bi = info.getArtwork();
         if (bi != null && bi.getWidth() > 0 && bi.getHeight() > 0) {
            return fromBufferedImage(bi);
         }
      } catch (Exception ignored) {
      }

      return null;
   }

   private static NativeImage fromBufferedImage(BufferedImage img) {
      int w = img.getWidth();
      int h = img.getHeight();
      BufferedImage argb = img;
      if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
         argb = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
         Graphics2D g = argb.createGraphics();
         try {
            g.drawImage(img, 0, 0, null);
         } finally {
            g.dispose();
         }
      }

      NativeImage out = new NativeImage(NativeImage.Format.RGBA, w, h, false);
      for (int y = 0; y < h; y++) {
         for (int x = 0; x < w; x++) {
            out.setColorArgb(x, y, argb.getRGB(x, y));
         }
      }
      return out;
   }

   private void syncArtworkTexture(MediaInfo info) {
      int fingerprint = fingerprintArtwork(info);
      if (fingerprint == lastArtworkFingerprint && artworkRegistered) {
         return;
      }
      lastArtworkFingerprint = fingerprint;

      NativeImage image = decodeArtwork(info);
      if (image == null) {
         if (artworkRegistered) {
            mc.getTextureManager().destroyTexture(artworkId);
            artworkRegistered = false;
         }
         return;
      }

      try {
         if (artworkRegistered) {
            mc.getTextureManager().destroyTexture(artworkId);
         }
         mc.getTextureManager().registerTexture(artworkId, new NativeImageBackedTexture(() -> "Fever Visual album artwork", image));
         artworkRegistered = true;
         mediaColor = getAverageColor(image, 1);
      } catch (Exception ignored) {
         artworkRegistered = false;
      }
   }

   public ColorRGBA getAverageColor(NativeImage image, int step) {
      int width = image.getWidth();
      int height = image.getHeight();
      long totalR = 0L;
      long totalG = 0L;
      long totalB = 0L;
      int sampledPixels = 0;

      for (int y = 0; y < height; y += step) {
         for (int x = 0; x < width; x += step) {
            int argb = image.getColorArgb(x, y);
            int a = argb >> 24 & 0xFF;
            if (a != 0) {
               totalR += argb >> 16 & 0xFF;
               totalG += argb >> 8 & 0xFF;
               totalB += argb & 0xFF;
               sampledPixels++;
            }
         }
      }

      if (sampledPixels == 0) {
         return ColorRGBA.WHITE;
      }

      return new ColorRGBA(
              (float) totalR / sampledPixels,
              (float) totalG / sampledPixels,
              (float) totalB / sampledPixels
      );
   }

   private final class BridgeMediaSession implements IMediaSession {
      private final String owner;

      private BridgeMediaSession(String owner) {
         this.owner = owner == null ? "" : owner;
      }

      @Override
      public String getOwner() {
         return this.owner;
      }

      @Override
      public MediaInfo getMedia() {
         return MusicTracker.this.mediaInfo;
      }

      @Override
      public void play() {
         MusicTracker.this.sendBridgeCommand("PLAY");
      }

      @Override
      public void pause() {
         MusicTracker.this.sendBridgeCommand("PAUSE");
      }

      @Override
      public void playPause() {
         MusicTracker.this.sendBridgeCommand("PLAY_PAUSE");
      }

      @Override
      public void stop() {
         MusicTracker.this.sendBridgeCommand("STOP");
      }

      @Override
      public void next() {
         MusicTracker.this.sendBridgeCommand("NEXT");
      }

      @Override
      public void previous() {
         MusicTracker.this.sendBridgeCommand("PREVIOUS");
      }
   }

   public void shutdown() {
      running.set(false);
      mediaExecutor.shutdownNow();
      this.stopBridgeProcess();

      session = null;
      mediaInfo = EMPTY_MEDIA;
      lyrics = "";
      lastTrack = "";
      mc.execute(() -> {
         if (artworkRegistered) {
            mc.getTextureManager().destroyTexture(artworkId);
            artworkRegistered = false;
         }
         textureCache.clear();
         colorCache.clear();
      });
   }

   public Identifier getImage() {
      return artworkRegistered ? artworkId : null;
   }

   public boolean haveActiveSession() {
      if (!running.get()) return false;

      boolean recent = System.currentTimeMillis() - lastMediaEventMs < MEDIA_STALE_TIMEOUT_MS;
      boolean playing = mediaInfo != null && mediaInfo.getPlaying();
      boolean hasContent = mediaInfo != null &&
              (!mediaInfo.getTitle().isEmpty() || !mediaInfo.getArtist().isEmpty());

      return (recent || playing) && hasContent;
   }

   public IMediaSession getSession() {
      return session;
   }

   public MediaInfo getMediaInfo() {
      return mediaInfo;
   }

   public ColorRGBA getMediaColor() {
      return mediaColor;
   }

   public String getLyrics() {
      return lyrics;
   }

   public String getLastTrack() {
      return lastTrack;
   }
}

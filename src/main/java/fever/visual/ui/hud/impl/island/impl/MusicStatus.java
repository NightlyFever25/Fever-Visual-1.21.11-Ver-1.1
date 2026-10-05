package fever.visual.ui.hud.impl.island.impl;

import dev.redstones.mediaplayerinfo.MediaInfo;
import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.window.MouseScrollEvent;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.ui.hud.impl.island.DynamicIsland;
import fever.visual.ui.hud.impl.island.ExtandableStatus;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.math.MathUtility;
import fever.visual.utility.render.RenderUtility;
import fever.visual.utility.render.obj.Rect;
import fever.visual.utility.sounds.MusicTracker;
import net.minecraft.util.Identifier;

public class MusicStatus extends ExtandableStatus implements IMinecraft {
   private static final Identifier NO_IMAGE_ICON = FeverVisual.id("icons/music/no_image.png");
   private static final Identifier PREVIOUS_ICON = FeverVisual.id("icons/music/previous.png");
   private static final Identifier PLAY_ICON = FeverVisual.id("icons/music/play.png");
   private static final Identifier PAUSE_ICON = FeverVisual.id("icons/music/pause.png");
   private static final Identifier NEXT_ICON = FeverVisual.id("icons/music/next.png");
   private static final Identifier TEXT_ICON = FeverVisual.id("icons/music/text.png");
   private static final Identifier YANDEX_MUSIC_ICON = FeverVisual.id("icons/media/yandex_music.png");
   private static final Identifier EDGE_ICON = FeverVisual.id("icons/media/edge.png");
   private static final Identifier SPOTIFY_ICON = FeverVisual.id("icons/media/spotify.png");

   private final Animation[] waveAnims = new Animation[4];
   private final Animation pausingAnim = new Animation(300L, 0.0F, Easing.BAKEK_SIZE);
   private final Animation hoverPrevious = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation hoverPause = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation hoverNext = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation hoverLyrics = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private boolean showLyrics = false;
   private int lyricsOffset = 0;

   private final EventListener<MouseScrollEvent> onMouseScroll = event -> {
      if (this.showLyrics) {
         DynamicIsland island = FeverVisual.getInstance().getHud().getIsland();
         if (island.active() == this && island.isExtended()) {
            if (event.getVerticalAmount() < 0.0) {
               this.lyricsOffset++;
            } else {
               if (!(event.getVerticalAmount() > 0.0)) {
                  return;
               }
               this.lyricsOffset--;
            }

            String lyrics = FeverVisual.getInstance().getMusicTracker().getLyrics();
            if (lyrics != null && !lyrics.isEmpty()) {
               String[] lines = lyrics.split("\\n");
               int maxOffset = Math.max(0, lines.length - 6);
               this.lyricsOffset = Math.min(Math.max(0, this.lyricsOffset), maxOffset);
            }
         }
      }
   };

   public MusicStatus(SelectSetting setting) {
      super(setting, "music");

      for (int i = 0; i < this.waveAnims.length; i++) {
         this.waveAnims[i] = new Animation(400L, 0.0F, Easing.LINEAR);
      }

      FeverVisual.getInstance().getEventManager().subscribe(this);
   }

   @Override
   public void draw(CustomDrawContext context) {
      DynamicIsland island = FeverVisual.getInstance().getHud().getIsland();
      float x = sr.getScaledWidth() / 2.0F - island.getSize().width / 2.0F;
      float y = 7.0F;
      MusicTracker tracker = FeverVisual.getInstance().getMusicTracker();
      ColorRGBA textColor = Colors.getTextColor();

      if (tracker != null && tracker.haveActiveSession() && tracker.getSession() != null) {
         MediaInfo media = tracker.getSession().getMedia();
         float expWidth = 164.0F;
         float expHeight = this.showLyrics ? 125.0F : 80.0F;
         float maxWidth = 100.0F;
         float defaultWidth = 32.0F + Fonts.MEDIUM.getFont(7.0F).width(media.getTitle());
         float width = this.size.width = island.isExtended() ? expWidth : Math.min(defaultWidth, maxWidth);
         float height = this.size.height = island.isExtended() ? expHeight : 15.0F;
         float extending = island.getExtendingAnim().getValue();
         float imageMargin = 4.0F + 6.0F * extending;
         float imageSize = 7.0F + 19.0F * extending;
         Identifier trackImage = tracker.getImage() != null ? tracker.getImage() : NO_IMAGE_ICON;
         float imageY = y + (island.isExtended() ? imageMargin : GuiUtility.getMiddleOfBox(imageSize, island.getSize().height));

         context.drawRoundedTexture(
                 trackImage, x + imageMargin - 10.0F + 10.0F * this.animation.getValue(), imageY, imageSize, imageSize, BorderRadius.all(1.0F + 5.0F * extending)
         );

         context.drawFadeoutText(
                 Fonts.MEDIUM.getFont(7.0F),
                 media.getTitle(),
                 x - 5.0F + 10.0F * this.animation.getValue() + 10.0F * this.animation.getValue() + 29.0F * extending,
                 y + 5.0F + 11.0F * extending,
                 textColor,
                 0.3F,
                 0.7F,
                 island.isExtended() ? expWidth - 30.0F : maxWidth + 5.0F
         );

         if (extending != 0.0F && tracker.getSession() != null) {
            context.drawFadeoutText(
                    Fonts.REGULAR.getFont(7.0F),
                    media.getArtist(),
                    x + 20.0F + 24.0F * extending,
                    y + 5.0F + 19.0F * extending,
                    textColor.withAlpha(178.5F * extending),
                    0.3F,
                    0.7F,
                    island.isExtended() ? expWidth - 30.0F : maxWidth + 5.0F
            );

            context.drawText(
                    Fonts.REGULAR.getFont(5.0F),
                    formatTime(media.getPosition()),
                    sr.getScaledWidth() / 2.0F - expWidth / 2.0F + 11.0F * extending,
                    y + 43.0F * extending,
                    textColor.withAlpha(255.0F)
            );

            context.drawText(
                    Fonts.REGULAR.getFont(5.0F),
                    formatTime(media.getDuration()),
                    sr.getScaledWidth() / 2.0F + expWidth / 2.0F - (9.5F + Fonts.REGULAR.getFont(5.0F).width(formatTime(media.getDuration())) * extending),
                    y + 43.0F * extending,
                    textColor.withAlpha(255.0F)
            );

            float barWidth = 116.0F;
            float barX = sr.getScaledWidth() / 2.0F - barWidth / 2.0F;
            context.drawRoundedRect(
                    barX, y + expHeight - (this.showLyrics ? 45 : 0) - 36.5F * extending, barWidth, 3.0F, BorderRadius.all(0.5F), textColor.withAlpha(63.75F)
            );

            float progressWidth = barWidth * Math.min(1.0F, (float) media.getPosition() / (float) media.getDuration());
            context.drawRoundedRect(
                    barX, y + expHeight - (this.showLyrics ? 45 : 0) - 36.5F * extending, progressWidth, 3.0F, BorderRadius.all(0.5F), textColor.withAlpha(150.0F)
            );

            this.pausingAnim.setDuration(600L);
            this.pausingAnim.update(media.getPlaying() ? 1.0F : 0.0F);

            if (extending > 0.7F) {
               float controlY = y + expHeight - 25.0F * extending;
               double mouseX = GuiUtility.getMouse().x();
               double mouseY = GuiUtility.getMouse().y();
               Rect previous = new Rect(sr.getScaledWidth() / 2.0F - 40.0F, controlY, 16.0F, 16.0F);
               Rect pause = new Rect(sr.getScaledWidth() / 2.0F - 8.0F, controlY, 16.0F, 16.0F);
               Rect next = new Rect(sr.getScaledWidth() / 2.0F + 24.0F, controlY, 16.0F, 16.0F);

               if (previous.hovered(mouseX, mouseY) || pause.hovered(mouseX, mouseY) || next.hovered(mouseX, mouseY)) {
                  CursorUtility.set(CursorType.HAND);
               }

               this.hoverPrevious.update(previous.hovered(mouseX, mouseY));
               this.hoverPause.update(pause.hovered(mouseX, mouseY));
               this.hoverNext.update(next.hovered(mouseX, mouseY));

               context.drawTexture(PREVIOUS_ICON, previous, textColor.withAlpha(255.0F - 100.0F * this.hoverPrevious.getValue()));

               float anim = this.pausingAnim.getValue();
               float centerX = pause.getX() + pause.getWidth() / 2.0F;
               float centerY = pause.getY() + pause.getHeight() / 2.0F;

               RenderUtility.rotate(context.getMatrices(), centerX, centerY, 90.0F * anim);
               RenderUtility.scale(context.getMatrices(), centerX, centerY, 1.0F - anim);
               context.drawTexture(
                       PLAY_ICON, pause, textColor.withAlpha(255.0F * (1.0F - anim) - 100.0F * this.hoverPause.getValue())
               );
               RenderUtility.end(context.getMatrices());
               RenderUtility.end(context.getMatrices());

               RenderUtility.rotate(context.getMatrices(), centerX, centerY, -90.0F + 90.0F * anim);
               RenderUtility.scale(context.getMatrices(), centerX, centerY, anim);
               context.drawTexture(PAUSE_ICON, pause, textColor.withAlpha(255.0F * anim - 100.0F * this.hoverPause.getValue()));
               RenderUtility.end(context.getMatrices());
               RenderUtility.end(context.getMatrices());

               context.drawTexture(NEXT_ICON, next, textColor.withAlpha(255.0F - 100.0F * this.hoverNext.getValue()));
            }
            String owner = null;
            String ownerName = tracker.getSession().getOwner();
            if (ownerName != null) {
               if (ownerName.toLowerCase().contains("yandex") || ownerName.toLowerCase().contains("яндекс")) {
                  owner = "yandex_music";
               } else if (ownerName.toLowerCase().contains("edge")) {
                  owner = "edge";
               } else if (ownerName.toLowerCase().contains("spotify")) {
                  owner = "spotify";
               }
            }

            if (owner != null) {
               context.drawTexture(this.getOwnerIcon(owner), x + expWidth - 22.0F, y + expHeight - 21.0F, 8.0F, 8.0F, ColorRGBA.WHITE);
            }
            String lyrics = tracker.getLyrics();
            boolean hasLyrics = lyrics != null && !lyrics.isEmpty();

            Rect lyricsRect = new Rect(x + 14.0F, y + expHeight - 21.0F, 8.0F, 8.0F);
            if (hasLyrics) {
               if (lyricsRect.hovered(GuiUtility.getMouse().x(), GuiUtility.getMouse().y())) {
                  CursorUtility.set(CursorType.HAND);
               }

               this.hoverLyrics.update(lyricsRect.hovered(GuiUtility.getMouse().x(), GuiUtility.getMouse().y()));
               context.drawTexture(TEXT_ICON, lyricsRect, textColor.withAlpha(255.0F - 100.0F * this.hoverLyrics.getValue()));
            }

            if (this.showLyrics && hasLyrics) {
               String[] lines = lyrics.split("\\n");
               int maxLines = Math.min(6, lines.length);
               if (this.lyricsOffset > lines.length - maxLines) {
                  this.lyricsOffset = Math.max(lines.length - maxLines, 0);
               }

               for (int i = 0; i < maxLines && i + this.lyricsOffset < lines.length; i++) {
                  context.drawFadeoutText(
                          Fonts.REGULAR.getFont(6.0F),
                          lines[i + this.lyricsOffset],
                          x + 10.0F,
                          y + 55.0F + i * 7,
                          textColor.withAlpha(255.0F * extending),
                          0.91F,
                          1.0F,
                          expWidth - 20.0F
                  );
               }
            }
         }
         for (int i = 0; i < this.waveAnims.length; i++) {
            float phase = (float) media.getPosition() * 8.0F + i * 0.7F;
            float size = media.getPlaying() ? (float) (2.0 + Math.abs(MathUtility.sin(phase)) * 8.0) : 3.0F;
            this.waveAnims[i].update(size);
            this.waveAnims[i].setDuration(1000L);
            context.drawRoundedRect(
                    x + MathUtility.interpolate(Math.min(defaultWidth, maxWidth), expWidth - 10.0F, extending)
                            - 2.0F - 10.0F * this.animation.getValue() + i * (2.0F + extending),
                    y + MathUtility.interpolate(4.25, 14.0, extending) + (7.0F - this.waveAnims[i].getValue()) / 2.0F,
                    1.0F + extending,
                    this.waveAnims[i].getValue(),
                    BorderRadius.all(0.5F),
                    tracker.getMediaColor()
            );
         }
      }
   }

   private Identifier getOwnerIcon(String owner) {
      return switch (owner) {
         case "yandex_music" -> YANDEX_MUSIC_ICON;
         case "edge" -> EDGE_ICON;
         case "spotify" -> SPOTIFY_ICON;
         default -> NO_IMAGE_ICON;
      };
   }

   @Override
   public void click(float mouseX, float mouseY, int button) {
      DynamicIsland island = FeverVisual.getInstance().getHud().getIsland();
      float x = sr.getScaledWidth() / 2.0F - island.getSize().width / 2.0F;
      float y = 7.0F;
      float width = this.size.width;
      float height = this.size.height;
      MusicTracker tracker = FeverVisual.getInstance().getMusicTracker();

      if (tracker != null && tracker.haveActiveSession() && tracker.getSession() != null) {
         var session = tracker.getSession();

         if (GuiUtility.isHovered(x + width / 2.0F - 40.0F, y + height - 9.0F - 16.0F, 16.0, 16.0, mouseX, mouseY)) {
            session.previous();
         }

         if (GuiUtility.isHovered(x + width / 2.0F - 8.0F, y + height - 9.0F - 16.0F, 16.0, 16.0, mouseX, mouseY)) {
            session.playPause();
         }

         if (GuiUtility.isHovered(x + width / 2.0F + 24.0F, y + height - 9.0F - 16.0F, 16.0, 16.0, mouseX, mouseY)) {
            session.next();
         }

         String lyrics = tracker.getLyrics();
         boolean hasLyrics = lyrics != null && !lyrics.isEmpty();
         Rect lyricsRect = new Rect(x + 14.0F, y + height - 21.0F, 8.0F, 8.0F);

         if (hasLyrics && GuiUtility.isHovered(lyricsRect.getX(), lyricsRect.getY(),
                 lyricsRect.getWidth(), lyricsRect.getHeight(), mouseX, mouseY)) {
            this.showLyrics = !this.showLyrics;
            if (this.showLyrics) {
               this.lyricsOffset = 0;
            }
         }
      }
   }

   @Override
   public boolean canShow() {
      MusicTracker tracker = FeverVisual.getInstance().getMusicTracker();
      if (tracker == null) return false;

      boolean hasSession = tracker.getSession() != null && tracker.haveActiveSession();
      if (!hasSession) return false;

      String owner = tracker.getSession().getOwner();
      return owner == null || !owner.toLowerCase().contains("gram");
   }

   public static String formatTime(long totalSeconds) {
      long minutes = totalSeconds / 60L;
      long seconds = totalSeconds % 60L;
      return String.format("%d:%02d", minutes, seconds);
   }

   @Override
   public ColorRGBA getColor() {
      MusicTracker tracker = FeverVisual.getInstance().getMusicTracker();
      if (tracker != null) {
         return super.getColor().mix(tracker.getMediaColor(), 0.2F);
      }
      return super.getColor();
   }
}

package fever.visual.ui.hud.impl.island;

import fever.visual.ui.hud.impl.island.impl.*;
import lombok.Generated;
import fever.visual.FeverVisual;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.player.ClientPlayerTickEvent;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.ui.hud.HudElement;
import fever.visual.ui.hud.impl.island.impl.*;
import fever.visual.ui.menu.dropdown.DropDownScreen;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.animation.types.ColorAnimation;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.TextUtility;
import fever.visual.utility.game.PlatformUtility;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.interfaces.IScaledResolution;
import fever.visual.utility.render.ScissorUtility;
import fever.visual.utility.time.Timer;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class DynamicIsland extends HudElement implements IMinecraft, IScaledResolution {
   private static final Identifier AIRPLANE_ICON = FeverVisual.id("icons/airplane.png");
   private static final int[] PING_THRESHOLDS = {450, 300, 150, 75};
   private static final ColorRGBA ADAPT_DARK = new ColorRGBA(0.0F, 0.0F, 0.0F);
   private static final ColorRGBA ADAPT_LIGHT = new ColorRGBA(255.0F, 255.0F, 255.0F);

   private final SelectSetting statuses = new SelectSetting(this, "hud.dynamic_island.statuses").draggable();
   private final List<IslandStatus> visibleStatuses = new ArrayList<>();
   private final IslandSize size = new IslandSize(48.0F, 15.0F);
   private boolean extended;
   private final Animation extendingAnim = new Animation(200L, 0.0F, Easing.LINEAR);
   private final Animation widthAnim = new Animation(500L, 0.0F, Easing.BAKEK_SIZE);
   private final Animation heightAnim = new Animation(500L, 0.0F, Easing.BAKEK_SIZE);
   private final Animation showPing = new Animation(500L, 0.0F, Easing.BAKEK);
   private final ColorAnimation backgroundColor = new ColorAnimation(300L, new ColorRGBA(0.0F, 0.0F, 0.0F), Easing.FIGMA_EASE_IN_OUT);
   private final ColorAnimation adaptColor = new ColorAnimation(300L, new ColorRGBA(255.0F, 255.0F, 255.0F), Easing.LINEAR);
   private final Timer timer = new Timer();
   private boolean dark;
   private boolean useDark;
   private IslandStatus last;
   private String cachedTime = "";
   private int cachedPing;
   private long lastTimeUpdate;
   private long lastPingUpdate;
   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (this.active() instanceof MusicStatus && mc.player.age % 2 == 0) {
      }
   };

   public DynamicIsland() {
      super("hud.dynamic_island", "icons/hud/island.png");
      new NotificationStatus(this.statuses);
      new PVPStatus(this.statuses);
      new EventStatus(this.statuses);
      new MineStatus(this.statuses);
      new MusicStatus(this.statuses);
      new DefaultStatus(this.statuses).alwaysEnabled();
      FeverVisual.getInstance().getFileManager().readFile("client");
      FeverVisual.getInstance().getEventManager().subscribe(this);
   }

   protected void renderComponent(UIContext context) {
      if (!Interface.isDynamicIslandEnabled()) {
         return;
      }
      this.width = this.size.width;
      this.height = this.size.height;
      this.x = sr.getScaledWidth() / 2.0F - this.width / 2.0F;
      this.y = 7.0F;
      BorderRadius radius = BorderRadius.all(7.0F + 11.0F * this.extendingAnim.getValue());
      long now = System.currentTimeMillis();
      if (now - this.lastTimeUpdate >= 1000L || this.cachedTime.isEmpty()) {
         this.cachedTime = TextUtility.getCurrentTime();
         this.lastTimeUpdate = now;
      }
      String time = this.cachedTime;
      if (this.timer.finished(2000L)) {
         float pixelX = mc.getWindow().getWidth() / 2.0F;
         float pixelY = mc.getWindow().getHeight() - (this.y + 5.0F);
         ColorRGBA pixel = ColorRGBA.fromPixel(pixelX, pixelY);
         boolean check = (pixel.getRed() + pixel.getGreen() + pixel.getBlue()) / 3.0F > 70.0F;
         this.useDark = check;
         this.timer.reset();
      }

      this.adaptColor.update(this.useDark ? ADAPT_DARK : ADAPT_LIGHT);
      this.dark = this.useDark;
      ColorRGBA elmtColor = this.adaptColor.getColor().withAlpha(255.0F * (1.0F - this.extendingAnim.getValue()));
      if (mc.player != null) {
         context.drawText(Fonts.MEDIUM.getFont(7.0F), time, this.x - Fonts.MEDIUM.getFont(9.0F).width(time) - 4.0F, this.y + 5.0F, elmtColor);
         if (!mc.isInSingleplayer() && mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid()) != null) {
            this.showPing.update(GuiUtility.isHovered(this.x + this.width + 4.0F + 4.0F * this.showPing.getValue(), this.y + 5.0F, 12.8F, 7.0, context));
            if (now - this.lastPingUpdate >= 500L) {
               this.cachedPing = mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid()).getLatency();
               this.lastPingUpdate = now;
            }
            int ping = this.cachedPing;
            context.drawText(
                    Fonts.MEDIUM.getFont(7.0F),
                    ping + " ms",
                    this.x + this.width + 4.0F + 4.0F * this.showPing.getValue(),
                    this.y + 5.0F,
                    elmtColor.mulAlpha(this.showPing.getValue())
            );

            for (int i = 0; i < 4; i++) {
               context.drawRoundedRect(
                       this.x + this.width + 9.0F + i * 2.7F + 4.0F * this.showPing.getValue(),
                       this.y + 8.0F - i,
                       2.0F,
                       3 + i,
                       BorderRadius.all(0.1F),
                       elmtColor.withAlpha(elmtColor.getAlpha() * 0.2F * (1.0F - this.showPing.getValue()))
               );
            }

            for (int i = 0; i < 4; i++) {
               if (ping < PING_THRESHOLDS[i]) {
                  context.drawRoundedRect(
                          this.x + this.width + 9.0F + i * 2.7F + 4.0F * this.showPing.getValue(),
                          this.y + 8.0F - i,
                          2.0F,
                          3 + i,
                          BorderRadius.all(0.1F),
                          elmtColor.mulAlpha(1.0F - this.showPing.getValue())
                  );
               }
            }
         } else {
            context.drawTexture(AIRPLANE_ICON, this.x + this.width + 8.0F, this.y + 3.5F, 8.0F, 8.0F, elmtColor);
         }
      }

      IslandStatus status = this.active();
      this.backgroundColor.update(status.getColor());
      boolean showGlass = Interface.showGlass();
      if (!showGlass) {
         context.drawShadow(
                 this.x - 5.0F,
                 this.y - 5.0F,
                 this.width + 10.0F,
                 this.height + 10.0F,
                 15.0F,
                 BorderRadius.all(6.0F),
                 ColorRGBA.BLACK.withAlpha(63.75F * this.dragAnim.getValue())
         );
      }
      if (!showGlass && Interface.showMinimalizm()) {
         context.drawBlurredRect(
                 this.x,
                 this.y,
                 this.width,
                 this.height,
                 45.0F,
                 7.0F,
                 BorderRadius.all(6.0F),
                 ColorRGBA.WHITE.withAlpha(255.0F * this.animation.getValue() * Interface.minimalizm())
         );
      }
      if (Interface.showGlass()) {
         context.drawLiquidGlass(
                 this.x,
                 this.y,
                 this.width,
                 this.height,
                 7.0F,
                 0.08F - 0.07F * this.dragAnim.getValue(),
                 BorderRadius.all(6.0F),
                 ColorRGBA.WHITE.withAlpha(255.0F * this.animation.getValue() * Interface.glass())
         );
      }
      if (!showGlass) {
         context.drawSquircle(
                 this.x - 1.0F,
                 this.y - 1.0F,
                 this.width + 2.0F,
                 this.height + 2.0F,
                 2.0F + 5.0F * this.extendingAnim.getValue(),
                 BorderRadius.all(8.0F + 11.0F * this.extendingAnim.getValue()),
                 Colors.WHITE.withAlpha(this.adaptColor.getColor().getRed() * 0.1F)
         );
      }
      if (!showGlass) {
         context.drawSquircle(
                 this.x, this.y, this.width, this.height, 2.0F + 5.0F * this.extendingAnim.getValue(), radius, this.backgroundColor.getColor().withAlpha(216.75F)
         );
      }

      for (SelectSetting.Value islandStatus : this.statuses.getValues()) {
         ((IslandStatus)islandStatus).getAnimation().update(status == islandStatus ? 1.0F : 0.0F);
      }

      ScissorUtility.push(context.getMatrices(), this.x, this.y, this.width, this.height);
      if (status.getAnimation().getValue() == 1.0F) {
         status.draw(context);
      } else {
         for (SelectSetting.Value islandStatus : this.statuses.getValues()) {
            if (((IslandStatus)islandStatus).getAnimation().getValue() > 0.0F) {
               ((IslandStatus)islandStatus).drawWithAlpha(context);
            }
         }
      }

      ScissorUtility.pop();
      this.widthAnim.update(status.size.width);
      this.heightAnim.update(status.size.height);
      this.size.width = this.widthAnim.getValue();
      this.size.height = this.heightAnim.getValue();
      this.extendingAnim.update(this.extended ? 1.0F : 0.0F);
      if (!(status instanceof ExtandableStatus)) {
         this.extended = false;
      }

      if (!PlatformUtility.isChatEditorOpen() && !(mc.currentScreen instanceof DropDownScreen) && mc.player != null || this.select) {
         this.extended = false;
      }

      if (!this.extended
              && status instanceof ExtandableStatus
              && GuiUtility.isHovered(
              (double)this.x, (double)this.y, (double)this.width, (double)this.height, (double)GuiUtility.getMouse().x(), (double)GuiUtility.getMouse().y()
      )) {
         CursorUtility.set(CursorType.HAND);
      }
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      super.onMouseClicked(mouseX, mouseY, button);
      this.handleClick((float)mouseX, (float)mouseY, button.getButtonIndex());
   }

   public boolean handleClick(float mouseX, float mouseY, int button) {
      float x = sr.getScaledWidth() / 2.0F - this.size.width / 2.0F;
      float y = 7.0F;
      if (this.extended) {
         if (!GuiUtility.isHovered((double)x, (double)y, (double)this.size.width, (double)this.size.height, (double)mouseX, (double)mouseY)) {
            this.extended = false;
         } else {
            this.active().click(mouseX, mouseY, button);
         }

         return true;
      } else if (GuiUtility.isHovered((double)x, (double)y, (double)this.size.width, (double)this.size.height, (double)mouseX, (double)mouseY)
              && this.active() instanceof ExtandableStatus) {
         this.extended = true;
         return true;
      } else {
         return false;
      }
   }

   public IslandStatus active() {
      return this.statuses().getLast();
   }

   public List<IslandStatus> statuses() {
      this.visibleStatuses.clear();
      List<SelectSetting.Value> values = this.statuses.getValues();
      for (int i = values.size() - 1; i >= 0; i--) {
         SelectSetting.Value value = values.get(i);
         IslandStatus islandStatus = (IslandStatus)value;
         if (islandStatus.canShow() && value.isSelected()) {
            this.visibleStatuses.add(islandStatus);
         }
      }
      return this.visibleStatuses;
   }

   @Generated
   public SelectSetting getStatuses() {
      return this.statuses;
   }

   @Generated
   public IslandSize getSize() {
      return this.size;
   }

   @Generated
   public boolean isExtended() {
      return this.extended;
   }

   @Generated
   public Animation getExtendingAnim() {
      return this.extendingAnim;
   }

   @Generated
   @Override
   public Animation getWidthAnim() {
      return this.widthAnim;
   }

   @Generated
   @Override
   public Animation getHeightAnim() {
      return this.heightAnim;
   }

   @Generated
   public Animation getShowPing() {
      return this.showPing;
   }

   @Generated
   public ColorAnimation getBackgroundColor() {
      return this.backgroundColor;
   }

   @Generated
   public ColorAnimation getAdaptColor() {
      return this.adaptColor;
   }

   @Generated
   public Timer getTimer() {
      return this.timer;
   }

   @Generated
   public boolean isDark() {
      return this.dark;
   }

   @Generated
   public boolean isUseDark() {
      return this.useDark;
   }

   @Generated
   public IslandStatus getLast() {
      return this.last;
   }

   @Generated
   public EventListener<ClientPlayerTickEvent> getOnTick() {
      return this.onTick;
   }
}

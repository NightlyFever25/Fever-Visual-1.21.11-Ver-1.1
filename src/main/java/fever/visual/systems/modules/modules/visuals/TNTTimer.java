package fever.visual.systems.modules.modules.visuals;

import fever.visual.framework.msdf.Fonts;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.PreHudRenderEvent;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.render.Utils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ModuleInfo(name = "TNT Timer", category = ModuleCategory.VISUALS, desc = "modules.descriptions.tnt_timer")
public class TNTTimer extends BaseModule {
   private final List<TntEntity> visibleTnt = new ArrayList<>();
   private final EventListener<PreHudRenderEvent> onHudRenderEvent = event -> {
      MatrixStack matrices = event.getContext().getMatrices();
      this.visibleTnt.clear();
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof TntEntity tnt) {
            this.visibleTnt.add(tnt);
         }
      }

      for (TntEntity tnt : this.visibleTnt) {
         this.renderBack(event, matrices, tnt);
      }
      for (TntEntity tnt : this.visibleTnt) {
         this.renderText(event, matrices, tnt);
      }

   };

   private void renderBack(PreHudRenderEvent event, MatrixStack matrices, TntEntity entity) {
      String text = this.getTimerText(entity);
      Vec3d renderPos = entity.getLerpedPos(event.getTickDelta()).add(0.0, 0.5, 0.0);
      Vec2f screenPos = Utils.worldToScreen(renderPos);
      if (screenPos != null) {
         float distance = (float)mc.player.getEntityPos().distanceTo(renderPos);
         float scale = MathHelper.clamp(1.0F - distance / 20.0F, 0.5F, 1.0F);
         matrices.push();
         matrices.translate(screenPos.x - 6.0F, screenPos.y, 0.0F);
         matrices.scale(scale, scale, 1.0F);
         int width = (int)Fonts.MEDIUM.getFont(11.0F).width(text);
         int x = -width / 2;
         event.getContext().drawRect(x - 3, 1.0F, width + 26, Fonts.MEDIUM.getFont(11.0F).height() + 8.0F, new ColorRGBA(0.0F, 0.0F, 0.0F, 100.0F));
         matrices.pop();
      }
   }

   private void renderText(PreHudRenderEvent event, MatrixStack matrices, TntEntity entity) {
      String text = this.getTimerText(entity);
      Vec3d renderPos = entity.getLerpedPos(event.getTickDelta()).add(0.0, 0.5, 0.0);
      Vec2f screenPos = Utils.worldToScreen(renderPos);
      if (screenPos != null) {
         float distance = (float)mc.player.getEntityPos().distanceTo(renderPos);
         float scale = MathHelper.clamp(1.0F - distance / 20.0F, 0.5F, 1.0F);
         matrices.push();
         matrices.translate(screenPos.x - 6.0F, screenPos.y, 0.0F);
         matrices.scale(scale, scale, 1.0F);
         int width = (int)Fonts.MEDIUM.getFont(11.0F).width(text);
         int x = -width / 2;
         event.getContext().drawText(Fonts.MEDIUM.getFont(11.0F), text, x + 16, 5.0F, ColorRGBA.WHITE);
         event.getContext().drawItem(Items.TNT, (float)x, 3.0F, 0.75F);
         matrices.pop();
      }
   }

   private String getTimerText(TntEntity entity) {
      float seconds = Math.max(0.0F, entity.getFuse() / 20.0F);
      return String.format(Locale.ROOT, "%.1f %s", seconds, Localizator.translate("sec"));
   }
}

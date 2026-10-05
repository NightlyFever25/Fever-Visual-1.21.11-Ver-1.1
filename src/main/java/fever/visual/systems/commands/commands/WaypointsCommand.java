package fever.visual.systems.commands.commands;

import java.util.Map.Entry;
import fever.visual.FeverVisual;
import fever.visual.framework.msdf.Fonts;
import fever.visual.systems.commands.Command;
import fever.visual.systems.commands.CommandBuilder;
import fever.visual.systems.commands.CommandContext;
import fever.visual.systems.commands.ValidationResult;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.HudRenderEvent;
import fever.visual.systems.waypoints.WayPointsManager;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.game.MessageUtility;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.interfaces.IScaledResolution;
import fever.visual.utility.render.Utils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class WaypointsCommand implements IMinecraft, IScaledResolution {
   private final EventListener<HudRenderEvent> onHudRenderEvent = event -> {
      MatrixStack matrices = event.getContext().getMatrices();
      this.renderBack(event, matrices);
      this.renderText(event, matrices);
   };

   public WaypointsCommand() {
      FeverVisual.getInstance().getEventManager().subscribe(this);
   }

   public Command command() {
      return CommandBuilder.begin("waypoint")
         .aliases("way")
         .desc("Метки")
         .param("action", p -> p.literal("add", "del", "clear"))
         .param("name", p -> p.optional().validator(ValidationResult::ok))
         .param("x", p -> p.optional().validator(this::verifyCoordinate))
         .param("y", p -> p.optional().validator(this::verifyCoordinate))
         .param("z", p -> p.optional().validator(this::verifyCoordinate))
         .handler(this::handle)
         .build();
   }

   private ValidationResult verifyCoordinate(String input) {
      try {
         Integer.parseInt(input);
         return ValidationResult.ok(input);
      } catch (NumberFormatException var3) {
         return ValidationResult.error("Не правильное число");
      }
   }

   private void handle(CommandContext ctx) {
      String action = (String)ctx.arguments().get(0);
      String name = (String)ctx.arguments().get(1);
      String x = (String)ctx.arguments().get(2);
      String y = (String)ctx.arguments().get(3);
      String z = (String)ctx.arguments().get(4);
      WayPointsManager wayPointsManager = FeverVisual.getInstance().getWayPointsManager();
      String var8 = action.toLowerCase();
      switch (var8) {
         case "add":
            if (name == null || x == null || y == null || z == null) {
               MessageUtility.error(Text.of("Укажите название и координаты (.way add \"Название\" x y z)"));
               return;
            }

            try {
               wayPointsManager.add(name, Integer.parseInt(x), Integer.parseInt(y), Integer.parseInt(z));
            } catch (NumberFormatException var11) {
               MessageUtility.error(Text.of("Координаты должны быть числами"));
            }
            break;
         case "del":
            if (name == null) {
               MessageUtility.error(Text.of("Укажите название (.way del \"Название\")"));
               return;
            }

            wayPointsManager.del(name);
            break;
         case "clear":
            wayPointsManager.clear();
      }
   }

   private void renderBack(HudRenderEvent event, MatrixStack matrices) {
      for (Entry<String, Vec3d> entry : FeverVisual.getInstance().getWayPointsManager().getEntries()) {
         String name = entry.getKey();
         Vec3d pos = entry.getValue();
         Vec3d renderPos = pos.add(0.0, 0.5, 0.0);
         Vec2f screenPos = Utils.worldToScreen(renderPos);
         if (screenPos != null) {
            float distance = (float)mc.player.getEntityPos().distanceTo(pos.add(0.5, 0.5, 0.5));
            float scale = MathHelper.clamp(1.0F - distance / 20.0F, 0.5F, 1.0F);
            matrices.push();
            matrices.translate(screenPos.x, screenPos.y, 0.0F);
            matrices.scale(scale, scale, 1.0F);
            int textWidth = (int)Fonts.MEDIUM.getFont(11.0F).width(name + " " + String.format("%.1f", mc.player.getEntityPos().distanceTo(pos)) + "m");
            int x = -textWidth / 2;
            int y = 5;
            event.getContext().drawRect(x - 3, y - 3, textWidth + 8, Fonts.MEDIUM.getFont(11.0F).height() + 6.0F, new ColorRGBA(0.0F, 0.0F, 0.0F, 100.0F));
            matrices.pop();
         }
      }
   }

   private void renderText(HudRenderEvent event, MatrixStack matrices) {
      for (Entry<String, Vec3d> entry : FeverVisual.getInstance().getWayPointsManager().getEntries()) {
         String name = entry.getKey();
         Vec3d pos = entry.getValue();
         Vec3d renderPos = pos.add(0.0, 0.5, 0.0);
         Vec2f screenPos = Utils.worldToScreen(renderPos);
         if (screenPos != null) {
            float distance = (float)mc.player.getEntityPos().distanceTo(pos.add(0.5, 0.5, 0.5));
            float scale = MathHelper.clamp(1.0F - distance / 20.0F, 0.5F, 1.0F);
            matrices.push();
            matrices.translate(screenPos.x, screenPos.y, 0.0F);
            matrices.scale(scale, scale, 1.0F);
            int textWidth = (int)Fonts.MEDIUM.getFont(11.0F).width(name + " " + String.format("%.1f", mc.player.getEntityPos().distanceTo(pos)) + "m");
            int x = -textWidth / 2;
            int y = 5;
            event.getContext()
               .drawText(Fonts.MEDIUM.getFont(11.0F), name + " " + String.format("%.1f", mc.player.getEntityPos().distanceTo(pos)) + "m", x, y, ColorRGBA.WHITE);
            matrices.pop();
         }
      }
   }
}

package fever.visual.systems.commands.commands;

import fever.visual.FeverVisual;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.systems.commands.Command;
import fever.visual.systems.commands.CommandBuilder;
import fever.visual.systems.commands.CommandContext;
import fever.visual.systems.commands.ParameterValidator;
import fever.visual.systems.commands.ValidationResult;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.HudRenderEvent;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.game.MessageUtility;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.render.Utils;
import java.util.List;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class GPSCommand implements IMinecraft {
   private static final float RADIUS = 80.0F;
   private static final float ARROW_SIZE = 18.0F;
   private static final float MARKER_SCREEN_SIZE = 48.0F;
   private static final Identifier ARROW_TEXTURE = FeverVisual.id("textures/particles/triangle.png");
   private static final Identifier MARKER_TEXTURE = FeverVisual.id("textures/gps.png");
   private static final ParameterValidator<String> ARGUMENT = new ParameterValidator<>() {
      @Override
      public ValidationResult validate(String text) {
         return ValidationResult.ok(text);
      }

      @Override
      public List<String> suggestions(String partial) {
         String lower = partial.toLowerCase();
         return List.of("add", "off").stream().filter(suggestion -> suggestion.startsWith(lower)).toList();
      }
   };

   private boolean active;
   private int targetX;
   private int targetY;
   private int targetZ;

   private final EventListener<HudRenderEvent> onHudRender = event -> {
      if (!this.active || mc.player == null || mc.world == null) {
         return;
      }

      float centerX = event.getContext().getScaledWindowWidth() / 2.0F;
      float centerY = event.getContext().getScaledWindowHeight() / 2.0F;
      double dx = this.targetX + 0.5 - mc.player.getX();
      double dz = this.targetZ + 0.5 - mc.player.getZ();
      double distance = Math.sqrt(dx * dx + dz * dz);
      Vec2f markerScreenPos = Utils.worldToScreen(new Vec3d(this.targetX + 0.5, this.targetY + 0.5, this.targetZ + 0.5));
      float targetYaw = (float)(MathHelper.atan2(dz, dx) * 180.0F / Math.PI) - 90.0F;
      float relativeYaw = MathHelper.wrapDegrees(targetYaw - mc.gameRenderer.getCamera().getYaw());
      double radians = Math.toRadians(relativeYaw);
      float arrowX = centerX + (float)Math.sin(radians) * RADIUS;
      float arrowY = centerY - (float)Math.cos(radians) * RADIUS;
      String distanceText = Integer.toString((int)Math.round(distance));
      Font font = Fonts.MEDIUM.getFont(7.0F);

      MatrixStack matrices = event.getContext().getMatrices();
      matrices.push();
      if (markerScreenPos != null) {
         matrices.push();
         matrices.translate(markerScreenPos.x, markerScreenPos.y, 0.0F);
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F));
         event.getContext().drawTexture(
                 MARKER_TEXTURE,
                 -MARKER_SCREEN_SIZE / 2.0F,
                 -MARKER_SCREEN_SIZE / 2.0F,
                 MARKER_SCREEN_SIZE,
                 MARKER_SCREEN_SIZE,
                 ColorRGBA.WHITE
         );
         matrices.pop();
      }
      event.getContext().drawText(font, distanceText, arrowX - font.width(distanceText) / 2.0F, arrowY - 17.0F, ColorRGBA.WHITE);
      matrices.translate(arrowX, arrowY, 0.0F);
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(relativeYaw));
      event.getContext().drawTexture(
              ARROW_TEXTURE,
              -ARROW_SIZE / 2.0F,
              -ARROW_SIZE / 2.0F,
              ARROW_SIZE,
              ARROW_SIZE,
              ColorRGBA.WHITE.withAlpha(180.0F)
      );
      matrices.pop();
   };

   public GPSCommand() {
      FeverVisual.getInstance().getEventManager().subscribe(this);
   }

   public Command command() {
      return CommandBuilder.begin("gps")
              .desc(".gps <x> <z> / .gps <x> <y> <z> / .gps off")
              .<String>param("args", p -> p.vararg().validator(ARGUMENT))
              .handler(this::handle)
              .build();
   }

   @SuppressWarnings("unchecked")
   private void handle(CommandContext context) {
      List<String> args = (List<String>)context.arguments().get(0);
      if (args.isEmpty()) {
         this.sendUsage();
         return;
      }

      String first = args.getFirst();
      if (first.equalsIgnoreCase("off")) {
         this.removeGPS();
         return;
      }

      if (first.equalsIgnoreCase("add")) {
         args = args.subList(1, args.size());
      }

      if (args.size() == 2) {
         Integer x = this.parseCoordinate(args.get(0));
         Integer z = this.parseCoordinate(args.get(1));
         if (x == null || z == null) {
            return;
         }

         this.addGPS(x, mc.player == null ? 64 : mc.player.getBlockY(), z);
         return;
      }

      if (args.size() == 3) {
         Integer x = this.parseCoordinate(args.get(0));
         Integer y = this.parseCoordinate(args.get(1));
         Integer z = this.parseCoordinate(args.get(2));
         if (x == null || y == null || z == null) {
            return;
         }

         this.addGPS(x, y, z);
         return;
      }

      this.sendUsage();
   }

   private void addGPS(int x, int y, int z) {
      this.targetX = x;
      this.targetY = y;
      this.targetZ = z;
      this.active = true;
      MessageUtility.info(Text.of("GPS enabled: X " + this.targetX + ", Y " + this.targetY + ", Z " + this.targetZ));
   }

   private Integer parseCoordinate(String text) {
      try {
         return Integer.parseInt(text);
      } catch (NumberFormatException ignored) {
         MessageUtility.error(Text.of("'" + text + "' is not a number"));
         return null;
      }
   }

   private void removeGPS() {
      this.active = false;
      MessageUtility.info(Text.of("GPS disabled"));
   }

   private void sendUsage() {
      MessageUtility.info(Text.of("Usage: .gps <x> <z> / .gps <x> <y> <z> / .gps off"));
   }
}

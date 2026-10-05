package fever.visual.systems.modules.modules.visuals;

import fever.visual.framework.base.UIContext;
import fever.visual.framework.base.CustomScreen;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.utility.colors.ColorRGBA;
import java.util.Arrays;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class CrosshairEditorScreen extends CustomScreen {
   private static final int GRID = Crosshair.GRID_SIZE;
   private static final float CELL = 10.0F;
   private static final float PITCH = 11.0F;
   private static final float WIN_W = 320.0F;
   private static final float WIN_H = 214.0F;

   private final Crosshair module;
   private final Screen parent;
   private final boolean[] grid;
   private boolean mirrorX = true;
   private boolean mirrorY;
   private MouseButton paintButton;
   private float mirrorXAnim = 1.0F;
   private float mirrorYAnim;
   private long lastNanos = System.nanoTime();

   public CrosshairEditorScreen(Crosshair module, Screen parent) {
      this.module = module;
      this.parent = parent;
      this.grid = module.customGrid();
   }

   @Override
   public void render(UIContext context) {
      int mouseX = context.getMouseX();
      int mouseY = context.getMouseY();
      if (this.paintButton != null) {
         this.applyPaintAt(mouseX, mouseY);
      }

      float x = this.winX();
      float y = this.winY();
      float rx = x + 196.0F;
      this.updateToggleAnimations();
      Font title = Fonts.SEMIBOLD.getFont(8.0F);
      Font text = Fonts.REGULAR.getFont(6.0F);
      Font small = Fonts.REGULAR.getFont(5.0F);
      context.updateBuffer();
      context.updateBlur();

      context.drawRect(0.0F, 0.0F, this.width, this.height, new ColorRGBA(0.0F, 0.0F, 0.0F, 50.0F));
      context.drawShadow(x, y, WIN_W, WIN_H, 22.0F, BorderRadius.all(12.0F), ColorRGBA.BLACK.withAlpha(105.0F));
      context.drawBlurredRect(x, y, WIN_W, WIN_H, 45.0F, BorderRadius.all(14.0F), ColorRGBA.WHITE.withAlpha(210.0F));
      context.drawLiquidGlass(x, y, WIN_W, WIN_H, 1.0F, BorderRadius.all(14.0F), new ColorRGBA(95.0F, 142.0F, 185.0F, 80.0F), true);
      context.drawRoundedRect(x, y, WIN_W, WIN_H, BorderRadius.all(14.0F), new ColorRGBA(22.0F, 34.0F, 44.0F, 178.0F));
      context.drawRoundedBorder(x, y, WIN_W, WIN_H, 1.2F, BorderRadius.all(14.0F), new ColorRGBA(110.0F, 165.0F, 215.0F, 120.0F));
      context.drawText(title, "Редактор прицела", x + 12.0F, y + 10.0F, ColorRGBA.WHITE.withAlpha(240.0F));
      context.drawText(small, "15x15", x + 104.0F, y + 12.5F, ColorRGBA.WHITE.withAlpha(90.0F));
      context.drawRect(x + 12.0F, y + 25.4F, 296.0F, 0.6F, ColorRGBA.WHITE.withAlpha(16.0F));

      this.drawGrid(context, mouseX, mouseY, x + 16.0F, y + 34.0F);
      this.drawPreview(context, rx, y + 39.0F, text);
      context.drawText(small, "Симметрия", rx, y + 97.0F, ColorRGBA.WHITE.withAlpha(130.0F));
      this.drawToggle(context, rx, y + 106.0F, "Зеркало X", this.mirrorXAnim, text);
      this.drawToggle(context, rx, y + 123.0F, "Зеркало Y", this.mirrorYAnim, text);
      this.drawButton(context, rx, y + 145.0F, 112.0F, 16.0F, "Крест", text);
      this.drawButton(context, rx, y + 167.0F, 112.0F, 16.0F, "Очистить", text);
      context.drawCenteredText(small, "ЛКМ - рисовать · ПКМ - стирать", rx + 56.0F, y + 192.5F, ColorRGBA.WHITE.withAlpha(110.0F));
      this.drawBackLink(context, x + WIN_W - 48.0F, y - 13.0F, small);
   }

   private void drawGrid(UIContext context, int mouseX, int mouseY, float gx, float gy) {
      context.drawRoundedRect(gx - 4.0F, gy - 4.0F, 172.0F, 172.0F, BorderRadius.all(8.0F), new ColorRGBA(255.0F, 255.0F, 255.0F, 10.0F));
      int hover = this.cellAt(mouseX, mouseY);
      for (int row = 0; row < GRID; row++) {
         for (int col = 0; col < GRID; col++) {
            int index = row * GRID + col;
            float x = gx + col * PITCH;
            float y = gy + row * PITCH;
            boolean axis = row == GRID / 2 || col == GRID / 2;
            ColorRGBA color = this.grid[index]
               ? new ColorRGBA(238.0F, 242.0F, 255.0F, 245.0F)
               : (axis ? new ColorRGBA(82.0F, 150.0F, 255.0F, 54.0F) : ColorRGBA.WHITE.withAlpha(22.0F));
            context.drawRoundedRect(x, y, CELL, CELL, BorderRadius.all(2.0F), color);
            if (index == hover) {
               context.drawRoundedBorder(x - 1.0F, y - 1.0F, CELL + 2.0F, CELL + 2.0F, 1.0F, BorderRadius.all(2.5F), new ColorRGBA(120.0F, 188.0F, 255.0F, 230.0F));
            }
         }
      }
   }

   private void drawPreview(UIContext context, float x, float y, Font font) {
      context.drawText(Fonts.REGULAR.getFont(5.0F), "Превью", x, y - 9.0F, ColorRGBA.WHITE.withAlpha(130.0F));
      context.drawRoundedRect(x, y, 112.0F, 50.0F, BorderRadius.all(8.0F), new ColorRGBA(255.0F, 255.0F, 255.0F, 10.0F));
      context.drawRoundedBorder(x, y, 112.0F, 50.0F, 0.6F, BorderRadius.all(8.0F), ColorRGBA.WHITE.withAlpha(22.0F));
      this.module.renderGridStyled(context, this.grid, x + 31.0F, y + 25.0F, 1.0F, 1.0F);
      this.module.renderGridStyled(context, this.grid, x + 73.5F, y + 25.0F, 2.0F, 1.0F);
   }

   private void drawToggle(UIContext context, float x, float y, String label, float enabled, Font font) {
      enabled = Math.max(0.0F, Math.min(1.0F, enabled));
      context.drawRoundedRect(x, y, 112.0F, 13.0F, BorderRadius.all(4.0F), ColorRGBA.WHITE.withAlpha(8.0F));
      context.drawText(font, label, x + 5.0F, y + 3.0F, ColorRGBA.WHITE.withAlpha(210.0F));
      ColorRGBA off = ColorRGBA.WHITE.withAlpha(42.0F);
      ColorRGBA on = new ColorRGBA(100.0F, 160.0F, 255.0F, 230.0F);
      float tx = x + 88.5F;
      float ty = y + 1.4F;
      float tw = 19.5F;
      float th = 10.2F;
      float knob = 7.8F;
      float knobX = tx + 1.2F + (tw - knob - 2.4F) * enabled;
      context.drawLegacyRoundedRect(tx, ty, tw, th, BorderRadius.all(th * 0.5F), off.mix(on, enabled));
      context.drawLegacyRoundedRect(knobX, ty + (th - knob) * 0.5F, knob, knob, BorderRadius.all(knob * 0.5F), ColorRGBA.WHITE.withAlpha(248.0F));
   }

   private void updateToggleAnimations() {
      long now = System.nanoTime();
      float dt = Math.min(0.1F, (now - this.lastNanos) / 1_000_000_000.0F);
      this.lastNanos = now;
      float k = 1.0F - (float)Math.exp(-dt * 18.0F);
      this.mirrorXAnim += ((this.mirrorX ? 1.0F : 0.0F) - this.mirrorXAnim) * k;
      this.mirrorYAnim += ((this.mirrorY ? 1.0F : 0.0F) - this.mirrorYAnim) * k;
   }

   private void drawButton(UIContext context, float x, float y, float w, float h, String label, Font font) {
      int mouseX = context.getMouseX();
      int mouseY = context.getMouseY();
      boolean hovered = hit(mouseX, mouseY, x, y, w, h);
      context.drawRoundedRect(x, y, w, h, BorderRadius.all(4.0F), ColorRGBA.WHITE.withAlpha(hovered ? 30.0F : 14.0F));
      context.drawRoundedBorder(x, y, w, h, 0.6F, BorderRadius.all(4.0F), ColorRGBA.WHITE.withAlpha(hovered ? 70.0F : 30.0F));
      context.drawCenteredText(font, label, x + w / 2.0F, y + h / 2.0F - font.height() / 2.0F, ColorRGBA.WHITE.withAlpha(hovered ? 255.0F : 220.0F));
   }

   private void drawBackLink(UIContext context, float x, float y, Font font) {
      boolean hovered = hit(context.getMouseX(), context.getMouseY(), x, y, 36.0F, 10.0F);
      context.drawText(font, "< Назад", x, y + 1.7F, ColorRGBA.WHITE.withAlpha(hovered ? 255.0F : 150.0F));
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      float x = this.winX();
      float y = this.winY();
      float rx = x + 196.0F;
      if (hit(mouseX, mouseY, x + WIN_W - 48.0F, y - 13.0F, 36.0F, 10.0F)) {
         this.close();
         return;
      }
      if (this.cellAt(mouseX, mouseY) != -1 && (button == MouseButton.LEFT || button == MouseButton.RIGHT)) {
         this.paintButton = button;
         this.applyPaintAt(mouseX, mouseY);
         return;
      }
      if (hit(mouseX, mouseY, rx, y + 106.0F, 112.0F, 13.0F)) {
         this.mirrorX = !this.mirrorX;
         return;
      }
      if (hit(mouseX, mouseY, rx, y + 123.0F, 112.0F, 13.0F)) {
         this.mirrorY = !this.mirrorY;
         return;
      }
      if (hit(mouseX, mouseY, rx, y + 145.0F, 112.0F, 16.0F)) {
         String preset = Crosshair.defaultGrid();
         for (int i = 0; i < this.grid.length; i++) {
            this.grid[i] = preset.charAt(i) == '1';
         }
         this.push();
         return;
      }
      if (hit(mouseX, mouseY, rx, y + 167.0F, 112.0F, 16.0F)) {
         Arrays.fill(this.grid, false);
         this.push();
      }
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.paintButton = null;
   }

   @Override
   public void onMouseDragged(double mouseX, double mouseY, MouseButton button, double deltaX, double deltaY) {
      if (this.paintButton != null) {
         this.applyPaintAt(mouseX, mouseY);
      }
   }

   @Override
   public void close() {
      MinecraftClient.getInstance().setScreen(this.parent);
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   private void applyPaintAt(double mx, double my) {
      int cell = this.cellAt(mx, my);
      if (cell == -1) {
         return;
      }
      boolean value = this.paintButton != MouseButton.RIGHT;
      int row = cell / GRID;
      int col = cell % GRID;
      this.setCell(row, col, value);
      if (this.mirrorX) {
         this.setCell(row, GRID - 1 - col, value);
      }
      if (this.mirrorY) {
         this.setCell(GRID - 1 - row, col, value);
      }
      if (this.mirrorX && this.mirrorY) {
         this.setCell(GRID - 1 - row, GRID - 1 - col, value);
      }
      this.push();
   }

   private void setCell(int row, int col, boolean value) {
      if (row >= 0 && row < GRID && col >= 0 && col < GRID) {
         this.grid[row * GRID + col] = value;
      }
   }

   private int cellAt(double mx, double my) {
      float gx = this.winX() + 16.0F;
      float gy = this.winY() + 34.0F;
      int col = (int)((mx - gx) / PITCH);
      int row = (int)((my - gy) / PITCH);
      if (col < 0 || col >= GRID || row < 0 || row >= GRID) {
         return -1;
      }
      float localX = (float)(mx - gx) - col * PITCH;
      float localY = (float)(my - gy) - row * PITCH;
      return localX >= 0.0F && localY >= 0.0F && localX <= CELL && localY <= CELL ? row * GRID + col : -1;
   }

   private void push() {
      this.module.setCustomGrid(this.grid.clone());
   }

   private float winX() {
      return this.width / 2.0F - WIN_W / 2.0F;
   }

   private float winY() {
      return this.height / 2.0F - WIN_H / 2.0F;
   }

   private static boolean hit(double mx, double my, float x, float y, float w, float h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }
}

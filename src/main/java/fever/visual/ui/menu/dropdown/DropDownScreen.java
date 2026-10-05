package fever.visual.ui.menu.dropdown;

import fever.visual.FeverVisual;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.config.ConfigFile;
import fever.visual.systems.config.ConfigManager;
import fever.visual.systems.friends.FriendManager;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.modules.Module;
import fever.visual.systems.modules.modules.other.Sounds;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.modules.modules.visuals.MenuModule;
import fever.visual.systems.theme.Theme;
import fever.visual.ui.components.ColorPicker;
import fever.visual.ui.components.textfield.FieldAction;
import fever.visual.ui.components.textfield.TextField;
import fever.visual.ui.menu.MenuScreen;
import fever.visual.ui.menu.api.MenuCategory;
import fever.visual.ui.menu.dropdown.components.MenuPanel;
import fever.visual.ui.menu.dropdown.components.module.ModuleComponent;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.cursor.CursorType;
import fever.visual.utility.game.cursor.CursorUtility;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.render.ScissorUtility;
import fever.visual.utility.sounds.ClientSounds;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Generated;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class DropDownScreen extends MenuScreen implements IMinecraft {
   private static final float GUI_WIDTH = 500.0F;
   private static final float GUI_HEIGHT = 330.0F;
   private static final float SIDEBAR_WIDTH = 116.0F;
   private static final float HEADER_HEIGHT = 42.0F;
   private static final float CATEGORY_Y = 49.0F;
   private static final float CATEGORY_HEIGHT = 23.0F;
   private static final float CATEGORY_GAP = 4.0F;
   private static final float SUB_HEIGHT = 18.0F;
   private static final float SUB_GAP = 4.0F;
   private static final float PROFILE_HEIGHT = 43.0F;

   private final Animation searchAnimation = new Animation(220L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation appendingAnim = new Animation(220L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation visualsAnimation = new Animation(260L, Easing.BAKEK_PAGES);
   private final List<ColorPicker> colorPickers = new ArrayList<>();
   private final Map<MenuCategory, MenuPanel> panelByCategory = new EnumMap<>(MenuCategory.class);
   private List<MenuPanel> panels = new ArrayList<>();
   private DashboardCategory selectedCategory = DashboardCategory.PVP;
   private VisualSubCategory visualSubCategory = VisualSubCategory.WORLD;
   private TextField searchField;
   private TextField friendField;
   private TextField configField;
   private String desc = "";
   private float panelWidth = GUI_WIDTH;
   private float panelHeight = GUI_HEIGHT;
   private float guiX;
   private float guiY;
   private float guiWidth;
   private float guiHeight;

   @Override
   protected void init() {
      this.closing = false;
      this.panels = Arrays.stream(MenuCategory.values()).map(MenuPanel::new).toList();
      this.panelByCategory.clear();
      for (MenuPanel panel : this.panels) {
         panel.set(0.0F, 0.0F, GUI_WIDTH - SIDEBAR_WIDTH, GUI_HEIGHT - HEADER_HEIGHT);
         panel.onInit();
         this.panelByCategory.put(panel.getCategory(), panel);
      }

      this.searchField = this.createField("Search modules");
      Map<String, FieldAction> searchActions = new HashMap<>();
      for (Module module : FeverVisual.getInstance().getModuleManager().getModules()) {
         FieldAction action = new FieldAction(module::toggle, () -> this.openModule(module));
         searchActions.put(module.getName(), action);
         searchActions.put(module.getName().replace(" ", ""), action);
      }
      this.searchField.setAppend(searchActions);
      this.friendField = this.createField("Enter nickname");
      this.configField = this.createField("New profile name");
      super.init();
   }

   private TextField createField(String preview) {
      TextField field = new TextField(Fonts.REGULAR.getFont(7.0F));
      field.setPreview(preview);
      field.setTextColor(Colors.getTextColor());
      return field;
   }

   @Override
   public void tick() {
      this.handleMovementKeys();
      super.tick();
   }

   @Override
   public void render(UIContext context) {
      this.menuAnimation.setDuration(this.closing ? 220L : 420L);
      this.menuAnimation.setEasing(Easing.FIGMA_EASE_IN_OUT);
      this.menuAnimation.update(this.closing ? 0.0F : 1.0F);
      float alpha = this.menuAnimation.getValue();
      if (alpha <= 0.01F) {
         return;
      }

      this.guiWidth = Math.min(GUI_WIDTH, this.width - 24.0F);
      this.guiHeight = Math.min(GUI_HEIGHT, this.height - 24.0F);
      this.guiX = (this.width - this.guiWidth) / 2.0F;
      this.guiY = (this.height - this.guiHeight) / 2.0F;
      this.panelWidth = this.guiWidth;
      this.panelHeight = this.guiHeight;
      this.desc = "";

      this.renderShell(context, alpha);
      this.renderHeader(context, alpha);
      this.renderSidebar(context, alpha);
      this.renderContent(context, alpha);
      this.renderProfile(context, alpha);
      this.renderDescription(context, alpha);

      for (ColorPicker picker : this.colorPickers) {
         picker.render(context);
         if (!(mc.currentScreen instanceof DropDownScreen)) {
            picker.setShowing(false);
         }
      }
      this.colorPickers.removeIf(picker -> picker.getAnimation().getValue() == 0.0F && !picker.isShowing());
   }

   private void renderShell(UIContext context, float alpha) {
      boolean dark = FeverVisual.getInstance().getThemeManager().getCurrentTheme() == Theme.DARK;
      MenuModule menu = FeverVisual.getInstance().getModuleManager().getModule(MenuModule.class);
      context.drawShadow(this.guiX - 5.0F, this.guiY - 5.0F, this.guiWidth + 10.0F, this.guiHeight + 10.0F, 28.0F, BorderRadius.all(15.0F), ColorRGBA.BLACK.withAlpha(105.0F * alpha));
      context.drawBlurredRect(this.guiX, this.guiY, this.guiWidth, this.guiHeight, 48.0F, BorderRadius.all(14.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha));
      if (Interface.showGlass()) {
         ColorRGBA top = menu == null ? ColorRGBA.WHITE.withAlpha(170.0F * alpha) : menu.getGlassColorTop(alpha);
         ColorRGBA bottom = menu == null ? top : menu.getGlassColorBottom(alpha);
         context.drawLiquidGlass(this.guiX, this.guiY, this.guiWidth, this.guiHeight, 3.0F, 0.08F, BorderRadius.all(14.0F), top, bottom);
      }
      context.drawRoundedRect(this.guiX, this.guiY, SIDEBAR_WIDTH, this.guiHeight, BorderRadius.left(14.0F, 14.0F), Colors.getBackgroundColor().withAlpha((dark ? 226.0F : 190.0F) * alpha));
      context.drawRoundedRect(this.guiX + SIDEBAR_WIDTH, this.guiY, this.guiWidth - SIDEBAR_WIDTH, this.guiHeight, BorderRadius.right(14.0F, 14.0F), Colors.getAdditionalColor().withAlpha((dark ? 210.0F : 174.0F) * alpha));
      context.drawRoundedBorder(this.guiX, this.guiY, this.guiWidth, this.guiHeight, 0.65F, BorderRadius.all(14.0F), Colors.getTextColor().withAlpha(38.0F * alpha));
      context.drawRect(this.guiX + SIDEBAR_WIDTH, this.guiY, 0.6F, this.guiHeight, Colors.getTextColor().withAlpha(24.0F * alpha));
   }

   private void renderHeader(UIContext context, float alpha) {
      float contentX = this.guiX + SIDEBAR_WIDTH;
      context.drawText(Fonts.SEMIBOLD.getFont(11.0F), this.selectedCategory.title, contentX + 18.0F, this.guiY + 15.0F, Colors.getTextColor().withAlpha(240.0F * alpha));
      context.drawRect(contentX + 18.0F, this.guiY + HEADER_HEIGHT - 1.0F, this.guiWidth - SIDEBAR_WIDTH - 36.0F, 0.5F, Colors.getTextColor().withAlpha(18.0F * alpha));

      if (!this.isModuleCategory()) {
         this.searchField.setFocused(false);
         return;
      }
      float width = 116.0F;
      float x = this.guiX + this.guiWidth - width - 15.0F;
      float y = this.guiY + 9.0F;
      this.searchAnimation.update(this.searchField.isFocused() ? 1.0F : 0.0F);
      context.drawRoundedRect(x, y, width, 23.0F, BorderRadius.all(7.0F), Colors.getBackgroundColor().withAlpha((145.0F + 35.0F * this.searchAnimation.getValue()) * alpha));
      context.drawRoundedBorder(x, y, width, 23.0F, 0.5F, BorderRadius.all(7.0F), Colors.getTextColor().withAlpha((20.0F + 28.0F * this.searchAnimation.getValue()) * alpha));
      context.drawTexture(FeverVisual.id("icons/search.png"), x + 7.0F, y + 7.0F, 9.0F, 9.0F, Colors.getTextColor().withAlpha(145.0F * alpha));
      this.searchField.set(x + 17.0F, y + 1.0F, width - 22.0F, 21.0F);
      this.searchField.setAlpha(alpha);
      this.searchField.setTextColor(Colors.getTextColor());
      this.searchField.render(context);
      this.appendingAnim.update(!this.searchField.getAppending().isBlank() ? 1.0F : 0.0F);
   }

   private void renderSidebar(UIContext context, float alpha) {
      context.drawText(Fonts.BOLD.getFont(14.0F), "F", this.guiX + 15.0F, this.guiY + 12.0F, Colors.getAccentColor().withAlpha(255.0F * alpha));
      context.drawText(Fonts.SEMIBOLD.getFont(8.5F), "Fever Visual", this.guiX + 35.0F, this.guiY + 11.0F, Colors.getTextColor().withAlpha(240.0F * alpha));
      context.drawText(Fonts.REGULAR.getFont(5.2F), "Minecraft 1.21.11", this.guiX + 35.0F, this.guiY + 23.0F, Colors.getTextColor().withAlpha(105.0F * alpha));

      this.visualsAnimation.update(this.selectedCategory == DashboardCategory.VISUALS ? 1.0F : 0.0F);
      float open = this.visualsAnimation.getValue();
      float categoryHeight = this.categoryHeight();
      float categoryGap = this.categoryGap();
      float subHeight = this.subHeight();
      float subGap = this.subGap();
      float y = this.guiY + this.categoryY();
      for (DashboardCategory category : DashboardCategory.values()) {
         this.renderCategoryRow(context, category, y, alpha);
         y += categoryHeight + categoryGap;
         if (category == DashboardCategory.VISUALS) {
            if (open > 0.02F) {
               float subY = y - (1.0F - open) * 8.0F;
               ScissorUtility.push(context.getMatrices(), this.guiX + 10.0F, y - 2.0F, SIDEBAR_WIDTH - 20.0F, (subHeight + subGap) * 2.0F * open + 3.0F);
               for (VisualSubCategory sub : VisualSubCategory.values()) {
                  this.renderSubCategory(context, sub, subY, alpha * open);
                  subY += subHeight + subGap;
               }
               ScissorUtility.pop();
            }
            y += ((subHeight + subGap) * 2.0F + (this.compactSidebar() ? 3.0F : 4.0F)) * open;
         }
         if (category == DashboardCategory.MISC) {
            y += this.compactSidebar() ? 3.0F : 7.0F;
         }
      }
   }

   private void renderCategoryRow(UIContext context, DashboardCategory category, float y, float alpha) {
      float x = this.guiX + 10.0F;
      float width = SIDEBAR_WIDTH - 20.0F;
      boolean selected = category == this.selectedCategory;
      float height = this.categoryHeight();
      boolean hovered = GuiUtility.isHovered(x, y, width, height, context.getMouseX(), context.getMouseY());
      if (selected || hovered) {
         context.drawRoundedRect(x, y, width, height, BorderRadius.all(6.0F), (selected ? Colors.getAdditionalColor() : Colors.getTextColor()).withAlpha((selected ? 105.0F : 22.0F) * alpha));
         if (selected) {
            context.drawRoundedRect(x + 1.0F, y + 4.0F, 2.0F, height - 8.0F, BorderRadius.all(1.0F), Colors.getAccentColor().withAlpha(240.0F * alpha));
         }
      }
      float textY = y + GuiUtility.getMiddleOfBox(Fonts.REGULAR.getFont(7.2F).height(), height);
      context.drawCenteredText(Fonts.SEMIBOLD.getFont(6.8F), category.icon, x + 13.0F, textY, (selected ? Colors.getAccentColor() : Colors.getTextColor()).withAlpha((selected ? 245.0F : 145.0F) * alpha));
      context.drawText(Fonts.REGULAR.getFont(7.2F), category.title, x + 27.0F, textY, Colors.getTextColor().withAlpha((selected ? 245.0F : 155.0F) * alpha));
      if (hovered) CursorUtility.set(CursorType.HAND);
   }

   private void renderSubCategory(UIContext context, VisualSubCategory sub, float y, float alpha) {
      float x = this.guiX + 31.0F;
      float width = SIDEBAR_WIDTH - 41.0F;
      boolean selected = sub == this.visualSubCategory;
      float height = this.subHeight();
      boolean hovered = GuiUtility.isHovered(x, y, width, height, context.getMouseX(), context.getMouseY());
      if (selected || hovered) {
         context.drawRoundedRect(x, y, width, height, BorderRadius.all(5.0F), Colors.getTextColor().withAlpha((selected ? 24.0F : 12.0F) * alpha));
      }
      context.drawText(Fonts.REGULAR.getFont(6.5F), sub.title, x + 10.0F, y + GuiUtility.getMiddleOfBox(Fonts.REGULAR.getFont(6.5F).height(), height), Colors.getTextColor().withAlpha((selected ? 225.0F : 125.0F) * alpha));
      if (hovered) CursorUtility.set(CursorType.HAND);
   }

   private void renderContent(UIContext context, float alpha) {
      float x = this.guiX + SIDEBAR_WIDTH;
      float y = this.guiY + HEADER_HEIGHT;
      float width = this.guiWidth - SIDEBAR_WIDTH;
      float height = this.guiHeight - HEADER_HEIGHT;
      if (this.selectedCategory == DashboardCategory.FRIENDS) {
         this.renderFriends(context, x, y, width, height, alpha);
      } else if (this.selectedCategory == DashboardCategory.CONFIG) {
         this.renderConfigs(context, x, y, width, height, alpha);
      } else {
         this.renderModules(context, x, y, width, height, alpha);
      }
   }

   private void renderModules(UIContext context, float x, float y, float width, float height, float alpha) {
      MenuPanel panel = this.getSelectedPanel();
      if (panel == null) return;
      panel.set(x, y, width, height);
      panel.getModulesScroll().update();
      List<ModuleComponent> modules = this.getVisibleModules(panel);
      float panelX = x + 18.0F;
      float panelY = y + 8.0F;
      float panelWidth = width - 36.0F;
      float panelHeight = height - 20.0F;
      if (this.selectedCategory == DashboardCategory.VISUALS && this.visualSubCategory == VisualSubCategory.PLAYER) {
         this.renderPlayerModules(context, panel, modules, panelX, panelY, panelWidth, panelHeight, alpha);
      } else {
         this.renderGroupedModules(context, panel, modules, panelX, panelY, panelWidth, panelHeight, alpha);
      }
   }

   private void renderGroupedModules(UIContext context, MenuPanel panel, List<ModuleComponent> modules, float x, float y, float width, float height, float alpha) {
      List<ModuleGroup> groups = this.buildGroups(modules);
      float sidePadding = 7.0F;
      float columnGap = 28.0F;
      float columnWidth = (width - sidePadding * 2.0F - columnGap) / 2.0F;
      float[] columnHeights = {0.0F, 0.0F};
      float scroll = (float)panel.getModulesScroll().getValue();
      ScissorUtility.push(context.getMatrices(), x - 2.0F, y, width + 4.0F, height);
      for (int i = 0; i < groups.size(); i++) {
         int column = i % 2;
         float groupX = x + sidePadding + column * (columnWidth + columnGap);
         float groupY = y + columnHeights[column] - scroll;
         float rendered = this.renderModuleGroup(context, groups.get(i), groupX, groupY, columnWidth, alpha, y, y + height);
         columnHeights[column] += rendered + 18.0F;
      }
      ScissorUtility.pop();
      panel.getModulesScroll().setMax(Math.min(0.0F, -Math.max(columnHeights[0], columnHeights[1]) + height));
   }

   private void renderPlayerModules(UIContext context, MenuPanel panel, List<ModuleComponent> modules, float x, float y, float width, float height, float alpha) {
      float previewWidth = Math.min(150.0F, width * 0.42F);
      float listWidth = Math.max(145.0F, width - previewWidth - 28.0F);
      ModuleGroup group = new ModuleGroup("Player");
      group.modules.addAll(modules);
      float scroll = (float)panel.getModulesScroll().getValue();
      ScissorUtility.push(context.getMatrices(), x - 2.0F, y, listWidth + 4.0F, height);
      float rendered = this.renderModuleGroup(context, group, x, y - scroll, listWidth, alpha, y, y + height);
      ScissorUtility.pop();
      panel.getModulesScroll().setMax(Math.min(0.0F, -rendered + height));
      this.renderPlayerPreview(context, x + listWidth + 14.0F, y, previewWidth, height, alpha);
   }

   private float renderModuleGroup(UIContext context, ModuleGroup group, float x, float y, float width, float alpha, float clipTop, float clipBottom) {
      if (group.modules.isEmpty()) return 0.0F;
      float headerHeight = 14.0F;
      float rowHeight = 22.0F;
      float rowY = y + headerHeight;
      for (ModuleComponent component : group.modules) {
         component.prepare(context, x, rowY, width, rowHeight);
         rowY += component.getAnimatedHeight();
      }
      float cardHeight = rowY - (y + headerHeight);
      if (y + headerHeight > clipTop && y < clipBottom) {
         context.drawText(Fonts.REGULAR.getFont(6.0F), group.title, x, y + 2.0F, Colors.getTextColor().withAlpha(115.0F * alpha));
      }
      float visibleY = Math.max(y + headerHeight, clipTop);
      float visibleBottom = Math.min(y + headerHeight + cardHeight, clipBottom);
      if (visibleBottom > visibleY) {
         context.drawBlurredRect(x, visibleY, width, visibleBottom - visibleY, 28.0F, BorderRadius.all(8.0F), ColorRGBA.WHITE.withAlpha(200.0F * alpha));
         context.drawRoundedRect(x, visibleY, width, visibleBottom - visibleY, BorderRadius.all(8.0F), Colors.getBackgroundColor().withAlpha(118.0F * alpha));
         context.drawRoundedBorder(x, visibleY, width, visibleBottom - visibleY, 0.5F, BorderRadius.all(8.0F), Colors.getTextColor().withAlpha(24.0F * alpha));
      }

      Font moduleFont = Fonts.REGULAR.getFont(7.0F);
      for (ModuleComponent component : group.modules) {
         float componentY = component.getY();
         if (componentY + component.getHeight() < clipTop || componentY > clipBottom) continue;
         float enabled = component.getEnableAnimation().getValue();
         String name = component.isBindingMode() ? this.bindingText(component.getModule()) : component.getModule().getName();
         context.drawFadeoutText(moduleFont, name, x + 8.0F, componentY + GuiUtility.getMiddleOfBox(moduleFont.height(), rowHeight), Colors.getTextColor().withAlpha((150.0F + 95.0F * enabled) * alpha), 0.75F, 1.0F, width - 53.0F);
         if (!component.getSettingComponents().isEmpty()) {
            context.drawTexture(FeverVisual.id("image/mainmenu/icons/settings.png"), x + width - 39.0F, componentY + 7.0F, 7.0F, 7.0F, Colors.getTextColor().withAlpha((145.0F + 55.0F * component.getExpandAnimation().getValue()) * alpha));
         }
         this.renderToggle(context, x + width - 29.0F, componentY + 5.0F, enabled, alpha);
         if (component.getAnimatedHeight() > rowHeight + 0.25F) {
            context.drawRect(x + 7.0F, componentY + rowHeight, 2.0F, component.getAnimatedHeight() - rowHeight, Colors.getAccentColor().withAlpha(105.0F * component.getExpandAnimation().getValue() * alpha));
            component.renderInlineSettings(context);
         }
         context.drawRect(x + 12.0F, componentY + component.getAnimatedHeight() - 0.5F, width - 24.0F, 0.5F, Colors.getTextColor().withAlpha(15.0F * alpha));
      }
      return headerHeight + cardHeight;
   }

   private void renderToggle(UIContext context, float x, float y, float progress, float alpha) {
      ColorRGBA off = Colors.getAdditionalColor();
      ColorRGBA on = Colors.getAccentColor();
      ColorRGBA track = off.mix(on, progress).withAlpha((185.0F + 70.0F * progress) * alpha);
      context.drawRoundedRect(x, y, 22.0F, 12.0F, BorderRadius.all(5.0F), track);
      context.drawRoundedRect(x + 2.0F + 11.0F * progress, y + 2.5F, 7.0F, 7.0F, BorderRadius.all(3.5F), ColorRGBA.WHITE.withAlpha(245.0F * alpha));
   }

   private void renderPlayerPreview(UIContext context, float x, float y, float width, float height, float alpha) {
      context.drawText(Fonts.REGULAR.getFont(6.0F), "Preview", x + 3.0F, y + 2.0F, Colors.getTextColor().withAlpha(115.0F * alpha));
      float boxY = y + 14.0F;
      context.drawBlurredRect(x, boxY, width, height - 14.0F, 30.0F, BorderRadius.all(9.0F), ColorRGBA.WHITE.withAlpha(190.0F * alpha));
      context.drawRoundedRect(x, boxY, width, height - 14.0F, BorderRadius.all(9.0F), Colors.getBackgroundColor().withAlpha(110.0F * alpha));
      context.drawRoundedBorder(x, boxY, width, height - 14.0F, 0.55F, BorderRadius.all(9.0F), Colors.getTextColor().withAlpha(30.0F * alpha));
      if (mc.player != null) {
         context.updateBuffer();
         int size = Math.max(42, Math.round(Math.min(width, height) * 0.46F));
         InventoryScreen.drawEntity(context.getOriginalContext(), Math.round(x + 10.0F), Math.round(y + 27.0F), Math.round(x + width - 10.0F), Math.round(y + height - 10.0F), size, 0.0625F, context.getMouseX(), context.getMouseY(), mc.player);
      }
   }

   private void renderFriends(UIContext context, float x, float y, float width, float height, float alpha) {
      float panelX = x + 25.0F;
      float panelY = y + 17.0F;
      float panelWidth = width - 50.0F;
      this.renderInputSurface(context, this.friendField, panelX, panelY, panelWidth - 31.0F, "Enter nickname", alpha);
      this.renderActionButton(context, panelX + panelWidth - 25.0F, panelY, 25.0F, 23.0F, "+", alpha);
      FriendManager manager = FeverVisual.getInstance().getFriendManager();
      float rowY = panelY + 34.0F;
      List<String> friends = manager == null ? List.of() : manager.listFriends();
      if (friends.isEmpty()) {
         context.drawText(Fonts.REGULAR.getFont(7.0F), "No friends added", panelX + 3.0F, rowY, Colors.getTextColor().withAlpha(105.0F * alpha));
         return;
      }
      for (String friend : friends) {
         if (rowY + 28.0F > y + height - 12.0F) break;
         context.drawRoundedRect(panelX, rowY, panelWidth, 27.0F, BorderRadius.all(7.0F), Colors.getBackgroundColor().withAlpha(115.0F * alpha));
         context.drawRoundedRect(panelX + 8.0F, rowY + 6.0F, 15.0F, 15.0F, BorderRadius.all(5.0F), Colors.getAccentColor().withAlpha(145.0F * alpha));
         context.drawCenteredText(Fonts.SEMIBOLD.getFont(7.0F), friend.substring(0, 1).toUpperCase(Locale.ROOT), panelX + 15.5F, rowY + 9.3F, ColorRGBA.WHITE.withAlpha(235.0F * alpha));
         context.drawText(Fonts.SEMIBOLD.getFont(7.0F), friend, panelX + 31.0F, rowY + 9.0F, Colors.getTextColor().withAlpha(225.0F * alpha));
         context.drawText(Fonts.REGULAR.getFont(8.0F), "x", panelX + panelWidth - 17.0F, rowY + 8.5F, Colors.getTextColor().withAlpha(120.0F * alpha));
         rowY += 32.0F;
      }
   }

   private void renderConfigs(UIContext context, float x, float y, float width, float height, float alpha) {
      ConfigManager manager = FeverVisual.getInstance().getConfigManager();
      manager.refresh();
      float panelX = x + 25.0F;
      float panelY = y + 17.0F;
      float panelWidth = width - 50.0F;
      this.renderInputSurface(context, this.configField, panelX, panelY, panelWidth - 67.0F, "New profile name", alpha);
      this.renderActionButton(context, panelX + panelWidth - 61.0F, panelY, 28.0F, 23.0F, "+", alpha);
      this.renderActionButton(context, panelX + panelWidth - 28.0F, panelY, 28.0F, 23.0F, "S", alpha);
      float rowY = panelY + 34.0F;
      for (ConfigFile config : new ArrayList<>(manager.getConfigFiles())) {
         if (rowY + 28.0F > y + height - 12.0F) break;
         boolean current = manager.getCurrent() == config;
         context.drawRoundedRect(panelX, rowY, panelWidth, 27.0F, BorderRadius.all(7.0F), Colors.getBackgroundColor().withAlpha((current ? 145.0F : 105.0F) * alpha));
         if (current) context.drawRoundedRect(panelX + 2.0F, rowY + 7.0F, 2.0F, 13.0F, BorderRadius.all(1.0F), Colors.getAccentColor().withAlpha(235.0F * alpha));
         context.drawText(Fonts.SEMIBOLD.getFont(7.0F), config.getFileName(), panelX + 11.0F, rowY + 9.0F, Colors.getTextColor().withAlpha(225.0F * alpha));
         context.drawText(Fonts.REGULAR.getFont(5.8F), current ? "Active" : "Click to load", panelX + panelWidth - 55.0F, rowY + 9.7F, Colors.getTextColor().withAlpha(100.0F * alpha));
         rowY += 32.0F;
      }
   }

   private void renderInputSurface(UIContext context, TextField field, float x, float y, float width, String placeholder, float alpha) {
      context.drawRoundedRect(x, y, width, 23.0F, BorderRadius.all(7.0F), Colors.getBackgroundColor().withAlpha(145.0F * alpha));
      context.drawRoundedBorder(x, y, width, 23.0F, 0.5F, BorderRadius.all(7.0F), Colors.getTextColor().withAlpha((field.isFocused() ? 52.0F : 20.0F) * alpha));
      field.setPreview(placeholder);
      field.set(x + 6.0F, y + 1.0F, width - 12.0F, 21.0F);
      field.setAlpha(alpha);
      field.setTextColor(Colors.getTextColor());
      field.render(context);
   }

   private void renderActionButton(UIContext context, float x, float y, float width, float height, String text, float alpha) {
      boolean hovered = GuiUtility.isHovered(x, y, width, height, context.getMouseX(), context.getMouseY());
      context.drawRoundedRect(x, y, width, height, BorderRadius.all(7.0F), (hovered ? Colors.getAccentColor() : Colors.getBackgroundColor()).withAlpha((hovered ? 175.0F : 145.0F) * alpha));
      context.drawCenteredText(Fonts.SEMIBOLD.getFont(8.0F), text, x + width / 2.0F, y + 7.0F, ColorRGBA.WHITE.withAlpha(235.0F * alpha));
      if (hovered) CursorUtility.set(CursorType.HAND);
   }

   private void renderProfile(UIContext context, float alpha) {
      float y = this.guiY + this.guiHeight - this.profileHeight();
      context.drawRect(this.guiX + 10.0F, y, SIDEBAR_WIDTH - 20.0F, 0.5F, Colors.getTextColor().withAlpha(18.0F * alpha));
      float avatarX = this.guiX + 14.0F;
      float avatarY = y + 9.0F;
      context.drawRoundedRect(avatarX, avatarY, 24.0F, 24.0F, BorderRadius.all(7.0F), Colors.getAccentColor().withAlpha(115.0F * alpha));
      if (mc.player != null) {
         context.updateBuffer();
         InventoryScreen.drawEntity(context.getOriginalContext(), Math.round(avatarX), Math.round(avatarY), Math.round(avatarX + 24.0F), Math.round(avatarY + 24.0F), 14, 0.0625F, this.width / 2.0F, this.height / 2.0F, mc.player);
      }
      String name = mc.getSession() == null ? "Player" : mc.getSession().getUsername();
      context.drawFadeoutText(Fonts.SEMIBOLD.getFont(6.8F), name, this.guiX + 44.0F, y + 12.0F, Colors.getTextColor().withAlpha(235.0F * alpha), 0.75F, 1.0F, 57.0F);
      context.drawText(Fonts.REGULAR.getFont(5.3F), "Fever user", this.guiX + 44.0F, y + 23.0F, Colors.getTextColor().withAlpha(95.0F * alpha));
   }

   private void renderDescription(UIContext context, float alpha) {
      if (this.desc == null || this.desc.isBlank() || this.desc.contains(".description")) return;
      Font font = Fonts.REGULAR.getFont(6.5F);
      float width = Math.min(this.guiWidth - 34.0F, font.width(this.desc) + 18.0F);
      float x = this.guiX + (this.guiWidth - width) / 2.0F;
      float y = this.guiY - 21.0F;
      context.drawBlurredRect(x, y, width, 16.0F, 22.0F, BorderRadius.all(6.0F), ColorRGBA.WHITE.withAlpha(210.0F * alpha));
      context.drawRoundedRect(x, y, width, 16.0F, BorderRadius.all(6.0F), Colors.getBackgroundColor().withAlpha(190.0F * alpha));
      context.drawFadeoutText(font, this.desc, x + 9.0F, y + 5.0F, Colors.getTextColor().withAlpha(220.0F * alpha), 0.8F, 1.0F, width - 18.0F);
   }

   private List<ModuleComponent> getVisibleModules(MenuPanel panel) {
      String query = this.searchField == null ? "" : this.searchField.getBuiltText().trim().toLowerCase(Locale.ROOT);
      return panel.getModuleComponents().stream()
         .filter(component -> this.selectedCategory != DashboardCategory.VISUALS || this.isVisualInSelectedSub(component.getModule().getName()))
         .filter(component -> query.isBlank() || component.getModule().getName().toLowerCase(Locale.ROOT).contains(query))
         .sorted(Comparator.comparing(component -> component.getModule().getName()))
         .toList();
   }

   private List<ModuleGroup> buildGroups(List<ModuleComponent> modules) {
      String[] titles = this.groupTitles();
      List<ModuleGroup> groups = new ArrayList<>();
      for (String title : titles) groups.add(new ModuleGroup(title));
      if (this.selectedCategory == DashboardCategory.VISUALS) {
         for (ModuleComponent component : modules) groups.get(Math.min(groups.size() - 1, this.visualGroup(component.getModule().getName()))).modules.add(component);
      } else {
         for (int i = 0; i < modules.size(); i++) groups.get(Math.min(groups.size() - 1, i * groups.size() / Math.max(1, modules.size()))).modules.add(modules.get(i));
      }
      groups.removeIf(group -> group.modules.isEmpty());
      return groups;
   }

   private String[] groupTitles() {
      return switch (this.selectedCategory) {
         case VISUALS -> this.visualSubCategory == VisualSubCategory.WORLD ? new String[]{"Environment", "World", "Effects", "Other"} : new String[]{"Player"};
         case PVP -> new String[]{"Main", "Targets", "Effects", "Other"};
         case DISPLAY -> new String[]{"Interface", "Widgets", "Menu", "Other"};
         default -> new String[]{"Main", "Player", "World", "Other"};
      };
   }

   private int visualGroup(String moduleName) {
      String name = compact(moduleName);
      if (this.visualSubCategory == VisualSubCategory.PLAYER) return 0;
      if (containsAny(name, "aspect", "camera", "freelook", "motion", "viewmodel", "hand", "glass")) return 2;
      if (containsAny(name, "esp", "particle", "prediction", "bubble", "trail", "jump", "sphere", "timer", "totem", "waypoint", "tab")) return 1;
      if (containsAny(name, "helper", "wings", "cape", "gender", "pet", "model", "nimb", "taksa")) return 3;
      return 0;
   }

   private boolean isVisualInSelectedSub(String name) {
      boolean player = containsAny(compact(name), "chinahat", "nimb", "wings", "cape", "femalegender", "custommodels", "modelchanger", "spheres", "badtrip", "armor", "armour");
      return this.visualSubCategory == VisualSubCategory.PLAYER ? player : !player;
   }

   private static String compact(String name) { return name.toLowerCase(Locale.ROOT).replace(" ", ""); }
   private static boolean containsAny(String value, String... needles) { for (String needle : needles) if (value.contains(needle)) return true; return false; }

   private boolean isModuleCategory() {
      return this.selectedCategory.menuCategory != null;
   }

   private MenuPanel getSelectedPanel() {
      return this.selectedCategory.menuCategory == null ? null : this.panelByCategory.get(this.selectedCategory.menuCategory);
   }

   private void openModule(Module module) {
      for (MenuPanel panel : this.panels) {
         for (ModuleComponent component : panel.getModuleComponents()) {
            if (component.getModule() == module) {
               this.selectedCategory = DashboardCategory.from(panel.getCategory());
               component.setExpanded(true);
               return;
            }
         }
      }
   }

   private String bindingText(Module module) {
      return module.getKey() == -1 ? Localizator.translate("menu.binding") : Localizator.translate("key") + ": " + fever.visual.utility.game.TextUtility.getKeyName(module.getKey());
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      ColorPicker picker = this.getTopColorPicker(mouseX, mouseY);
      if (picker != null) {
         picker.onMouseClicked(mouseX, mouseY, button);
         this.bringColorPickerToFront(picker);
         return;
      }
      if (button == MouseButton.LEFT && this.handleSidebarClick(mouseX, mouseY)) return;

      if (this.isModuleCategory() && this.searchField.isHovered(mouseX, mouseY)) {
         this.searchField.onMouseClicked(mouseX, mouseY, button);
         return;
      }
      this.searchField.setFocused(false);

      if (this.selectedCategory == DashboardCategory.FRIENDS && this.handleFriendsClick(mouseX, mouseY, button)) return;
      if (this.selectedCategory == DashboardCategory.CONFIG && this.handleConfigsClick(mouseX, mouseY, button)) return;

      MenuPanel panel = this.getSelectedPanel();
      if (panel != null && GuiUtility.isHovered(this.guiX + SIDEBAR_WIDTH, this.guiY + HEADER_HEIGHT, this.guiWidth - SIDEBAR_WIDTH, this.guiHeight - HEADER_HEIGHT, mouseX, mouseY)) {
         List<ModuleComponent> visible = this.getVisibleModules(panel);
         for (int i = visible.size() - 1; i >= 0; i--) {
            ModuleComponent component = visible.get(i);
            if (component.isHovered(mouseX, mouseY)) {
               component.onMouseClicked(mouseX, mouseY, button);
               return;
            }
         }
      }
      super.onMouseClicked(mouseX, mouseY, button);
   }

   private boolean handleSidebarClick(double mouseX, double mouseY) {
      float categoryHeight = this.categoryHeight();
      float categoryGap = this.categoryGap();
      float subHeight = this.subHeight();
      float subGap = this.subGap();
      float y = this.guiY + this.categoryY();
      float open = this.visualsAnimation.getValue();
      for (DashboardCategory category : DashboardCategory.values()) {
         if (GuiUtility.isHovered(this.guiX + 10.0F, y, SIDEBAR_WIDTH - 20.0F, categoryHeight, mouseX, mouseY)) {
            this.selectedCategory = category;
            this.friendField.setFocused(false);
            this.configField.setFocused(false);
            return true;
         }
         y += categoryHeight + categoryGap;
         if (category == DashboardCategory.VISUALS) {
            float subY = y - (1.0F - open) * 8.0F;
            if (this.selectedCategory == DashboardCategory.VISUALS && open > 0.35F) {
               for (VisualSubCategory sub : VisualSubCategory.values()) {
                  if (GuiUtility.isHovered(this.guiX + 31.0F, subY, SIDEBAR_WIDTH - 41.0F, subHeight, mouseX, mouseY)) {
                     this.visualSubCategory = sub;
                     MenuPanel panel = this.panelByCategory.get(MenuCategory.VISUALS);
                     if (panel != null) panel.getModulesScroll().reset();
                     return true;
                  }
                  subY += subHeight + subGap;
               }
            }
            y += ((subHeight + subGap) * 2.0F + (this.compactSidebar() ? 3.0F : 4.0F)) * open;
         }
         if (category == DashboardCategory.MISC) y += this.compactSidebar() ? 3.0F : 7.0F;
      }
      return false;
   }

   private boolean handleFriendsClick(double mouseX, double mouseY, MouseButton button) {
      float x = this.guiX + SIDEBAR_WIDTH + 25.0F;
      float y = this.guiY + HEADER_HEIGHT + 17.0F;
      float width = this.guiWidth - SIDEBAR_WIDTH - 50.0F;
      if (this.friendField.isHovered(mouseX, mouseY)) {
         this.friendField.onMouseClicked(mouseX, mouseY, button);
         return true;
      }
      this.friendField.setFocused(false);
      if (button == MouseButton.LEFT && GuiUtility.isHovered(x + width - 25.0F, y, 25.0F, 23.0F, mouseX, mouseY)) {
         this.addFriend();
         return true;
      }
      float rowY = y + 34.0F;
      for (String friend : new ArrayList<>(FeverVisual.getInstance().getFriendManager().listFriends())) {
         if (button == MouseButton.LEFT && GuiUtility.isHovered(x + width - 25.0F, rowY, 25.0F, 27.0F, mouseX, mouseY)) {
            FeverVisual.getInstance().getFriendManager().remove(friend);
            return true;
         }
         rowY += 32.0F;
      }
      return false;
   }

   private boolean handleConfigsClick(double mouseX, double mouseY, MouseButton button) {
      float x = this.guiX + SIDEBAR_WIDTH + 25.0F;
      float y = this.guiY + HEADER_HEIGHT + 17.0F;
      float width = this.guiWidth - SIDEBAR_WIDTH - 50.0F;
      if (this.configField.isHovered(mouseX, mouseY)) {
         this.configField.onMouseClicked(mouseX, mouseY, button);
         return true;
      }
      this.configField.setFocused(false);
      if (button == MouseButton.LEFT && GuiUtility.isHovered(x + width - 61.0F, y, 28.0F, 23.0F, mouseX, mouseY)) {
         this.createConfig();
         return true;
      }
      if (button == MouseButton.LEFT && GuiUtility.isHovered(x + width - 28.0F, y, 28.0F, 23.0F, mouseX, mouseY)) {
         FeverVisual.getInstance().getConfigManager().saveCurrent();
         return true;
      }
      float rowY = y + 34.0F;
      for (ConfigFile config : new ArrayList<>(FeverVisual.getInstance().getConfigManager().getConfigFiles())) {
         if (button == MouseButton.LEFT && GuiUtility.isHovered(x, rowY, width, 27.0F, mouseX, mouseY)) {
            config.load();
            return true;
         }
         rowY += 32.0F;
      }
      return false;
   }

   private void addFriend() {
      String name = this.friendField.getBuiltText().trim();
      if (!name.isBlank()) {
         FeverVisual.getInstance().getFriendManager().add(name);
         this.friendField.clear();
      }
   }

   private void createConfig() {
      String name = this.configField.getBuiltText().trim();
      if (!name.isBlank()) {
         FeverVisual.getInstance().getConfigManager().createConfig(name);
         this.configField.clear();
      }
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.colorPickers.forEach(picker -> picker.onMouseReleased(mouseX, mouseY, button));
      this.searchField.onMouseReleased(mouseX, mouseY, button);
      this.friendField.onMouseReleased(mouseX, mouseY, button);
      this.configField.onMouseReleased(mouseX, mouseY, button);
      MenuPanel panel = this.getSelectedPanel();
      if (panel != null) this.getVisibleModules(panel).forEach(component -> component.onMouseReleased(mouseX, mouseY, button));
      super.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      MenuPanel panel = this.getSelectedPanel();
      if (panel != null && GuiUtility.isHovered(this.guiX + SIDEBAR_WIDTH, this.guiY + HEADER_HEIGHT, this.guiWidth - SIDEBAR_WIDTH, this.guiHeight - HEADER_HEIGHT, mouseX, mouseY)) {
         for (ModuleComponent component : this.getVisibleModules(panel)) component.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
         panel.getModulesScroll().scroll(verticalAmount);
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      for (ColorPicker picker : this.colorPickers) picker.onKeyPressed(input.key(), input.scancode(), input.modifiers());
      TextField focused = this.focusedField();
      if (focused != null && !this.isBindingModule()) {
         if ((input.key() == 257 || input.key() == 335) && focused == this.friendField) this.addFriend();
         else if ((input.key() == 257 || input.key() == 335) && focused == this.configField) this.createConfig();
         else focused.onKeyPressed(input.key(), input.scancode(), input.modifiers());
         return true;
      }
      MenuPanel panel = this.getSelectedPanel();
      if (panel != null) this.getVisibleModules(panel).forEach(component -> component.onKeyPressed(input.key(), input.scancode(), input.modifiers()));
      return super.keyPressed(input);
   }

   @Override
   public boolean charTyped(CharInput input) {
      TextField focused = this.focusedField();
      if (focused != null && !this.isBindingModule()) {
         focused.charTyped((char)input.codepoint(), input.modifiers());
         return true;
      }
      MenuPanel panel = this.getSelectedPanel();
      if (panel != null) {
         for (ModuleComponent component : this.getVisibleModules(panel)) if (component.charTyped((char)input.codepoint(), input.modifiers())) return true;
      }
      return super.charTyped(input);
   }

   private TextField focusedField() {
      if (this.searchField != null && this.searchField.isFocused()) return this.searchField;
      if (this.friendField != null && this.friendField.isFocused()) return this.friendField;
      if (this.configField != null && this.configField.isFocused()) return this.configField;
      return null;
   }

   private ColorPicker getTopColorPicker(double mouseX, double mouseY) {
      for (int i = this.colorPickers.size() - 1; i >= 0; i--) {
         ColorPicker picker = this.colorPickers.get(i);
         if (picker.isHovered(mouseX, mouseY) || picker.isMouseCaptured()) return picker;
      }
      return null;
   }

   private void bringColorPickerToFront(ColorPicker picker) {
      if (this.colorPickers.remove(picker)) this.colorPickers.add(picker);
   }

   private boolean isBindingModule() {
      return this.panels.stream().flatMap(panel -> panel.getModuleComponents().stream()).anyMatch(ModuleComponent::isBindingMode);
   }

   private void handleMovementKeys() {
      if (mc.player == null || this.focusedField() != null) return;
      KeyBinding[] keys = {mc.options.forwardKey, mc.options.leftKey, mc.options.backKey, mc.options.rightKey, mc.options.jumpKey};
      for (KeyBinding key : keys) {
         int keyCode = InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey()).getCode();
         key.setPressed(InputUtil.isKeyPressed(mc.getWindow(), keyCode));
      }
   }

   private boolean compactSidebar() {
      return this.guiHeight < 280.0F;
   }

   private float categoryY() {
      return this.compactSidebar() ? 40.0F : CATEGORY_Y;
   }

   private float categoryHeight() {
      return this.compactSidebar() ? 16.0F : CATEGORY_HEIGHT;
   }

   private float categoryGap() {
      return this.compactSidebar() ? 2.0F : CATEGORY_GAP;
   }

   private float subHeight() {
      return this.compactSidebar() ? 13.0F : SUB_HEIGHT;
   }

   private float subGap() {
      return this.compactSidebar() ? 1.0F : SUB_GAP;
   }

   private float profileHeight() {
      return this.compactSidebar() ? 34.0F : PROFILE_HEIGHT;
   }

   @Override
   public void close() {
      this.closing = true;
      MenuModule menu = FeverVisual.getInstance().getModuleManager().getModule(MenuModule.class);
      if (menu != null) menu.disable();
      Sounds sounds = FeverVisual.getInstance().getModuleManager().getModule(Sounds.class);
      if (sounds != null && sounds.isEnabled()) ClientSounds.CLICKGUI_OPEN.play(sounds.getVolume().getCurrentValue(), 1.0F);
      FeverVisual.getInstance().getConfigManager().saveCurrent();
      FeverVisual.getInstance().getFileManager().writeFile("client");
      if (TextField.LAST_FIELD != null) TextField.LAST_FIELD.setFocused(false);
      super.close();
   }

   @Override public boolean shouldPause() { return false; }
   @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) { }
   @Override public boolean shouldCloseOnEsc() { return true; }

   @Generated public Animation getSearchAnimation() { return this.searchAnimation; }
   @Generated public Animation getAppendingAnim() { return this.appendingAnim; }
   @Generated public List<MenuPanel> getPanels() { return this.panels; }
   @Generated public float getPanelWidth() { return this.panelWidth; }
   @Generated public float getPanelHeight() { return this.panelHeight; }
   @Generated public String getDesc() { return this.desc; }
   @Generated public List<ColorPicker> getColorPickers() { return this.colorPickers; }
   @Generated public TextField getSearchField() { return this.searchField; }
   @Generated public void setDesc(String desc) { this.desc = desc == null ? "" : desc; }

   private enum DashboardCategory {
      PVP("Pvp", "C", MenuCategory.COMBAT), VISUALS("Visuals", "V", MenuCategory.VISUALS),
      DISPLAY("Display", "D", MenuCategory.DISPLAY), MISC("Miscellaneous", "M", MenuCategory.MISC),
      CONFIG("Config", "P", null), FRIENDS("Friends", "F", null);
      private final String title;
      private final String icon;
      private final MenuCategory menuCategory;
      DashboardCategory(String title, String icon, MenuCategory menuCategory) { this.title = title; this.icon = icon; this.menuCategory = menuCategory; }
      private static DashboardCategory from(MenuCategory category) {
         for (DashboardCategory value : values()) if (value.menuCategory == category) return value;
         return PVP;
      }
   }

   private enum VisualSubCategory {
      PLAYER("Player"), WORLD("World");
      private final String title;
      VisualSubCategory(String title) { this.title = title; }
   }

   private static final class ModuleGroup {
      private final String title;
      private final List<ModuleComponent> modules = new ArrayList<>();
      private ModuleGroup(String title) { this.title = title; }
   }
}

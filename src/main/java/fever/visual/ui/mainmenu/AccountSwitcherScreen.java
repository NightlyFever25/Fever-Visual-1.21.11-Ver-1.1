package fever.visual.ui.mainmenu;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomScreen;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.framework.objects.MouseButton;
import fever.visual.framework.objects.gradient.impl.VerticalGradient;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.ui.components.textfield.TextField;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.math.MathUtility;
import fever.visual.utility.render.DrawUtility;
import fever.visual.utility.render.RenderUtility;
import fever.visual.utility.render.ScissorUtility;
import fever.visual.utility.render.obj.Rect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;

public class AccountSwitcherScreen extends CustomScreen implements IMinecraft {

    private final Screen parent;
    private final AccountManager manager;
    private final TextField inputField;
    private float scrollOffset = 0.0f;
    private float targetScrollOffset = 0.0f;
    private float[] hoverAnimations1;
    private int selectedAccountIndex = -1;
    private float randomHoverAnim = 0.0f;
    private float addHoverAnim = 0.0f;
    private float cancelHoverAnim = 0.0f;
    private final Animation openAnimation = new Animation(300L, 0.0f, Easing.FIGMA_EASE_IN_OUT);
    private boolean closing = false;

    public AccountSwitcherScreen(Screen parent, AccountManager manager) {
        this.parent = parent;
        this.manager = manager;
        this.inputField = new TextField(Fonts.MEDIUM.getFont(7.0f));
        this.inputField.setPreview(Localizator.translate("modules.desc.account"));
    }

    private Identifier getSkinTexture(String username) {
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes());
        com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(uuid, username);
        return MinecraftClient.getInstance().getSkinProvider().supplySkinTextures(profile, false).get().body().texturePath();
    }

    @Override
    public void render(UIContext context) {
        this.openAnimation.update(!this.closing);
        float animValue = this.openAnimation.getValue();
        if (this.closing && animValue == 0.0f) {
            if (this.parent != null) {
                mc.setScreen(this.parent);
            } else {
                super.close();
            }
            return;
        }

        boolean isGlassMode = Interface.glassSelected();
        List<Account> accounts = this.manager.getAccounts();
        this.scrollOffset = MathUtility.interpolate(this.scrollOffset, this.targetScrollOffset, 0.15f);

        int centerX = (int) ((float) this.width / 2.0f);
        Rect rect = new Rect((float) (-this.width) / 2.0f, (float) (-this.width) / 3.0f, (float) this.width * 1.5f, this.width);

        context.drawRoundedRect(0.0f, 0.0f, (float) this.width, (float) this.height, BorderRadius.ZERO,
                new VerticalGradient(new ColorRGBA(26.0f, 34.0f, 56.0f), new ColorRGBA(5.0f, 3.0f, 12.0f)));

        RenderUtility.scale(context.getMatrices(), (float) this.width / 2.0f, (float) this.height / 2.0f, 1.1f);
        context.drawTexture(FeverVisual.id("image/mainmenu/background.png"), rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
        RenderUtility.end(context.getMatrices());

        context.updateBlur();

        float scale = 0.8f + 0.2f * animValue;
        float alpha = animValue;

        int panelWidth = 380;
        int panelHeight = 300;
        int panelX = centerX - panelWidth / 2;
        int panelY = (int) ((float) (this.height - panelHeight) / 2.0f);

        RenderUtility.scale(context.getMatrices(), (float) this.width / 2.0f, (float) this.height / 2.0f, scale);

        if (isGlassMode) {
            DrawUtility.drawLiquidGlass(context.getMatrices(), panelX, panelY, panelWidth, panelHeight,
                    BorderRadius.all(12.0f), ColorRGBA.WHITE.withAlpha(255.0f * alpha), 1.0f, 50.0f,
                    ColorRGBA.WHITE.withAlpha(255.0f), 1.0f, true, 0.0f, 0.08f, 2.0f, false);
        } else {
            context.drawBlurredRect(panelX, panelY, panelWidth, panelHeight, 45.0f,
                    BorderRadius.all(12.0f), ColorRGBA.WHITE.withAlpha(255.0f * alpha));
        }

        context.drawRoundedRect((float) panelX, (float) panelY, (float) panelWidth, (float) panelHeight,
                BorderRadius.all(12.0f), Colors.getBackgroundColor().withAlpha(255.0f * (isGlassMode ? 0.2f : 0.9f) * alpha));

        context.drawCenteredText(Fonts.SEMIBOLD.getFont(14.0f), "Alt Manager", (float) this.width / 2.0f, panelY + 10,
                Colors.getTextColor().withAlpha(255.0f * alpha));

        int leftPanelWidth = 140;
        int leftPanelHeight = panelHeight - 40;
        int leftPanelX = panelX + 8;
        int leftPanelY = panelY + 32;

        int listWidth = panelWidth - leftPanelWidth - 24;
        int listHeight = leftPanelHeight;
        int listX = leftPanelX + leftPanelWidth + 8;
        int listY = leftPanelY;

        int inputWidth = leftPanelWidth - 16;
        int inputHeight = 24;
        int inputX = leftPanelX + 8;
        int inputY = leftPanelY + 8;

        int randomButtonWidth = (leftPanelWidth - 21) / 2;
        int randomButtonHeight = 22;
        int randomButtonX = leftPanelX + 8;
        int randomButtonY = leftPanelY + leftPanelHeight - 8 - 22 - 8 - 22;

        int addButtonWidth = (leftPanelWidth - 21) / 2;
        int addButtonHeight = 22;
        int addButtonX = randomButtonX + randomButtonWidth + 5;
        int addButtonY = randomButtonY;

        int cancelButtonWidth = leftPanelWidth - 16;
        int cancelButtonHeight = 22;
        int cancelButtonX = leftPanelX + 8;
        int cancelButtonY = randomButtonY + randomButtonHeight + 8;

        context.drawRoundedRect((float) inputX, (float) inputY, (float) inputWidth, (float) inputHeight,
                BorderRadius.all(6.0f), Colors.getBackgroundColor().withAlpha(255.0f * (isGlassMode ? 0.2f : 0.9f)));

        this.inputField.setX(inputX + 5);
        this.inputField.setY((float) inputY + (float) (inputHeight - 14) / 2.0f);
        this.inputField.setWidth(inputWidth - 10);
        this.inputField.setHeight(14.0f);
        this.inputField.render(context);

        context.drawRoundedRect((float) listX, (float) listY, (float) listWidth, (float) listHeight,
                BorderRadius.all(8.0f), Colors.getBackgroundColor().withAlpha(255.0f * (isGlassMode ? 0.2f : 0.9f)));

        ScissorUtility.push(context.getMatrices(), (float) listX, (float) listY, (float) listWidth, (float) listHeight);

        if (this.hoverAnimations1 == null || this.hoverAnimations1.length != accounts.size()) {
            this.hoverAnimations1 = new float[accounts.size()];
        }

        float startY = listY + 5;
        float itemHeight = 30.0f;

        for (int i = 0; i < accounts.size(); ++i) {
            float y = startY - this.scrollOffset + (float) i * itemHeight;
            if (y + itemHeight < (float) listY || y > (float) (listY + listHeight)) continue;

            boolean isHovered = GuiUtility.isHovered((double) (listX + 5), (double) y, (double) (listWidth - 10), 26.0,
                    context.getMouseX(), context.getMouseY());

            this.hoverAnimations1[i] = MathUtility.interpolate(this.hoverAnimations1[i], isHovered ? 1.0 : 0.0, 0.15f);

            ColorRGBA bgColor = i == this.selectedAccountIndex ?
                    (isGlassMode ? new ColorRGBA(60.0f, 60.0f, 80.0f, 230.0f) : new ColorRGBA(0.0f, 0.0f, 0.0f, 230.0f)) :
                    (isGlassMode ? new ColorRGBA(40.0f, 40.0f, 50.0f, 200.0f).mix(new ColorRGBA(60.0f, 60.0f, 80.0f, 230.0f), this.hoverAnimations1[i]) :
                            new ColorRGBA(0.0f, 0.0f, 0.0f, 200.0f).mix(new ColorRGBA(20.0f, 20.0f, 20.0f, 230.0f), this.hoverAnimations1[i]));

            context.drawRoundedRect((float) (listX + 5), y, (float) (listWidth - 10), 26.0f, BorderRadius.all(5.0f), bgColor);
            context.drawRoundedRect((float) (listX + 5), y, (float) (listWidth - 10), 26.0f, BorderRadius.all(5.0f),
                    isGlassMode ? new ColorRGBA(80.0f, 80.0f, 100.0f, 100.0f) : new ColorRGBA(30.0f, 30.0f, 30.0f, 100.0f));

            float skinSize = 20.0f;
            float skinX = listX + 8;
            float skinY = y + 3.0f;
            Identifier skinTexture = this.getSkinTexture(accounts.get(i).getUsername());

            DrawUtility.drawRoundedTextureWithUV(context.getMatrices(), skinTexture, skinX, skinY, skinSize, skinSize,
                    BorderRadius.all(4.0f), ColorRGBA.WHITE, 0.125f, 0.125f, 0.25f, 0.25f);
            DrawUtility.drawRoundedTextureWithUV(context.getMatrices(), skinTexture, skinX, skinY, skinSize, skinSize,
                    BorderRadius.all(4.0f), ColorRGBA.WHITE, 0.625f, 0.125f, 0.75f, 0.25f);

            float nickX = skinX + skinSize + 8.0f;
            float nickY = y + (26.0f - Fonts.MEDIUM.getFont(8.0f).height()) / 2.0f;
            context.drawText(Fonts.MEDIUM.getFont(8.0f), accounts.get(i).getUsername(), nickX, nickY, Colors.getTextColor());
        }

        ScissorUtility.pop();

        boolean isHoveredRandom = GuiUtility.isHovered((double) randomButtonX, (double) randomButtonY,
                (double) randomButtonWidth, (double) randomButtonHeight, context.getMouseX(), context.getMouseY());
        this.randomHoverAnim = MathUtility.interpolate(this.randomHoverAnim, isHoveredRandom ? 1.0 : 0.0, 0.15f);
        context.drawRoundedRect((float) randomButtonX, (float) randomButtonY, (float) randomButtonWidth, (float) randomButtonHeight,
                BorderRadius.all(6.0f), Colors.getBackgroundColor().withAlpha(255.0f * (isGlassMode ? 0.2f : 0.9f)));
        context.drawCenteredText(Fonts.SEMIBOLD.getFont(7.0f), Localizator.translate("modules.desc.account.random"), (float) randomButtonX + (float) randomButtonWidth / 2.0f,
                (float) randomButtonY + (float) (randomButtonHeight - 7) / 2.0f, Colors.getTextColor());

        boolean isHoveredAdd = GuiUtility.isHovered((double) addButtonX, (double) addButtonY,
                (double) addButtonWidth, (double) addButtonHeight, context.getMouseX(), context.getMouseY());
        this.addHoverAnim = MathUtility.interpolate(this.addHoverAnim, isHoveredAdd ? 1.0 : 0.0, 0.15f);
        context.drawRoundedRect((float) addButtonX, (float) addButtonY, (float) addButtonWidth, (float) addButtonHeight,
                BorderRadius.all(6.0f), Colors.getBackgroundColor().withAlpha(255.0f * (isGlassMode ? 0.2f : 0.9f)));
        context.drawCenteredText(Fonts.SEMIBOLD.getFont(7.0f),Localizator.translate( "modules.desc.account.add"), (float) addButtonX + (float) addButtonWidth / 2.0f,
                (float) addButtonY + (float) (addButtonHeight - 7) / 2.0f, Colors.getTextColor());

        boolean isHoveredCancel = GuiUtility.isHovered((double) cancelButtonX, (double) cancelButtonY,
                (double) cancelButtonWidth, (double) cancelButtonHeight, context.getMouseX(), context.getMouseY());
        this.cancelHoverAnim = MathUtility.interpolate(this.cancelHoverAnim, isHoveredCancel ? 1.0 : 0.0, 0.15f);
        context.drawRoundedRect((float) cancelButtonX, (float) cancelButtonY, (float) cancelButtonWidth, (float) cancelButtonHeight,
                BorderRadius.all(6.0f), Colors.getBackgroundColor().withAlpha(255.0f * (isGlassMode ? 0.2f : 0.9f)));
        context.drawCenteredText(Fonts.SEMIBOLD.getFont(7.0f), Localizator.translate("modules.desc.account.cancel"), (float) cancelButtonX + (float) cancelButtonWidth / 2.0f,
                (float) cancelButtonY + (float) (cancelButtonHeight - 7) / 2.0f, Colors.getTextColor());

        RenderUtility.end(context.getMatrices());
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        List<Account> accounts = this.manager.getAccounts();
        int centerX = (int) ((float) this.width / 2.0f);
        int panelWidth = 380;
        int panelHeight = 300;
        int panelX = centerX - panelWidth / 2;
        int panelY = (int) ((float) (this.height - panelHeight) / 2.0f);

        int leftPanelWidth = 140;
        int leftPanelHeight = panelHeight - 40;
        int leftPanelX = panelX + 8;
        int leftPanelY = panelY + 32;

        int listWidth = panelWidth - leftPanelWidth - 24;
        int listHeight = leftPanelHeight;
        int listX = leftPanelX + leftPanelWidth + 8;
        int listY = leftPanelY;

        int inputWidth = leftPanelWidth - 16;
        int inputHeight = 24;
        int inputX = leftPanelX + 8;
        int inputY = leftPanelY + 8;

        int randomButtonWidth = (leftPanelWidth - 21) / 2;
        int randomButtonHeight = 22;
        int randomButtonX = leftPanelX + 8;
        int randomButtonY = leftPanelY + leftPanelHeight - 8 - 22 - 8 - 22;

        int addButtonWidth = (leftPanelWidth - 21) / 2;
        int addButtonHeight = 22;
        int addButtonX = randomButtonX + randomButtonWidth + 5;
        int addButtonY = randomButtonY;

        int cancelButtonWidth = leftPanelWidth - 16;
        int cancelButtonHeight = 22;
        int cancelButtonX = leftPanelX + 8;
        int cancelButtonY = randomButtonY + randomButtonHeight + 8;

        if (GuiUtility.isHovered((double) inputX, (double) inputY, (double) inputWidth, (double) inputHeight, mouseX, mouseY)) {
            this.inputField.onMouseClicked(mouseX, mouseY, button);
            return;
        }

        if (button == MouseButton.LEFT && GuiUtility.isHovered((double) addButtonX, (double) addButtonY,
                (double) addButtonWidth, (double) addButtonHeight, mouseX, mouseY)) {
            String newAccount = this.inputField.getBuiltText().trim();
            if (!newAccount.isEmpty() && accounts.stream().noneMatch(a -> a.getUsername().equalsIgnoreCase(newAccount))) {
                this.manager.addAccount(newAccount);
                this.inputField.clear();
                String finalAccountName = newAccount;
                Account newAcc = accounts.stream().filter(a -> a.getUsername().equals(finalAccountName)).findFirst().orElse(null);
                if (newAcc != null) {
                    this.manager.select(newAcc);
                    this.selectedAccountIndex = accounts.indexOf(newAcc);
                }
            }
            return;
        }

        if (button == MouseButton.LEFT && GuiUtility.isHovered((double) randomButtonX, (double) randomButtonY,
                (double) randomButtonWidth, (double) randomButtonHeight, mouseX, mouseY)) {
            String accountName = this.generateRandomName();
            while (true) {
                String checkName = accountName;
                if (accounts.stream().noneMatch(a -> a.getUsername().equals(checkName))) break;
                accountName = this.generateRandomName();
            }
            this.manager.addAccount(accountName);
            String finalAccountName = accountName;
            Account newAcc = accounts.stream().filter(a -> a.getUsername().equals(finalAccountName)).findFirst().orElse(null);
            if (newAcc != null) {
                this.manager.select(newAcc);
                this.selectedAccountIndex = accounts.indexOf(newAcc);
            }
            return;
        }

        if (button == MouseButton.LEFT && GuiUtility.isHovered((double) cancelButtonX, (double) cancelButtonY,
                (double) cancelButtonWidth, (double) cancelButtonHeight, mouseX, mouseY)) {
            this.close();
            return;
        }

        if (GuiUtility.isHovered((double) listX, (double) listY, (double) listWidth, (double) listHeight, mouseX, mouseY)) {
            float startY = listY + 5;
            float itemHeight = 30.0f;

            for (int i = 0; i < accounts.size(); ++i) {
                float y = startY - this.scrollOffset + (float) i * itemHeight;
                if (y + itemHeight < (float) listY || y > (float) (listY + listHeight)) continue;

                if (GuiUtility.isHovered((double) (listX + 5), (double) y, (double) (listWidth - 10), 26.0, mouseX, mouseY)) {
                    float skinSize = 20.0f;
                    float skinX = listX + 8;
                    float skinY = y + 3.0f;
                    float nickX = skinX + skinSize + 8.0f;
                    float nickY = y + (26.0f - Fonts.MEDIUM.getFont(8.0f).height()) / 2.0f;
                    float nickWidth = Fonts.MEDIUM.getFont(8.0f).width(accounts.get(i).getUsername());

                    if (GuiUtility.isHovered((double) nickX, (double) (nickY - 2.0f), (double) nickWidth, 12.0, mouseX, mouseY) && button == MouseButton.LEFT) {
                        GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), accounts.get(i).getUsername());
                        return;
                    }

                    if (button == MouseButton.LEFT) {
                        Account selected = accounts.get(i);
                        this.manager.select(selected);
                        this.selectedAccountIndex = i;
                        return;
                    }

                    if (button == MouseButton.RIGHT) {
                        if (this.selectedAccountIndex == i) {
                            this.selectedAccountIndex = -1;
                        }
                        this.manager.removeAccount(accounts.get(i));
                        int maxOffset = Math.max(0, (int) ((float) accounts.size() * itemHeight) - listHeight);
                        this.targetScrollOffset = Math.max(0.0f, Math.min(this.targetScrollOffset, (float) maxOffset));
                        return;
                    }
                }
            }
        }

        super.onMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        if (this.inputField.isFocused()) {
            this.inputField.onMouseReleased(mouseX, mouseY, button);
        }
        super.onMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        List<Account> accounts = this.manager.getAccounts();
        int centerX = (int) ((float) this.width / 2.0f);
        int panelWidth = 380;
        int panelHeight = 300;
        int panelX = centerX - panelWidth / 2;
        int panelY = (int) ((float) (this.height - panelHeight) / 2.0f);
        int leftPanelWidth = 140;
        int listWidth = panelWidth - leftPanelWidth - 24;
        int listHeight = panelHeight - 40;
        int listX = panelX + 8 + leftPanelWidth + 8;
        int listY = panelY + 32;

        if (mouseY >= (double) listY && mouseY <= (double) (listY + listHeight) &&
                mouseX >= (double) listX && mouseX <= (double) (listX + listWidth)) {
            this.targetScrollOffset = (float) ((double) this.targetScrollOffset - verticalAmount * 25.0);
            int maxOffset = Math.max(0, accounts.size() * 30 - listHeight + 10);
            this.targetScrollOffset = Math.max(0.0f, Math.min(this.targetScrollOffset, (float) maxOffset));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();
        if (this.inputField.isFocused() && (keyCode == 257 || keyCode == 335)) {
            String newAccount = this.inputField.getBuiltText().trim();
            if (!newAccount.isEmpty() && this.manager.getAccounts().stream().noneMatch(a -> a.getUsername().equalsIgnoreCase(newAccount))) {
                this.manager.addAccount(newAccount);
                this.inputField.clear();
            }
            return true;
        }

        if (this.inputField.isFocused()) {
            this.inputField.onKeyPressed(keyCode, scanCode, modifiers);
        }

        if (keyCode == 256) {
            this.close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        char chr = (char)input.codepoint();
        int modifiers = input.modifiers();
        if (this.inputField.isFocused()) {
            this.inputField.charTyped(chr, modifiers);
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void close() {
        this.closing = true;
    }

    private String generateRandomName() {
        String[] prefixes = new String[]{"Pro", "Epic", "Cool", "Super", "Mega", "Ultra", "Hyper", "Dark", "Light", "Fire"};
        String[] suffixes = new String[]{"Gamer", "Player", "Master", "King", "Lord", "Warrior", "Hunter", "Ninja", "Dragon", "Phoenix"};
        return prefixes[(int) (Math.random() * (double) prefixes.length)] +
                suffixes[(int) (Math.random() * (double) suffixes.length)] +
                (int) (Math.random() * 1000.0);
    }
}

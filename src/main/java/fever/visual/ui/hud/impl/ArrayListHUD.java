package fever.visual.ui.hud.impl;

import fever.visual.FeverVisual;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.systems.modules.Module;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.theme.Theme;
import fever.visual.ui.hud.HudList;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.render.RenderUtility;
import fever.visual.utility.render.compat.RenderSystem;

import java.util.*;

public class ArrayListHUD extends HudList {

    private final Map<Module, Animation> moduleAnimations = new HashMap<>();
    private final Map<Module, Animation> moduleContentAnimations = new HashMap<>();
    private final Map<Module, Float> moduleWidthCache = new HashMap<>();
    private final List<Module> modulesToRenderCache = new ArrayList<>();
    private List<Module> cachedModules = new ArrayList<>();
    private int lastModuleFingerprint;
    private boolean modulesToRenderDirty = true;
    private Module[] renderModules = new Module[32];
    private float[] renderX = new float[32];
    private float[] renderY = new float[32];
    private float[] renderWidth = new float[32];
    private float[] renderHeight = new float[32];
    private float[] renderAnim = new float[32];
    private float[] renderAlpha = new float[32];
    private float[] renderContentAlpha = new float[32];
    private float[] renderTextX = new float[32];
    private float[] renderTextY = new float[32];
    private int renderCount;

    public ArrayListHUD() {
        super("hud.arraylist", "icons/hud/arraylist.png");
    }

    @Override
    public void render(UIContext context) {
        this.update(context);
        float anim = this.animation.getValue() * this.visible.getValue();
        if (anim != 0.0F) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, anim));
            float scale = 0.5F + anim * 0.5F - 0.05F * this.selecting.getValue();
            RenderUtility.scale(context.getMatrices(), this.x + this.width / 2.0F, this.y + this.height / 2.0F, scale);
            this.renderComponent(context);
            RenderUtility.end(context.getMatrices());
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public void update(UIContext context) {
        updateModuleList();
        updateModuleAnimations();
        Font font = Fonts.REGULAR.getFont(7.0F);
        this.width = 80.0F;

        for (Module module : cachedModules) {
            Animation anim = moduleAnimations.get(module);
            if (anim != null && anim.getValue() > 0.01F) {
                float textWidth = font.width(module.getName());
                this.width = Math.max(this.width, textWidth + 20.0F);
            }
        }
        for (Map.Entry<Module, Animation> entry : moduleAnimations.entrySet()) {
            if (entry.getValue().getValue() > 0.01F) {
                float textWidth = font.width(entry.getKey().getName());
                this.width = Math.max(this.width, textWidth + 20.0F);
            }
        }

        this.height = 0.0F;
        float elementHeight = 13.0F;
        float elementSpacing = 0.5F;

        for (Module module : cachedModules) {
            Animation anim = moduleAnimations.get(module);
            if (anim != null) {
                this.height += (elementHeight + elementSpacing) * anim.getValue();
            }
        }

        super.update(context);
    }

    @Override
    protected void renderComponent(UIContext context) {
        if (cachedModules.isEmpty() && !hasVisibleAnimatedModules()) {
            return;
        }

        boolean dark = FeverVisual.getInstance().getThemeManager().getCurrentTheme() == Theme.DARK;
        float glassIntensity = Interface.glass();
        float minimalizm = Interface.minimalizm();
        boolean showGlass = Interface.showGlass();
        boolean showMinimalizm = Interface.showMinimalizm();

        ColorRGBA bgColor = Colors.getBackgroundColor()
                .withAlpha(255.0F * (dark ? 0.8F - 0.6F * glassIntensity : 0.7F));

        Font font = Fonts.REGULAR.getFont(7.0F);
        float elementHeight = 13.0F;
        float elementSpacing = 0.5F;
        float padding = 5.0F;
        float rounding = 4.5F;

        int screenWidth = mc.getWindow().getScaledWidth();
        boolean isLeftSide = this.x < screenWidth / 2.0F;

        float maxWidth = this.width;
        float currentY = this.y;
        List<Module> modulesToRender = getModulesToRender(font);

        this.renderCount = 0;
        for (Module module : modulesToRender) {
            Animation anim = moduleAnimations.get(module);
            Animation contentAnim = moduleContentAnimations.get(module);
            if (anim == null || contentAnim == null) continue;

            float animValue = anim.getValue();
            if (animValue <= 0.01F) continue;

            float textWidth = getModuleWidth(module, font);
            float elementWidth = textWidth + padding * 2.0F;

            float scaledWidth = elementWidth * animValue;
            float scaledHeight = elementHeight * animValue;

            float elementX;
            if (isLeftSide) {
                elementX = this.x;
            } else {
                elementX = this.x + maxWidth - scaledWidth;
            }

            float globalAlpha = 255.0F * this.animation.getValue() * this.visible.getValue();
            float alpha = globalAlpha * animValue;
            float contentAlpha = globalAlpha * contentAnim.getValue();
            ensureRenderCapacity(this.renderCount + 1);
            int index = this.renderCount++;
            this.renderModules[index] = module;
            this.renderX[index] = elementX;
            this.renderY[index] = currentY;
            this.renderWidth[index] = scaledWidth;
            this.renderHeight[index] = scaledHeight;
            this.renderAnim[index] = animValue;
            this.renderAlpha[index] = alpha;
            this.renderContentAlpha[index] = contentAlpha;

            float textScale = contentAnim.getValue();
            float scaledTextWidth = textWidth * textScale;
            float scaledTextHeight = font.height() * textScale;
            this.renderTextX[index] = elementX + (scaledWidth - scaledTextWidth) / 2.0F;
            this.renderTextY[index] = currentY + (scaledHeight - scaledTextHeight) / 2.0F;
            currentY += scaledHeight + elementSpacing;
        }

        if (this.dragAnim.getValue() > 0.001F) {
            for (int i = 0; i < this.renderCount; i++) {
                context.drawShadow(
                        this.renderX[i] - 5.0F,
                        this.renderY[i] - 5.0F,
                        this.renderWidth[i] + 10.0F,
                        this.renderHeight[i] + 10.0F,
                        15.0F,
                        BorderRadius.all(6.0F * this.renderAnim[i]),
                        ColorRGBA.BLACK.withAlpha(63.75F * this.dragAnim.getValue() * this.renderAnim[i])
                );
            }
        }

        if (showMinimalizm) {
            for (int i = 0; i < this.renderCount; i++) {
                context.drawBlurredRect(
                        this.renderX[i], this.renderY[i], this.renderWidth[i], this.renderHeight[i],
                        45.0F, 7.0F,
                        BorderRadius.all(rounding * this.renderAnim[i]),
                        ColorRGBA.WHITE.withAlpha(this.renderAlpha[i] * minimalizm)
                );
            }
        }

        if (showGlass) {
            for (int i = 0; i < this.renderCount; i++) {
                context.drawLiquidGlass(
                        this.renderX[i], this.renderY[i], this.renderWidth[i], this.renderHeight[i],
                        7.0F, 0.08F,
                        BorderRadius.all(rounding * this.renderAnim[i]),
                        ColorRGBA.WHITE.withAlpha(this.renderAlpha[i] * glassIntensity)
                );
            }
        }

        for (int i = 0; i < this.renderCount; i++) {
            context.drawSquircle(
                    this.renderX[i], this.renderY[i], this.renderWidth[i], this.renderHeight[i],
                    rounding * this.renderAnim[i],
                    BorderRadius.all(rounding * this.renderAnim[i]),
                    bgColor.withAlpha((int)(bgColor.getAlpha() * this.animation.getValue() * this.renderAnim[i]))
            );
        }

        for (int i = 0; i < this.renderCount; i++) {
            context.drawText(
                    font,
                    this.renderModules[i].getName(),
                    this.renderTextX[i],
                    this.renderTextY[i],
                    Colors.getTextColor().withAlpha((int)this.renderContentAlpha[i])
            );
        }
    }

    private void ensureRenderCapacity(int capacity) {
        if (capacity <= this.renderModules.length) return;
        int newCapacity = Math.max(capacity, this.renderModules.length * 2);
        this.renderModules = Arrays.copyOf(this.renderModules, newCapacity);
        this.renderX = Arrays.copyOf(this.renderX, newCapacity);
        this.renderY = Arrays.copyOf(this.renderY, newCapacity);
        this.renderWidth = Arrays.copyOf(this.renderWidth, newCapacity);
        this.renderHeight = Arrays.copyOf(this.renderHeight, newCapacity);
        this.renderAnim = Arrays.copyOf(this.renderAnim, newCapacity);
        this.renderAlpha = Arrays.copyOf(this.renderAlpha, newCapacity);
        this.renderContentAlpha = Arrays.copyOf(this.renderContentAlpha, newCapacity);
        this.renderTextX = Arrays.copyOf(this.renderTextX, newCapacity);
        this.renderTextY = Arrays.copyOf(this.renderTextY, newCapacity);
    }

    private void updateModuleList() {
        List<Module> allModules = FeverVisual.getInstance().getModuleManager().getModules();
        int fingerprint = 1;
        for (Module module : allModules) {
            if (module.isHidden()) continue;

            if (!moduleAnimations.containsKey(module)) {
                Animation anim = new Animation(300L, 0.0F, Easing.BAKEK);
                moduleAnimations.put(module, anim);

                Animation contentAnim = new Animation(200L, 0.0F, Easing.BAKEK);
                moduleContentAnimations.put(module, contentAnim);
                if (module.isEnabled()) {
                    anim.setValue(0.0F);
                    contentAnim.setValue(0.0F);
                }
            }
            fingerprint = 31 * fingerprint + System.identityHashCode(module);
            fingerprint = 31 * fingerprint + (module.isEnabled() ? 1 : 0);
            fingerprint = 31 * fingerprint + (module.isHidden() ? 1 : 0);
            fingerprint = 31 * fingerprint + module.getCategory().ordinal();
        }

        if (fingerprint != this.lastModuleFingerprint) {
            Font font = Fonts.REGULAR.getFont(7.0F);
            List<Module> visibleModules = new ArrayList<>();
            for (Module module : allModules) {
                if (module.isEnabled() && !module.isHidden() && module.getCategory() != ModuleCategory.VISUALS) {
                    visibleModules.add(module);
                    getModuleWidth(module, font);
                }
            }
            visibleModules.sort((a, b) -> Float.compare(getModuleWidth(b, font), getModuleWidth(a, font)));
            this.cachedModules = visibleModules;
            this.lastModuleFingerprint = fingerprint;
            this.modulesToRenderDirty = true;
        }
    }

    private void updateModuleAnimations() {
        for (Map.Entry<Module, Animation> entry : moduleAnimations.entrySet()) {
            Module module = entry.getKey();
            Animation anim = entry.getValue();
            Animation contentAnim = moduleContentAnimations.get(module);

            if (contentAnim != null) {
                boolean shouldShow = module.isEnabled() && !module.isHidden() && module.getCategory() != ModuleCategory.VISUALS;
                anim.update(shouldShow);
                if (shouldShow) {
                    if (anim.getValue() >= 0.7F) {
                        contentAnim.update(true);
                    }
                } else {
                    if (contentAnim.getValue() <= 0.1F) {
                    } else {
                        contentAnim.update(false);
                    }
                }
            }
        }
    }

    private List<Module> getModulesToRender(Font font) {
        if (!this.modulesToRenderDirty) {
            return this.modulesToRenderCache;
        }

        this.modulesToRenderCache.clear();
        this.modulesToRenderCache.addAll(cachedModules);
        for (Module module : moduleAnimations.keySet()) {
            Animation animation = moduleAnimations.get(module);
            if (!this.modulesToRenderCache.contains(module) && animation != null && animation.getValue() > 0.01F) {
                this.modulesToRenderCache.add(module);
            }
        }

        this.modulesToRenderCache.sort((a, b) -> Float.compare(getModuleWidth(b, font), getModuleWidth(a, font)));
        this.modulesToRenderDirty = false;
        return this.modulesToRenderCache;
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        Font font = Fonts.REGULAR.getFont(7.0F);
        float elementHeight = 13.0F;
        float elementSpacing = 0.5F;
        float padding = 5.0F;
        float currentY = this.y;
        float maxWidth = this.width;
        int screenWidth = mc.getWindow().getScaledWidth();
        boolean isLeftSide = this.x < screenWidth / 2.0F;

        for (Module module : getModulesToRender(font)) {
            Animation animation = moduleAnimations.get(module);
            if (animation == null || animation.getValue() <= 0.01F) {
                continue;
            }

            float animValue = animation.getValue();
            float elementWidth = (getModuleWidth(module, font) + padding * 2.0F) * animValue;
            float elementRenderedHeight = elementHeight * animValue;
            float elementX = isLeftSide ? this.x : this.x + maxWidth - elementWidth;

            if (mouseX >= elementX
                    && mouseX <= elementX + elementWidth
                    && mouseY >= currentY
                    && mouseY <= currentY + elementRenderedHeight) {
                return true;
            }

            currentY += elementRenderedHeight + elementSpacing;
        }

        return false;
    }

    @Override
    public boolean isHovered(float mouseX, float mouseY) {
        return this.isHovered((double)mouseX, (double)mouseY);
    }

    @Override
    public boolean show() {
        return !cachedModules.isEmpty() || hasVisibleAnimatedModules();
    }

    private float getModuleWidth(Module module, Font font) {
        return this.moduleWidthCache.computeIfAbsent(module, key -> font.width(key.getName()));
    }

    private boolean hasVisibleAnimatedModules() {
        for (Animation animation : this.moduleAnimations.values()) {
            if (animation.getValue() > 0.01F) {
                return true;
            }
        }

        return false;
    }
}

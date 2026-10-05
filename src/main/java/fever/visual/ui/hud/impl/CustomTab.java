package fever.visual.ui.hud.impl;

import com.mojang.authlib.GameProfile;
import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.systems.modules.modules.visuals.BetterTab;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.ui.hud.HudElement;
import fever.visual.ui.hud.impl.island.DynamicIsland;
import fever.visual.utility.animation.base.Animation;
import fever.visual.utility.animation.base.Easing;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.gui.GuiUtility;
import fever.visual.utility.render.ScissorUtility;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CustomTab extends HudElement {
    private static final float TOP = 10.0f;
    private static final float SIDE_GAP = 5.0f;
    private static final float ROW_HEIGHT = 10.0f;
    private static final float PANEL_PADDING = 6.0f;
    private static final float HEADER_TOP_OFFSET = 4.0f;
    private static final float TEXT_LINE_HEIGHT = 9.0f;
    private static final float MAX_PANEL_SCREEN_SHARE = 0.60f;
    private static final long LAYOUT_REFRESH_MS = 250L;
    private static final ColorRGBA HEART_SCORE_COLOR = new ColorRGBA(255.0f, 94.0f, 94.0f);
    private static final ColorRGBA PING_UNKNOWN_COLOR = new ColorRGBA(170.0f, 170.0f, 170.0f);
    private static final ColorRGBA PING_GOOD_COLOR = new ColorRGBA(80.0f, 255.0f, 130.0f);
    private static final ColorRGBA PING_MEDIUM_COLOR = new ColorRGBA(255.0f, 205.0f, 82.0f);
    private static final ColorRGBA PING_BAD_COLOR = new ColorRGBA(255.0f, 94.0f, 94.0f);
    private static final Comparator<PlayerListEntry> ENTRY_ORDERING = Comparator
            .comparingInt((PlayerListEntry entry) -> -entry.getListOrder())
            .thenComparingInt(entry -> entry.getGameMode() == GameMode.SPECTATOR ? 1 : 0)
            .thenComparing(entry -> {
                Team team = entry.getScoreboardTeam();
                return team == null ? "" : team.getName();
            })
            .thenComparing(entry -> entry.getProfile().name(), String.CASE_INSENSITIVE_ORDER);
    private final Animation overlayAnimation = new Animation(170L, 0.0f, Easing.FIGMA_EASE_IN_OUT);
    @Nullable
    private Layout cachedLayout;
    private int cachedScaledWindowWidth = -1;
    private int cachedScaledWindowHeight = -1;
    private int cachedPlayerCount = -1;
    private long cachedPlayerLimit = -1L;
    private int cachedHeaderHash;
    private int cachedFooterHash;
    @Nullable
    private ScoreboardObjective cachedObjective;
    private long nextLayoutRefreshAtMs;
    private static boolean deferredRender;
    private static int deferredWidth;
    @Nullable
    private static Scoreboard deferredScoreboard;
    @Nullable
    private static ScoreboardObjective deferredObjective;
    @Nullable
    private static PlayerListHud deferredVanilla;
    @Nullable
    private static Text deferredHeader;
    @Nullable
    private static Text deferredFooter;

    public CustomTab() {
        super("hud.custom_tab", "icons/hud/who.png");
    }

    @Override
    public void update(UIContext context) {
        this.width = 0.0f;
        this.height = 0.0f;
        super.update(context);
    }

    @Override
    protected void renderComponent(UIContext context) {
        this.renderStyledTab(context, context.getScaledWindowWidth(), null, null, mc.inGameHud.getPlayerListHud(), null, null, this.animation.getValue() * this.visible.getValue());
    }

    @Override
    public boolean show() {
        return false;
    }

    public static boolean renderReplacement(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, @Nullable ScoreboardObjective objective, PlayerListHud vanilla, @Nullable Text header, @Nullable Text footer) {
        CustomTab tab = CustomTab.getElement();
        if (tab == null) {
            return false;
        }
        if (!tab.isReplacementEnabled()) {
            tab.overlayAnimation.setValue(0.0f);
            return false;
        }
        if (mc.currentScreen instanceof ChatScreen) {
            tab.overlayAnimation.setValue(0.0f);
            return false;
        }
        tab.overlayAnimation.update(true);
        float alpha = tab.overlayAnimation.getValue();
        deferredRender = true;
        deferredWidth = scaledWindowWidth;
        deferredScoreboard = scoreboard;
        deferredObjective = objective;
        deferredVanilla = vanilla;
        deferredHeader = header;
        deferredFooter = footer;
        return true;
    }

    public static void renderDeferred(DrawContext context) {
        if (!deferredRender) {
            return;
        }
        deferredRender = false;
        CustomTab tab = getElement();
        PlayerListHud vanilla = deferredVanilla;
        if (tab == null || vanilla == null || !tab.isReplacementEnabled() || mc.currentScreen instanceof ChatScreen) {
            clearDeferredData();
            return;
        }

        context.createNewRootLayer();
        tab.renderStyledTab(
                CustomDrawContext.isolated(context, CustomDrawContext.Pass.POST),
                deferredWidth,
                deferredScoreboard,
                deferredObjective,
                vanilla,
                deferredHeader,
                deferredFooter,
                tab.overlayAnimation.getValue()
        );
        clearDeferredData();
    }

    private static void clearDeferredData() {
        deferredScoreboard = null;
        deferredObjective = null;
        deferredVanilla = null;
        deferredHeader = null;
        deferredFooter = null;
    }

    public static void onVanillaVisibleChanged(boolean visible) {
        CustomTab tab = CustomTab.getElement();
        if (tab != null && !visible) {
            tab.overlayAnimation.setValue(0.0f);
            deferredRender = false;
            clearDeferredData();
        }
    }

    public static boolean shouldReplaceVanilla() {
        CustomTab tab = CustomTab.getElement();
        return tab != null && tab.isReplacementEnabled() && !(mc.currentScreen instanceof ChatScreen);
    }

    private boolean isReplacementEnabled() {
        if (mc.player == null || mc.world == null) {
            return false;
        }
        return Interface.isHudElementEnabled(this.name);
    }

    private static CustomTab getElement() {
        if (FeverVisual.getInstance().getHud() == null) {
            return null;
        }
        return FeverVisual.getInstance().getHud().getElementByName("hud.custom_tab");
    }

    private Layout getLayout(CustomDrawContext context, int scaledWindowWidth, @Nullable Scoreboard scoreboard, @Nullable ScoreboardObjective objective, PlayerListHud vanilla, @Nullable Text header, @Nullable Text footer) {
        int scaledWindowHeight = context.getScaledWindowHeight();
        int playerCount = mc.player == null || mc.player.networkHandler == null ? 0 : mc.player.networkHandler.getListedPlayerListEntries().size();
        long playerLimit = BetterTab.getPlayerLimit(80L);
        int headerHash = header == null ? 0 : header.getString().hashCode();
        int footerHash = footer == null ? 0 : footer.getString().hashCode();
        long now = Util.getMeasuringTimeMs();

        boolean invalid = this.cachedLayout == null
                || now >= this.nextLayoutRefreshAtMs
                || this.cachedScaledWindowWidth != scaledWindowWidth
                || this.cachedScaledWindowHeight != scaledWindowHeight
                || this.cachedPlayerCount != playerCount
                || this.cachedPlayerLimit != playerLimit
                || this.cachedObjective != objective
                || this.cachedHeaderHash != headerHash
                || this.cachedFooterHash != footerHash;

        if (invalid) {
            this.cachedLayout = this.buildLayout(context, scaledWindowWidth, scoreboard, objective, vanilla, header, footer, playerLimit);
            this.cachedScaledWindowWidth = scaledWindowWidth;
            this.cachedScaledWindowHeight = scaledWindowHeight;
            this.cachedPlayerCount = playerCount;
            this.cachedPlayerLimit = playerLimit;
            this.cachedObjective = objective;
            this.cachedHeaderHash = headerHash;
            this.cachedFooterHash = footerHash;
            this.nextLayoutRefreshAtMs = now + LAYOUT_REFRESH_MS;
        }

        return this.cachedLayout;
    }

    private Layout buildLayout(CustomDrawContext context, int scaledWindowWidth, @Nullable Scoreboard scoreboard, @Nullable ScoreboardObjective objective, PlayerListHud vanilla, @Nullable Text header, @Nullable Text footer, long playerLimit) {
        List<PlayerRow> rows = this.collectRows(scoreboard, objective, vanilla, playerLimit);
        boolean heads = this.shouldShowHeads();
        float sideMargin = BetterTab.getSideMargin(50);
        float horizontalWidthLimit = scaledWindowWidth - sideMargin * 2.0f;
        float hardWidthLimit = scaledWindowWidth * MAX_PANEL_SCREEN_SHARE;
        float maxPanelWidth = Math.max(90.0f, Math.min(horizontalWidthLimit, hardWidthLimit));

        int headerWrapWidth = Math.max(80, Math.round(maxPanelWidth - PANEL_PADDING * 2.0f));
        List<WrappedLine> headerLines = this.wrapLines(header, headerWrapWidth);
        List<WrappedLine> footerLines = this.wrapLines(footer, headerWrapWidth);
        float panelY = this.getPanelTop();
        float headerHeight = headerLines.isEmpty() ? 0.0f : HEADER_TOP_OFFSET + (float) headerLines.size() * TEXT_LINE_HEIGHT + 8.0f;
        float footerHeight = footerLines.isEmpty() ? 0.0f : (float) footerLines.size() * TEXT_LINE_HEIGHT + 8.0f;
        int rowsByHeightCap = Math.max(1, (int) ((context.getScaledWindowHeight() * MAX_PANEL_SCREEN_SHARE - headerHeight - footerHeight - PANEL_PADDING * 2.0f) / ROW_HEIGHT));
        int availableRows = Math.max(1, (int) ((float) context.getScaledWindowHeight() - panelY - 14.0f - headerHeight - footerHeight) / (int) ROW_HEIGHT);
        int maxRows = Math.max(1, Math.min(BetterTab.getMaxRows(20), Math.min(availableRows, rowsByHeightCap)));
        int entryCount = Math.max(1, rows.size());
        int rowCount = entryCount;
        int columns = 1;
        while (rowCount > maxRows) {
            rowCount = (entryCount + ++columns - 1) / columns;
        }
        int nameWidth = 68;
        int scoreWidth = 0;
        for (PlayerRow row : rows) {
            nameWidth = Math.max(nameWidth, row.nameWidth());
            if (row.scoreText() != null) {
                scoreWidth = Math.max(scoreWidth, row.scoreWidth());
            }
        }
        float headWidth = heads ? 12.0f : 0.0f;
        float pingWidth = 21.0f;
        float scoreColumn = scoreWidth > 0 ? Math.max(24.0f, (float) scoreWidth + 8.0f) : 0.0f;
        float rawColumnWidth = headWidth + (float) nameWidth + scoreColumn + pingWidth + 12.0f;
        float maxColumnWidth = Math.max(78.0f, (maxPanelWidth - PANEL_PADDING * 2.0f - (float) (columns - 1) * SIDE_GAP) / (float) columns);
        float columnWidth = MathHelper.clamp(rawColumnWidth, 82.0f, maxColumnWidth);
        float listWidth = (float) columns * columnWidth + (float) (columns - 1) * SIDE_GAP;
        int maxWrappedTextWidth = 0;
        for (WrappedLine line : headerLines) {
            maxWrappedTextWidth = Math.max(maxWrappedTextWidth, line.width());
        }
        for (WrappedLine line : footerLines) {
            maxWrappedTextWidth = Math.max(maxWrappedTextWidth, line.width());
        }
        float panelWidth = MathHelper.clamp(Math.max(listWidth + PANEL_PADDING * 2.0f, (float) maxWrappedTextWidth + PANEL_PADDING * 2.0f), 90.0f, maxPanelWidth);
        float panelHeight = headerHeight + (float) rowCount * ROW_HEIGHT + PANEL_PADDING * 2.0f + footerHeight;
        float panelX = (float) scaledWindowWidth / 2.0f - panelWidth / 2.0f;
        float bodyX = panelX + (panelWidth - listWidth) / 2.0f;
        float bodyY = panelY + PANEL_PADDING + headerHeight;
        return new Layout(rows, headerLines, footerLines, heads, panelX, panelY, panelWidth, panelHeight, bodyX, bodyY, columnWidth, rowCount, columns, headWidth, scoreColumn, pingWidth);
    }

    private void renderStyledTab(CustomDrawContext context, int scaledWindowWidth, @Nullable Scoreboard scoreboard, @Nullable ScoreboardObjective objective, PlayerListHud vanilla, @Nullable Text header, @Nullable Text footer, float alpha) {
        Layout layout = this.getLayout(context, scaledWindowWidth, scoreboard, objective, vanilla, header, footer);
        if (layout.rows().isEmpty()) {
            return;
        }
        alpha = MathHelper.clamp(alpha, 0.0f, 1.0f);
        float slide = (1.0f - alpha) * -8.0f;
        float panelY = layout.panelY() + slide;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, Math.min(1.0f, alpha));
        try {
            context.drawShadow(layout.panelX() - 5.0f, panelY - 5.0f, layout.panelWidth() + 10.0f, layout.panelHeight() + 10.0f, 15.0f, BorderRadius.all(6.0f), ColorRGBA.BLACK.withAlpha(55.0f * alpha));
            context.drawClientRect(layout.panelX(), panelY, layout.panelWidth(), layout.panelHeight(), alpha, 0.0f, 7.0f);
            context.getOriginalContext().createNewRootLayer();
            ScissorUtility.push(context.getMatrices(), layout.panelX(), panelY, layout.panelWidth(), layout.panelHeight());
            try {
                this.renderWrappedBlock(context, layout.headerLines(), layout.panelX(), panelY + PANEL_PADDING + (layout.headerLines().isEmpty() ? 0.0f : HEADER_TOP_OFFSET), layout.panelWidth(), alpha);
                this.renderRows(context, layout, layout.bodyY() + slide, alpha);
                float footerY = panelY + layout.panelHeight() - PANEL_PADDING - (float) layout.footerLines().size() * TEXT_LINE_HEIGHT;
                if (!layout.footerLines().isEmpty()) {
                    this.renderWrappedBlock(context, layout.footerLines(), layout.panelX(), footerY, layout.panelWidth(), alpha);
                }
            } finally {
                ScissorUtility.pop();
            }
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private float renderWrappedBlock(CustomDrawContext context, List<WrappedLine> lines, float panelX, float y, float panelWidth, float alpha) {
        for (WrappedLine line : lines) {
            context.drawText(mc.textRenderer, line.text(), Math.round(panelX + panelWidth / 2.0f - (float) line.width() / 2.0f), Math.round(y), ColorRGBA.WHITE.withAlpha(255.0f * alpha).getRGB(), true);
            y += TEXT_LINE_HEIGHT;
        }
        return lines.isEmpty() ? y : y + 2.0f;
    }

    private void renderRows(CustomDrawContext context, Layout layout, float bodyY, float alpha) {
        for (int i = 0; i < layout.rows().size(); ++i) {
            PlayerRow row = layout.rows().get(i);
            int column = i / layout.rowCount();
            int rowIndex = i % layout.rowCount();
            float cellX = layout.bodyX() + (float) column * (layout.columnWidth() + SIDE_GAP);
            float cellY = bodyY + (float) rowIndex * ROW_HEIGHT;
            context.drawRoundedRect(cellX, cellY + 1.0f, layout.columnWidth(), 8.0f, BorderRadius.all(3.0f), Colors.getAdditionalColor().withAlpha((row.entry().getGameMode() == GameMode.SPECTATOR ? 20.0f : 31.0f) * alpha));
        }

        for (int i = 0; i < layout.rows().size(); ++i) {
            PlayerRow row = layout.rows().get(i);
            int column = i / layout.rowCount();
            int rowIndex = i % layout.rowCount();
            float cellX = layout.bodyX() + (float) column * (layout.columnWidth() + SIDE_GAP);
            float cellY = bodyY + (float) rowIndex * ROW_HEIGHT;
            float textX = cellX + 5.0f;
            if (layout.heads()) {
                this.renderHead(context, row, Math.round(textX), Math.round(cellY + 1.0f), alpha);
                textX += layout.headWidth();
            }
            float scoreStart = cellX + layout.columnWidth() - layout.pingWidth() - layout.scoreColumn();
            int nameColor = row.entry().getGameMode() == GameMode.SPECTATOR ? ColorRGBA.WHITE.withAlpha(145.0f * alpha).getRGB() : ColorRGBA.WHITE.withAlpha(255.0f * alpha).getRGB();
            context.drawText(mc.textRenderer, row.name(), Math.round(textX), Math.round(cellY + GuiUtility.getMiddleOfBox(9.0f, ROW_HEIGHT)), nameColor, true);
            if (row.scoreText() != null && layout.scoreColumn() > 0.0f) {
                context.drawText(
                        mc.textRenderer,
                        row.scoreText(),
                        Math.round(scoreStart + layout.scoreColumn() - row.scoreWidth() - 3.0f),
                        Math.round(cellY + GuiUtility.getMiddleOfBox(9.0f, ROW_HEIGHT)),
                        row.hearts() ? HEART_SCORE_COLOR.withAlpha(255.0f * alpha).getRGB() : ColorRGBA.WHITE.withAlpha(205.0f * alpha).getRGB(),
                        true
                );
            }
        }

        for (int i = 0; i < layout.rows().size(); ++i) {
            PlayerRow row = layout.rows().get(i);
            int column = i / layout.rowCount();
            int rowIndex = i % layout.rowCount();
            float cellX = layout.bodyX() + (float) column * (layout.columnWidth() + SIDE_GAP);
            float cellY = bodyY + (float) rowIndex * ROW_HEIGHT;
            this.renderPing(context, cellX + layout.columnWidth() - 16.0f, cellY + 2.0f, row.entry().getLatency(), alpha);
        }
    }

    private void renderHead(CustomDrawContext context, PlayerRow row, int x, int y, float alpha) {
        PlayerSkinDrawer.draw(context.getOriginalContext(), row.entry().getSkinTextures().body().texturePath(), x, y, 8, row.showHat(), row.upsideDown(), ColorRGBA.WHITE.withAlpha(255.0f * alpha).getRGB());
    }

    private void renderPing(CustomDrawContext context, float x, float y, int latency, float alpha) {
        int bars = latency < 0 ? 1 : latency < 150 ? 4 : latency < 300 ? 3 : latency < 600 ? 2 : 1;
        ColorRGBA color = latency < 0
                ? PING_UNKNOWN_COLOR
                : (latency < 150 ? PING_GOOD_COLOR : (latency < 300 ? PING_MEDIUM_COLOR : PING_BAD_COLOR));
        for (int i = 0; i < 4; ++i) {
            float barHeight = 2.0f + (float) i * 1.5f;
            ColorRGBA barColor = i < bars ? color.withAlpha(230.0f * alpha) : Colors.getTextColor().withAlpha(42.0f * alpha);
            context.drawRect(x + (float) i * 3.0f, y + 6.0f - barHeight, 2.0f, barHeight, barColor);
        }
    }

    private List<PlayerRow> collectRows(@Nullable Scoreboard scoreboard, @Nullable ScoreboardObjective objective, PlayerListHud vanilla, long limit) {
        if (mc.player == null || mc.player.networkHandler == null) {
            return List.of();
        }
        List<PlayerListEntry> entries = new ArrayList<>(mc.player.networkHandler.getListedPlayerListEntries());
        entries.sort(ENTRY_ORDERING);
        int rowsSize = (int)Math.min(limit, entries.size());
        List<PlayerRow> rows = new ArrayList<>(rowsSize);
        for (int i = 0; i < rowsSize; i++) {
            PlayerListEntry entry = entries.get(i);
            ScoreInfo score = this.getScore(entry, scoreboard, objective);
            Text name = vanilla.getPlayerName(entry);
            int nameWidth = mc.textRenderer.getWidth(name);
            int scoreWidth = score.text() == null ? 0 : mc.textRenderer.getWidth(score.text());
            rows.add(new PlayerRow(entry, name, nameWidth, score.text(), scoreWidth, score.hearts(), entry.shouldShowHat(), this.isUpsideDown(entry)));
        }
        return rows;
    }

    private List<WrappedLine> wrapLines(@Nullable Text text, int width) {
        if (text == null) {
            return List.of();
        }

        List<OrderedText> ordered = mc.textRenderer.wrapLines(text, width);
        List<WrappedLine> result = new ArrayList<>(ordered.size());
        for (OrderedText line : ordered) {
            result.add(new WrappedLine(line, mc.textRenderer.getWidth(line)));
        }
        return result;
    }

    private boolean isUpsideDown(PlayerListEntry entry) {
        GameProfile profile = entry.getProfile();
        PlayerEntity player = mc.world == null ? null : mc.world.getPlayerByUuid(profile.id());
        return player != null && player.getCustomName() != null
                && ("Dinnerbone".equals(player.getCustomName().getString()) || "Grumm".equals(player.getCustomName().getString()));
    }

    private ScoreInfo getScore(PlayerListEntry entry, @Nullable Scoreboard scoreboard, @Nullable ScoreboardObjective objective) {
        if (scoreboard == null || objective == null || entry.getGameMode() == GameMode.SPECTATOR) {
            return ScoreInfo.EMPTY;
        }
        ReadableScoreboardScore score = scoreboard.getScore(ScoreHolder.fromProfile(entry.getProfile()), objective);
        if (score == null) {
            return ScoreInfo.EMPTY;
        }
        if (objective.getRenderType() == ScoreboardCriterion.RenderType.HEARTS) {
            return new ScoreInfo(Text.literal(this.formatHearts(score.getScore())).formatted(Formatting.RED), true);
        }
        NumberFormat numberFormat = objective.getNumberFormatOr(StyledNumberFormat.YELLOW);
        return new ScoreInfo(ReadableScoreboardScore.getFormattedScore(score, numberFormat), false);
    }

    private String formatHearts(int score) {
        float health = (float) score / 2.0f;
        if (score % 2 == 0) {
            return Integer.toString(score / 2);
        }
        return String.format(Locale.ROOT, "%.1f", health);
    }

    private boolean shouldShowHeads() {
        return mc.isInSingleplayer() || (mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection().isEncrypted());
    }

    private float getPanelTop() {
        if (FeverVisual.getInstance().getHud() == null) {
            return TOP;
        }
        DynamicIsland island = FeverVisual.getInstance().getHud().getIsland();
        if (island == null || !island.isShowing() || !Interface.isHudElementEnabled("hud.dynamic_island")) {
            return TOP;
        }
        float islandY = island.getY() > 0.0f ? island.getY() : 7.0f;
        float islandHeight = Math.max(15.0f, island.getHeight());
        return Math.max(TOP, islandY + islandHeight + 8.0f);
    }

    private record PlayerRow(PlayerListEntry entry, Text name, int nameWidth, @Nullable Text scoreText, int scoreWidth, boolean hearts, boolean showHat, boolean upsideDown) {
    }

    private record ScoreInfo(@Nullable Text text, boolean hearts) {
        private static final ScoreInfo EMPTY = new ScoreInfo(null, false);
    }

    private record WrappedLine(OrderedText text, int width) {
    }

    private record Layout(List<PlayerRow> rows, List<WrappedLine> headerLines, List<WrappedLine> footerLines, boolean heads, float panelX, float panelY, float panelWidth, float panelHeight, float bodyX, float bodyY, float columnWidth, int rowCount, int columns, float headWidth, float scoreColumn, float pingWidth) {
    }
}

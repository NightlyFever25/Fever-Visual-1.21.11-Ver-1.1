package fever.visual.ui.hud.impl;

import fever.visual.FeverVisual;
import fever.visual.framework.base.UIContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.MouseButton;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.modules.modules.visuals.NoRender;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.ui.hud.HudElement;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.game.PlatformUtility;
import fever.visual.utility.gui.GuiUtility;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CustomScoreboard extends HudElement {
    private static final float PANEL_MARGIN = 3.0f;
    private static final int MAX_ENTRIES = 15;
    private static final long LINES_REFRESH_MS = 120L;
    private static final float TITLE_FONT_SIZE = 8.0f;
    private static final float LINE_FONT_SIZE = 7.0f;
    private static final float TITLE_AREA_HEIGHT = 18.0f;
    private static final float LINE_HEIGHT = 10.0f;
    private static final float CONTENT_PADDING = 9.0f;
    private static final Text EMPTY_SCOREBOARD_TEXT = Text.literal("Scoreboard");
    private static final ColorRGBA SCORE_TEXT_COLOR = new ColorRGBA(255.0f, 94.0f, 94.0f);
    private static final Comparator<ScoreboardEntry> ENTRY_ORDER = Comparator
            .comparingInt(ScoreboardEntry::value)
            .reversed()
            .thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER);

    private final BooleanSetting scores = new BooleanSetting(this, "hud.custom_scoreboard.scores").enable();
    private List<ScoreboardLine> lines = List.of();
    private Text title = Text.empty();
    private float titleWidth = 0.0f;
    private float maxContentWidth = 0.0f;
    @Nullable
    private ScoreboardObjective currentObjective;
    @Nullable
    private ScoreboardObjective cachedObjective;
    private boolean cachedScores = true;
    private long nextRefreshAtMs = 0L;
    private boolean manuallyPositioned = false;

    public CustomScoreboard() {
        super("hud.custom_scoreboard", "icons/hud/world.png");
    }

    @Override
    public void update(UIContext context) {
        this.currentObjective = this.getCurrentObjective();
        this.refreshLinesIfNeeded(this.currentObjective);

        float screenWidth = context.getScaledWindowWidth();
        float screenHeight = context.getScaledWindowHeight();
        float maxPanelWidth = Math.max(48.0f, screenWidth - PANEL_MARGIN * 2.0f);
        float minPanelWidth = Math.min(96.0f, maxPanelWidth);
        float maxPanelHeight = Math.max(28.0f, screenHeight - PANEL_MARGIN * 2.0f);
        this.width = MathHelper.clamp((float) this.maxContentWidth + CONTENT_PADDING * 2.0f, minPanelWidth, maxPanelWidth);
        this.height = Math.min(24.0f + (float) Math.max(1, this.lines.size()) * LINE_HEIGHT, maxPanelHeight);

        super.update(context);
        if (this.manuallyPositioned || this.isDragging()) {
            this.clampToScreen(context, PANEL_MARGIN);
        } else {
            this.anchorToVanillaSidebar(context, PANEL_MARGIN);
        }
    }

    @Override
    protected void renderComponent(UIContext context) {
        context.drawClientRect(this.x, this.y, this.width, this.height, this.animation.getValue(), this.dragAnim.getValue(), 7.0f);
        context.enableScissor(Math.round(this.x), Math.round(this.y), Math.round(this.x + this.width), Math.round(this.y + this.height));
        try {
            ColorRGBA textColor = Colors.getTextColor();
            Font titleFont = Fonts.MEDIUM.getFont(TITLE_FONT_SIZE);
            Font lineFont = Fonts.REGULAR.getFont(LINE_FONT_SIZE);
            context.drawRect(this.x, this.y + 18.0f, this.width, 4.0f, Colors.getSeparatorColor());
            float titleX = this.x + this.width / 2.0f - this.titleWidth / 2.0f;
            float titleY = this.y + GuiUtility.getMiddleOfBox(titleFont.height(), TITLE_AREA_HEIGHT);
            context.drawText(titleFont, this.title, titleX, titleY);

            if (this.lines.isEmpty()) {
                context.drawText(lineFont, EMPTY_SCOREBOARD_TEXT.getString(), this.x + 7.0f, this.y + 24.0f, textColor.withAlpha(150.0f));
                return;
            }

            float yOffset = this.y + 24.0f;
            for (ScoreboardLine line : this.lines) {
                context.drawText(lineFont, line.name(), this.x + CONTENT_PADDING, yOffset);
                if (line.score() != null && line.scoreWidth() > 0) {
                    context.drawText(
                            lineFont,
                            line.score().getString(),
                            this.x + this.width - CONTENT_PADDING - line.scoreWidth(),
                            yOffset,
                            SCORE_TEXT_COLOR
                    );
                }
                yOffset += LINE_HEIGHT;
            }
        } finally {
            context.disableScissor();
        }
    }

    @Override
    public boolean show() {
        return !this.isVanillaScoreboardRemoved() && (this.currentObjective != null || mc.currentScreen instanceof ChatScreen);
    }

    private void anchorToVanillaSidebar(UIContext context, float margin) {
        float screenWidth = context.getScaledWindowWidth();
        float screenHeight = context.getScaledWindowHeight();
        this.width = Math.min(this.width, Math.max(48.0f, screenWidth - margin * 2.0f));
        this.height = Math.min(this.height, Math.max(28.0f, screenHeight - margin * 2.0f));
        this.x = MathHelper.clamp(screenWidth - this.width - margin, margin, Math.max(margin, screenWidth - this.width - margin));
        this.y = MathHelper.clamp(screenHeight / 2.0f - this.height / 2.0f, margin, Math.max(margin, screenHeight - this.height - margin));
    }

    private void clampToScreen(UIContext context, float margin) {
        float screenWidth = context.getScaledWindowWidth();
        float screenHeight = context.getScaledWindowHeight();
        this.x = MathHelper.clamp(this.x, margin, Math.max(margin, screenWidth - this.width - margin));
        this.y = MathHelper.clamp(this.y, margin, Math.max(margin, screenHeight - this.height - margin));
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        boolean wasDragging = this.isDragging();
        float startX = this.getStartDragX();
        float startY = this.getStartDragY();
        super.onMouseReleased(mouseX, mouseY, button);
        if (wasDragging && (this.x != startX || this.y != startY)) {
            this.manuallyPositioned = true;
        }
    }

    @Override
    public void setX(float x) {
        super.setX(x);
        this.manuallyPositioned = true;
    }

    @Override
    public void setY(float y) {
        super.setY(y);
        this.manuallyPositioned = true;
    }

    public static boolean shouldReplaceVanilla() {
        if (FeverVisual.getInstance().getHud() == null) {
            return false;
        }
        if (!Interface.isHudElementEnabled("hud.custom_scoreboard")) {
            return false;
        }
        CustomScoreboard scoreboard = FeverVisual.getInstance().getHud().getElementByName("hud.custom_scoreboard");
        if (scoreboard == null || scoreboard.isVanillaScoreboardRemoved()) {
            return false;
        }
        if (PlatformUtility.isLabyMod() || PlatformUtility.isLunarClient()) {
            return scoreboard.currentObjective != null || scoreboard.getCurrentObjective() != null || mc.currentScreen instanceof ChatScreen;
        }
        return scoreboard != null && scoreboard.isShowing() && !scoreboard.isVanillaScoreboardRemoved();
    }

    private void refreshLinesIfNeeded(@Nullable ScoreboardObjective objective) {
        boolean showScores = this.scores.isEnabled();
        long now = Util.getMeasuringTimeMs();
        boolean objectiveChanged = this.cachedObjective != objective;
        boolean scoresModeChanged = this.cachedScores != showScores;

        if (now < this.nextRefreshAtMs && !objectiveChanged && !scoresModeChanged) {
            return;
        }

        this.cachedObjective = objective;
        this.cachedScores = showScores;
        this.nextRefreshAtMs = now + LINES_REFRESH_MS;

        this.title = objective != null ? objective.getDisplayName() : Text.literal(Localizator.translate(this.name));
        Font titleFont = Fonts.MEDIUM.getFont(TITLE_FONT_SIZE);
        this.titleWidth = titleFont.width(this.title.getString());

        if (objective == null) {
            this.lines = List.of();
            this.maxContentWidth = this.titleWidth;
            return;
        }

        this.lines = this.getLines(objective, showScores);
        float maxWidth = this.titleWidth;
        for (ScoreboardLine line : this.lines) {
            maxWidth = Math.max(maxWidth, line.fullWidth());
        }
        this.maxContentWidth = maxWidth;
    }

    private List<ScoreboardLine> getLines(ScoreboardObjective objective, boolean showScores) {
        Scoreboard scoreboard = objective.getScoreboard();

        List<ScoreboardEntry> entries = new ArrayList<>(scoreboard.getScoreboardEntries(objective));
        entries.removeIf(ScoreboardEntry::hidden);
        entries.sort(ENTRY_ORDER);

        int linesCount = Math.min(MAX_ENTRIES, entries.size());
        List<ScoreboardLine> result = new ArrayList<>(linesCount);
        for (int i = 0; i < linesCount; i++) {
            result.add(this.getLine(scoreboard, objective, entries.get(i), showScores));
        }

        return result;
    }

    private ScoreboardLine getLine(Scoreboard scoreboard, ScoreboardObjective objective, ScoreboardEntry entry, boolean showScores) {
        Team team = scoreboard.getScoreHolderTeam(entry.owner());
        Text name = this.buildDisplayName(team, entry);
        Font lineFont = Fonts.REGULAR.getFont(LINE_FONT_SIZE);
        float nameWidth = lineFont.width(name.getString());

        Text score = null;
        float scoreWidth = 0.0f;
        float lineWidth = nameWidth;

        if (showScores && this.shouldRenderScore(objective)) {
            NumberFormat numberFormat = objective.getNumberFormatOr(StyledNumberFormat.RED);
            score = entry.formatted(numberFormat);
            scoreWidth = lineFont.width(score.getString());
            if (scoreWidth > 0) {
                lineWidth += lineFont.width(":") + scoreWidth;
            }
        }

        return new ScoreboardLine(name, score, scoreWidth, lineWidth);
    }

    private Text buildDisplayName(@Nullable Team team, ScoreboardEntry entry) {
        Text entryName = entry.name();
        if (team == null) {
            return entryName;
        }

        Text decorated = Team.decorateName(team, entryName);
        if (!decorated.getString().isBlank()) {
            return decorated;
        }

        MutableText fallback = Text.empty();
        fallback.append(team.getPrefix());
        fallback.append(entryName);
        fallback.append(team.getSuffix());
        return fallback;
    }

    private boolean shouldRenderScore(ScoreboardObjective objective) {
        NumberFormat numberFormat = objective.getNumberFormatOr(StyledNumberFormat.EMPTY);
        return !(numberFormat instanceof StyledNumberFormat);
    }

    private ScoreboardObjective getCurrentObjective() {
        if (mc.world == null || mc.player == null) {
            return null;
        }
        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective objective = null;
        Team team = scoreboard.getScoreHolderTeam(mc.player.getNameForScoreboard());
        if (team != null) {
            ScoreboardDisplaySlot teamSlot = ScoreboardDisplaySlot.fromFormatting(team.getColor());
            if (teamSlot != null) {
                objective = scoreboard.getObjectiveForSlot(teamSlot);
            }
        }
        return objective != null ? objective : scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
    }

    private boolean isVanillaScoreboardRemoved() {
        NoRender noRender = FeverVisual.getInstance().getModuleManager().getModule(NoRender.class);
        return noRender.isEnabled() && noRender.getScoreboard().isSelected();
    }

    private record ScoreboardLine(Text name, @Nullable Text score, float scoreWidth, float fullWidth) {
    }
}

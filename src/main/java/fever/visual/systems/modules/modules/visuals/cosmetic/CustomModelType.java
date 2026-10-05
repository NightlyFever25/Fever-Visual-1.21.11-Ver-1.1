package fever.visual.systems.modules.modules.visuals.cosmetic;
//Да давай пасти бездарность :)
//Made by FeverVisuals (NightlyFever)
import fever.visual.FeverVisual;
import net.minecraft.util.Identifier;

public enum CustomModelType {
    CRAZY_RABBIT("Безумный кролик", FeverVisual.id("custom_models/rabbit.png"), ModelKey.RABBIT),
    WHITE_DEMON("Белый демон", FeverVisual.id("custom_models/whitedemon.png"), ModelKey.DEMON),
    RED_DEMON("Красный демон", FeverVisual.id("custom_models/reddemon.png"), ModelKey.DEMON),
    FREDDY_BEAR("Фредди медведь", FeverVisual.id("custom_models/freddy.png"), ModelKey.FREDDY),
    AMOGUS("Амогус", FeverVisual.id("custom_models/amogus.png"), ModelKey.AMOGUS);

    private final String displayName;
    private final Identifier texture;
    private final ModelKey modelKey;

    CustomModelType(String displayName, Identifier texture, ModelKey modelKey) {
        this.displayName = displayName;
        this.texture = texture;
        this.modelKey = modelKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Identifier getTexture() {
        return texture;
    }

    public ModelKey getModelKey() {
        return modelKey;
    }

    public enum ModelKey {
        RABBIT, DEMON, FREDDY, AMOGUS
    }
}

package com.holdmylua.source.lua_runtime;

import com.holdmylua.source.global.GlobalsStorage;
import com.holdmylua.source.global.LuaContext;
import com.holdmylua.source.patricles.Particle;
import com.holdmylua.source.patricles.ParticleManager;
import com.holdmylua.source.patricles.scripting.Texture;
import com.holdmylua.source.scripting.custom_api.KeyBindManager;
import com.holdmylua.source.scripting.script_wrappers.CameraApi;
import com.holdmylua.source.scripting.script_wrappers.Easings;
import com.holdmylua.source.scripting.script_wrappers.ItemApi;
import com.holdmylua.source.scripting.script_wrappers.JSItems;
import com.holdmylua.source.scripting.script_wrappers.JSTags;
import com.holdmylua.source.scripting.script_wrappers.MatrixApi;
import com.holdmylua.source.scripting.script_wrappers.PlayerApi;
import com.holdmylua.source.scripting.script_wrappers.SoundApi;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public class LuaScriptCache {
    public static int swingSpeed = 9;
    private final Globals globals;
    private final LuaValue chunk;
    private final LuaTable contextTable = LuaBindings.table();
    private boolean canRun = true;
    private static final LuaContext context = new LuaContext();
    private final MatrixApi mInstance = new MatrixApi();
    private final ItemApi iInstance = new ItemApi();
    private final Texture textureInstance = new Texture();
    private final JSItems jsItemsInstance = new JSItems();
    private final JSTags jsTagsInstance = new JSTags();
    private final PlayerApi pInstance = new PlayerApi();
    private final Easings easingsInstance = new Easings();
    private final KeyBindManager keyBindManagerInstance = new KeyBindManager();
    private final SoundApi sInstance = new SoundApi();
    private final CameraApi cInstance = new CameraApi();
    private final ParticleManager particleManagerInstance = new ParticleManager();

    public LuaScriptCache(String sourceCode) throws IOException {
        this.globals = LuaScriptManager.getInstance().sharedGlobals;
        this.globals.set("M", LuaBindings.math(this.mInstance));
        this.globals.set("I", LuaBindings.items(this.iInstance));
        this.globals.set("Texture", LuaBindings.texture(this.textureInstance));
        this.globals.set("Items", LuaBindings.jsItems(this.jsItemsInstance));
        this.globals.set("Tags", LuaBindings.jsTags(this.jsTagsInstance));
        this.globals.set("P", LuaBindings.player(this.pInstance));
        this.globals.set("Easings", LuaBindings.easings(this.easingsInstance));
        this.globals.set("KeyBindManager", LuaBindings.keyBindManager(this.keyBindManagerInstance));
        this.globals.set("S", LuaBindings.sound(this.sInstance));
        this.globals.set("C", LuaBindings.camera(this.cInstance));
        this.globals.set("particleManager", LuaBindings.particleManager(this.particleManagerInstance));
        this.globals.set("swingSpeed", (LuaValue)LuaValue.valueOf((int)swingSpeed));
        this.globals.set("registry", CoerceJavaToLua.coerce(GlobalsStorage.registry));
        this.globals.set("renderAsBlock", CoerceJavaToLua.coerce(GlobalsStorage.renderAsBlock));
        this.globals.set("translateItem", CoerceJavaToLua.coerce(GlobalsStorage.translateItem));
        this.globals.set("itemSwingSpeed", CoerceJavaToLua.coerce(GlobalsStorage.itemSwingSpeed));
        this.globals.set("animator", LuaBindings.animator(GlobalsStorage.modelPartAnimator));
        this.globals.set("renderAsBlock", CoerceJavaToLua.coerce(GlobalsStorage.renderAsBlock));
        this.globals.set("translateItem", CoerceJavaToLua.coerce(GlobalsStorage.translateItem));
        this.globals.set("itemSwingSpeed", CoerceJavaToLua.coerce(GlobalsStorage.itemSwingSpeed));
        this.globals.set("useDuration", CoerceJavaToLua.coerce(GlobalsStorage.useDuration));
        this.globals.set("usingItem", CoerceJavaToLua.coerce(GlobalsStorage.usingItem));
        this.globals.set("debugger", LuaBindings.debugger(GlobalsStorage.debugTextRenderer));
        this.globals.set("applyBlockRotation", CoerceJavaToLua.coerce(GlobalsStorage.applyBlockRotation));
        this.globals.set("context", this.contextTable);
        this.chunk = this.globals.load(sourceCode);
    }

    public void execute(MatrixStack matrices, boolean bl, HashMap<String, Object> registry, float swingProgress, ItemStack item, AbstractClientPlayerEntity player, Hand hand, boolean mainHand, float deltaTime, float equipProgress, float mainHandSwingProgress, float offHandSwingProgress, boolean mainHandSwitchEvent, boolean offHandSwitchEvent, boolean swingMHand, boolean swingOHand, boolean interact, boolean blockBreaking, List<Particle> particles) {
        if (!this.canRun) {
            return;
        }
        try {
            context.update(matrices, bl, swingProgress, item, player, hand, mainHand, deltaTime, equipProgress, mainHandSwingProgress, offHandSwingProgress, mainHandSwitchEvent, offHandSwitchEvent, swingMHand, swingOHand, interact, blockBreaking, particles);
            this.updateContextTable();
            this.globals.set("context", this.contextTable);
            this.globals.set("swingSpeed", LuaValue.valueOf(swingSpeed));
            this.chunk.invoke();
        }
        catch (Exception e) {
            System.err.println("[HoldMyItems] Lua runtime error: " + e.getMessage());
            SystemToast.show((ToastManager)MinecraftClient.getInstance().getToastManager(), (SystemToast.Type)SystemToast.Type.PACK_LOAD_FAILURE, (Text)Text.of((String)"HMI Lua Runtime error!"), (Text)Text.of((String)this.shortLuaError(e.getMessage())));
            this.canRun = false;
        }
    }

    private void updateContextTable() {
        LuaBindings.put(this.contextTable, "matrices", context.matrices);
        LuaBindings.put(this.contextTable, "bl", context.bl);
        LuaBindings.put(this.contextTable, "swingProgress", context.swingProgress);
        LuaBindings.put(this.contextTable, "item", context.item);
        LuaBindings.put(this.contextTable, "player", context.player);
        LuaBindings.put(this.contextTable, "hand", context.hand);
        LuaBindings.put(this.contextTable, "mainHand", context.mainHand);
        LuaBindings.put(this.contextTable, "deltaTime", context.deltaTime);
        LuaBindings.put(this.contextTable, "equipProgress", context.equipProgress);
        LuaBindings.put(this.contextTable, "mainHandSwingProgress", context.mainHandSwingProgress);
        LuaBindings.put(this.contextTable, "offHandSwingProgress", context.offHandSwingProgress);
        LuaBindings.put(this.contextTable, "mainHandSwitchEvent", context.mainHandSwitchEvent);
        LuaBindings.put(this.contextTable, "offHandSwitchEvent", context.offHandSwitchEvent);
        LuaBindings.put(this.contextTable, "swingMHand", context.swingMHand);
        LuaBindings.put(this.contextTable, "swingOHand", context.swingOHand);
        LuaBindings.put(this.contextTable, "interact", context.interact);
        LuaBindings.put(this.contextTable, "blockBreaking", context.blockBreaking);
        LuaBindings.put(this.contextTable, "particles", context.particles);
    }

    private String shortLuaError(String message) {
        if (message == null || message.isBlank()) {
            return "See log for details";
        }

        String[] lines = message.split("\\R");
        String last = lines.length == 0 ? message : lines[lines.length - 1];
        return last.length() > 120 ? last.substring(0, 120) : last;
    }
}

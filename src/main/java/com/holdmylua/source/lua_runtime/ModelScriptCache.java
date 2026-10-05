
package com.holdmylua.source.lua_runtime;

import com.holdmylua.source.LuaTestHMI;
import com.holdmylua.source.global.GlobalsStorage;
import com.holdmylua.source.global.item_model.ItemModelContext;
import com.holdmylua.source.model.ModelPartAnimator;
import com.holdmylua.source.scripting.custom_api.KeyBindManager;
import com.holdmylua.source.scripting.script_wrappers.Easings;
import com.holdmylua.source.scripting.script_wrappers.ItemApi;
import com.holdmylua.source.scripting.script_wrappers.JSItems;
import com.holdmylua.source.scripting.script_wrappers.JSTags;
import com.holdmylua.source.scripting.script_wrappers.MatrixApi;
import com.holdmylua.source.scripting.script_wrappers.PlayerApi;
import java.io.IOException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public class ModelScriptCache {
    private static ItemModelContext data = new ItemModelContext(false, 0.0f, (AbstractClientPlayerEntity)MinecraftClient.getInstance().player, Hand.MAIN_HAND, false, LuaTestHMI.deltaTime, 0.0f, 0.0f, 0.0f, false, false, false, false, false, false, Items.AIR.getDefaultStack());
    private final Globals globals;
    private final LuaValue chunk;
    private final LuaTable dataTable = LuaBindings.table();
    private boolean canRun = true;
    private final MatrixApi mInstance = new MatrixApi();
    private final ItemApi iInstance = new ItemApi();
    private final JSItems jsItemsInstance = new JSItems();
    private final JSTags jsTagsInstance = new JSTags();
    private final PlayerApi pInstance = new PlayerApi();
    private final Easings easingsInstance = new Easings();
    private final KeyBindManager keyBindManagerInstance = new KeyBindManager();

    public ModelScriptCache(String sourceCode) throws IOException {
        this.globals = LuaScriptManager.getInstance().sharedGlobals;
        this.globals.set("M", LuaBindings.math(this.mInstance));
        this.globals.set("I", LuaBindings.items(this.iInstance));
        this.globals.set("Items", LuaBindings.jsItems(this.jsItemsInstance));
        this.globals.set("Tags", LuaBindings.jsTags(this.jsTagsInstance));
        this.globals.set("P", LuaBindings.player(this.pInstance));
        this.globals.set("Easings", LuaBindings.easings(this.easingsInstance));
        this.globals.set("KeyBindManager", LuaBindings.keyBindManager(this.keyBindManagerInstance));
        this.globals.set("registry", CoerceJavaToLua.coerce(GlobalsStorage.registry));
        this.globals.set("animator", LuaBindings.animator(GlobalsStorage.modelPartAnimator));
        this.globals.set("debugger", LuaBindings.debugger(GlobalsStorage.debugTextRenderer));
        this.globals.set("data", this.dataTable);
        this.chunk = this.globals.load(sourceCode);
    }

    public void executeModel(ItemModelContext data, ItemStack itemStack, AbstractClientPlayerEntity player, ModelPartAnimator modelPartAnimator) {
        if (!this.canRun) {
            return;
        }
        try {
            ModelScriptCache.data.set(data);
            this.updateDataTable();
            this.globals.set("data", this.dataTable);
            this.chunk.call();
        }
        catch (Exception e) {
            System.err.println("[HoldMyItems] Lua runtime error: " + e.getMessage());
            SystemToast.show((ToastManager)MinecraftClient.getInstance().getToastManager(), (SystemToast.Type)SystemToast.Type.PACK_LOAD_FAILURE, (Text)Text.of((String)"HMI Lua Runtime error!"), (Text)Text.of((String)this.shortLuaError(e.getMessage())));
            this.canRun = false;
        }
    }

    private void updateDataTable() {
        LuaBindings.put(this.dataTable, "bl", data.bl);
        LuaBindings.put(this.dataTable, "swingProgress", data.swingProgress);
        LuaBindings.put(this.dataTable, "item", data.item);
        LuaBindings.put(this.dataTable, "player", data.player);
        LuaBindings.put(this.dataTable, "hand", data.hand);
        LuaBindings.put(this.dataTable, "mainHand", data.mainHand);
        LuaBindings.put(this.dataTable, "deltaTime", data.deltaTime);
        LuaBindings.put(this.dataTable, "equipProgress", data.equipProgress);
        LuaBindings.put(this.dataTable, "mainHandSwingProgress", data.mainHandSwingProgress);
        LuaBindings.put(this.dataTable, "offHandSwingProgress", data.offHandSwingProgress);
        LuaBindings.put(this.dataTable, "mainHandSwitchEvent", data.mainHandSwitchEvent);
        LuaBindings.put(this.dataTable, "offHandSwitchEvent", data.offHandSwitchEvent);
        LuaBindings.put(this.dataTable, "swingMHand", data.swingMHand);
        LuaBindings.put(this.dataTable, "swingOHand", data.swingOHand);
        LuaBindings.put(this.dataTable, "interact", data.interact);
        LuaBindings.put(this.dataTable, "blockBreaking", data.blockBreaking);
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

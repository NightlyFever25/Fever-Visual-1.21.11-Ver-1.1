package com.holdmylua.source.lua_runtime;

import com.holdmylua.source.data_structures.SpearData;
import com.holdmylua.source.model.ModelPartAnimator;
import com.holdmylua.source.patricles.Particle;
import com.holdmylua.source.patricles.ParticleManager;
import com.holdmylua.source.patricles.scripting.Texture;
import com.holdmylua.source.scripting.custom_api.DebugTextRenderer;
import com.holdmylua.source.scripting.custom_api.KeyBindManager;
import com.holdmylua.source.scripting.script_wrappers.CameraApi;
import com.holdmylua.source.scripting.script_wrappers.Easings;
import com.holdmylua.source.scripting.script_wrappers.ItemApi;
import com.holdmylua.source.scripting.script_wrappers.JSItems;
import com.holdmylua.source.scripting.script_wrappers.JSTags;
import com.holdmylua.source.scripting.script_wrappers.MatrixApi;
import com.holdmylua.source.scripting.script_wrappers.PlayerApi;
import com.holdmylua.source.scripting.script_wrappers.SoundApi;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public final class LuaBindings {
    private LuaBindings() {
    }

    public static LuaTable table() {
        return new LuaTable();
    }

    public static void put(LuaTable table, String key, Object value) {
        table.set(key, CoerceJavaToLua.coerce(value));
    }

    public static void put(LuaTable table, String key, boolean value) {
        table.set(key, LuaValue.valueOf(value));
    }

    public static void put(LuaTable table, String key, double value) {
        table.set(key, LuaValue.valueOf(value));
    }

    public static LuaTable math(MatrixApi api) {
        LuaTable table = table();
        table.set("scale", fn(args -> {
            api.scale(matrix(args, 2), d(args, 3), d(args, 4), d(args, 5));
            return LuaValue.NIL;
        }));
        table.set("push", fn(args -> {
            api.push(matrix(args, 2));
            return LuaValue.NIL;
        }));
        table.set("pop", fn(args -> {
            api.pop(matrix(args, 2));
            return LuaValue.NIL;
        }));
        table.set("moveX", fn(args -> {
            api.moveX(matrix(args, 2), d(args, 3));
            return LuaValue.NIL;
        }));
        table.set("moveY", fn(args -> {
            api.moveY(matrix(args, 2), d(args, 3));
            return LuaValue.NIL;
        }));
        table.set("moveZ", fn(args -> {
            api.moveZ(matrix(args, 2), d(args, 3));
            return LuaValue.NIL;
        }));
        table.set("translate", fn(args -> {
            api.translate(matrix(args, 2), d(args, 3), d(args, 4), d(args, 5));
            return LuaValue.NIL;
        }));
        table.set("rotateX", fn(args -> {
            if (args.narg() >= 6) {
                api.rotateX(matrix(args, 2), d(args, 3), d(args, 4), d(args, 5), d(args, 6));
            } else {
                api.rotateX(matrix(args, 2), d(args, 3));
            }
            return LuaValue.NIL;
        }));
        table.set("rotateY", fn(args -> {
            if (args.narg() >= 6) {
                api.rotateY(matrix(args, 2), d(args, 3), d(args, 4), d(args, 5), d(args, 6));
            } else {
                api.rotateY(matrix(args, 2), d(args, 3));
            }
            return LuaValue.NIL;
        }));
        table.set("rotateZ", fn(args -> {
            if (args.narg() >= 6) {
                api.rotateZ(matrix(args, 2), d(args, 3), d(args, 4), d(args, 5), d(args, 6));
            } else {
                api.rotateZ(matrix(args, 2), d(args, 3));
            }
            return LuaValue.NIL;
        }));
        table.set("sin", fn(args -> LuaValue.valueOf(api.sin(d(args, 2)))));
        table.set("cos", fn(args -> LuaValue.valueOf(api.cos(d(args, 2)))));
        table.set("clamp", fn(args -> LuaValue.valueOf(api.clamp(d(args, 2), d(args, 3), d(args, 4)))));
        table.set("floor", fn(args -> LuaValue.valueOf(api.floor(d(args, 2)))));
        table.set("abs", fn(args -> LuaValue.valueOf(api.abs(d(args, 2)))));
        table.set("lerp", fn(args -> LuaValue.valueOf(api.lerp(d(args, 2), d(args, 3), d(args, 4)))));
        table.set("pow", fn(args -> LuaValue.valueOf(api.pow(d(args, 2), d(args, 3)))));
        table.set("ceil", fn(args -> LuaValue.valueOf(api.ceil(d(args, 2)))));
        table.set("round", fn(args -> LuaValue.valueOf(api.round(d(args, 2)))));
        table.set("shear", fn(args -> {
            api.shear(matrix(args, 2), d(args, 3), d(args, 4), d(args, 5));
            return LuaValue.NIL;
        }));
        table.set("PI", LuaValue.valueOf(api.PI));
        return table;
    }

    public static LuaTable items(ItemApi api) {
        LuaTable table = table();
        table.set("getAttackDamage", fn(args -> LuaValue.valueOf(api.getAttackDamage(itemStack(args, 2)))));
        table.set("isOf", fn(args -> LuaValue.valueOf(api.isOf(itemStack(args, 2), item(args, 3)))));
        table.set("isIn", fn(args -> LuaValue.valueOf(api.isIn(itemStack(args, 2), tag(args, 3)))));
        table.set("isEmpty", fn(args -> LuaValue.valueOf(api.isEmpty(itemStack(args, 2)))));
        table.set("getUseAction", fn(args -> LuaValue.valueOf(api.getUseAction(itemStack(args, 2)))));
        table.set("getName", fn(args -> LuaValue.valueOf(api.getName(itemStack(args, 2)))));
        table.set("getActualName", fn(args -> LuaValue.valueOf(api.getActualName(itemStack(args, 2)))));
        table.set("isChargedCrossbow", fn(args -> LuaValue.valueOf(api.isChargedCrossbow(itemStack(args, 2)))));
        table.set("getDefaultStack", fn(args -> coerce(api.getDefaultStack(item(args, 2)))));
        table.set("isBlock", fn(args -> LuaValue.valueOf(api.isBlock(itemStack(args, 2)))));
        table.set("shouldTranslateItem", fn(args -> LuaValue.valueOf(api.shouldTranslateItem(itemStack(args, 2)))));
        table.set("isCustomTranslate", fn(args -> LuaValue.valueOf(api.isCustomTranslate(itemStack(args, 2)))));
        table.set("setTranslate", fn(args -> {
            api.setTranslate(itemStack(args, 2), args.arg(3).toboolean());
            return LuaValue.NIL;
        }));
        table.set("setRenderAsBlock", fn(args -> {
            api.setRenderAsBlock(itemStack(args, 2), args.arg(3).toboolean());
            return LuaValue.NIL;
        }));
        table.set("shouldRenderAsBlock", fn(args -> LuaValue.valueOf(api.shouldRenderAsBlock(itemStack(args, 2)))));
        table.set("isLantern", fn(args -> LuaValue.valueOf(api.isLantern(itemStack(args, 2)))));
        table.set("isThrowable", fn(args -> LuaValue.valueOf(api.isThrowable(itemStack(args, 2)))));
        table.set("setSwingSpeed", fn(args -> {
            api.setSwingSpeed(itemStack(args, 2), d(args, 3));
            return LuaValue.NIL;
        }));
        table.set("isEnchanted", fn(args -> LuaValue.valueOf(api.isEnchanted(itemStack(args, 2)))));
        table.set("getComponents", fn(args -> coerce(ItemApi.getComponents(itemStack(args, 2)))));
        table.set("copyAppearanceComponents", fn(args -> {
            api.copyAppearanceComponents(itemStack(args, 2));
            return LuaValue.NIL;
        }));
        table.set("setMainStack", fn(args -> {
            api.setMainStack(item(args, 2));
            return LuaValue.NIL;
        }));
        table.set("setOffStack", fn(args -> {
            api.setOffStack(item(args, 2));
            return LuaValue.NIL;
        }));
        table.set("getSpearData", fn(args -> spearData(api.getSpearData(itemStack(args, 2)))));
        return table;
    }

    public static LuaTable player(PlayerApi api) {
        LuaTable table = table();
        table.set("getHealth", fn(args -> LuaValue.valueOf(api.getHealth(player(args, 2)))));
        table.set("isSneaking", fn(args -> LuaValue.valueOf(api.isSneaking(player(args, 2)))));
        table.set("isOnGround", fn(args -> LuaValue.valueOf(api.isOnGround(player(args, 2)))));
        table.set("isSwimming", fn(args -> LuaValue.valueOf(api.isSwimming(player(args, 2)))));
        table.set("isClimbing", fn(args -> LuaValue.valueOf(api.isClimbing(player(args, 2)))));
        table.set("isCrawling", fn(args -> LuaValue.valueOf(api.isCrawling(player(args, 2)))));
        table.set("isSubmergedInWater", fn(args -> LuaValue.valueOf(api.isSubmergedInWater(player(args, 2)))));
        table.set("isTouchingWater", fn(args -> LuaValue.valueOf(api.isTouchingWater(player(args, 2)))));
        table.set("isUsingSpyglass", fn(args -> LuaValue.valueOf(api.isUsingSpyglass(player(args, 2)))));
        table.set("isUsingRiptide", fn(args -> LuaValue.valueOf(api.isUsingRiptide(player(args, 2)))));
        table.set("getX", fn(args -> LuaValue.valueOf(api.getX(player(args, 2)))));
        table.set("getY", fn(args -> LuaValue.valueOf(api.getY(player(args, 2)))));
        table.set("getZ", fn(args -> LuaValue.valueOf(api.getZ(player(args, 2)))));
        table.set("getXSpeed", fn(args -> LuaValue.valueOf(api.getXSpeed(player(args, 2)))));
        table.set("getYSpeed", fn(args -> LuaValue.valueOf(api.getYSpeed(player(args, 2)))));
        table.set("getZSpeed", fn(args -> LuaValue.valueOf(api.getZSpeed(player(args, 2)))));
        table.set("getSpeed", fn(args -> LuaValue.valueOf(api.getSpeed(player(args, 2)))));
        table.set("isUsingItem", fn(args -> LuaValue.valueOf(api.isUsingItem(player(args, 2)))));
        table.set("getYaw", fn(args -> LuaValue.valueOf(api.getYaw(player(args, 2)))));
        table.set("getPitch", fn(args -> LuaValue.valueOf(api.getPitch(player(args, 2)))));
        table.set("getMainItem", fn(args -> coerce(api.getMainItem(player(args, 2)))));
        table.set("getOffhandItem", fn(args -> coerce(api.getOffhandItem(player(args, 2)))));
        table.set("getActiveHand", fn(args -> coerce(api.getActiveHand(player(args, 2)))));
        table.set("getAge", fn(args -> LuaValue.valueOf(api.getAge(player(args, 2)))));
        table.set("isItemCoolingDown", fn(args -> LuaValue.valueOf(api.isItemCoolingDown(itemStack(args, 2), player(args, 3)))));
        table.set("getSwingCount", fn(args -> LuaValue.valueOf(api.getSwingCount(player(args, 2)))));
        table.set("getStandingBlock", fn(args -> LuaValue.valueOf(api.getStandingBlock(player(args, 2)))));
        table.set("getBlockBelow", fn(args -> LuaValue.valueOf(api.getBlockBelow(player(args, 2), args.arg(3).toint()))));
        table.set("getBlockAbove", fn(args -> LuaValue.valueOf(api.getBlockAbove(player(args, 2), args.arg(3).toint()))));
        table.set("hasVehicle", fn(args -> LuaValue.valueOf(api.hasVehicle(player(args, 2), args.arg(3).toint()))));
        return table;
    }

    public static LuaTable easings(Easings api) {
        LuaTable table = table();
        table.set("easeInOutBack", fn(args -> LuaValue.valueOf(api.easeInOutBack(d(args, 2)))));
        table.set("easeInSine", fn(args -> LuaValue.valueOf(api.easeInSine(d(args, 2)))));
        table.set("easeOutSine", fn(args -> LuaValue.valueOf(api.easeOutSine(d(args, 2)))));
        table.set("easeInOutSine", fn(args -> LuaValue.valueOf(api.easeInOutSine(d(args, 2)))));
        table.set("easeInQuad", fn(args -> LuaValue.valueOf(api.easeInQuad(d(args, 2)))));
        table.set("easeOutQuad", fn(args -> LuaValue.valueOf(api.easeOutQuad(d(args, 2)))));
        table.set("easeInOutQuad", fn(args -> LuaValue.valueOf(api.easeInOutQuad(d(args, 2)))));
        table.set("easeInCubic", fn(args -> LuaValue.valueOf(api.easeInCubic(d(args, 2)))));
        table.set("easeOutCubic", fn(args -> LuaValue.valueOf(api.easeOutCubic(d(args, 2)))));
        table.set("easeInOutCubic", fn(args -> LuaValue.valueOf(api.easeInOutCubic(d(args, 2)))));
        table.set("easeInQuart", fn(args -> LuaValue.valueOf(api.easeInQuart(d(args, 2)))));
        table.set("easeOutQuart", fn(args -> LuaValue.valueOf(api.easeOutQuart(d(args, 2)))));
        table.set("easeInOutQuart", fn(args -> LuaValue.valueOf(api.easeInOutQuart(d(args, 2)))));
        table.set("easeInQuint", fn(args -> LuaValue.valueOf(api.easeInQuint(d(args, 2)))));
        table.set("easeOutQuint", fn(args -> LuaValue.valueOf(api.easeOutQuint(d(args, 2)))));
        table.set("easeInOutQuint", fn(args -> LuaValue.valueOf(api.easeInOutQuint(d(args, 2)))));
        table.set("easeInExpo", fn(args -> LuaValue.valueOf(api.easeInExpo(d(args, 2)))));
        table.set("easeOutExpo", fn(args -> LuaValue.valueOf(api.easeOutExpo(d(args, 2)))));
        table.set("easeInOutExpo", fn(args -> LuaValue.valueOf(api.easeInOutExpo(d(args, 2)))));
        table.set("easeInCirc", fn(args -> LuaValue.valueOf(api.easeInCirc(d(args, 2)))));
        table.set("easeOutCirc", fn(args -> LuaValue.valueOf(api.easeOutCirc(d(args, 2)))));
        table.set("easeInOutCirc", fn(args -> LuaValue.valueOf(api.easeInOutCirc(d(args, 2)))));
        table.set("easeInBack", fn(args -> LuaValue.valueOf(api.easeInBack(d(args, 2)))));
        table.set("easeOutBack", fn(args -> LuaValue.valueOf(api.easeOutBack(d(args, 2)))));
        table.set("easeInElastic", fn(args -> LuaValue.valueOf(api.easeInElastic(d(args, 2)))));
        table.set("easeOutElastic", fn(args -> LuaValue.valueOf(api.easeOutElastic(d(args, 2)))));
        table.set("easeInOutElastic", fn(args -> LuaValue.valueOf(api.easeInOutElastic(d(args, 2)))));
        table.set("easeOutBounce", fn(args -> LuaValue.valueOf(api.easeOutBounce(d(args, 2)))));
        table.set("easeInBounce", fn(args -> LuaValue.valueOf(api.easeInBounce(d(args, 2)))));
        table.set("easeInOutBounce", fn(args -> LuaValue.valueOf(api.easeInOutBounce(d(args, 2)))));
        table.set("cubicEase", fn(args -> LuaValue.valueOf(api.cubicEase(d(args, 2)))));
        return table;
    }

    public static LuaTable jsItems(JSItems api) {
        LuaTable table = table();
        table.set("get", fn(args -> coerce(api.get(args.arg(2).checkjstring()))));
        table.set("checkItemName", fn(args -> LuaValue.valueOf(api.checkItemName(itemStack(args, 2)))));
        return table;
    }

    public static LuaTable jsTags(JSTags api) {
        LuaTable table = table();
        table.set("getVanillaTag", fn(args -> coerce(api.getVanillaTag(args.arg(2).checkjstring()))));
        table.set("getFabricTag", fn(args -> coerce(api.getFabricTag(args.arg(2).checkjstring()))));
        return table;
    }

    public static LuaTable texture(Texture api) {
        LuaTable table = table();
        table.set("of", fn(args -> coerce(api.of(args.arg(2).checkjstring(), args.arg(3).checkjstring()))));
        return table;
    }

    public static LuaTable particleManager(ParticleManager api) {
        LuaTable table = table();
        table.set("addParticle", fn(args -> {
            @SuppressWarnings("unchecked")
            ArrayList<Particle> particles = (ArrayList<Particle>) args.arg(2).checkuserdata(ArrayList.class);
            Identifier texture = (Identifier) args.arg(17).checkuserdata(Identifier.class);
            LuaValue optionalFunc = args.narg() >= 24 ? args.arg(24) : LuaValue.NIL;
            if (args.narg() >= 25 && !args.arg(25).isnil()) {
                api.addParticle(particles, args.arg(3).toboolean(), d(args, 4), d(args, 5), d(args, 6), d(args, 7), d(args, 8), d(args, 9), d(args, 10), d(args, 11), d(args, 12), d(args, 13), d(args, 14), d(args, 15), d(args, 16), texture, args.arg(18).checkjstring(), hand(args, 19), args.arg(20).checkjstring(), args.arg(21).checkjstring(), d(args, 22), d(args, 23), optionalFunc, matrix(args, 25));
            } else if (args.narg() >= 24 && optionalFunc.isfunction()) {
                api.addParticle(particles, args.arg(3).toboolean(), d(args, 4), d(args, 5), d(args, 6), d(args, 7), d(args, 8), d(args, 9), d(args, 10), d(args, 11), d(args, 12), d(args, 13), d(args, 14), d(args, 15), d(args, 16), texture, args.arg(18).checkjstring(), hand(args, 19), args.arg(20).checkjstring(), args.arg(21).checkjstring(), d(args, 22), d(args, 23), optionalFunc);
            } else {
                api.addParticle(particles, args.arg(3).toboolean(), d(args, 4), d(args, 5), d(args, 6), d(args, 7), d(args, 8), d(args, 9), d(args, 10), d(args, 11), d(args, 12), d(args, 13), d(args, 14), d(args, 15), d(args, 16), texture, args.arg(18).checkjstring(), hand(args, 19), args.arg(20).checkjstring(), args.arg(21).checkjstring(), d(args, 22), d(args, 23));
            }
            return LuaValue.NIL;
        }));
        return table;
    }

    public static LuaTable animator(ModelPartAnimator api) {
        LuaTable table = table();
        table.set("moveX", fn(args -> {
            api.moveX(args.arg(2).toint(), args.arg(3).toint(), f(args, 4));
            return LuaValue.NIL;
        }));
        table.set("moveY", fn(args -> {
            api.moveY(args.arg(2).toint(), args.arg(3).toint(), f(args, 4));
            return LuaValue.NIL;
        }));
        table.set("moveZ", fn(args -> {
            api.moveZ(args.arg(2).toint(), args.arg(3).toint(), f(args, 4));
            return LuaValue.NIL;
        }));
        table.set("rotateX", fn(args -> {
            if (args.narg() >= 7) {
                api.rotateX(args.arg(2).toint(), args.arg(3).toint(), f(args, 4), f(args, 5), f(args, 6), f(args, 7));
            } else {
                api.rotateX(args.arg(2).toint(), args.arg(3).toint(), f(args, 4));
            }
            return LuaValue.NIL;
        }));
        table.set("rotateY", fn(args -> {
            if (args.narg() >= 7) {
                api.rotateY(args.arg(2).toint(), args.arg(3).toint(), f(args, 4), f(args, 5), f(args, 6), f(args, 7));
            } else {
                api.rotateY(args.arg(2).toint(), args.arg(3).toint(), f(args, 4));
            }
            return LuaValue.NIL;
        }));
        table.set("rotateZ", fn(args -> {
            if (args.narg() >= 7) {
                api.rotateZ(args.arg(2).toint(), args.arg(3).toint(), f(args, 4), f(args, 5), f(args, 6), f(args, 7));
            } else {
                api.rotateZ(args.arg(2).toint(), args.arg(3).toint(), f(args, 4));
            }
            return LuaValue.NIL;
        }));
        table.set("scale", fn(args -> {
            api.scale(args.arg(2).toint(), args.arg(3).toint(), f(args, 4), f(args, 5), f(args, 6));
            return LuaValue.NIL;
        }));
        return table;
    }

    public static LuaTable keyBindManager(KeyBindManager api) {
        LuaTable table = table();
        table.set("isKeyPressed", fn(args -> LuaValue.valueOf(api.isKeyPressed(args.arg(2).toint()))));
        return table;
    }

    public static LuaTable sound(SoundApi api) {
        LuaTable table = table();
        table.set("playSound", fn(args -> {
            api.playSound(args.arg(2).checkjstring(), d(args, 3));
            return LuaValue.NIL;
        }));
        return table;
    }

    public static LuaTable camera(CameraApi api) {
        LuaTable table = table();
        table.set("setCamPos", fn(args -> {
            api.setCamPos(d(args, 2), d(args, 3), d(args, 4));
            return LuaValue.NIL;
        }));
        table.set("setCamRot", fn(args -> {
            api.setCamRot(d(args, 2), d(args, 3), d(args, 4));
            return LuaValue.NIL;
        }));
        return table;
    }

    public static LuaTable debugger(DebugTextRenderer api) {
        LuaTable table = table();
        table.set("out", fn(args -> {
            api.out(args.arg(2).tojstring());
            return LuaValue.NIL;
        }));
        return table;
    }

    private static LuaTable spearData(SpearData data) {
        LuaTable table = table();
        table.set("canDamage", LuaValue.valueOf(data.canDamage));
        table.set("canKnockback", LuaValue.valueOf(data.canKnockback));
        table.set("canDismount", LuaValue.valueOf(data.canDismount));
        table.set("hitImpact", LuaValue.valueOf(data.hitImpact));
        return table;
    }

    private static LuaValue coerce(Object value) {
        return CoerceJavaToLua.coerce(value);
    }

    private static LuaValue fn(LuaCallable callable) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return callable.call(args);
            }
        };
    }

    private static double d(Varargs args, int index) {
        return args.arg(index).todouble();
    }

    private static float f(Varargs args, int index) {
        return (float) args.arg(index).todouble();
    }

    private static MatrixStack matrix(Varargs args, int index) {
        return (MatrixStack) args.arg(index).checkuserdata(MatrixStack.class);
    }

    private static ItemStack itemStack(Varargs args, int index) {
        return (ItemStack) args.arg(index).checkuserdata(ItemStack.class);
    }

    private static Item item(Varargs args, int index) {
        return (Item) args.arg(index).checkuserdata(Item.class);
    }

    @SuppressWarnings("unchecked")
    private static TagKey<Item> tag(Varargs args, int index) {
        return (TagKey<Item>) args.arg(index).checkuserdata(TagKey.class);
    }

    private static AbstractClientPlayerEntity player(Varargs args, int index) {
        return (AbstractClientPlayerEntity) args.arg(index).checkuserdata(AbstractClientPlayerEntity.class);
    }

    private static Hand hand(Varargs args, int index) {
        return (Hand) args.arg(index).checkuserdata(Hand.class);
    }

    private interface LuaCallable {
        LuaValue call(Varargs args);
    }
}

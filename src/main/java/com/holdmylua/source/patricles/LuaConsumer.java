
package com.holdmylua.source.patricles;

import com.holdmylua.source.patricles.Particle;
import java.util.function.Consumer;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

public class LuaConsumer
implements Consumer<Particle> {
    private final LuaFunction function;

    public LuaConsumer(LuaFunction function) {
        this.function = function;
    }

    @Override
    public void accept(Particle particle) {
        try {
            LuaTable table = this.toLuaTable(particle);
            this.function.call(table);
            this.applyLuaTable(particle, table);
        }
        catch (Exception ignored) {
        }
    }

    private LuaTable toLuaTable(Particle particle) {
        LuaTable table = new LuaTable();
        table.set("x", LuaValue.valueOf(particle.x));
        table.set("y", LuaValue.valueOf(particle.y));
        table.set("z", LuaValue.valueOf(particle.z));
        table.set("dx", LuaValue.valueOf(particle.dx));
        table.set("dy", LuaValue.valueOf(particle.dy));
        table.set("dz", LuaValue.valueOf(particle.dz));
        table.set("rx", LuaValue.valueOf(particle.rx));
        table.set("ry", LuaValue.valueOf(particle.ry));
        table.set("rz", LuaValue.valueOf(particle.rz));
        table.set("drx", LuaValue.valueOf(particle.drx));
        table.set("dry", LuaValue.valueOf(particle.dry));
        table.set("drz", LuaValue.valueOf(particle.drz));
        table.set("maxScale", LuaValue.valueOf(particle.maxScale));
        table.set("dead", LuaValue.valueOf(particle.dead));
        return table;
    }

    private void applyLuaTable(Particle particle, LuaTable table) {
        particle.x = number(table, "x", particle.x);
        particle.y = number(table, "y", particle.y);
        particle.z = number(table, "z", particle.z);
        particle.dx = number(table, "dx", particle.dx);
        particle.dy = number(table, "dy", particle.dy);
        particle.dz = number(table, "dz", particle.dz);
        particle.rx = number(table, "rx", particle.rx);
        particle.ry = number(table, "ry", particle.ry);
        particle.rz = number(table, "rz", particle.rz);
        particle.drx = number(table, "drx", particle.drx);
        particle.dry = number(table, "dry", particle.dry);
        particle.drz = number(table, "drz", particle.drz);
        particle.maxScale = number(table, "maxScale", particle.maxScale);
        LuaValue dead = table.get("dead");
        if (!dead.isnil()) {
            particle.dead = dead.toboolean();
        }
    }

    private double number(LuaTable table, String key, double fallback) {
        LuaValue value = table.get(key);
        return value.isnumber() ? value.todouble() : fallback;
    }
}

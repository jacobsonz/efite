package dev.eflite;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StaminaManager {
    private final EpicLitePlugin plugin;
    private final Map<UUID, PlayerData> data = new HashMap<>();

    StaminaManager(EpicLitePlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    PlayerData get(Player p) {
        return data.computeIfAbsent(p.getUniqueId(), k -> {
            PlayerData d = new PlayerData();
            d.stamina = max();
            return d;
        });
    }

    void remove(Player p) { data.remove(p.getUniqueId()); }

    double max() { return plugin.getConfig().getDouble("stamina.max", 100.0); }

    boolean spend(Player p, double cost) {
        PlayerData d = get(p);
        if (d.stamina < cost) return false;
        d.stamina -= cost;
        d.lastSpend = System.currentTimeMillis();
        return true;
    }

    void set(Player p, double value) {
        get(p).stamina = Math.max(0, Math.min(max(), value));
    }

    private void tick() {
        long now = System.currentTimeMillis();
        double regen = plugin.getConfig().getDouble("stamina.regen-per-second", 18.0) / 20.0;
        long delay = plugin.getConfig().getLong("stamina.regen-delay-ms", 900);
        boolean bar = plugin.getConfig().getBoolean("stamina.show-actionbar", true);

        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData d = get(p);

            // movement direction tracking (used to pick the dodge direction)
            Vector cur = p.getLocation().toVector();
            if (d.lastPos != null) {
                Vector delta = cur.clone().subtract(d.lastPos).setY(0);
                if (delta.lengthSquared() > 0.03 * 0.03) {
                    d.moveDir = delta.normalize();
                    d.moveDirAt = now;
                }
            }
            d.lastPos = cur;

            if (now - d.lastSpend >= delay && d.stamina < max()) {
                d.stamina = Math.min(max(), d.stamina + regen);
            }

            boolean recent = now - d.lastSpend < 2500;
            if (bar && (recent || d.stamina < max() - 0.5) && now - d.lastBar >= 150) {
                d.lastBar = now;
                p.sendActionBar(render(d.stamina));
            }
        }
    }

    private Component render(double stamina) {
        int cells = 20;
        int filled = (int) Math.round(stamina / max() * cells);
        double frac = stamina / max();
        NamedTextColor c = frac > 0.5 ? NamedTextColor.GREEN : frac > 0.25 ? NamedTextColor.YELLOW : NamedTextColor.RED;
        return Component.text("Stamina ", NamedTextColor.GRAY)
                .append(Component.text("|".repeat(filled), c))
                .append(Component.text("|".repeat(cells - filled), NamedTextColor.DARK_GRAY));
    }
}

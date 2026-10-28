package dev.eflite;

import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

import java.util.List;

public final class CombatListener implements Listener {
    private final EpicLitePlugin plugin;

    CombatListener(EpicLitePlugin plugin) {
        this.plugin = plugin;
    }

    // ---- melee: stamina cost, combo chain, finisher -----------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || e.getCause() != DamageCause.ENTITY_ATTACK) return;
        PlayerData d = plugin.stamina().get(p);
        if (d.internalDamage) return;

        WeaponType type = WeaponType.of(p.getInventory().getItemInMainHand());
        if (type == null) return;

        double cost = plugin.getConfig().getDouble("attack-cost." + type.name(), 8.0);
        if (!plugin.stamina().spend(p, cost)) {
            // exhausted: weak, unfocused hit and the combo breaks
            e.setDamage(e.getDamage() * plugin.getConfig().getDouble("stamina.exhausted-damage-multiplier", 0.4));
            d.combo = 0;
            d.lastHit = 0;
            return;
        }

        long now = System.currentTimeMillis();
        long window = plugin.getConfig().getLong("combo.window-ms", 900);
        d.combo = (d.lastHit != 0 && now - d.lastHit <= window) ? (d.combo + 1) % 4 : 0;
        d.lastHit = now;

        List<Double> mult = plugin.getConfig().getDoubleList("combo.damage-multipliers");
        if (d.combo < mult.size()) e.setDamage(e.getDamage() * mult.get(d.combo));

        plugin.animation().slash(p, d.combo);

        if (d.combo == 3 && e.getEntity() instanceof LivingEntity victim) {
            Vector away = victim.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
            if (away.lengthSquared() > 1e-6) {
                double kb = plugin.getConfig().getDouble("combo.finisher-knockback", 0.9);
                victim.setVelocity(victim.getVelocity().add(away.normalize().multiply(kb).setY(0.3)));
            }
            plugin.animation().burst(victim.getLocation().add(0, 1, 0), Particle.CRIT, 25, 0.4);
        }
    }

    // ---- dodge: double-tap sneak -----------------------------------------------------------------------

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking() || !plugin.getConfig().getBoolean("dodge.enabled", true)) return;
        Player p = e.getPlayer();
        PlayerData d = plugin.stamina().get(p);
        long now = System.currentTimeMillis();
        long tap = plugin.getConfig().getLong("dodge.double-tap-ms", 300);

        if (d.lastSneak != 0 && now - d.lastSneak <= tap) {
            d.lastSneak = 0;
            dodge(p, d, now);
        } else {
            d.lastSneak = now;
        }
    }

    private void dodge(Player p, PlayerData d, long now) {
        if (now < d.dodgeReadyAt || p.isFlying() || p.isGliding()) return;
        if (!plugin.stamina().spend(p, plugin.getConfig().getDouble("dodge.cost", 22))) return;

        d.dodgeReadyAt = now + plugin.getConfig().getLong("dodge.cooldown-ms", 700);
        d.invulnUntil = now + plugin.getConfig().getInt("dodge.invulnerable-ticks", 10) * 50L;

        Vector dir;
        if (now - d.moveDirAt < 400 && d.moveDir.lengthSquared() > 0) {
            dir = d.moveDir.clone(); // dodge the way you were moving
        } else {
            dir = p.getEyeLocation().getDirection().setY(0); // standing still: backstep
            if (dir.lengthSquared() < 1e-6) dir = new Vector(0, 0, 1);
            dir.normalize().multiply(-1);
        }
        double speed = plugin.getConfig().getDouble("dodge.speed", 0.95);
        p.setVelocity(dir.multiply(speed).setY(0.18));
        plugin.animation().burst(p.getLocation().add(0, 0.2, 0), Particle.CLOUD, 12, 0.3);
        plugin.animation().sound(p.getLocation(), "minecraft:entity.phantom.flap", 0.8f, 1.4f);
    }

    /** i-frames: ignore incoming damage while dodging. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamaged(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        PlayerData d = plugin.stamina().get(p);
        if (System.currentTimeMillis() >= d.invulnUntil) return;
        switch (e.getCause()) {
            case VOID, KILL, SUICIDE, WORLD_BORDER -> { }
            default -> e.setCancelled(true);
        }
    }

    // ---- skills: swap-hand key (F) ---------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (!plugin.getConfig().getBoolean("skills.enabled", true)) return;
        WeaponType type = WeaponType.of(e.getPlayer().getInventory().getItemInMainHand());
        if (type == null) return;
        e.setCancelled(true);
        plugin.skills().cast(e.getPlayer(), type);
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent e) {
        PlayerData d = plugin.stamina().get(e.getPlayer());
        d.combo = 0;
        d.lastHit = 0;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.stamina().remove(e.getPlayer());
    }
}

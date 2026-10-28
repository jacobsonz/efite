package dev.eflite;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Active weapon skills, cast with the swap-hand key. */
public final class SkillManager {
    private final EpicLitePlugin plugin;

    SkillManager(EpicLitePlugin plugin) { this.plugin = plugin; }

    public void cast(Player p, WeaponType type) {
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("skills." + type.name());
        if (cfg == null) return;
        PlayerData d = plugin.stamina().get(p);
        long now = System.currentTimeMillis();

        long ready = d.skillReadyAt.getOrDefault(type, 0L);
        if (now < ready) {
            p.sendActionBar(Component.text("Skill ready in " + String.format("%.1f", (ready - now) / 1000.0) + "s", NamedTextColor.GRAY));
            return;
        }
        if (!plugin.stamina().spend(p, cfg.getDouble("cost", 30))) {
            p.sendActionBar(Component.text("Not enough stamina", NamedTextColor.RED));
            return;
        }
        d.skillReadyAt.put(type, now + cfg.getLong("cooldown-ms", 6000));

        double base = baseDamage(p) * cfg.getDouble("damage-multiplier", 1.5);
        switch (type) {
            case SWORD -> crescentSlash(p, cfg, base);
            case AXE -> earthsplitter(p, cfg, base);
            case SPEAR -> lunge(p, cfg, base);
            case MACE -> seismicSlam(p, cfg, base);
        }
    }

    // ---- skills -----------------------------------------------------------------------------------------

    private void crescentSlash(Player p, ConfigurationSection cfg, double dmg) {
        double range = cfg.getDouble("range", 4.5);
        double cos = Math.cos(Math.toRadians(cfg.getDouble("angle-degrees", 140) / 2.0));
        Vector fwd = p.getEyeLocation().getDirection().setY(0).normalize();
        plugin.animation().slash(p, 3);
        for (LivingEntity v : p.getWorld().getNearbyLivingEntities(p.getLocation(), range)) {
            if (!valid(p, v)) continue;
            Vector to = v.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
            if (to.lengthSquared() < 1e-6 || to.normalize().dot(fwd) < cos) continue;
            hit(p, v, dmg);
            v.setVelocity(v.getVelocity().add(to.multiply(0.5).setY(0.2)));
        }
    }

    private void earthsplitter(Player p, ConfigurationSection cfg, double dmg) {
        double radius = cfg.getDouble("radius", 3.5);
        Vector fwd = p.getEyeLocation().getDirection().setY(0).normalize();
        Location center = p.getLocation().add(fwd.clone().multiply(2.0));
        plugin.animation().slash(p, 2);
        center.getWorld().spawnParticle(Particle.BLOCK, center, 60, radius / 2, 0.2, radius / 2, 0.1, Material.DIRT.createBlockData());
        plugin.animation().sound(center, "minecraft:entity.generic.explode", 0.7f, 1.3f);
        for (LivingEntity v : center.getWorld().getNearbyLivingEntities(center, radius)) {
            if (!valid(p, v)) continue;
            hit(p, v, dmg);
            v.setVelocity(v.getVelocity().add(new Vector(0, 0.6, 0)));
        }
    }

    private void lunge(Player p, ConfigurationSection cfg, double dmg) {
        double distance = cfg.getDouble("distance", 7.0);
        Vector dir = p.getEyeLocation().getDirection().setY(0).normalize();
        PlayerData d = plugin.stamina().get(p);
        d.invulnUntil = Math.max(d.invulnUntil, System.currentTimeMillis() + 300);
        plugin.animation().slash(p, 3);
        Set<UUID> hitSet = new HashSet<>();
        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (!p.isOnline() || p.isDead() || ticks++ >= 8) { cancel(); return; }
                p.setVelocity(dir.clone().multiply(distance / 8.0 * 0.85).setY(0.04));
                p.getWorld().spawnParticle(Particle.CRIT, p.getLocation().add(0, 1, 0), 4, 0.2, 0.3, 0.2, 0.05);
                for (LivingEntity v : p.getWorld().getNearbyLivingEntities(p.getLocation().add(0, 1, 0), 1.7)) {
                    if (!valid(p, v) || !hitSet.add(v.getUniqueId())) continue;
                    hit(p, v, dmg);
                    v.setVelocity(v.getVelocity().add(dir.clone().multiply(0.6).setY(0.25)));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void seismicSlam(Player p, ConfigurationSection cfg, double dmg) {
        double radius = cfg.getDouble("radius", 4.5);
        Location c = p.getLocation();
        plugin.animation().slash(p, 2);
        c.getWorld().spawnParticle(Particle.EXPLOSION, c, 3, radius / 3, 0.1, radius / 3, 0.0);
        c.getWorld().spawnParticle(Particle.BLOCK, c, 80, radius / 2, 0.2, radius / 2, 0.1, Material.STONE.createBlockData());
        plugin.animation().sound(c, "minecraft:entity.generic.explode", 1.0f, 0.8f);
        for (LivingEntity v : c.getWorld().getNearbyLivingEntities(c, radius)) {
            if (!valid(p, v)) continue;
            Vector out = v.getLocation().toVector().subtract(c.toVector()).setY(0);
            if (out.lengthSquared() < 1e-6) out = new Vector(0, 0, 1);
            hit(p, v, dmg);
            v.setVelocity(v.getVelocity().add(out.normalize().multiply(0.9).setY(0.45)));
        }
    }

    // ---- helpers ----------------------------------------------------------------------------------------

    private double baseDamage(Player p) {
        AttributeInstance a = p.getAttribute(Attribute.ATTACK_DAMAGE);
        return a == null ? 1.0 : a.getValue();
    }

    private boolean valid(Player p, LivingEntity v) {
        if (v.equals(p) || v.isDead() || !v.isValid() || v instanceof ArmorStand) return false;
        return !(v instanceof Tameable t && p.equals(t.getOwner()));
    }

    /** Applies damage through the normal event pipeline (so claim/PvP plugins can veto it) without re-entering the combo handler. */
    private void hit(Player p, LivingEntity v, double dmg) {
        PlayerData d = plugin.stamina().get(p);
        d.internalDamage = true;
        try {
            v.damage(dmg, p);
        } finally {
            d.internalDamage = false;
        }
    }
}

package dev.eflite;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Built-in "animation" layer: a flat slash-arc model (from the resource pack) shown on an ItemDisplay
 * that is rotated with client-side interpolation, plus particles, sounds and the vanilla arm swing.
 * Everything here is visible to other players and needs no client mod.
 *
 * This is deliberately a small self-contained backend. A skeletal-animation backend (BetterModel,
 * ModelEngine, ...) could be plugged in by replacing slash() with calls into that library.
 */
public final class AnimationManager {
    static final String TAG = "eflite_fx";
    private final EpicLitePlugin plugin;

    AnimationManager(EpicLitePlugin plugin) { this.plugin = plugin; }

    private ItemStack arcItem() {
        String mat = plugin.getConfig().getString("animation.base-material", "PAPER");
        Material m = Material.matchMaterial(mat);
        ItemStack it = new ItemStack(m == null ? Material.PAPER : m);
        ItemMeta meta = it.getItemMeta();
        NamespacedKey key = NamespacedKey.fromString(plugin.getConfig().getString("animation.item-model", "eflite:slash_arc"));
        if (key != null) meta.setItemModel(key);
        it.setItemMeta(meta);
        return it;
    }

    /**
     * Plays a combo slash. step 0 = left-to-right, 1 = right-to-left, 2 = overhead chop, 3 = wide finisher.
     */
    public void slash(Player p, int step) {
        p.swingMainHand();
        World w = p.getWorld();
        Location loc = p.getLocation();
        w.playSound(loc, step == 3 ? "minecraft:entity.player.attack.strong" : "minecraft:entity.player.attack.sweep",
                SoundCategory.PLAYERS, 1.0f, 0.9f + 0.1f * step);

        if (!plugin.getConfig().getBoolean("animation.enabled", true)) return;

        double yawOffset = plugin.getConfig().getDouble("animation.yaw-offset", 0.0);
        float phi = (float) Math.toRadians(loc.getYaw() + yawOffset);
        float base = (float) Math.PI - phi;            // rotates the model forward (-Z) onto the player facing
        float scale = step == 3 ? 5.4f : 4.2f;

        Quaternionf start;
        Quaternionf end;
        if (step == 2) {
            // vertical chop: crescent swings from high-forward to low-forward
            start = new Quaternionf().rotationY(base).rotateX((float) Math.toRadians(70));
            end = new Quaternionf().rotationY(base).rotateX((float) Math.toRadians(-55));
        } else {
            float sweep = (float) Math.toRadians(step == 3 ? 115 : 75);
            float dir = step == 1 ? -1f : 1f;
            start = new Quaternionf().rotationY(base - dir * sweep);
            end = new Quaternionf().rotationY(base + dir * sweep);
        }

        Location at = loc.clone().add(0, 1.0, 0);
        at.setYaw(0);
        at.setPitch(0);

        ItemDisplay d = w.spawn(at, ItemDisplay.class, e -> {
            e.setItemStack(arcItem());
            e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            e.setBillboard(Display.Billboard.FIXED);
            e.setBrightness(new Display.Brightness(15, 15));
            e.setViewRange((float) plugin.getConfig().getDouble("animation.view-range", 1.0));
            e.setPersistent(false);
            e.addScoreboardTag(TAG);
            e.setTransformation(new Transformation(new Vector3f(0, 0, 0), start, new Vector3f(scale), new Quaternionf()));
        });

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!d.isValid()) return;
            d.setInterpolationDelay(0);
            d.setInterpolationDuration(4);
            d.setTransformation(new Transformation(new Vector3f(0, 0, 0), end, new Vector3f(scale), new Quaternionf()));
        }, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (d.isValid()) d.remove(); }, 7L);

        if (step == 3) {
            w.spawnParticle(Particle.CRIT, loc.clone().add(loc.getDirection().multiply(1.5)).add(0, 1.0, 0), 18, 0.6, 0.4, 0.6, 0.2);
        }
    }

    public void burst(Location at, Particle particle, int count, double spread) {
        at.getWorld().spawnParticle(particle, at, count, spread, 0.2, spread, 0.02);
    }

    public void sound(Location at, String key, float volume, float pitch) {
        at.getWorld().playSound(at, key, SoundCategory.PLAYERS, volume, pitch);
    }

    /** Removes any leftover effect entities (called on disable). */
    public void cleanup() {
        for (World w : Bukkit.getWorlds()) {
            for (ItemDisplay d : w.getEntitiesByClass(ItemDisplay.class)) {
                if (d.getScoreboardTags().contains(TAG)) d.remove();
            }
        }
    }
}

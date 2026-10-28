package dev.eflite;

import org.bukkit.inventory.ItemStack;

/** Weapon categories, detected by material name so this compiles regardless of which weapon materials exist. */
public enum WeaponType {
    SWORD, AXE, SPEAR, MACE;

    public static WeaponType of(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        String n = item.getType().name();
        if (n.endsWith("_SWORD")) return SWORD;
        if (n.endsWith("_AXE")) return AXE;
        if (n.endsWith("_SPEAR") || n.equals("TRIDENT")) return SPEAR;
        if (n.equals("MACE")) return MACE;
        return null;
    }
}

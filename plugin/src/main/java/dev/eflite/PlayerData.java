package dev.eflite;

import org.bukkit.util.Vector;

import java.util.EnumMap;
import java.util.Map;

/** Per-player runtime state. Only touched from the main thread. */
final class PlayerData {
    double stamina;
    long lastSpend;
    long lastSneak;
    long dodgeReadyAt;
    long invulnUntil;
    int combo;
    long lastHit;
    boolean internalDamage;        // true while the plugin itself is applying damage
    Vector lastPos;
    Vector moveDir = new Vector();
    long moveDirAt;
    long lastBar;
    final Map<WeaponType, Long> skillReadyAt = new EnumMap<>(WeaponType.class);
}

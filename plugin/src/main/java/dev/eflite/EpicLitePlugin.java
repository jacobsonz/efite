package dev.eflite;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class EpicLitePlugin extends JavaPlugin {
    private StaminaManager stamina;
    private AnimationManager animation;
    private SkillManager skills;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        stamina = new StaminaManager(this);
        animation = new AnimationManager(this);
        skills = new SkillManager(this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
    }

    @Override
    public void onDisable() {
        if (animation != null) animation.cleanup();
    }

    StaminaManager stamina() { return stamina; }
    AnimationManager animation() { return animation; }
    SkillManager skills() { return skills; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("/eflite reload | stamina <player> <value>", NamedTextColor.GRAY));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                reloadConfig();
                sender.sendMessage(Component.text("EpicLite config reloaded.", NamedTextColor.GREEN));
            }
            case "stamina" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /eflite stamina <player> <value>", NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                    return true;
                }
                try {
                    stamina.set(target, Double.parseDouble(args[2]));
                    sender.sendMessage(Component.text("Stamina set.", NamedTextColor.GREEN));
                } catch (NumberFormatException ex) {
                    sender.sendMessage(Component.text("Value must be a number.", NamedTextColor.RED));
                }
            }
            default -> sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return List.of("reload", "stamina");
        return List.of();
    }
}

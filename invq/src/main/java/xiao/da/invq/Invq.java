package xiao.da.invq;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Invq extends JavaPlugin implements Listener {

    private Map<UUID, Long> blockedPlayers = new HashMap<>();
    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info(config.getString("plugin-enabled-message"));
    }

    @Override
    public void onDisable() {
        getLogger().info(config.getString("plugin-disabled-message"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("invq")) {
            if (sender instanceof Player && sender.hasPermission("invq.use")) {
                if (args.length != 1) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("usage-message")));
                    return true;
                }

                Player targetPlayer = Bukkit.getPlayer(args[0]);
                if (targetPlayer == null) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("player-not-found-message")));
                    return true;
                }

                ItemStack[] items = targetPlayer.getInventory().getContents();
                for (ItemStack item : items) {
                    if (item != null) {
                        Item droppedItem = targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), item);
                    }
                }
                targetPlayer.getInventory().clear();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("items-dropped-message").replace("{player}", targetPlayer.getName())));

                blockedPlayers.put(targetPlayer.getUniqueId(), System.currentTimeMillis() + 1000);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        blockedPlayers.remove(targetPlayer.getUniqueId());
                    }
                }.runTaskLater(this, 20); // 20 ticks = 1 second

                return true;
            } else if (sender instanceof ConsoleCommandSender) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("console-message")));
                return true;
            } else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("no-permission-message")));
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (blockedPlayers.containsKey(player.getUniqueId())) {
            if (System.currentTimeMillis() < blockedPlayers.get(player.getUniqueId())) {
                event.setCancelled(true);
            } else {
                blockedPlayers.remove(player.getUniqueId());
            }
        }
    }
}

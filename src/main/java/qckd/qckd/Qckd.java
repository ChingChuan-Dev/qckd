package qckd.qckd;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Qckd extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private boolean isTracking = false;
    private final Map<UUID, PlayerStats> statsMap = new HashMap<>();

    private static class PlayerStats {
        int kills = 0;
        int deaths = 0;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (getCommand("qckd") != null) {
            getCommand("qckd").setExecutor(this);
            getCommand("qckd").setTabCompleter(this);
        }
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("QCKD 插件已加载");
    }

    @Override
    public void onDisable() {
        getLogger().info("QCKD 插件已卸载");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!getConfig().getBoolean("enable-plugin", true)) return;
        if (!isTracking) return;

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        statsMap.putIfAbsent(victim.getUniqueId(), new PlayerStats());
        statsMap.get(victim.getUniqueId()).deaths++;

        if (killer != null) {
            statsMap.putIfAbsent(killer.getUniqueId(), new PlayerStats());
            statsMap.get(killer.getUniqueId()).kills++;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!getConfig().getBoolean("enable-plugin", true) && (args.length > 0 && !args[0].equalsIgnoreCase("reload"))) {
            sender.sendMessage(msg("plugin-disabled"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!checkPerm(sender, "qckd.admin")) return true;
            reloadConfig();
            sender.sendMessage(msg("reload-success"));
            return true;
        }

        if (args[0].equalsIgnoreCase("statistics")) {
            if (!checkPerm(sender, "qckd.admin")) return true;
            if (args.length < 2) {
                sender.sendMessage(msg("prefix") + "&c用法: /qckd statistics <start|stop>");
                return true;
            }
            if (args[1].equalsIgnoreCase("start")) {
                if (isTracking) {
                    sender.sendMessage(msg("stats-already-running"));
                    return true;
                }
                startStatistics(sender);
            } else if (args[1].equalsIgnoreCase("stop")) {
                if (!isTracking) {
                    sender.sendMessage(msg("stats-not-running"));
                    return true;
                }
                stopStatistics(sender);
            } else {
                sender.sendMessage(msg("prefix") + "&c未知参数: " + args[1]);
            }
            return true;
        }
        sendHelp(sender);
        return true;
    }

    private void startStatistics(CommandSender sender) {
        isTracking = true;
        statsMap.clear();
        Bukkit.broadcastMessage(msg("stats-started"));
    }

    private void stopStatistics(CommandSender sender) {
        isTracking = false;
        Bukkit.broadcastMessage(msg("stats-stopped"));

        if (statsMap.isEmpty()) {
            sender.sendMessage(msg("prefix") + "&7本次无数据，未生成表格。");
            return;
        }


        saveToExcel(sender);
    }

    private void saveToExcel(CommandSender sender) {
        try {
            File reportDir = new File(getDataFolder(), "reports");
            if (!reportDir.exists()) {
                reportDir.mkdirs();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String fileName = "stats_" + sdf.format(new Date()) + ".csv";
            File file = new File(reportDir, fileName);

            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                writer.write('\ufeff');

                writer.println("玩家名,击杀数,死亡数,KD值");

                for (Map.Entry<UUID, PlayerStats> entry : statsMap.entrySet()) {
                    String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    if (name == null) name = "Unknown";

                    PlayerStats s = entry.getValue();
                    double kd = s.deaths == 0 ? s.kills : (double) s.kills / s.deaths;

                    writer.println(String.format("%s,%d,%d,%.2f", name, s.kills, s.deaths, kd));
                }
            }

            sender.sendMessage(msg("prefix") + "&a表格已导出至: &f" + file.getPath());

        } catch (Exception e) {
            sender.sendMessage(msg("prefix") + "&c导出表格失败，请查看后台报错。");
            e.printStackTrace();
        }
    }


    private void sendHelp(CommandSender sender) {
        sender.sendMessage(msg("help-header"));
        sender.sendMessage(ChatColor.GRAY + "/qckd help - 查看帮助");
        sender.sendMessage(ChatColor.GRAY + "/qckd statistics start - 开始统计");
        sender.sendMessage(ChatColor.GRAY + "/qckd statistics stop - 结束并导出表格");
    }

    private boolean checkPerm(CommandSender sender, String perm) {
        if (!sender.hasPermission(perm)) {
            sender.sendMessage(msg("no-permission"));
            return false;
        }
        return true;
    }

    private String msg(String key) {
        String prefix = getConfig().getString("messages.prefix", "");
        String text = getConfig().getString("messages." + key, "");
        if (text == null || text.isEmpty()) return key.equals("prefix") ? "" : key;
        return ChatColor.translateAlternateColorCodes('&', key.equals("prefix") ? text : prefix + text);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(Arrays.asList("help", "statistics", "reload"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("statistics")) return filter(Arrays.asList("start", "stop"), args[1]);
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String input) {
        return list.stream().filter(s -> s.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}

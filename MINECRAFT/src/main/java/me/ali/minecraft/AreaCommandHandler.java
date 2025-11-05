package me.ali.minecraft;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AreaCommandHandler implements CommandExecutor {
    private final Main plugin;
    
    public AreaCommandHandler(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        plugin.getLogger().info("AreaCommandHandler called with args: " + String.join(", ", args));
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("هذا الكوماند للاعبين فقط!", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            showHelp(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage(Component.text("الاستخدام: /area create <iron|gold|emerald|diamond>", NamedTextColor.RED));
                    return true;
                }
                handleCreateArea(player, args[1]);
                break;
                
            case "pos1":
                handlePos1(player);
                break;
                
            case "pos2":
                handlePos2(player);
                break;
                
            case "set":
                if (args.length < 3) {
                    player.sendMessage(Component.text("الاستخدام: /area set <اسم المنطقة> <iron|gold|emerald|diamond>", NamedTextColor.RED));
                    return true;
                }
                handleSetArea(player, args[1], args[2]);
                break;
                
            case "list":
                handleListAreas(player);
                break;
                
            case "delete":
                if (args.length < 2) {
                    player.sendMessage(Component.text("الاستخدام: /area delete <اسم المنطقة>", NamedTextColor.RED));
                    return true;
                }
                handleDeleteArea(player, args[1]);
                break;
                
            default:
                showHelp(player);
                break;
        }
        
        return true;
    }
    
    private void showHelp(Player player) {
        player.sendMessage(Component.text("=== أوامر المناطق ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/area pos1 - تحديد النقطة الأولى", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/area pos2 - تحديد النقطة الثانية", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/area create <نوع> - إنشاء منطقة جديدة", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/area set <اسم> <نوع> - تغيير نوع المنطقة", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/area list - عرض جميع المناطق", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/area delete <اسم> - حذف منطقة", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("الأنواع: iron, gold, emerald, diamond", NamedTextColor.GREEN));
    }
    
    private void handleCreateArea(Player player, String type) {
        PlayerSelectionData selection = plugin.getPlayerSelections().get(player.getUniqueId());
        
        if (selection == null || !selection.hasValidSelection()) {
            player.sendMessage(Component.text("يجب تحديد pos1 و pos2 أولاً!", NamedTextColor.RED));
            return;
        }
        
        Material material = getMaterialFromType(type);
        if (material == null) {
            player.sendMessage(Component.text("نوع غير صحيح! استخدم: iron, gold, emerald, diamond", NamedTextColor.RED));
            return;
        }
        
        String areaName = type + "_area_" + System.currentTimeMillis();
        
        // تأكد من أن الارتفاع 3 blocks
        adjustHeightTo3Blocks(selection);
        
        Area area = new Area(areaName, selection.getPos1(), selection.getPos2(), material);
        plugin.getAreaManager().addArea(area);
        
        player.sendMessage(Component.text("تم إنشاء منطقة " + type + " بنجاح!", NamedTextColor.GREEN));
        player.sendMessage(Component.text("اسم المنطقة: " + areaName, NamedTextColor.YELLOW));
        
        // مسح التحديد
        plugin.getPlayerSelections().remove(player.getUniqueId());
    }
    
    private void adjustHeightTo3Blocks(PlayerSelectionData selection) {
        // تعديل الارتفاع ليكون 3 blocks بالضبط
        double minY = Math.min(selection.getPos1().getY(), selection.getPos2().getY());
        selection.getPos1().setY(minY);
        selection.getPos2().setY(minY + 2); // 3 blocks height (0, 1, 2)
    }
    
    private void handlePos1(Player player) {
        PlayerSelectionData selection = plugin.getPlayerSelections().computeIfAbsent(
            player.getUniqueId(), k -> new PlayerSelectionData());
        selection.setPos1(player.getLocation().clone());
        player.sendMessage(Component.text("تم تحديد النقطة الأولى!", NamedTextColor.GREEN));
    }
    
    private void handlePos2(Player player) {
        PlayerSelectionData selection = plugin.getPlayerSelections().computeIfAbsent(
            player.getUniqueId(), k -> new PlayerSelectionData());
        selection.setPos2(player.getLocation().clone());
        player.sendMessage(Component.text("تم تحديد النقطة الثانية!", NamedTextColor.GREEN));
    }
    
    private void handleSetArea(Player player, String areaName, String type) {
        Area area = plugin.getAreaManager().getArea(areaName);
        if (area == null) {
            player.sendMessage(Component.text("المنطقة غير موجودة!", NamedTextColor.RED));
            return;
        }
        
        Material material = getMaterialFromType(type);
        if (material == null) {
            player.sendMessage(Component.text("نوع غير صحيح! استخدم: iron, gold, emerald, diamond", NamedTextColor.RED));
            return;
        }
        
        area.setMaterial(material);
        player.sendMessage(Component.text("تم تغيير نوع المنطقة إلى " + type, NamedTextColor.GREEN));
    }
    
    private void handleListAreas(Player player) {
        var areas = plugin.getAreaManager().getAllAreas();
        if (areas.isEmpty()) {
            player.sendMessage(Component.text("لا توجد مناطق!", NamedTextColor.YELLOW));
            return;
        }
        
        player.sendMessage(Component.text("=== المناطق الموجودة ===", NamedTextColor.GOLD));
        for (Area area : areas) {
            player.sendMessage(Component.text("- " + area.getName() + 
                             " (" + area.getMaterial().name().toLowerCase() + ")", NamedTextColor.YELLOW));
        }
    }
    
    private void handleDeleteArea(Player player, String areaName) {
        if (plugin.getAreaManager().removeArea(areaName)) {
            player.sendMessage(Component.text("تم حذف المنطقة بنجاح!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("المنطقة غير موجودة!", NamedTextColor.RED));
        }
    }
    
    private Material getMaterialFromType(String type) {
        switch (type.toLowerCase()) {
            case "iron": return Material.IRON_BLOCK;
            case "gold": return Material.GOLD_BLOCK;
            case "emerald": return Material.EMERALD_BLOCK;
            case "diamond": return Material.DIAMOND_BLOCK;
            default: return null;
        }
    }
}
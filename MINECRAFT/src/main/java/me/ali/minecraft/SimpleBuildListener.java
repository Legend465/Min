package me.ali.minecraft;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;

public class SimpleBuildListener implements Listener {

    private final Main plugin;

    public SimpleBuildListener(Main plugin) {
        this.plugin = plugin;
    }

    // 1. تحويل البلوكات المبنية في المناطق
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (e.isCancelled()) return;
        
        Location loc = e.getBlockPlaced().getLocation();
        for (Area area : plugin.getAreaManager().getAllAreas()) {
            if (area.contains(loc)) {
                e.getBlockPlaced().setType(area.getMaterial(), false);
                return;
            }
        }
    }

    // 2. البناء من بعيد - Right Click في الهواء
    @EventHandler(priority = EventPriority.NORMAL)
    public void onRemoteBuild(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR) return;
        
        Player player = e.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType() == Material.AIR || !item.getType().isBlock()) {
            return;
        }

        // البحث عن البلوك المستهدف - حتى 300 بلوك!
        RayTraceResult ray = player.getWorld().rayTraceBlocks(
            player.getEyeLocation(),
            player.getLocation().getDirection(),
            300.0,
            FluidCollisionMode.NEVER,
            true
        );

        if (ray == null || ray.getHitBlock() == null) return;
        
        Block target = ray.getHitBlock();
        if (target.getType() == Material.AIR) return;

        // المكان اللي هنحط فيه البلوك (فوق البلوك المستهدف)
        Location buildLoc = target.getLocation().add(0, 1, 0);
        Block buildBlock = buildLoc.getBlock();
        
        if (buildBlock.getType() != Material.AIR) return;

        // تحديد المادة - بدون شرط المنطقة!
        Material mat = item.getType();
        for (Area area : plugin.getAreaManager().getAllAreas()) {
            if (area.contains(buildLoc)) {
                mat = area.getMaterial();
                break;
            }
        }

        // وضع البلوك
        buildBlock.setType(mat, false);
        
        // تقليل العدد (لو مش creative)
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }

        e.setCancelled(true);
    }

    // 3. بناء عمود أو منطقة - Right Click على بلوك
    @EventHandler(priority = EventPriority.HIGH)
    public void onColumnBuild(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Block block = e.getClickedBlock();
        if (block == null) return;
        
        BlockFace face = e.getBlockFace();
        Player player = e.getPlayer();
        
        // فقط من فوق أو من تحت
        if (face != BlockFace.UP && face != BlockFace.DOWN) return;

        // التحقق من المنطقة
        Area area = null;
        for (Area a : plugin.getAreaManager().getAllAreas()) {
            if (a.contains(block.getLocation())) {
                area = a;
                break;
            }
        }
        
        if (area == null) return;

        e.setCancelled(true);
        
        boolean isSneaking = player.isSneaking();
        int chunkSize = isSneaking ? 5 : 1; // Shift = بناء منطقة 5×5
        int maxHeight = 320;
        
        List<Block> blocks = new ArrayList<>();
        
        if (face == BlockFace.UP) {
            // دوس من فوق = بناء من السماء لتحت
            if (isSneaking) {
                // بناء منطقة كاملة من السماء
                buildAreaFromSky(block, area, blocks, chunkSize);
                player.sendMessage(Component.text("☁️ Building from SKY (" + chunkSize + "×" + chunkSize + ")...", NamedTextColor.AQUA));
            } else {
                // عمود واحد من السماء
                buildColumnFromSky(block, area, blocks);
                player.sendMessage(Component.text("☁️ Building column from SKY...", NamedTextColor.AQUA));
            }
        } else if (face == BlockFace.DOWN) {
            // دوس من تحت = بناء لفوق
            if (isSneaking) {
                buildAreaColumn(block, area, blocks, 1, maxHeight, chunkSize);
                player.sendMessage(Component.text("🔼 Building UP (" + chunkSize + "×" + chunkSize + ")...", NamedTextColor.YELLOW));
            } else {
                buildColumn(block, area, blocks, 1, maxHeight);
                player.sendMessage(Component.text("🔼 Building UP...", NamedTextColor.YELLOW));
            }
        }
        
        // بناء كل البلوكات دفعة واحدة (أسرع)
        Material mat = area.getMaterial();
        int count = 0;
        for (Block b : blocks) {
            b.setType(mat, false);
            count++;
            // تحديث كل 50 بلوك لتحسين الأداء
            if (count % 50 == 0) {
                player.getWorld().getChunkAt(b.getLocation()).load();
            }
        }
        
        if (blocks.size() > 0) {
            String arrow = face == BlockFace.UP ? "☁️↓" : "↑";
            player.sendMessage(Component.text("⚡ " + getMaterialName(mat) + " ×" + blocks.size() + " " + arrow, NamedTextColor.GOLD));
        } else {
            player.sendMessage(Component.text("❌ No blocks to build!", NamedTextColor.RED));
        }
    }

    // بناء عمود واحد
    private void buildColumn(Block start, Area area, List<Block> blocks, int direction, int maxDist) {
        Location startLoc = start.getLocation();
        int worldMax = startLoc.getWorld().getMaxHeight();
        int worldMin = startLoc.getWorld().getMinHeight();
        
        for (int i = 1; i <= maxDist; i++) {
            int y = startLoc.getBlockY() + (i * direction);
            
            if (y >= worldMax || y < worldMin) break;
            
            Location loc = new Location(startLoc.getWorld(), startLoc.getX(), y, startLoc.getZ());
            if (!area.contains(loc)) break;
            
            Block b = loc.getBlock();
            if (b.getType() == Material.AIR) {
                blocks.add(b);
            }
        }
    }
    
    // بناء عمود من السماء (من أعلى نقطة في المنطقة)
    private void buildColumnFromSky(Block start, Area area, List<Block> blocks) {
        Location startLoc = start.getLocation();
        int worldMax = startLoc.getWorld().getMaxHeight() - 1;
        int worldMin = startLoc.getWorld().getMinHeight();
        
        // ابدأ من أعلى نقطة ممكنة
        for (int y = worldMax; y >= startLoc.getBlockY(); y--) {
            Location loc = new Location(startLoc.getWorld(), startLoc.getX(), y, startLoc.getZ());
            
            if (!area.contains(loc)) continue;
            
            Block b = loc.getBlock();
            if (b.getType() == Material.AIR) {
                blocks.add(b);
            }
        }
    }
    
    // بناء منطقة كاملة من السماء (chunk)
    private void buildAreaFromSky(Block center, Area area, List<Block> blocks, int size) {
        Location centerLoc = center.getLocation();
        int worldMax = centerLoc.getWorld().getMaxHeight() - 1;
        int halfSize = size / 2;
        
        // بناء منطقة مربعة من السماء
        for (int x = -halfSize; x <= halfSize; x++) {
            for (int z = -halfSize; z <= halfSize; z++) {
                for (int y = worldMax; y >= centerLoc.getBlockY(); y--) {
                    Location loc = new Location(
                        centerLoc.getWorld(),
                        centerLoc.getBlockX() + x,
                        y,
                        centerLoc.getBlockZ() + z
                    );
                    
                    if (!area.contains(loc)) continue;
                    
                    Block b = loc.getBlock();
                    if (b.getType() == Material.AIR) {
                        blocks.add(b);
                    }
                }
            }
        }
    }
    
    // بناء منطقة كاملة لفوق أو لتحت
    private void buildAreaColumn(Block center, Area area, List<Block> blocks, int direction, int maxDist, int size) {
        Location centerLoc = center.getLocation();
        int worldMax = centerLoc.getWorld().getMaxHeight();
        int worldMin = centerLoc.getWorld().getMinHeight();
        int halfSize = size / 2;
        
        for (int x = -halfSize; x <= halfSize; x++) {
            for (int z = -halfSize; z <= halfSize; z++) {
                for (int i = 1; i <= maxDist; i++) {
                    int y = centerLoc.getBlockY() + (i * direction);
                    
                    if (y >= worldMax || y < worldMin) break;
                    
                    Location loc = new Location(
                        centerLoc.getWorld(),
                        centerLoc.getBlockX() + x,
                        y,
                        centerLoc.getBlockZ() + z
                    );
                    
                    if (!area.contains(loc)) continue;
                    
                    Block b = loc.getBlock();
                    if (b.getType() == Material.AIR) {
                        blocks.add(b);
                    }
                }
            }
        }
    }

    private String getMaterialName(Material mat) {
        switch (mat) {
            case DIAMOND_BLOCK: return "الماس";
            case GOLD_BLOCK: return "ذهب";
            case IRON_BLOCK: return "حديد";
            case EMERALD_BLOCK: return "زمرد";
            default: return mat.name().toLowerCase();
        }
    }
}

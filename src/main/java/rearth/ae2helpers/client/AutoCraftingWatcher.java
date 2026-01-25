package rearth.ae2helpers.client;

import appeng.api.stacks.AEKey;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.menu.me.items.CraftingTermMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.network.PacketDistributor;
import rearth.ae2helpers.ae2helpers;
import rearth.ae2helpers.network.FillCraftingSlotPacket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AutoCraftingWatcher {
    
    public static final AutoCraftingWatcher INSTANCE = new AutoCraftingWatcher();
    
    // crafting slot index (0-9) to ingredient being crafted / waiting
    private final Map<Integer, Ingredient> pendingSlots = new HashMap<>();
    private boolean active = false;
    
    
    public void setPending(Map<Integer, Ingredient> recipeMap, Set<Integer> slotsToWatch) {
        this.pendingSlots.clear();
        for (Integer slotIndex : slotsToWatch) {
            Ingredient ing = recipeMap.get(slotIndex);
            if (ing != null && !ing.isEmpty()) {
                this.pendingSlots.put(slotIndex, ing);
            }
        }
        this.active = !this.pendingSlots.isEmpty();
    }
    
    public void clear() {
        this.pendingSlots.clear();
        this.active = false;
    }
    
    public void onTick(MEStorageScreen<?> screen) {
        if (!active || pendingSlots.isEmpty()) return;
        
        if (!(screen.getMenu() instanceof CraftingTermMenu menu)) {
            clear();
            return;
        }
        
        var repo = menu.getClientRepo();
        if (repo == null) return;
        
        var it = pendingSlots.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            int slotIndex = entry.getKey();
            Ingredient ingredient = entry.getValue();
            
            // if slot has been filled otherwise (e.g. by user)
            if (menu.getSlot(slotIndex).hasItem()) {
                it.remove();
                continue;
            }
            
            // check if system has this ingredient
            var entries = repo.getByIngredient(ingredient);
            AEKey bestMatch = null;
            
            for (var potential : entries) {
                // Check if we have at least 1 stored
                if (potential.getStoredAmount() > 0) {
                    bestMatch = potential.getWhat();
                    break;
                }
            }
            
            // move found ingredient to slot
            if (bestMatch != null) {
                
                ae2helpers.LOGGER.info("Found match for slot " + slotIndex + ": " + bestMatch.wrapForDisplayOrFilter());
                
                // Send packet to server
                PacketDistributor.sendToServer(new FillCraftingSlotPacket(slotIndex, bestMatch));
                
                // Stop watching this slot locally (server will fill it shortly)
                it.remove();
            }
        }
        
        if (pendingSlots.isEmpty()) {
            active = false;
        }
    }
    
    
    public void renderGhosts(GuiGraphics guiGraphics, Slot slot) {
        if (!active || !pendingSlots.containsKey(slot.index)) return;
        
        if (slot.hasItem()) return;
        
        Ingredient ingredient = pendingSlots.get(slot.index);
        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length == 0) return;
        
        // Cycle items based on time
        long time = Minecraft.getInstance().level.getGameTime() / 30;
        ItemStack stackToRender = stacks[(int) (time % stacks.length)];
        
        // Render Ghost Logic
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 100); // Draw on top of slot
        
        // 1. Render the item
        guiGraphics.renderItem(stackToRender, slot.x, slot.y);
        
        // 2. Render a semi-transparent gray overlay to make it look "ghostly"
//        RenderSystem.disableDepthTest();
//        guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x608B8B8B);
//        RenderSystem.enableDepthTest();
        
        guiGraphics.pose().popPose();
    }
}

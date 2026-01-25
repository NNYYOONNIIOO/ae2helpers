package rearth.ae2helpers.mixin.crafter;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.menu.me.common.MEStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.ae2helpers.client.AutoCraftingWatcher;

@Mixin(MEStorageScreen.class)
public class MEStorageScreenMixin {
    
    // todo
    
    @Inject(method = "containerTick", at = @At("RETURN"))
    private void onContainerTick(CallbackInfo ci) {
        MEStorageScreen<?> screen = (MEStorageScreen<?>) (Object) this;
        AutoCraftingWatcher.INSTANCE.onTick(screen);
    }
    
    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        System.out.println("Screen removed");
        // AutoCraftingWatcher.INSTANCE.clear();
    }
    
    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void onRenderSlot(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        AutoCraftingWatcher.INSTANCE.renderGhosts(guiGraphics, slot);
    }
}
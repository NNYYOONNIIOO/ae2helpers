package rearth.ae2helpers.mixin;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.parts.automation.StackWorldBehaviors;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rearth.ae2helpers.util.PatternProviderImportContext;

@Mixin(PatternProviderLogic.class)
public abstract class PatternProviderImportMixin {
    
    @Shadow @Final private IManagedGridNode mainNode;
    @Shadow @Final private IActionSource actionSource;
    @Shadow @Final private PatternProviderLogicHost host;
    
    @Unique
    private StackImportStrategy ae2helpers$importStrategy;
    
    @Unique
    private Direction ae2helpers$currentSide;
    
    @Inject(method = "doWork", at = @At("RETURN"), cancellable = true)
    private void ae2helpers$onDoWork(CallbackInfoReturnable<Boolean> cir) {
        if (!this.mainNode.isActive()) return;
        
        if (ae2helpers$doImportWork()) {
            cir.setReturnValue(true);
        }
    }
    
    // this is probably not ideal
    @Inject(method = "hasWorkToDo", at = @At("RETURN"), cancellable = true)
    private void ae2helpers$hasWorkToDo(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
    
    @Inject(method = "clearContent", at = @At("HEAD"))
    private void ae2helpers$onClearContent(CallbackInfo ci) {
        this.ae2helpers$importStrategy = null;
        this.ae2helpers$currentSide = null;
    }
    
    @Unique
    private boolean ae2helpers$doImportWork() {
        
        // PatternProviderPart returns a Set containing exactly one Direction (the side it is attached to).
        var targets = this.host.getTargets();
        if (targets.isEmpty()) return false;
        
        // Since we only operate on sided providers, we can always just take the first one
        var side = targets.iterator().next();
        
        // Initialization / Reset if side changed (e.g. Wrenched)
        if (this.ae2helpers$importStrategy == null || this.ae2helpers$currentSide != side) {
            var be = this.host.getBlockEntity();
            if (be == null || be.getLevel() == null) return false;
            
            var level = (ServerLevel) be.getLevel();
            var pos = be.getBlockPos();
            
            // The machine we want to import FROM is at pos.relative(side)
            // The face of that machine is side.getOpposite()
            this.ae2helpers$importStrategy = StackWorldBehaviors.createImportFacade(
              level,
              pos.relative(side),
              side.getOpposite(),
              (key) -> true
            );
            this.ae2helpers$currentSide = side;
        }
        
        // do actual transfer
        var context = new PatternProviderImportContext(
          this.mainNode.getGrid().getStorageService(),
          this.mainNode.getGrid().getEnergyService(),
          this.actionSource
        );
        
        this.ae2helpers$importStrategy.transfer(context);
        return context.hasDoneWork();
    }
}
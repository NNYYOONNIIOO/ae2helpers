package rearth.ae2helpers.util;

import appeng.api.behaviors.StackTransferContext;
import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.util.prioritylist.IPartitionList;
import org.jetbrains.annotations.Nullable;

public class PatternProviderImportContext implements StackTransferContext {
    private final IStorageService internalStorage;
    private final IEnergySource energySource;
    private final IActionSource actionSource;
    
    private final int initialOperations;
    private int operationsRemaining;
    
    public PatternProviderImportContext(IStorageService internalStorage,
                                        IEnergySource energySource,
                                        IActionSource actionSource) {
        this.internalStorage = internalStorage;
        this.energySource = energySource;
        this.actionSource = actionSource;
        
        initialOperations = 64;
        operationsRemaining = 64;
        
    }
    
    @Override
    public IStorageService getInternalStorage() {
        return internalStorage;
    }
    
    @Override
    public IEnergySource getEnergySource() {
        return energySource;
    }
    
    @Override
    public IActionSource getActionSource() {
        return actionSource;
    }
    
    @Override
    public int getOperationsRemaining() {
        return operationsRemaining;
    }
    
    @Override
    public void setOperationsRemaining(int operationsRemaining) {
        this.operationsRemaining = operationsRemaining;
    }
    
    @Override
    public void reduceOperationsRemaining(long inserted) {
        this.operationsRemaining -= (int) inserted;
    }
    
    @Override
    public boolean hasOperationsLeft() {
        return operationsRemaining > 0;
    }
    
    @Override
    public boolean hasDoneWork() {
        // Either we did work flag, or operations were consumed
        return initialOperations > operationsRemaining;
    }
    
    // --- Filters (Not used for this logic, but required by interface) ---
    
    @Override
    public boolean isKeyTypeEnabled(AEKeyType space) {
        return true; // Allow all types (Items, Fluids, etc)
    }
    
    @Override
    public boolean isInFilter(AEKey key) {
        return true; // We filter via 'expectations' in the insert method instead
    }
    
    @Override
    public @Nullable IPartitionList getFilter() {
        return null; // No GUI filter
    }
    
    @Override
    public void setInverted(boolean inverted) {
        // No-op
    }
    
    @Override
    public boolean isInverted() {
        return false;
    }
    
    @Override
    public boolean canInsert(AEItemKey what, long amount) {
        return internalStorage.getInventory().insert(
          what,
          amount,
          Actionable.SIMULATE,
          actionSource) > 0;
    }
}

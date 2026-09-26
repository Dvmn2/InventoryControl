package net.dvmn2.inventorycontrol.client.mixin;

import net.dvmn2.inventorycontrol.client.state.LockedSlotsState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Клиентская часть запрета выбирать закрытый слот хотбара:
 * колесо мыши перепрыгивает через закрытые слоты, цифровые клавиши на них не реагируют.
 * Действует только на инвентарь локального игрока (не на серверную часть в одиночной игре).
 * <p>
 * Класс должен быть в секции "client" mixins.json.
 */
@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {

    @Shadow
    @Final
    public PlayerEntity player;

    private boolean invctrl$isLocalInventory() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.player != null && client.player == this.player;
    }

    @Inject(method = "setSelectedSlot", at = @At("HEAD"), cancellable = true)
    private void invctrl$blockLockedSelect(int slot, CallbackInfo ci) {
        if (!LockedSlotsState.isEmpty() && invctrl$isLocalInventory() && LockedSlotsState.isLocked(slot)) {
            ci.cancel();
        }
    }
}
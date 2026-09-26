package net.dvmn2.inventorycontrol.client.mixin;

import net.dvmn2.inventorycontrol.client.state.LockedSlotsState;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code HandledScreen} — предок всех экранов с инвентарём (E, креатив, сундуки...).
 * Закрытые слоты игрока определяются по {@code slot.inventory instanceof PlayerInventory}
 * и {@code slot.getIndex()} (см. {@link LockedSlotsState#isLockedSlot(Slot)}).
 * <p>
 * Что делает миксин для закрытого слота:
 * <ul>
 *   <li>вместо слота рисуется текстура locked_slot (18x18) — предмет, если он на миг
 *       там окажется, не виден;</li>
 *   <li>слот "невидим" для мыши: нет подсветки, тултипа, кликов, drag, клавиш Q/цифр;</li>
 *   <li>обмен цифрой/F с закрытым хотбар-слотом или оффхендом блокируется.</li>
 * </ul>
 * ВАЖНО: это только косметика. Реальная блокировка — на сервере (InventoryEnforcer).
 * <p>
 * Если после обновления Minecraft/Yarn миксин перестанет грузиться — сменилось имя
 * метода (drawSlot / getSlotAt / onMouseClick) или сигнатура drawGuiTexture.
 * Лог Mixin прямо укажет, какая цель не найдена.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    // Спрайт: assets/inventorycontrol/textures/gui/sprites/locked_slot.png
    private static final Identifier invctrl$LOCKED_SLOT = Identifier.of("inventorycontrol", "locked_slot");

    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void invctrl$drawLockedSlot(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (!LockedSlotsState.isLockedSlot(slot)) {
            return;
        }
        // drawSlot вызывается внутри трансляции на (x, y) экрана, поэтому координаты слота относительные.
        // Спрайт 18x18 закрывает весь слот вместе с рамкой (предмет: +1, +1).
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, invctrl$LOCKED_SLOT,
                slot.x - 1, slot.y - 1, 18, 18);
        ci.cancel();
    }

    /**
     * Закрытый слот для мыши не существует: нет focusedSlot -> нет подсветки, тултипа,
     * кликов, drag и клавиш быстрых действий.
     */
    @Inject(method = "getSlotAt", at = @At("RETURN"), cancellable = true)
    private void invctrl$ignoreLockedSlot(double mouseX, double mouseY, CallbackInfoReturnable<Slot> cir) {
        if (LockedSlotsState.isLockedSlot(cir.getReturnValue())) {
            cir.setReturnValue(null);
        }
    }

    @Inject(
            method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void invctrl$blockLockedClick(Slot slot, int slotId, int button,
                                          SlotActionType actionType, CallbackInfo ci) {
        if (LockedSlotsState.isLockedSlot(slot)) {
            ci.cancel();
            return;
        }
        // Обмен (SWAP): button = 0..8 (цифровая клавиша) или 40 (оффхенд, клавиша F) —
        // это ровно индексы PlayerInventory, поэтому проверяем напрямую.
        if (actionType == SlotActionType.SWAP && LockedSlotsState.isLocked(button)) {
            ci.cancel();
        }
    }
}
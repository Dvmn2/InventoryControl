package net.dvmn2.inventorycontrol.client.mixin;

import net.dvmn2.inventorycontrol.client.state.LockedSlotsState;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
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
 * Отдельно от обычных слотов может быть заблокирован личный крафт 2x2: это 5
 * слотов (результат + 4 ячейки матрицы) верхнего инвентаря экрана {@code InventoryScreen}
 * (личный инвентарь игрока, не верстак — у него другой экран/тип). Эти слоты не
 * принадлежат {@code PlayerInventory}, поэтому проверяются отдельно —
 * {@link LockedSlotsState#isCraftingCellLocked(int)} по id конкретной ячейки.
 * <p>
 * Что делает миксин для заблокированного слота (обычного закрытого или крафта):
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

    /**
     * Заблокирован ли слот: обычный закрытый слот PlayerInventory, либо —
     * при заблокированном крафте — любой из 5 слотов личной сетки 2x2
     * (результат + матрица), но только в самом экране инвентаря игрока
     * (не в сундуке/верстаке/другом экране, где верхний инвентарь — не крафт).
     */
    @SuppressWarnings("ConstantConditions")
    private boolean invctrl$isBlockedSlot(Slot slot) {
        if (LockedSlotsState.isLockedSlot(slot)) {
            return true;
        }
        int cellId = invctrl$craftCellId(slot);
        return cellId != -1 && LockedSlotsState.isCraftingCellLocked(cellId);
    }

    /**
     * id ячейки личного крафта 2x2 (1-4 — сетка, 5 — результат) для слота экрана
     * {@code InventoryScreen}, чья инвентарь-принадлежность — не PlayerInventory
     * (т.е. это слот крафта, а не хранилища/брони). {@code slot.id} — индекс слота
     * в самом экране (0 — результат, 1-4 — сетка), совпадает с нумерацией
     * серверного {@code CraftingCell}/{@code InventoryClickEvent#getRawSlot()}.
     * -1, если это не слот крафта личного инвентаря игрока.
     */
    private int invctrl$craftCellId(Slot slot) {
        if (slot == null || slot.inventory instanceof PlayerInventory || !((Object) this instanceof InventoryScreen)) {
            return -1;
        }
        return switch (slot.id) {
            case 0 -> 5;
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            case 4 -> 4;
            default -> -1;
        };
    }

    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void invctrl$drawLockedSlot(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (!invctrl$isBlockedSlot(slot)) {
            return;
        }
        // drawSlot вызывается внутри трансляции на (x, y) экрана, поэтому координаты слота относительные.
        // Спрайт 18x18 закрывает весь слот вместе с рамкой (предмет: +1, +1).
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, invctrl$LOCKED_SLOT,
                slot.x - 1, slot.y - 1, 18, 18);
        ci.cancel();
    }

    /**
     * Заблокированный слот для мыши не существует: нет focusedSlot -> нет подсветки, тултипа,
     * кликов, drag и клавиш быстрых действий.
     */
    @Inject(method = "getSlotAt", at = @At("RETURN"), cancellable = true)
    private void invctrl$ignoreLockedSlot(double mouseX, double mouseY, CallbackInfoReturnable<Slot> cir) {
        if (invctrl$isBlockedSlot(cir.getReturnValue())) {
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
        if (invctrl$isBlockedSlot(slot)) {
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
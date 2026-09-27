package net.dvmn2.inventorycontrol.client.state;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;

/**
 * Хранит последний полученный от плагина набор закрытых слотов PlayerInventory
 * и набор заблокированных ячеек личного крафта 2x2 (id 1-5: 1-4 — сетка, 5 — результат).
 * Обновляется через {@code client.execute(...)}, доступ из рендера безопасен.
 * Это чисто визуальное состояние: авторитетная проверка — на сервере.
 */
public final class LockedSlotsState {

    private static volatile IntSet lockedSlots = new IntOpenHashSet();
    private static volatile IntSet craftingLockedCells = new IntOpenHashSet();

    private LockedSlotsState() {
    }

    public static void setLockedSlots(int[] slots) {
        lockedSlots = new IntOpenHashSet(slots);
    }

    public static void setCraftingLockedCells(int[] cellIds) {
        craftingLockedCells = new IntOpenHashSet(cellIds);
    }

    public static boolean isLocked(int inventoryIndex) {
        return lockedSlots.contains(inventoryIndex);
    }

    public static boolean isLockedSlot(Slot slot) {
        return slot != null
                && slot.inventory instanceof PlayerInventory
                && lockedSlots.contains(slot.getIndex());
    }

    /**
     * @param cellId id ячейки крафта (1-4 — сетка, 5 — результат)
     */
    public static boolean isCraftingCellLocked(int cellId) {
        return craftingLockedCells.contains(cellId);
    }

    public static boolean isEmpty() {
        return lockedSlots.isEmpty();
    }

    public static void clear() {
        lockedSlots = new IntOpenHashSet();
        craftingLockedCells = new IntOpenHashSet();
    }
}
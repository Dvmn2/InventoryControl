package net.dvmn2.inventorycontrol.client.state;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;

/**
 * Хранит последний полученный от плагина набор закрытых слотов в нумерации
 * {@code PlayerInventory} (она же Bukkit): 0-8 хотбар, 9-35 основной инвентарь,
 * 36-39 броня (36 — ботинки, 39 — шлем), 40 — оффхенд.
 * <p>
 * Слот экрана определяется как {@code slot.inventory instanceof PlayerInventory}
 * + {@code slot.getIndex()}, поэтому перевод в id {@code PlayerScreenHandler}
 * не нужен и закрытые слоты работают в любом экране (инвентарь, сундук, креатив...).
 * <p>
 * Обновляется через {@code client.execute(...)}, так что доступ из рендера безопасен.
 * Это чисто визуальное состояние: авторитетная проверка — на сервере.
 */
public final class LockedSlotsState {

    private static volatile IntSet lockedSlots = new IntOpenHashSet();

    private LockedSlotsState() {
    }

    public static void setLockedSlots(int[] slots) {
        lockedSlots = new IntOpenHashSet(slots);
    }

    /**
     * @param inventoryIndex индекс в терминах PlayerInventory
     */
    public static boolean isLocked(int inventoryIndex) {
        return lockedSlots.contains(inventoryIndex);
    }

    /**
     * @return true, если слот экрана — закрытый слот инвентаря игрока
     */
    public static boolean isLockedSlot(Slot slot) {
        return slot != null
                && slot.inventory instanceof PlayerInventory
                && lockedSlots.contains(slot.getIndex());
    }

    public static boolean isEmpty() {
        return lockedSlots.isEmpty();
    }

    public static void clear() {
        lockedSlots = new IntOpenHashSet();
    }
}
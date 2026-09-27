package net.dvmn2.inventorycontrol.client.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload от плагина (канал {@code dvmn2:locked_slots}): список закрытых слотов
 * в нумерации PlayerInventory (0-8 хотбар, 9-35 основной инвентарь,
 * 36-39 броня, 40 оффхенд) плюс список заблокированных ячеек личного крафта
 * 2x2 (id 1-5: 1-4 — сетка, 5 — результат; см. серверный {@code CraftingCell}).
 * <p>
 * Формат: VarInt count, count × VarInt slot, VarInt craftCount, craftCount × VarInt cellId.
 */
public record LockedSlotsPayload(int[] lockedSlots, int[] craftingLockedCells) implements CustomPayload {

    public static final CustomPayload.Id<LockedSlotsPayload> ID =
            new CustomPayload.Id<>(Identifier.of("dvmn2", "locked_slots"));

    /**
     * Реальный максимум — 41 слот PlayerInventory (0-40) и 5 ячеек крафта. Берём
     * с запасом на будущее расширение формата, но всё равно ограничиваем: без этой
     * проверки испорченный/вредоносный пакет с огромным VarInt-count уронил бы
     * клиента через {@code NegativeArraySizeException} или {@code OutOfMemoryError}
     * ещё до того, как мы успели бы что-то провалидировать по смыслу.
     */
    private static final int MAX_LOCKED_SLOTS = 64;
    private static final int MAX_CRAFTING_CELLS = 16;

    public static final PacketCodec<PacketByteBuf, LockedSlotsPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeVarInt(payload.lockedSlots().length);
                for (int slot : payload.lockedSlots()) {
                    buf.writeVarInt(slot);
                }
                buf.writeVarInt(payload.craftingLockedCells().length);
                for (int cellId : payload.craftingLockedCells()) {
                    buf.writeVarInt(cellId);
                }
            },
            buf -> {
                int count = buf.readVarInt();
                if (count < 0 || count > MAX_LOCKED_SLOTS) {
                    throw new IllegalArgumentException(
                            "locked_slots: недопустимое количество слотов: " + count);
                }
                int[] slots = new int[count];
                for (int i = 0; i < count; i++) {
                    slots[i] = buf.readVarInt();
                }
                int craftCount = buf.readVarInt();
                if (craftCount < 0 || craftCount > MAX_CRAFTING_CELLS) {
                    throw new IllegalArgumentException(
                            "locked_slots: недопустимое количество ячеек крафта: " + craftCount);
                }
                int[] craftCells = new int[craftCount];
                for (int i = 0; i < craftCount; i++) {
                    craftCells[i] = buf.readVarInt();
                }
                return new LockedSlotsPayload(slots, craftCells);
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
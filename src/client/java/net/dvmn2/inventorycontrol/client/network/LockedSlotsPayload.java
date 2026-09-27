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
                int[] slots = new int[count];
                for (int i = 0; i < count; i++) {
                    slots[i] = buf.readVarInt();
                }
                int craftCount = buf.readVarInt();
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
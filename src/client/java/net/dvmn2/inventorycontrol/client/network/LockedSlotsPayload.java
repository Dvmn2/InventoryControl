package net.dvmn2.inventorycontrol.client.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload от плагина (канал {@code dvmn2:locked_slots}): список закрытых слотов
 * в нумерации PlayerInventory (0-8 хотбар, 9-35 основной инвентарь,
 * 36-39 броня, 40 оффхенд).
 * <p>
 * Формат: VarInt count, затем count × VarInt slot —
 * см. {@code InventoryEnforcer#syncLockedSlots} на стороне сервера.
 */
public record LockedSlotsPayload(int[] lockedSlots) implements CustomPayload {

    public static final CustomPayload.Id<LockedSlotsPayload> ID =
            new CustomPayload.Id<>(Identifier.of("dvmn2", "locked_slots"));

    public static final PacketCodec<PacketByteBuf, LockedSlotsPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeVarInt(payload.lockedSlots().length);
                for (int slot : payload.lockedSlots()) {
                    buf.writeVarInt(slot);
                }
            },
            buf -> {
                int count = buf.readVarInt();
                int[] slots = new int[count];
                for (int i = 0; i < count; i++) {
                    slots[i] = buf.readVarInt();
                }
                return new LockedSlotsPayload(slots);
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
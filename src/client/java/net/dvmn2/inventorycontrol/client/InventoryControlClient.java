package net.dvmn2.inventorycontrol.client;

import net.dvmn2.inventorycontrol.client.network.LockedSlotsPayload;
import net.dvmn2.inventorycontrol.client.state.LockedSlotsState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class InventoryControlClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Регистрация приёмника заодно сообщает серверу (minecraft:register), что мод установлен —
        // по этому плагин решает, кикать ли игрока.
        PayloadTypeRegistry.playS2C().register(LockedSlotsPayload.ID, LockedSlotsPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(LockedSlotsPayload.ID, (payload, context) ->
                context.client().execute(() -> LockedSlotsState.setLockedSlots(payload.lockedSlots())));

        // При отключении от сервера сбрасываем состояние, иначе закрытые слоты
        // "утекут" в одиночную игру или на другой сервер.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> LockedSlotsState.clear());
    }
}
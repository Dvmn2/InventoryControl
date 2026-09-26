package net.dvmn2.inventorycontrol.client.mixin;

import net.dvmn2.inventorycontrol.client.state.LockedSlotsState;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Рисует текстуру закрытого слота поверх хотбара на HUD (внизу экрана).
 * Закрытые слоты на сервере пустые, поэтому порядок отрисовки относительно предметов неважен.
 * Оффхенд и броню на HUD не рисуем: пустой оффхенд ванильный HUD тоже не показывает.
 */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    private static final Identifier invctrl$LOCKED_SLOT = Identifier.of("inventorycontrol", "locked_slot");

    @Inject(method = "renderHotbar", at = @At("TAIL"))
    private void invctrl$overlayLockedHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (LockedSlotsState.isEmpty()) {
            return;
        }
        int centerX = context.getScaledWindowWidth() / 2;
        int y = context.getScaledWindowHeight() - 20;
        for (int i = 0; i < 9; i++) {
            if (LockedSlotsState.isLocked(i)) {
                // предмет хотбара рисуется в (centerX - 88 + 20*i, height - 19); спрайт на 1px больше
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, invctrl$LOCKED_SLOT,
                        centerX - 89 + i * 20, y, 18, 18);
            }
        }
    }
}
package club.someoneice.callablehorse.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "respawn", at= @At("RETURN"))
    public void afterRespawn(ServerPlayer player, boolean keepEverything, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir) {
        var newPlayer = cir.getReturnValue();
        newPlayer.setCompoundTag(player.getCompoundTag());
    }
}

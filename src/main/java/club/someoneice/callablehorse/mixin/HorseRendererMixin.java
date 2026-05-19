package club.someoneice.callablehorse.mixin;

import club.someoneice.callablehorse.client.HorseChestLayer;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HorseRenderer;
import net.minecraft.world.entity.animal.horse.Horse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HorseRenderer.class)
public abstract class HorseRendererMixin extends AbstractHorseRenderer<Horse, HorseModel<Horse>> {
    private HorseRendererMixin(EntityRendererProvider.Context context, HorseModel<Horse> model, float scale) {
        super(context, model, scale);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void callableHorse$addChestLayer(EntityRendererProvider.Context context, CallbackInfo ci) {
        this.addLayer(new HorseChestLayer(this));
    }
}

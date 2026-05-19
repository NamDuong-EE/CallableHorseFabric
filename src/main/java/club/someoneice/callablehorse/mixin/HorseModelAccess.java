package club.someoneice.callablehorse.mixin;

import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HorseModel.class)
public interface HorseModelAccess {
    @Accessor
    ModelPart getBody();
}

package ro.abyssmc.abyssdynasty.mixin;
import ro.abyssmc.abyssdynasty.AbyssDynasty;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
 @Inject(method="placeBlock",at=@At("RETURN"))
 private void dynasty$placed(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir){
  if(cir.getReturnValue() && context.getPlayer() instanceof ServerPlayer p) AbyssDynasty.placed(p,context.getClickedPos());
 }
}

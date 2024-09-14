package mod.chiselsandbits.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import mod.chiselsandbits.api.util.RayTracingUtils;

public class FluidUtils {
  public static Block getPlayerFacingBlock(Player playerEntity) {
    // check player eye position
    final HitResult hitResult = RayTracingUtils.rayTracePlayer(playerEntity);
    final Vec3 hitResultLoc = hitResult.getLocation();
    final BlockPos hitPos = new BlockPos((int) hitResultLoc.x, (int) hitResultLoc.y, (int) hitResultLoc.z);

    // check if is block
    if (hitResult.getType() == HitResult.Type.BLOCK)
    {
      final Block block = playerEntity.level().getBlockState(hitPos).getBlock();

      return block;
    }

    return null;
  }

  public static boolean isBlockFluid(Block block) {
    if (block == null) {
      return false;
    }

    BlockState state = block.defaultBlockState();
    Fluid fluid = state.getFluidState().getType();

    return fluid != Fluids.EMPTY;
  }

  public static boolean isPlayerFacingFluidBlock(Player playerEntity) {
    final Block block = getPlayerFacingBlock(playerEntity);

    return isBlockFluid(block);
  }
}

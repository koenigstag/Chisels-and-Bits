package mod.chiselsandbits.chiseling.eligibility;

import com.communi.suggestu.scena.core.registries.IPlatformRegistryManager;
import mod.chiselsandbits.api.IgnoreBlockLogic;
import mod.chiselsandbits.api.blockinformation.IBlockInformation;
import mod.chiselsandbits.api.chiseling.eligibility.IEligibilityAnalysisResult;
import mod.chiselsandbits.api.chiseling.eligibility.IEligibilityManager;
import mod.chiselsandbits.api.config.IServerConfiguration;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.block.ChiseledBlock;
import mod.chiselsandbits.blockinformation.BlockInformation;
import mod.chiselsandbits.materials.LegacyMaterialManager;
import mod.chiselsandbits.registrars.ModBlocks;
import mod.chiselsandbits.registrars.ModTags;
import mod.chiselsandbits.stateinfo.additional.StateVariantManager;
import mod.chiselsandbits.utils.ClassUtils;
import mod.chiselsandbits.utils.ReflectionHelperBlock;
import mod.chiselsandbits.utils.SimpleMaxSizedCache;
import mod.chiselsandbits.utils.TranslationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


@SuppressWarnings("ConstantConditions")
public class EligibilityManager implements IEligibilityManager
{
    private static final Logger LOGGER = LogManager.getLogger();

    private static final EligibilityManager INSTANCE = new EligibilityManager();

    private static final SimpleMaxSizedCache<IBlockInformation, IEligibilityAnalysisResult> cache =
        new SimpleMaxSizedCache<>(() -> IPlatformRegistryManager.getInstance().getBlockStateIdMap().size() == 0 ? 1000 : IPlatformRegistryManager.getInstance().getBlockStateIdMap().size());

    private EligibilityManager()
    {
    }

    public static EligibilityManager getInstance()
    {
        return INSTANCE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public IEligibilityAnalysisResult analyse(@NotNull final IBlockInformation blockInformation)
    {
        return cache.get(blockInformation, () -> {
            if (blockInformation.getBlockState().getBlock() instanceof ChiseledBlock)
            {
                return new EligibilityAnalysisResult(
                  false,
                  true,
                  LocalStrings.ChiselSupportIsAlreadyChiseled.getText()
                );
            }

            if (blockInformation.getVariant().isPresent())
            {
                return new EligibilityAnalysisResult(
                    true,
                    false,
                    LocalStrings.ChiselSupportLogicIgnored.getText()
                );
            }

            if (blockInformation.isFluid())
            {
              if (blockInformation.getFluidState().is(ModTags.Fluids.BLOCKED_CHISELABLE_FLUIDS))
              {
                  return new EligibilityAnalysisResult(
                    false,
                    false,
                    TranslationUtils.build(LocalStrings.ChiselSupportTagBlackListed)
                  );
              }

              if (blockInformation.getFluidState().is(ModTags.Fluids.FORCED_CHISELABLE_FLUIDS))
              {
                  return new EligibilityAnalysisResult(
                    true,
                    false,
                    TranslationUtils.build(LocalStrings.ChiselSupportTagWhitelisted)
                  );
              }
            }
            else
            {
              if (blockInformation.getBlockState().is(ModTags.Blocks.BLOCKED_CHISELABLE))
              {
                  return new EligibilityAnalysisResult(
                    false,
                    false,
                    TranslationUtils.build(LocalStrings.ChiselSupportTagBlackListed)
                  );
              }

              if (blockInformation.getBlockState().is(ModTags.Blocks.FORCED_CHISELABLE))
              {
                  return new EligibilityAnalysisResult(
                    true,
                    false,
                    TranslationUtils.build(LocalStrings.ChiselSupportTagWhitelisted)
                  );
              }
            }

            final Block blk = blockInformation.getBlockState().getBlock();

            try
            {
                // require basic hardness behavior...
                final ReflectionHelperBlock pb = ModBlocks.REFLECTION_HELPER_BLOCK.get();
                final Class<? extends Block> blkClass = blk.getClass();

                // custom dropping behavior?
                pb.getDrops(blockInformation.getBlockState(), null);
                final Class<?> wc = ClassUtils.getDeclaringClass(blkClass, pb.getLastInvokedThreadLocalMethodName(), BlockState.class, LootContext.Builder.class);
                final boolean quantityDroppedTest = wc == Block.class || wc == BlockBehaviour.class || wc == LiquidBlock.class;

                final boolean isNotSlab = Item.byBlock(blk) != Items.AIR || blockInformation.getBlockState().getBlock() instanceof LiquidBlock;
                boolean itemExistsOrNotSpecialDrops = quantityDroppedTest || isNotSlab;

                // ignore blocks with custom collision.
                pb.getShape(null, null, null, null);
                Class<?> collisionClass = ClassUtils.getDeclaringClass(blkClass, pb.getLastInvokedThreadLocalMethodName(), BlockState.class, BlockGetter.class, BlockPos.class, CollisionContext.class);
                boolean noCustomCollision = collisionClass == Block.class || collisionClass == BlockBehaviour.class || blk.getClass() == SlimeBlock.class || collisionClass == LiquidBlock.class;

                // full cube specifically is tied to lighting... so for glass
                // Compatibility use isFullBlock which can be true for glass.
                boolean isFullBlock = blockInformation.getBlockState().canOcclude() || blk instanceof AbstractGlassBlock || blk instanceof LiquidBlock;
                final BlockEligibilityAnalysisData info = BlockEligibilityAnalysisData.createFromState(blockInformation);

                final boolean tickingBehavior = blk.isRandomlyTicking(blockInformation.getBlockState()) && IServerConfiguration.getInstance().getBlackListRandomTickingBlocks().get();
                boolean hasBehavior = (blk instanceof EntityBlock || tickingBehavior);

                if (blkClass.isAnnotationPresent(IgnoreBlockLogic.class))
                {
                    isFullBlock = true;
                    noCustomCollision = true;
                    hasBehavior = false;
                    itemExistsOrNotSpecialDrops = true;
                }

                if (info.isCompatible() && noCustomCollision && info.getHardness() >= -0.01f && isFullBlock && !hasBehavior && itemExistsOrNotSpecialDrops)
                {
                    return new EligibilityAnalysisResult(
                      true,
                      false,
                      TranslationUtils.build ((blkClass.isAnnotationPresent(IgnoreBlockLogic.class))
                                                ? LocalStrings.ChiselSupportLogicIgnored
                                                : LocalStrings.ChiselSupportGenericSupported
                      ));
                }

                if (!blockInformation.getBlockState().getFluidState().isEmpty() && blockInformation.getBlockState().getBlock() instanceof LiquidBlock)
                {
                    return new EligibilityAnalysisResult(
                      true,
                      false,
                      TranslationUtils.build(LocalStrings.ChiselSupportGenericFluidSupport)
                    );
                }

                EligibilityAnalysisResult result = null;
                if (!info.isCompatible())
                {
                    result = new EligibilityAnalysisResult(
                      false,
                      false,
                      TranslationUtils.build(LocalStrings.ChiselSupportCompatDeactivated)
                    );
                }
                else if (!noCustomCollision)
                {
                    result = new EligibilityAnalysisResult(
                      false,
                      false,
                      TranslationUtils.build(LocalStrings.ChiselSupportCustomCollision)
                    );
                }
                else if (info.getHardness() < -0.01f)
                {
                    result = new EligibilityAnalysisResult(
                      false,
                      false,
                      TranslationUtils.build(LocalStrings.ChiselSupportNoHardness)
                    );
                }
                else if (!isNotSlab)
                {
                    result = new EligibilityAnalysisResult(
                      false,
                      false,
                      TranslationUtils.build(LocalStrings.ChiselSupportIsSlab)
                    );
                }
                else if (!isFullBlock)
                {
                    result = new EligibilityAnalysisResult(
                      false,
                      false,
                      TranslationUtils.build(LocalStrings.ChiselSupportNotFullBlock)
                    );
                }
                else if (hasBehavior)
                {
                    result = new EligibilityAnalysisResult(
                      false,
                      false,
                      TranslationUtils.build(LocalStrings.ChiselSupportHasBehaviour)
                    );
                }
                else if (!quantityDroppedTest)
                {
                    result = new EligibilityAnalysisResult(
                      false,
                      false,
                      TranslationUtils.build(LocalStrings.ChiselSupportHasCustomDrops)
                    );
                }
                return result;
            }
            catch (final Throwable t)
            {
                return new EligibilityAnalysisResult(
                  false,
                  false,
                  TranslationUtils.build(LocalStrings.ChiselSupportFailureToAnalyze)
                );
            }
        });
    }

    /**
     * Performs a chiselability analysis on the given {@link ItemStack}.
     *
     * @param provider The {@link ItemLike} to analyze.
     * @return The analysis result.
     */
    @Override
    public IEligibilityAnalysisResult analyse(@NotNull final ItemStack provider)
    {
        final Item item = provider.getItem();
        if (item instanceof BlockItem blockItem) {
            return analyse(new BlockInformation(blockItem.getBlock().defaultBlockState(), StateVariantManager.getInstance().getStateVariant(blockItem.getBlock().defaultBlockState(), provider)));
        }

        return new EligibilityAnalysisResult(
          false,
          false,
          TranslationUtils.build(LocalStrings.ChiselSupportGenericNotSupported)
        );
    }
}

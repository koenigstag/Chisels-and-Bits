package mod.chiselsandbits.item;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.chiseling.ChiselingOperation;
import mod.chiselsandbits.api.chiseling.IChiselingContext;
import mod.chiselsandbits.api.chiseling.IChiselingManager;
import mod.chiselsandbits.api.chiseling.ILocalChiselingContextCache;
import mod.chiselsandbits.api.chiseling.mode.IChiselMode;
import mod.chiselsandbits.api.client.render.preview.chiseling.IChiselContextPreviewRendererRegistry;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.config.IServerConfiguration;
import mod.chiselsandbits.api.item.chisel.IChiselItem;
import mod.chiselsandbits.api.item.chisel.IChiselingItem;
import mod.chiselsandbits.api.item.click.ClickProcessingState;
import mod.chiselsandbits.api.item.named.IDynamicallyHighlightedNameItem;
import mod.chiselsandbits.api.notifications.INotificationManager;
import mod.chiselsandbits.api.util.LocalStrings;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import mod.chiselsandbits.chiseling.ChiselingManager;
import mod.chiselsandbits.chiseling.LocalChiselingContextCache;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.registrars.ModTags;
import mod.chiselsandbits.utils.FluidUtils;
import mod.chiselsandbits.utils.ItemStackUtils;
import mod.chiselsandbits.utils.TranslationUtils;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ChiselItem extends DiggerItem implements IChiselItem, IDynamicallyHighlightedNameItem
{

    private static final Logger LOGGER = LogManager.getLogger();

    public ChiselItem(
      final Tier tier,
      final Properties builderIn)
    {
        super(
          0.1F,
          -2.8F,
          tier,
          ModTags.Blocks.CHISELED_BLOCK,
          builderIn
        );
    }

    @Override
    public Component getName(final ItemStack stack)
    {
        if (stack.getOrCreateTag().contains("chiselError")) {
            return Component.Serializer.fromJson(stack.getOrCreateTag().getString("chiselError"));
        }

        return super.getName(stack);
    }

    @Override
    public void appendHoverText(
      @NotNull final ItemStack stack, @Nullable final Level worldIn, @NotNull final List<Component> tooltip, @NotNull final TooltipFlag flagIn)
    {
        final IChiselMode mode = getMode(stack);
        if (mode.getGroup().isPresent())
        {
            tooltip.add(TranslationUtils.build("chiselmode.mode_grouped", mode.getGroup().get().getDisplayName(), mode.getDisplayName()));
        }
        else
        {
            tooltip.add(TranslationUtils.build("chiselmode.mode", mode.getDisplayName()));
        }


        super.appendHoverText(stack, worldIn, tooltip, flagIn);
    }

    @NotNull
    @Override
    public IChiselMode getMode(final ItemStack stack)
    {
        final CompoundTag stackNbt = stack.getOrCreateTag();
        if (stackNbt.contains(NbtConstants.CHISEL_MODE))
        {
            final String chiselModeName = stackNbt.getString(NbtConstants.CHISEL_MODE);
            try
            {
                final Optional<IChiselMode> registryMode = IChiselMode.getRegistry().get(new ResourceLocation(chiselModeName));
                if (registryMode.isEmpty())
                {
                    return IChiselMode.getDefaultMode();
                }

                return registryMode.get();
            }
            catch (IllegalArgumentException illegalArgumentException)
            {
                LOGGER.error(String.format("An ItemStack got loaded with a name that is not a valid chisel mode: %s", chiselModeName));
                this.setMode(stack, IChiselMode.getDefaultMode());
            }
        }

        return IChiselMode.getDefaultMode();
    }

    @Override
    public void setMode(final ItemStack stack, final IChiselMode mode)
    {
        if (mode == null)
            return;

        stack.getOrCreateTag().putString(NbtConstants.CHISEL_MODE, Objects.requireNonNull(mode.getRegistryName()).toString());
    }

    @NotNull
    @Override
    public Collection<IChiselMode> getPossibleModes()
    {
        return IChiselMode.getRegistry()
                 .getValues()
                 .stream()
                 .filter(mode -> !mode.requiresPlaceableEditStack())
                 .sorted(Comparator.comparing(IChiselMode::getRegistryName))
                 .collect(Collectors.toList());
    }

    @Override
    public ClickProcessingState handleLeftClickProcessing(
      final Player playerEntity,
      final InteractionHand hand,
      final BlockPos position,
      final Direction face,
      final ClickProcessingState currentState
    )
    {
        final ItemStack itemStack = playerEntity.getItemInHand(hand);
        if (itemStack.isEmpty() || itemStack.getItem() != this)
        {
            return currentState;
        }

        // skip if block is fluid
        if (FluidUtils.isPlayerFacingFluidBlock(playerEntity))
        {
            return currentState;
        }

        final IChiselingItem chiselingItem = (IChiselingItem) itemStack.getItem();
        final IChiselMode chiselMode = chiselingItem.getMode(itemStack);

        final IChiselingContext context = IChiselingManager.getInstance().getOrCreateContext(
          playerEntity,
          chiselMode,
          ChiselingOperation.CHISELING,
          false,
          itemStack);

        final ClickProcessingState resultState = chiselMode.onLeftClickBy(
          playerEntity,
          context
        );

        if (context.isComplete())
        {
            playerEntity.getCooldowns().addCooldown(this, Constants.TICKS_BETWEEN_CHISEL_USAGE);
            ILocalChiselingContextCache.getInstance().clear(ChiselingOperation.CHISELING);
        }

        if (context.getError().isPresent() && context.getWorld().isClientSide()) {
            INotificationManager.getInstance().notify(
              context.getMode().getIcon(),
              new Vec3(1, 0, 0),
              context.getError().get()
            );
        }

        return resultState;
    }

    @Override
    public void onLeftClickProcessingEnd(final Player player, final ItemStack stack)
    {
        final IChiselMode chiselMode = getMode(stack);
        Optional<IChiselingContext> context = IChiselingManager.getInstance().get(
          player,
          chiselMode,
          ChiselingOperation.CHISELING);

        if (context.isEmpty()) {
            context = LocalChiselingContextCache.getInstance().get(ChiselingOperation.CHISELING);
        }

        context.ifPresent(c -> {
            chiselMode.onStoppedLeftClicking(player, c);
            if (c.isComplete()) {
                player.getCooldowns().addCooldown(this, Constants.TICKS_BETWEEN_CHISEL_USAGE);
                LocalChiselingContextCache.getInstance().clear(ChiselingOperation.CHISELING);
            }
        });
    }

    @Override
    public boolean canUse(final Player playerEntity, @NotNull final ItemStack stack)
    {
        final boolean isAllowedToUse = ChiselingManager.getInstance().canChisel(playerEntity) && !playerEntity.getCooldowns().isOnCooldown(stack.getItem());
        if (getMode(stack).isSingleClickUse() && !isAllowedToUse && playerEntity.level().isClientSide() && IClientConfiguration.getInstance().getShowCoolDownError().get()) {
            INotificationManager.getInstance().notify(
                    getMode(stack).getIcon(),
                    new Vec3(1, 0, 0),
                    LocalStrings.ChiselAttemptFailedWaitForCoolDown.getText()
            );
        }

        return isAllowedToUse;
    }

    @Override
    public boolean shouldDrawDefaultHighlight(@NotNull final Player playerEntity)
    {
        final ItemStack itemStack = ItemStackUtils.getHighlightItemStackFromPlayer(playerEntity);
        if (itemStack.isEmpty() || itemStack.getItem() != this)
        {
            return true;
        }

        final IChiselingItem chiselingItem = (IChiselingItem) itemStack.getItem();
        final IChiselMode chiselMode = chiselingItem.getMode(itemStack);

        final Optional<IChiselingContext> potentiallyExistingContext =
          IChiselingManager.getInstance().get(playerEntity, chiselMode, ChiselingOperation.CHISELING);
        if (potentiallyExistingContext.isPresent())
        {
            final IChiselingContext context = potentiallyExistingContext.get();

            if (context.getMutator().isPresent())
            {
                return false;
            }

            final IChiselingContext currentContextSnapshot = context.createSnapshot();
            chiselMode.onLeftClickBy(
              playerEntity,
              currentContextSnapshot
            );

            return currentContextSnapshot.getMutator().isEmpty();
        }

        final Optional<IChiselingContext> localCachedContext = ILocalChiselingContextCache
                                                                 .getInstance()
                                                                 .get(ChiselingOperation.CHISELING);

        if (localCachedContext.isPresent()
          &&
              localCachedContext.get().getMode().isStillValid(playerEntity, localCachedContext.get(), ChiselingOperation.CHISELING)
        )
        {
            final IChiselingContext context = localCachedContext.get();

            if (
              context.getMode() == chiselMode
            )

            if (context.getMutator().isPresent())
            {
                return false;
            }

            return context.getMutator().isEmpty();
        }

        final IChiselingContext context = IChiselingManager.getInstance().create(
          playerEntity,
          chiselMode,
          ChiselingOperation.CHISELING,
          true,
          itemStack);

        //We try a left click render first.
        chiselMode.onLeftClickBy(
          playerEntity,
          context
        );

        //Store it in the local cache.
        if (!context.isComplete())
        ILocalChiselingContextCache.getInstance().set(ChiselingOperation.CHISELING, context);

        return context.getMutator().isEmpty();
    }

    @Override
    public void renderHighlight(
      final Player playerEntity,
      final LevelRenderer worldRenderer,
      final PoseStack matrixStack,
      final float partialTicks)
    {
        final ItemStack itemStack = ItemStackUtils.getHighlightItemStackFromPlayer(playerEntity);
        if (itemStack.isEmpty() || itemStack.getItem() != this)
        {
            return;
        }

        final IChiselingItem chiselingItem = (IChiselingItem) itemStack.getItem();
        final IChiselMode chiselMode = chiselingItem.getMode(itemStack);

        final Optional<IChiselingContext> potentiallyExistingContext =
          IChiselingManager.getInstance().get(playerEntity, chiselMode, ChiselingOperation.CHISELING);

        final Optional<IChiselingContext> potentiallyCachedContext = ILocalChiselingContextCache.getInstance()
                                                                       .get(ChiselingOperation.CHISELING);
        IChiselingContext context;
        if (potentiallyExistingContext.isPresent()) {
            context = potentiallyExistingContext.get();

            chiselMode.onLeftClickBy(
              playerEntity,
              context
            );
        }
        else if (potentiallyCachedContext.isPresent()
                   && potentiallyCachedContext.get().getMode() == chiselMode
                   && potentiallyCachedContext.get().getModeOfOperandus() == ChiselingOperation.CHISELING
                   && chiselMode.isStillValid(playerEntity, potentiallyCachedContext.get(), ChiselingOperation.CHISELING)) {
            context = potentiallyCachedContext.get();
        }
        else
        {
            context =  IChiselingManager.getInstance().create(
              playerEntity,
              chiselMode,
              ChiselingOperation.CHISELING,
              true,
              itemStack
            );

            chiselMode.onLeftClickBy(
              playerEntity,
              context
            );
        }

        if (context.getMutator().isEmpty())
        {
            ILocalChiselingContextCache.getInstance().clear(ChiselingOperation.CHISELING);
            //No bit was included in the chiseling action
            //We interacted with something we could not chisel
            return;
        }

        if (context.getMutator().isPresent() && context.getError().isEmpty()) {
            IChiselContextPreviewRendererRegistry.getInstance().getCurrent()
              .renderExistingContextsBoundingBox(matrixStack, context);
            ILocalChiselingContextCache.getInstance().set(ChiselingOperation.CHISELING, context);
        }
    }

    @Override
    public boolean isDamageableDuringChiseling()
    {
        return true;
    }

    @Override
    public int getMaxDamage()
    {
        return getTier().getUses() * IServerConfiguration.getInstance().getBitSize().get().getBitsPerBlock();
    }

    public int getBarWidth(ItemStack p_150900_) {
        return Math.round(13.0F - (float)p_150900_.getDamageValue() * 13.0F / (float)this.getMaxDamage());
    }

    public int getBarColor(ItemStack p_150901_) {
        float f = Math.max(0.0F, ((float)this.getMaxDamage() - (float)p_150901_.getDamageValue()) / (float)this.getMaxDamage());
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public ItemStack adaptItemStack(final ItemStack currentToolStack)
    {
        final Optional<IChiselingContext> chiselingContext =
          ILocalChiselingContextCache.getInstance().get(ChiselingOperation.CHISELING);
        final Optional<IChiselingContext> placingContext =
          ILocalChiselingContextCache.getInstance().get(ChiselingOperation.PLACING);

        if (chiselingContext.isPresent() && chiselingContext.get().getError().isPresent()) {
            final ItemStack errorStack = currentToolStack.copy();
            errorStack.getOrCreateTag().putString(
              "chiselError",
              Component.Serializer.toJson(chiselingContext.get().getError().get())
            );

            return errorStack;
        }

        if (placingContext.isPresent() && placingContext.get().getError().isPresent()) {
            final ItemStack errorStack = currentToolStack.copy();
            errorStack.getOrCreateTag().putString(
              "chiselError",
              Component.Serializer.toJson(placingContext.get().getError().get())
            );

            return errorStack;
        }

        return currentToolStack;
    }
}

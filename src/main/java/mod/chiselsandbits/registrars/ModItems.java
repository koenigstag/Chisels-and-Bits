package mod.chiselsandbits.registrars;

import com.google.common.collect.Maps;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.item.BitBagItem;
import mod.chiselsandbits.item.bit.BitItem;
import mod.chiselsandbits.item.ChiselItem;
import mod.chiselsandbits.item.ChiseledBlockItem;
import mod.chiselsandbits.item.MagnifyingGlassItem;
import mod.chiselsandbits.materials.MaterialManager;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemTier;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

import static mod.chiselsandbits.registrars.ModItemGroups.CHISELS_AND_BITS;

public final class ModItems
{

    private final static DeferredRegister<Item> ITEM_REGISTRAR = DeferredRegister.create(ForgeRegistries.ITEMS, Constants.MOD_ID);

    private ModItems()
    {
        throw new IllegalStateException("Tried to initialize: ModItems but this is a Utility class.");
    }

    public static final Map<Material, RegistryObject<ChiseledBlockItem>> MATERIAL_TO_ITEM_CONVERSIONS = Maps.newHashMap();


    public static final RegistryObject<ChiselItem> ITEM_CHISEL_STONE =
      ITEM_REGISTRAR.register("chisel_stone", () -> new ChiselItem(ItemTier.STONE, new Item.Properties().group(CHISELS_AND_BITS).maxStackSize(1)));
    public static final RegistryObject<ChiselItem> ITEM_CHISEL_IRON =
      ITEM_REGISTRAR.register("chisel_iron", () -> new ChiselItem(ItemTier.IRON, new Item.Properties().group(CHISELS_AND_BITS).maxStackSize(1)));
    public static final RegistryObject<ChiselItem> ITEM_CHISEL_GOLD =
      ITEM_REGISTRAR.register("chisel_gold", () -> new ChiselItem(ItemTier.GOLD, new Item.Properties().group(CHISELS_AND_BITS).maxStackSize(1)));
    public static final RegistryObject<ChiselItem> ITEM_CHISEL_DIAMOND =
      ITEM_REGISTRAR.register("chisel_diamond", () -> new ChiselItem(ItemTier.DIAMOND, new Item.Properties().group(CHISELS_AND_BITS).maxStackSize(1)));
    public static final RegistryObject<ChiselItem> ITEM_CHISEL_NETHERITE =
      ITEM_REGISTRAR.register("chisel_netherite", () -> new ChiselItem(ItemTier.NETHERITE, new Item.Properties().group(CHISELS_AND_BITS).maxStackSize(1)));
    public static final RegistryObject<BitItem> ITEM_BLOCK_BIT =
      ITEM_REGISTRAR.register("block_bit", () -> new BitItem(new Item.Properties().group(
      CHISELS_AND_BITS)));

    public static final RegistryObject<MagnifyingGlassItem> ITEM_MAGNIFYING_GLASS =
      ITEM_REGISTRAR.register("magnifying_glass", () -> new MagnifyingGlassItem(new Item.Properties().group(
        CHISELS_AND_BITS)));

    public static final RegistryObject<BitBagItem> ITEM_BIT_BAG_DEFAULT =
      ITEM_REGISTRAR.register("bit_bag", () -> new BitBagItem(new Item.Properties().group(
        CHISELS_AND_BITS)));
    public static final RegistryObject<BitBagItem> ITEM_BIT_BAG_DYED    =
      ITEM_REGISTRAR.register("bit_bag_dyed", () -> new BitBagItem(new Item.Properties().group(
        CHISELS_AND_BITS)));

    public static void onModConstruction() {
        ITEM_REGISTRAR.register(FMLJavaModLoadingContext.get().getModEventBus());

        MaterialManager.getInstance().getKnownMaterials()
          .forEach((name, material) -> {
              MATERIAL_TO_ITEM_CONVERSIONS.put(
                material,
                ITEM_REGISTRAR.register(
                  "chiseled" + name,
                  () -> new ChiseledBlockItem(
                    ModBlocks.MATERIAL_TO_BLOCK_CONVERSIONS.get(material).get(),
                    new Item.Properties()
                  )
                )
              );
          });
    }
}

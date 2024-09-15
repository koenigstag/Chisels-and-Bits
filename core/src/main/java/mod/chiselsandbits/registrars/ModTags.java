package mod.chiselsandbits.registrars;

import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ModTags
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static final class Items
    {
        public static TagKey<Item> CHISEL  = tag("chisel");
        public static TagKey<Item> BIT_BAG = tag("bit_bag");

        public static TagKey<Item> FORGE_PAPER = forge("paper");

        private static void init() {}

        private static TagKey<Item> tag(String name)
        {
            return TagKey.create(Registries.ITEM, new ResourceLocation(Constants.MOD_ID, name));
        }
        private static TagKey<Item> forge(String name)
        {
            return TagKey.create(Registries.ITEM, new ResourceLocation("forge:" + name));
        }
    }

    public static final class Blocks
    {
        public static TagKey<Block> FORCED_CHISELABLE  = tag("chiselable/forced");
        public static TagKey<Block> BLOCKED_CHISELABLE = tag("chiselable/blocked");
        public static TagKey<Block> CHISELED_BLOCK     = tag("chiseled/block");

        private static void init() {}

        private static TagKey<Block> tag(String name)
        {
            return TagKey.create(Registries.BLOCK, new ResourceLocation(Constants.MOD_ID, name));
        }
    }

    public static final class Fluids
    {
        public static TagKey<Fluid> FORCED_CHISELABLE_FLUIDS  = tag("chiselable/forced-fluids");
        public static TagKey<Fluid> BLOCKED_CHISELABLE_FLUIDS = tag("chiselable/blocked-fluids");

        private static void init() {}

        private static TagKey<Fluid> tag(String name)
        {
            return TagKey.create(Registries.FLUID, new ResourceLocation(Constants.MOD_ID, name));
        }
    }

    public static void onModConstruction()
    {
        Items.init();
        Blocks.init();
        LOGGER.info("Loaded tag configuration.");
    }
}

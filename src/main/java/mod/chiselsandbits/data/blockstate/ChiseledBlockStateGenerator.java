package mod.chiselsandbits.data.blockstate;

import com.google.common.collect.Maps;
import com.ldtteam.datagenerators.blockstate.BlockstateJson;
import com.ldtteam.datagenerators.blockstate.BlockstateModelJson;
import com.ldtteam.datagenerators.blockstate.BlockstateVariantJson;
import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.utils.Constants;
import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DirectoryCache;
import net.minecraft.data.IDataProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ChiselsAndBits.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ChiseledBlockStateGenerator implements IDataProvider
{
    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent event)
    {
        event.getGenerator().addProvider(new ChiseledBlockStateGenerator(event.getGenerator()));
    }

    private final DataGenerator generator;

    private ChiseledBlockStateGenerator(final DataGenerator generator) {this.generator = generator;}

    @Override
    public void act(final DirectoryCache cache) throws IOException
    {
        for (RegistryObject<BlockChiseled> blockChiseledRegistryObject : ModBlocks.getMaterialToBlockConversions().values())
        {
            BlockChiseled blockChiseled = blockChiseledRegistryObject.get();
            actOnBlock(cache, blockChiseled);
        }
    }

    @Override
    public String getName()
    {
        return "Chiseled block blockstate generator";
    }

    public void actOnBlock(final DirectoryCache cache, final Block block) throws IOException
    {
        final Map<String, BlockstateVariantJson> variants = Maps.newHashMap();

        block.getStateContainer().getProperties().stream().forEach(property -> {
            property.getAllowedValues().forEach(value -> {
                final String variantKey = String.format("%s=%s", property.getName(), value);
                String modelFile = Constants.DataGenerator.CHISELED_BLOCK_MODEL.toString();
                final BlockstateModelJson model = new BlockstateModelJson(modelFile, 0, 0);
                variants.put(variantKey, new BlockstateVariantJson(model));
            });
        });

        final BlockstateJson blockstateJson = new BlockstateJson(variants);
        final Path blockstateFolder = this.generator.getOutputFolder().resolve(Constants.DataGenerator.BLOCKSTATE_DIR);
        final Path blockstatePath = blockstateFolder.resolve(block.getRegistryName().getPath() + ".json");

        IDataProvider.save(Constants.DataGenerator.GSON, cache, blockstateJson.serialize(), blockstatePath);
    }
}

package mod.chiselsandbits.forge.data.tag;

import mod.chiselsandbits.api.util.constants.Constants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ForgeBlockTagsProvider;
import net.minecraftforge.common.data.ForgeItemTagsProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TagGeneratorEventHandler
{

    @SubscribeEvent
    public static void dataGeneratorSetup(final GatherDataEvent event)
    {
        DataGenerator gen = event.getGenerator();
        PackOutput packOutput = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        ForgeBlockTagsProvider forgeBlockTags = new ForgeBlockTagsProvider(packOutput, lookupProvider, existingFileHelper);
        event.getGenerator().addProvider(true, forgeBlockTags);
        event.getGenerator().addProvider(true, new ForgeItemTagsProvider(packOutput, lookupProvider, forgeBlockTags.contentsGetter(), existingFileHelper));

        ModBlockTagGenerator modBlockTags = new ModBlockTagGenerator(packOutput, lookupProvider, existingFileHelper);
        event.getGenerator().addProvider(true, modBlockTags);
        ModItemTagGenerator modItemTags = new ModItemTagGenerator(packOutput, lookupProvider, modBlockTags.contentsGetter(), existingFileHelper);
        event.getGenerator().addProvider(true, modItemTags);
        ModFluidTagGenerator modFluidTags = new ModFluidTagGenerator(packOutput, lookupProvider, existingFileHelper);
        event.getGenerator().addProvider(true, modFluidTags);
    }
}

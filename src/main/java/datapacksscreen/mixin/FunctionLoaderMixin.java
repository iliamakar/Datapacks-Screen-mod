package datapacksscreen.mixin;

import datapacksscreen.AutoReloadMod;
import datapacksscreen.interfaces.IFunctionLoaderMixin;
import com.google.common.collect.ImmutableMap;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.FunctionLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(FunctionLoader.class)
public class FunctionLoaderMixin implements IFunctionLoaderMixin {

    @Final
    @Shadow
    private static Logger LOGGER;
    @Shadow
    private volatile Map<Identifier, CommandFunction> functions;
    @Final
    @Shadow
    private TagGroupLoader<CommandFunction> tagLoader;
    @Shadow
    private volatile Map<Identifier, Collection<CommandFunction>> tags;

    public void addFunctions(Map<Identifier, CommandFunction> commands, ResourceManager resourceManager) {
        List<Path> deleted = AutoReloadMod.getWatcher().getDeleted();
        if (commands.size() > 0 || deleted.size() > 0) {
            ImmutableMap.Builder<Identifier, CommandFunction> builder = ImmutableMap.builder();
            if (deleted.size() > 0) {
                Map<Identifier, CommandFunction> map = new HashMap<>(functions);
                deleted.forEach(path -> {
                    try {
                        map.remove(AutoReloadMod.getIdentifierFromPath(path));
                    } catch (InvalidIdentifierException e) {
                        LOGGER.error(e.getMessage());
                    }

                });
                builder.putAll(map);
            } else {
                builder.putAll(functions);
            }

            builder.putAll(commands);
            functions = builder.buildKeepingLast();
            tags = tagLoader.load(resourceManager);
        }
    }

}

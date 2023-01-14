package datapacksscreen.interfaces;

import net.minecraft.resource.ResourceManager;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.util.Identifier;

import java.util.Map;

public interface IFunctionLoaderMixin {

    void addFunctions(Map<Identifier, CommandFunction> commands, ResourceManager resourceManager);
}

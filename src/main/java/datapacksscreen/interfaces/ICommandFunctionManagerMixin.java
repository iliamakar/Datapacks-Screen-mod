package datapacksscreen.interfaces;

import net.minecraft.server.function.FunctionLoader;

public interface ICommandFunctionManagerMixin {
    FunctionLoader getFunctionLoader();
    void updateTickFunctions();
}

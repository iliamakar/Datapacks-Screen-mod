package datapacksscreen.mixin;

import datapacksscreen.interfaces.ICommandFunctionManagerMixin;
import com.google.common.collect.ImmutableList;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.CommandFunctionManager;
import net.minecraft.server.function.FunctionLoader;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(CommandFunctionManager.class)
public class CommandFunctionManagerMixin implements ICommandFunctionManagerMixin {

    @Shadow
    private FunctionLoader loader;
    @Shadow
    private List<CommandFunction> tickFunctions;
    @Final
    @Shadow
    private static Identifier TICK_TAG_ID;

    public FunctionLoader getFunctionLoader() {
        return loader;
    }

    public void updateTickFunctions() {
        tickFunctions = ImmutableList.copyOf(loader.getTagOrEmpty(TICK_TAG_ID));
    }
}

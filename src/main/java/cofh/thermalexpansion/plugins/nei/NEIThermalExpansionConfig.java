package cofh.thermalexpansion.plugins.nei;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import cofh.thermalexpansion.ThermalExpansion;
import cofh.thermalexpansion.block.TEBlocks;
import cofh.thermalexpansion.gui.client.GuiWorkbench;
import cofh.thermalexpansion.plugins.nei.handlers.RecipeHandlerCharger;
import cofh.thermalexpansion.plugins.nei.handlers.RecipeHandlerCraftingMachine;
import cofh.thermalexpansion.plugins.nei.handlers.RecipeHandlerCraftingSecure;
import cofh.thermalexpansion.plugins.nei.handlers.RecipeHandlerCraftingUpgrade;
import cofh.thermalexpansion.plugins.nei.handlers.RecipeHandlerCrucible;
import cofh.thermalexpansion.plugins.nei.handlers.RecipeHandlerFurnace;
import cofh.thermalexpansion.plugins.nei.handlers.RecipeHandlerInsolator;
import cofh.thermalexpansion.plugins.nei.handlers.RecipeHandlerPulverizer;
import cofh.thermalexpansion.plugins.nei.handlers.RecipeHandlerSawmill;
import cofh.thermalexpansion.plugins.nei.handlers.RecipeHandlerSmelter;
import cofh.thermalexpansion.plugins.nei.handlers.RecipeHandlerTransposer;

import net.minecraft.item.ItemStack;

public class NEIThermalExpansionConfig implements IConfigureNEI {

	/* IConfigureNEI */
	@Override
	public void loadConfig() {

		API.registerRecipeHandler(RecipeHandlerCraftingMachine.instance);
		API.registerUsageHandler(RecipeHandlerCraftingMachine.instance);

		API.registerRecipeHandler(RecipeHandlerCraftingUpgrade.instance);
		API.registerUsageHandler(RecipeHandlerCraftingUpgrade.instance);

		API.registerRecipeHandler(RecipeHandlerCraftingSecure.instance);
		API.registerUsageHandler(RecipeHandlerCraftingSecure.instance);

		API.registerRecipeHandler(RecipeHandlerFurnace.instance);
		API.registerUsageHandler(RecipeHandlerFurnace.instance);

		API.registerRecipeHandler(RecipeHandlerPulverizer.instance);
		API.registerUsageHandler(RecipeHandlerPulverizer.instance);

		API.registerRecipeHandler(RecipeHandlerSawmill.instance);
		API.registerUsageHandler(RecipeHandlerSawmill.instance);

		API.registerRecipeHandler(RecipeHandlerSmelter.instance);
		API.registerUsageHandler(RecipeHandlerSmelter.instance);

		API.registerRecipeHandler(RecipeHandlerCrucible.instance);
		API.registerUsageHandler(RecipeHandlerCrucible.instance);

		API.registerRecipeHandler(RecipeHandlerTransposer.instance);
		API.registerUsageHandler(RecipeHandlerTransposer.instance);

		API.registerRecipeHandler(RecipeHandlerInsolator.instance);
		API.registerUsageHandler(RecipeHandlerInsolator.instance);

		API.registerRecipeHandler(RecipeHandlerCharger.instance);
		API.registerUsageHandler(RecipeHandlerCharger.instance);

		API.registerGuiOverlayHandler(GuiWorkbench.class, new NEIRecipeOverlayHandler(), "crafting");

		API.hideItem(new ItemStack(TEBlocks.blockAirBarrier));
		API.hideItem(new ItemStack(TEBlocks.blockAirLight));
		API.hideItem(new ItemStack(TEBlocks.blockAirSignal));
	}

	@Override
	public String getName() {

		return ThermalExpansion.modName;
	}

	@Override
	public String getVersion() {

		return ThermalExpansion.version;
	}

}

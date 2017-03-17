package cofh.thermalexpansion.gui.container.automaton;

import cofh.lib.gui.slot.SlotEnergy;
import cofh.thermalexpansion.block.automaton.TileActivator;
import cofh.thermalexpansion.gui.container.ContainerTEBase;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.tileentity.TileEntity;

public class ContainerActivator extends ContainerTEBase {

	TileActivator myTile;

	public ContainerActivator(InventoryPlayer inventory, TileEntity tile) {

		super(inventory, tile);

		myTile = (TileActivator) tile;

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				addSlotToContainer(new Slot(myTile, j + i * 3, 62 + j * 18, 17 + i * 18));
			}
		}
		Slot slot = new SlotEnergy(myTile, myTile.getChargeSlot(), 8, 53);
		if (myTile.getEnergyStorage().getMaxEnergyStored() > 0 || slot.getStack() != null) {
			addSlotToContainer(slot);
		}
	}

}

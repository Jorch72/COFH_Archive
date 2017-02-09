package cofh.thermalexpansion.gui.client.device;

import cofh.lib.gui.element.ElementButton;
import cofh.lib.gui.element.ElementEnergyStored;
import cofh.thermalexpansion.block.device.TileEnergyBuffer;
import cofh.thermalexpansion.gui.container.ContainerTEBase;
import cofh.thermalexpansion.init.TEProps;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public class GuiEnergyBuffer extends GuiDeviceBase {

	public static final String TEX_PATH = TEProps.PATH_GUI_DEVICE + "energy_buffer.png";
	public static final ResourceLocation TEXTURE = new ResourceLocation(TEX_PATH);

	protected TileEnergyBuffer myTile;

	private ElementButton decInput;
	private ElementButton incInput;
	private ElementButton decOutput;
	private ElementButton incOutput;

	private ElementButton enableInput;
	private ElementButton enableOutput;

	public GuiEnergyBuffer(InventoryPlayer inventory, TileEntity tile) {

		super(new ContainerTEBase(inventory, tile), tile, inventory.player, TEXTURE);

		generateInfo("tab.thermalexpansion.device.energy_buffer", 3);

		myTile = (TileEnergyBuffer) tile;
	}

	@Override
	public void initGui() {

		super.initGui();

		addElement(new ElementEnergyStored(this, 80, 18, myTile.getEnergyStorage()));

		decInput = new ElementButton(this, 19, 56, "DecInput", 176, 0, 176, 14, 176, 28, 14, 14, TEX_PATH).setToolTipLocalized(true);
		incInput = new ElementButton(this, 35, 56, "IncInput", 190, 0, 190, 14, 190, 28, 14, 14, TEX_PATH).setToolTipLocalized(true);
		decOutput = new ElementButton(this, 127, 56, "DecOutput", 176, 0, 176, 14, 176, 28, 14, 14, TEX_PATH).setToolTipLocalized(true);
		incOutput = new ElementButton(this, 143, 56, "IncOutput", 190, 0, 190, 14, 190, 28, 14, 14, TEX_PATH).setToolTipLocalized(true);

		enableInput = new ElementButton(this, 26, 17, "EnInput", 176, 42, 176, 58, 176, 74, 16, 16, TEX_PATH);
		enableOutput = new ElementButton(this, 134, 17, "EnOutput", 208, 42, 208, 58, 208, 74, 16, 16, TEX_PATH);

		addElement(decInput);
		addElement(incInput);
		addElement(decOutput);
		addElement(incOutput);

		addElement(enableInput);
		addElement(enableOutput);
	}

	@Override
	protected void updateElementInformation() {

		super.updateElementInformation();

		if (myTile.enableAutoInput) {
			enableInput.setToolTip("info.thermalexpansion.buffer.disableInput");
			enableInput.setSheetX(176);
			enableInput.setHoverX(176);
		} else {
			enableInput.setToolTip("info.thermalexpansion.buffer.enableInput");
			enableInput.setSheetX(192);
			enableInput.setHoverX(192);
		}

		if (myTile.enableAutoOutput) {
			enableOutput.setToolTip("info.thermalexpansion.buffer.disableOutput");
			enableOutput.setSheetX(208);
			enableOutput.setHoverX(208);
		} else {
			enableOutput.setToolTip("info.thermalexpansion.buffer.enableOutput");
			enableOutput.setSheetX(224);
			enableOutput.setHoverX(224);
		}
	}

	@Override
	public void handleElementButtonClick(String buttonName, int mouseButton) {

		boolean enInput = myTile.enableAutoInput;
		boolean enOutput = myTile.enableAutoOutput;
		int curInput = myTile.amountInput;
		int curOutput = myTile.amountOutput;

		boolean modeToggle = false;
		int change;
		float pitch = 1.0F;

		if (buttonName.equals("EnInput")) {
			myTile.enableAutoInput = !myTile.enableAutoInput;
			modeToggle = true;
			pitch = myTile.enableAutoInput ? 1.0F : 0.8F;
		}
		if (buttonName.equals("EnOutput")) {
			myTile.enableAutoOutput = !myTile.enableAutoOutput;
			modeToggle = true;
			pitch = myTile.enableAutoOutput ? 1.0F : 0.8F;
		}
		if (!modeToggle) {
			if (GuiScreen.isShiftKeyDown()) {
				change = 32;
				pitch = 0.9F;
				if (mouseButton == 1) {
					change = 16;
					pitch = 0.8F;
				}
			} else if (GuiScreen.isCtrlKeyDown()) {
				change = 8;
				pitch = 0.7F;
				if (mouseButton == 1) {
					change = 4;
					pitch = 0.6F;
				}
			} else {
				change = 1;
				pitch = 0.5F;
			}
			if (buttonName.equalsIgnoreCase("DecInput")) {
				myTile.amountInput -= change;
				pitch -= 0.1F;
			} else if (buttonName.equalsIgnoreCase("IncInput")) {
				myTile.amountInput += change;
				pitch += 0.1F;
			} else if (buttonName.equalsIgnoreCase("DecOutput")) {
				myTile.amountOutput -= change;
				pitch -= 0.1F;
			} else if (buttonName.equalsIgnoreCase("IncOutput")) {
				myTile.amountOutput += change;
				pitch += 0.1F;
			}
		}
		playClickSound(1.0F, pitch);

		myTile.sendModePacket();

		myTile.enableAutoInput = enInput;
		myTile.enableAutoOutput = enOutput;
		myTile.amountInput = curInput;
		myTile.amountOutput = curOutput;
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int x, int y) {

		String input = "" + myTile.amountInput;
		String output = "" + myTile.amountOutput;

		int xInput = 29;
		int xOutput = 137;

		if (myTile.amountInput < 10) {
			xInput += 3;
		}
		if (myTile.amountOutput < 10) {
			xOutput += 3;
		}
		fontRendererObj.drawString(input, xInput, 42, 0x404040);
		fontRendererObj.drawString(output, xOutput, 42, 0x404040);

		super.drawGuiContainerForegroundLayer(x, y);
	}

}

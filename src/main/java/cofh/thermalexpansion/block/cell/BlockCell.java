package cofh.thermalexpansion.block.cell;

import codechicken.lib.block.IParticleProvider;
import codechicken.lib.block.IType;
import codechicken.lib.block.property.unlisted.UnlistedIntegerProperty;
import codechicken.lib.item.ItemStackRegistry;
import codechicken.lib.model.blockbakery.BlockBakery;
import codechicken.lib.model.blockbakery.BlockBakeryProperties;
import codechicken.lib.model.blockbakery.IBakeryBlock;
import codechicken.lib.model.blockbakery.ICustomBlockBakery;
import cofh.core.util.CoreUtils;
import cofh.core.util.crafting.RecipeUpgradeOverride;
import cofh.lib.util.helpers.BlockHelper;
import cofh.lib.util.helpers.ItemHelper;
import cofh.lib.util.helpers.StringHelper;
import cofh.thermalexpansion.ThermalExpansion;
import cofh.thermalexpansion.block.BlockTEBase;
import cofh.thermalexpansion.block.CommonProperties;
import cofh.thermalexpansion.block.simple.BlockFrame;
import cofh.thermalexpansion.init.TEItemsOld;
import cofh.thermalexpansion.render.RenderCell;
import cofh.thermalexpansion.util.ReconfigurableHelper;
import cofh.thermalexpansion.util.crafting.PulverizerManager;
import cofh.thermalexpansion.util.crafting.TECraftingHandler;
import cofh.thermalfoundation.item.ItemMaterial;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Locale;

import static cofh.lib.util.helpers.ItemHelper.ShapedRecipe;

public class BlockCell extends BlockTEBase implements IBakeryBlock {

	public static final PropertyEnum<Types> TYPES = PropertyEnum.create("type", Types.class);

	public static final UnlistedIntegerProperty CHARGE_PROPERTY = new UnlistedIntegerProperty("charge");

	public BlockCell() {

		super(Material.IRON);
		setHardness(20.0F);
		setResistance(120.0F);
		setUnlocalizedName("thermalexpansion.cell");
		setDefaultState(getDefaultState().withProperty(TYPES, Types.BASIC));
	}

	@Override
	public int getMetaFromState(IBlockState state) {

		return state.getValue(TYPES).ordinal();
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {

		return getDefaultState().withProperty(TYPES, Types.fromMeta(meta));
	}

	@Override
	@SideOnly (Side.CLIENT)
	public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {

		return BlockBakery.handleExtendedState((IExtendedBlockState) state, world.getTileEntity(pos));
	}

	@Override
	protected BlockStateContainer createBlockState() {

		return new ExtendedBlockState.Builder(this).add(TYPES).add(BlockBakeryProperties.LAYER_FACE_SPRITE_MAP).add(CommonProperties.TYPE_PROPERTY).add(CHARGE_PROPERTY).add(CommonProperties.FACING_PROPERTY).add(CommonProperties.ACTIVE_SPRITE_PROPERTY).build();
	}

	@Override
	public TileEntity createNewTileEntity(World world, int metadata) {

		if (metadata >= Types.values().length) {
			return null;
		}
		Types type = Types.fromMeta(metadata);
		if (type == Types.CREATIVE) {
			if (!enable[Types.CREATIVE.meta()]) {
				return null;
			}
			return new TileCellCreative(metadata);
		}
		return new TileCell(metadata);
	}

	@Override
	public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list) {

		if (enable[0]) {
			list.add(ItemBlockCell.setDefaultTag(new ItemStack(item, 1, 0), -1));
		}
		for (int i = 1; i < Types.values().length; i++) {
			list.add(ItemBlockCell.setDefaultTag(new ItemStack(item, 1, i), 0));
			list.add(ItemBlockCell.setDefaultTag(new ItemStack(item, 1, i), TileCell.CAPACITY[i]));
		}
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase living, ItemStack stack) {

		if (getMetaFromState(state) == 0 && !enable[0]) {
			world.setBlockToAir(pos);
			return;
		}
		if (stack.getTagCompound() != null) {
			TileCell tile = (TileCell) world.getTileEntity(pos);

			tile.setEnergyStored(stack.getTagCompound().getInteger("Energy"));
			tile.energySend = stack.getTagCompound().getInteger("Send");
			tile.energyReceive = stack.getTagCompound().getInteger("Recv");

			int facing = BlockHelper.determineXZPlaceFacing(living);
			int storedFacing = ReconfigurableHelper.getFacing(stack);
			byte[] sideCache = ReconfigurableHelper.getSideCache(stack, tile.getDefaultSides());

			tile.sideCache[0] = sideCache[0];
			tile.sideCache[1] = sideCache[1];
			tile.sideCache[facing] = sideCache[storedFacing];
			tile.sideCache[BlockHelper.getLeftSide(facing)] = sideCache[BlockHelper.getLeftSide(storedFacing)];
			tile.sideCache[BlockHelper.getRightSide(facing)] = sideCache[BlockHelper.getRightSide(storedFacing)];
			tile.sideCache[BlockHelper.getOppositeSide(facing)] = sideCache[BlockHelper.getOppositeSide(storedFacing)];
		}
		super.onBlockPlacedBy(world, pos, state, living, stack);
	}

	@Override
	public float getBlockHardness(IBlockState blockState, World world, BlockPos pos) {

		return HARDNESS[getMetaFromState(blockState)];
	}

	@Override
	public float getExplosionResistance(World world, BlockPos pos, Entity exploder, Explosion explosion) {

		return RESISTANCE[getMetaFromState(world.getBlockState(pos))];
	}

	@Override
	public boolean hasComparatorInputOverride(IBlockState state) {

		return true;
	}

	@Override
	public boolean isSideSolid(IBlockState base_state, IBlockAccess world, BlockPos pos, EnumFacing side) {

		return true;
	}

	@Override
	@SideOnly (Side.CLIENT)
	public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {

		return layer == BlockRenderLayer.CUTOUT || layer == BlockRenderLayer.TRANSLUCENT;
	}

	@Override
	@SideOnly (Side.CLIENT)
	public ICustomBlockBakery getCustomBakery() {

		return RenderCell.instance;
	}

	@Override
	public boolean isFullCube(IBlockState state) {

		return false;
	}

	@Override
	public NBTTagCompound getItemStackTag(IBlockAccess world, BlockPos pos) {

		NBTTagCompound tag = super.getItemStackTag(world, pos);
		TileCell tile = (TileCell) world.getTileEntity(pos);

		if (tile != null) {
			if (tag == null) {
				tag = new NBTTagCompound();
			}
			ReconfigurableHelper.setItemStackTagReconfig(tag, tile);

			tag.setInteger("Energy", tile.getEnergyStored(null));
			tag.setInteger("Send", tile.energySend);
			tag.setInteger("Recv", tile.energyReceive);
		}
		return tag;
	}

	/* IDismantleable */
	@Override
	public boolean canDismantle(World world, BlockPos pos, IBlockState state, EntityPlayer player) {

		if (state.getBlock().getMetaFromState(state) == Types.CREATIVE.ordinal() && !CoreUtils.isOp(player)) {
			return false;
		}
		return super.canDismantle(world, pos, state, player);
	}

	/* IInitializer */
	@Override
	public boolean initialize() {

		TileCell.initialize();
		TileCellCreative.initialize();

		cellCreative = new ItemStack(this, 1, Types.CREATIVE.meta());
		cellBasic = new ItemStack(this, 1, Types.BASIC.meta());
		cellHardened = new ItemStack(this, 1, Types.HARDENED.meta());
		cellReinforced = new ItemStack(this, 1, Types.REINFORCED.meta());
		cellResonant = new ItemStack(this, 1, Types.RESONANT.meta());

		ItemBlockCell.setDefaultTag(cellCreative, 0);
		ItemBlockCell.setDefaultTag(cellBasic, 0);
		ItemBlockCell.setDefaultTag(cellHardened, 0);
		ItemBlockCell.setDefaultTag(cellReinforced, 0);
		ItemBlockCell.setDefaultTag(cellResonant, 0);

		ItemStackRegistry.registerCustomItemStack("cellCreative", cellCreative);
		ItemStackRegistry.registerCustomItemStack("cellBasic", cellBasic);
		ItemStackRegistry.registerCustomItemStack("cellHardened", cellHardened);
		ItemStackRegistry.registerCustomItemStack("cellReinforced", cellReinforced);
		ItemStackRegistry.registerCustomItemStack("cellResonant", cellResonant);

		return true;
	}

	@Override
	public boolean postInit() {

		if (enable[Types.BASIC.meta()]) {
			GameRegistry.addRecipe(ShapedRecipe(cellBasic, " I ", "IXI", " P ", 'I', "ingotCopper", 'X', BlockFrame.frameCellBasic, 'P', TEItemsOld.powerCoilElectrum));
			PulverizerManager.addRecipe(4000, cellBasic, ItemHelper.cloneStack(Items.REDSTONE, 8), ItemHelper.cloneStack(ItemMaterial.ingotLead, 3));
		}
		if (enable[Types.HARDENED.meta()]) {
			GameRegistry.addRecipe(ShapedRecipe(cellHardened, " I ", "IXI", " P ", 'I', "ingotCopper", 'X', BlockFrame.frameCellHardened, 'P', TEItemsOld.powerCoilElectrum));
			GameRegistry.addRecipe(new RecipeUpgradeOverride(cellHardened, new Object[] { " I ", "IXI", " I ", 'I', "ingotInvar", 'X', cellBasic }).addInteger("Send", TileCell.MAX_SEND[1], TileCell.MAX_SEND[2]).addInteger("Recv", TileCell.MAX_RECEIVE[1], TileCell.MAX_RECEIVE[2]));
			GameRegistry.addRecipe(ShapedRecipe(cellHardened, "IYI", "YXY", "IPI", 'I', "ingotInvar", 'X', BlockFrame.frameCellBasic, 'Y', "ingotCopper", 'P', TEItemsOld.powerCoilElectrum));
			PulverizerManager.addRecipe(4000, cellHardened, ItemHelper.cloneStack(Items.REDSTONE, 8), ItemHelper.cloneStack(ItemMaterial.ingotInvar, 3));
		}
		if (enable[Types.REINFORCED.meta()]) {
			GameRegistry.addRecipe(ShapedRecipe(cellReinforced, " X ", "YCY", "IPI", 'C', BlockFrame.frameCellReinforcedFull, 'I', "ingotLead", 'P', TEItemsOld.powerCoilElectrum, 'X', "ingotElectrum", 'Y', "ingotElectrum"));
		}
		if (enable[Types.RESONANT.meta()]) {
			GameRegistry.addRecipe(ShapedRecipe(cellResonant, " X ", "YCY", "IPI", 'C', BlockFrame.frameCellResonantFull, 'I', "ingotLead", 'P', TEItemsOld.powerCoilElectrum, 'X', "ingotElectrum", 'Y', "ingotElectrum"));
			GameRegistry.addRecipe(new RecipeUpgradeOverride(cellResonant, new Object[] { " I ", "IXI", " I ", 'I', "ingotEnderium", 'X', cellReinforced }).addInteger("Send", TileCell.MAX_SEND[3], TileCell.MAX_SEND[4]).addInteger("Recv", TileCell.MAX_RECEIVE[3], TileCell.MAX_RECEIVE[4]));
		}
		TECraftingHandler.addSecureRecipe(cellCreative);
		TECraftingHandler.addSecureRecipe(cellBasic);
		TECraftingHandler.addSecureRecipe(cellHardened);
		TECraftingHandler.addSecureRecipe(cellReinforced);
		TECraftingHandler.addSecureRecipe(cellResonant);

		return true;
	}

	public enum Types implements IStringSerializable, IType, IParticleProvider {
		CREATIVE, BASIC, HARDENED, REINFORCED, RESONANT;

		@Override
		public String getName() {

			return name().toLowerCase(Locale.US);
		}

		public static Types fromMeta(int meta) {

			try {
				return values()[meta];
			} catch (IndexOutOfBoundsException e) {
				throw new RuntimeException("Someone has requested an invalid metadata for a block inside ThermalExpansion.", e);
			}
		}

		@Override
		public int meta() {

			return ordinal();
		}

		@Override
		public IProperty<?> getTypeProperty() {

			return TYPES;
		}

		@Override
		public String getParticleTexture() {

			return "thermalexpansion:blocks/cell/cell_" + getName();
		}

		public static int meta(Types type) {

			return type.ordinal();
		}
	}

	public static final String[] NAMES = { "creative", "basic", "hardened", "reinforced", "resonant" };
	public static final float[] HARDNESS = { -1.0F, 5.0F, 15.0F, 20.0F, 20.0F };
	public static final int[] RESISTANCE = { 1200, 15, 90, 120, 120 };
	public static boolean[] enable = new boolean[Types.values().length];

	static {
		String category = "Cell.";

		enable[0] = ThermalExpansion.CONFIG.get(category + StringHelper.titleCase(NAMES[0]), "Enable", true);
		for (int i = 1; i < Types.values().length; i++) {
			enable[i] = ThermalExpansion.CONFIG.get(category + StringHelper.titleCase(NAMES[i]), "Recipe.Enable", true);
		}
	}

	public static final String TEXTURE_DEFAULT = "CellConfig_";
	public static final String TEXTURE_CB = "CellConfig_CB_";

	public static String textureSelection = TEXTURE_DEFAULT;

	public static ItemStack cellCreative;
	public static ItemStack cellBasic;
	public static ItemStack cellHardened;
	public static ItemStack cellReinforced;
	public static ItemStack cellResonant;

}

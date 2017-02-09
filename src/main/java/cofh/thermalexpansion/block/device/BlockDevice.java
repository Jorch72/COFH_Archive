package cofh.thermalexpansion.block.device;

import codechicken.lib.model.ModelRegistryHelper;
import codechicken.lib.model.blockbakery.BlockBakery;
import codechicken.lib.model.blockbakery.BlockBakeryProperties;
import codechicken.lib.model.blockbakery.CCBakeryModel;
import codechicken.lib.texture.IWorldBlockTextureProvider;
import codechicken.lib.texture.TextureUtils;
import cofh.api.core.IModelRegister;
import cofh.lib.util.helpers.BlockHelper;
import cofh.thermalexpansion.block.BlockTEBase;
import cofh.thermalexpansion.init.TEProps;
import cofh.thermalexpansion.init.TETextures;
import cofh.thermalexpansion.util.ReconfigurableHelper;
import cofh.thermalfoundation.item.ItemMaterial;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.List;

import static cofh.lib.util.helpers.ItemHelper.ShapedRecipe;
import static cofh.lib.util.helpers.ItemHelper.addRecipe;

public class BlockDevice extends BlockTEBase implements IModelRegister, IWorldBlockTextureProvider {

	public static final PropertyEnum<BlockDevice.Type> VARIANT = PropertyEnum.create("type", Type.class);

	public BlockDevice() {

		super(Material.IRON);

		setUnlocalizedName("device");

		setHardness(15.0F);
		setResistance(25.0F);
	}

	@Override
	protected BlockStateContainer createBlockState() {

		BlockStateContainer.Builder builder = new BlockStateContainer.Builder(this);
		// Listed
		builder.add(VARIANT);
		// UnListed
		builder.add(BlockBakeryProperties.LAYER_FACE_SPRITE_MAP);
		builder.add(TEProps.ACTIVE);
		builder.add(TEProps.FACING);
		builder.add(TEProps.SIDE_CONFIG[0]).add(TEProps.SIDE_CONFIG[1]).add(TEProps.SIDE_CONFIG[2]).add(TEProps.SIDE_CONFIG[3]).add(TEProps.SIDE_CONFIG[4]).add(TEProps.SIDE_CONFIG[5]);

		return builder.build();
	}

	@Override
	@SideOnly (Side.CLIENT)
	public void getSubBlocks(@Nonnull Item item, CreativeTabs tab, List<ItemStack> list) {

		for (int i = 0; i < Type.METADATA_LOOKUP.length; i++) {
			if (enable[i]) {
				list.add(ItemBlockDevice.setDefaultTag(new ItemStack(item, 1, i)));
			}
		}
	}

	/* TYPE METHODS */
	@Override
	public IBlockState getStateFromMeta(int meta) {

		return this.getDefaultState().withProperty(VARIANT, Type.byMetadata(meta));
	}

	@Override
	public int getMetaFromState(IBlockState state) {

		return state.getValue(VARIANT).getMetadata();
	}

	@Override
	public int damageDropped(IBlockState state) {

		return state.getValue(VARIANT).getMetadata();
	}

	/* ITileEntityProvider */
	@Override
	public TileEntity createNewTileEntity(World world, int metadata) {

		if (metadata >= Type.values().length) {
			return null;
		}
		switch (Type.byMetadata(metadata)) {
			case WATER_GEN:
				return new TileWaterGen();
			case NULLIFIER:
				return new TileNullifier();
			case HEAT_SINK:
				return new TileHeatSink();
			case TAPPER:
				return null;
			//			case EXTENDER:
			//				return new TileExtender();
			//			case CONCENTRATOR:                      // TODO
			//				return null;
			//			case ITEM_BUFFER:
			//				return new TileItemBuffer();
			//			case FLUID_BUFFER:                      // TODO
			//				return null;
			//			case ENERGY_BUFFER:                     // TODO
			//				return null;
			//			case LEXICON:                           // TODO
			//				return null;
			//			case CHUNK_LOADER:                      // TODO
			//				return null;
			default:
				return null;
		}
	}

	/* BLOCK METHODS */
	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase living, ItemStack stack) {

		if (stack.getTagCompound() != null) {
			TileDeviceBase tile = (TileDeviceBase) world.getTileEntity(pos);

			tile.readAugmentsFromNBT(stack.getTagCompound());
			tile.setEnergyStored(stack.getTagCompound().getInteger("Energy"));

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
	public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {

		return false;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {

		return true;
	}

	@Override
	public boolean isSideSolid(IBlockState base_state, IBlockAccess world, BlockPos pos, EnumFacing side) {

		return true;
	}

	/* RENDERING METHODS */
	@Override
	@SideOnly (Side.CLIENT)
	public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {

		return layer == BlockRenderLayer.SOLID || layer == BlockRenderLayer.CUTOUT;
	}

	@Override
	@SideOnly (Side.CLIENT)
	public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {

		return BlockBakery.handleExtendedState((IExtendedBlockState) super.getExtendedState(state, world, pos), world.getTileEntity(pos));
	}

	@Override
	public TextureAtlasSprite getTexture(EnumFacing side, int metadata) {

		return side != EnumFacing.NORTH ? TETextures.DEVICE_SIDE : TETextures.DEVICE_FACE[metadata % Type.values().length];
	}

	@Override
	public TextureAtlasSprite getTexture(EnumFacing side, IBlockState state, BlockRenderLayer layer, IBlockAccess world, BlockPos pos) {

		TileEntity tileEntity = world.getTileEntity(pos);
		if (tileEntity instanceof TileDeviceBase) {
			TileDeviceBase tile = ((TileDeviceBase) tileEntity);
			return tile.getTexture(side.ordinal(), layer == BlockRenderLayer.SOLID ? 0 : 1, 0);
		}
		return TextureUtils.getMissingSprite();
	}

	/* IModelRegister */
	@Override
	@SideOnly (Side.CLIENT)
	public void registerModels() {

		StateMap.Builder stateMap = new StateMap.Builder();
		stateMap.ignore(VARIANT);
		ModelLoader.setCustomStateMapper(this, stateMap.build());

		ModelResourceLocation location = new ModelResourceLocation(getRegistryName(), "normal");
		for (Type type : Type.values()) {
			ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), type.getMetadata(), location);
		}
		ModelRegistryHelper.register(location, new CCBakeryModel("thermalexpansion:blocks/device/device_side"));
	}

	/* IInitializer */
	@Override
	public boolean preInit() {

		this.setRegistryName("device");
		GameRegistry.register(this);

		ItemBlockDevice itemBlock = new ItemBlockDevice(this);
		itemBlock.setRegistryName(this.getRegistryName());
		GameRegistry.register(itemBlock);

		return true;
	}

	@Override
	public boolean initialize() {

		TileDeviceBase.config();

		TileWaterGen.initialize();
		TileNullifier.initialize();
		TileHeatSink.initialize();

		//TileExtender.initialize();
		//TileConcentrator.initialize();
		//TileItemBuffer.initialize();

		deviceWaterGen = ItemBlockDevice.setDefaultTag(new ItemStack(this, 1, Type.WATER_GEN.getMetadata()));
		deviceNullifier = ItemBlockDevice.setDefaultTag(new ItemStack(this, 1, Type.NULLIFIER.getMetadata()));
		deviceHeatSink = ItemBlockDevice.setDefaultTag(new ItemStack(this, 1, Type.HEAT_SINK.getMetadata()));

		//		deviceExtender = ItemBlockDevice.setDefaultTag(new ItemStack(this, 1, Type.EXTENDER.getMetadata()));
		//
		//		deviceItemBuffer = ItemBlockDevice.setDefaultTag(new ItemStack(this, 1, Type.ITEM_BUFFER.getMetadata()));

		return true;
	}

	@Override
	public boolean postInit() {

		String deviceFrame = "thermalexpansion:deviceFrame";
		String tinPart = "thermalexpansion:machineTin";

		// @formatter:off
		if (enable[Type.WATER_GEN.getMetadata()]) {
			addRecipe(ShapedRecipe(deviceWaterGen,
					" X ",
					"YCY",
					"IPI",
					'C', deviceFrame,
					'I', tinPart,
					'P', ItemMaterial.powerCoilGold, // TODO: Not this.
					'X', Items.BUCKET,
					'Y', "dustRedstone"
			));
		}
		if (enable[Type.NULLIFIER.getMetadata()]) {
			addRecipe(ShapedRecipe(deviceNullifier,
					" X ",
					"YCY",
					"IPI",
					'C', deviceFrame,
					'I', tinPart,
					'P', ItemMaterial.powerCoilGold, // TODO: Not this.
					'X', Items.LAVA_BUCKET,
					'Y', "dustRedstone"
			));
		}

//		if (enable[Type.ITEM_BUFFER.getMetadata()]) {
//			addRecipe(ShapedRecipe(deviceItemBuffer,
//					" X ",
//					"YCY",
//					"IPI",
//					'C', deviceFrame,
//					'I', tinPart,
//					'P', ItemMaterial.powerCoilGold, // TODO: Not this.
//					'X', Items.LAVA_BUCKET,
//					'Y', "dustRedstone"
//			));
//		}
		// @formatter:on

		return true;
	}

	/* TYPE */
	public enum Type implements IStringSerializable {

		// @formatter:off
		WATER_GEN(0, "water_gen"),
		NULLIFIER(1, "nullifier"),
		HEAT_SINK(2, "heat_sink"),
		TAPPER(3, "tapper");

//		ITEM_BUFFER(3, "item_buffer"),
//		FLUID_BUFFER(4, "fluid_buffer"),
//		ENERGY_BUFFER(5, "energy_buffer"),
//		LEXICON(6, "lexicon"),
//		CHUNK_LOADER(7, "chunk_loader"),
//		FOUNTAIN(8, "fountain"),
//		EXTENDER(9, "extender"),
//		CONCENTRATOR(10, "concentrator");
		// @formatter:on

		private static final BlockDevice.Type[] METADATA_LOOKUP = new BlockDevice.Type[values().length];
		private final int metadata;
		private final String name;
		private final int light;

		Type(int metadata, String name, int light) {

			this.metadata = metadata;
			this.name = name;
			this.light = light;
		}

		Type(int metadata, String name) {

			this(metadata, name, 0);
		}

		public int getMetadata() {

			return this.metadata;
		}

		@Override
		public String getName() {

			return this.name;
		}

		public int getLight() {

			return light;
		}

		public static Type byMetadata(int metadata) {

			if (metadata < 0 || metadata >= METADATA_LOOKUP.length) {
				metadata = 0;
			}
			return METADATA_LOOKUP[metadata];
		}

		static {
			for (Type type : values()) {
				METADATA_LOOKUP[type.getMetadata()] = type;
			}
		}
	}

	public static boolean[] enable = new boolean[Type.values().length];

	/* REFERENCES */
	public static ItemStack deviceWaterGen;
	public static ItemStack deviceNullifier;
	public static ItemStack deviceHeatSink;
	public static ItemStack deviceItemBuffer;
	public static ItemStack deviceFluidBuffer;
	public static ItemStack deviceEnergyBuffer;
	public static ItemStack deviceLexicon;
	public static ItemStack deviceChunkLoader;

	public static ItemStack deviceFountain;
	public static ItemStack deviceExtender;
	public static ItemStack deviceConcentrator;

}

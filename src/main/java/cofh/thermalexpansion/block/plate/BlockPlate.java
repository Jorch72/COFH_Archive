package cofh.thermalexpansion.block.plate;

import codechicken.lib.block.IParticleProvider;
import codechicken.lib.block.IType;
import codechicken.lib.block.property.unlisted.UnlistedIntegerProperty;
import codechicken.lib.item.ItemStackRegistry;
import codechicken.lib.util.BlockUtils;
import cofh.api.block.IBlockConfigGui;
import cofh.core.util.crafting.RecipeUpgrade;
import cofh.lib.util.helpers.ItemHelper;
import cofh.lib.util.helpers.StringHelper;
import cofh.thermalexpansion.ThermalExpansion;
import cofh.thermalexpansion.block.BlockTEBase;
import cofh.thermalexpansion.block.CommonProperties;
import cofh.thermalexpansion.block.cell.BlockCell;
import codechicken.lib.model.blockbakery.BlockBakery;
import codechicken.lib.model.blockbakery.IBakeryBlock;
import codechicken.lib.model.blockbakery.ICustomBlockBakery;
import cofh.thermalexpansion.item.TEItems;
import cofh.thermalexpansion.render.RenderPlate;
import cofh.thermalexpansion.util.crafting.TECraftingHandler;
import cofh.thermalexpansion.util.crafting.TransposerManager;
import cofh.thermalfoundation.fluid.TFFluids;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static cofh.lib.util.helpers.ItemHelper.ShapedRecipe;

public class BlockPlate extends BlockTEBase implements IBlockConfigGui, IBakeryBlock {

    private static class PlateMaterial extends Material {

		public PlateMaterial(MapColor color) {

			super(color);
			this.setRequiresTool();
		}

		@Override
		public boolean isSolid() {

			return false;
		}

    }
	public static final Material material = new PlateMaterial(MapColor.IRON);

    public static final PropertyEnum<Types> TYPES = PropertyEnum.create("type", Types.class);
    public static final UnlistedIntegerProperty ALIGNMENT_PROPERTY = new UnlistedIntegerProperty("alignment");

	public BlockPlate() {

		super(material);
		//setBlockBounds(0, 0, 0, 1, 0.0625F, 1);
		setHardness(15.0F);
		setResistance(25.0F);
		setUnlocalizedName("thermalexpansion.plate");
		basicGui = false;
	}

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(TYPES).meta();
    }


    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(TYPES, Types.fromMeta(meta));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return BlockBakery.handleExtendedState((IExtendedBlockState) state, world.getTileEntity(pos));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new ExtendedBlockState.Builder(this).add(TYPES).add(ALIGNMENT_PROPERTY).add(CommonProperties.FACING_PROPERTY).add(CommonProperties.TYPE_PROPERTY).build();
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return layer == BlockRenderLayer.SOLID || layer == BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean openConfigGui(IBlockAccess world, BlockPos pos, EnumFacing side, EntityPlayer player) {

		return ((TilePlateBase) world.getTileEntity(pos)).openGui(player);
	}

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
	public NBTTagCompound getItemStackTag(IBlockAccess world, BlockPos pos) {

		NBTTagCompound tag = super.getItemStackTag(world, pos);
		TilePlateBase tile = (TilePlateBase) world.getTileEntity(pos);

		if (tile instanceof TilePlatePoweredBase) {
			if (tag == null) {
				tag = new NBTTagCompound();
			}
			tag.setInteger("Energy", ((TilePlatePoweredBase) tile).getEnergyStored(null));
		}
		return tag;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int metadata) {

		if (metadata >= Types.values().length) {
			return null;
		}
		switch (Types.values()[metadata]) {
		case FRAME:
			return new TilePlateBase();
		case SIGNAL:
			return new TilePlateSignal();
		case IMPULSE:
			return new TilePlateImpulse();
		case TRANSLOCATE:
			return new TilePlateTranslocate();
		case CHARGE:
			return new TilePlateCharge();
		case EXCURSION:
			return world.isRemote ? new TilePlateExcursionClient() : new TilePlateExcursion();
		case TELEPORT:
			return new TilePlateTeleporter();
		default:
			return null;
		}
	}

	@Override
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {

		for (int i = 0; i < Types.values().length; i++) {
			list.add(new ItemStack(item, 1, i));
		}
	}

	@Override
	public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis_fd) {

		boolean r = false;
		l: {
			if (world.isRemote) {
				break l;
			}
			TilePlateBase tile = (TilePlateBase) world.getTileEntity(pos);
			if (tile == null) {
				break l;
			}
			tile.rotated();
			int axis = axis_fd.ordinal();
			if ((axis >> 1) == (tile.alignment >> 1)) {
				if (tile.canRotate()) {
					tile.direction = (byte) ((tile.direction + 1) % 6);
					r = true;
				}
			} else {
				// TODO: rotate to next valid alignment
			}
		}
		if (r) {
            BlockUtils.fireBlockUpdate(world, pos);
		}
		return r;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random r) {

		TilePlateBase tile = (TilePlateBase) world.getTileEntity(pos);
		if (tile == null) {
			return;
		}
		tile.randomDisplayTick(world, r);
	}

    @Override
    public void onFallenUpon(World world, BlockPos pos, Entity entity, float fallDistance) {
		// TODO: this method is literally never called on this block by anything
		// because mojang couldn't so much as design a wet paper bag to save their lives
		// ASM to change the magic "get the block below us" value of 0.2 to 0.02 "works"
		// but will probably break several other things since the fall calculation is performed
		// after the new position is known but is still stored only in local variables in moveEntity

			TilePlateBase tile = (TilePlateBase) world.getTileEntity(pos);
			if (tile == null) {
				return;
			}
			AxisAlignedBB bb = entity.getEntityBoundingBox();
			if (!bb.intersectsWith(getCollisionBlockBounds(tile, pos))) {
				return;
			}
			switch (Types.values()[getMetaFromState(world.getBlockState(pos))]) {
			case IMPULSE:
				if ((tile.direction >> 1) == 0 && (tile.alignment == 0)) {
					entity.fallDistance = 0;
				}
				break;
			case TRANSLOCATE:
				tile.onEntityCollidedWithBlock(entity);
				break;
			default:

			}

	}

    @Override
    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
		TilePlateBase tile = (TilePlateBase) world.getTileEntity(pos);
		if (tile == null) {
			return;
		}
		AxisAlignedBB bb = entity.getEntityBoundingBox();
		if (!bb.intersectsWith(getCollisionBlockBounds(tile, pos))) {
			return;
		}
		tile.onEntityCollidedWithBlock(entity);
	}

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {

        TilePlateBase theTile = (TilePlateBase) source.getTileEntity(pos);
        if (theTile != null) {
            switch (theTile.alignment) {
                case 0:
                    return new AxisAlignedBB(0, 0, 0, 1, 1 / 16f, 1);
                case 1:
                    return new AxisAlignedBB(0, 15 / 16f, 0, 1, 1, 1);
                case 2:
                    return new AxisAlignedBB(0, 0, 0, 1, 1, 1 / 16f);
                case 3:
                    return new AxisAlignedBB(0, 0, 15 / 16f, 1, 1, 1);
                case 4:
                    return new AxisAlignedBB(0, 0, 0, 1 / 16f, 1, 1);
                case 5:
                    return new AxisAlignedBB(15 / 16f, 0, 0, 1, 1, 1);
                default:
            }
        }

        return new AxisAlignedBB(0, 0, 0, 1, 0.0625F, 1);
    }

    public AxisAlignedBB getCollisionBlockBounds(TilePlateBase theTile, BlockPos pos) {

		float A = 1 / 16f;
		float B = 15 / 16f;
		float O = (theTile.direction == 7 ? 16 : 2) / 16f;
		AxisAlignedBB bb = null;
		switch (theTile.alignment) {
		case 0:
			bb = new AxisAlignedBB(A, 0, A, B, O, B).offset(pos);
			break;
		case 1:
			bb = new AxisAlignedBB(A, 1 - O, A, B, 1, B).offset(pos);
			break;
		case 2:
			bb = new AxisAlignedBB(A, A, 0, B, B, O).offset(pos);
			break;
		case 3:
			bb = new AxisAlignedBB(A, A, 1 - O, B, B, 1).offset(pos);
			break;
		case 4:
			bb = new AxisAlignedBB(0, A, A, O, B, B).offset(pos);
			break;
		case 5:
			bb = new AxisAlignedBB(1 - O, A, A, 1, B, B).offset(pos);
			break;
		default:
		}
		return bb;
	}

	//@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {

		TilePlateBase theTile = (TilePlateBase) world.getTileEntity(new BlockPos(x,y,z));
		if (theTile != null) {
			switch (theTile.alignment) {
			case 0:
				//setBlockBounds(0, 0, 0, 1, 1 / 16f, 1);
				return;
			case 1:
				//setBlockBounds(0, 15 / 16f, 0, 1, 1, 1);
				return;
			case 2:
				//setBlockBounds(0, 0, 0, 1, 1, 1 / 16f);
				return;
			case 3:
				//setBlockBounds(0, 0, 15 / 16f, 1, 1, 1);
				return;
			case 4:
				//setBlockBounds(0, 0, 0, 1 / 16f, 1, 1);
				return;
			case 5:
				//setBlockBounds(15 / 16f, 0, 0, 1, 1, 1);
				return;
			default:
			}
		}
	}

    @Nullable
    @Override
    public RayTraceResult collisionRayTrace(IBlockState blockState, World worldIn, BlockPos pos, Vec3d start, Vec3d end) {
		//setBlockBoundsBasedOnState(world, x, y, z);
		return super.collisionRayTrace(blockState, worldIn, pos, start, end);
	}

	//@Override
	protected AxisAlignedBB getStatelessBoundingBox(World world, int x, int y, int z) {

		return null;
	}

    @Override
    @SideOnly(Side.CLIENT)
    public ICustomBlockBakery getCustomBakery() {
        return RenderPlate.instance;
    }

    /* IInitializer */
    @Override
	public boolean initialize() {

		TilePlateBase.initialize();
		TilePlateSignal.initialize();
		TilePlateImpulse.initialize();
		TilePlateTranslocate.initialize();
		TilePlateCharge.initialize();
		TilePlateExcursion.initialize();
		TilePlateTeleporter.initialize();

		plateFrame = new ItemStack(this, 1, Types.FRAME.ordinal());
		plateSignal = new ItemStack(this, 1, Types.SIGNAL.ordinal());
		plateImpulse = new ItemStack(this, 1, Types.IMPULSE.ordinal());
		plateTranslocate = new ItemStack(this, 1, Types.TRANSLOCATE.ordinal());
		plateCharge = new ItemStack(this, 1, Types.CHARGE.ordinal());
		plateExcursion = new ItemStack(this, 1, Types.EXCURSION.ordinal());
		plateTeleport = new ItemStack(this, 1, Types.TELEPORT.ordinal());

		ItemStackRegistry.registerCustomItemStack("plateFrame", plateFrame);
        ItemStackRegistry.registerCustomItemStack("plateSignal", plateSignal);
        ItemStackRegistry.registerCustomItemStack("plateImpulse", plateImpulse);
        ItemStackRegistry.registerCustomItemStack("plateTranslocate", plateTranslocate);
        ItemStackRegistry.registerCustomItemStack("plateCharge", plateCharge);
        ItemStackRegistry.registerCustomItemStack("plateExcursion", plateExcursion);
        ItemStackRegistry.registerCustomItemStack("plateTeleport", plateTeleport);

		return true;
	}

	@Override
	public boolean postInit() {

		// @formatter:off
		if (enable[Types.FRAME.ordinal()]) {
			ItemHelper.addRecipe(ShapedRecipe(plateFrame, "SGS",
					"I I",
					"SIS",
					'S', "ingotSignalum",
					'G', "blockGlassHardened",
					'I', "ingotInvar"));
		}

		if (enable[Types.SIGNAL.ordinal()]) {
			TransposerManager.addTEFillRecipe(2000, plateFrame, plateSignal, new FluidStack(TFFluids.fluidRedstone, 1000), false);
		}

		if (enable[Types.IMPULSE.ordinal()]) {
			TransposerManager.addTEFillRecipe(2000, plateFrame, plateImpulse, new FluidStack(TFFluids.fluidGlowstone, 1000), false);
		}

		if (enable[Types.TRANSLOCATE.ordinal()]) {
			TransposerManager.addTEFillRecipe(2000, plateFrame, plateTranslocate, new FluidStack(TFFluids.fluidEnder, 1000), false);
		}

		if (enable[Types.CHARGE.ordinal()]) {
			ItemHelper.addRecipe(new RecipeUpgrade(5, plateCharge, new Object[] {
					"EGE",
					"IPI",
					"ECE",
					'E', "ingotElectrum",
					'G', "gemDiamond",
					'I', TEItems.powerCoilSilver,
					'P', plateSignal,
					'C', TEItems.powerCoilGold,
			}));
		}

		if (enable[Types.EXCURSION.ordinal()]) {
			ItemHelper.addRecipe(new RecipeUpgrade(5, plateExcursion, new Object[] {
					"EGE",
					"GPG",
					"ECE",
					'E', "ingotSignalum",
					'G', "blockQuartz",
					'P', plateImpulse,
					'C', TEItems.powerCoilGold,
			}));
		}

		if (enable[Types.TELEPORT.ordinal()]) {
			ItemHelper.addRecipe(new RecipeUpgrade(5, plateTeleport, new Object[] {
					"EIE",
					"IPI",
					"ECE",
					'E', "ingotEnderium",
					'I', "ingotBronze",
					'P', plateTranslocate,
					'C', BlockCell.cellHardened,
			}));
		}

		TECraftingHandler.addSecureRecipe(plateSignal);
		TECraftingHandler.addSecureRecipe(plateImpulse);
		TECraftingHandler.addSecureRecipe(plateTranslocate);
		TECraftingHandler.addSecureRecipe(plateCharge);
		TECraftingHandler.addSecureRecipe(plateExcursion);
		TECraftingHandler.addSecureRecipe(plateTeleport);

		return true;
		// @formatter:on
	}

	public enum Types implements IStringSerializable, IType, IParticleProvider {
		FRAME,
		SIGNAL,
		IMPULSE,
		TRANSLOCATE,
		CHARGE,
		EXCURSION,
		TELEPORT;

		public int texture = ordinal() > 3 ? 7 : 2;

        @Override
        public String getName() {
            return name().toLowerCase(Locale.US);
        }

        public static Types fromMeta(int meta) {
            try {
                return values()[meta];
            } catch (IndexOutOfBoundsException e){
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
            return "thermalexpansion:blocks/plate/plate_bottom";
        }
    }

	public static final String[] NAMES = { "frame", "signal", "impulse", "translocate", "charge", "excursion", "teleport" };
	public static boolean[] enable = new boolean[Types.values().length];

	static {
		String category = "Plate.";

		for (int i = 0; i < Types.values().length; i++) {
			enable[i] = ThermalExpansion.config.get(category + StringHelper.titleCase(NAMES[i]), "Recipe.Enable", true);
		}
	}

	public static ItemStack plateFrame;
	public static ItemStack plateSignal;
	public static ItemStack plateImpulse;
	public static ItemStack plateTranslocate;
	public static ItemStack plateCharge;
	public static ItemStack plateExcursion;
	public static ItemStack plateTeleport;

}

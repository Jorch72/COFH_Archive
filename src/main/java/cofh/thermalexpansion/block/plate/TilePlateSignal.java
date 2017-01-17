package cofh.thermalexpansion.block.plate;

import codechicken.lib.util.BlockUtils;
import codechicken.lib.util.ServerUtils;
import cofh.core.RegistrySocial;
import cofh.core.network.PacketCoFHBase;
import cofh.lib.util.helpers.MathHelper;
import cofh.thermalexpansion.block.TEBlocks;
import cofh.thermalexpansion.gui.client.plate.GuiPlateSignal;
import cofh.thermalexpansion.gui.container.ContainerTEBase;
import com.mojang.authlib.GameProfile;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.GameRegistry;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

//import thermalexpansion.gui.client.plate.GuiPlateSignal;

public class TilePlateSignal extends TilePlateBase implements ITickable {

	public static void initialize() {

		GameRegistry.registerTileEntity(TilePlateSignal.class, "cofh.thermalexpansion.PlateSignal");
	}

	public static final byte MIN_DISTANCE = 0;
	public static final byte MAX_DISTANCE = 15;
	public static final byte MIN_INTENSITY = 0;
	public static final byte MAX_INTENSITY = 15;
	public static final byte MIN_DURATION = 2;
	public static final byte MAX_DURATION = 40;

	public byte distance = 15;
	public byte intensity = 15;
	public byte duration = 20;
	public byte collisionMode = 0;
	byte collided = 0;

	public TilePlateSignal() {

		super(BlockPlate.Types.SIGNAL);
	}

	@Override
	public void blockBroken() {

		removeSignal();
		super.blockBroken();
	}

	@Override
	public void rotated() {

		removeSignal();
	}

	//@Override
	public boolean canUpdate() {

		// FIXME: in 1.8 we can differentiate random world ticks and update ticks on the block.
		// we can use that to destroy the block
		return true;
	}

	private void removeSignal() {

		int[] v = getVector(distance + 1);
		int x = v[0], y = v[1], z = v[2];

		if (worldObj.getBlockState(getPos().add(x,y,z)).getBlock().equals(TEBlocks.blockAirSignal)) {
			worldObj.setBlockState(getPos().add(x,y,z), Blocks.AIR.getDefaultState(), 3);
		}
        BlockUtils.fireBlockUpdate(worldObj,getPos());
	}

	@Override
	public void update() {

		if (collided > 0) {
			markChunkDirty();
			if (--collided == 0) {
				removeSignal();
			}
		}
	}

	@Override
	public void onEntityCollidedWithBlock(Entity entity) {

		if (worldObj.isRemote) {
			return;
		}

		switch (collisionMode) {
		case 3:
			if (!(entity instanceof IMob)) {
				return;
			}
			break;
		case 2:
			if (!(entity instanceof EntityPlayer)) {
				return;
			}
			break;
		case 1:
			if (!(entity instanceof EntityLivingBase)) {
				return;
			}
			break;
		case 0:
		default:
			break;
		}

		l: if (filterSecure && !getAccess().isPublic()) {
			o: if (entity instanceof EntityItem) {
				String name = ((EntityItem) entity).getThrower();
				if (name == null) {
					break o;
				}
				if (getAccess().isRestricted() && RegistrySocial.playerHasAccess(name, getOwner())) {
					break l;
				}
				GameProfile i = ServerUtils.mc().getPlayerProfileCache().getGameProfileForUsername(name);
				if (getOwner().getId().equals(i.getId())) {
					break l;
				}
			} else if (canPlayerAccess((EntityPlayer) entity)) {
				break l;
			}
			return;
		}

		if (collided > 0) {
			collided = duration;
			if (worldObj.getTotalWorldTime() % 10 != 0) {
				return;
			}
		}

		int[] v = getVector(distance + 1);
		int x = v[0], y = v[1], z = v[2];

        BlockPos offsetPos = getPos().add(x,y,z);
		if (worldObj.isAirBlock(offsetPos)) {
			if (worldObj.setBlockState(offsetPos, TEBlocks.blockAirSignal.getStateFromMeta(intensity), 3)) {
				markChunkDirty();

				BlockUtils.fireBlockUpdate(worldObj,pos);
			}
			collided = duration;
		}
	}

	@Override
	protected boolean readPortableTagInternal(EntityPlayer player, NBTTagCompound tag) {

		distance = tag.getByte("Dist");
		intensity = tag.getByte("Int");
		duration = tag.getByte("Time");
		collisionMode = tag.getByte("Mode");

		return true;
	}

	@Override
	protected boolean writePortableTagInternal(EntityPlayer player, NBTTagCompound tag) {

		tag.setByte("Dist", distance);
		tag.setByte("Int", intensity);
		tag.setByte("Time", duration);
		tag.setByte("Mode", collisionMode);

		return true;
	}

	/* GUI METHODS */
	@Override
	public Object getGuiClient(InventoryPlayer inventory) {

		return new GuiPlateSignal(inventory, this);
	}

	@Override
	public Object getGuiServer(InventoryPlayer inventory) {

		return new ContainerTEBase(inventory, this, false, false);
	}

	/* NBT METHODS */
	@Override
	public void readFromNBT(NBTTagCompound nbt) {

		super.readFromNBT(nbt);

		distance = nbt.getByte("Dist");
		intensity = nbt.getByte("Int");
		duration = nbt.getByte("Time");
		collided = nbt.getByte("Col");
		collisionMode = nbt.getByte("cMode");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {

		super.writeToNBT(nbt);

		nbt.setByte("Dist", distance);
		nbt.setByte("Int", intensity);
		nbt.setByte("Time", duration);
		nbt.setByte("Col", collided);
		nbt.setByte("cMode", collisionMode);
        return nbt;
	}

	/* NETWORK METHODS */
	@Override
	public PacketCoFHBase getPacket() {

		PacketCoFHBase payload = super.getPacket();

		payload.addByte(distance);
		payload.addByte(intensity);
		payload.addByte(duration);
		payload.addByte(collided);
		payload.addByte(collisionMode);

		return payload;
	}

	@Override
	public PacketCoFHBase getGuiPacket() {

		PacketCoFHBase payload = super.getGuiPacket();

		payload.addByte(distance);
		payload.addByte(intensity);
		payload.addByte(duration);

		return payload;
	}

	@Override
	public PacketCoFHBase getModePacket() {

		PacketCoFHBase payload = super.getModePacket();

		payload.addByte(MathHelper.clamp(distance, MIN_DISTANCE, MAX_DISTANCE));
		payload.addByte(MathHelper.clamp(intensity, MIN_INTENSITY, MAX_INTENSITY));
		payload.addByte(MathHelper.clamp(duration, MIN_DURATION, MAX_DURATION));
		payload.addByte(collisionMode);

		return payload;
	}

	@Override
	protected void handleGuiPacket(PacketCoFHBase payload) {

		super.handleGuiPacket(payload);

		distance = payload.getByte();
		intensity = payload.getByte();
		duration = payload.getByte();
	}

	@Override
	protected void handleModePacket(PacketCoFHBase payload) {

		super.handleModePacket(payload);

		byte newDist = payload.getByte();

		if (newDist != distance) {
			removeSignal();
			distance = newDist;
		}
		intensity = payload.getByte();
		duration = payload.getByte();
		collisionMode = payload.getByte();
	}

	/* ITilePacketHandler */
	@Override
	public void handleTilePacket(PacketCoFHBase payload, boolean isServer) {

		super.handleTilePacket(payload, isServer);

		if (!isServer) {
			distance = payload.getByte();
			intensity = payload.getByte();
			duration = payload.getByte();
			collisionMode = payload.getByte();
		} else {
			payload.getByte();
			payload.getByte();
			payload.getByte();
			payload.getByte();
		}
	}

}

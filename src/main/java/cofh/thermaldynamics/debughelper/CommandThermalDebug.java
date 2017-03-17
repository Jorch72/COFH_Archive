package cofh.thermaldynamics.debughelper;

import codechicken.lib.raytracer.RayTracer;
import codechicken.lib.util.ItemUtils;
import cofh.lib.util.BlockPosition;
import cofh.lib.util.helpers.MathHelper;
import cofh.thermaldynamics.block.TileTDBase;
import cofh.thermaldynamics.init.TDBlocks;
import com.google.common.base.Throwables;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

public class CommandThermalDebug extends CommandBase {

	@Override
	public String getCommandName() {

		return "debug";
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender p_71519_1_) {

		return true;
	}

	@Override
	public String getCommandUsage(ICommandSender p_71518_1_) {

		return "debug";
	}

	Random rand = new Random();

	Field chunksToUnload;

	private static final String[] trueWords = { "true", "t", "1", "yes", "oui", "affirmative", "truth", "yarp", "uhuh", "yep", "doit", "yea", "tango", "heckyeah", "win" };
	private static final String[] falseWords = { "false", "f", "0", "no", "non", "negative", "cake", "narp", "nuhuh", "nope", "dont", "nay", "foxtrot", "hellno", "fail" };
	private static final String[] mixWords = { "random", "r", "0.5", "imfeelinglucky", "yesno", "supriseme", "whatever", "schrodinger" };

	public static boolean textToBoolean(String s) {

		s = s.trim();
		for (String trueWord : trueWords) {
			if (trueWord.equalsIgnoreCase(s)) {
				return true;
			}
		}
		for (String falseWord : falseWords) {
			if (falseWord.equalsIgnoreCase(s)) {
				return false;
			}
		}

		for (String mixWord : mixWords) {
			if (mixWord.equalsIgnoreCase(s)) {
				return MathHelper.RANDOM.nextBoolean();
			}
		}

		throw new RuntimeException("Unable to interpret word " + s + " as true/false");
	}

	public String randString() {

		StringBuilder builder = new StringBuilder("rand_");
		int z = MathHelper.RANDOM.nextInt(10) + 1;
		for (int i = 0; i < z; i++) {
			builder.append((char) ('a' + MathHelper.RANDOM.nextInt(26)));
		}
		return builder.toString();
	}

	public static volatile boolean serverOverclock = false;

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

		if (args.length == 0) {
			return;
		}

		if ("overclock".equals(args[0])) {
			serverOverclock = !serverOverclock;
			sender.addChatMessage(new TextComponentString("Server Overclock = " + serverOverclock));
		}

		if ("lag".equals(args[0])) {
			if (args.length == 1) {
				DebugTickHandler.lag = 0;
			} else {
				DebugTickHandler.lag = (long) (parseDouble(args[1]) * 1000 * 1000);
			}
		}

		if ("longRange".equals(args[0])) {

			if (!(sender instanceof EntityPlayerMP)) {
				return;
			}

			EntityPlayerMP playerMP = (EntityPlayerMP) sender;
			BlockPosition pos = new BlockPosition((int) Math.floor(playerMP.posX), (int) Math.floor(playerMP.posY) - 5, (int) Math.floor(playerMP.posZ));

			final World world = playerMP.getEntityWorld();

			pos.setOrientation(EnumFacing.NORTH);

			int n = Integer.valueOf(args[1]);

			for (int i = 0; i < n; i++) {
				world.setBlockState(pos.pos(), TDBlocks.blockDuct[4].getStateFromMeta(1), 3);
				pos.getTileEntity(world, TileTDBase.class).blockPlaced();
				pos.moveForwards(1);
			}

			for (int i = 0; i < 4; i++) {
				world.setBlockState(pos.pos(), TDBlocks.blockDuct[4].getStateFromMeta(1), 3);
				pos.getTileEntity(world, TileTDBase.class).blockPlaced();
				pos.moveRight(1);
			}

			for (int i = 0; i < n; i++) {
				if (!world.setBlockState(pos.pos(), TDBlocks.blockDuct[4].getStateFromMeta(1), 3)) {
					world.setBlockState(pos.pos(), TDBlocks.blockDuct[4].getStateFromMeta(1), 3);
				}
				pos.getTileEntity(world, TileTDBase.class).blockPlaced();
				pos.moveBackwards(1);
			}

			return;
		}

		if ("addRandNBT".equals(args[0])) {
			if (!(sender instanceof EntityPlayerMP)) {
				return;
			}

			EntityPlayerMP player = (EntityPlayerMP) sender;

			ItemStack heldItem = ItemUtils.getHeldStack(player);
			if (heldItem == null) {
				return;
			}

			heldItem.setStackDisplayName(randString());
			for (int j = 0; j < 4; j++) {
				NBTTagCompound tag = new NBTTagCompound();
				for (int i = 0; i < 5; i++) {
					tag.setString(randString(), randString());
				}
				for (int i = 0; i < 5; i++) {
					tag.setInteger(randString(), MathHelper.RANDOM.nextInt());
				}
				heldItem.getTagCompound().setTag(randString(), tag);
			}

			NBTTagCompound tag = heldItem.getTagCompound();
			for (int i = 0; i < 5; i++) {
				tag.setString(randString(), randString());
			}
			for (int i = 0; i < 5; i++) {
				tag.setInteger(randString(), MathHelper.RANDOM.nextInt());
			}

			if (MathHelper.RANDOM.nextInt(4) == 0) {
				tag.setTag("ench", new NBTTagCompound());
			}

			player.updateHeldItem();

		} else if ("showLoading".equals(args[0])) {
			DebugTickHandler.showLoading = !DebugTickHandler.showLoading;
		} else if ("unload".equals(args[0])) {
			if (!(sender instanceof EntityPlayerMP)) {
				return;
			}

			if (chunksToUnload == null) {
				chunksToUnload = ReflectionHelper.findField(ChunkProviderServer.class, "chunksToUnload");
			}

			EntityPlayerMP player = (EntityPlayerMP) sender;
			RayTraceResult trace = RayTracer.retrace(player, 100);
			Chunk chunk = player.worldObj.getChunkFromBlockCoords(trace.getBlockPos());
			Set<Long> o;
			try {
				// noinspection unchecked
				o = (Set<Long>) chunksToUnload.get(player.getServerWorld().getChunkProvider());
			} catch (IllegalAccessException e) {
				throw Throwables.propagate(e);
			}

			o.add(ChunkPos.asLong(chunk.xPosition, chunk.zPosition));
		} else if ("grids".equals(args[0])) {
			DebugTickHandler.showParticles = !DebugTickHandler.showParticles;
		} else if ("generate".equals(args[0]) && args.length == 2) {
			if (!(sender instanceof EntityPlayerMP)) {
				return;
			}

			EntityPlayerMP playerMP = (EntityPlayerMP) sender;
			BlockPosition pos = new BlockPosition((int) Math.floor(playerMP.posX), (int) Math.floor(playerMP.posY) - 5, (int) Math.floor(playerMP.posZ));

			final World world = playerMP.getEntityWorld();
			if (pos.getBlock(world) != Blocks.AIR) {
				return;
			}

			pos.setOrientation(EnumFacing.NORTH);

			LinkedList<BlockPosition> positions = new LinkedList<BlockPosition>();

			int n = Integer.valueOf(args[1]);

			for (int i = 0; i < n; i++) {
				if (rand.nextInt(20) == 0) {
					positions.add(pos.copy());
				}

				world.setBlockState(pos.pos(), TDBlocks.blockDuct[2].getStateFromMeta(0), 3);

				pos.getTileEntity(world, TileTDBase.class).blockPlaced();

				if (rand.nextInt(4) == 0) {
					pos.setOrientation(pos.orientation = rand.nextBoolean() ? EnumFacing.UP : EnumFacing.DOWN);
				}
				pos.moveForwards(1);
			}

			for (@SuppressWarnings ("unused") BlockPosition p : positions) {
				// world.setBlock(p.x, p.y + 1, p.z, Blocks.chest, 0, 3);
			}
		}
	}

}

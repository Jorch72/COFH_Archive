//package cofh.thermalfoundation.plugins.mfr;
//
//import cofh.lib.util.helpers.DamageHelper;
//
//import net.minecraft.entity.EntityLivingBase;
//import net.minecraft.nbt.NBTTagCompound;
//import net.minecraft.util.DamageSource;
//
//import powercrystals.minefactoryreloaded.api.ILiquidDrinkHandler;
//
//public class DrinkHandlerPyrotheum implements ILiquidDrinkHandler {
//
//	public static DrinkHandlerPyrotheum instance = new DrinkHandlerPyrotheum();
//
//	@Override
//	public void onDrink(EntityLivingBase player) {
//
//		player.attackEntityFrom(new InternalPyrotheumDamage(), 15);
//		player.setFire(60);
//		NBTTagCompound tag = player.getEntityData();
//		tag.setLong("drankLavaTime", player.worldObj.getTotalWorldTime());
//	}
//
//	protected class InternalPyrotheumDamage extends DamageSource {
//
//		public InternalPyrotheumDamage() {
//
//			super(DamageHelper.pyrotheum.damageType);
//			this.setDamageBypassesArmor();
//			this.setFireDamage();
//			this.setDifficultyScaled();
//		}
//	}
//
//}

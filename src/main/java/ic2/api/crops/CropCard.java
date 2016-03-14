package ic2.api.crops;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Base agriculture crop.
 *
 * Any crop extending this can be registered using registerCrop to be added into the game.
 */
public abstract class CropCard {
	public CropCard() {
		modId = getModId(); // initialize mod id while we should be in the real owner's init event
	}

	/**
	 * Plant name for identifying this crop within your mod.
	 *
	 * The name has to be unique within the mod and is used for saving.
	 * By default this name will be also used to determine displayKey() and registerSprites().
	 *
	 * @note changing name or owner will cause existing crops in users' worlds to disappear.
	 *
	 * @return Plant name
	 */
	public abstract String name();

	/**
	 * Determine the mod id owning this crop.
	 *
	 * The owner serves as a name space. With every mod using a different owner, a mod only has to
	 * make sure it doesn't have conflicts with name() in itself.
	 * It's recommended to hard code this to your mod id as specified in the @Mod annotation.
	 * Do not use IC2's mod id here.
	 *
	 * @note changing name or owner will cause existing crops in users' worlds to disappear.
	 *
	 * @return Mod id.
	 */
	public String owner() { // TODO: make abstract
		return modId;
	}

	/**
	 * Translation key for display to the player.
	 *
	 * It's highly recommended to specify a valid key from your language file here, e.g. add
	 * "yourmod.crop.yourCropName = Your crop's name" to the language file, override name() to
	 * return "yourCropName" and override displayName() to return "yourmod.crop."+name().
	 *
	 * @return Unlocalized name.
	 */
	public String displayName() {
		return name(); // return the raw name for backwards compatibility
	}

	/**
	 * Your name here, will be shown in "Discovered by:" when analyzing seeds.
	 *
	 * @return Your name
	 */
	public String discoveredBy() {
		return "unknown";
	}

	/**
	 * Description of your plant. Keep it short, a few characters per line for up to two lines.
	 * Default is showing attributes of your plant, 2 per line.
	 *
	 * @param i line to get, starting from 0
	 * @return The line
	 */
	public String desc(int i) {
		String[] att = attributes();

		if (att == null || att.length == 0) return "";

		if (i == 0) {
			String s = att[0];
			if (att.length >= 2) {
				s+=", "+att[1];
				if (att.length >= 3) s+=",";
			}
			return s;
		}
		if (att.length < 3) return "";
		String s = att[2];
		if (att.length >= 4) s+=", "+att[3];
		return s;
	}

	/**
	 * *
	 * @param crop reference to ICropTile
	 * @return roots lengt use in isBlockBelow
	 */
	public int getrootslength(ICropTile crop) { // TODO: camel case
		return 1;
	}

	/**
	 * Tier of the plant. Ranges from 1 to 16, 0 is Weed.
	 * Valuable and powerful crops have higher tiers, useless and weak ones have lower tiers.
	 *
	 * @return Tier
	 */
	public abstract int tier();

	/**
	 * Describe the plant through a set of stats, influencing breeding.
	 * Plants sharing stats and attributes will tend to cross-breed more often.
	 *
	 * Stats:
	 * - 0: Chemistry (Industrial uses based on chemical plant components)
	 * - 1: Consumable (Food, potion ingredients, stuff meant to be eaten or similarly used)
	 * - 2: Defensive (Plants with defense capabilities (damaging, explosive, chemical) or special abilities in general)
	 * - 3: Colorful (How colorful/aesthetically/beautiful is the plant, like dye-plants or plants without actual effects)
	 * - 4: Weed (Is this plant weed-like and rather unwanted/quick-spreading? Rare super-breed plants should have low values here)
	 *
	 * @param n index of the requested stat
	 * @return The requested value of the stats
	 */
	public abstract int stat(int n); // TODO: change to fixed property object or builder with other attributes like tier

	/**
	 * Additional attributes of the plant, also influencing breeding.
	 * Plants sharing stats and attributes will tend to cross-breed more often.
	 *
	 * @return Attributes as an array of strings
	 */
	public abstract String[] attributes(); // TODO: default to none

	/**
	 * Determine the max crop size.
	 *
	 * Currently only used for texture allocation.
	 */
	public abstract int maxSize();

	/**
	 * Instantiate your Icons here.
	 *
	 * This method will get called by IC2, don't call it yourself.
	 *
	 * It's highly recommended to use your own assert domain here, e.g. yourmod:crop/* instead of
	 * ic2:crop/*, which will then read assets/yourmod/textures/blocks/crop/*.png.
	 */
	@SideOnly(Side.CLIENT)
	public void registerSprites(IIconRegister iconRegister) {
		textures = new IIcon[maxSize()];

		for (int i = 1; i <= textures.length; i++) {
			// ic2:crop/blockCrop.NAME.n is the legacy name for backwards compatiblity
			textures[i - 1] = iconRegister.registerIcon("ic2:crop/blockCrop."+name()+"."+i);
		}
	}

	/**
	 * Sprite the crop is meant to be rendered with.
	 *
	 * @param crop reference to ICropTile
	 * @return 0-255, representing the sprite index on the crop's spritesheet.
	 */
	@SideOnly(Side.CLIENT)
	public IIcon getSprite(ICropTile crop) {
		if (crop.getSize() <= 0 || crop.getSize() > textures.length) return null;

		return textures[crop.getSize() - 1];
	}

	/**
	 * Amount of growth points needed to increase the plant's size.
	 * Default is 200 * tier.
	 */
	public int growthDuration(ICropTile crop) {
		return tier() * 200;
	}

	/**
	 * Check whether the plant can grow further.
	 *
	 * Consider:
	 * - Humidity, nutrients and air quality
	 * - Current size
	 * - Light level
	 * - Special biomes or conditions, accessible through crop.worldObj
	 *
	 * This method will be called upon empty upgraded crops to check whether a neighboring plant can cross onto it! Don't check if the size is greater than 0 and if the ID is real.
	 *
	 * @param crop reference to ICropTile
	 * @return Whether the crop can grow
	 */
	public abstract boolean canGrow(ICropTile crop);

	/**
	 * Calculate the influence for the plant to grow based on humidity, nutrients and air.
	 * Normal behavior is rating the three stats "normal", with each of them ranging from 0-30.
	 * Basic rule: Assume everything returns 10. All together must equal 30. Add the factors to your likings, for example (humidity*0.7)+(nutrients*0.9)+(air*1.4)
	 *
	 * Default is humidity + nutrients + air (no factors).
	 *
	 * @param crop reference to ICropTile
	 * @param humidity ground humidity, influenced by hydration
	 * @param nutrients nutrient quality in ground, based on fertilizers
	 * @param air air quality, influences by open gardens and less crops surrounding this one
	 * @return 0-30
	 */
	public int weightInfluences(ICropTile crop, float humidity, float nutrients, float air) {
		return (int) (humidity + nutrients + air);
	}

	/**
	 * Used to determine whether the plant can crossbreed with another crop.
	 * Default is allow crossbreeding if the size is greater or equal than 3.
	 *
	 * @param crop crop to crossbreed with
	 */
	public boolean canCross(ICropTile crop) {
		return crop.getSize() >= 3;
	}


	/**
	 * Called when the plant is rightclicked by a player.
	 * Default action is harvesting.
	 *
	 * Only called Serverside.
	 *
	 * @param crop reference to ICropTile
	 * @param player player rightclicking the crop
	 * @return Whether the plant has changed
	 */
	public boolean rightclick(ICropTile crop, EntityPlayer player) {
		return crop.harvest(true);
	}

	/**
	 * Use in Crop Havester with insert Cropnalyzer to get best Output.
	 *
	 * @param crop reference to ICropTile
	 * @return  need crop  size for best output.
	 */

	public abstract int getOptimalHavestSize(ICropTile crop); // TODO: default to maxSize

	/**
	 * Check whether the crop can be harvested.
	 *
	 * @param crop reference to ICropTile
	 * @return Whether the crop can be harvested in its current state.
	 */
	public abstract boolean canBeHarvested(ICropTile crop); // TODO: default to maxSize check

	/**
	 * Base chance for dropping the plant's gains, specify values greater than 1 for multiple drops.
	 * Default is 0.95^tier.
	 *
	 * @return Chance to drop the gains
	 */
	public float dropGainChance() { // TODO: change to double
		return (float) Math.pow(0.95, tier());
	}

	/**
	 * Item obtained from harvesting the plant.
	 *
	 * @param crop reference to ICropTile
	 * @return Item obtained
	 */
	public abstract ItemStack getGain(ICropTile crop);

	/**
	 * Get the size of the plant after harvesting.
	 * Default is 1.
	 *
	 * @param crop reference to ICropTile
	 * @return Plant size after harvesting
	 */
	public byte getSizeAfterHarvest(ICropTile crop) {return 1;} // TODO: change to int


	/**
	 * Called when the plant is left clicked by a player.
	 * Default action is picking the plant.
	 *
	 * Only called server side.
	 *
	 * @param crop reference to ICropTile
	 * @param player player left clicked the crop
	 * @return Whether the plant has changed
	 */
	public boolean leftclick(ICropTile crop, EntityPlayer player) { // TODO: camel case
		return crop.pick(true);
	}

	/**
	 * Base chance for dropping seeds when the plant is picked.
	 * Default is 0.5*0.8^tier with a bigger chance for sizes greater than 2 and absolutely no chance for size 0.
	 *
	 * @param crop reference to ICropTile
	 * @return Chance to drop the seeds
	 */
	public float dropSeedChance(ICropTile crop) {
		if (crop.getSize() == 1) return 0;
		float base = 0.5F;
		if (crop.getSize() == 2) base/=2F;
		for (int i = 0; i < tier(); i++) {base*=0.8;}
		return base;
	}

	/**
	 * Obtain seeds dropped when the plant is picked.
	 * Multiple drops of the returned ItemStack can occur.
	 * Default action is generating a seed from this crop.
	 *
	 * @param crop reference to ICropTile
	 * @return Seeds
	 */
	public ItemStack getSeeds(ICropTile crop) {
		return crop.generateSeeds(crop.getCrop(), crop.getGrowth(), crop.getGain(), crop.getResistance(), crop.getScanLevel());
	}

	/**
	 * Called when a neighbor block to the crop has changed.
	 *
	 * @param crop reference to ICropTile
	 */
	public void onNeighbourChange(ICropTile crop) {
		//
	}

	/**
	 * Check if the crop should emit redstone.
	 *
	 * @return Whether the crop should emit redstone
	 */
	public int emitRedstone(ICropTile crop) {return 0;}

	/**
	 * Called when the crop is destroyed.
	 *
	 * @param crop reference to ICropTile
	 */
	public void onBlockDestroyed(ICropTile crop) {
		//
	}

	/**
	 * Get the light value emitted by the plant.
	 *
	 * @param crop reference to ICropTile
	 * @return Light value emitted
	 */
	public int getEmittedLight(ICropTile crop) {
		return 0;
	}

	/**
	 * Default is true if the entity is an EntityLiving in jumping or sprinting state.
	 *
	 * @param crop reference to ICropTile
	 * @param entity entity colliding
	 * @return Whether trampling calculation should happen, return false if the plant is no longer valid.
	 */
	public boolean onEntityCollision(ICropTile crop, Entity entity) {
		if (entity instanceof EntityLivingBase) {
			return ((EntityLivingBase) entity).isSprinting();
		}

		return false;
	}


	/**
	 * Called every time the crop ticks.
	 * Should be called every 256 ticks or around 13 seconds.
	 *
	 * @param crop reference to ICropTile
	 */
	public void tick(ICropTile crop) {
		// nothing by default
	}

	/**
	 * Check whether this plant spreads weed to surrounding tiles.
	 * Default is true if the plant has a high growth stat (or is weeds) and size greater or equal than 2.
	 *
	 * @param crop reference to ICropTile
	 * @return Whether the plant spreads weed
	 */
	public boolean isWeed(ICropTile crop) {
		return crop.getSize() >= 2 &&
				(crop.getCrop() == Crops.weed || crop.getGrowth() >= 24);
	}


	/**
	 * Get this plant's ID.
	 *
	 * @return ID of this CropCard or -1 if it's not registered
	 * @deprecated IDs aren't used anymore.
	 */
	@Deprecated
	public final int getId() {
		return Crops.instance.getIdFor(this);
	}

	private static String getModId() {
		ModContainer modContainer = Loader.instance().activeModContainer();

		if (modContainer != null) {
			return modContainer.getModId();
		} else {
			// this is bad if you are not actually IC2
			assert false;

			return "unknown";
		}
	}

	@SideOnly(Side.CLIENT)
	protected IIcon textures[];

	private final String modId; // TODO: make owner abstract, remove modId auto detection
	// TODO: add a clean way to obtain world reference and position
}

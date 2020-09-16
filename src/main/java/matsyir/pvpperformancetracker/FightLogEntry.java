package matsyir.pvpperformancetracker;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.awt.Color;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;

import lombok.Getter;
import net.runelite.api.GraphicID;
import net.runelite.api.HeadIcon;
import net.runelite.api.Player;
import net.runelite.client.chat.ChatMessageBuilder;
import org.apache.commons.text.WordUtils;

// A fight log entry for a single Fighter. Will be saved in a List of FightLogEntries in the Fighter class.
@Getter
public class FightLogEntry implements Comparable<FightLogEntry>
{
	public static final NumberFormat nf;
	static
	{
		nf = NumberFormat.getInstance();
		nf.setRoundingMode(RoundingMode.HALF_UP);
		nf.setMaximumFractionDigits(2);
	}

	// general data
	// don't expose attacker name since it is present in the parent class (Fighter), so it is
	// redundant use of storage
	public String attackerName;
	@Expose
	@SerializedName("t")
	private long time;


	// attacker data
	@Expose
	@SerializedName("G")
	// current attacker's gear. The attacker is not necessarily the competitor.
	// Set using PlayerComposition::getEquipmentIds
	private int[] attackerGear;
	@Expose
	@SerializedName("O")
	private HeadIcon attackerOverhead;
	@Expose
	@SerializedName("m") // m because movement?
	private AnimationData animationData;
	@Expose
	@SerializedName("d")
	private double deservedDamage;
	@Expose
	@SerializedName("a")
	private double accuracy;
	@Expose
	@SerializedName("h") // h for highest hit
	private int maxHit;
	@Expose
	@SerializedName("l") // l for lowest hit
	private int minHit;
	@Expose
	@SerializedName("s")
	private boolean splash; // true if it was a magic attack and it splashed

	// defender data
	@Expose
	@SerializedName("g")
	private int[] defenderGear;
	@Expose
	@SerializedName("o")
	private HeadIcon defenderOverhead;

	@Expose
	@SerializedName("p")
	private int attackerOffensivePray; // offensive pray saved as SpriteID since that's all we use it for.

	public FightLogEntry(Player attacker, Player defender, PvpDamageCalc pvpDamageCalc, int attackerOffensivePray)
	{
		this.attackerName = attacker.getName();
		this.attackerGear = attacker.getPlayerComposition().getEquipmentIds();
		this.attackerOverhead = attacker.getOverheadIcon();
		this.animationData = AnimationData.dataForAnimation(attacker.getAnimation());
		this.deservedDamage = pvpDamageCalc.getAverageHit();
		this.accuracy = pvpDamageCalc.getAccuracy();
		this.minHit = pvpDamageCalc.getMinHit();
		this.maxHit = pvpDamageCalc.getMaxHit();
		this.splash = animationData.attackStyle == AnimationData.AttackStyle.MAGIC && defender.getGraphic() == GraphicID.SPLASH;
		this.time = Instant.now().toEpochMilli();

		this.defenderGear = defender.getPlayerComposition().getEquipmentIds();
		this.defenderOverhead = defender.getOverheadIcon();
		this.attackerOffensivePray = attackerOffensivePray;
	}

	public FightLogEntry(int [] attackerGear, int deservedDamage, double accuracy, int minHit, int maxHit, int [] defenderGear, String attackerName)
	{
		this.attackerName = attackerName;
		this.attackerGear = attackerGear;
		this.attackerOverhead = HeadIcon.MAGIC;
		this.animationData = Math.random() <= 0.5 ? AnimationData.MELEE_DAGGER_SLASH : AnimationData.MAGIC_ANCIENT_MULTI_TARGET;
		this.deservedDamage = deservedDamage;
		this.accuracy = accuracy;
		this.minHit = minHit;
		this.maxHit = maxHit;
		this.splash = Math.random() >= 0.5;
		this.time = Instant.now().toEpochMilli();
		this.defenderGear = defenderGear;
		this.defenderOverhead = HeadIcon.MAGIC;
	}


	public boolean success()
	{
		return animationData.attackStyle.getProtection() != defenderOverhead;
	}

	public String toChatMessage()
	{
		Color darkRed = new Color(127, 0, 0); // same color as default clan chat color
		return new ChatMessageBuilder()
			.append(darkRed, attackerName + ": ")
			.append(Color.BLACK, "Style: ")
			.append(darkRed, WordUtils.capitalizeFully(animationData.attackStyle.toString()))
			.append(Color.BLACK, "  Hit: ")
			.append(darkRed, getHitRange())
			.append(Color.BLACK, "  Acc: ")
			.append(darkRed, nf.format(accuracy))
			.append(Color.BLACK, "  AvgHit: ")
			.append(darkRed, nf.format(deservedDamage))
			.append(Color.BLACK, " Spec?: ")
			.append(darkRed, animationData.isSpecial ? "Y" : "N")
			.append(Color.BLACK, " OffP?:")
			.append(darkRed, success() ? "Y" : "N")
			.build();
	}

	String getHitRange()
	{
		return minHit + "-" + maxHit;
	}


	// use to sort by last fight time, to sort fights by date/time.
	@Override
	public int compareTo(FightLogEntry o)
	{
		long diff = time - o.time;

		// if diff = 0, return 0. Otherwise, divide diff by its absolute value. This will result in
		// -1 for negative numbers, and 1 for positive numbers, keeping the sign and a safely small int.
		return diff == 0 ? 0 :
			(int)(diff / Math.abs(diff));
	}
}

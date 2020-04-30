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
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.QueuedMessage;
import org.apache.commons.text.WordUtils;

// A fight log entry for a single Fighter. Will be saved in a List of FightLogEntries in the Fighter class.
@Getter
public class FightLogEntry
{
	public static final NumberFormat nf;
	static
	{
		nf = NumberFormat.getInstance();
		nf.setRoundingMode(RoundingMode.HALF_UP);
		nf.setMaximumFractionDigits(2);
	}

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
	@SerializedName("t")
	private long time;
	@Expose
	@SerializedName("n")
	private String attackerName;

	public FightLogEntry(Player attacker, Player defender, PvpDamageCalc pvpDamageCalc)
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
	}

	public FightLogEntry(int [] attackerGear, int deservedDamage, double accuracy, int minHit, int maxHit, int [] defenderGear, String attackerName)
	{
		this.attackerName = attackerName;
		this.attackerGear = attackerGear;
		this.attackerOverhead = HeadIcon.MAGIC;
		this.animationData = Math.random() >= 0.5 ? AnimationData.MELEE_DAGGER_SLASH : AnimationData.MAGIC_ANCIENT_MULTI_TARGET;
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

	public String toChatMessage(String name)
	{
		return new ChatMessageBuilder()
			.append(Color.BLACK, name + ": ")
			.append(Color.GRAY, "Style: ")
			.append(Color.BLACK, WordUtils.capitalizeFully(animationData.attackStyle.toString()))
			.append(Color.GRAY, "  Hit: ")
			.append(Color.BLACK, getHitRange())
			.append(Color.GRAY, "  Acc: ")
			.append(Color.BLACK, nf.format(accuracy))
			.append(Color.GRAY, "  AvgHit: ")
			.append(Color.BLACK, nf.format(deservedDamage))
			.append(Color.GRAY, " Spec?: ")
			.append(Color.BLACK, animationData.isSpecial ? "Y" : "N")
			.append(Color.GRAY, " OffP?:")
			.append(Color.BLACK, success() ? "Y" : "N")
			.build();
	}

	String getHitRange()
	{
		return minHit + "-" + maxHit;
	}
}

package matsyir.pvpperformancetracker.models;

import lombok.Getter;
import net.runelite.api.events.HitsplatApplied;

/**
 * Helper class to store a HitsplatApplied event along with the
 * opponent's health ratio/scale polled shortly after the event occurred.
 */
public class HitsplatInfo
{
	@Getter
	private final HitsplatApplied event;

	// health ratio/scale at the time of the hitsplat
	@Getter
	private int healthRatio = -1;
	@Getter
	private int healthScale = -1;

	public HitsplatInfo(HitsplatApplied event)
	{
		this.event = event;
	}

	// Called to store the HP state
	public void setHp(int ratio, int scale)
	{
		this.healthRatio = ratio;
		this.healthScale = scale;
	}
} 
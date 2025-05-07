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

	// Use volatile as these will be updated by a scheduled executor thread
	@Getter
	private volatile int polledHealthRatio = -1;
	@Getter
	private volatile int polledHealthScale = -1;

	public HitsplatInfo(HitsplatApplied event)
	{
		this.event = event;
	}

	// Called by the delayed task to store the polled HP state
	public void setPolledHp(int ratio, int scale)
	{
		this.polledHealthRatio = ratio;
		this.polledHealthScale = scale;
	}
} 
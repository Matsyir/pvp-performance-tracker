package matsyir.pvpperformancetracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PvpPerformanceTrackerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PvpPerformanceTrackerPlugin.class);
		RuneLite.main(args);
	}
}
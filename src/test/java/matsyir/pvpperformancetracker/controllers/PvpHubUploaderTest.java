package matsyir.pvpperformancetracker.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import matsyir.pvpperformancetracker.PvpPerformanceTrackerConfig;
import matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PvpHubUploaderTest
{
	private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

	@Test
	public void uploadIncludesEstimatedAttackerHp()
	{
		PvpPerformanceTrackerPlugin.CONFIG = new PvpPerformanceTrackerConfig()
		{
		};
		FightLogEntry entry = new FightLogEntry(new int[12], 25, 0.75, 0, 50, new int[12], "opponent");
		entry.setAttackerEstimatedHp(42);

		FightPerformance fight = new FightPerformance();
		fight.competitor = new Fighter("competitor");
		fight.opponent = new Fighter("opponent");
		fight.opponent.getFightLogEntries().add(entry);

		JsonObject upload = GSON.fromJson(PvpHubUploader.serializeFightUpload(fight, GSON, 0), JsonObject.class);
		JsonObject uploadedEntry = upload.getAsJsonObject("o").getAsJsonArray("l").get(0).getAsJsonObject();

		assertEquals(42, uploadedEntry.get("aH").getAsInt());
		assertTrue(GSON.toJson(fight).contains("\"aH\":42"));
	}
}

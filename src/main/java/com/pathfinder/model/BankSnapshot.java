package com.pathfinder.model;

import java.util.Collections;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BankSnapshot
{
	long coins;
	long platinumTokens;
	long totalLiquidCash;
	long estimatedTotalValue;
	long timestamp;

	@Builder.Default
	Set<Integer> ownedItemIds = Collections.emptySet();

	public boolean hasScanned()
	{
		return timestamp > 0;
	}

	public boolean ownsItem(int itemId)
	{
		return ownedItemIds.contains(itemId);
	}

	public static String formatGpValue(long gp)
	{
		if (gp >= 1_000_000_000L)
		{
			return String.format("%.1fB", gp / 1_000_000_000.0);
		}
		if (gp >= 1_000_000L)
		{
			return String.format("%.1fM", gp / 1_000_000.0);
		}
		if (gp >= 1_000L)
		{
			return String.format("%.1fK", gp / 1_000.0);
		}
		return String.valueOf(gp);
	}
}

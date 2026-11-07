package com.pathfinder.model;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Recommendation
{
	public enum Status
	{
		READY,
		ALMOST_READY,
		LOCKED
	}

	Activity activity;
	Status status;
	List<String> missingRequirements;
	int missingLevelSum;
	int score;
}

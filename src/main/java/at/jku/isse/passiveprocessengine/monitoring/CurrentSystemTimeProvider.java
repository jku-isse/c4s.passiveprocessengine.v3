package at.jku.isse.passiveprocessengine.monitoring;

import java.time.OffsetDateTime;

public class CurrentSystemTimeProvider implements ITimeStampProvider{

	@Override
	public OffsetDateTime getLastChangeTimeStamp() {
		return OffsetDateTime.now();
	}

}

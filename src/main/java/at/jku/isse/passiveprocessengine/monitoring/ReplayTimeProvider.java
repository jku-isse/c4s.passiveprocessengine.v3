package at.jku.isse.passiveprocessengine.monitoring;

import java.time.OffsetDateTime;

public class ReplayTimeProvider implements ITimeStampProvider{

	private OffsetDateTime lastChange;
	
	@Override
	public OffsetDateTime getLastChangeTimeStamp() {
		return lastChange;
	}
	
	public void setLastChangeTimeStamp(OffsetDateTime timestamp) {
		this.lastChange = timestamp;
	}

}

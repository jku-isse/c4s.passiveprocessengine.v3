package at.jku.isse.passiveprocessengine.monitoring;

import java.time.OffsetDateTime;

public interface ITimeStampProvider {

	public OffsetDateTime getLastChangeTimeStamp();
}

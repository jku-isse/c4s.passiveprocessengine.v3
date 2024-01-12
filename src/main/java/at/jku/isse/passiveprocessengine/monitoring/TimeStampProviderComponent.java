package at.jku.isse.passiveprocessengine.monitoring;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

@Component
public class TimeStampProviderComponent {

	final ITimeStampProvider provider;
	
	public TimeStampProviderComponent(ITimeStampProvider provider) {
		this.provider = provider;
	}
	
	public OffsetDateTime getLastChangeTimeStamp() {
		return provider.getLastChangeTimeStamp();
	}
	
}

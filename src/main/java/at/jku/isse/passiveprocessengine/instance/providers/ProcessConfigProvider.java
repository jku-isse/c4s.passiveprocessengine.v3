package at.jku.isse.passiveprocessengine.instance.providers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import at.jku.isse.designspace.artifactconnector.core.repository.ArtifactIdentifier;
import at.jku.isse.designspace.artifactconnector.core.repository.FetchResponse;
import at.jku.isse.designspace.artifactconnector.core.repository.FetchResponse.ErrorResponse;
import at.jku.isse.designspace.artifactconnector.core.repository.FetchResponse.SuccessResponse;
import at.jku.isse.designspace.artifactconnector.core.repository.IArtifactProvider;
import at.jku.isse.passiveprocessengine.core.InstanceRepository;
import at.jku.isse.passiveprocessengine.core.PPEInstance;
import at.jku.isse.passiveprocessengine.core.PPEInstanceType;
import at.jku.isse.passiveprocessengine.core.SchemaRegistry;
import at.jku.isse.passiveprocessengine.instance.types.ProcessConfigBaseElementType;

public class ProcessConfigProvider implements IArtifactProvider {

	private final SchemaRegistry schemaReg;
	private final InstanceRepository ws;
	
	public ProcessConfigProvider(SchemaRegistry schemaReg, InstanceRepository ws) {
		this.schemaReg = schemaReg;
		this.ws = ws;
	}
	
	@Override
	public Map<PPEInstanceType, List<String>> getSupportedIdentifiers() {
		return Map.of(getDefaultArtifactInstanceType(), List.of("Designspace Id"));
	}
	
	@Override
	public PPEInstanceType getDefaultArtifactInstanceType() {
		return schemaReg.getTypeByName(ProcessConfigBaseElementType.typeId);
	}

	@Override
	public Set<PPEInstanceType> getProvidedArtifactInstanceTypes() {
		return Set.of(schemaReg.getTypeByName(ProcessConfigBaseElementType.typeId));				
	}

	@Override
	public Set<FetchResponse> fetchArtifact(Set<ArtifactIdentifier> artifactIdentifiers) {
		return artifactIdentifiers.stream()
			.map(id -> {
				Optional<PPEInstance> optInst = ws.findInstanceyById(id.getId());
				if (optInst.isEmpty()) {
					return new ErrorResponse("No ProcessConfig found for id: "+Objects.toString(id));
				} else {
					return new SuccessResponse(optInst.get());
				}
			})
			.collect(Collectors.toSet());
	}

	@Override
	public Set<FetchResponse> forceFetchArtifact(Set<ArtifactIdentifier> artifactIdentifiers) {
		return fetchArtifact(artifactIdentifiers);
	}


	
}

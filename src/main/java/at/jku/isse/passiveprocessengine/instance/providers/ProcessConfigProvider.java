package at.jku.isse.passiveprocessengine.instance.providers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
 
import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstance;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.instance.types.ProcessConfigBaseElementType;
import at.jku.isse.passiveprocessengine.rdfwrapper.artifactprovider.ArtifactIdentifier;
import at.jku.isse.passiveprocessengine.rdfwrapper.artifactprovider.FetchResponse;
import at.jku.isse.passiveprocessengine.rdfwrapper.artifactprovider.FetchResponse.ErrorResponse;
import at.jku.isse.passiveprocessengine.rdfwrapper.artifactprovider.FetchResponse.SuccessResponse;
import at.jku.isse.passiveprocessengine.rdfwrapper.artifactprovider.IArtifactProvider;

public class ProcessConfigProvider implements IArtifactProvider {

	private final NodeToDomainResolver schemaReg;

	
	public ProcessConfigProvider(NodeToDomainResolver schemaReg) {
		this.schemaReg = schemaReg;

	}
	
	@Override
	public Map<RDFInstanceType, List<String>> getSupportedIdentifiers() {
		return Map.of(getDefaultArtifactInstanceType(), List.of("Designspace Id"));
	}
	
	@Override
	public RDFInstanceType getDefaultArtifactInstanceType() {
		return schemaReg.findNonDeletedInstanceTypeByFQN(ProcessConfigBaseElementType.typeId).orElse(null);
	}

	@Override
	public Set<RDFInstanceType> getProvidedArtifactInstanceTypes() {
		var type = schemaReg.findNonDeletedInstanceTypeByFQN(ProcessConfigBaseElementType.typeId);
		if (type.isEmpty()) 
			return Collections.emptySet();
		else 
			return Set.of(type.get());				
	}

	@Override
	public Set<FetchResponse> fetchArtifact(Set<ArtifactIdentifier> artifactIdentifiers) {
		return artifactIdentifiers.stream()
			.map(id -> {
				Optional<RDFInstance> optInst = schemaReg.findInstanceById(id.getId());
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

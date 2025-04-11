package at.jku.isse.passiveprocessengine.core;

import at.jku.isse.passiveprocessengine.rdfwrapper.NodeToDomainResolver;
import at.jku.isse.passiveprocessengine.rdfwrapper.PrimitiveTypesFactory;
import at.jku.isse.passiveprocessengine.rdfwrapper.RDFInstanceType;
import at.jku.isse.passiveprocessengine.rdfwrapper.metaschema.MetaElementFactory;


public abstract class AbstractTypeProvider {

	protected final NodeToDomainResolver schemaRegistry;
	protected final PrimitiveTypesFactory primitives;
	protected final MetaElementFactory metaElements;
	protected RDFInstanceType type;
	
	protected AbstractTypeProvider(NodeToDomainResolver schemaRegistry) {
		super();
		this.schemaRegistry = schemaRegistry;
		this.primitives = schemaRegistry.getMetaschemata().getPrimitiveTypesFactory();
		this.metaElements = schemaRegistry.getMetaschemata().getMetaElements();
	}	
	
}

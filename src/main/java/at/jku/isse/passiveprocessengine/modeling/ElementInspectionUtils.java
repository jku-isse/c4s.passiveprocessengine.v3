package at.jku.isse.passiveprocessengine.modeling;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import at.jku.isse.designspace.core.model.CollectionProperty;
import at.jku.isse.designspace.core.model.Instance;
import at.jku.isse.designspace.core.model.InstanceType;
import at.jku.isse.designspace.core.model.MapProperty;
import at.jku.isse.designspace.core.model.Property;
import at.jku.isse.designspace.core.model.PropertyType;
import at.jku.isse.designspace.core.model.SingleProperty;
import at.jku.isse.designspace.rule.arl.repair.AbstractRepairAction;
import at.jku.isse.designspace.rule.arl.repair.RepairNode;
import at.jku.isse.designspace.rule.arl.repair.RestrictionNode;
import at.jku.isse.designspace.rule.arl.repair.UnknownRepairValue;
import at.jku.isse.designspace.rule.model.ConsistencyRuleType;
import at.jku.isse.designspace.rule.model.Rule;
import at.jku.isse.passiveprocessengine.WrapperCache;
import at.jku.isse.passiveprocessengine.definition.ProcessDefinition;
import at.jku.isse.passiveprocessengine.instance.ConstraintWrapper;
import at.jku.isse.passiveprocessengine.instance.ProcessException;
import at.jku.isse.passiveprocessengine.instance.ProcessInstance;
import at.jku.isse.passiveprocessengine.instance.ProcessStep;
import at.jku.isse.passiveprocessengine.instance.StepLifecycle.Conditions;


public class ElementInspectionUtils {
	
	public static void printInstance(Instance inst, StringBuffer printer) {    	
    			inst.getProperties().stream()
    			.sorted(new PropertyComparator())
    			.forEach(prop -> printer.append(prop.name+": "+propertyToString(prop)+"\r\n"));
    			    	
    }
    
    public static void printInstanceType(InstanceType inst, StringBuffer printer) {
    	inst.getProperties().stream()
		.sorted(new PropertyComparator())
		.forEach(prop -> printer.append(prop.name+": "+propertyToString(prop)+"\r\n"));
    }
    
    private static class PropertyComparator implements Comparator<Property> {

		@Override
		public int compare(Property o1, Property o2) {
			return o1.name.compareTo(o2.name);
		}
    	
    }
    

    
    private static final String propertyToString(Property prop) {
    	if (prop instanceof SingleProperty) {
    		return singleValueToComponent(prop.get());
    	} else     	
    	if (prop instanceof CollectionProperty) {
    		 return collectionValueToComponent((Collection) prop.get());
    	} else
    	if (prop instanceof MapProperty) {
    		// not supported yet
    		return mapValueToComponent(((MapProperty)prop).get());
    	}
    	else return "Unknown Property ";
    }; 
    
    private static String singleValueToComponent(Object value) {
    	if (value instanceof Instance) {
    		Instance inst = (Instance)value;
    		return inst.name();
    	} else if (value instanceof InstanceType) {
        		InstanceType inst = (InstanceType)value;
        		return inst.name();
        } else if (value instanceof PropertyType) {
        	PropertyType pt = (PropertyType)value;
        	return String.format("PropertyType: %s %s of type %s", pt.name(), pt.cardinality(), pt.referencedInstanceType());
        } else
    	return value != null ? value.toString() : "null";
    }
    
    private static String collectionValueToComponent(Collection value) {
    	if (value == null || value.size() == 0)	
    		return "[ ]";
    	else if (value.size() == 1)
    		return singleValueToComponent(value.iterator().next());
    	else {
    		StringBuffer listBuf = new StringBuffer();    		
    		value.stream().forEach(val -> listBuf.append("\r\n   "+singleValueToComponent(val)));
    		return listBuf.toString();
    	}
    }
    
    private static String mapValueToComponent(Map<String, Object> value) {
    	StringBuffer buffMap = new StringBuffer();
    	value.entrySet().stream().forEach(entry -> buffMap.append("\r\n     "+entry.getKey()+": "+mapToString(entry.getValue())));
    	return buffMap.toString();
    }
        
    private static final String mapToString(Object obj) {
    	if (obj instanceof Collection) {
    		return collectionValueToComponent((Collection)obj);
    	} else     	
    	if (obj instanceof Map) {
    		return mapValueToComponent((Map)obj);
    	} else {
    		return singleValueToComponent(obj);
    	}
    	
    };
    
    public static void assertAllConstraintsAreValid(ProcessInstance proc) throws ProcessException{
    	ProcessException pex = new ProcessException("ConstraintErrors");    	
    	proc.getProcessSteps().stream()
    	.peek(td -> System.out.println("Visiting Step: "+td.getName()))
    	.forEach(td -> {
    		td.getDefinition().getInputToOutputMappingRules().entrySet().stream().forEach(entry -> {
    			InstanceType type = td.getInstance().getProperty("crd_datamapping_"+entry.getKey()).propertyType().referencedInstanceType();
    			ConsistencyRuleType crt = (ConsistencyRuleType)type;
    			if (crt.hasRuleError()) {
    				pex.getErrorMessages().add(crt.ruleError());
    			}
    			String eval = (String) crt.ruleEvaluations().get().stream()
    					.map(rule -> ((Rule)rule).result()+"" )
    					.collect(Collectors.joining(",","[","]"));
    			System.out.println("Checking "+crt.name() +" Result: "+ eval);
    		});
    		ProcessDefinition pd = td.getProcess() !=null ? td.getProcess().getDefinition() : (ProcessDefinition)td.getDefinition();
    		td.getDefinition().getQAConstraints().stream().forEach(entry -> {
    			//InstanceType type = td.getInstance().getProperty(ProcessStep.getQASpecId(entry, ProcessStep.getOrCreateDesignSpaceInstanceType(ws, td.getDefinition()))).propertyType().referencedInstanceType();
    			String id = ProcessStep.getQASpecId(entry, pd);
    			ConstraintWrapper cw = WrapperCache.getWrappedInstance(ConstraintWrapper.class, (Instance) td.getInstance().getPropertyAsMap(ProcessStep.CoreProperties.qaState.toString()).get(id));
    			ConsistencyRuleType crt = (ConsistencyRuleType)cw.getCr().getInstanceType();
    			if (crt.hasRuleError()) {
    				pex.getErrorMessages().add(crt.ruleError());
    			}
    			String eval = (String) crt.ruleEvaluations().get().stream()
    					.map(rule -> ((Rule)rule).result()+"" )
    					.collect(Collectors.joining(",","[","]"));
    			System.out.println("Checking "+crt.name() +" Result: "+ eval);

    		});
    		for (Conditions condition : Conditions.values()) {
    			if (td.getDefinition().getCondition(condition).isPresent()) {
    				InstanceType type = td.getInstance().getProperty(condition.toString()).propertyType().referencedInstanceType();
    				ConsistencyRuleType crt = (ConsistencyRuleType)type;
    				if (crt.hasRuleError()) {
    					pex.getErrorMessages().add(crt.ruleError());
    				}
    				String eval = (String) crt.ruleEvaluations().get().stream()
    						.map(rule -> ((Rule)rule).result()+"" )
    						.collect(Collectors.joining(",","[","]"));
    				System.out.println("Checking "+crt.name() +" Result: "+ eval);
    			}	
    		}

    	});
    	if (pex.getErrorMessages().size() > 0)
    		throw pex;
    }
    
    public static void printFullProcessToLog(ProcessInstance proc) {
		printProcessToLog(proc, " ");
	}
	
	public static void printProcessToLog(ProcessInstance proc, String prefix) {
		
		System.out.println(prefix+proc.toString());
		String nextIndent = "  "+prefix;
		proc.getProcessSteps().stream().forEach(step -> {
			if (step instanceof ProcessInstance) {
				printProcessToLog((ProcessInstance) step, nextIndent);
			} else {
				
				System.out.println(nextIndent+step.toString());
			}
		});
		proc.getDecisionNodeInstances().stream().forEach(dni -> System.out.println(nextIndent+dni.toString()));
	}
	
	 public static void compileRestrictedRepairTree(RepairNode node, int position, StringBuffer printInto) {
	    	String treeLevel = "\n";
	    	for (int i = 0; i < position; i++) 
	    		treeLevel = treeLevel.concat(" -- ");
	    	if (node instanceof AbstractRepairAction) {
	    		AbstractRepairAction ra = (AbstractRepairAction)node;
	    		RestrictionNode rootNode =  ra.getValue()==UnknownRepairValue.UNKNOWN && ra.getRepairValueOption().getRestriction() != null ? ra.getRepairValueOption().getRestriction().getRootNode() : null;
	    		if (rootNode != null) {
	    			printInto.append(treeLevel.concat(compileRestrictedRepair(ra,rootNode.printNodeTree(false,40))));
	    			printInto.append("\n"+ rootNode.toTreeString(40));
	    		} else	
	    			printInto.append(treeLevel.concat(node.toString()));
	    	} else
	    		printInto.append(treeLevel.concat(node.toString()));
	    	for (RepairNode child : node.getChildren()) {
	    		compileRestrictedRepairTree(child, position + 1, printInto);
	    	}
	    }
	    
	    
	    public static String compileRestrictedRepair(AbstractRepairAction ra, String restriction) {
			String target = ra.getElement() != null ? ((Instance)ra.getElement()).name() : "";
			StringBuffer list = new StringBuffer();
			switch(ra.getOperator()) {
			case ADD:							 
				list.append(String.format("Add to %s of ", ra.getProperty()));
				list.append(target);
				list.append(restriction);
				break;
			case MOD_EQ:
			case MOD_GT:
			case MOD_LT:
			case MOD_NEQ:				
				list.append(String.format("Set the %s of ", ra.getProperty()));
				list.append(target);			
				list.append(" to");
				list.append(restriction);
				break;
			case REMOVE:					
				list.append(String.format("Remove from %s of ", ra.getProperty()));
				list.append(target);
				list.append(restriction);
				break;
			default:
				break;		
			}
			return list.toString();
		}
}

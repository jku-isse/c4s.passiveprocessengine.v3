package at.jku.isse.passiveprocessengine.repair;

public class RootSourceRepairIdentifier {

	
	// determines where to find the source that determines 
	// the output of a step if a repair asks for 
	// an element to be added or removed from the output of a step (post condition fulfillment)
	public void findRootSourceForOutputRepair() {
		// obtain the property that needs repair (is the identified loci of repair)
		// obtain the step's out_xxx/@rl_ruleScopes property, from this set filter out the rule that needs repairing, and analyse only crd_datamapping_ ... rules
		// for all these datamappings (typically only one, but more could exist)
			// find all input yyy parameters that have that rule also in in_yyy@rl_ruleScope 
		
	}
	
	
	// determines where to find the source that determines 
	// the input of a step if a repair asks for 
	// an element to be added or removed from the input of a step (pre condition fulfillment)
	public void findRootSourceForInputRepair() {
		
	}
}

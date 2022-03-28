package at.jku.isse.passiveprocessengine.instance;

import java.util.LinkedList;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProcessException extends Exception {
	private static final long serialVersionUID = 1L;
	@NonNull
	String mainMessage;
	List<String> errorMessages = new LinkedList<>();
	
}

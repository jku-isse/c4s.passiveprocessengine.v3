package at.jku.isse.passiveprocessengine.instance;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@EqualsAndHashCode(callSuper = true)
public class ProcessException extends Exception {
	private static final long serialVersionUID = 1L;	
	@Getter
	List<String> errorMessages = new LinkedList<>();

	public ProcessException(String mainMessage, List<String> errorMessages) {
		super(mainMessage);
		this.errorMessages = errorMessages;
	}

	public ProcessException(@NonNull String mainMessage) {
		super(mainMessage);
	}
	
	@Override
	public String getMessage() {
		return super.getMessage() +errorMessages.stream().collect(Collectors.joining(","," : \r\n[","]"));
	}





}

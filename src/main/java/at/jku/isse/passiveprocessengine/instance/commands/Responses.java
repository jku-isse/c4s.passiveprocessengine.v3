package at.jku.isse.passiveprocessengine.instance.commands;

import lombok.Data;

public class Responses {
	
	@Data
	public static class InputResponse { 
		String success = null;
		String error = null;
	
		public static InputResponse okResponse() {
			InputResponse okResp = new InputResponse();
			okResp.success = "OK";
			return okResp;
		}
		
		public static InputResponse errorResponse(String errMsg) {
			InputResponse errResp = new InputResponse();
			errResp.error = errMsg;
			return errResp;
		}
	}
	

}

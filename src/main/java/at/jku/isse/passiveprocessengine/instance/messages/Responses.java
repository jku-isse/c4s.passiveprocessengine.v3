package at.jku.isse.passiveprocessengine.instance.messages;

import lombok.Data;

public class Responses {
	
	@Data
	public static class IOResponse { 
		String success = null;
		String error = null;
	
		public static IOResponse okResponse() {
			IOResponse okResp = new IOResponse();
			okResp.success = "OK";
			return okResp;
		}
		
		public static IOResponse errorResponse(String errMsg) {
			IOResponse errResp = new IOResponse();
			errResp.error = errMsg;
			return errResp;
		}
	}
	

}

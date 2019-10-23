package restdemo;

import org.junit.Assert;
import io.restassured.path.xml.XmlPath;
import io.restassured.response.Response;

public class Validator {

	public void xmlEvaluator(Response response, String userId) { 
	XmlPath xmlPathEvaluator = response.xmlPath();
	String xml = xmlPathEvaluator.prettify();
	System.out.println(xml);
	String Headerstatus = xmlPathEvaluator.getString("invitationResponse.ResponseHeader.status");
	Assert.assertEquals(Headerstatus, "OK");
	String statusMessage = xmlPathEvaluator.getString("invitationResponse.ResponseBody.responseStatus");
	Assert.assertEquals(statusMessage, "SUCCESS");
	String id = xmlPathEvaluator.getString("invitationResponse.ResponseBody.id");
	Assert.assertEquals(id, userId);
	String status = xmlPathEvaluator.getString("invitationResponse.ResponseBody.status");
	Assert.assertEquals(statusMessage, "SUCCESS");
	Assert.assertEquals(status, "INVITED");
	}
	
	
	public void xmlEvaluatorErrorMessage(Response response, String userId) { 
		XmlPath xmlPathEvaluator = response.xmlPath();
		String xml = xmlPathEvaluator.prettify();
		System.out.println(xml);
		String Headerstatus = xmlPathEvaluator.getString("invitationResponse.ResponseHeader.status");
		Assert.assertEquals(Headerstatus, "ERROR");
		String id = xmlPathEvaluator.getString("invitationResponse.ResponseBody.id");
		Assert.assertEquals(id, userId);
		String status = xmlPathEvaluator.getString("invitationResponse.ResponseBody.status");
		Assert.assertEquals(status, "CANCELLED");
		
		}
	
}

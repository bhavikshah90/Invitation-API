package restdemo;

import restdemo.Validator;
import org.junit.Assert;
import org.junit.Before;

import java.io.FileInputStream;
import java.io.IOException;

import org.apache.http.HttpStatus;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import io.restassured.RestAssured;
import io.restassured.path.xml.XmlPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;


class InvitationApi {
	
	
	private String applicationId;
	private String token;
	private String userId;
	private String loginPayload = "<ServiceRequest>\r\n" + 
			"   			 <RequestHeader>\r\n" + 
			"		        <consumerAppId>123</consumerAppId>\r\n" + 
			"		        <serviceName>AuthenticationService</serviceName>\r\n" + 
			"        		<operationName>authenticate</operationName>\r\n" + 
			"			</RequestHeader>\r\n" + 
			"			<RequestPayload>\r\n" + 
			"				<data>\r\n" + 
			"                    <AuthenticationRequestData>\r\n" + 
			"                        <applicationId>123</applicationId>\r\n" + 
			"                        <applicationPassword>Test123</applicationPassword>\r\n" + 
			"                        <clientIP>127.0.0.1</clientIP>\r\n" + 
			"                    </AuthenticationRequestData>\r\n" + 
			"                </data>\r\n" + 
			"			</RequestPayload>\r\n" + 
			"		</ServiceRequest>\r\n" ;
	Validator validator = new Validator();
	
	 @Before
	 public void setBaseUri () {
		 RestAssured.baseURI = "https://testApplication.com/invitation";
		 
	  }

	@Test
 	public void generateToken() {
	 	RestAssured.baseURI = "http://authenticate.testapp.com:5030/Service/httpservice/";
		RequestSpecification req = RestAssured.given();
		req.header("Content-Type","text/XML");
		req.header("Accept","text/XML");	
		req.body(loginPayload);
		req.log().ifValidationFails();
		Response response = req.post("/");
		XmlPath xmlPathEvaluator = response.xmlPath();
		int statusCode = response.getStatusCode();
		if(statusCode == HttpStatus.SC_METHOD_NOT_ALLOWED) {
			System.out.println(response.getStatusLine());
		}
		String xml = xmlPathEvaluator.prettify();
		System.out.println(xml);
		String headerStatus = xmlPathEvaluator.getString("ServiceResponse.ResponseHeader.status");
		String errorMessage = xmlPathEvaluator.getString("ServiceResponse.ResponseBody.AuthenticationResponseData.errorMessage");
		token = xmlPathEvaluator.getString("ServiceResponse.ResponseBody.AuthenticationResponseData.token");
		
		applicationId = xmlPathEvaluator.getString("ServiceResponse.ResponseBody.AuthenticationResponseData.applicationId");
		if(headerStatus.equals("OK")) {
		Assert.assertEquals(errorMessage, "");
		Assert.assertNotNull("Token is generated", token);
		Assert.assertNotNull("ApplicationId is generated", applicationId);
		}
		else if(headerStatus.equals("ERROR")){
			Assert.assertNotEquals(errorMessage, "");
			Assert.assertNull("Token is not generated", token);
			Assert.assertNull("ApplicationId is not generated", applicationId);
				
		}
		
	}

	@Test
	public void createInvitation() throws IOException {
		String path = "post.xlsx";
		FileInputStream fis = new FileInputStream(path);
		XSSFWorkbook wb = new XSSFWorkbook(fis);
		XSSFSheet sheet = wb.getSheetAt(0);
		int rowNo = sheet.getLastRowNum() - sheet.getFirstRowNum();
		for (int i = 1; i <= rowNo; i++) {
			String requestHeader = "<requestHeader> \r\n" + 
					"        	<applicationId>"+applicationId+"</applicationId> --> Authentication Application\r\n" + 
					"        	<appToken>"+token+"<appToken>\r\n" + 
					"        	<userID>"+sheet.getRow(i).getCell(0).getStringCellValue()+"</userID>\r\n" + 
					"        </requestHeader>\r\n" ;
			String applicationData = "<applicationData>\r\n" + 
					"        		<appID>"+sheet.getRow(i).getCell(1).getStringCellValue()+"</appID>\r\n" + 
					"        		<appName>"+sheet.getRow(i).getCell(2).getStringCellValue()+"</appName>\r\n" + 
					"        		<appRole>"+sheet.getRow(i).getCell(3).getStringCellValue()+"</appRole>\r\n" + 
					"        		<appLocation>"+sheet.getRow(i).getCell(4).getStringCellValue()+"</appLocation>\r\n" + 
					"        	</applicationData>	";
			String userData = "<userData>\r\n" + 
					"        		<userName>"+sheet.getRow(i).getCell(5).getStringCellValue()+"</userName> \r\n" + 
					"        		<userEmail>"+sheet.getRow(i).getCell(6).getStringCellValue()+"</userEmail> \r\n" + 
					"        	</userData>\r\n"; 
			String requestBody =
					"        <requestBody>\r\n" + applicationData + userData +
					"        	<inviteOption>\r\n" + 
					"        		<inviteExpiry>"+sheet.getRow(i).getCell(7).getStringCellValue()+"</inviteExpiry> --> Follows date format: MM-DD-YYYY\r\n" + 
					"        </requestBody>\r\n" ;
			String payload = "<invitationRequest> <invitationRequest> \r\n" + 
					    		requestHeader +
					    		requestBody   + 
				    		 "</invitationRequest> \r\n" ; 	
			RequestSpecification httpRequest = RestAssured.given();
			httpRequest.accept("text/XML");
			httpRequest.contentType("text/XML");
			httpRequest.body(payload);
			Response response = httpRequest.post("/create");
			Assert.assertEquals( response.contentType(), "text/xml; charset=UTF-8" );
			System.out.println(response.contentType().toString());
			String conn = response.getHeader("Connection");
			Assert.assertEquals( conn, "keep-alive");
			int successCode = response.getStatusCode();
	        System.out.println(response.getStatusLine());
	       
			if (successCode == HttpStatus.SC_CREATED){
	        	XmlPath xmlPathEvaluator = response.xmlPath();
				String xml = xmlPathEvaluator.prettify();
				System.out.println(xml);
				String Headerstatus = xmlPathEvaluator.getString("invitationResponse.ResponseHeader.status");
				Assert.assertEquals(Headerstatus, "OK");
				String statusMessage = xmlPathEvaluator.getString("invitationResponse.ResponseBody.responseStatus");
				Assert.assertEquals(statusMessage, "SUCCESS");
				userId = xmlPathEvaluator.getString("invitationResponse.ResponseBody.id");
				String status = xmlPathEvaluator.getString("invitationResponse.ResponseBody.status");
				Assert.assertEquals(statusMessage, "SUCCESS");
				Assert.assertEquals(status, "QUEUED");
	        }
	        else if((successCode == HttpStatus.SC_PRECONDITION_FAILED)||(successCode== HttpStatus.SC_UNAUTHORIZED)){
	        	XmlPath xmlPathEvaluator = response.xmlPath();
				String xml = xmlPathEvaluator.prettify();
				System.out.println(xml);
				String Headerstatus = xmlPathEvaluator.getString("invitationResponse.ResponseHeader.status");
				Assert.assertEquals(Headerstatus, "ERROR");
				userId = xmlPathEvaluator.getString("invitationResponse.ResponseBody.id");
				String status = xmlPathEvaluator.getString("invitationResponse.ResponseBody.status");
				Assert.assertEquals(status, "CANCELLED");
	        }
	        else if (successCode == 498) {
	        	System.out.println("Invalid Token");
	        	XmlPath xmlPathEvaluator = response.xmlPath();
				String xml = xmlPathEvaluator.prettify();
				System.out.println(xml);
				String Headerstatus = xmlPathEvaluator.getString("invitationResponse.ResponseHeader.status");
				Assert.assertEquals(Headerstatus, "ERROR");
				userId = xmlPathEvaluator.getString("invitationResponse.ResponseBody.id");
				String status = xmlPathEvaluator.getString("invitationResponse.ResponseBody.status");
				Assert.assertEquals(status, "CANCELLED");	        	
	        }
		}
	}
	
	@Test
	public void fetchInvitation() {

		Response response;
		response = RestAssured.get("/fetch/"+userId);
		Assert.assertEquals( response.contentType(), "text/xml; charset=UTF-8" );
		System.out.println(response.contentType().toString());
		String conn = response.getHeader("Connection");
		Assert.assertEquals( conn, "keep-alive");
		int successCode = response.getStatusCode();
        System.out.println(successCode);
     	if((successCode == HttpStatus.SC_UNAUTHORIZED)||(successCode == HttpStatus.SC_METHOD_NOT_ALLOWED)||
				(successCode == 498)|| (successCode == HttpStatus.SC_NOT_FOUND)) {
        	System.out.println(response.getStatusLine());
        	validator.xmlEvaluatorErrorMessage(response, userId);
        }
        else if (successCode == HttpStatus.SC_OK) {
		validator.xmlEvaluator(response, userId);
        }
        
	    else validator.xmlEvaluatorErrorMessage(response, userId);
		}
	
	@Test
	public void updateInvitation() throws IOException {
		String path = "post.xlsx";
		FileInputStream fis = new FileInputStream(path);
		XSSFWorkbook wb = new XSSFWorkbook(fis);
		XSSFSheet sheet = wb.getSheetAt(0);
		int rowNo = sheet.getLastRowNum() - sheet.getFirstRowNum();
		for (int i = 1; i <= rowNo; i++) {
		String requestHeader = "<requestHeader> \r\n" + 
				"        	<applicationId>"+applicationId+"</applicationId> --> Authentication Application\r\n" + 
				"        	<appToken>"+token+"<appToken>\r\n" + 
				"        	<userID>"+sheet.getRow(i).getCell(0).getStringCellValue()+"</userID>\r\n" + 
				"        </requestHeader>\r\n" ;
		String applicationData = "<applicationData>\r\n" + 
				"        		<appID>"+sheet.getRow(i).getCell(1).getStringCellValue()+"</appID>\r\n" + 
				"        		<appName>"+sheet.getRow(i).getCell(2).getStringCellValue()+"</appName>\r\n" + 
				"        		<appRole>"+sheet.getRow(i).getCell(3).getStringCellValue()+"</appRole>\r\n" + 
				"        		<appLocation>"+sheet.getRow(i).getCell(4).getStringCellValue()+"</appLocation>\r\n" + 
				"        	</applicationData>	";
		String userData = "<userData>\r\n" + 
				"        		<userName>"+sheet.getRow(i).getCell(5).getStringCellValue()+"</userName> \r\n" + 
				"        		<userEmail>"+sheet.getRow(i).getCell(6).getStringCellValue()+"</userEmail> \r\n" + 
				"        	</userData>\r\n"; 
		String requestBody =
				"        <requestBody>\r\n" + applicationData + userData +
				"        	<inviteOption>\r\n" + 
				"        		<inviteExpiry>"+sheet.getRow(i).getCell(7).getStringCellValue()+"</inviteExpiry> --> Follows date format: MM-DD-YYYY\r\n" + 
				"        </requestBody>\r\n" ;
		String payload = "<invitationRequest> <invitationRequest> \r\n" + 
				    		requestHeader +
				    		requestBody   + 
			    		 "</invitationRequest> \r\n" ; 
				
		RequestSpecification httpRequest = RestAssured.given();
		httpRequest.accept("text/XML");
		httpRequest.contentType("text/XML");
		httpRequest.body(payload);
		Response response = httpRequest.put("/update/"+userId);
		
		Assert.assertEquals( response.contentType(), "text/xml; charset=UTF-8" );
		System.out.println(response.contentType().toString());
		String conn = response.getHeader("Connection");
		Assert.assertEquals( conn, "keep-alive");
		
		int successCode = response.getStatusCode();
		System.out.println(successCode);
		if((successCode == HttpStatus.SC_UNAUTHORIZED)||(successCode == HttpStatus.SC_METHOD_NOT_ALLOWED)||
				(successCode == 498)) {
        	System.out.println(response.getStatusLine());
        	validator.xmlEvaluatorErrorMessage(response, userId);
        }
        else if((successCode == HttpStatus.SC_CREATED)||(successCode == HttpStatus.SC_NO_CONTENT)) {
        validator.xmlEvaluator(response, userId);
        }
        else {
        	validator.xmlEvaluatorErrorMessage(response, userId);
        }
		}
	}
	

	@Test
	public void deleteInvitation() throws IOException {
		RequestSpecification httpRequest = RestAssured.given();
		httpRequest.accept("text/XML");
		httpRequest.contentType("text/XML");
		String path = "post.xlsx";
		FileInputStream fis = new FileInputStream(path);
		XSSFWorkbook wb = new XSSFWorkbook(fis);
		XSSFSheet sheet = wb.getSheetAt(0);
		int rowNo = sheet.getLastRowNum() - sheet.getFirstRowNum();
		for (int i = 1; i <= rowNo; i++) {
		String payload = "<invitationRequest> \r\n" + 
				"    	<requestHeader> \r\n" + 
				"        	<applicationId>"+applicationId+"</applicationId> --> Authentication Application\r\n" + 
				"        	<appToken>"+token+"<appToken>\r\n" + 
				"        	<userID>"+sheet.getRow(i).getCell(0).getStringCellValue()+"</userID>\r\n" + 
				"        </requestHeader>\r\n" + 
				"        <requestBody>\r\n" + 
				"        	<invitationID>"+userId+"</invitationID>\r\n" + 
				"        </requestBody>\r\n" + 
				"    </invitationRequest> ";
		
		httpRequest.body(payload);
		Response response = httpRequest.post("/delete");
		Assert.assertEquals( response.contentType(), "text/xml; charset=UTF-8" );
		System.out.println(response.contentType().toString());
		String conn = response.getHeader("Connection");
		Assert.assertEquals( conn, "keep-alive");
		int successCode = response.getStatusCode();
        System.out.println(successCode);
        
        if((successCode == HttpStatus.SC_UNAUTHORIZED)||(successCode == HttpStatus.SC_METHOD_NOT_ALLOWED)||
				(successCode == 498)|| (successCode == HttpStatus.SC_NOT_FOUND)) {
        	System.out.println(response.getStatusLine());
        	validator.xmlEvaluatorErrorMessage(response, userId);
        }
        
        else if(successCode ==  HttpStatus.SC_OK) {
		validator.xmlEvaluator(response, userId);
        }
        
        else validator.xmlEvaluatorErrorMessage(response, userId);
	}
	}
	
	
	
	
	
	
	
	
}

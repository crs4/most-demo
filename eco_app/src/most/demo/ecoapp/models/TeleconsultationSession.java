package most.demo.ecoapp.models;

import java.io.Serializable;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

public class TeleconsultationSession implements Serializable {


	private static final long serialVersionUID = -6277133365800493720L;
	
	private String id;
	private TeleconsultationSessionState tss;
	private HashMap<String,String> voipParams;


	public TeleconsultationSession(String id , TeleconsultationSessionState tss)
	{
		this.id = id;
		this.tss = tss;
	}
	
	
	public void setVoipParams(JSONObject sd) 
    { 
		JSONObject applicantVoipData;
		try {
			JSONObject sessionData = sd.getJSONObject("data").getJSONObject("session");
			
			applicantVoipData = sessionData.getJSONObject("teleconsultation").getJSONObject("applicant").getJSONObject("voip_data");
			
			String sipServerIp = applicantVoipData.getJSONObject("sip_server").getString("address");
		    String sipServerPort=applicantVoipData.getJSONObject("sip_server").getString("port");
		    String sipServerTransport=applicantVoipData.getString("sip_transport");
		    String sipUserName = applicantVoipData.getString("sip_user");
		    String sipUserPwd = applicantVoipData.getString("sip_password");
		    String specExtension = sessionData.getJSONObject("teleconsultation").getJSONObject("specialist").getJSONObject("voip_data").getString("extension");
		    
		    this.voipParams = new HashMap<String,String>();
			
	    	voipParams.put("sipServerIp",sipServerIp); 
			voipParams.put("sipServerPort",sipServerPort); // default 5060
			voipParams.put("sipServerTransport",sipServerTransport); 
			
			
			// used by the app for calling the specified extension, not used directly by the VoipLib (TO CHECK)
			voipParams.put("specExtension",specExtension); 

			// specialista	
			voipParams.put("sipUserPwd", sipUserPwd); // 
			voipParams.put("sipUserName",sipUserName); // specialista
			
			//params.put("turnServerIp",  sipServerIp);
			//params.put("turnServerUser",sipUserName);   
			//params.put("turnServerPwd",sipUserPwd); 

			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   
    }
	

	public HashMap<String, String> getVoipParams() {
		return voipParams;
	}


	public String getId() {
		return id;
	}

	
	public void setState(TeleconsultationSessionState tss)
	{
		this.tss = tss;
	}
	
	public TeleconsultationSessionState getState()
	{
		return this.tss;
	}
}

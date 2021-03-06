package most.demo.specapp.models;

import java.io.Serializable;

public class Teleconsultation implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1408055529735190987L;
	private String id;
	private String info;
	private SpecUser specialist = null;
	
	public SpecUser getSpecialist() {
		return specialist;
	}

	public void setSpecialist(SpecUser specialist) {
		this.specialist = specialist;
	}

	TeleconsultationSession lastSession;
	
	public TeleconsultationSession getLastSession() {
		return lastSession;
	}

	public String getId() {
		return id;
	}

    public String getInfo()
    {
    	return this.info;
    }
    
	public Teleconsultation(String id, String info, TeleconsultationSession lastSession)
	{
		this.id = id;
		this.info = info;
		this.lastSession = lastSession;
	}
	
}

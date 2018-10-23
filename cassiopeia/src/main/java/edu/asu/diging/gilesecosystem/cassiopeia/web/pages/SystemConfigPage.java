package edu.asu.diging.gilesecosystem.cassiopeia.web.pages;

public class SystemConfigPage {

    private String baseUrl;
    private String gilesAccessToken;
    private String OCRType;
    private String languageType;
    
    public String getBaseUrl() {
        return baseUrl;
    }
    public void setBaseUrl(String nepomukUrl) {
        this.baseUrl = nepomukUrl;
    }

    public String getGilesAccessToken() {
        return gilesAccessToken;
    }

    public void setGilesAccessToken(String gilesAccessToken) {
        this.gilesAccessToken = gilesAccessToken;
    }

    public String getOCRType() {
        return OCRType;
    }

    public void setOCRType(String oCRType) {
        OCRType = oCRType;
    }
    
    public String getLanguageType() {
		return languageType;
	}
    
    public void setLanguageType(String languagetype) {
		languageType = languagetype;
	}   

}

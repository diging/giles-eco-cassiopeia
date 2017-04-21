package edu.asu.diging.gilesecosystem.cassiopeia.web.pages;

public class SystemConfigPage {

    private String baseUrl;
    private String gilesAccessToken;
    private boolean createhOCR;
    
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

    public boolean isCreatehOCR() {
        return createhOCR;
    }

    public void setCreatehOCR(boolean createhOCR) {
        this.createhOCR = createhOCR;
    }

}

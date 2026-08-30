package com.falconenergy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payments.pawapay")
public class PawaPayProperties {
    private String apiToken; private String baseUrl; private String environment;
    public String getApiToken(){return apiToken;} public void setApiToken(String value){apiToken=value;}
    public String getBaseUrl(){return baseUrl;} public void setBaseUrl(String value){baseUrl=value;}
    public String getEnvironment(){return environment;} public void setEnvironment(String value){environment=value;}
    public boolean enabled(){return apiToken!=null&&!apiToken.isBlank()&&baseUrl!=null&&!baseUrl.isBlank()&&environment!=null&&!environment.isBlank();}
}

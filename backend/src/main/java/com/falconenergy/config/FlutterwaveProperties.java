package com.falconenergy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payments.flutterwave")
public class FlutterwaveProperties {
    private String clientId;
    private String clientSecret;
    private String baseUrl = "https://developersandbox-api.flutterwave.com";
    private String environment = "sandbox";
    private String webhookSecretHash;
    private String redirectUrl;
    private String scenarioKey;
    private String frontendReturnUrl = "http://localhost:8080/customer/dashboard";
    private String tokenUrl = "https://idp.flutterwave.com/realms/flutterwave/protocol/openid-connect/token";
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 20;
    public String getClientId(){ return clientId; } public void setClientId(String value){ clientId=value; }
    public String getClientSecret(){ return clientSecret; } public void setClientSecret(String value){ clientSecret=value; }
    public String getBaseUrl(){ return baseUrl; } public void setBaseUrl(String value){ baseUrl=value; }
    public String getEnvironment(){ return environment; } public void setEnvironment(String value){ environment=value; }
    public String getWebhookSecretHash(){ return webhookSecretHash; } public void setWebhookSecretHash(String value){ webhookSecretHash=value; }
    public String getRedirectUrl(){ return redirectUrl; } public void setRedirectUrl(String value){ redirectUrl=value; }
    public String getScenarioKey(){ return scenarioKey; } public void setScenarioKey(String value){ scenarioKey=value; }
    public String getFrontendReturnUrl(){ return frontendReturnUrl; } public void setFrontendReturnUrl(String value){ frontendReturnUrl=value; }
    public String getTokenUrl(){ return tokenUrl; } public void setTokenUrl(String value){ tokenUrl=value; }
    public int getConnectTimeoutSeconds(){ return connectTimeoutSeconds; } public void setConnectTimeoutSeconds(int value){ connectTimeoutSeconds=value; }
    public int getReadTimeoutSeconds(){ return readTimeoutSeconds; } public void setReadTimeoutSeconds(int value){ readTimeoutSeconds=value; }
    public boolean enabled(){ return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank(); }
}

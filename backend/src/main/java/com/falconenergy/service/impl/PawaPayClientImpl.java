package com.falconenergy.service.impl;

import com.falconenergy.config.PawaPayProperties;
import com.falconenergy.service.PawaPayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.math.BigDecimal;
import java.util.*;

@Component @RequiredArgsConstructor
public class PawaPayClientImpl implements PawaPayClient {
    private final PawaPayProperties properties;
    private RestClient client(){ if(!properties.enabled()||!("sandbox".equalsIgnoreCase(properties.getEnvironment())||"production".equalsIgnoreCase(properties.getEnvironment()))) throw new IllegalStateException("pawaPay is not configured."); return RestClient.builder().baseUrl(properties.getBaseUrl().replaceAll("/$", "")).defaultHeader("Authorization", "Bearer "+properties.getApiToken()).build(); }
    public DepositResult initiateDeposit(UUID depositId, BigDecimal amount, String currency, String phoneNumber, String correspondent, String invoiceNumber){
        try {
            Map<String,Object> body=new LinkedHashMap<>(); body.put("depositId",depositId.toString()); body.put("amount",amount.stripTrailingZeros().toPlainString()); body.put("currency",currency); body.put("payer",Map.of("type","MMO","accountDetails",Map.of("phoneNumber",phoneNumber,"provider",correspondent))); body.put("customerMessage","Falcon Fuel"); body.put("clientReferenceId",invoiceNumber); body.put("metadata",List.of(Map.of("invoiceNumber",invoiceNumber)));
            Map<?,?> result=client().post().uri("/v2/deposits").contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(Map.class);
            if(result==null||result.get("status")==null) throw new IllegalStateException("pawaPay returned an invalid deposit response.");
            String reason=result.get("failureReason") instanceof Map<?,?> failure ? (failure.get("failureMessage")==null?"Deposit rejected.":String.valueOf(failure.get("failureMessage"))) : null;
            return new DepositResult(String.valueOf(result.get("status")),reason);
        } catch(RestClientResponseException exception){ throw new IllegalStateException("pawaPay rejected the deposit: "+exception.getResponseBodyAsString(),exception); }
        catch(RestClientException exception){ throw new IllegalStateException("pawaPay deposit request could not be completed.",exception); }
    }
    public String publicKeyPem(String keyId){
        try { List<?> keys=client().get().uri("/public-key/http").retrieve().body(List.class); if(keys!=null) for(Object entry:keys) if(entry instanceof Map<?,?> key&&keyId.equals(String.valueOf(key.get("id")))) return String.valueOf(key.get("key")); throw new IllegalStateException("pawaPay callback signing key was not found."); }
        catch(RestClientException exception){ throw new IllegalStateException("pawaPay public keys could not be retrieved.",exception); }
    }
}

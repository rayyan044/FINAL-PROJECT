package com.falconenergy.service.impl;

import com.falconenergy.config.FlutterwaveProperties;
import com.falconenergy.service.FlutterwaveClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Component @RequiredArgsConstructor
public class FlutterwaveClientImpl implements FlutterwaveClient {
    private static final String TOKEN_URL = "https://idp.flutterwave.com/realms/flutterwave/protocol/openid-connect/token";
    private final FlutterwaveProperties properties;
    private volatile String token; private volatile Instant expiresAt = Instant.EPOCH;

    @Override public ChargeResult createMobileMoneyCharge(String email, String name, String phone, String network, BigDecimal amount, String currency, String reference) {
        Map<?,?> customer = post("/customers", Map.of("email", email, "name", Map.of("first", firstName(name), "last", "Customer")), UUID.randomUUID().toString());
        String customerId = string(data(customer).get("id"));
        Map<?,?> method = post("/payment-methods", Map.of("type", "mobile_money", "mobile_money", Map.of("country_code", "255", "network", network, "phone_number", localPhone(phone))), UUID.randomUUID().toString());
        String methodId = string(data(method).get("id"));
        Map<String,Object> charge = new LinkedHashMap<>(); charge.put("amount", amount); charge.put("currency", currency); charge.put("reference", reference); charge.put("customer_id", customerId); charge.put("payment_method_id", methodId); charge.put("meta", Map.of("source", "falcon-fuel", "reference", reference));
        if (properties.getRedirectUrl()!=null && !properties.getRedirectUrl().isBlank()) charge.put("redirect_url", properties.getRedirectUrl());
        return result(post("/charges", charge, reference));
    }
    @Override public ChargeResult retrieveCharge(String chargeId) { return result(get("/charges/" + chargeId)); }

    private Map<?,?> post(String uri, Object body, String idempotency) { return request().post().uri(uri).contentType(MediaType.APPLICATION_JSON).header("X-Trace-Id", trace()).header("X-Idempotency-Key", idempotency).headers(h -> scenario(h)).body(body).retrieve().body(Map.class); }
    private Map<?,?> get(String uri) { return request().get().uri(uri).header("X-Trace-Id", trace()).headers(h -> scenario(h)).retrieve().body(Map.class); }
    private RestClient request() { if(!properties.enabled()) throw new IllegalStateException("Flutterwave is not configured."); return RestClient.builder().baseUrl(properties.getBaseUrl().replaceAll("/$", "")).defaultHeader("Authorization", "Bearer " + accessToken()).build(); }
    private void scenario(org.springframework.http.HttpHeaders headers) { if("sandbox".equalsIgnoreCase(properties.getEnvironment()) && properties.getScenarioKey()!=null && !properties.getScenarioKey().isBlank()) headers.set("X-Scenario-Key", properties.getScenarioKey()); }
    private String accessToken() { if(token != null && Instant.now().isBefore(expiresAt.minusSeconds(60))) return token; synchronized(this) { if(token != null && Instant.now().isBefore(expiresAt.minusSeconds(60))) return token; try { Map<?,?> response=RestClient.create().post().uri(TOKEN_URL).contentType(MediaType.APPLICATION_FORM_URLENCODED).body("client_id="+enc(properties.getClientId())+"&client_secret="+enc(properties.getClientSecret())+"&grant_type=client_credentials").retrieve().body(Map.class); token=string(response.get("access_token")); Object expiry=response.get("expires_in"); long seconds=expiry==null?600:Long.parseLong(String.valueOf(expiry)); expiresAt=Instant.now().plusSeconds(seconds); return token; } catch(RestClientResponseException ex) { throw new IllegalStateException("Flutterwave authentication failed.", ex); } } }
    private ChargeResult result(Map<?,?> response) { Map<?,?> d=data(response); Map<?,?> next = map(d.get("next_action")); Map<?,?> redirect = map(next.get("redirect_url")); String url=redirect.isEmpty()?string(next.get("url")):string(redirect.get("url")); return new ChargeResult(string(d.get("id")), string(d.get("reference")), decimal(d.get("amount")), string(d.get("currency")), string(d.get("status")), string(next.get("type")), url, string(d.get("failure_reason"))); }
    private static Map<?,?> data(Map<?,?> r){ return map(r==null?null:r.get("data")); } @SuppressWarnings("unchecked") private static Map<?,?> map(Object o){ return o instanceof Map<?,?> m?m:Map.of(); } private static String string(Object o){return o==null?null:String.valueOf(o);} private static BigDecimal decimal(Object o){return o==null?null:new BigDecimal(String.valueOf(o));} private static String trace(){return "falcon"+UUID.randomUUID().toString().replace("-", "");} private static String firstName(String value){return value==null||value.isBlank()?"Falcon":value.trim().split("\\s+")[0];} private static String localPhone(String phone){String p=phone.replaceAll("\\D", "");return p.startsWith("255")?p.substring(3):p;} private static String enc(String v){return java.net.URLEncoder.encode(v, java.nio.charset.StandardCharsets.UTF_8);}
}

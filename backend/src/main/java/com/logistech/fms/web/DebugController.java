package com.logistech.fms.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/** Diagnóstico temporal: muestra qué JWKS ve el backend en marcha. */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

  @Value("${app.supabase.url:https://npswaigxcmyddqwdktge.supabase.co}")
  private String supabaseUrl;

  @GetMapping("/jwks")
  public Map<String, Object> jwks() {
    String url = supabaseUrl + "/auth/v1/.well-known/jwks.json";
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("jwksUrl", url);
    try {
      String body = RestClient.create().get().uri(url).retrieve().body(String.class);
      JsonNode keys = new ObjectMapper().readTree(body).get("keys");
      List<String> kids = new ArrayList<>();
      keys.forEach(k -> kids.add(k.get("kid").asText() + " [" + k.get("alg").asText() + "]"));
      result.put("kids", kids);
    } catch (Exception e) {
      result.put("error", e.toString());
    }
    return result;
  }
}

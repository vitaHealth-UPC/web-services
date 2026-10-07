package com.tata.identitysubscription.interfaces.rest.resources;
import java.time.Instant;
public record AccountResource(String id, String name, String email, String status, String accessToken, Instant expiresAt) {
 public AccountResource(String id,String name,String email,String status) { this(id,name,email,status,null,null); }
}

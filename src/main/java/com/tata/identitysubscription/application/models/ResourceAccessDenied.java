package com.tata.identitysubscription.application.models;
public final class ResourceAccessDenied extends RuntimeException {
 public ResourceAccessDenied() { super("The authenticated session cannot access this resource"); }
}

package com.tata.identitysubscription.application.internal.outboundservices;
public interface VerificationDeliveryPort { void send(String email, String code); }

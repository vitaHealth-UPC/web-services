package com.tata.carelink.application.internal.outboundservices;
import com.tata.carelink.application.models.AuthenticatedCareLinkResult;
import com.tata.carelink.application.models.CareLinkResult;
public interface LinkSessionPort { AuthenticatedCareLinkResult establish(CareLinkResult link); }

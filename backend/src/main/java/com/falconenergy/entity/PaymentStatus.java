package com.falconenergy.entity;
/** Local transaction state. COMPLETED is set by the in-app payment simulation. */
public enum PaymentStatus { PENDING, PROCESSING, COMPLETED, SUCCESSFUL, FAILED, CANCELLED, EXPIRED, REVERSED }

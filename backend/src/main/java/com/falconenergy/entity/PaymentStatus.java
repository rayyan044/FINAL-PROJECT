package com.falconenergy.entity;
/** Server-controlled payment state. Provider status is retained separately on Payment. */
public enum PaymentStatus {
 INITIATED, PENDING, PROCESSING, ACTION_REQUIRED, SUCCESSFUL, FAILED, CANCELLED, EXPIRED, UNKNOWN,
 /** Legacy state retained for historical pawaPay rows. New Flutterwave payments use SUCCESSFUL. */
 COMPLETED, REVERSED
}

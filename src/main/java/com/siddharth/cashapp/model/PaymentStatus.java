package com.siddharth.cashapp.model;

public enum PaymentStatus {
    UNMATCHED,          // no money applied to any invoice yet
    PARTIALLY_APPLIED,  // some money applied, some left over (credit on account)
    FULLY_APPLIED       // every rupee of the payment went to invoices
}

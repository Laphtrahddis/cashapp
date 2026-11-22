package com.siddharth.cashapp.model;

public enum InvoiceStatus {
    OPEN,            // nothing paid yet
    PARTIALLY_PAID,  // some money applied, some still owed
    PAID             // open amount is zero
}

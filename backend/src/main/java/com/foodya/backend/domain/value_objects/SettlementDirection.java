package com.foodya.backend.domain.value_objects;

/** Which way the money moves when a settlement is marked done. */
public enum SettlementDirection {
    /** The platform pays the payee (merchant revenue, driver earnings). */
    PAYOUT,
    /** The payee hands money to the platform (a driver holding more COD cash than they earned). */
    COLLECTION
}
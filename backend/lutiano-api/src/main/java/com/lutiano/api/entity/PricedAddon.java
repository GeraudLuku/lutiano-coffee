package com.lutiano.api.entity;

/** An add-on with its current price, as chosen for a sale line or an order line. */
public record PricedAddon(String id, String name, long priceXaf) {}

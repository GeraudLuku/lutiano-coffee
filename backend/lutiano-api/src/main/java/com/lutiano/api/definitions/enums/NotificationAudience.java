package com.lutiano.api.definitions.enums;

/** Who receives an event: the person it is about, or the staff who approve (finance = administrators; stock = also inventory managers). */
public enum NotificationAudience { PERSON, APPROVERS_FINANCE, APPROVERS_STOCK, STOCK_ALERT }

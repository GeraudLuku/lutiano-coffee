package com.lutiano.api.proxy;

import java.util.Map;
import com.lutiano.api.service.notification.NotificationCatalog;

/** The title of a push notification, in the recipient's language, for each event. */
public final class PushTitles {
  private static final Map<String, String[]> TITLES = Map.ofEntries(
      Map.entry(NotificationCatalog.STOCK_LOW, new String[] {"Alerte de stock bas", "Low Stock Alert"}),
      Map.entry(NotificationCatalog.STOCK_OUT, new String[] {"Rupture de stock", "Out of Stock"}),
      Map.entry(NotificationCatalog.STOCK_IN_TRANSIT, new String[] {"Stock en route", "Stock on its way"}),
      Map.entry(NotificationCatalog.STOCK_REQUEST_APPROVED, new String[] {"Demande de stock approuvée", "Stock Request Approved"}),
      Map.entry(NotificationCatalog.STOCK_REQUEST_REJECTED, new String[] {"Demande de stock refusée", "Stock Request Rejected"}),
      Map.entry(NotificationCatalog.PAYOUT_APPROVED, new String[] {"Paiement approuvé", "Payout Approved"}),
      Map.entry(NotificationCatalog.PAYOUT_PAID, new String[] {"Paiement effectué", "Payout Paid"}),
      Map.entry(NotificationCatalog.PAYOUT_REVERSED, new String[] {"Paiement annulé", "Payout Reversed"}),
      Map.entry(NotificationCatalog.PAYOUT_NUMBER_VERIFIED, new String[] {"Numéro de paiement confirmé", "Payout Number Confirmed"}),
      Map.entry(NotificationCatalog.PAYOUT_NUMBER_REJECTED, new String[] {"Numéro de paiement refusé", "Payout Number Rejected"}),
      Map.entry(NotificationCatalog.EARNINGS_FINALIZED, new String[] {"Gains du mois arrêtés", "Monthly Earnings Final"}),
      Map.entry(NotificationCatalog.TARGET_REACHED, new String[] {"Objectif atteint", "Target Reached"}),
      Map.entry(NotificationCatalog.TARGET_GOAL_REACHED, new String[] {"Objectif atteint", "Target Reached"}),
      Map.entry(NotificationCatalog.CAMPAIGN_BONUS, new String[] {"Prime de campagne", "Campaign Bonus"}),
      Map.entry(NotificationCatalog.TARGET_MILESTONE, new String[] {"Progression de l'objectif", "Target Progress"}),
      Map.entry(NotificationCatalog.CASH_HANDOVER, new String[] {"Remise de caisse", "Cash Handover"}),
      Map.entry(NotificationCatalog.SHIFT_STILL_OPEN, new String[] {"Shift encore ouvert", "Shift Still Open"}),
      Map.entry(NotificationCatalog.REWARD_REDEEMED, new String[] {"Récompense obtenue", "Reward Redeemed"}),
      Map.entry(NotificationCatalog.REWARD_USED, new String[] {"Récompense utilisée", "Reward Used"}),
      Map.entry(NotificationCatalog.REWARD_EXPIRED, new String[] {"Récompense expirée", "Reward Expired"}),
      Map.entry(NotificationCatalog.ORDER_PLACED, new String[] {"Nouvelle commande", "New Order"}),
      Map.entry(NotificationCatalog.ORDER_ACCEPTED, new String[] {"Commande acceptée", "Order Accepted"}),
      Map.entry(NotificationCatalog.ORDER_REJECTED, new String[] {"Commande refusée", "Order Declined"}),
      Map.entry(NotificationCatalog.ORDER_READY, new String[] {"Commande prête", "Order Ready"}),
      Map.entry(NotificationCatalog.ORDER_COLLECTED, new String[] {"Commande terminée", "Order Complete"}),
      Map.entry(NotificationCatalog.ORDER_COLLECTED_POINTS, new String[] {"Commande terminée", "Order Complete"}),
      Map.entry(NotificationCatalog.ORDER_CANCELLED, new String[] {"Commande annulée", "Order Cancelled"}),
      Map.entry(NotificationCatalog.ORDER_EXPIRED, new String[] {"Commande expirée", "Order Expired"}),
      Map.entry(NotificationCatalog.SUPPORT_REPLIED, new String[] {"Réponse du support", "Support Reply"}));

  private PushTitles() {}

  public static String of(String eventKey, String language) {
    String[] t = TITLES.get(eventKey);
    boolean en = "en".equals(language);
    return t == null ? "LUTIANO" : t[en ? 1 : 0];
  }
}

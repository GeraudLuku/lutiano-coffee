package com.lutiano.api.service.notification;

import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import com.lutiano.api.definitions.enums.NotificationAudience;

/**
 * Every message the platform can send: its name, who gets it, the placeholders its text may use and the default
 * French and English texts. Administrators edit the texts (and switch SMS off per event) in the Notifications page;
 * what they save lives in the database, these are only the starting point.
 */
public final class NotificationCatalog {
  public static final String ESSENTIAL = "ESSENTIAL", MARKETING = "MARKETING";
    /** {@code oneTimeCode}: contains a code that is never stored or shown, and the event can neither be switched off nor resent. */
  public record Event(String key, String name, String category, NotificationAudience audience, List<String> placeholders, boolean oneTimeCode, String fr, String en) {}

  public static final String OTP_SIGN_IN = "OTP_SIGN_IN", OTP_PAYOUT_NUMBER = "OTP_PAYOUT_NUMBER", OTP_PASSWORD_RESET = "OTP_PASSWORD_RESET",
      PAYOUT_APPROVED = "PAYOUT_APPROVED", PAYOUT_PAID = "PAYOUT_PAID", PAYOUT_REVERSED = "PAYOUT_REVERSED",
      PAYOUT_NUMBER_VERIFIED = "PAYOUT_NUMBER_VERIFIED", PAYOUT_NUMBER_REJECTED = "PAYOUT_NUMBER_REJECTED",
      EARNINGS_FINALIZED = "EARNINGS_FINALIZED", TARGET_REACHED = "TARGET_REACHED",
      APPROVAL_REFUND = "APPROVAL_REFUND", APPROVAL_PAYOUT = "APPROVAL_PAYOUT", APPROVAL_PAYOUT_REVERSAL = "APPROVAL_PAYOUT_REVERSAL",
      APPROVAL_CASH_OUTFLOW = "APPROVAL_CASH_OUTFLOW", APPROVAL_STOCK = "APPROVAL_STOCK",
      STOCK_LOW = "STOCK_LOW", STOCK_OUT = "STOCK_OUT",
      CASH_HANDOVER = "CASH_HANDOVER", SHIFT_VARIANCE = "SHIFT_VARIANCE", STOCK_IN_TRANSIT = "STOCK_IN_TRANSIT",
      STOCK_REQUEST_APPROVED = "STOCK_REQUEST_APPROVED", STOCK_REQUEST_REJECTED = "STOCK_REQUEST_REJECTED",
      TARGET_MILESTONE = "TARGET_MILESTONE", CAMPAIGN_BONUS = "CAMPAIGN_BONUS", TARGET_GOAL_REACHED = "TARGET_GOAL_REACHED", SHIFT_STILL_OPEN = "SHIFT_STILL_OPEN",
      OTP_PHONE_CHANGE = "OTP_PHONE_CHANGE", OTP_CUSTOMER = "OTP_CUSTOMER", ORDER_PLACED = "ORDER_PLACED", REWARD_REDEEMED = "REWARD_REDEEMED", REWARD_USED = "REWARD_USED", REWARD_EXPIRED = "REWARD_EXPIRED", ORDER_ACCEPTED = "ORDER_ACCEPTED", ORDER_REJECTED = "ORDER_REJECTED",
      ORDER_READY = "ORDER_READY", ORDER_COLLECTED = "ORDER_COLLECTED", ORDER_COLLECTED_POINTS = "ORDER_COLLECTED_POINTS", ORDER_CANCELLED = "ORDER_CANCELLED", ORDER_EXPIRED = "ORDER_EXPIRED", SUPPORT_REPLIED = "SUPPORT_REPLIED";

  /** Messages that appear in the app only: they start with the SMS channel switched off (an administrator can still switch it on). */
  public static final Set<String> IN_APP_ONLY = Set.of(REWARD_REDEEMED, REWARD_USED, REWARD_EXPIRED, ORDER_PLACED, ORDER_ACCEPTED, ORDER_REJECTED, ORDER_READY, ORDER_COLLECTED, ORDER_COLLECTED_POINTS, ORDER_CANCELLED, ORDER_EXPIRED, TARGET_MILESTONE, TARGET_GOAL_REACHED, SHIFT_STILL_OPEN, SUPPORT_REPLIED);

  public static final List<Event> EVENTS = List.of(
      new Event(OTP_SIGN_IN, "Sign-in code (2FA)", ESSENTIAL, NotificationAudience.PERSON, List.of("code"), true,
          "LUTIANO : votre code de connexion est {code}. Valable 5 minutes. Ne le partagez avec personne.",
          "LUTIANO: your sign-in code is {code}. Valid for 5 minutes. Never share it."),
      new Event(OTP_PAYOUT_NUMBER, "Payout number code", ESSENTIAL, NotificationAudience.PERSON, List.of("code"), true,
          "LUTIANO : code {code} pour confirmer ce numéro de paiement. Valable 5 minutes. Communiquez-le uniquement à votre responsable LUTIANO.",
          "LUTIANO: code {code} to confirm this payout number. Valid for 5 minutes. Give it only to your LUTIANO manager."),
      new Event(OTP_PASSWORD_RESET, "Password reset code", ESSENTIAL, NotificationAudience.PERSON, List.of("code"), true,
          "LUTIANO : code {code} pour réinitialiser votre mot de passe. Valable 5 minutes. Ne le partagez avec personne.",
          "LUTIANO: code {code} to reset your password. Valid for 5 minutes. Never share it."),
      new Event(PAYOUT_APPROVED, "Payout approved", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "amount", "reference"), false,
          "LUTIANO : votre paiement {reference} de {amount} est approuvé et sera effectué prochainement.",
          "LUTIANO: your payout {reference} of {amount} is approved and will be paid soon."),
      new Event(PAYOUT_PAID, "Payout paid", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "amount", "method", "reference"), false,
          "LUTIANO : {amount} vous ont été versés par {method} (réf. {reference}).",
          "LUTIANO: {amount} was paid to you by {method} (ref. {reference})."),
      new Event(PAYOUT_REVERSED, "Payout reversed", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "amount", "reference"), false,
          "LUTIANO : le paiement {reference} de {amount} a été corrigé et annulé. Ce montant est de nouveau à payer.",
          "LUTIANO: payout {reference} of {amount} was corrected and reversed. The amount is payable again."),
      new Event(PAYOUT_NUMBER_VERIFIED, "Payout number confirmed", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "provider", "phone"), false,
          "LUTIANO : votre numéro {provider} {phone} est confirmé pour vos paiements.",
          "LUTIANO: your {provider} number {phone} is confirmed for your payouts."),
      new Event(PAYOUT_NUMBER_REJECTED, "Payout number rejected", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "provider", "phone"), false,
          "LUTIANO : votre numéro {provider} {phone} n'a pas pu être confirmé. Contactez votre responsable.",
          "LUTIANO: your {provider} number {phone} could not be confirmed. Contact your manager."),
      new Event(EARNINGS_FINALIZED, "Monthly earnings finalised", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "month", "total"), false,
          "LUTIANO : vos gains de {month} sont arrêtés : {total}. Détail dans l'application.",
          "LUTIANO: your {month} earnings are final: {total}. Details in the app."),
      new Event(TARGET_REACHED, "Target reached", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "target", "bonus"), false,
          "LUTIANO : bravo ! Objectif « {target} » atteint, prime de {bonus}.",
          "LUTIANO: well done! Target \"{target}\" reached, bonus {bonus}."),
      new Event(APPROVAL_REFUND, "Refund waiting for approval", ESSENTIAL, NotificationAudience.APPROVERS_FINANCE, List.of("reference", "amount", "by"), false,
          "LUTIANO : remboursement {reference} de {amount} demandé par {by}, à approuver.",
          "LUTIANO: refund {reference} of {amount} requested by {by} needs approval."),
      new Event(APPROVAL_PAYOUT, "Payout waiting for approval", ESSENTIAL, NotificationAudience.APPROVERS_FINANCE, List.of("reference", "amount", "by"), false,
          "LUTIANO : paiement {reference} de {amount} enregistré par {by}, à approuver.",
          "LUTIANO: payout {reference} of {amount} recorded by {by} needs approval."),
      new Event(APPROVAL_PAYOUT_REVERSAL, "Payout reversal waiting for approval", ESSENTIAL, NotificationAudience.APPROVERS_FINANCE, List.of("reference", "amount", "by"), false,
          "LUTIANO : annulation du paiement {reference} ({amount}) demandée par {by}, à approuver.",
          "LUTIANO: reversal of payout {reference} ({amount}) requested by {by} needs approval."),
      new Event(APPROVAL_CASH_OUTFLOW, "Cash outflow waiting for approval", ESSENTIAL, NotificationAudience.APPROVERS_FINANCE, List.of("reference", "amount", "by"), false,
          "LUTIANO : sortie de caisse {reference} de {amount} demandée par {by}, à approuver.",
          "LUTIANO: cash outflow {reference} of {amount} requested by {by} needs approval."),
      new Event(APPROVAL_STOCK, "Stock request waiting for approval", ESSENTIAL, NotificationAudience.APPROVERS_STOCK, List.of("what", "by"), false,
          "LUTIANO : demande de stock de {by} en attente d'approbation : {what}.",
          "LUTIANO: stock request from {by} is waiting for approval: {what}."),
      new Event(CASH_HANDOVER, "Cash handover to acknowledge", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "amount", "from", "reference"), false,
          "LUTIANO : {from} vous remet {amount} (réf. {reference}). Confirmez la réception dans l'application.",
          "LUTIANO: {from} is handing you {amount} (ref. {reference}). Confirm receipt in the app."),
      new Event(SHIFT_VARIANCE, "Cash shift difference to review", ESSENTIAL, NotificationAudience.APPROVERS_FINANCE, List.of("seller", "amount"), false,
          "LUTIANO : écart de caisse de {amount} sur la caisse de {seller}, à examiner.",
          "LUTIANO: a cash difference of {amount} on {seller}'s shift needs review."),
      new Event(STOCK_LOW, "Stock running low", ESSENTIAL, NotificationAudience.STOCK_ALERT, List.of("product", "location", "quantity"), false,
          "LUTIANO : stock bas — {product} ({location}) : {quantity} restant(s).",
          "LUTIANO: low stock — {product} ({location}): {quantity} left."),
      new Event(STOCK_OUT, "Out of stock", ESSENTIAL, NotificationAudience.STOCK_ALERT, List.of("product", "location", "quantity"), false,
          "LUTIANO : rupture de stock — {product} ({location}).",
          "LUTIANO: out of stock — {product} ({location})."),
      new Event(STOCK_IN_TRANSIT, "Stock on its way", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "location"), false,
          "LUTIANO : du stock est en route vers {location}. Confirmez la réception dans l'application.",
          "LUTIANO: stock is on its way to {location}. Confirm receipt in the app."),
      new Event(STOCK_REQUEST_APPROVED, "Stock request approved", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "reference"), false,
          "LUTIANO : votre demande de stock {reference} est approuvée et en préparation.",
          "LUTIANO: your stock request {reference} is approved and being prepared."),
      new Event(STOCK_REQUEST_REJECTED, "Stock request rejected", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "reference", "reason"), false,
          "LUTIANO : votre demande de stock {reference} est refusée. Motif : {reason}",
          "LUTIANO: your stock request {reference} was rejected. Reason: {reason}"),
      new Event(TARGET_MILESTONE, "Target at 80%", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "target", "percent"), false,
          "LUTIANO : vous êtes à {percent} % de l'objectif « {target} ». Continuez !",
          "LUTIANO: you are at {percent}% of the target \"{target}\". Keep going!"),
      new Event(TARGET_GOAL_REACHED, "Target reached", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "target"), false,
          "LUTIANO : objectif « {target} » atteint ! Bravo.",
          "LUTIANO: target \"{target}\" reached! Well done."),
      new Event(SHIFT_STILL_OPEN, "Cash shift still open", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "since"), false,
          "LUTIANO : votre caisse est ouverte depuis le {since}. Comptez-la et clôturez le shift.",
          "LUTIANO: your cash shift has been open since {since}. Count the drawer and close it."),
      new Event(ORDER_PLACED, "New customer order", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "reference", "customer", "total"), false,
          "LUTIANO : nouvelle commande {reference} de {customer} ({total}).",
          "LUTIANO: new order {reference} from {customer} ({total})."),
      new Event(ORDER_ACCEPTED, "Order accepted", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "reference"), false,
          "LUTIANO : votre commande {reference} est acceptée, elle est en préparation.",
          "LUTIANO: your order {reference} was accepted and is being prepared."),
      new Event(ORDER_REJECTED, "Order rejected", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "reference", "reason"), false,
          "LUTIANO : votre commande {reference} n'a pas pu être acceptée. {reason}",
          "LUTIANO: your order {reference} could not be accepted. {reason}"),
      new Event(ORDER_READY, "Order ready", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "reference"), false,
          "LUTIANO : votre commande {reference} est prête, vous pouvez la récupérer.",
          "LUTIANO: your order {reference} is ready to collect."),
      new Event(ORDER_COLLECTED, "Order collected", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "reference"), false,
          "LUTIANO : merci ! Votre commande {reference} est terminée.",
          "LUTIANO: thank you! Your order {reference} is complete."),
      new Event(ORDER_COLLECTED_POINTS, "Order collected, points earned", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "reference", "points"), false,
          "LUTIANO : merci ! Commande {reference} terminée, vous avez gagné {points} points.",
          "LUTIANO: thank you! Order {reference} is complete and you earned {points} points."),
      new Event(ORDER_CANCELLED, "Order cancelled", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "reference"), false,
          "LUTIANO : votre commande {reference} est annulée.",
          "LUTIANO: your order {reference} was cancelled."),
      new Event(ORDER_EXPIRED, "Order expired", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "reference"), false,
          "LUTIANO : votre commande {reference} a expiré, personne ne l'a prise en charge à temps.",
          "LUTIANO: your order {reference} expired because it was not handled in time."),
      new Event(REWARD_REDEEMED, "Reward redeemed", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "reward", "until"), false,
          "LUTIANO : « {reward} » est dans votre portefeuille de récompenses, valable jusqu'au {until}.",
          "LUTIANO: \"{reward}\" is in your rewards wallet, valid until {until}."),
      new Event(REWARD_USED, "Reward used", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "reward"), false,
          "LUTIANO : votre récompense « {reward} » a été utilisée. Bonne dégustation !",
          "LUTIANO: your reward \"{reward}\" was used. Enjoy!"),
      new Event(REWARD_EXPIRED, "Reward expired", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "reward"), false,
          "LUTIANO : votre récompense « {reward} » a expiré.",
          "LUTIANO: your reward \"{reward}\" has expired."),
      new Event(OTP_CUSTOMER, "Customer verification code", ESSENTIAL, NotificationAudience.PERSON, List.of("code"), true,
          "LUTIANO : votre code de vérification est {code}. Valable 5 minutes. Ne le partagez avec personne.",
          "LUTIANO: your verification code is {code}. Valid for 5 minutes. Never share it."),
      new Event(OTP_PHONE_CHANGE, "New phone number code", ESSENTIAL, NotificationAudience.PERSON, List.of("code"), true,
          "LUTIANO : code {code} pour confirmer votre nouveau numéro. Valable 5 minutes. Ne le partagez avec personne.",
          "LUTIANO: code {code} to confirm your new phone number. Valid for 5 minutes. Never share it."),
      new Event(CAMPAIGN_BONUS, "Campaign bonus", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "campaign", "bonus"), false,
          "LUTIANO : campagne « {campaign} » terminée, prime de {bonus} ajoutée à vos gains. Bravo !",
          "LUTIANO: campaign \"{campaign}\" is over, a bonus of {bonus} was added to your earnings. Well done!"),
      new Event(SUPPORT_REPLIED, "Support request answered", ESSENTIAL, NotificationAudience.PERSON, List.of("name", "reference"), false,
          "LUTIANO : l'équipe a répondu à votre demande {reference}. Ouvrez l'application pour lire la réponse.",
          "LUTIANO: the team answered your request {reference}. Open the app to read the reply."));

  private static final Map<String, Event> BY_KEY = new LinkedHashMap<>();
  static { for (Event e : EVENTS) BY_KEY.put(e.key(), e); }
  private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");

  private NotificationCatalog() {}

  /** The area of the platform a message is about, for grouping in the inbox. */
  public static String topic(String key) {
    return switch (key) {
      case PAYOUT_APPROVED, PAYOUT_PAID, PAYOUT_REVERSED, PAYOUT_NUMBER_VERIFIED, PAYOUT_NUMBER_REJECTED, APPROVAL_PAYOUT, APPROVAL_PAYOUT_REVERSAL,
           APPROVAL_CASH_OUTFLOW, CASH_HANDOVER, SHIFT_VARIANCE -> "PAYMENTS";
      case REWARD_REDEEMED, REWARD_USED, REWARD_EXPIRED, APPROVAL_REFUND, ORDER_PLACED, ORDER_ACCEPTED, ORDER_REJECTED, ORDER_READY, ORDER_COLLECTED, ORDER_COLLECTED_POINTS, ORDER_CANCELLED, ORDER_EXPIRED -> "SALES";
      case STOCK_LOW, STOCK_OUT, STOCK_IN_TRANSIT, APPROVAL_STOCK, STOCK_REQUEST_APPROVED, STOCK_REQUEST_REJECTED -> "INVENTORY";
      case EARNINGS_FINALIZED, TARGET_REACHED, CAMPAIGN_BONUS -> "COMMISSIONS";
      default -> "SECURITY";
    };
  }

  /** The in-app category of an event, as the Seller app groups notifications. */
  public static String inAppCategory(String key) {
    return switch (key) {
      case STOCK_LOW, STOCK_OUT, STOCK_IN_TRANSIT -> "INVENTORY";
      case APPROVAL_STOCK, STOCK_REQUEST_APPROVED, STOCK_REQUEST_REJECTED -> "REQUESTS";
      case APPROVAL_REFUND, ORDER_PLACED -> "SALES";
      case ORDER_ACCEPTED, ORDER_REJECTED, ORDER_READY, ORDER_COLLECTED, ORDER_COLLECTED_POINTS, ORDER_CANCELLED, ORDER_EXPIRED -> "ORDERS";
      case REWARD_REDEEMED, REWARD_USED, REWARD_EXPIRED -> "LOYALTY";
      case PAYOUT_APPROVED, PAYOUT_PAID, PAYOUT_REVERSED, PAYOUT_NUMBER_VERIFIED, PAYOUT_NUMBER_REJECTED, EARNINGS_FINALIZED, CAMPAIGN_BONUS, APPROVAL_PAYOUT, APPROVAL_PAYOUT_REVERSAL -> "EARNINGS";
      case TARGET_REACHED, TARGET_MILESTONE, TARGET_GOAL_REACHED -> "TARGETS";
      case CASH_HANDOVER, SHIFT_VARIANCE, SHIFT_STILL_OPEN, APPROVAL_CASH_OUTFLOW -> "OPERATIONS";
      default -> "SYSTEM";
    };
  }

  public static List<String> keysOfTopic(String topic) { return EVENTS.stream().map(Event::key).filter(k -> topic(k).equals(topic)).toList(); }

  public static Event get(String key) {
    Event e = BY_KEY.get(key);
    if (e == null) throw new IllegalArgumentException("Unknown notification event " + key);
    return e;
  }

  public static boolean exists(String key) { return BY_KEY.containsKey(key); }

  /** The text with each {placeholder} replaced by its value (empty when missing). */
  public static String render(String template, Map<String, String> params) {
    Matcher m = PLACEHOLDER.matcher(template);
    StringBuilder out = new StringBuilder();
    while (m.find()) m.appendReplacement(out, Matcher.quoteReplacement(params.getOrDefault(m.group(1), "")));
    return m.appendTail(out).toString();
  }

  /** The placeholders a text uses that the event does not allow. */
  public static List<String> unknownPlaceholders(Event event, String template) {
    Matcher m = PLACEHOLDER.matcher(template);
    ArrayList<String> bad = new ArrayList<>();
    while (m.find()) if (!event.placeholders().contains(m.group(1)) && !bad.contains(m.group(1))) bad.add(m.group(1));
    return bad;
  }
}

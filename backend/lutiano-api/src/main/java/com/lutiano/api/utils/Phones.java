package com.lutiano.api.utils;

/** Cameroonian mobile numbers: stored as +2376XXXXXXXX, shown with the middle hidden. */
public final class Phones {
  private Phones() {}

  /** The number as +2376XXXXXXXX, or null when it is not a Cameroonian mobile number. Accepts spaces, dashes, "237" or "+237" in front. */
  public static String normalize(String raw) {
    if (raw == null) return null;
    String digits = raw.replaceAll("[\\s.()-]", "");
    if (digits.startsWith("+")) digits = digits.substring(1);
    if (digits.startsWith("237")) digits = digits.substring(3);
    return digits.matches("6\\d{8}") ? "+237" + digits : null;
  }

  /** Like {@link #normalize} but a bad number is a 400 with a message the user can act on. */
  public static String require(String raw, String field) {
    String phone = normalize(Validate.text(raw, field, 30));
    if (phone == null) throw Validate.bad("Enter a Cameroonian mobile number, e.g. +237 6XX XX XX XX.");
    return phone;
  }

  /** +237 6•• •• •• 22: enough to recognise the number, not to use it. */
  public static String mask(String phone) {
    return phone == null || phone.length() < 6 ? "" : phone.substring(0, 5) + "•• •• •• " + phone.substring(phone.length() - 2);
  }
}

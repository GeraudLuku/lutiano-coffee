package com.lutiano.api.service.reference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

/** Cameroon's ten regions and their main towns, used for sales location addresses. */
public final class CameroonRegions {
  public static final Map<String, List<String>> CITIES_BY_REGION;

  static {
    Map<String, List<String>> m = new LinkedHashMap<>();
    m.put("Adamawa", List.of("Ngaoundéré", "Meiganga", "Tibati", "Tignère", "Banyo"));
    m.put("Centre", List.of("Yaoundé", "Mbalmayo", "Obala", "Bafia", "Soa", "Mfou", "Eséka", "Akonolinga", "Nanga-Eboko"));
    m.put("East", List.of("Bertoua", "Batouri", "Abong-Mbang", "Yokadouma", "Bélabo"));
    m.put("Far North", List.of("Maroua", "Kousséri", "Mokolo", "Yagoua", "Mora", "Kaélé"));
    m.put("Littoral", List.of("Douala", "Edéa", "Nkongsamba", "Loum", "Mbanga", "Manjo", "Yabassi"));
    m.put("North", List.of("Garoua", "Guider", "Figuil", "Pitoa", "Poli", "Tcholliré"));
    m.put("North-West", List.of("Bamenda", "Kumbo", "Ndop", "Wum", "Nkambé", "Mbengwi", "Bali"));
    m.put("South", List.of("Ebolowa", "Kribi", "Sangmélima", "Ambam", "Lolodorf", "Campo"));
    m.put("South-West", List.of("Buea", "Limbe", "Kumba", "Tiko", "Mutengene", "Mamfe", "Idenau"));
    m.put("West", List.of("Bafoussam", "Dschang", "Foumban", "Mbouda", "Bafang", "Bangangté", "Baham"));
    CITIES_BY_REGION = Collections.unmodifiableMap(m);
  }

  private CameroonRegions() {}

  public static boolean isRegion(String region) { return CITIES_BY_REGION.containsKey(region); }
}

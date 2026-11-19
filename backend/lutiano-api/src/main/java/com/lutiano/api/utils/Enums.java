package com.lutiano.api.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The constant names of an enum as strings (in declaration order), for validating values that arrive and are stored as text. */
public final class Enums {
  private Enums() {}

  public static <E extends Enum<E>> Set<String> names(Class<E> type) {
    Set<String> out = new LinkedHashSet<>();
    for (E e : type.getEnumConstants()) out.add(e.name());
    return Collections.unmodifiableSet(out);
  }

  public static <E extends Enum<E>> List<String> list(Class<E> type) {
    return Arrays.stream(type.getEnumConstants()).map(Enum::name).toList();
  }
}

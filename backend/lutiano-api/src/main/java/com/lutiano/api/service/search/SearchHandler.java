package com.lutiano.api.service.search;

import java.util.List;
import java.util.Optional;

/** Turns one kind of MariaDB record into its search document. Always reads current state, never the event's contents. */
public interface SearchHandler {
  String type();

  /** The document for this record now, or empty when it no longer exists (the document is then removed). */
  Optional<SearchDocument> load(String id);

  /** Up to {@code limit} ids after {@code afterId} in id order, for backfills. */
  List<String> idsAfter(String afterId, int limit);
}

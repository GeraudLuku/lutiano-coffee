import 'package:flutter/widgets.dart';

import 'core_localizations.dart';

export 'core_localizations.dart';

/// The shared package's own texts: `context.coreL10n.retry`.
extension CoreL10nContext on BuildContext {
  CoreLocalizations get coreL10n => CoreLocalizations.of(this);
}

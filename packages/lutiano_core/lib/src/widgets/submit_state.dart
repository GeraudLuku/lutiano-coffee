import 'package:material_ui/material_ui.dart';

import '../core/api_client.dart';

/// Busy and error handling for a screen or dialog that sends something to the API.
///
/// [submit] turns the busy flag on, runs [action], and returns true when it succeeded. A failure is shown through
/// [error] (the server's message, never raw exception text) and returns false. Nothing runs after the widget is gone.
mixin SubmitState<T extends StatefulWidget> on State<T> {
  bool busy = false;
  String? error;

  /// [describe] lets a screen word a failure itself (for example to mention a saved draft); by default it is [errorMessage].
  Future<bool> submit(Future<void> Function() action,
      {String Function(Object error)? describe}) async {
    setState(() {
      busy = true;
      error = null;
    });
    try {
      await action();
      if (mounted) setState(() => busy = false);
      return mounted;
    } catch (e) {
      if (mounted) {
        setState(() {
          error = (describe ?? errorMessage)(e);
          busy = false;
        });
      }
      return false;
    }
  }
}

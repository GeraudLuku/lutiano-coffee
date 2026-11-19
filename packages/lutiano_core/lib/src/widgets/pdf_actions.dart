import 'dart:typed_data';

import 'package:flutter/widgets.dart';
import 'package:material_ui/material_ui.dart'
    show AlertDialog, showDialog, CircularProgressIndicator;
import 'package:printing/printing.dart';

import '../core/api_client.dart';
import 'feedback.dart';

/// Fetches a server-generated PDF, with a small "preparing" dialog while it loads, and opens it in the system print / share sheet.
/// A failure is shown as a message (the server's wording, never raw exception text).
Future<void> openPdf(BuildContext context,
    {required Future<List<int>> Function() fetch,
    required String fileName,
    required String preparing}) async {
  final navigator = Navigator.of(context, rootNavigator: true);
  final messenger = context;
  showDialog<void>(
    context: context,
    barrierDismissible: false,
    builder: (_) => AlertDialog(
      content: Row(children: [
        const SizedBox(
            width: 28,
            height: 28,
            child: CircularProgressIndicator(strokeWidth: 3)),
        const SizedBox(width: 16),
        Expanded(child: Text(preparing)),
      ]),
    ),
  );
  Uint8List? bytes;
  Object? failure;
  try {
    bytes = Uint8List.fromList(await fetch());
  } catch (e) {
    failure = e;
  }
  navigator.pop();
  if (bytes != null) {
    await Printing.layoutPdf(onLayout: (_) async => bytes!, name: fileName);
  } else if (messenger.mounted) {
    showMessage(messenger, errorMessage(failure!), error: true);
  }
}

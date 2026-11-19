import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import '../../core/l10n.dart';

/// Scans the QR code of a sales point and returns its text; "Enter code manually" returns what was typed. Null when the customer backs out.
class ScanScreen extends StatefulWidget {
  const ScanScreen({super.key});

  @override
  State<ScanScreen> createState() => _ScanScreenState();
}

class _ScanScreenState extends State<ScanScreen> {
  final _controller =
      MobileScannerController(formats: const [BarcodeFormat.qrCode]);
  bool _done = false;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _onDetect(BarcodeCapture capture) {
    if (_done) return;
    final value =
        capture.barcodes.map((b) => b.rawValue).whereType<String>().firstOrNull;
    if (value == null || value.isEmpty) return;
    _done = true;
    Navigator.of(context).pop(value);
  }

  Future<void> _typeCode() async {
    final l = context.l10n;
    final code = await showDialog<String>(
      context: context,
      builder: (ctx) => DialogFields(
          builder: (ctx, f) => AlertDialog(
                title: Text(l.enterCodeManually),
                content: TextField(
                    controller: f[0],
                    autofocus: true,
                    decoration:
                        InputDecoration(hintText: l.salesPointCodeHint)),
                actions: [
                  TextButton(
                      onPressed: () => Navigator.of(ctx).pop(),
                      child: Text(l.cancel)),
                  FilledButton(
                      onPressed: () => Navigator.of(ctx).pop(f[0].text),
                      child: Text(l.continueLabel)),
                ],
              )),
    );
    if (code != null && code.trim().isNotEmpty && mounted) {
      _done = true;
      Navigator.of(context).pop(code.trim());
    }
  }

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
          title: Text(l.scanToOrder),
          backgroundColor: Colors.black,
          foregroundColor: Colors.white),
      body: Stack(children: [
        MobileScanner(
            controller: _controller,
            onDetect: _onDetect,
            errorBuilder: (context, error) => Center(
                child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Text(l.cameraUnavailable,
                        textAlign: TextAlign.center,
                        style: const TextStyle(color: Colors.white))))),
        Positioned(
          left: 0,
          right: 0,
          bottom: 32,
          child: Column(children: [
            Text(l.pointAtQr, style: const TextStyle(color: Colors.white)),
            const SizedBox(height: 12),
            OutlinedButton.icon(
                style: OutlinedButton.styleFrom(
                    foregroundColor: Colors.white,
                    side: const BorderSide(color: Colors.white)),
                onPressed: _typeCode,
                icon: const Icon(Icons.keyboard_outlined),
                label: Text(l.enterCodeManually)),
          ]),
        ),
      ]),
    );
  }
}

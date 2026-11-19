import 'package:material_ui/material_ui.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';

/// Scans a customer's loyalty QR code and returns its text (the app never types or shows the token).
/// Returns null when the seller backs out.
class ScanCustomerScreen extends StatefulWidget {
  const ScanCustomerScreen({super.key});

  @override
  State<ScanCustomerScreen> createState() => _ScanCustomerScreenState();
}

class _ScanCustomerScreenState extends State<ScanCustomerScreen> {
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

  @override
  Widget build(BuildContext context) => Scaffold(
        backgroundColor: Colors.black,
        appBar: AppBar(title: Text(context.l10n.scanCustomerQr)),
        body: Stack(children: [
          MobileScanner(
            controller: _controller,
            onDetect: _onDetect,
            errorBuilder: (context, error) => Center(
              child: Padding(
                padding: EdgeInsets.all(24),
                child: Text(context.l10n.theCameraIsNotAvailable,
                    textAlign: TextAlign.center,
                    style: TextStyle(color: Colors.white)),
              ),
            ),
          ),
          Align(
            alignment: Alignment.bottomCenter,
            child: Padding(
              padding: EdgeInsets.all(24),
              child: Text(context.l10n.pointTheCameraAtThe,
                  textAlign: TextAlign.center,
                  style: TextStyle(color: AppColors.white70)),
            ),
          ),
        ]),
      );
}

/// Opens the scanner and returns the scanned text, or null.
Future<String?> scanCustomerQr(BuildContext context) =>
    Navigator.of(context).push<String>(
        MaterialPageRoute(builder: (_) => const ScanCustomerScreen()));

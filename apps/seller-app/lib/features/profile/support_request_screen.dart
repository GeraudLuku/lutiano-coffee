import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'support_widgets.dart';

/// Batch 8 · Screen 8 — Contact support: type, subject, description and an optional image (up to 10 MB).
class SupportRequestScreen extends StatefulWidget {
  const SupportRequestScreen({super.key, this.type = SupportType.question});
  final SupportType type;

  @override
  State<SupportRequestScreen> createState() => _SupportRequestScreenState();
}

class _SupportRequestScreenState extends State<SupportRequestScreen>
    with SubmitState<SupportRequestScreen> {
  late SupportType _type = widget.type;
  final _subject = TextEditingController(),
      _description = TextEditingController();
  PickedImage? _image;

  @override
  void dispose() {
    _subject.dispose();
    _description.dispose();
    super.dispose();
  }

  Future<void> _attach() async {
    final picked = await pickImage(context);
    if (picked == null || !mounted) return;
    if (picked.bytes.length > maxImageBytes) {
      showMessage(context, context.l10n.fileTooBig, error: true);
      return;
    }
    setState(() => _image = picked);
  }

  Future<void> _send() async {
    final l = context.l10n;
    final app = AppScope.read(context);
    if (_subject.text.trim().isEmpty || _description.text.trim().isEmpty) {
      setState(() => error = l.supportFillIn);
      return;
    }
    SupportRequest? created;
    final ok = await submit(() async {
      created = await app.support.create(
          type: _type,
          subject: _subject.text.trim(),
          description: _description.text.trim(),
          fileBytes: _image?.bytes,
          fileName: _image?.name,
          contentType: _image?.contentType);
    });
    if (!ok || !mounted) return;
    app.dataChanged();
    showMessage(context, l.requestSentRef(created!.reference));
    Navigator.pop(context);
  }

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.contactSupport)),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        InputDecorator(
          decoration: InputDecoration(labelText: l.requestType),
          child: DropdownButtonHideUnderline(
            child: DropdownButton<SupportType>(
              isExpanded: true,
              value: _type,
              items: [
                for (final t in SupportType.values)
                  DropdownMenuItem(
                      value: t, child: Text(supportTypeLabel(context, t))),
              ],
              onChanged: (t) => setState(() => _type = t ?? _type),
            ),
          ),
        ),
        const SizedBox(height: 12),
        TextField(
            controller: _subject,
            maxLength: 150,
            decoration: InputDecoration(
                labelText: l.supportSubject, hintText: l.supportSubjectHint)),
        TextField(
            controller: _description,
            maxLength: 2000,
            minLines: 4,
            maxLines: 8,
            decoration: InputDecoration(
                labelText: l.supportDescription,
                hintText: l.supportDescriptionHint)),
        const SizedBox(height: 8),
        OutlinedButton.icon(
          onPressed: busy ? null : _attach,
          icon: const Icon(Icons.attach_file),
          label: Text(_image?.name ?? l.attachImage),
        ),
        if (_image != null)
          Align(
              alignment: Alignment.centerLeft,
              child: TextButton(
                  onPressed: () => setState(() => _image = null),
                  child: Text(l.removeAttachment))),
        Text(l.maxTenMb,
            style: const TextStyle(color: AppColors.muted, fontSize: 12.5)),
        if (error != null) ...[const SizedBox(height: 12), ErrorBanner(error!)],
        const SizedBox(height: 16),
        FilledButton.icon(
          onPressed: busy ? null : _send,
          icon: const Icon(Icons.send),
          label: Text(busy ? l.sendingRequest : l.submitRequest),
        ),
      ]),
    );
  }
}

import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../state/app_scope.dart';
import 'request_details_screen.dart';

/// Write to the team: what it is about, a title, the details and an optional photo (a receipt, a screenshot).
class SupportRequestScreen extends StatefulWidget {
  const SupportRequestScreen({super.key});

  @override
  State<SupportRequestScreen> createState() => _SupportRequestScreenState();
}

class _SupportRequestScreenState extends State<SupportRequestScreen>
    with SubmitState<SupportRequestScreen> {
  final _form = GlobalKey<FormState>();
  final _subject = TextEditingController();
  final _description = TextEditingController();
  String _type = 'ORDER';
  PickedImage? _image;

  @override
  void dispose() {
    _subject.dispose();
    _description.dispose();
    super.dispose();
  }

  Future<void> _send() async {
    if (!_form.currentState!.validate()) return;
    final app = AppScope.read(context);
    String? id;
    final ok = await submit(() async {
      final r = await app.inbox.sendRequest(
          type: _type,
          subject: _subject.text.trim(),
          description: _description.text.trim(),
          imageBytes: _image?.bytes,
          imageName: _image?.name,
          imageType: _image?.contentType);
      id = r.id;
    });
    if (!ok || id == null || !mounted) return;
    showMessage(context, context.l10n.requestSent);
    await Navigator.of(context).pushReplacement(MaterialPageRoute(
        builder: (_) => RequestDetailsScreen(requestId: id!)));
  }

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    final types = {
      'ORDER': l.reqOrder,
      'REWARDS': l.reqRewards,
      'ACCOUNT': l.reqAccount,
      'ISSUE': l.reqIssue,
      'QUESTION': l.reqQuestion,
      'IMPROVEMENT': l.reqIdea
    };
    return Scaffold(
      appBar: AppBar(title: Text(l.contactSupport)),
      body: Form(
        key: _form,
        child: ListView(padding: const EdgeInsets.all(16), children: [
          DropdownButtonFormField<String>(
            initialValue: _type,
            decoration: InputDecoration(labelText: l.requestCategory),
            items: [
              for (final e in types.entries)
                DropdownMenuItem(value: e.key, child: Text(e.value))
            ],
            onChanged: (v) => setState(() => _type = v ?? _type),
          ),
          const SizedBox(height: 12),
          TextFormField(
              controller: _subject,
              maxLength: 150,
              decoration: InputDecoration(labelText: l.requestTitle),
              validator: (v) =>
                  (v == null || v.trim().length < 3) ? l.enterATitle : null),
          TextFormField(
              controller: _description,
              maxLength: 2000,
              minLines: 4,
              maxLines: 8,
              decoration: InputDecoration(labelText: l.requestDescription),
              validator: (v) => (v == null || v.trim().length < 5)
                  ? l.describeTheProblem
                  : null),
          const SizedBox(height: 8),
          OutlinedButton.icon(
              icon: const Icon(Icons.attach_file),
              label: Text(_image == null ? l.addPhotoOptional : _image!.name),
              onPressed: () async {
                final picked = await pickImage(context);
                if (picked != null && mounted) setState(() => _image = picked);
              }),
          const SizedBox(height: 16),
          if (error != null) ErrorBanner(error!),
          FilledButton(
              onPressed: busy ? null : _send,
              child: Text(busy ? l.pleaseWait : l.submitRequest)),
        ]),
      ),
    );
  }
}

import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import 'package:lutiano_core/lutiano_core.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';

/// Batch 8 · Screen 2 — Edit profile: photo and name; a new phone number is confirmed with a code sent to it. Email stays with the admin.
class EditProfileScreen extends StatefulWidget {
  const EditProfileScreen({super.key});

  @override
  State<EditProfileScreen> createState() => _EditProfileScreenState();
}

class _EditProfileScreenState extends State<EditProfileScreen>
    with SubmitState<EditProfileScreen> {
  late final TextEditingController _name =
      TextEditingController(text: AppScope.read(context).user!.fullName);

  @override
  void dispose() {
    _name.dispose();
    super.dispose();
  }

  Future<void> _photo() async {
    final app = AppScope.read(context);
    final picked = await pickImage(context);
    if (picked == null || !mounted) return;
    if (picked.bytes.length > maxImageBytes) {
      showMessage(context, context.l10n.fileTooBig, error: true);
      return;
    }
    final ok = await submit(() async {
      await app.account
          .uploadPhoto(picked.bytes, picked.name, picked.contentType);
      await app.refreshContext();
      app.dataChanged();
    });
    if (ok && mounted) showMessage(context, context.l10n.photoUpdated);
  }

  Future<void> _save() async {
    final app = AppScope.read(context);
    final name = _name.text.trim();
    if (name.length < 2) {
      setState(() => error = context.l10n.enterYourName);
      return;
    }
    final ok = await submit(() async {
      app.user = await app.account.update(fullName: name);
      app.dataChanged();
    });
    if (ok && mounted) {
      showMessage(context, context.l10n.profileSaved);
      Navigator.pop(context);
    }
  }

  Future<void> _changePhone() async {
    final done = await showDialog<bool>(
        context: context, builder: (_) => const _PhoneChangeDialog());
    if (done == true && mounted) {
      setState(() {});
      showMessage(context, context.l10n.phoneChanged);
    }
  }

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final user = app.user!;
    final l = context.l10n;
    final photo = app.api.mediaUrl(user.imageUrl);
    return Scaffold(
      appBar: AppBar(title: Text(l.editProfile)),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        Center(
          child: Stack(children: [
            CircleAvatar(
              radius: 52,
              backgroundColor: AppColors.navy,
              backgroundImage: photo == null ? null : NetworkImage(photo),
              child: photo != null
                  ? null
                  : Text(
                      user.firstName.isEmpty
                          ? '?'
                          : user.firstName[0].toUpperCase(),
                      style: const TextStyle(
                          color: AppColors.gold,
                          fontSize: 40,
                          fontWeight: FontWeight.w800)),
            ),
            Positioned(
              right: 0,
              bottom: 0,
              child: IconButton.filled(
                tooltip: l.changePhoto,
                onPressed: busy ? null : _photo,
                icon: const Icon(Icons.photo_camera, size: 18),
                style: IconButton.styleFrom(
                    backgroundColor: AppColors.gold,
                    foregroundColor: AppColors.navy),
              ),
            ),
          ]),
        ),
        const SizedBox(height: 20),
        TextField(
            controller: _name,
            maxLength: 150,
            textCapitalization: TextCapitalization.words,
            decoration: InputDecoration(labelText: l.fullNameLabel)),
        AppCard(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          child: Column(children: [
            MenuTile(
                icon: Icons.phone_outlined,
                title: l.phone,
                subtitle: user.phone ?? l.notSet,
                onTap: _changePhone),
            MenuTile(
                icon: Icons.mail_outline, title: l.email, subtitle: user.email),
          ]),
        ),
        const SizedBox(height: 8),
        InfoNote(l.emailReadOnly),
        if (error != null) ...[const SizedBox(height: 12), ErrorBanner(error!)],
        const SizedBox(height: 16),
        FilledButton.icon(
          onPressed: busy ? null : _save,
          icon: const Icon(Icons.check),
          label: Text(busy ? l.saving : l.saveChanges),
        ),
      ]),
    );
  }
}

/// Asks for the new number, sends a code to it, and confirms the code. Pops true when the number changed.
class _PhoneChangeDialog extends StatefulWidget {
  const _PhoneChangeDialog();

  @override
  State<_PhoneChangeDialog> createState() => _PhoneChangeDialogState();
}

class _PhoneChangeDialogState extends State<_PhoneChangeDialog>
    with SubmitState<_PhoneChangeDialog> {
  final _phone = TextEditingController(), _code = TextEditingController();
  bool _sent = false;

  @override
  void dispose() {
    _phone.dispose();
    _code.dispose();
    super.dispose();
  }

  Future<void> _next() async {
    final app = AppScope.read(context);
    if (!_sent) {
      final ok =
          await submit(() => app.account.startPhoneChange(_phone.text.trim()));
      if (ok && mounted) setState(() => _sent = true);
      return;
    }
    final ok = await submit(() async {
      app.user = await app.account.confirmPhone(_code.text.trim());
    });
    if (ok && mounted) Navigator.pop(context, true);
  }

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    return AlertDialog(
      title: Text(l.changePhone),
      content: SingleChildScrollView(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          if (!_sent)
            TextField(
                controller: _phone,
                keyboardType: TextInputType.phone,
                decoration: InputDecoration(
                    labelText: l.newPhoneLabel, hintText: '+237 6 90 00 00 00'))
          else ...[
            Text(l.codeSentTo(_phone.text.trim())),
            const SizedBox(height: 10),
            TextField(
                controller: _code,
                keyboardType: TextInputType.number,
                decoration: InputDecoration(labelText: l.confirmationCode)),
          ],
          if (error != null) ...[
            const SizedBox(height: 8),
            ErrorBanner(error!)
          ],
        ]),
      ),
      actions: [
        TextButton(
            onPressed: () => Navigator.pop(context), child: Text(l.cancel)),
        TextButton(
            onPressed: busy ? null : _next,
            child:
                Text(busy ? l.saving : (_sent ? l.confirm : l.sendPhoneCode))),
      ],
    );
  }
}

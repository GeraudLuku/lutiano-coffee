import 'package:image_picker/image_picker.dart';
import 'package:material_ui/material_ui.dart';

import '../l10n/core_l10n.dart';

/// A picked image: its bytes, a file name and the type the server expects.
class PickedImage {
  const PickedImage(this.bytes, this.name, this.contentType);
  final List<int> bytes;
  final String name, contentType;
}

const maxImageBytes = 10 * 1024 * 1024;

/// Asks camera or gallery, then returns the picked image (null when cancelled).
Future<PickedImage?> pickImage(BuildContext context) async {
  final source = await showModalBottomSheet<ImageSource>(
    context: context,
    builder: (ctx) => SafeArea(
      child: Column(mainAxisSize: MainAxisSize.min, children: [
        ListTile(
            leading: const Icon(Icons.photo_camera_outlined),
            title: Text(context.coreL10n.takePhoto),
            onTap: () => Navigator.pop(ctx, ImageSource.camera)),
        ListTile(
            leading: const Icon(Icons.photo_library_outlined),
            title: Text(context.coreL10n.chooseFromGallery),
            onTap: () => Navigator.pop(ctx, ImageSource.gallery)),
      ]),
    ),
  );
  if (source == null) return null;
  final file = await ImagePicker()
      .pickImage(source: source, maxWidth: 1600, imageQuality: 85);
  if (file == null) return null;
  final name = file.name.isEmpty ? 'photo.jpg' : file.name;
  final lower = name.toLowerCase();
  final type = lower.endsWith('.png')
      ? 'image/png'
      : lower.endsWith('.webp')
          ? 'image/webp'
          : 'image/jpeg';
  return PickedImage(await file.readAsBytes(), name, type);
}

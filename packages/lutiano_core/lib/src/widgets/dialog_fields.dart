import 'package:material_ui/material_ui.dart';

/// Owns the text fields of a dialog and disposes them when the dialog has left the screen.
///
/// Disposing controllers as soon as `showDialog` returns crashes ("A TextEditingController was used after being disposed"): the dialog is
/// still built while it fades out. So the dialog keeps its own controllers here and returns what was typed through `Navigator.pop`.
class DialogFields extends StatefulWidget {
  const DialogFields(
      {super.key,
      this.count = 1,
      this.initial = const [],
      required this.builder});

  /// How many fields; [initial] gives starting texts by position.
  final int count;
  final List<String> initial;
  final Widget Function(
      BuildContext context, List<TextEditingController> fields) builder;

  @override
  State<DialogFields> createState() => _DialogFieldsState();
}

class _DialogFieldsState extends State<DialogFields> {
  late final List<TextEditingController> _fields = List.generate(
      widget.count,
      (i) => TextEditingController(
          text: i < widget.initial.length ? widget.initial[i] : ''));

  @override
  void dispose() {
    for (final f in _fields) {
      f.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => widget.builder(context, _fields);
}

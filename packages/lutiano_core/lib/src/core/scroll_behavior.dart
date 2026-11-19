import 'package:material_ui/material_ui.dart';

/// Every scrollable can be pulled even when its content is shorter than the screen, so pull to refresh also works on short pages
/// (a seller with one request, or no deliveries yet, can still pull to see what is new).
class AppScrollBehavior extends MaterialScrollBehavior {
  const AppScrollBehavior();

  @override
  ScrollPhysics getScrollPhysics(BuildContext context) =>
      AlwaysScrollableScrollPhysics(parent: super.getScrollPhysics(context));
}

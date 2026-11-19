import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_seller/features/profile/about_screen.dart';
import 'package:lutiano_seller/state/app_scope.dart';
import 'package:lutiano_seller/state/app_state.dart';

import '../support/fake_api.dart';
import '../support/harness.dart';

void main() {
  testWidgets('About opens the terms and the privacy policy', (tester) async {
    final state = AppState(api: FakeApi({}).client());
    await tester.pumpWidget(
        AppScope(state: state, child: testApp(home: const AboutScreen())));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Terms of Use'));
    await tester.pumpAndSettle();
    expect(find.text('Your account'), findsOneWidget);
    expect(find.textContaining('Draft text'), findsOneWidget);
    await tester.pageBack();
    await tester.pumpAndSettle();

    await tester.tap(find.text('Privacy Policy'));
    await tester.pumpAndSettle();
    expect(find.text('What we keep about you'), findsOneWidget);
  });
}

// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'core_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class CoreLocalizationsEn extends CoreLocalizations {
  CoreLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get retry => 'Retry';

  @override
  String get all => 'All';

  @override
  String get loading => 'Loading';

  @override
  String get addToCart => 'Add to cart';

  @override
  String get decreaseQuantity => 'Decrease quantity';

  @override
  String get increaseQuantity => 'Increase quantity';

  @override
  String get errUnexpected => 'Something went wrong. Try again.';

  @override
  String get errNetwork =>
      'Cannot reach the LUTIANO server. Check your connection and try again.';

  @override
  String get errSessionExpired => 'Your session has expired. Sign in again.';

  @override
  String get errForbidden => 'You do not have permission to do this.';

  @override
  String errGeneric(int status) {
    return 'Something went wrong ($status). Try again.';
  }

  @override
  String get takePhoto => 'Take a photo';

  @override
  String get chooseFromGallery => 'Choose from gallery';
}

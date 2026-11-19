// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'core_localizations.dart';

// ignore_for_file: type=lint

/// The translations for French (`fr`).
class CoreLocalizationsFr extends CoreLocalizations {
  CoreLocalizationsFr([String locale = 'fr']) : super(locale);

  @override
  String get retry => 'Réessayer';

  @override
  String get all => 'Tous';

  @override
  String get loading => 'Chargement';

  @override
  String get addToCart => 'Ajouter au panier';

  @override
  String get decreaseQuantity => 'Diminuer la quantité';

  @override
  String get increaseQuantity => 'Augmenter la quantité';

  @override
  String get errUnexpected => 'Une erreur est survenue. Réessayez.';

  @override
  String get errNetwork =>
      'Impossible de joindre le serveur LUTIANO. Vérifiez votre connexion et réessayez.';

  @override
  String get errSessionExpired => 'Votre session a expiré. Reconnectez-vous.';

  @override
  String get errForbidden => 'Vous n\'avez pas l\'autorisation de faire cela.';

  @override
  String errGeneric(int status) {
    return 'Une erreur est survenue ($status). Réessayez.';
  }

  @override
  String get takePhoto => 'Prendre une photo';

  @override
  String get chooseFromGallery => 'Choisir dans la galerie';
}

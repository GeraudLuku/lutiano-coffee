// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for French (`fr`).
class AppLocalizationsFr extends AppLocalizations {
  AppLocalizationsFr([String locale = 'fr']) : super(locale);

  @override
  String get appTitle => 'LUTIANO Coffee';

  @override
  String get coffee => 'Coffee';

  @override
  String get tagline => 'Un café d\'exception, plus près de vous.';

  @override
  String get welcomeHeadline => 'Un grand café, plus près de vous';

  @override
  String get welcomeSubtitle =>
      'Du café d\'exception dans nos points de vente, chez nos baristas mobiles et sur vos marchés préférés.';

  @override
  String get getStarted => 'Commencer';

  @override
  String get alreadyHaveAccount => 'Vous avez déjà un compte ?';

  @override
  String get logIn => 'Se connecter';

  @override
  String get signUp => 'S\'inscrire';

  @override
  String get noAccount => 'Pas encore de compte ?';

  @override
  String get logInToYourAccount => 'Connectez-vous à votre compte';

  @override
  String get accessYourOrders =>
      'Accédez à vos commandes, vos récompenses et plus encore.';

  @override
  String get sessionEnded =>
      'Votre session a pris fin. Connectez-vous à nouveau.';

  @override
  String get phoneNumber => 'Numéro de téléphone';

  @override
  String get enterValidPhone =>
      'Entrez un numéro mobile camerounais (6XX XX XX XX).';

  @override
  String get password => 'Mot de passe';

  @override
  String get enterYourPassword => 'Entrez votre mot de passe.';

  @override
  String get passwordOptional =>
      'Mot de passe (facultatif, 10 caractères min.)';

  @override
  String get passwordTooShort => 'Utilisez au moins 10 caractères.';

  @override
  String get passwordsDoNotMatch => 'Les mots de passe ne correspondent pas.';

  @override
  String get confirmPassword => 'Confirmer le mot de passe';

  @override
  String get showPassword => 'Afficher le mot de passe';

  @override
  String get hidePassword => 'Masquer le mot de passe';

  @override
  String get signingIn => 'Connexion…';

  @override
  String get sendMeACode => 'M\'envoyer un code';

  @override
  String get forgotPassword => 'Mot de passe oublié ?';

  @override
  String get or => 'ou';

  @override
  String get continueWithGoogle => 'Continuer avec Google';

  @override
  String get continueWithApple => 'Continuer avec Apple';

  @override
  String get createYourAccount => 'Créez votre compte';

  @override
  String get joinLutiano =>
      'Rejoignez LUTIANO Coffee : du bon café et des récompenses.';

  @override
  String get fullName => 'Nom complet';

  @override
  String get enterYourName => 'Entrez votre nom.';

  @override
  String get emailOptional => 'E-mail (facultatif)';

  @override
  String get enterValidEmail => 'Entrez une adresse e-mail valide.';

  @override
  String get iAgreeTo => 'J\'accepte les';

  @override
  String get and => 'et la';

  @override
  String get iAgreeToTermsAndPrivacy =>
      'J\'accepte les conditions d\'utilisation et la politique de confidentialité';

  @override
  String get acceptTermsToContinue => 'Acceptez les conditions pour continuer.';

  @override
  String get createAccount => 'Créer le compte';

  @override
  String get pleaseWait => 'Veuillez patienter…';

  @override
  String get verifyAccount => 'Vérifier le compte';

  @override
  String get verifyYourPhone => 'Vérifiez votre téléphone';

  @override
  String enterCodeSentTo(String phone) {
    return 'Entrez le code à 6 chiffres envoyé au $phone.';
  }

  @override
  String get verify => 'Vérifier';

  @override
  String get verifying => 'Vérification…';

  @override
  String get resendCode => 'Renvoyer le code';

  @override
  String resendIn(int seconds) {
    return 'Renvoyer le code dans $seconds s';
  }

  @override
  String get codeSent => 'Un nouveau code a été envoyé.';

  @override
  String get resetPassword => 'Réinitialiser le mot de passe';

  @override
  String get weWillSendCode =>
      'Nous allons envoyer un code sur votre téléphone.';

  @override
  String get enterCodeAndNewPassword =>
      'Entrez le code reçu et choisissez un nouveau mot de passe.';

  @override
  String get sendCode => 'Envoyer le code';

  @override
  String get sixDigitCode => 'Code à 6 chiffres';

  @override
  String get enterTheCode => 'Entrez le code à 6 chiffres.';

  @override
  String get newPassword => 'Nouveau mot de passe';

  @override
  String get passwordChanged => 'Votre mot de passe a été changé.';

  @override
  String get oneLastStep => 'Dernière étape';

  @override
  String get addYourPhoneWhy =>
      'Les commandes et les récompenses suivent votre numéro de téléphone. Nous le confirmerons par SMS.';

  @override
  String get home => 'Accueil';

  @override
  String get menu => 'Menu';

  @override
  String get orders => 'Commandes';

  @override
  String get rewards => 'Récompenses';

  @override
  String get profile => 'Profil';

  @override
  String hello(String name) {
    return 'Bonjour, $name';
  }

  @override
  String get coffeeLover => 'Amateur de café';

  @override
  String get scanToOrder => 'Scanner pour commander';

  @override
  String get scanToOrderHint =>
      'Scannez le code QR d\'un point de vente LUTIANO.';

  @override
  String get findASalesPoint => 'Trouver un point de vente';

  @override
  String get recentSalesPoints => 'Points de vente récents';

  @override
  String get categories => 'Catégories';

  @override
  String get popularDrinks => 'Boissons populaires';

  @override
  String get seeAll => 'Voir tout';

  @override
  String get cart => 'Panier';

  @override
  String get searchMenu => 'Rechercher dans le menu…';

  @override
  String get noProductsFound => 'Rien trouvé';

  @override
  String get tryAnotherSearch => 'Essayez une autre recherche ou catégorie.';

  @override
  String get chooseSalesPointHint =>
      'Choisissez un point de vente pour voir ce qui est disponible et commander.';

  @override
  String orderingFrom(String name) {
    return 'Commande chez $name';
  }

  @override
  String get choose => 'Choisir';

  @override
  String get change => 'Changer';

  @override
  String fromPrice(String price) {
    return 'à partir de $price';
  }

  @override
  String get unavailableHere =>
      'Non disponible à ce point de vente pour le moment.';

  @override
  String get addToFavourites => 'Ajouter aux favoris';

  @override
  String get removeFromFavourites => 'Retirer des favoris';

  @override
  String get size => 'Taille';

  @override
  String get addOnsOptional => 'Suppléments (facultatif)';

  @override
  String addToCartPrice(String price) {
    return 'Ajouter au panier ($price)';
  }

  @override
  String get addedToCart => 'Ajouté au panier !';

  @override
  String addedToCartMessage(String name) {
    return '$name a été ajouté à votre panier.';
  }

  @override
  String get continueShopping => 'Continuer mes achats';

  @override
  String viewCart(int count) {
    return 'Voir le panier ($count)';
  }

  @override
  String get yourCart => 'Votre panier';

  @override
  String get clear => 'Vider';

  @override
  String get cartEmpty => 'Votre panier est vide';

  @override
  String get cartEmptyMessage =>
      'Parcourez le menu et ajoutez votre café préféré.';

  @override
  String get browseMenu => 'Voir le menu';

  @override
  String get salesPoint => 'Point de vente';

  @override
  String get total => 'Total';

  @override
  String get proceedToCheckout => 'Passer la commande';

  @override
  String get favourites => 'Favoris';

  @override
  String get noFavourites => 'Aucun favori pour l\'instant';

  @override
  String get noFavouritesMessage =>
      'Touchez le cœur d\'un produit pour le garder ici.';

  @override
  String get salesPoints => 'Points de vente';

  @override
  String get searchSalesPoints => 'Rechercher un point de vente…';

  @override
  String get noSalesPoints => 'Aucun point de vente trouvé';

  @override
  String get pointAtQr =>
      'Pointez la caméra vers le code QR du point de vente.';

  @override
  String get enterCodeManually => 'Saisir le code';

  @override
  String get salesPointCodeHint => 'Code (LUTIANO:OP:…)';

  @override
  String get cancel => 'Annuler';

  @override
  String get continueLabel => 'Continuer';

  @override
  String get cameraUnavailable =>
      'La caméra n\'est pas disponible. Vous pouvez saisir le code.';

  @override
  String get codeNotRecognised => 'Ce code n\'est pas reconnu.';

  @override
  String get openNow => 'Ouvert';

  @override
  String get closedNow => 'Fermé';

  @override
  String get openingHours => 'Horaires d\'ouverture';

  @override
  String get paymentMethods => 'Paiements acceptés';

  @override
  String get cashAtSalesPoint => 'Espèces au point de vente';

  @override
  String get selectThisSalesPoint => 'Choisir ce point de vente';

  @override
  String get orderHere => 'Commander ici';

  @override
  String get typeFixed => 'Boutique';

  @override
  String get typeMobile => 'Barista mobile';

  @override
  String get typeMarket => 'Stand de marché';

  @override
  String get monday => 'Lundi';

  @override
  String get tuesday => 'Mardi';

  @override
  String get wednesday => 'Mercredi';

  @override
  String get thursday => 'Jeudi';

  @override
  String get friday => 'Vendredi';

  @override
  String get saturday => 'Samedi';

  @override
  String get sunday => 'Dimanche';

  @override
  String get accountDetails => 'Détails du compte';

  @override
  String get securityAndLogin => 'Sécurité et connexion';

  @override
  String get language => 'Langue';

  @override
  String get followPhone => 'Suivre le téléphone';

  @override
  String get aboutLutiano => 'À propos de LUTIANO';

  @override
  String get termsOfUse => 'Conditions d\'utilisation';

  @override
  String get privacyPolicy => 'Politique de confidentialité';

  @override
  String get licenses => 'Licences';

  @override
  String versionValue(String version) {
    return 'Version $version';
  }

  @override
  String get logOut => 'Se déconnecter';

  @override
  String get logOutQuestion => 'Se déconnecter ?';

  @override
  String get logOutConfirm =>
      'Voulez-vous vraiment vous déconnecter de votre compte ?';

  @override
  String get profileSaved => 'Profil enregistré.';

  @override
  String get saveChanges => 'Enregistrer';

  @override
  String get save => 'Enregistrer';

  @override
  String get changePhoneNumber => 'Changer de numéro';

  @override
  String get newPhoneNumber => 'Nouveau numéro';

  @override
  String get receiveOffers => 'Recevoir les offres';

  @override
  String get receiveOffersHint =>
      'Nouveautés, offres et campagnes par notification.';

  @override
  String get changePassword => 'Changer le mot de passe';

  @override
  String get setPassword => 'Définir un mot de passe';

  @override
  String get currentPassword => 'Mot de passe actuel';

  @override
  String get newPasswordMin => 'Nouveau mot de passe (10 caractères min.)';

  @override
  String get twoStepVerification => 'Vérification en deux étapes';

  @override
  String get twoStepHint => 'Demander un code SMS après votre mot de passe.';

  @override
  String get twoStepNeedsPassword => 'Définissez d\'abord un mot de passe.';

  @override
  String get settingSaved => 'Réglage enregistré.';

  @override
  String get signOutOtherDevices => 'Déconnecter les autres appareils';

  @override
  String get signedOutEverywhere => 'Les autres appareils ont été déconnectés.';

  @override
  String get deleteAccount => 'Supprimer le compte';

  @override
  String get deleteAccountQuestion => 'Supprimer votre compte ?';

  @override
  String get deleteAccountWarning =>
      'Vos informations personnelles sont supprimées et vous êtes déconnecté. Cette action est irréversible.';

  @override
  String get legalDraftNotice =>
      'Texte provisoire, à faire relire par le conseil juridique de LUTIANO avant la mise en service de l\'application.';

  @override
  String get termsBody =>
      '## À qui s\'adressent ces conditions\nCes conditions s\'appliquent aux clients de LUTIANO Coffee qui utilisent cette application.\n\n## Votre compte\nVotre compte est personnel. Gardez votre mot de passe et vos codes secrets. Vous êtes responsable de ce qui est fait avec votre compte.\n\n## Commandes\nUne commande est une demande adressée à un point de vente LUTIANO. Elle est acceptée par le vendeur et payée au point de vente. Les prix et la disponibilité sont ceux affichés au point de vente au moment de la commande.\n\n## Récompenses\nLes points et récompenses suivent les règles affichées dans l\'application. Ils n\'ont pas de valeur en espèces et peuvent expirer.\n\n## Usage acceptable\nUtilisez l\'application pour vos propres achats uniquement. N\'essayez pas d\'accéder à des données qui ne sont pas les vôtres et ne perturbez pas le service.\n\n## Fermeture du compte\nVous pouvez supprimer votre compte depuis l\'application à tout moment. LUTIANO peut suspendre un compte qui ne respecte pas ces conditions.\n\n## Modifications et contact\nCes conditions peuvent évoluer ; l\'application affichera la nouvelle version.';

  @override
  String get privacyBody =>
      '## Ce que nous conservons sur vous\nVotre nom, numéro de téléphone, adresse e-mail si vous en donnez une, langue, les commandes que vous passez et les points que vous gagnez. Si vous vous connectez avec Google ou Apple, nous conservons l\'identifiant qu\'ils nous fournissent.\n\n## Pourquoi nous l\'utilisons\nPour préparer vos commandes, vous donner vos récompenses, sécuriser votre compte et répondre à vos demandes. Nous ne vendons pas vos données.\n\n## Notifications\nNous conservons l\'identifiant de notification de votre téléphone pour vous informer de vos commandes et récompenses. Les offres ne sont envoyées que si vous y avez consenti.\n\n## Qui peut les voir\nLe personnel de LUTIANO, dans la limite de ce que son rôle exige, et les services que nous utilisons pour envoyer SMS et notifications.\n\n## Durée de conservation\nLes enregistrements de commandes sont conservés aussi longtemps que la comptabilité l\'exige. Lorsque vous supprimez votre compte, vos informations personnelles sont supprimées.\n\n## Vos choix\nVous pouvez modifier vos informations, votre langue et vos consentements dans l\'application, et supprimer votre compte.\n\n## Sécurité\nVotre session est conservée dans le stockage sécurisé de votre téléphone et se termine à votre déconnexion.';

  @override
  String get checkout => 'Commande';

  @override
  String get orderReview => 'Récapitulatif';

  @override
  String get totalToPay => 'Total à payer';

  @override
  String get paymentMethod => 'Mode de paiement';

  @override
  String get payAtTheCounter => 'Payez au retrait de votre commande';

  @override
  String get comingSoon => 'Bientôt disponible';

  @override
  String get noteForTheSeller => 'Note pour le vendeur';

  @override
  String get noteHint => 'ex. moins de sucre, très chaud';

  @override
  String get confirmedBySeller =>
      'Votre commande est confirmée dès que le vendeur l\'accepte. Vous payez au point de vente.';

  @override
  String get placeOrder => 'Passer la commande';

  @override
  String get placingOrder => 'Envoi…';

  @override
  String get orderSent => 'Commande envoyée !';

  @override
  String get orderSentMessage =>
      'Votre commande a été envoyée au point de vente. Vous serez notifié à chaque étape.';

  @override
  String get orderNumber => 'Numéro de commande';

  @override
  String get viewOrderDetails => 'Voir la commande';

  @override
  String get myOrders => 'Mes commandes';

  @override
  String get filterPending => 'En cours';

  @override
  String get filterCompleted => 'Terminées';

  @override
  String get filterCancelled => 'Annulées';

  @override
  String get noOrdersYet => 'Aucune commande';

  @override
  String get noOrdersMessage =>
      'Vous n\'avez pas encore passé de commande. Parcourez le menu et commandez votre café préféré.';

  @override
  String get orderDetails => 'Détails de la commande';

  @override
  String get items => 'Articles';

  @override
  String get orderTracking => 'Suivi de la commande';

  @override
  String get viewReceipt => 'Voir le reçu';

  @override
  String get preparingReceipt => 'Préparation du reçu…';

  @override
  String get buyAgain => 'Commander à nouveau';

  @override
  String get cancelOrder => 'Annuler la commande';

  @override
  String get cancelOrderQuestion => 'Annuler cette commande ?';

  @override
  String get cancelOrderMessage =>
      'Le point de vente ne l\'a pas encore acceptée, vous pouvez donc l\'annuler maintenant.';

  @override
  String get keepOrder => 'Garder la commande';

  @override
  String get nothingToReorder =>
      'Aucun de ces articles ne peut être commandé pour le moment.';

  @override
  String get someItemsUnavailable =>
      'Certains articles ne sont plus disponibles et ont été retirés.';

  @override
  String get orderPlaced => 'Commande envoyée';

  @override
  String get orderBeingPrepared => 'En préparation';

  @override
  String get orderReadyStatus => 'Prête';

  @override
  String get orderCompleted => 'Terminée';

  @override
  String get orderDeclined => 'Refusée';

  @override
  String get orderCancelledStatus => 'Annulée';

  @override
  String get orderExpired => 'Expirée';

  @override
  String get stepPlaced => 'Votre commande a été reçue.';

  @override
  String get stepAccepted => 'Le point de vente l\'a acceptée et la prépare.';

  @override
  String get stepReady => 'Votre commande est prête à être récupérée.';

  @override
  String get stepCompleted => 'Remise et payée. Merci !';

  @override
  String get stepDeclined =>
      'Le point de vente n\'a pas pu accepter cette commande.';

  @override
  String get stepCancelled => 'Cette commande a été annulée.';

  @override
  String get stepExpired =>
      'Personne n\'a pris en charge cette commande à temps.';

  @override
  String get yourPointsBalance => 'Votre solde de points';

  @override
  String pointsValue(int points) {
    return '$points pts';
  }

  @override
  String get pts => 'pts';

  @override
  String get noPointsYet => 'Pas encore de points';

  @override
  String get noPointsYetMessage =>
      'Passez une commande ou montrez votre QR fidélité en point de vente pour gagner des points.';

  @override
  String pointsExpiringSoon(int points, int days) {
    return '$points pts expirent dans $days jours';
  }

  @override
  String membershipTier(String tier) {
    return 'Membre $tier';
  }

  @override
  String spendToReachTier(String amount, String tier) {
    return 'Dépensez encore $amount cette année pour atteindre $tier';
  }

  @override
  String get topTierReached =>
      'Vous avez atteint le niveau le plus haut. Merci !';

  @override
  String get showMyLoyaltyQr => 'Afficher mon QR fidélité';

  @override
  String get myLoyaltyQr => 'Mon QR fidélité';

  @override
  String get loyaltyQrHint =>
      'Montrez ce code au vendeur quand vous payez en boutique, pour que l\'achat vous rapporte des points.';

  @override
  String get rewardsCatalogue => 'Catalogue de récompenses';

  @override
  String get myRewards => 'Mes récompenses';

  @override
  String rewardsReady(int count) {
    return '$count prête(s) à l\'emploi';
  }

  @override
  String get pointsHistory => 'Historique des points';

  @override
  String get membershipTiers => 'Niveaux de membre';

  @override
  String get howPointsWork => 'Comment fonctionnent les points';

  @override
  String get noRewards => 'Aucune récompense';

  @override
  String get noRewardsMessage => 'Les nouvelles récompenses apparaîtront ici.';

  @override
  String get blockedMinPoints => 'Il vous faut plus de points pour échanger.';

  @override
  String get blockedNotEnough => 'Pas encore assez de points.';

  @override
  String blockedPurchases(int count) {
    return 'Nécessite au moins $count achats.';
  }

  @override
  String get blockedSpend => 'Nécessite plus de dépenses cumulées.';

  @override
  String redeemCost(int points) {
    return 'Coûte $points points';
  }

  @override
  String get redeemHint =>
      'La récompense va dans votre portefeuille. Montrez son QR code au vendeur pour la recevoir.';

  @override
  String get confirmRedemption => 'Échanger';

  @override
  String get rewardRedeemed => 'Récompense obtenue !';

  @override
  String get myReward => 'Ma récompense';

  @override
  String get rewardQr => 'QR code de la récompense';

  @override
  String get showThisToSeller => 'Montrez ce code au vendeur.';

  @override
  String validUntilDate(String date) {
    return 'Valable jusqu\'au $date';
  }

  @override
  String rewardUsedOn(String date) {
    return 'Utilisée le $date';
  }

  @override
  String get rewardExpiredMessage =>
      'Cette récompense a expiré ou n\'est plus valable.';

  @override
  String get stateAvailable => 'Disponible';

  @override
  String get stateUsed => 'Utilisée';

  @override
  String get stateExpired => 'Expirée';

  @override
  String get noRewardsHere => 'Rien ici';

  @override
  String get noRewardsHereMessage =>
      'Les récompenses que vous échangez apparaissent ici.';

  @override
  String get typeEarned => 'Gagnés';

  @override
  String get typeUsed => 'Utilisés';

  @override
  String get pointsEarned => 'Points gagnés';

  @override
  String get pointsUsed => 'Points utilisés';

  @override
  String get pointsExpired => 'Points expirés';

  @override
  String get pointsReturned => 'Points rendus';

  @override
  String get pointsAdjusted => 'Points ajustés';

  @override
  String get tiersHint =>
      'Votre niveau suit ce que vous avez dépensé sur les 12 derniers mois.';

  @override
  String get currentTier => 'Actuel';

  @override
  String get startingTier => 'Niveau de départ';

  @override
  String tierNeeds(String amount) {
    return 'Dépensez $amount par an';
  }

  @override
  String tierEarns(int percent) {
    return 'Gagnez $percent % des points de base';
  }

  @override
  String get earnPoints => 'Gagner des points';

  @override
  String earnRule(int points, String amount) {
    return 'Gagnez $points points pour chaque tranche de $amount dépensée.';
  }

  @override
  String get usePoints => 'Utiliser les points';

  @override
  String useRule(int points) {
    return 'Échangez des récompenses à partir de $points points.';
  }

  @override
  String get pointsValidity => 'Validité';

  @override
  String validityRule(int months) {
    return 'Les points sont valables $months mois à partir du jour où ils sont gagnés.';
  }

  @override
  String get currentPromotions => 'Promotions en cours';

  @override
  String get pointsEstimate => 'Estimation des points';

  @override
  String get purchaseAmount => 'Montant de l\'achat';

  @override
  String get calculate => 'Calculer';

  @override
  String get estimatedPoints => 'Points estimés';

  @override
  String get estimateNote =>
      'Les points réellement gagnés suivent les règles et promotions au moment de l\'achat.';

  @override
  String get notifications => 'Notifications';

  @override
  String get markAllRead => 'Tout marquer comme lu';

  @override
  String get notificationSettings => 'Paramètres de notification';

  @override
  String get noNotifications => 'Aucune notification';

  @override
  String get noNotificationsMessage =>
      'Les nouvelles de vos commandes, récompenses et offres apparaissent ici.';

  @override
  String get close => 'Fermer';

  @override
  String get catOrders => 'Commandes';

  @override
  String get catLoyalty => 'Fidélité et récompenses';

  @override
  String get catPromotions => 'Promotions et offres';

  @override
  String get catSystem => 'Système et actualités';

  @override
  String get alwaysOn => 'Toujours activé';

  @override
  String get pushOnPhone => 'Notifications push sur ce téléphone';

  @override
  String get pushNotOnThisPhone =>
      'Les notifications push ne sont pas configurées sur ce téléphone : les messages arrivent seulement dans l\'application.';

  @override
  String get hintLoyalty => 'Points, niveau, récompenses';

  @override
  String get hintPromotions => 'Nouveaux produits, réductions, campagnes';

  @override
  String get hintSystem => 'Mises à jour et annonces';

  @override
  String get ntOrderAccepted => 'Commande acceptée';

  @override
  String get ntOrderDeclined => 'Commande refusée';

  @override
  String get ntOrderReady => 'Commande prête';

  @override
  String get ntOrderComplete => 'Commande terminée';

  @override
  String get ntOrderCancelled => 'Commande annulée';

  @override
  String get ntOrderExpired => 'Commande expirée';

  @override
  String get ntRewardRedeemed => 'Récompense obtenue';

  @override
  String get ntRewardUsed => 'Récompense utilisée';

  @override
  String get ntRewardExpired => 'Récompense expirée';

  @override
  String get ntSupportReply => 'Réponse du support';

  @override
  String get ntLutiano => 'LUTIANO Coffee';

  @override
  String get helpAndSupport => 'Aide et assistance';

  @override
  String get contactSupport => 'Contacter l\'assistance';

  @override
  String get myRequests => 'Mes demandes';

  @override
  String get faq => 'Questions fréquentes';

  @override
  String get supportRequest => 'Demande d\'assistance';

  @override
  String get teamReply => 'Réponse de l\'équipe';

  @override
  String get noReplyYet =>
      'L\'équipe n\'a pas encore répondu. Vous serez notifié.';

  @override
  String get noRequests => 'Aucune demande';

  @override
  String get noRequestsMessage => 'Vos demandes à l\'équipe apparaissent ici.';

  @override
  String get requestSent => 'Demande envoyée.';

  @override
  String get requestCategory => 'De quoi s\'agit-il ?';

  @override
  String get requestTitle => 'Titre';

  @override
  String get requestDescription => 'Décrivez le problème ou la question';

  @override
  String get enterATitle => 'Entrez un titre court.';

  @override
  String get describeTheProblem => 'Dites-nous-en un peu plus.';

  @override
  String get addPhotoOptional => 'Ajouter une photo (facultatif)';

  @override
  String get submitRequest => 'Envoyer la demande';

  @override
  String get reqOrder => 'Commande et paiement';

  @override
  String get reqRewards => 'Points et récompenses';

  @override
  String get reqAccount => 'Mon compte';

  @override
  String get reqIssue => 'Signaler un problème';

  @override
  String get reqQuestion => 'Une question';

  @override
  String get reqIdea => 'Suggérer une amélioration';

  @override
  String get reqOpen => 'Ouverte';

  @override
  String get reqInProgress => 'En cours';

  @override
  String get reqResolved => 'Résolue';

  @override
  String get faqQ1 => 'Comment commander ?';

  @override
  String get faqA1 =>
      'Choisissez un point de vente (scannez son code QR ou choisissez-le dans la liste), ajoutez des produits au panier et passez votre commande. Un vendeur l\'accepte et vous payez en espèces au retrait.';

  @override
  String get faqQ2 => 'Comment gagner des points ?';

  @override
  String get faqA2 =>
      'Montrez votre code QR fidélité au vendeur quand vous payez, ou commandez dans l\'application. Les points sont ajoutés à la remise de la commande.';

  @override
  String get faqQ3 => 'Comment utiliser une récompense ?';

  @override
  String get faqA3 =>
      'Échangez-la dans l\'onglet Récompenses, puis montrez son code QR au vendeur au point de vente.';

  @override
  String get faqQ4 => 'Puis-je annuler une commande ?';

  @override
  String get faqA4 =>
      'Oui, tant que le point de vente ne l\'a pas acceptée. Ensuite, contactez le point de vente ou écrivez à l\'assistance.';

  @override
  String get faqQ5 => 'J\'ai oublié mon mot de passe.';

  @override
  String get faqA5 =>
      'Sur l\'écran de connexion, choisissez Mot de passe oublié, ou connectez-vous avec un code envoyé sur votre téléphone.';
}

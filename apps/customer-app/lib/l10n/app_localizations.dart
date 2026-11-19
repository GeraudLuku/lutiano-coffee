import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';
import 'app_localizations_fr.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
      : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations)!;
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
    delegate,
    GlobalMaterialLocalizations.delegate,
    GlobalCupertinoLocalizations.delegate,
    GlobalWidgetsLocalizations.delegate,
  ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('fr')
  ];

  /// No description provided for @appTitle.
  ///
  /// In en, this message translates to:
  /// **'LUTIANO Coffee'**
  String get appTitle;

  /// No description provided for @coffee.
  ///
  /// In en, this message translates to:
  /// **'Coffee'**
  String get coffee;

  /// No description provided for @tagline.
  ///
  /// In en, this message translates to:
  /// **'Premium coffee, closer to you.'**
  String get tagline;

  /// No description provided for @welcomeHeadline.
  ///
  /// In en, this message translates to:
  /// **'Great Coffee, Closer to You'**
  String get welcomeHeadline;

  /// No description provided for @welcomeSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Premium coffee from our points of sale, mobile baristas and your favourite markets.'**
  String get welcomeSubtitle;

  /// No description provided for @getStarted.
  ///
  /// In en, this message translates to:
  /// **'Get Started'**
  String get getStarted;

  /// No description provided for @alreadyHaveAccount.
  ///
  /// In en, this message translates to:
  /// **'Already have an account?'**
  String get alreadyHaveAccount;

  /// No description provided for @logIn.
  ///
  /// In en, this message translates to:
  /// **'Log In'**
  String get logIn;

  /// No description provided for @signUp.
  ///
  /// In en, this message translates to:
  /// **'Sign Up'**
  String get signUp;

  /// No description provided for @noAccount.
  ///
  /// In en, this message translates to:
  /// **'Don\'t have an account?'**
  String get noAccount;

  /// No description provided for @logInToYourAccount.
  ///
  /// In en, this message translates to:
  /// **'Log In to Your Account'**
  String get logInToYourAccount;

  /// No description provided for @accessYourOrders.
  ///
  /// In en, this message translates to:
  /// **'Access your orders, rewards and more.'**
  String get accessYourOrders;

  /// No description provided for @sessionEnded.
  ///
  /// In en, this message translates to:
  /// **'Your session ended. Please log in again.'**
  String get sessionEnded;

  /// No description provided for @phoneNumber.
  ///
  /// In en, this message translates to:
  /// **'Phone number'**
  String get phoneNumber;

  /// No description provided for @enterValidPhone.
  ///
  /// In en, this message translates to:
  /// **'Enter a Cameroonian mobile number (6XX XX XX XX).'**
  String get enterValidPhone;

  /// No description provided for @password.
  ///
  /// In en, this message translates to:
  /// **'Password'**
  String get password;

  /// No description provided for @enterYourPassword.
  ///
  /// In en, this message translates to:
  /// **'Enter your password.'**
  String get enterYourPassword;

  /// No description provided for @passwordOptional.
  ///
  /// In en, this message translates to:
  /// **'Password (optional, 10+ characters)'**
  String get passwordOptional;

  /// No description provided for @passwordTooShort.
  ///
  /// In en, this message translates to:
  /// **'Use at least 10 characters.'**
  String get passwordTooShort;

  /// No description provided for @passwordsDoNotMatch.
  ///
  /// In en, this message translates to:
  /// **'The passwords do not match.'**
  String get passwordsDoNotMatch;

  /// No description provided for @confirmPassword.
  ///
  /// In en, this message translates to:
  /// **'Confirm password'**
  String get confirmPassword;

  /// No description provided for @showPassword.
  ///
  /// In en, this message translates to:
  /// **'Show password'**
  String get showPassword;

  /// No description provided for @hidePassword.
  ///
  /// In en, this message translates to:
  /// **'Hide password'**
  String get hidePassword;

  /// No description provided for @signingIn.
  ///
  /// In en, this message translates to:
  /// **'Logging in…'**
  String get signingIn;

  /// No description provided for @sendMeACode.
  ///
  /// In en, this message translates to:
  /// **'Send me a code'**
  String get sendMeACode;

  /// No description provided for @forgotPassword.
  ///
  /// In en, this message translates to:
  /// **'Forgot password?'**
  String get forgotPassword;

  /// No description provided for @or.
  ///
  /// In en, this message translates to:
  /// **'or'**
  String get or;

  /// No description provided for @continueWithGoogle.
  ///
  /// In en, this message translates to:
  /// **'Continue with Google'**
  String get continueWithGoogle;

  /// No description provided for @continueWithApple.
  ///
  /// In en, this message translates to:
  /// **'Continue with Apple'**
  String get continueWithApple;

  /// No description provided for @createYourAccount.
  ///
  /// In en, this message translates to:
  /// **'Create Your Account'**
  String get createYourAccount;

  /// No description provided for @joinLutiano.
  ///
  /// In en, this message translates to:
  /// **'Join LUTIANO Coffee and enjoy great coffee and rewards.'**
  String get joinLutiano;

  /// No description provided for @fullName.
  ///
  /// In en, this message translates to:
  /// **'Full name'**
  String get fullName;

  /// No description provided for @enterYourName.
  ///
  /// In en, this message translates to:
  /// **'Enter your name.'**
  String get enterYourName;

  /// No description provided for @emailOptional.
  ///
  /// In en, this message translates to:
  /// **'Email (optional)'**
  String get emailOptional;

  /// No description provided for @enterValidEmail.
  ///
  /// In en, this message translates to:
  /// **'Enter a valid email address.'**
  String get enterValidEmail;

  /// No description provided for @iAgreeTo.
  ///
  /// In en, this message translates to:
  /// **'I agree to the'**
  String get iAgreeTo;

  /// No description provided for @and.
  ///
  /// In en, this message translates to:
  /// **'and'**
  String get and;

  /// No description provided for @iAgreeToTermsAndPrivacy.
  ///
  /// In en, this message translates to:
  /// **'I agree to the Terms of Use and the Privacy Policy'**
  String get iAgreeToTermsAndPrivacy;

  /// No description provided for @acceptTermsToContinue.
  ///
  /// In en, this message translates to:
  /// **'Accept the terms to continue.'**
  String get acceptTermsToContinue;

  /// No description provided for @createAccount.
  ///
  /// In en, this message translates to:
  /// **'Create Account'**
  String get createAccount;

  /// No description provided for @pleaseWait.
  ///
  /// In en, this message translates to:
  /// **'Please wait…'**
  String get pleaseWait;

  /// No description provided for @verifyAccount.
  ///
  /// In en, this message translates to:
  /// **'Verify Account'**
  String get verifyAccount;

  /// No description provided for @verifyYourPhone.
  ///
  /// In en, this message translates to:
  /// **'Verify Your Phone'**
  String get verifyYourPhone;

  /// No description provided for @enterCodeSentTo.
  ///
  /// In en, this message translates to:
  /// **'Enter the 6-digit code we sent to {phone}.'**
  String enterCodeSentTo(String phone);

  /// No description provided for @verify.
  ///
  /// In en, this message translates to:
  /// **'Verify'**
  String get verify;

  /// No description provided for @verifying.
  ///
  /// In en, this message translates to:
  /// **'Verifying…'**
  String get verifying;

  /// No description provided for @resendCode.
  ///
  /// In en, this message translates to:
  /// **'Resend code'**
  String get resendCode;

  /// No description provided for @resendIn.
  ///
  /// In en, this message translates to:
  /// **'Resend code in {seconds}s'**
  String resendIn(int seconds);

  /// No description provided for @codeSent.
  ///
  /// In en, this message translates to:
  /// **'A new code was sent.'**
  String get codeSent;

  /// No description provided for @resetPassword.
  ///
  /// In en, this message translates to:
  /// **'Reset Password'**
  String get resetPassword;

  /// No description provided for @weWillSendCode.
  ///
  /// In en, this message translates to:
  /// **'We will send a code to your phone.'**
  String get weWillSendCode;

  /// No description provided for @enterCodeAndNewPassword.
  ///
  /// In en, this message translates to:
  /// **'Enter the code we sent and choose a new password.'**
  String get enterCodeAndNewPassword;

  /// No description provided for @sendCode.
  ///
  /// In en, this message translates to:
  /// **'Send code'**
  String get sendCode;

  /// No description provided for @sixDigitCode.
  ///
  /// In en, this message translates to:
  /// **'6-digit code'**
  String get sixDigitCode;

  /// No description provided for @enterTheCode.
  ///
  /// In en, this message translates to:
  /// **'Enter the 6-digit code.'**
  String get enterTheCode;

  /// No description provided for @newPassword.
  ///
  /// In en, this message translates to:
  /// **'New password'**
  String get newPassword;

  /// No description provided for @passwordChanged.
  ///
  /// In en, this message translates to:
  /// **'Your password was changed.'**
  String get passwordChanged;

  /// No description provided for @oneLastStep.
  ///
  /// In en, this message translates to:
  /// **'One Last Step'**
  String get oneLastStep;

  /// No description provided for @addYourPhoneWhy.
  ///
  /// In en, this message translates to:
  /// **'Orders and rewards go with your phone number. We will confirm it by SMS.'**
  String get addYourPhoneWhy;

  /// No description provided for @home.
  ///
  /// In en, this message translates to:
  /// **'Home'**
  String get home;

  /// No description provided for @menu.
  ///
  /// In en, this message translates to:
  /// **'Menu'**
  String get menu;

  /// No description provided for @orders.
  ///
  /// In en, this message translates to:
  /// **'Orders'**
  String get orders;

  /// No description provided for @rewards.
  ///
  /// In en, this message translates to:
  /// **'Rewards'**
  String get rewards;

  /// No description provided for @profile.
  ///
  /// In en, this message translates to:
  /// **'Profile'**
  String get profile;

  /// No description provided for @hello.
  ///
  /// In en, this message translates to:
  /// **'Hello, {name}'**
  String hello(String name);

  /// No description provided for @coffeeLover.
  ///
  /// In en, this message translates to:
  /// **'Coffee lover'**
  String get coffeeLover;

  /// No description provided for @scanToOrder.
  ///
  /// In en, this message translates to:
  /// **'Scan to Order'**
  String get scanToOrder;

  /// No description provided for @scanToOrderHint.
  ///
  /// In en, this message translates to:
  /// **'Scan the QR code at a LUTIANO sales point.'**
  String get scanToOrderHint;

  /// No description provided for @findASalesPoint.
  ///
  /// In en, this message translates to:
  /// **'Find a sales point'**
  String get findASalesPoint;

  /// No description provided for @recentSalesPoints.
  ///
  /// In en, this message translates to:
  /// **'Recent sales points'**
  String get recentSalesPoints;

  /// No description provided for @categories.
  ///
  /// In en, this message translates to:
  /// **'Categories'**
  String get categories;

  /// No description provided for @popularDrinks.
  ///
  /// In en, this message translates to:
  /// **'Popular Drinks'**
  String get popularDrinks;

  /// No description provided for @seeAll.
  ///
  /// In en, this message translates to:
  /// **'See all'**
  String get seeAll;

  /// No description provided for @cart.
  ///
  /// In en, this message translates to:
  /// **'Cart'**
  String get cart;

  /// No description provided for @searchMenu.
  ///
  /// In en, this message translates to:
  /// **'Search the menu…'**
  String get searchMenu;

  /// No description provided for @noProductsFound.
  ///
  /// In en, this message translates to:
  /// **'Nothing found'**
  String get noProductsFound;

  /// No description provided for @tryAnotherSearch.
  ///
  /// In en, this message translates to:
  /// **'Try another search or category.'**
  String get tryAnotherSearch;

  /// No description provided for @chooseSalesPointHint.
  ///
  /// In en, this message translates to:
  /// **'Choose a sales point to see what is available and order.'**
  String get chooseSalesPointHint;

  /// No description provided for @orderingFrom.
  ///
  /// In en, this message translates to:
  /// **'Ordering from {name}'**
  String orderingFrom(String name);

  /// No description provided for @choose.
  ///
  /// In en, this message translates to:
  /// **'Choose'**
  String get choose;

  /// No description provided for @change.
  ///
  /// In en, this message translates to:
  /// **'Change'**
  String get change;

  /// No description provided for @fromPrice.
  ///
  /// In en, this message translates to:
  /// **'from {price}'**
  String fromPrice(String price);

  /// No description provided for @unavailableHere.
  ///
  /// In en, this message translates to:
  /// **'Not available at this sales point right now.'**
  String get unavailableHere;

  /// No description provided for @addToFavourites.
  ///
  /// In en, this message translates to:
  /// **'Add to favourites'**
  String get addToFavourites;

  /// No description provided for @removeFromFavourites.
  ///
  /// In en, this message translates to:
  /// **'Remove from favourites'**
  String get removeFromFavourites;

  /// No description provided for @size.
  ///
  /// In en, this message translates to:
  /// **'Size'**
  String get size;

  /// No description provided for @addOnsOptional.
  ///
  /// In en, this message translates to:
  /// **'Add-ons (optional)'**
  String get addOnsOptional;

  /// No description provided for @addToCartPrice.
  ///
  /// In en, this message translates to:
  /// **'Add to Cart ({price})'**
  String addToCartPrice(String price);

  /// No description provided for @addedToCart.
  ///
  /// In en, this message translates to:
  /// **'Added to cart!'**
  String get addedToCart;

  /// No description provided for @addedToCartMessage.
  ///
  /// In en, this message translates to:
  /// **'{name} was added to your cart.'**
  String addedToCartMessage(String name);

  /// No description provided for @continueShopping.
  ///
  /// In en, this message translates to:
  /// **'Continue shopping'**
  String get continueShopping;

  /// No description provided for @viewCart.
  ///
  /// In en, this message translates to:
  /// **'View cart ({count})'**
  String viewCart(int count);

  /// No description provided for @yourCart.
  ///
  /// In en, this message translates to:
  /// **'Your Cart'**
  String get yourCart;

  /// No description provided for @clear.
  ///
  /// In en, this message translates to:
  /// **'Clear'**
  String get clear;

  /// No description provided for @cartEmpty.
  ///
  /// In en, this message translates to:
  /// **'Your cart is empty'**
  String get cartEmpty;

  /// No description provided for @cartEmptyMessage.
  ///
  /// In en, this message translates to:
  /// **'Browse the menu and add your favourite coffee.'**
  String get cartEmptyMessage;

  /// No description provided for @browseMenu.
  ///
  /// In en, this message translates to:
  /// **'Browse Menu'**
  String get browseMenu;

  /// No description provided for @salesPoint.
  ///
  /// In en, this message translates to:
  /// **'Sales point'**
  String get salesPoint;

  /// No description provided for @total.
  ///
  /// In en, this message translates to:
  /// **'Total'**
  String get total;

  /// No description provided for @proceedToCheckout.
  ///
  /// In en, this message translates to:
  /// **'Proceed to Checkout'**
  String get proceedToCheckout;

  /// No description provided for @favourites.
  ///
  /// In en, this message translates to:
  /// **'Favourites'**
  String get favourites;

  /// No description provided for @noFavourites.
  ///
  /// In en, this message translates to:
  /// **'No favourites yet'**
  String get noFavourites;

  /// No description provided for @noFavouritesMessage.
  ///
  /// In en, this message translates to:
  /// **'Tap the heart on a product to keep it here.'**
  String get noFavouritesMessage;

  /// No description provided for @salesPoints.
  ///
  /// In en, this message translates to:
  /// **'Sales Points'**
  String get salesPoints;

  /// No description provided for @searchSalesPoints.
  ///
  /// In en, this message translates to:
  /// **'Search sales points…'**
  String get searchSalesPoints;

  /// No description provided for @noSalesPoints.
  ///
  /// In en, this message translates to:
  /// **'No sales points found'**
  String get noSalesPoints;

  /// No description provided for @pointAtQr.
  ///
  /// In en, this message translates to:
  /// **'Point the camera at the sales point\'s QR code.'**
  String get pointAtQr;

  /// No description provided for @enterCodeManually.
  ///
  /// In en, this message translates to:
  /// **'Enter code manually'**
  String get enterCodeManually;

  /// No description provided for @salesPointCodeHint.
  ///
  /// In en, this message translates to:
  /// **'Code (LUTIANO:OP:…)'**
  String get salesPointCodeHint;

  /// No description provided for @cancel.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get cancel;

  /// No description provided for @continueLabel.
  ///
  /// In en, this message translates to:
  /// **'Continue'**
  String get continueLabel;

  /// No description provided for @cameraUnavailable.
  ///
  /// In en, this message translates to:
  /// **'The camera is not available. You can enter the code instead.'**
  String get cameraUnavailable;

  /// No description provided for @codeNotRecognised.
  ///
  /// In en, this message translates to:
  /// **'This code was not recognised.'**
  String get codeNotRecognised;

  /// No description provided for @openNow.
  ///
  /// In en, this message translates to:
  /// **'Open now'**
  String get openNow;

  /// No description provided for @closedNow.
  ///
  /// In en, this message translates to:
  /// **'Closed now'**
  String get closedNow;

  /// No description provided for @openingHours.
  ///
  /// In en, this message translates to:
  /// **'Opening hours'**
  String get openingHours;

  /// No description provided for @paymentMethods.
  ///
  /// In en, this message translates to:
  /// **'Accepted payments'**
  String get paymentMethods;

  /// No description provided for @cashAtSalesPoint.
  ///
  /// In en, this message translates to:
  /// **'Cash at the sales point'**
  String get cashAtSalesPoint;

  /// No description provided for @selectThisSalesPoint.
  ///
  /// In en, this message translates to:
  /// **'Select this sales point'**
  String get selectThisSalesPoint;

  /// No description provided for @orderHere.
  ///
  /// In en, this message translates to:
  /// **'Order Here'**
  String get orderHere;

  /// No description provided for @typeFixed.
  ///
  /// In en, this message translates to:
  /// **'Shop'**
  String get typeFixed;

  /// No description provided for @typeMobile.
  ///
  /// In en, this message translates to:
  /// **'Mobile barista'**
  String get typeMobile;

  /// No description provided for @typeMarket.
  ///
  /// In en, this message translates to:
  /// **'Market stand'**
  String get typeMarket;

  /// No description provided for @monday.
  ///
  /// In en, this message translates to:
  /// **'Monday'**
  String get monday;

  /// No description provided for @tuesday.
  ///
  /// In en, this message translates to:
  /// **'Tuesday'**
  String get tuesday;

  /// No description provided for @wednesday.
  ///
  /// In en, this message translates to:
  /// **'Wednesday'**
  String get wednesday;

  /// No description provided for @thursday.
  ///
  /// In en, this message translates to:
  /// **'Thursday'**
  String get thursday;

  /// No description provided for @friday.
  ///
  /// In en, this message translates to:
  /// **'Friday'**
  String get friday;

  /// No description provided for @saturday.
  ///
  /// In en, this message translates to:
  /// **'Saturday'**
  String get saturday;

  /// No description provided for @sunday.
  ///
  /// In en, this message translates to:
  /// **'Sunday'**
  String get sunday;

  /// No description provided for @accountDetails.
  ///
  /// In en, this message translates to:
  /// **'Account Details'**
  String get accountDetails;

  /// No description provided for @securityAndLogin.
  ///
  /// In en, this message translates to:
  /// **'Security & Login'**
  String get securityAndLogin;

  /// No description provided for @language.
  ///
  /// In en, this message translates to:
  /// **'Language'**
  String get language;

  /// No description provided for @followPhone.
  ///
  /// In en, this message translates to:
  /// **'Follow the phone'**
  String get followPhone;

  /// No description provided for @aboutLutiano.
  ///
  /// In en, this message translates to:
  /// **'About LUTIANO'**
  String get aboutLutiano;

  /// No description provided for @termsOfUse.
  ///
  /// In en, this message translates to:
  /// **'Terms of Use'**
  String get termsOfUse;

  /// No description provided for @privacyPolicy.
  ///
  /// In en, this message translates to:
  /// **'Privacy Policy'**
  String get privacyPolicy;

  /// No description provided for @licenses.
  ///
  /// In en, this message translates to:
  /// **'Licenses'**
  String get licenses;

  /// No description provided for @versionValue.
  ///
  /// In en, this message translates to:
  /// **'Version {version}'**
  String versionValue(String version);

  /// No description provided for @logOut.
  ///
  /// In en, this message translates to:
  /// **'Log Out'**
  String get logOut;

  /// No description provided for @logOutQuestion.
  ///
  /// In en, this message translates to:
  /// **'Log out?'**
  String get logOutQuestion;

  /// No description provided for @logOutConfirm.
  ///
  /// In en, this message translates to:
  /// **'Are you sure you want to log out of your account?'**
  String get logOutConfirm;

  /// No description provided for @profileSaved.
  ///
  /// In en, this message translates to:
  /// **'Profile saved.'**
  String get profileSaved;

  /// No description provided for @saveChanges.
  ///
  /// In en, this message translates to:
  /// **'Save Changes'**
  String get saveChanges;

  /// No description provided for @save.
  ///
  /// In en, this message translates to:
  /// **'Save'**
  String get save;

  /// No description provided for @changePhoneNumber.
  ///
  /// In en, this message translates to:
  /// **'Change phone number'**
  String get changePhoneNumber;

  /// No description provided for @newPhoneNumber.
  ///
  /// In en, this message translates to:
  /// **'New phone number'**
  String get newPhoneNumber;

  /// No description provided for @receiveOffers.
  ///
  /// In en, this message translates to:
  /// **'Receive offers'**
  String get receiveOffers;

  /// No description provided for @receiveOffersHint.
  ///
  /// In en, this message translates to:
  /// **'News, offers and campaigns by notification.'**
  String get receiveOffersHint;

  /// No description provided for @changePassword.
  ///
  /// In en, this message translates to:
  /// **'Change password'**
  String get changePassword;

  /// No description provided for @setPassword.
  ///
  /// In en, this message translates to:
  /// **'Set a password'**
  String get setPassword;

  /// No description provided for @currentPassword.
  ///
  /// In en, this message translates to:
  /// **'Current password'**
  String get currentPassword;

  /// No description provided for @newPasswordMin.
  ///
  /// In en, this message translates to:
  /// **'New password (10+ characters)'**
  String get newPasswordMin;

  /// No description provided for @twoStepVerification.
  ///
  /// In en, this message translates to:
  /// **'Two-step verification'**
  String get twoStepVerification;

  /// No description provided for @twoStepHint.
  ///
  /// In en, this message translates to:
  /// **'Ask for an SMS code after your password.'**
  String get twoStepHint;

  /// No description provided for @twoStepNeedsPassword.
  ///
  /// In en, this message translates to:
  /// **'Set a password first.'**
  String get twoStepNeedsPassword;

  /// No description provided for @settingSaved.
  ///
  /// In en, this message translates to:
  /// **'Setting saved.'**
  String get settingSaved;

  /// No description provided for @signOutOtherDevices.
  ///
  /// In en, this message translates to:
  /// **'Sign out of other devices'**
  String get signOutOtherDevices;

  /// No description provided for @signedOutEverywhere.
  ///
  /// In en, this message translates to:
  /// **'Other devices were signed out.'**
  String get signedOutEverywhere;

  /// No description provided for @deleteAccount.
  ///
  /// In en, this message translates to:
  /// **'Delete Account'**
  String get deleteAccount;

  /// No description provided for @deleteAccountQuestion.
  ///
  /// In en, this message translates to:
  /// **'Delete your account?'**
  String get deleteAccountQuestion;

  /// No description provided for @deleteAccountWarning.
  ///
  /// In en, this message translates to:
  /// **'Your personal details are removed and you are signed out. This cannot be undone.'**
  String get deleteAccountWarning;

  /// No description provided for @legalDraftNotice.
  ///
  /// In en, this message translates to:
  /// **'Draft text, to be reviewed by LUTIANO\'s legal adviser before the app is released.'**
  String get legalDraftNotice;

  /// No description provided for @termsBody.
  ///
  /// In en, this message translates to:
  /// **'## Who these terms are for\nThese terms apply to customers of LUTIANO Coffee who use this app.\n\n## Your account\nYour account is personal. Keep your password and codes private. You are responsible for what is done with your account.\n\n## Orders\nAn order is a request to a LUTIANO sales point. It is accepted by the seller and paid at the sales point. Prices and availability are those shown at the sales point when you order.\n\n## Rewards\nPoints and rewards follow the rules shown in the app. They have no cash value and can expire.\n\n## Acceptable use\nUse the app for your own purchases only. Do not try to reach data that is not yours or disturb the service.\n\n## Closing your account\nYou can delete your account from the app at any time. LUTIANO may suspend an account that does not respect these terms.\n\n## Changes and contact\nThese terms may change; the app will show the new version.'**
  String get termsBody;

  /// No description provided for @privacyBody.
  ///
  /// In en, this message translates to:
  /// **'## What we keep about you\nYour name, phone number, email address if you give one, language, the orders you place and the points you earn. If you sign in with Google or Apple we keep the identifier they give us.\n\n## Why we use it\nTo prepare your orders, give you your rewards, keep your account safe and answer your requests. We do not sell your data.\n\n## Notifications\nWe keep your phone\'s notification token to tell you about your orders and rewards. Offers are sent only if you agreed.\n\n## Who can see it\nLUTIANO staff, within what their role needs, and the services we use to send SMS and notifications.\n\n## How long we keep it\nOrder records are kept as long as the accounts require. When you delete your account your personal details are removed.\n\n## Your choices\nYou can change your details, language and consent in the app, and delete your account.\n\n## Security\nYour session is kept in your phone\'s secure storage and ends when you log out.'**
  String get privacyBody;

  /// No description provided for @checkout.
  ///
  /// In en, this message translates to:
  /// **'Checkout'**
  String get checkout;

  /// No description provided for @orderReview.
  ///
  /// In en, this message translates to:
  /// **'Order review'**
  String get orderReview;

  /// No description provided for @totalToPay.
  ///
  /// In en, this message translates to:
  /// **'Total to pay'**
  String get totalToPay;

  /// No description provided for @paymentMethod.
  ///
  /// In en, this message translates to:
  /// **'Payment method'**
  String get paymentMethod;

  /// No description provided for @payAtTheCounter.
  ///
  /// In en, this message translates to:
  /// **'Pay when you collect your order'**
  String get payAtTheCounter;

  /// No description provided for @comingSoon.
  ///
  /// In en, this message translates to:
  /// **'Coming soon'**
  String get comingSoon;

  /// No description provided for @noteForTheSeller.
  ///
  /// In en, this message translates to:
  /// **'Note for the seller'**
  String get noteForTheSeller;

  /// No description provided for @noteHint.
  ///
  /// In en, this message translates to:
  /// **'e.g. less sugar, extra hot'**
  String get noteHint;

  /// No description provided for @confirmedBySeller.
  ///
  /// In en, this message translates to:
  /// **'Your order is confirmed once the seller accepts it. You pay at the sales point.'**
  String get confirmedBySeller;

  /// No description provided for @placeOrder.
  ///
  /// In en, this message translates to:
  /// **'Place Order'**
  String get placeOrder;

  /// No description provided for @placingOrder.
  ///
  /// In en, this message translates to:
  /// **'Sending…'**
  String get placingOrder;

  /// No description provided for @orderSent.
  ///
  /// In en, this message translates to:
  /// **'Order Placed!'**
  String get orderSent;

  /// No description provided for @orderSentMessage.
  ///
  /// In en, this message translates to:
  /// **'Your order was sent to the sales point. You will be notified when it changes.'**
  String get orderSentMessage;

  /// No description provided for @orderNumber.
  ///
  /// In en, this message translates to:
  /// **'Order number'**
  String get orderNumber;

  /// No description provided for @viewOrderDetails.
  ///
  /// In en, this message translates to:
  /// **'View Order Details'**
  String get viewOrderDetails;

  /// No description provided for @myOrders.
  ///
  /// In en, this message translates to:
  /// **'My Orders'**
  String get myOrders;

  /// No description provided for @filterPending.
  ///
  /// In en, this message translates to:
  /// **'Pending'**
  String get filterPending;

  /// No description provided for @filterCompleted.
  ///
  /// In en, this message translates to:
  /// **'Completed'**
  String get filterCompleted;

  /// No description provided for @filterCancelled.
  ///
  /// In en, this message translates to:
  /// **'Cancelled'**
  String get filterCancelled;

  /// No description provided for @noOrdersYet.
  ///
  /// In en, this message translates to:
  /// **'No orders yet'**
  String get noOrdersYet;

  /// No description provided for @noOrdersMessage.
  ///
  /// In en, this message translates to:
  /// **'You haven\'t placed any orders yet. Browse the menu and order your favourite coffee.'**
  String get noOrdersMessage;

  /// No description provided for @orderDetails.
  ///
  /// In en, this message translates to:
  /// **'Order Details'**
  String get orderDetails;

  /// No description provided for @items.
  ///
  /// In en, this message translates to:
  /// **'Items'**
  String get items;

  /// No description provided for @orderTracking.
  ///
  /// In en, this message translates to:
  /// **'Order Tracking'**
  String get orderTracking;

  /// No description provided for @viewReceipt.
  ///
  /// In en, this message translates to:
  /// **'View Receipt'**
  String get viewReceipt;

  /// No description provided for @preparingReceipt.
  ///
  /// In en, this message translates to:
  /// **'Preparing your receipt…'**
  String get preparingReceipt;

  /// No description provided for @buyAgain.
  ///
  /// In en, this message translates to:
  /// **'Buy Again'**
  String get buyAgain;

  /// No description provided for @cancelOrder.
  ///
  /// In en, this message translates to:
  /// **'Cancel Order'**
  String get cancelOrder;

  /// No description provided for @cancelOrderQuestion.
  ///
  /// In en, this message translates to:
  /// **'Cancel this order?'**
  String get cancelOrderQuestion;

  /// No description provided for @cancelOrderMessage.
  ///
  /// In en, this message translates to:
  /// **'The sales point has not accepted it yet, so you can cancel it now.'**
  String get cancelOrderMessage;

  /// No description provided for @keepOrder.
  ///
  /// In en, this message translates to:
  /// **'Keep Order'**
  String get keepOrder;

  /// No description provided for @nothingToReorder.
  ///
  /// In en, this message translates to:
  /// **'None of these items can be ordered right now.'**
  String get nothingToReorder;

  /// No description provided for @someItemsUnavailable.
  ///
  /// In en, this message translates to:
  /// **'Some items are no longer available and were left out.'**
  String get someItemsUnavailable;

  /// No description provided for @orderPlaced.
  ///
  /// In en, this message translates to:
  /// **'Order placed'**
  String get orderPlaced;

  /// No description provided for @orderBeingPrepared.
  ///
  /// In en, this message translates to:
  /// **'Being prepared'**
  String get orderBeingPrepared;

  /// No description provided for @orderReadyStatus.
  ///
  /// In en, this message translates to:
  /// **'Ready'**
  String get orderReadyStatus;

  /// No description provided for @orderCompleted.
  ///
  /// In en, this message translates to:
  /// **'Completed'**
  String get orderCompleted;

  /// No description provided for @orderDeclined.
  ///
  /// In en, this message translates to:
  /// **'Declined'**
  String get orderDeclined;

  /// No description provided for @orderCancelledStatus.
  ///
  /// In en, this message translates to:
  /// **'Cancelled'**
  String get orderCancelledStatus;

  /// No description provided for @orderExpired.
  ///
  /// In en, this message translates to:
  /// **'Expired'**
  String get orderExpired;

  /// No description provided for @stepPlaced.
  ///
  /// In en, this message translates to:
  /// **'Your order has been received.'**
  String get stepPlaced;

  /// No description provided for @stepAccepted.
  ///
  /// In en, this message translates to:
  /// **'The sales point accepted it and is preparing it.'**
  String get stepAccepted;

  /// No description provided for @stepReady.
  ///
  /// In en, this message translates to:
  /// **'Your order is ready to collect.'**
  String get stepReady;

  /// No description provided for @stepCompleted.
  ///
  /// In en, this message translates to:
  /// **'Handed over and paid. Thank you!'**
  String get stepCompleted;

  /// No description provided for @stepDeclined.
  ///
  /// In en, this message translates to:
  /// **'The sales point could not accept this order.'**
  String get stepDeclined;

  /// No description provided for @stepCancelled.
  ///
  /// In en, this message translates to:
  /// **'This order was cancelled.'**
  String get stepCancelled;

  /// No description provided for @stepExpired.
  ///
  /// In en, this message translates to:
  /// **'Nobody handled this order in time.'**
  String get stepExpired;

  /// No description provided for @yourPointsBalance.
  ///
  /// In en, this message translates to:
  /// **'Your points balance'**
  String get yourPointsBalance;

  /// No description provided for @pointsValue.
  ///
  /// In en, this message translates to:
  /// **'{points} pts'**
  String pointsValue(int points);

  /// No description provided for @pts.
  ///
  /// In en, this message translates to:
  /// **'pts'**
  String get pts;

  /// No description provided for @noPointsYet.
  ///
  /// In en, this message translates to:
  /// **'No points yet'**
  String get noPointsYet;

  /// No description provided for @noPointsYetMessage.
  ///
  /// In en, this message translates to:
  /// **'Place an order or show your loyalty QR at a sales point to start earning points.'**
  String get noPointsYetMessage;

  /// No description provided for @pointsExpiringSoon.
  ///
  /// In en, this message translates to:
  /// **'{points} pts expire within {days} days'**
  String pointsExpiringSoon(int points, int days);

  /// No description provided for @membershipTier.
  ///
  /// In en, this message translates to:
  /// **'{tier} Member'**
  String membershipTier(String tier);

  /// No description provided for @spendToReachTier.
  ///
  /// In en, this message translates to:
  /// **'Spend {amount} more this year to reach {tier}'**
  String spendToReachTier(String amount, String tier);

  /// No description provided for @topTierReached.
  ///
  /// In en, this message translates to:
  /// **'You have reached the top tier. Thank you!'**
  String get topTierReached;

  /// No description provided for @showMyLoyaltyQr.
  ///
  /// In en, this message translates to:
  /// **'Show my loyalty QR'**
  String get showMyLoyaltyQr;

  /// No description provided for @myLoyaltyQr.
  ///
  /// In en, this message translates to:
  /// **'My Loyalty QR'**
  String get myLoyaltyQr;

  /// No description provided for @loyaltyQrHint.
  ///
  /// In en, this message translates to:
  /// **'Show this code to the seller when you pay in store, so the purchase earns points.'**
  String get loyaltyQrHint;

  /// No description provided for @rewardsCatalogue.
  ///
  /// In en, this message translates to:
  /// **'Rewards Catalogue'**
  String get rewardsCatalogue;

  /// No description provided for @myRewards.
  ///
  /// In en, this message translates to:
  /// **'My Rewards'**
  String get myRewards;

  /// No description provided for @rewardsReady.
  ///
  /// In en, this message translates to:
  /// **'{count} ready to use'**
  String rewardsReady(int count);

  /// No description provided for @pointsHistory.
  ///
  /// In en, this message translates to:
  /// **'Points History'**
  String get pointsHistory;

  /// No description provided for @membershipTiers.
  ///
  /// In en, this message translates to:
  /// **'Membership Tiers'**
  String get membershipTiers;

  /// No description provided for @howPointsWork.
  ///
  /// In en, this message translates to:
  /// **'How Points Work'**
  String get howPointsWork;

  /// No description provided for @noRewards.
  ///
  /// In en, this message translates to:
  /// **'No rewards yet'**
  String get noRewards;

  /// No description provided for @noRewardsMessage.
  ///
  /// In en, this message translates to:
  /// **'New rewards will appear here.'**
  String get noRewardsMessage;

  /// No description provided for @blockedMinPoints.
  ///
  /// In en, this message translates to:
  /// **'You need more points to redeem.'**
  String get blockedMinPoints;

  /// No description provided for @blockedNotEnough.
  ///
  /// In en, this message translates to:
  /// **'Not enough points yet.'**
  String get blockedNotEnough;

  /// No description provided for @blockedPurchases.
  ///
  /// In en, this message translates to:
  /// **'Needs at least {count} purchases.'**
  String blockedPurchases(int count);

  /// No description provided for @blockedSpend.
  ///
  /// In en, this message translates to:
  /// **'Needs more total spending.'**
  String get blockedSpend;

  /// No description provided for @redeemCost.
  ///
  /// In en, this message translates to:
  /// **'Costs {points} points'**
  String redeemCost(int points);

  /// No description provided for @redeemHint.
  ///
  /// In en, this message translates to:
  /// **'The reward goes to your wallet. Show its QR code to the seller to get it.'**
  String get redeemHint;

  /// No description provided for @confirmRedemption.
  ///
  /// In en, this message translates to:
  /// **'Redeem'**
  String get confirmRedemption;

  /// No description provided for @rewardRedeemed.
  ///
  /// In en, this message translates to:
  /// **'Reward Redeemed!'**
  String get rewardRedeemed;

  /// No description provided for @myReward.
  ///
  /// In en, this message translates to:
  /// **'My Reward'**
  String get myReward;

  /// No description provided for @rewardQr.
  ///
  /// In en, this message translates to:
  /// **'Reward QR code'**
  String get rewardQr;

  /// No description provided for @showThisToSeller.
  ///
  /// In en, this message translates to:
  /// **'Show this code to the seller.'**
  String get showThisToSeller;

  /// No description provided for @validUntilDate.
  ///
  /// In en, this message translates to:
  /// **'Valid until {date}'**
  String validUntilDate(String date);

  /// No description provided for @rewardUsedOn.
  ///
  /// In en, this message translates to:
  /// **'Used {date}'**
  String rewardUsedOn(String date);

  /// No description provided for @rewardExpiredMessage.
  ///
  /// In en, this message translates to:
  /// **'This reward has expired or is no longer valid.'**
  String get rewardExpiredMessage;

  /// No description provided for @stateAvailable.
  ///
  /// In en, this message translates to:
  /// **'Available'**
  String get stateAvailable;

  /// No description provided for @stateUsed.
  ///
  /// In en, this message translates to:
  /// **'Used'**
  String get stateUsed;

  /// No description provided for @stateExpired.
  ///
  /// In en, this message translates to:
  /// **'Expired'**
  String get stateExpired;

  /// No description provided for @noRewardsHere.
  ///
  /// In en, this message translates to:
  /// **'Nothing here'**
  String get noRewardsHere;

  /// No description provided for @noRewardsHereMessage.
  ///
  /// In en, this message translates to:
  /// **'Rewards you redeem appear here.'**
  String get noRewardsHereMessage;

  /// No description provided for @typeEarned.
  ///
  /// In en, this message translates to:
  /// **'Earned'**
  String get typeEarned;

  /// No description provided for @typeUsed.
  ///
  /// In en, this message translates to:
  /// **'Used'**
  String get typeUsed;

  /// No description provided for @pointsEarned.
  ///
  /// In en, this message translates to:
  /// **'Points earned'**
  String get pointsEarned;

  /// No description provided for @pointsUsed.
  ///
  /// In en, this message translates to:
  /// **'Points used'**
  String get pointsUsed;

  /// No description provided for @pointsExpired.
  ///
  /// In en, this message translates to:
  /// **'Points expired'**
  String get pointsExpired;

  /// No description provided for @pointsReturned.
  ///
  /// In en, this message translates to:
  /// **'Points returned'**
  String get pointsReturned;

  /// No description provided for @pointsAdjusted.
  ///
  /// In en, this message translates to:
  /// **'Points adjusted'**
  String get pointsAdjusted;

  /// No description provided for @tiersHint.
  ///
  /// In en, this message translates to:
  /// **'Your tier follows what you spent over the last 12 months.'**
  String get tiersHint;

  /// No description provided for @currentTier.
  ///
  /// In en, this message translates to:
  /// **'Current'**
  String get currentTier;

  /// No description provided for @startingTier.
  ///
  /// In en, this message translates to:
  /// **'Where everyone starts'**
  String get startingTier;

  /// No description provided for @tierNeeds.
  ///
  /// In en, this message translates to:
  /// **'Spend {amount} a year'**
  String tierNeeds(String amount);

  /// No description provided for @tierEarns.
  ///
  /// In en, this message translates to:
  /// **'Earn {percent}% of the base points'**
  String tierEarns(int percent);

  /// No description provided for @earnPoints.
  ///
  /// In en, this message translates to:
  /// **'Earn points'**
  String get earnPoints;

  /// No description provided for @earnRule.
  ///
  /// In en, this message translates to:
  /// **'Get {points} points for every {amount} spent.'**
  String earnRule(int points, String amount);

  /// No description provided for @usePoints.
  ///
  /// In en, this message translates to:
  /// **'Use points'**
  String get usePoints;

  /// No description provided for @useRule.
  ///
  /// In en, this message translates to:
  /// **'Redeem rewards from {points} points.'**
  String useRule(int points);

  /// No description provided for @pointsValidity.
  ///
  /// In en, this message translates to:
  /// **'Validity'**
  String get pointsValidity;

  /// No description provided for @validityRule.
  ///
  /// In en, this message translates to:
  /// **'Points are valid for {months} months from the day they are earned.'**
  String validityRule(int months);

  /// No description provided for @currentPromotions.
  ///
  /// In en, this message translates to:
  /// **'Current promotions'**
  String get currentPromotions;

  /// No description provided for @pointsEstimate.
  ///
  /// In en, this message translates to:
  /// **'Points estimate'**
  String get pointsEstimate;

  /// No description provided for @purchaseAmount.
  ///
  /// In en, this message translates to:
  /// **'Purchase amount'**
  String get purchaseAmount;

  /// No description provided for @calculate.
  ///
  /// In en, this message translates to:
  /// **'Calculate'**
  String get calculate;

  /// No description provided for @estimatedPoints.
  ///
  /// In en, this message translates to:
  /// **'Estimated points'**
  String get estimatedPoints;

  /// No description provided for @estimateNote.
  ///
  /// In en, this message translates to:
  /// **'The points you actually earn follow the rules and promotions at the time of the purchase.'**
  String get estimateNote;

  /// No description provided for @notifications.
  ///
  /// In en, this message translates to:
  /// **'Notifications'**
  String get notifications;

  /// No description provided for @markAllRead.
  ///
  /// In en, this message translates to:
  /// **'Mark all as read'**
  String get markAllRead;

  /// No description provided for @notificationSettings.
  ///
  /// In en, this message translates to:
  /// **'Notification Settings'**
  String get notificationSettings;

  /// No description provided for @noNotifications.
  ///
  /// In en, this message translates to:
  /// **'No notifications'**
  String get noNotifications;

  /// No description provided for @noNotificationsMessage.
  ///
  /// In en, this message translates to:
  /// **'Updates about your orders, rewards and offers appear here.'**
  String get noNotificationsMessage;

  /// No description provided for @close.
  ///
  /// In en, this message translates to:
  /// **'Close'**
  String get close;

  /// No description provided for @catOrders.
  ///
  /// In en, this message translates to:
  /// **'Orders'**
  String get catOrders;

  /// No description provided for @catLoyalty.
  ///
  /// In en, this message translates to:
  /// **'Loyalty & Rewards'**
  String get catLoyalty;

  /// No description provided for @catPromotions.
  ///
  /// In en, this message translates to:
  /// **'Promotions & Offers'**
  String get catPromotions;

  /// No description provided for @catSystem.
  ///
  /// In en, this message translates to:
  /// **'System & News'**
  String get catSystem;

  /// No description provided for @alwaysOn.
  ///
  /// In en, this message translates to:
  /// **'Always on'**
  String get alwaysOn;

  /// No description provided for @pushOnPhone.
  ///
  /// In en, this message translates to:
  /// **'Push on this phone'**
  String get pushOnPhone;

  /// No description provided for @pushNotOnThisPhone.
  ///
  /// In en, this message translates to:
  /// **'Push notifications are not set up on this phone, so messages arrive in the app only.'**
  String get pushNotOnThisPhone;

  /// No description provided for @hintLoyalty.
  ///
  /// In en, this message translates to:
  /// **'Points, tier updates, rewards'**
  String get hintLoyalty;

  /// No description provided for @hintPromotions.
  ///
  /// In en, this message translates to:
  /// **'New products, discounts, campaigns'**
  String get hintPromotions;

  /// No description provided for @hintSystem.
  ///
  /// In en, this message translates to:
  /// **'App updates and announcements'**
  String get hintSystem;

  /// No description provided for @ntOrderAccepted.
  ///
  /// In en, this message translates to:
  /// **'Order accepted'**
  String get ntOrderAccepted;

  /// No description provided for @ntOrderDeclined.
  ///
  /// In en, this message translates to:
  /// **'Order declined'**
  String get ntOrderDeclined;

  /// No description provided for @ntOrderReady.
  ///
  /// In en, this message translates to:
  /// **'Order ready'**
  String get ntOrderReady;

  /// No description provided for @ntOrderComplete.
  ///
  /// In en, this message translates to:
  /// **'Order complete'**
  String get ntOrderComplete;

  /// No description provided for @ntOrderCancelled.
  ///
  /// In en, this message translates to:
  /// **'Order cancelled'**
  String get ntOrderCancelled;

  /// No description provided for @ntOrderExpired.
  ///
  /// In en, this message translates to:
  /// **'Order expired'**
  String get ntOrderExpired;

  /// No description provided for @ntRewardRedeemed.
  ///
  /// In en, this message translates to:
  /// **'Reward redeemed'**
  String get ntRewardRedeemed;

  /// No description provided for @ntRewardUsed.
  ///
  /// In en, this message translates to:
  /// **'Reward used'**
  String get ntRewardUsed;

  /// No description provided for @ntRewardExpired.
  ///
  /// In en, this message translates to:
  /// **'Reward expired'**
  String get ntRewardExpired;

  /// No description provided for @ntSupportReply.
  ///
  /// In en, this message translates to:
  /// **'Support reply'**
  String get ntSupportReply;

  /// No description provided for @ntLutiano.
  ///
  /// In en, this message translates to:
  /// **'LUTIANO Coffee'**
  String get ntLutiano;

  /// No description provided for @helpAndSupport.
  ///
  /// In en, this message translates to:
  /// **'Help & Support'**
  String get helpAndSupport;

  /// No description provided for @contactSupport.
  ///
  /// In en, this message translates to:
  /// **'Contact Support'**
  String get contactSupport;

  /// No description provided for @myRequests.
  ///
  /// In en, this message translates to:
  /// **'My Requests'**
  String get myRequests;

  /// No description provided for @faq.
  ///
  /// In en, this message translates to:
  /// **'Frequently Asked Questions'**
  String get faq;

  /// No description provided for @supportRequest.
  ///
  /// In en, this message translates to:
  /// **'Support Request'**
  String get supportRequest;

  /// No description provided for @teamReply.
  ///
  /// In en, this message translates to:
  /// **'Reply from the team'**
  String get teamReply;

  /// No description provided for @noReplyYet.
  ///
  /// In en, this message translates to:
  /// **'The team has not replied yet. You will be notified.'**
  String get noReplyYet;

  /// No description provided for @noRequests.
  ///
  /// In en, this message translates to:
  /// **'No requests'**
  String get noRequests;

  /// No description provided for @noRequestsMessage.
  ///
  /// In en, this message translates to:
  /// **'Your requests to the team appear here.'**
  String get noRequestsMessage;

  /// No description provided for @requestSent.
  ///
  /// In en, this message translates to:
  /// **'Request sent.'**
  String get requestSent;

  /// No description provided for @requestCategory.
  ///
  /// In en, this message translates to:
  /// **'What is it about?'**
  String get requestCategory;

  /// No description provided for @requestTitle.
  ///
  /// In en, this message translates to:
  /// **'Title'**
  String get requestTitle;

  /// No description provided for @requestDescription.
  ///
  /// In en, this message translates to:
  /// **'Describe the problem or question'**
  String get requestDescription;

  /// No description provided for @enterATitle.
  ///
  /// In en, this message translates to:
  /// **'Enter a short title.'**
  String get enterATitle;

  /// No description provided for @describeTheProblem.
  ///
  /// In en, this message translates to:
  /// **'Tell us a little more.'**
  String get describeTheProblem;

  /// No description provided for @addPhotoOptional.
  ///
  /// In en, this message translates to:
  /// **'Add a photo (optional)'**
  String get addPhotoOptional;

  /// No description provided for @submitRequest.
  ///
  /// In en, this message translates to:
  /// **'Send Request'**
  String get submitRequest;

  /// No description provided for @reqOrder.
  ///
  /// In en, this message translates to:
  /// **'Order & payment'**
  String get reqOrder;

  /// No description provided for @reqRewards.
  ///
  /// In en, this message translates to:
  /// **'Points & rewards'**
  String get reqRewards;

  /// No description provided for @reqAccount.
  ///
  /// In en, this message translates to:
  /// **'My account'**
  String get reqAccount;

  /// No description provided for @reqIssue.
  ///
  /// In en, this message translates to:
  /// **'Report an issue'**
  String get reqIssue;

  /// No description provided for @reqQuestion.
  ///
  /// In en, this message translates to:
  /// **'A question'**
  String get reqQuestion;

  /// No description provided for @reqIdea.
  ///
  /// In en, this message translates to:
  /// **'Suggest an improvement'**
  String get reqIdea;

  /// No description provided for @reqOpen.
  ///
  /// In en, this message translates to:
  /// **'Open'**
  String get reqOpen;

  /// No description provided for @reqInProgress.
  ///
  /// In en, this message translates to:
  /// **'In progress'**
  String get reqInProgress;

  /// No description provided for @reqResolved.
  ///
  /// In en, this message translates to:
  /// **'Resolved'**
  String get reqResolved;

  /// No description provided for @faqQ1.
  ///
  /// In en, this message translates to:
  /// **'How do I order?'**
  String get faqQ1;

  /// No description provided for @faqA1.
  ///
  /// In en, this message translates to:
  /// **'Choose a sales point (scan its QR code or pick it from the list), add products to your cart and place your order. A seller accepts it and you pay in cash when you collect it.'**
  String get faqA1;

  /// No description provided for @faqQ2.
  ///
  /// In en, this message translates to:
  /// **'How do I earn points?'**
  String get faqQ2;

  /// No description provided for @faqA2.
  ///
  /// In en, this message translates to:
  /// **'Show your loyalty QR code to the seller when you pay, or order in the app. Points are added when the order is handed over.'**
  String get faqA2;

  /// No description provided for @faqQ3.
  ///
  /// In en, this message translates to:
  /// **'How do I use a reward?'**
  String get faqQ3;

  /// No description provided for @faqA3.
  ///
  /// In en, this message translates to:
  /// **'Redeem it from the Rewards tab, then show its QR code to the seller at the sales point.'**
  String get faqA3;

  /// No description provided for @faqQ4.
  ///
  /// In en, this message translates to:
  /// **'Can I cancel an order?'**
  String get faqQ4;

  /// No description provided for @faqA4.
  ///
  /// In en, this message translates to:
  /// **'Yes, while the sales point has not accepted it yet. After that, contact the sales point or write to support.'**
  String get faqA4;

  /// No description provided for @faqQ5.
  ///
  /// In en, this message translates to:
  /// **'I forgot my password.'**
  String get faqQ5;

  /// No description provided for @faqA5.
  ///
  /// In en, this message translates to:
  /// **'On the log in screen choose Forgot password, or log in with a code sent to your phone.'**
  String get faqA5;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en', 'fr'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return AppLocalizationsEn();
    case 'fr':
      return AppLocalizationsFr();
  }

  throw FlutterError(
      'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
      'an issue with the localizations generation tool. Please file an issue '
      'on GitHub with a reproducible sample app and the gen-l10n configuration '
      'that was used.');
}

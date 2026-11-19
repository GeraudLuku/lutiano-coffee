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

  /// No description provided for @enterYourEmail.
  ///
  /// In en, this message translates to:
  /// **'Enter your email.'**
  String get enterYourEmail;

  /// No description provided for @enterTheCodeFromThe.
  ///
  /// In en, this message translates to:
  /// **'Enter the code from the SMS.'**
  String get enterTheCodeFromThe;

  /// No description provided for @chooseAPasswordOfAt.
  ///
  /// In en, this message translates to:
  /// **'Choose a password of at least 10 characters.'**
  String get chooseAPasswordOfAt;

  /// No description provided for @passwordChangedSignInWith.
  ///
  /// In en, this message translates to:
  /// **'Password changed. Sign in with your new password.'**
  String get passwordChangedSignInWith;

  /// No description provided for @forgotPassword.
  ///
  /// In en, this message translates to:
  /// **'Forgot Password'**
  String get forgotPassword;

  /// No description provided for @enterTheEmailOfYour.
  ///
  /// In en, this message translates to:
  /// **'Enter the email of your seller account. We will send a code by SMS to the phone number saved on it.'**
  String get enterTheEmailOfYour;

  /// No description provided for @email.
  ///
  /// In en, this message translates to:
  /// **'Email'**
  String get email;

  /// No description provided for @ifAnAccountWithA.
  ///
  /// In en, this message translates to:
  /// **'If an account with a phone number exists for this email, a code was sent by SMS. It is valid for 5 minutes. No SMS? Ask your administrator to reset your access.'**
  String get ifAnAccountWithA;

  /// No description provided for @codeFromTheSms.
  ///
  /// In en, this message translates to:
  /// **'Code from the SMS'**
  String get codeFromTheSms;

  /// No description provided for @newPassword10Characters.
  ///
  /// In en, this message translates to:
  /// **'New password (10+ characters)'**
  String get newPassword10Characters;

  /// No description provided for @useADifferentEmail.
  ///
  /// In en, this message translates to:
  /// **'Use a different email'**
  String get useADifferentEmail;

  /// No description provided for @lutiano.
  ///
  /// In en, this message translates to:
  /// **'LUTIANO'**
  String get lutiano;

  /// No description provided for @welcomeBack.
  ///
  /// In en, this message translates to:
  /// **'Welcome Back 👋'**
  String get welcomeBack;

  /// No description provided for @signInToYourSeller.
  ///
  /// In en, this message translates to:
  /// **'Sign in to your seller account'**
  String get signInToYourSeller;

  /// No description provided for @yourSessionEndedPleaseSign.
  ///
  /// In en, this message translates to:
  /// **'Your session ended. Please sign in again.'**
  String get yourSessionEndedPleaseSign;

  /// No description provided for @password.
  ///
  /// In en, this message translates to:
  /// **'Password'**
  String get password;

  /// No description provided for @forgotPassword2.
  ///
  /// In en, this message translates to:
  /// **'Forgot password?'**
  String get forgotPassword2;

  /// No description provided for @scanCustomerQr.
  ///
  /// In en, this message translates to:
  /// **'Scan Customer QR'**
  String get scanCustomerQr;

  /// No description provided for @theCameraIsNotAvailable.
  ///
  /// In en, this message translates to:
  /// **'The camera is not available. Allow camera access for this app in Settings, then try again.'**
  String get theCameraIsNotAvailable;

  /// No description provided for @pointTheCameraAtThe.
  ///
  /// In en, this message translates to:
  /// **'Point the camera at the customer\'s loyalty QR code.'**
  String get pointTheCameraAtThe;

  /// No description provided for @saleDetails.
  ///
  /// In en, this message translates to:
  /// **'Sale Details'**
  String get saleDetails;

  /// No description provided for @dateTime.
  ///
  /// In en, this message translates to:
  /// **'Date & Time'**
  String get dateTime;

  /// No description provided for @salesOperation.
  ///
  /// In en, this message translates to:
  /// **'Sales operation'**
  String get salesOperation;

  /// No description provided for @paymentMethod.
  ///
  /// In en, this message translates to:
  /// **'Payment method'**
  String get paymentMethod;

  /// No description provided for @customer.
  ///
  /// In en, this message translates to:
  /// **'Customer'**
  String get customer;

  /// No description provided for @loyaltyPointsEarned.
  ///
  /// In en, this message translates to:
  /// **'Loyalty Points Earned'**
  String get loyaltyPointsEarned;

  /// No description provided for @cashReceived.
  ///
  /// In en, this message translates to:
  /// **'Cash Received'**
  String get cashReceived;

  /// No description provided for @changeGiven.
  ///
  /// In en, this message translates to:
  /// **'Change Given'**
  String get changeGiven;

  /// No description provided for @totalAmount.
  ///
  /// In en, this message translates to:
  /// **'Total Amount'**
  String get totalAmount;

  /// No description provided for @viewPrintReceipt.
  ///
  /// In en, this message translates to:
  /// **'View / Print Receipt'**
  String get viewPrintReceipt;

  /// No description provided for @salesHistory.
  ///
  /// In en, this message translates to:
  /// **'Sales History'**
  String get salesHistory;

  /// No description provided for @sales.
  ///
  /// In en, this message translates to:
  /// **'Sales'**
  String get sales;

  /// No description provided for @xaf.
  ///
  /// In en, this message translates to:
  /// **'XAF'**
  String get xaf;

  /// No description provided for @items.
  ///
  /// In en, this message translates to:
  /// **'Items'**
  String get items;

  /// No description provided for @noSalesInThisPeriod.
  ///
  /// In en, this message translates to:
  /// **'No sales in this period'**
  String get noSalesInThisPeriod;

  /// No description provided for @completedSalesWillAppearHere.
  ///
  /// In en, this message translates to:
  /// **'Completed sales will appear here.'**
  String get completedSalesWillAppearHere;

  /// No description provided for @newSale.
  ///
  /// In en, this message translates to:
  /// **'New Sale'**
  String get newSale;

  /// No description provided for @quickActions.
  ///
  /// In en, this message translates to:
  /// **'Quick actions'**
  String get quickActions;

  /// No description provided for @salesTrend7Days.
  ///
  /// In en, this message translates to:
  /// **'Sales trend (7 days)'**
  String get salesTrend7Days;

  /// No description provided for @noSalesOperationAssigned.
  ///
  /// In en, this message translates to:
  /// **'No sales operation assigned'**
  String get noSalesOperationAssigned;

  /// No description provided for @askYourManagerToAssign.
  ///
  /// In en, this message translates to:
  /// **'Ask your manager to assign you to an active sales operation, then refresh.'**
  String get askYourManagerToAssign;

  /// No description provided for @refresh.
  ///
  /// In en, this message translates to:
  /// **'Refresh'**
  String get refresh;

  /// No description provided for @openYourCashShift.
  ///
  /// In en, this message translates to:
  /// **'Open your cash shift'**
  String get openYourCashShift;

  /// No description provided for @cashSalesAreRecordedAgainst.
  ///
  /// In en, this message translates to:
  /// **'Cash sales are recorded against an open cash shift. Open one now?'**
  String get cashSalesAreRecordedAgainst;

  /// No description provided for @notNow.
  ///
  /// In en, this message translates to:
  /// **'Not now'**
  String get notNow;

  /// No description provided for @openShift.
  ///
  /// In en, this message translates to:
  /// **'Open shift'**
  String get openShift;

  /// No description provided for @myDay.
  ///
  /// In en, this message translates to:
  /// **'My Day'**
  String get myDay;

  /// No description provided for @todaySSales.
  ///
  /// In en, this message translates to:
  /// **'Today\'s Sales'**
  String get todaySSales;

  /// No description provided for @orders.
  ///
  /// In en, this message translates to:
  /// **'Orders'**
  String get orders;

  /// No description provided for @myCommission.
  ///
  /// In en, this message translates to:
  /// **'My Commission'**
  String get myCommission;

  /// No description provided for @pointsIssued.
  ///
  /// In en, this message translates to:
  /// **'Points Issued'**
  String get pointsIssued;

  /// No description provided for @receiveStock.
  ///
  /// In en, this message translates to:
  /// **'Receive Stock'**
  String get receiveStock;

  /// No description provided for @countWhatArrivedBeforeConfirming.
  ///
  /// In en, this message translates to:
  /// **'Count what arrived before confirming. Stock in transit cannot be sold until you confirm it.'**
  String get countWhatArrivedBeforeConfirming;

  /// No description provided for @noDeliveriesWaiting.
  ///
  /// In en, this message translates to:
  /// **'No deliveries waiting'**
  String get noDeliveriesWaiting;

  /// No description provided for @stockSentToYourOperation.
  ///
  /// In en, this message translates to:
  /// **'Stock sent to your operation will appear here.'**
  String get stockSentToYourOperation;

  /// No description provided for @inTransit.
  ///
  /// In en, this message translates to:
  /// **'In transit'**
  String get inTransit;

  /// No description provided for @confirmReceipt.
  ///
  /// In en, this message translates to:
  /// **'Confirm Receipt'**
  String get confirmReceipt;

  /// No description provided for @explainWhyTheQuantityIs.
  ///
  /// In en, this message translates to:
  /// **'Explain why the quantity is different.'**
  String get explainWhyTheQuantityIs;

  /// No description provided for @quantityReceived.
  ///
  /// In en, this message translates to:
  /// **'Quantity received'**
  String get quantityReceived;

  /// No description provided for @reasonMissingDamaged.
  ///
  /// In en, this message translates to:
  /// **'Reason (missing, damaged…)'**
  String get reasonMissingDamaged;

  /// No description provided for @cancel.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get cancel;

  /// No description provided for @inventory.
  ///
  /// In en, this message translates to:
  /// **'Inventory'**
  String get inventory;

  /// No description provided for @myInventory.
  ///
  /// In en, this message translates to:
  /// **'My Inventory'**
  String get myInventory;

  /// No description provided for @unitsInStock.
  ///
  /// In en, this message translates to:
  /// **'Units in stock'**
  String get unitsInStock;

  /// No description provided for @lowStock.
  ///
  /// In en, this message translates to:
  /// **'Low stock'**
  String get lowStock;

  /// No description provided for @stockValueSellingPrice.
  ///
  /// In en, this message translates to:
  /// **'Stock value (selling price)'**
  String get stockValueSellingPrice;

  /// No description provided for @viewInventory.
  ///
  /// In en, this message translates to:
  /// **'View Inventory'**
  String get viewInventory;

  /// No description provided for @allProductsAtThisOperation.
  ///
  /// In en, this message translates to:
  /// **'All products at this operation'**
  String get allProductsAtThisOperation;

  /// No description provided for @stockMovements.
  ///
  /// In en, this message translates to:
  /// **'Stock Movements'**
  String get stockMovements;

  /// No description provided for @receivedStockAndSales.
  ///
  /// In en, this message translates to:
  /// **'Received stock and sales'**
  String get receivedStockAndSales;

  /// No description provided for @lowStockAlerts.
  ///
  /// In en, this message translates to:
  /// **'Low Stock Alerts'**
  String get lowStockAlerts;

  /// No description provided for @confirmDeliveriesFromTheCentral.
  ///
  /// In en, this message translates to:
  /// **'Confirm deliveries from the central store'**
  String get confirmDeliveriesFromTheCentral;

  /// No description provided for @allStockLevelsAreHealthy.
  ///
  /// In en, this message translates to:
  /// **'All stock levels are healthy'**
  String get allStockLevelsAreHealthy;

  /// No description provided for @askYourInventoryManagerFor.
  ///
  /// In en, this message translates to:
  /// **'Ask your inventory manager for a replenishment to avoid stockouts.'**
  String get askYourInventoryManagerFor;

  /// No description provided for @productDetails.
  ///
  /// In en, this message translates to:
  /// **'Product Details'**
  String get productDetails;

  /// No description provided for @currentStock.
  ///
  /// In en, this message translates to:
  /// **'Current Stock'**
  String get currentStock;

  /// No description provided for @minimumLevel.
  ///
  /// In en, this message translates to:
  /// **'Minimum Level'**
  String get minimumLevel;

  /// No description provided for @salesPrice.
  ///
  /// In en, this message translates to:
  /// **'Sales Price'**
  String get salesPrice;

  /// No description provided for @sku.
  ///
  /// In en, this message translates to:
  /// **'SKU'**
  String get sku;

  /// No description provided for @recentMovements.
  ///
  /// In en, this message translates to:
  /// **'Recent movements'**
  String get recentMovements;

  /// No description provided for @noMovementsYet.
  ///
  /// In en, this message translates to:
  /// **'No movements yet'**
  String get noMovementsYet;

  /// No description provided for @searchProduct.
  ///
  /// In en, this message translates to:
  /// **'Search product…'**
  String get searchProduct;

  /// No description provided for @noProductsMatch.
  ///
  /// In en, this message translates to:
  /// **'No products match'**
  String get noProductsMatch;

  /// No description provided for @all.
  ///
  /// In en, this message translates to:
  /// **'All'**
  String get all;

  /// No description provided for @inReceived.
  ///
  /// In en, this message translates to:
  /// **'In (Received)'**
  String get inReceived;

  /// No description provided for @outSold.
  ///
  /// In en, this message translates to:
  /// **'Out (Sold)'**
  String get outSold;

  /// No description provided for @noMovementsToShow.
  ///
  /// In en, this message translates to:
  /// **'No movements to show'**
  String get noMovementsToShow;

  /// No description provided for @stock.
  ///
  /// In en, this message translates to:
  /// **'Stock: '**
  String get stock;

  /// No description provided for @mySalesOperation.
  ///
  /// In en, this message translates to:
  /// **'My Sales Operation'**
  String get mySalesOperation;

  /// No description provided for @today.
  ///
  /// In en, this message translates to:
  /// **'Today'**
  String get today;

  /// No description provided for @last7Days.
  ///
  /// In en, this message translates to:
  /// **'Last 7 days'**
  String get last7Days;

  /// No description provided for @thisMonth.
  ///
  /// In en, this message translates to:
  /// **'This month'**
  String get thisMonth;

  /// No description provided for @transactions.
  ///
  /// In en, this message translates to:
  /// **'Transactions'**
  String get transactions;

  /// No description provided for @myQrCode.
  ///
  /// In en, this message translates to:
  /// **'My QR Code'**
  String get myQrCode;

  /// No description provided for @showMyPermanentQrCode.
  ///
  /// In en, this message translates to:
  /// **'Show my permanent QR code'**
  String get showMyPermanentQrCode;

  /// No description provided for @assignedProducts.
  ///
  /// In en, this message translates to:
  /// **'Assigned Products'**
  String get assignedProducts;

  /// No description provided for @productsAndStockICan.
  ///
  /// In en, this message translates to:
  /// **'Products and stock I can sell'**
  String get productsAndStockICan;

  /// No description provided for @switchOperation.
  ///
  /// In en, this message translates to:
  /// **'Switch Operation'**
  String get switchOperation;

  /// No description provided for @inactive.
  ///
  /// In en, this message translates to:
  /// **'Inactive'**
  String get inactive;

  /// No description provided for @chooseSalesOperation.
  ///
  /// In en, this message translates to:
  /// **'Choose sales operation'**
  String get chooseSalesOperation;

  /// No description provided for @switchingEmptiesTheCurrentCart.
  ///
  /// In en, this message translates to:
  /// **'Switching empties the current cart.'**
  String get switchingEmptiesTheCurrentCart;

  /// No description provided for @showThisQrCodeTo.
  ///
  /// In en, this message translates to:
  /// **'Show this QR code to customers to start a purchase at your operation.'**
  String get showThisQrCodeTo;

  /// No description provided for @qrCodeUnavailableCheckYour.
  ///
  /// In en, this message translates to:
  /// **'QR code unavailable. Check your connection.'**
  String get qrCodeUnavailableCheckYour;

  /// No description provided for @thisIsYourPermanentQr.
  ///
  /// In en, this message translates to:
  /// **'This is your permanent QR code. It identifies the sales operation, not you or a single sale, and stays the same if sellers change.'**
  String get thisIsYourPermanentQr;

  /// No description provided for @aboutLutiano.
  ///
  /// In en, this message translates to:
  /// **'About LUTIANO'**
  String get aboutLutiano;

  /// No description provided for @sellerOperationsApp.
  ///
  /// In en, this message translates to:
  /// **'Seller / Operations App'**
  String get sellerOperationsApp;

  /// No description provided for @licenses.
  ///
  /// In en, this message translates to:
  /// **'Licenses'**
  String get licenses;

  /// No description provided for @thirdPartySoftwareAndCredits.
  ///
  /// In en, this message translates to:
  /// **'Third-party software and credits'**
  String get thirdPartySoftwareAndCredits;

  /// No description provided for @passwordChanged.
  ///
  /// In en, this message translates to:
  /// **'Password changed.'**
  String get passwordChanged;

  /// No description provided for @security.
  ///
  /// In en, this message translates to:
  /// **'Security'**
  String get security;

  /// No description provided for @keepYourAccountSecureUse.
  ///
  /// In en, this message translates to:
  /// **'Keep your account secure. Use at least 10 characters and do not share your password.'**
  String get keepYourAccountSecureUse;

  /// No description provided for @currentPassword.
  ///
  /// In en, this message translates to:
  /// **'Current password'**
  String get currentPassword;

  /// No description provided for @newPassword.
  ///
  /// In en, this message translates to:
  /// **'New password'**
  String get newPassword;

  /// No description provided for @confirmNewPassword.
  ///
  /// In en, this message translates to:
  /// **'Confirm new password'**
  String get confirmNewPassword;

  /// No description provided for @myProfile.
  ///
  /// In en, this message translates to:
  /// **'My Profile'**
  String get myProfile;

  /// No description provided for @phone.
  ///
  /// In en, this message translates to:
  /// **'Phone'**
  String get phone;

  /// No description provided for @myRole.
  ///
  /// In en, this message translates to:
  /// **'My Role'**
  String get myRole;

  /// No description provided for @operation.
  ///
  /// In en, this message translates to:
  /// **'Operation'**
  String get operation;

  /// No description provided for @changePassword.
  ///
  /// In en, this message translates to:
  /// **'Change Password'**
  String get changePassword;

  /// No description provided for @updateYourAccountPassword.
  ///
  /// In en, this message translates to:
  /// **'Update your account password'**
  String get updateYourAccountPassword;

  /// No description provided for @helpSupport.
  ///
  /// In en, this message translates to:
  /// **'Help & Support'**
  String get helpSupport;

  /// No description provided for @getHelpFromTheLutiano.
  ///
  /// In en, this message translates to:
  /// **'Get help from the LUTIANO team'**
  String get getHelpFromTheLutiano;

  /// No description provided for @versionAndLegalInformation.
  ///
  /// In en, this message translates to:
  /// **'Version and legal information'**
  String get versionAndLegalInformation;

  /// No description provided for @signOut.
  ///
  /// In en, this message translates to:
  /// **'Sign Out'**
  String get signOut;

  /// No description provided for @yourNamePhoneAndRole.
  ///
  /// In en, this message translates to:
  /// **'Your name, phone and role are managed by your administrator.'**
  String get yourNamePhoneAndRole;

  /// No description provided for @forAccountStockOrPayment.
  ///
  /// In en, this message translates to:
  /// **'For account, stock or payment problems, contact your operations manager or administrator. They can reset your access, assign your sales operation and review stock or cash issues.'**
  String get forAccountStockOrPayment;

  /// No description provided for @ok.
  ///
  /// In en, this message translates to:
  /// **'OK'**
  String get ok;

  /// No description provided for @signOut2.
  ///
  /// In en, this message translates to:
  /// **'Sign out?'**
  String get signOut2;

  /// No description provided for @signOut3.
  ///
  /// In en, this message translates to:
  /// **'Sign out'**
  String get signOut3;

  /// No description provided for @myRolePermissions.
  ///
  /// In en, this message translates to:
  /// **'My Role & Permissions'**
  String get myRolePermissions;

  /// No description provided for @operationsRole.
  ///
  /// In en, this message translates to:
  /// **'Operations role'**
  String get operationsRole;

  /// No description provided for @roleDescription.
  ///
  /// In en, this message translates to:
  /// **'Role description'**
  String get roleDescription;

  /// No description provided for @myPermissions.
  ///
  /// In en, this message translates to:
  /// **'My permissions'**
  String get myPermissions;

  /// No description provided for @permissionsAreEnforcedByThe.
  ///
  /// In en, this message translates to:
  /// **'Permissions are enforced by the server. Your administrator manages your role and operation.'**
  String get permissionsAreEnforcedByThe;

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

  /// No description provided for @yourCartIsEmpty.
  ///
  /// In en, this message translates to:
  /// **'Your cart is empty'**
  String get yourCartIsEmpty;

  /// No description provided for @addProducts.
  ///
  /// In en, this message translates to:
  /// **'Add products'**
  String get addProducts;

  /// No description provided for @orderSummary.
  ///
  /// In en, this message translates to:
  /// **'Order Summary'**
  String get orderSummary;

  /// No description provided for @total.
  ///
  /// In en, this message translates to:
  /// **'Total'**
  String get total;

  /// No description provided for @pricesAreSetCentrallyAnd.
  ///
  /// In en, this message translates to:
  /// **'Prices are set centrally and cannot be changed at checkout.'**
  String get pricesAreSetCentrallyAnd;

  /// No description provided for @proceedToPayment.
  ///
  /// In en, this message translates to:
  /// **'Proceed to Payment'**
  String get proceedToPayment;

  /// No description provided for @notEnoughStockReduceThe.
  ///
  /// In en, this message translates to:
  /// **'Not enough stock — reduce the quantity.'**
  String get notEnoughStockReduceThe;

  /// No description provided for @remove.
  ///
  /// In en, this message translates to:
  /// **'Remove'**
  String get remove;

  /// No description provided for @comingSoon.
  ///
  /// In en, this message translates to:
  /// **'Coming soon'**
  String get comingSoon;

  /// No description provided for @collectPayment.
  ///
  /// In en, this message translates to:
  /// **'Collect Payment'**
  String get collectPayment;

  /// No description provided for @paymentMethod2.
  ///
  /// In en, this message translates to:
  /// **'Payment method'**
  String get paymentMethod2;

  /// No description provided for @amountReceivedCash.
  ///
  /// In en, this message translates to:
  /// **'Amount received (cash)'**
  String get amountReceivedCash;

  /// No description provided for @changeToGive.
  ///
  /// In en, this message translates to:
  /// **'Change to give'**
  String get changeToGive;

  /// No description provided for @confirmOnlyAfterYouHave.
  ///
  /// In en, this message translates to:
  /// **'Confirm only after you have received the cash. The sale is recorded when the server confirms it.'**
  String get confirmOnlyAfterYouHave;

  /// No description provided for @selectProducts.
  ///
  /// In en, this message translates to:
  /// **'Select Products'**
  String get selectProducts;

  /// No description provided for @noProducts.
  ///
  /// In en, this message translates to:
  /// **'No products'**
  String get noProducts;

  /// No description provided for @nothingMatchesOrNoStock.
  ///
  /// In en, this message translates to:
  /// **'Nothing matches, or no stock has been allocated to this operation yet.'**
  String get nothingMatchesOrNoStock;

  /// No description provided for @viewCart.
  ///
  /// In en, this message translates to:
  /// **'View Cart'**
  String get viewCart;

  /// No description provided for @saleCompleted.
  ///
  /// In en, this message translates to:
  /// **'Sale Completed!'**
  String get saleCompleted;

  /// No description provided for @thePurchaseHasBeenRecorded.
  ///
  /// In en, this message translates to:
  /// **'The purchase has been recorded successfully.'**
  String get thePurchaseHasBeenRecorded;

  /// No description provided for @saleReference.
  ///
  /// In en, this message translates to:
  /// **'Sale Reference'**
  String get saleReference;

  /// No description provided for @viewReceipt.
  ///
  /// In en, this message translates to:
  /// **'View Receipt'**
  String get viewReceipt;

  /// No description provided for @backToHome.
  ///
  /// In en, this message translates to:
  /// **'Back to Home'**
  String get backToHome;

  /// No description provided for @customerOptional.
  ///
  /// In en, this message translates to:
  /// **'Customer (optional)'**
  String get customerOptional;

  /// No description provided for @removeCustomer.
  ///
  /// In en, this message translates to:
  /// **'Remove customer'**
  String get removeCustomer;

  /// No description provided for @home.
  ///
  /// In en, this message translates to:
  /// **'Home'**
  String get home;

  /// No description provided for @profile.
  ///
  /// In en, this message translates to:
  /// **'Profile'**
  String get profile;

  /// No description provided for @enterTheAmountNeeded.
  ///
  /// In en, this message translates to:
  /// **'Enter the amount needed.'**
  String get enterTheAmountNeeded;

  /// No description provided for @sayWhatTheCashIs.
  ///
  /// In en, this message translates to:
  /// **'Say what the cash is for.'**
  String get sayWhatTheCashIs;

  /// No description provided for @requestCashOutflow.
  ///
  /// In en, this message translates to:
  /// **'Request cash outflow'**
  String get requestCashOutflow;

  /// No description provided for @cashLeavesYourDrawerOnly.
  ///
  /// In en, this message translates to:
  /// **'Cash leaves your drawer only after a manager approves.'**
  String get cashLeavesYourDrawerOnly;

  /// No description provided for @purpose.
  ///
  /// In en, this message translates to:
  /// **'Purpose'**
  String get purpose;

  /// No description provided for @amount.
  ///
  /// In en, this message translates to:
  /// **'Amount'**
  String get amount;

  /// No description provided for @whatIsItFor.
  ///
  /// In en, this message translates to:
  /// **'What is it for?'**
  String get whatIsItFor;

  /// No description provided for @enterTheAmountYouAre.
  ///
  /// In en, this message translates to:
  /// **'Enter the amount you are handing over.'**
  String get enterTheAmountYouAre;

  /// No description provided for @chooseWhoReceivesTheCash.
  ///
  /// In en, this message translates to:
  /// **'Choose who receives the cash.'**
  String get chooseWhoReceivesTheCash;

  /// No description provided for @enterTheDepositSlipReference.
  ///
  /// In en, this message translates to:
  /// **'Enter the deposit slip reference.'**
  String get enterTheDepositSlipReference;

  /// No description provided for @recordRemittance.
  ///
  /// In en, this message translates to:
  /// **'Record remittance'**
  String get recordRemittance;

  /// No description provided for @handToManager.
  ///
  /// In en, this message translates to:
  /// **'Hand to manager'**
  String get handToManager;

  /// No description provided for @bankDeposit.
  ///
  /// In en, this message translates to:
  /// **'Bank deposit'**
  String get bankDeposit;

  /// No description provided for @handedTo.
  ///
  /// In en, this message translates to:
  /// **'Handed to'**
  String get handedTo;

  /// No description provided for @depositSlipReference.
  ///
  /// In en, this message translates to:
  /// **'Deposit slip reference'**
  String get depositSlipReference;

  /// No description provided for @theCashLeavesYourDrawer.
  ///
  /// In en, this message translates to:
  /// **'The cash leaves your drawer now. It stays outstanding until the recipient confirms the amount.'**
  String get theCashLeavesYourDrawer;

  /// No description provided for @enterTheCashYouCounted.
  ///
  /// In en, this message translates to:
  /// **'Enter the cash you counted.'**
  String get enterTheCashYouCounted;

  /// No description provided for @explainTheDifference.
  ///
  /// In en, this message translates to:
  /// **'Explain the difference.'**
  String get explainTheDifference;

  /// No description provided for @recountCash.
  ///
  /// In en, this message translates to:
  /// **'Recount cash'**
  String get recountCash;

  /// No description provided for @expectedCash.
  ///
  /// In en, this message translates to:
  /// **'Expected cash'**
  String get expectedCash;

  /// No description provided for @yourFirstCount.
  ///
  /// In en, this message translates to:
  /// **'Your first count'**
  String get yourFirstCount;

  /// No description provided for @countedCashNow.
  ///
  /// In en, this message translates to:
  /// **'Counted cash now'**
  String get countedCashNow;

  /// No description provided for @reasonForTheDifference.
  ///
  /// In en, this message translates to:
  /// **'Reason for the difference'**
  String get reasonForTheDifference;

  /// No description provided for @cashShift.
  ///
  /// In en, this message translates to:
  /// **'Cash Shift'**
  String get cashShift;

  /// No description provided for @noOpenCashShift.
  ///
  /// In en, this message translates to:
  /// **'No open cash shift'**
  String get noOpenCashShift;

  /// No description provided for @openAShiftWithYour.
  ///
  /// In en, this message translates to:
  /// **'Open a shift with your float before your first sale.'**
  String get openAShiftWithYour;

  /// No description provided for @previousShifts.
  ///
  /// In en, this message translates to:
  /// **'Previous shifts'**
  String get previousShifts;

  /// No description provided for @expectedInYourDrawer.
  ///
  /// In en, this message translates to:
  /// **'Expected in your drawer'**
  String get expectedInYourDrawer;

  /// No description provided for @opened.
  ///
  /// In en, this message translates to:
  /// **'Opened'**
  String get opened;

  /// No description provided for @openingFloat.
  ///
  /// In en, this message translates to:
  /// **'Opening float'**
  String get openingFloat;

  /// No description provided for @refundsPaid.
  ///
  /// In en, this message translates to:
  /// **'− Refunds paid'**
  String get refundsPaid;

  /// No description provided for @approvedOutflows.
  ///
  /// In en, this message translates to:
  /// **'− Approved outflows'**
  String get approvedOutflows;

  /// No description provided for @remitted.
  ///
  /// In en, this message translates to:
  /// **'− Remitted'**
  String get remitted;

  /// No description provided for @expectedCash2.
  ///
  /// In en, this message translates to:
  /// **'= Expected cash'**
  String get expectedCash2;

  /// No description provided for @requestOutflow.
  ///
  /// In en, this message translates to:
  /// **'Request outflow'**
  String get requestOutflow;

  /// No description provided for @closeShift.
  ///
  /// In en, this message translates to:
  /// **'Close shift'**
  String get closeShift;

  /// No description provided for @outflowRequests.
  ///
  /// In en, this message translates to:
  /// **'Outflow requests'**
  String get outflowRequests;

  /// No description provided for @cancelRequest.
  ///
  /// In en, this message translates to:
  /// **'Cancel request'**
  String get cancelRequest;

  /// No description provided for @remittances.
  ///
  /// In en, this message translates to:
  /// **'Remittances'**
  String get remittances;

  /// No description provided for @cashLedger.
  ///
  /// In en, this message translates to:
  /// **'Cash ledger'**
  String get cashLedger;

  /// No description provided for @awaitingApproval.
  ///
  /// In en, this message translates to:
  /// **'Awaiting approval'**
  String get awaitingApproval;

  /// No description provided for @approved.
  ///
  /// In en, this message translates to:
  /// **'Approved'**
  String get approved;

  /// No description provided for @rejected.
  ///
  /// In en, this message translates to:
  /// **'Rejected'**
  String get rejected;

  /// No description provided for @cancelled.
  ///
  /// In en, this message translates to:
  /// **'Cancelled'**
  String get cancelled;

  /// No description provided for @awaitingConfirmation.
  ///
  /// In en, this message translates to:
  /// **'Awaiting confirmation'**
  String get awaitingConfirmation;

  /// No description provided for @confirmed.
  ///
  /// In en, this message translates to:
  /// **'Confirmed'**
  String get confirmed;

  /// No description provided for @disputed.
  ///
  /// In en, this message translates to:
  /// **'Disputed'**
  String get disputed;

  /// No description provided for @managerReview.
  ///
  /// In en, this message translates to:
  /// **'Manager review'**
  String get managerReview;

  /// No description provided for @recountRequested.
  ///
  /// In en, this message translates to:
  /// **'Recount requested'**
  String get recountRequested;

  /// No description provided for @reviewed.
  ///
  /// In en, this message translates to:
  /// **'Reviewed'**
  String get reviewed;

  /// No description provided for @balanced.
  ///
  /// In en, this message translates to:
  /// **'Balanced'**
  String get balanced;

  /// No description provided for @openCashShift.
  ///
  /// In en, this message translates to:
  /// **'Open cash shift'**
  String get openCashShift;

  /// No description provided for @countTheCashInYour.
  ///
  /// In en, this message translates to:
  /// **'Count the cash in your drawer before your first sale.'**
  String get countTheCashInYour;

  /// No description provided for @explainTheDifferenceBeforeClosing.
  ///
  /// In en, this message translates to:
  /// **'Explain the difference before closing.'**
  String get explainTheDifferenceBeforeClosing;

  /// No description provided for @closeCashShift.
  ///
  /// In en, this message translates to:
  /// **'Close cash shift'**
  String get closeCashShift;

  /// No description provided for @countedCash.
  ///
  /// In en, this message translates to:
  /// **'Counted cash'**
  String get countedCash;

  /// No description provided for @coffeeBusinessOs.
  ///
  /// In en, this message translates to:
  /// **'Coffee Business OS'**
  String get coffeeBusinessOs;

  /// No description provided for @retry.
  ///
  /// In en, this message translates to:
  /// **'Retry'**
  String get retry;

  /// No description provided for @addToCart.
  ///
  /// In en, this message translates to:
  /// **'Add to cart'**
  String get addToCart;

  /// No description provided for @inStock.
  ///
  /// In en, this message translates to:
  /// **'In stock'**
  String get inStock;

  /// No description provided for @outOfStock.
  ///
  /// In en, this message translates to:
  /// **'Out of stock'**
  String get outOfStock;

  /// No description provided for @statusActive.
  ///
  /// In en, this message translates to:
  /// **'Active'**
  String get statusActive;

  /// No description provided for @statusCompleted.
  ///
  /// In en, this message translates to:
  /// **'Completed'**
  String get statusCompleted;

  /// No description provided for @appTitle.
  ///
  /// In en, this message translates to:
  /// **'LUTIANO Seller'**
  String get appTitle;

  /// No description provided for @sending.
  ///
  /// In en, this message translates to:
  /// **'Sending…'**
  String get sending;

  /// No description provided for @sendCode.
  ///
  /// In en, this message translates to:
  /// **'Send Code'**
  String get sendCode;

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

  /// No description provided for @saving.
  ///
  /// In en, this message translates to:
  /// **'Saving…'**
  String get saving;

  /// No description provided for @setNewPassword.
  ///
  /// In en, this message translates to:
  /// **'Set New Password'**
  String get setNewPassword;

  /// No description provided for @enterYourEmailShort.
  ///
  /// In en, this message translates to:
  /// **'Enter your email'**
  String get enterYourEmailShort;

  /// No description provided for @enterYourPassword.
  ///
  /// In en, this message translates to:
  /// **'Enter your password'**
  String get enterYourPassword;

  /// No description provided for @signingIn.
  ///
  /// In en, this message translates to:
  /// **'Signing in…'**
  String get signingIn;

  /// No description provided for @signIn.
  ///
  /// In en, this message translates to:
  /// **'Sign In'**
  String get signIn;

  /// No description provided for @products.
  ///
  /// In en, this message translates to:
  /// **'Products'**
  String get products;

  /// No description provided for @myOperation.
  ///
  /// In en, this message translates to:
  /// **'My Operation'**
  String get myOperation;

  /// No description provided for @noAlerts.
  ///
  /// In en, this message translates to:
  /// **'No alerts'**
  String get noAlerts;

  /// No description provided for @cashShiftClosed.
  ///
  /// In en, this message translates to:
  /// **'Cash shift closed'**
  String get cashShiftClosed;

  /// No description provided for @openAShiftToStartSelling.
  ///
  /// In en, this message translates to:
  /// **'Open a shift to start selling'**
  String get openAShiftToStartSelling;

  /// No description provided for @open.
  ///
  /// In en, this message translates to:
  /// **'Open'**
  String get open;

  /// No description provided for @close.
  ///
  /// In en, this message translates to:
  /// **'Close'**
  String get close;

  /// No description provided for @receiptConfirmedDifference.
  ///
  /// In en, this message translates to:
  /// **'Receipt confirmed. The difference has been reported.'**
  String get receiptConfirmedDifference;

  /// No description provided for @confirm.
  ///
  /// In en, this message translates to:
  /// **'Confirm'**
  String get confirm;

  /// No description provided for @openingShift.
  ///
  /// In en, this message translates to:
  /// **'Opening…'**
  String get openingShift;

  /// No description provided for @closing.
  ///
  /// In en, this message translates to:
  /// **'Closing…'**
  String get closing;

  /// No description provided for @enterYourCurrentPassword.
  ///
  /// In en, this message translates to:
  /// **'Enter your current password'**
  String get enterYourCurrentPassword;

  /// No description provided for @useAtLeast10.
  ///
  /// In en, this message translates to:
  /// **'Use at least 10 characters'**
  String get useAtLeast10;

  /// No description provided for @use72OrFewer.
  ///
  /// In en, this message translates to:
  /// **'Use 72 characters or fewer'**
  String get use72OrFewer;

  /// No description provided for @passwordsDoNotMatch.
  ///
  /// In en, this message translates to:
  /// **'Passwords do not match'**
  String get passwordsDoNotMatch;

  /// No description provided for @changePasswordButton.
  ///
  /// In en, this message translates to:
  /// **'Change Password'**
  String get changePasswordButton;

  /// No description provided for @seller.
  ///
  /// In en, this message translates to:
  /// **'Seller'**
  String get seller;

  /// No description provided for @notAssigned.
  ///
  /// In en, this message translates to:
  /// **'Not assigned'**
  String get notAssigned;

  /// No description provided for @notSet.
  ///
  /// In en, this message translates to:
  /// **'Not set'**
  String get notSet;

  /// No description provided for @cartWillBeCleared.
  ///
  /// In en, this message translates to:
  /// **'Your current cart will be cleared.'**
  String get cartWillBeCleared;

  /// No description provided for @needToSignInAgain.
  ///
  /// In en, this message translates to:
  /// **'You will need to sign in again to record sales.'**
  String get needToSignInAgain;

  /// No description provided for @permRecordCashSales.
  ///
  /// In en, this message translates to:
  /// **'Record cash sales at my assigned operation'**
  String get permRecordCashSales;

  /// No description provided for @permOpenCloseShift.
  ///
  /// In en, this message translates to:
  /// **'Open and close my cash shift'**
  String get permOpenCloseShift;

  /// No description provided for @permViewInventory.
  ///
  /// In en, this message translates to:
  /// **'View assigned inventory and stock movements'**
  String get permViewInventory;

  /// No description provided for @permConfirmDeliveries.
  ///
  /// In en, this message translates to:
  /// **'Confirm stock deliveries and report differences'**
  String get permConfirmDeliveries;

  /// No description provided for @permShowQr.
  ///
  /// In en, this message translates to:
  /// **'Show my operation QR code'**
  String get permShowQr;

  /// No description provided for @permViewOwnSales.
  ///
  /// In en, this message translates to:
  /// **'View my own sales history and receipts'**
  String get permViewOwnSales;

  /// No description provided for @permChangePrices.
  ///
  /// In en, this message translates to:
  /// **'Change prices or apply discounts'**
  String get permChangePrices;

  /// No description provided for @permApproveRefunds.
  ///
  /// In en, this message translates to:
  /// **'Approve refunds or stock adjustments'**
  String get permApproveRefunds;

  /// No description provided for @permManageProducts.
  ///
  /// In en, this message translates to:
  /// **'Manage products, users or operations'**
  String get permManageProducts;

  /// No description provided for @roleDescMobile.
  ///
  /// In en, this message translates to:
  /// **'Sells LUTIANO products at mobile locations (events, offices, pop-up points) and manages the stock and cash allocated to the mobile operation.'**
  String get roleDescMobile;

  /// No description provided for @roleDescMarket.
  ///
  /// In en, this message translates to:
  /// **'Sells LUTIANO products directly in markets and the field, and manages the stock and cash allocated to the agent operation.'**
  String get roleDescMarket;

  /// No description provided for @roleDescFixed.
  ///
  /// In en, this message translates to:
  /// **'Sells LUTIANO products at a fixed point of sale and manages its stock and cash drawer.'**
  String get roleDescFixed;

  /// No description provided for @recordingSale.
  ///
  /// In en, this message translates to:
  /// **'Recording sale…'**
  String get recordingSale;

  /// No description provided for @confirmPayment.
  ///
  /// In en, this message translates to:
  /// **'Confirm Payment'**
  String get confirmPayment;

  /// No description provided for @checking.
  ///
  /// In en, this message translates to:
  /// **'Checking…'**
  String get checking;

  /// No description provided for @request.
  ///
  /// In en, this message translates to:
  /// **'Request'**
  String get request;

  /// No description provided for @recording.
  ///
  /// In en, this message translates to:
  /// **'Recording…'**
  String get recording;

  /// No description provided for @record.
  ///
  /// In en, this message translates to:
  /// **'Record'**
  String get record;

  /// No description provided for @submitRecount.
  ///
  /// In en, this message translates to:
  /// **'Submit recount'**
  String get submitRecount;

  /// No description provided for @recountSubmitted.
  ///
  /// In en, this message translates to:
  /// **'Recount submitted.'**
  String get recountSubmitted;

  /// No description provided for @shiftOpened.
  ///
  /// In en, this message translates to:
  /// **'Shift opened.'**
  String get shiftOpened;

  /// No description provided for @outflowRequested.
  ///
  /// In en, this message translates to:
  /// **'Outflow requested. Wait for approval before taking the cash.'**
  String get outflowRequested;

  /// No description provided for @remittanceRecorded.
  ///
  /// In en, this message translates to:
  /// **'Remittance recorded.'**
  String get remittanceRecorded;

  /// No description provided for @notCounted.
  ///
  /// In en, this message translates to:
  /// **'Not counted'**
  String get notCounted;

  /// No description provided for @shiftClosedMatches.
  ///
  /// In en, this message translates to:
  /// **'Shift closed. Cash matches.'**
  String get shiftClosedMatches;

  /// No description provided for @salesTrend7Days2.
  ///
  /// In en, this message translates to:
  /// **'Sales Trend (7 days)'**
  String get salesTrend7Days2;

  /// No description provided for @decreaseQuantity.
  ///
  /// In en, this message translates to:
  /// **'Decrease quantity'**
  String get decreaseQuantity;

  /// No description provided for @increaseQuantity.
  ///
  /// In en, this message translates to:
  /// **'Increase quantity'**
  String get increaseQuantity;

  /// No description provided for @periodToday.
  ///
  /// In en, this message translates to:
  /// **'Today'**
  String get periodToday;

  /// No description provided for @periodWeek.
  ///
  /// In en, this message translates to:
  /// **'This Week'**
  String get periodWeek;

  /// No description provided for @periodMonth.
  ///
  /// In en, this message translates to:
  /// **'This Month'**
  String get periodMonth;

  /// No description provided for @periodCustom.
  ///
  /// In en, this message translates to:
  /// **'Custom'**
  String get periodCustom;

  /// No description provided for @pointsEarnedValue.
  ///
  /// In en, this message translates to:
  /// **'+{points} pts'**
  String pointsEarnedValue(int points);

  /// No description provided for @itemsCount.
  ///
  /// In en, this message translates to:
  /// **'Items ({count})'**
  String itemsCount(int count);

  /// No description provided for @saleSubtitle.
  ///
  /// In en, this message translates to:
  /// **'{time} · Cash · {operation}'**
  String saleSubtitle(String time, String operation);

  /// No description provided for @pointsValue.
  ///
  /// In en, this message translates to:
  /// **'{points} pts'**
  String pointsValue(String points);

  /// No description provided for @deliveriesToConfirm.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{1 delivery to confirm} other{{count} deliveries to confirm}}'**
  String deliveriesToConfirm(int count);

  /// No description provided for @shiftOpenSince.
  ///
  /// In en, this message translates to:
  /// **'Cash shift open since {time}'**
  String shiftOpenSince(String time);

  /// No description provided for @expectedCashValue.
  ///
  /// In en, this message translates to:
  /// **'Expected cash: {amount}'**
  String expectedCashValue(String amount);

  /// No description provided for @unitsSentAt.
  ///
  /// In en, this message translates to:
  /// **'{count} units · sent {when}'**
  String unitsSentAt(int count, String when);

  /// No description provided for @enterQuantityBetween.
  ///
  /// In en, this message translates to:
  /// **'Enter a quantity between 0 and {max}.'**
  String enterQuantityBetween(int max);

  /// No description provided for @receiptConfirmedUnits.
  ///
  /// In en, this message translates to:
  /// **'Receipt confirmed. {count} units added to your stock.'**
  String receiptConfirmedUnits(int count);

  /// No description provided for @confirmProduct.
  ///
  /// In en, this message translates to:
  /// **'Confirm {product}'**
  String confirmProduct(String product);

  /// No description provided for @unitsDispatched.
  ///
  /// In en, this message translates to:
  /// **'{count} units were dispatched.'**
  String unitsDispatched(int count);

  /// No description provided for @productsNeedAttention.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{1 product needs attention} other{{count} products need attention}}'**
  String productsNeedAttention(int count);

  /// No description provided for @stockAtOrBelow.
  ///
  /// In en, this message translates to:
  /// **'Stock is at or below {level}. Ask your inventory manager for a replenishment.'**
  String stockAtOrBelow(int level);

  /// No description provided for @minLevelSuffix.
  ///
  /// In en, this message translates to:
  /// **' · min {min}'**
  String minLevelSuffix(int min);

  /// No description provided for @operationsAssigned.
  ///
  /// In en, this message translates to:
  /// **'{count} operations assigned to you'**
  String operationsAssigned(int count);

  /// No description provided for @assignedSellers.
  ///
  /// In en, this message translates to:
  /// **'Assigned sellers ({count})'**
  String assignedSellers(int count);

  /// No description provided for @qrCodeFor.
  ///
  /// In en, this message translates to:
  /// **'QR code for {name}'**
  String qrCodeFor(String name);

  /// No description provided for @versionValue.
  ///
  /// In en, this message translates to:
  /// **'Version {version}'**
  String versionValue(String version);

  /// No description provided for @cashReceivedAtLeast.
  ///
  /// In en, this message translates to:
  /// **'Cash received must be at least {amount}.'**
  String cashReceivedAtLeast(String amount);

  /// No description provided for @messageCartSavedDraft.
  ///
  /// In en, this message translates to:
  /// **'{message} Your cart is saved as a draft.'**
  String messageCartSavedDraft(String message);

  /// No description provided for @availableCount.
  ///
  /// In en, this message translates to:
  /// **'{count} available'**
  String availableCount(int count);

  /// No description provided for @drawerShouldHold.
  ///
  /// In en, this message translates to:
  /// **'Your drawer should hold only {amount}.'**
  String drawerShouldHold(String amount);

  /// No description provided for @inYourDrawer.
  ///
  /// In en, this message translates to:
  /// **'In your drawer: {amount}'**
  String inYourDrawer(String amount);

  /// No description provided for @managerNote.
  ///
  /// In en, this message translates to:
  /// **'Manager: {note}'**
  String managerNote(String note);

  /// No description provided for @recountAsked.
  ///
  /// In en, this message translates to:
  /// **'A manager asked you to recount the shift of {date}.'**
  String recountAsked(String date);

  /// No description provided for @cashSalesCount.
  ///
  /// In en, this message translates to:
  /// **'+ Cash sales ({count})'**
  String cashSalesCount(int count);

  /// No description provided for @receivedAmount.
  ///
  /// In en, this message translates to:
  /// **'Received {amount}'**
  String receivedAmount(String amount);

  /// No description provided for @shiftExpected.
  ///
  /// In en, this message translates to:
  /// **'{date} · {amount} expected'**
  String shiftExpected(String date, String amount);

  /// No description provided for @shiftClosedVariance.
  ///
  /// In en, this message translates to:
  /// **'Shift closed with a variance of {amount}. A manager will review it.'**
  String shiftClosedVariance(String amount);

  /// No description provided for @typeFixedPos.
  ///
  /// In en, this message translates to:
  /// **'Fixed POS'**
  String get typeFixedPos;

  /// No description provided for @typeMobile.
  ///
  /// In en, this message translates to:
  /// **'Mobile Operation'**
  String get typeMobile;

  /// No description provided for @typeMarket.
  ///
  /// In en, this message translates to:
  /// **'Market Agent'**
  String get typeMarket;

  /// No description provided for @roleFixedPos.
  ///
  /// In en, this message translates to:
  /// **'Fixed POS Barista'**
  String get roleFixedPos;

  /// No description provided for @roleMobile.
  ///
  /// In en, this message translates to:
  /// **'Mobile Barista'**
  String get roleMobile;

  /// No description provided for @roleMarket.
  ///
  /// In en, this message translates to:
  /// **'Market Sales Agent'**
  String get roleMarket;

  /// No description provided for @ledgerOpeningFloat.
  ///
  /// In en, this message translates to:
  /// **'Opening float'**
  String get ledgerOpeningFloat;

  /// No description provided for @ledgerCashSale.
  ///
  /// In en, this message translates to:
  /// **'Cash sale'**
  String get ledgerCashSale;

  /// No description provided for @ledgerRefundPaid.
  ///
  /// In en, this message translates to:
  /// **'Refund paid'**
  String get ledgerRefundPaid;

  /// No description provided for @ledgerOutflow.
  ///
  /// In en, this message translates to:
  /// **'Approved outflow'**
  String get ledgerOutflow;

  /// No description provided for @ledgerRemittance.
  ///
  /// In en, this message translates to:
  /// **'Remittance'**
  String get ledgerRemittance;

  /// No description provided for @catSupplies.
  ///
  /// In en, this message translates to:
  /// **'Supplies'**
  String get catSupplies;

  /// No description provided for @catTransport.
  ///
  /// In en, this message translates to:
  /// **'Transport'**
  String get catTransport;

  /// No description provided for @catMaintenance.
  ///
  /// In en, this message translates to:
  /// **'Maintenance'**
  String get catMaintenance;

  /// No description provided for @catCleaning.
  ///
  /// In en, this message translates to:
  /// **'Cleaning'**
  String get catCleaning;

  /// No description provided for @catOther.
  ///
  /// In en, this message translates to:
  /// **'Other'**
  String get catOther;

  /// No description provided for @greetingMorning.
  ///
  /// In en, this message translates to:
  /// **'Good morning,'**
  String get greetingMorning;

  /// No description provided for @greetingAfternoon.
  ///
  /// In en, this message translates to:
  /// **'Good afternoon,'**
  String get greetingAfternoon;

  /// No description provided for @greetingEvening.
  ///
  /// In en, this message translates to:
  /// **'Good evening,'**
  String get greetingEvening;

  /// No description provided for @cash.
  ///
  /// In en, this message translates to:
  /// **'Cash'**
  String get cash;

  /// No description provided for @mtnMomo.
  ///
  /// In en, this message translates to:
  /// **'MTN MoMo'**
  String get mtnMomo;

  /// No description provided for @orangeMoney.
  ///
  /// In en, this message translates to:
  /// **'Orange Money'**
  String get orangeMoney;

  /// No description provided for @productsRunningLow.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{1 product is running low} other{{count} products are running low}}'**
  String productsRunningLow(int count);

  /// No description provided for @stockInTitle.
  ///
  /// In en, this message translates to:
  /// **'Stock In · {product}'**
  String stockInTitle(String product);

  /// No description provided for @stockOutTitle.
  ///
  /// In en, this message translates to:
  /// **'Stock Out · {product}'**
  String stockOutTitle(String product);

  /// No description provided for @saleReferenceValue.
  ///
  /// In en, this message translates to:
  /// **'Sale {reference}'**
  String saleReferenceValue(String reference);

  /// No description provided for @varianceValue.
  ///
  /// In en, this message translates to:
  /// **'Variance: {amount}'**
  String varianceValue(String amount);

  /// No description provided for @countedVariance.
  ///
  /// In en, this message translates to:
  /// **'Counted {counted} · variance {variance}'**
  String countedVariance(String counted, String variance);

  /// No description provided for @bankDepositRef.
  ///
  /// In en, this message translates to:
  /// **'Bank deposit · {reference}'**
  String bankDepositRef(String reference);

  /// No description provided for @handedToName.
  ///
  /// In en, this message translates to:
  /// **'Handed to {name}'**
  String handedToName(String name);

  /// No description provided for @manager.
  ///
  /// In en, this message translates to:
  /// **'manager'**
  String get manager;

  /// No description provided for @errNetwork.
  ///
  /// In en, this message translates to:
  /// **'Cannot reach the LUTIANO server. Check your connection and try again.'**
  String get errNetwork;

  /// No description provided for @errSessionExpired.
  ///
  /// In en, this message translates to:
  /// **'Your session has expired. Sign in again.'**
  String get errSessionExpired;

  /// No description provided for @errForbidden.
  ///
  /// In en, this message translates to:
  /// **'You do not have permission to do this.'**
  String get errForbidden;

  /// No description provided for @errGeneric.
  ///
  /// In en, this message translates to:
  /// **'Something went wrong ({status}). Try again.'**
  String errGeneric(int status);

  /// No description provided for @errUnexpected.
  ///
  /// In en, this message translates to:
  /// **'Something went wrong. Try again.'**
  String get errUnexpected;

  /// No description provided for @loading.
  ///
  /// In en, this message translates to:
  /// **'Loading'**
  String get loading;

  /// No description provided for @loadingMore.
  ///
  /// In en, this message translates to:
  /// **'Loading more…'**
  String get loadingMore;

  /// No description provided for @viewAllShifts.
  ///
  /// In en, this message translates to:
  /// **'All shifts'**
  String get viewAllShifts;

  /// No description provided for @viewCashLedger.
  ///
  /// In en, this message translates to:
  /// **'View cash ledger'**
  String get viewCashLedger;

  /// No description provided for @noShiftsYet.
  ///
  /// In en, this message translates to:
  /// **'No shifts yet'**
  String get noShiftsYet;

  /// No description provided for @noLedgerEntries.
  ///
  /// In en, this message translates to:
  /// **'No cash movements yet'**
  String get noLedgerEntries;

  /// No description provided for @preparingReceipt.
  ///
  /// In en, this message translates to:
  /// **'Preparing the receipt…'**
  String get preparingReceipt;

  /// No description provided for @totalReceived.
  ///
  /// In en, this message translates to:
  /// **'Total received'**
  String get totalReceived;

  /// No description provided for @consumedToday.
  ///
  /// In en, this message translates to:
  /// **'Consumed today'**
  String get consumedToday;

  /// No description provided for @consumptionDetail.
  ///
  /// In en, this message translates to:
  /// **'Consumption detail'**
  String get consumptionDetail;

  /// No description provided for @linkedToSale.
  ///
  /// In en, this message translates to:
  /// **'Linked to sale'**
  String get linkedToSale;

  /// No description provided for @dateAndTime.
  ///
  /// In en, this message translates to:
  /// **'Date & time'**
  String get dateAndTime;

  /// No description provided for @productLabel.
  ///
  /// In en, this message translates to:
  /// **'Product'**
  String get productLabel;

  /// No description provided for @quantityConsumed.
  ///
  /// In en, this message translates to:
  /// **'Quantity consumed'**
  String get quantityConsumed;

  /// No description provided for @remainingStock.
  ///
  /// In en, this message translates to:
  /// **'Remaining stock'**
  String get remainingStock;

  /// No description provided for @viewSaleDetails.
  ///
  /// In en, this message translates to:
  /// **'View sale details'**
  String get viewSaleDetails;

  /// No description provided for @inventorySummary.
  ///
  /// In en, this message translates to:
  /// **'Inventory summary'**
  String get inventorySummary;

  /// No description provided for @stockInReceived.
  ///
  /// In en, this message translates to:
  /// **'Stock in (received)'**
  String get stockInReceived;

  /// No description provided for @stockOutConsumed.
  ///
  /// In en, this message translates to:
  /// **'Stock out (consumed)'**
  String get stockOutConsumed;

  /// No description provided for @netMovement.
  ///
  /// In en, this message translates to:
  /// **'Net movement'**
  String get netMovement;

  /// No description provided for @topConsumedProducts.
  ///
  /// In en, this message translates to:
  /// **'Top consumed products'**
  String get topConsumedProducts;

  /// No description provided for @nothingConsumedInPeriod.
  ///
  /// In en, this message translates to:
  /// **'Nothing was consumed in this period'**
  String get nothingConsumedInPeriod;

  /// No description provided for @receivedConsumedAndTop.
  ///
  /// In en, this message translates to:
  /// **'Received, consumed and top products'**
  String get receivedConsumedAndTop;

  /// No description provided for @stockRequests.
  ///
  /// In en, this message translates to:
  /// **'Stock Requests'**
  String get stockRequests;

  /// No description provided for @stockRequestsSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Request and track stock replenishment'**
  String get stockRequestsSubtitle;

  /// No description provided for @totalRequests.
  ///
  /// In en, this message translates to:
  /// **'Total requests'**
  String get totalRequests;

  /// No description provided for @requestsApproved.
  ///
  /// In en, this message translates to:
  /// **'Approved'**
  String get requestsApproved;

  /// No description provided for @requestsPending.
  ///
  /// In en, this message translates to:
  /// **'Pending'**
  String get requestsPending;

  /// No description provided for @requestsRejected.
  ///
  /// In en, this message translates to:
  /// **'Rejected'**
  String get requestsRejected;

  /// No description provided for @requestsInFulfillment.
  ///
  /// In en, this message translates to:
  /// **'In fulfillment'**
  String get requestsInFulfillment;

  /// No description provided for @requestsCompleted.
  ///
  /// In en, this message translates to:
  /// **'Completed'**
  String get requestsCompleted;

  /// No description provided for @requestsCancelled.
  ///
  /// In en, this message translates to:
  /// **'Cancelled'**
  String get requestsCancelled;

  /// No description provided for @requestStock.
  ///
  /// In en, this message translates to:
  /// **'Request Stock'**
  String get requestStock;

  /// No description provided for @myRequests.
  ///
  /// In en, this message translates to:
  /// **'My Requests'**
  String get myRequests;

  /// No description provided for @pendingApprovals.
  ///
  /// In en, this message translates to:
  /// **'Pending Approvals'**
  String get pendingApprovals;

  /// No description provided for @searchByReference.
  ///
  /// In en, this message translates to:
  /// **'Search by reference…'**
  String get searchByReference;

  /// No description provided for @noRequestsYet.
  ///
  /// In en, this message translates to:
  /// **'No stock requests yet'**
  String get noRequestsYet;

  /// No description provided for @noRequestsMatch.
  ///
  /// In en, this message translates to:
  /// **'No requests match'**
  String get noRequestsMatch;

  /// No description provided for @requestsWillAppearHere.
  ///
  /// In en, this message translates to:
  /// **'Requests you send will appear here.'**
  String get requestsWillAppearHere;

  /// No description provided for @requestDetails.
  ///
  /// In en, this message translates to:
  /// **'Request Details'**
  String get requestDetails;

  /// No description provided for @requestInformation.
  ///
  /// In en, this message translates to:
  /// **'Request information'**
  String get requestInformation;

  /// No description provided for @requestedDelivery.
  ///
  /// In en, this message translates to:
  /// **'Requested delivery'**
  String get requestedDelivery;

  /// No description provided for @asSoonAsPossible.
  ///
  /// In en, this message translates to:
  /// **'As soon as possible'**
  String get asSoonAsPossible;

  /// No description provided for @requestReason.
  ///
  /// In en, this message translates to:
  /// **'Reason'**
  String get requestReason;

  /// No description provided for @totalQuantity.
  ///
  /// In en, this message translates to:
  /// **'Total quantity'**
  String get totalQuantity;

  /// No description provided for @awaitingReview.
  ///
  /// In en, this message translates to:
  /// **'Your request is awaiting review by the operations team.'**
  String get awaitingReview;

  /// No description provided for @stockBeingPrepared.
  ///
  /// In en, this message translates to:
  /// **'Stock is being prepared for allocation.'**
  String get stockBeingPrepared;

  /// No description provided for @duplicateAndModify.
  ///
  /// In en, this message translates to:
  /// **'Duplicate & Modify'**
  String get duplicateAndModify;

  /// No description provided for @fulfillmentProgress.
  ///
  /// In en, this message translates to:
  /// **'Fulfillment progress'**
  String get fulfillmentProgress;

  /// No description provided for @stepApproved.
  ///
  /// In en, this message translates to:
  /// **'Approved'**
  String get stepApproved;

  /// No description provided for @stepOnItsWay.
  ///
  /// In en, this message translates to:
  /// **'On its way'**
  String get stepOnItsWay;

  /// No description provided for @stepAllocated.
  ///
  /// In en, this message translates to:
  /// **'Allocated'**
  String get stepAllocated;

  /// No description provided for @confirmDeliveriesNote.
  ///
  /// In en, this message translates to:
  /// **'Confirm each delivery in Receive Stock when it arrives.'**
  String get confirmDeliveriesNote;

  /// No description provided for @selectProductsToRequest.
  ///
  /// In en, this message translates to:
  /// **'Select products to request'**
  String get selectProductsToRequest;

  /// No description provided for @next.
  ///
  /// In en, this message translates to:
  /// **'Next'**
  String get next;

  /// No description provided for @requestSummary.
  ///
  /// In en, this message translates to:
  /// **'Request Summary'**
  String get requestSummary;

  /// No description provided for @reviewAndSubmit.
  ///
  /// In en, this message translates to:
  /// **'Review and submit your request'**
  String get reviewAndSubmit;

  /// No description provided for @submitRequest.
  ///
  /// In en, this message translates to:
  /// **'Submit Request'**
  String get submitRequest;

  /// No description provided for @sendingRequest.
  ///
  /// In en, this message translates to:
  /// **'Sending…'**
  String get sendingRequest;

  /// No description provided for @reasonOptional.
  ///
  /// In en, this message translates to:
  /// **'Reason (optional)'**
  String get reasonOptional;

  /// No description provided for @chooseDate.
  ///
  /// In en, this message translates to:
  /// **'Choose a date'**
  String get chooseDate;

  /// No description provided for @startFromPrevious.
  ///
  /// In en, this message translates to:
  /// **'Start from a previous request'**
  String get startFromPrevious;

  /// No description provided for @previousRequests.
  ///
  /// In en, this message translates to:
  /// **'Previous requests'**
  String get previousRequests;

  /// No description provided for @newStockRequest.
  ///
  /// In en, this message translates to:
  /// **'New Stock Request'**
  String get newStockRequest;

  /// No description provided for @pickAtLeastOneProduct.
  ///
  /// In en, this message translates to:
  /// **'Choose at least one product.'**
  String get pickAtLeastOneProduct;

  /// No description provided for @cannotRequestStock.
  ///
  /// In en, this message translates to:
  /// **'Your role cannot request stock. Ask your inventory manager.'**
  String get cannotRequestStock;

  /// No description provided for @noProductsToRequest.
  ///
  /// In en, this message translates to:
  /// **'No products to show'**
  String get noProductsToRequest;

  /// No description provided for @requestedItems.
  ///
  /// In en, this message translates to:
  /// **'Requested items ({count})'**
  String requestedItems(int count);

  /// No description provided for @productsSelected.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{1 product selected} other{{count} products selected}}'**
  String productsSelected(int count);

  /// No description provided for @totalQuantityValue.
  ///
  /// In en, this message translates to:
  /// **'Total quantity: {count}'**
  String totalQuantityValue(int count);

  /// No description provided for @requestItemsLine.
  ///
  /// In en, this message translates to:
  /// **'{when} · {count, plural, =1{1 item} other{{count} items}}'**
  String requestItemsLine(String when, int count);

  /// No description provided for @requestedOnWhen.
  ///
  /// In en, this message translates to:
  /// **'Requested on {when}'**
  String requestedOnWhen(String when);

  /// No description provided for @approvedOnWhen.
  ///
  /// In en, this message translates to:
  /// **'Approved on {when}'**
  String approvedOnWhen(String when);

  /// No description provided for @rejectedOnWhen.
  ///
  /// In en, this message translates to:
  /// **'Rejected on {when}'**
  String rejectedOnWhen(String when);

  /// No description provided for @cancelledOnWhen.
  ///
  /// In en, this message translates to:
  /// **'Cancelled on {when}'**
  String cancelledOnWhen(String when);

  /// No description provided for @completedOnWhen.
  ///
  /// In en, this message translates to:
  /// **'Completed on {when}'**
  String completedOnWhen(String when);

  /// No description provided for @requestReasonValue.
  ///
  /// In en, this message translates to:
  /// **'Reason: {reason}'**
  String requestReasonValue(String reason);

  /// No description provided for @requestSubmitted.
  ///
  /// In en, this message translates to:
  /// **'Request {reference} submitted'**
  String requestSubmitted(String reference);

  /// No description provided for @stockNow.
  ///
  /// In en, this message translates to:
  /// **'Stock: {count}'**
  String stockNow(int count);

  /// No description provided for @receivedOfSent.
  ///
  /// In en, this message translates to:
  /// **'{received} of {sent} received'**
  String receivedOfSent(int received, int sent);

  /// No description provided for @needsAttention.
  ///
  /// In en, this message translates to:
  /// **'Needs attention'**
  String get needsAttention;

  /// No description provided for @allOutflowRequests.
  ///
  /// In en, this message translates to:
  /// **'All outflow requests'**
  String get allOutflowRequests;

  /// No description provided for @allRemittances.
  ///
  /// In en, this message translates to:
  /// **'All remittances'**
  String get allRemittances;

  /// No description provided for @noOutflowsYet.
  ///
  /// In en, this message translates to:
  /// **'No outflow requests yet'**
  String get noOutflowsYet;

  /// No description provided for @noRemittancesYet.
  ///
  /// In en, this message translates to:
  /// **'No remittances yet'**
  String get noRemittancesYet;

  /// No description provided for @shiftDetails.
  ///
  /// In en, this message translates to:
  /// **'Shift details'**
  String get shiftDetails;

  /// No description provided for @reconciliation.
  ///
  /// In en, this message translates to:
  /// **'Reconciliation'**
  String get reconciliation;

  /// No description provided for @variance.
  ///
  /// In en, this message translates to:
  /// **'Variance'**
  String get variance;

  /// No description provided for @countHistory.
  ///
  /// In en, this message translates to:
  /// **'Count history'**
  String get countHistory;

  /// No description provided for @closingCount.
  ///
  /// In en, this message translates to:
  /// **'Closing count'**
  String get closingCount;

  /// No description provided for @recountLabel.
  ///
  /// In en, this message translates to:
  /// **'Recount'**
  String get recountLabel;

  /// No description provided for @managerReviewTitle.
  ///
  /// In en, this message translates to:
  /// **'Manager review'**
  String get managerReviewTitle;

  /// No description provided for @cashMovements.
  ///
  /// In en, this message translates to:
  /// **'Cash movements'**
  String get cashMovements;

  /// No description provided for @shiftNotReviewed.
  ///
  /// In en, this message translates to:
  /// **'No review needed: the count matched.'**
  String get shiftNotReviewed;

  /// No description provided for @startRecount.
  ///
  /// In en, this message translates to:
  /// **'Count again'**
  String get startRecount;

  /// No description provided for @outflowsAwaiting.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{1 outflow request awaiting approval} other{{count} outflow requests awaiting approval}}'**
  String outflowsAwaiting(int count);

  /// No description provided for @remittancesAwaiting.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{1 remittance awaiting confirmation} other{{count} remittances awaiting confirmation}} · {amount}'**
  String remittancesAwaiting(int count, String amount);

  /// No description provided for @remittancesDisputed.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{1 remittance disputed} other{{count} remittances disputed}}'**
  String remittancesDisputed(int count);

  /// No description provided for @recountsRequested.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{1 recount requested} other{{count} recounts requested}}'**
  String recountsRequested(int count);

  /// No description provided for @closeRemindOutflows.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{1 outflow request is still waiting and will be withdrawn when you close.} other{{count} outflow requests are still waiting and will be withdrawn when you close.}}'**
  String closeRemindOutflows(int count);

  /// No description provided for @closeRemindRemittances.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{1 remittance} other{{count} remittances}} ({amount}) not yet confirmed by the recipient. Follow up before you leave.'**
  String closeRemindRemittances(int count, String amount);

  /// No description provided for @shiftOpenedEarlier.
  ///
  /// In en, this message translates to:
  /// **'This shift has been open since {when}. Count the drawer and close it.'**
  String shiftOpenedEarlier(String when);

  /// No description provided for @reviewedByWhen.
  ///
  /// In en, this message translates to:
  /// **'Reviewed by {name} on {when}'**
  String reviewedByWhen(String name, String when);

  /// No description provided for @countedByWhen.
  ///
  /// In en, this message translates to:
  /// **'{name} · {when}'**
  String countedByWhen(String name, String when);

  /// No description provided for @expectedCountedValue.
  ///
  /// In en, this message translates to:
  /// **'Expected {expected} · counted {counted}'**
  String expectedCountedValue(String expected, String counted);

  /// No description provided for @decidedByNote.
  ///
  /// In en, this message translates to:
  /// **'{name}: {note}'**
  String decidedByNote(String name, String note);

  /// No description provided for @earnings.
  ///
  /// In en, this message translates to:
  /// **'Earnings'**
  String get earnings;

  /// No description provided for @earningsSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Commissions, bonuses and payouts'**
  String get earningsSubtitle;

  /// No description provided for @totalEarnings.
  ///
  /// In en, this message translates to:
  /// **'Total earnings'**
  String get totalEarnings;

  /// No description provided for @commissionsLabel.
  ///
  /// In en, this message translates to:
  /// **'Commissions'**
  String get commissionsLabel;

  /// No description provided for @bonusesLabel.
  ///
  /// In en, this message translates to:
  /// **'Bonuses'**
  String get bonusesLabel;

  /// No description provided for @adjustmentsLabel.
  ///
  /// In en, this message translates to:
  /// **'Adjustments'**
  String get adjustmentsLabel;

  /// No description provided for @provisionalChip.
  ///
  /// In en, this message translates to:
  /// **'Provisional'**
  String get provisionalChip;

  /// No description provided for @finalChip.
  ///
  /// In en, this message translates to:
  /// **'Final'**
  String get finalChip;

  /// No description provided for @provisionalNote.
  ///
  /// In en, this message translates to:
  /// **'Provisional: amounts become final when the admin team closes the month.'**
  String get provisionalNote;

  /// No description provided for @stillToBePaid.
  ///
  /// In en, this message translates to:
  /// **'Still to be paid out'**
  String get stillToBePaid;

  /// No description provided for @viewBreakdown.
  ///
  /// In en, this message translates to:
  /// **'View Breakdown'**
  String get viewBreakdown;

  /// No description provided for @earningsByPeriod.
  ///
  /// In en, this message translates to:
  /// **'Earnings by Period'**
  String get earningsByPeriod;

  /// No description provided for @salesThatEarned.
  ///
  /// In en, this message translates to:
  /// **'Sales that earned commission'**
  String get salesThatEarned;

  /// No description provided for @earningsByOperation.
  ///
  /// In en, this message translates to:
  /// **'Earnings by Operation'**
  String get earningsByOperation;

  /// No description provided for @trendsAndAnalytics.
  ///
  /// In en, this message translates to:
  /// **'Trends & Analytics'**
  String get trendsAndAnalytics;

  /// No description provided for @commissionRulesTitle.
  ///
  /// In en, this message translates to:
  /// **'Commission Rules'**
  String get commissionRulesTitle;

  /// No description provided for @earningsStatement.
  ///
  /// In en, this message translates to:
  /// **'Earnings Statement'**
  String get earningsStatement;

  /// No description provided for @commissionBreakdown.
  ///
  /// In en, this message translates to:
  /// **'Commission Breakdown'**
  String get commissionBreakdown;

  /// No description provided for @totalCommissions.
  ///
  /// In en, this message translates to:
  /// **'Total commissions'**
  String get totalCommissions;

  /// No description provided for @bySource.
  ///
  /// In en, this message translates to:
  /// **'By source'**
  String get bySource;

  /// No description provided for @productSales.
  ///
  /// In en, this message translates to:
  /// **'Product sales'**
  String get productSales;

  /// No description provided for @addOnSales.
  ///
  /// In en, this message translates to:
  /// **'Add-ons sales'**
  String get addOnSales;

  /// No description provided for @bonusesAndAdjustments.
  ///
  /// In en, this message translates to:
  /// **'Bonuses and adjustments'**
  String get bonusesAndAdjustments;

  /// No description provided for @byProductTop.
  ///
  /// In en, this message translates to:
  /// **'By product (top 5)'**
  String get byProductTop;

  /// No description provided for @noEarningsThisMonth.
  ///
  /// In en, this message translates to:
  /// **'No earnings this month'**
  String get noEarningsThisMonth;

  /// No description provided for @periodDaily.
  ///
  /// In en, this message translates to:
  /// **'Daily'**
  String get periodDaily;

  /// No description provided for @periodWeekly.
  ///
  /// In en, this message translates to:
  /// **'Weekly'**
  String get periodWeekly;

  /// No description provided for @periodMonthly.
  ///
  /// In en, this message translates to:
  /// **'Monthly'**
  String get periodMonthly;

  /// No description provided for @earningDetails.
  ///
  /// In en, this message translates to:
  /// **'Earnings Details'**
  String get earningDetails;

  /// No description provided for @saleInformation.
  ///
  /// In en, this message translates to:
  /// **'Sale information'**
  String get saleInformation;

  /// No description provided for @saleAmount.
  ///
  /// In en, this message translates to:
  /// **'Sale amount'**
  String get saleAmount;

  /// No description provided for @myEarnings.
  ///
  /// In en, this message translates to:
  /// **'My earnings'**
  String get myEarnings;

  /// No description provided for @refundedCommission.
  ///
  /// In en, this message translates to:
  /// **'Taken back after a refund'**
  String get refundedCommission;

  /// No description provided for @netCommission.
  ///
  /// In en, this message translates to:
  /// **'Net commission'**
  String get netCommission;

  /// No description provided for @noSalesEarnedYet.
  ///
  /// In en, this message translates to:
  /// **'No commission this month'**
  String get noSalesEarnedYet;

  /// No description provided for @payoutsTitle.
  ///
  /// In en, this message translates to:
  /// **'Payouts & Payment History'**
  String get payoutsTitle;

  /// No description provided for @totalPaidOut.
  ///
  /// In en, this message translates to:
  /// **'Total paid out'**
  String get totalPaidOut;

  /// No description provided for @payoutCompleted.
  ///
  /// In en, this message translates to:
  /// **'Completed'**
  String get payoutCompleted;

  /// No description provided for @payoutPending.
  ///
  /// In en, this message translates to:
  /// **'Pending'**
  String get payoutPending;

  /// No description provided for @payoutCancelled.
  ///
  /// In en, this message translates to:
  /// **'Cancelled'**
  String get payoutCancelled;

  /// No description provided for @noPayoutsYet.
  ///
  /// In en, this message translates to:
  /// **'No payouts yet'**
  String get noPayoutsYet;

  /// No description provided for @payoutDetails.
  ///
  /// In en, this message translates to:
  /// **'Payout Details'**
  String get payoutDetails;

  /// No description provided for @currentInformation.
  ///
  /// In en, this message translates to:
  /// **'Information'**
  String get currentInformation;

  /// No description provided for @payoutDate.
  ///
  /// In en, this message translates to:
  /// **'Payout date'**
  String get payoutDate;

  /// No description provided for @requestedOn.
  ///
  /// In en, this message translates to:
  /// **'Requested on'**
  String get requestedOn;

  /// No description provided for @referenceLabel.
  ///
  /// In en, this message translates to:
  /// **'Reference'**
  String get referenceLabel;

  /// No description provided for @accountLabel.
  ///
  /// In en, this message translates to:
  /// **'Account'**
  String get accountLabel;

  /// No description provided for @amountBreakdown.
  ///
  /// In en, this message translates to:
  /// **'Amount breakdown'**
  String get amountBreakdown;

  /// No description provided for @payoutAmount.
  ///
  /// In en, this message translates to:
  /// **'Payout amount'**
  String get payoutAmount;

  /// No description provided for @statusLabel.
  ///
  /// In en, this message translates to:
  /// **'Status'**
  String get statusLabel;

  /// No description provided for @monthStatement.
  ///
  /// In en, this message translates to:
  /// **'Earnings of the month'**
  String get monthStatement;

  /// No description provided for @earningsAnalytics.
  ///
  /// In en, this message translates to:
  /// **'Earnings Analytics'**
  String get earningsAnalytics;

  /// No description provided for @keyInsights.
  ///
  /// In en, this message translates to:
  /// **'Key insights'**
  String get keyInsights;

  /// No description provided for @legendEarnings.
  ///
  /// In en, this message translates to:
  /// **'Earnings'**
  String get legendEarnings;

  /// No description provided for @legendCommissions.
  ///
  /// In en, this message translates to:
  /// **'Commissions'**
  String get legendCommissions;

  /// No description provided for @noAnalyticsYet.
  ///
  /// In en, this message translates to:
  /// **'Not enough earnings yet to show a trend.'**
  String get noAnalyticsYet;

  /// No description provided for @commissionRate.
  ///
  /// In en, this message translates to:
  /// **'Commission rate'**
  String get commissionRate;

  /// No description provided for @myOperations.
  ///
  /// In en, this message translates to:
  /// **'My operations'**
  String get myOperations;

  /// No description provided for @noOperationEarnings.
  ///
  /// In en, this message translates to:
  /// **'No sales in this month'**
  String get noOperationEarnings;

  /// No description provided for @statementIntro.
  ///
  /// In en, this message translates to:
  /// **'Generate and download your earnings statement for a period.'**
  String get statementIntro;

  /// No description provided for @selectPeriod.
  ///
  /// In en, this message translates to:
  /// **'Select period'**
  String get selectPeriod;

  /// No description provided for @statementIncludes.
  ///
  /// In en, this message translates to:
  /// **'Statement includes'**
  String get statementIncludes;

  /// No description provided for @sectionSummary.
  ///
  /// In en, this message translates to:
  /// **'Total earnings (summary)'**
  String get sectionSummary;

  /// No description provided for @sectionBreakdown.
  ///
  /// In en, this message translates to:
  /// **'Commission breakdown'**
  String get sectionBreakdown;

  /// No description provided for @sectionBonuses.
  ///
  /// In en, this message translates to:
  /// **'Bonuses'**
  String get sectionBonuses;

  /// No description provided for @sectionAdjustments.
  ///
  /// In en, this message translates to:
  /// **'Adjustments'**
  String get sectionAdjustments;

  /// No description provided for @sectionSales.
  ///
  /// In en, this message translates to:
  /// **'Sales summary (count and amount)'**
  String get sectionSales;

  /// No description provided for @exportPdf.
  ///
  /// In en, this message translates to:
  /// **'Export Statement (PDF)'**
  String get exportPdf;

  /// No description provided for @preparingStatement.
  ///
  /// In en, this message translates to:
  /// **'Preparing the statement…'**
  String get preparingStatement;

  /// No description provided for @chooseOneSection.
  ///
  /// In en, this message translates to:
  /// **'Choose at least one section.'**
  String get chooseOneSection;

  /// No description provided for @last3Months.
  ///
  /// In en, this message translates to:
  /// **'Last 3 months'**
  String get last3Months;

  /// No description provided for @last6Months.
  ///
  /// In en, this message translates to:
  /// **'Last 6 months'**
  String get last6Months;

  /// No description provided for @last12Months.
  ///
  /// In en, this message translates to:
  /// **'Last 12 months'**
  String get last12Months;

  /// No description provided for @rulesIntro.
  ///
  /// In en, this message translates to:
  /// **'Your earnings are calculated from the commission and bonus rules set by LUTIANO for your role.'**
  String get rulesIntro;

  /// No description provided for @rulesNote.
  ///
  /// In en, this message translates to:
  /// **'Only validated, non-refunded sales count. A refund takes back the matching commission.'**
  String get rulesNote;

  /// No description provided for @noRulesForRole.
  ///
  /// In en, this message translates to:
  /// **'No commission rules are set for your role yet.'**
  String get noRulesForRole;

  /// No description provided for @monthLabelTitle.
  ///
  /// In en, this message translates to:
  /// **'Month'**
  String get monthLabelTitle;

  /// No description provided for @vsPreviousMonth.
  ///
  /// In en, this message translates to:
  /// **'{percent} vs previous month'**
  String vsPreviousMonth(String percent);

  /// No description provided for @salesCountValue.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{1 sale} other{{count} sales}}'**
  String salesCountValue(int count);

  /// No description provided for @lastNDays.
  ///
  /// In en, this message translates to:
  /// **'Last {count} days'**
  String lastNDays(int count);

  /// No description provided for @lastNWeeks.
  ///
  /// In en, this message translates to:
  /// **'Last {count} weeks'**
  String lastNWeeks(int count);

  /// No description provided for @lastNMonths.
  ///
  /// In en, this message translates to:
  /// **'Last {count} months'**
  String lastNMonths(int count);

  /// No description provided for @rulePercentOf.
  ///
  /// In en, this message translates to:
  /// **'{percent} of the sale value'**
  String rulePercentOf(String percent);

  /// No description provided for @ruleFixedPerUnit.
  ///
  /// In en, this message translates to:
  /// **'{amount} per unit'**
  String ruleFixedPerUnit(String amount);

  /// No description provided for @payoutForMonth.
  ///
  /// In en, this message translates to:
  /// **'For {month}'**
  String payoutForMonth(String month);

  /// No description provided for @insightChange.
  ///
  /// In en, this message translates to:
  /// **'Earnings {change} compared with the month before.'**
  String insightChange(String change);

  /// No description provided for @insightCommissionShare.
  ///
  /// In en, this message translates to:
  /// **'Commissions make up {percent} of your earnings.'**
  String insightCommissionShare(String percent);

  /// No description provided for @insightBestMonth.
  ///
  /// In en, this message translates to:
  /// **'{month} was your best month ({amount}).'**
  String insightBestMonth(String month, String amount);

  /// No description provided for @targetsAndBonuses.
  ///
  /// In en, this message translates to:
  /// **'Targets & Bonuses'**
  String get targetsAndBonuses;

  /// No description provided for @targetsSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Track progress and bonuses'**
  String get targetsSubtitle;

  /// No description provided for @targetProgress.
  ///
  /// In en, this message translates to:
  /// **'Target progress'**
  String get targetProgress;

  /// No description provided for @salesTargetLabel.
  ///
  /// In en, this message translates to:
  /// **'Target'**
  String get salesTargetLabel;

  /// No description provided for @achievedLabel.
  ///
  /// In en, this message translates to:
  /// **'Achieved'**
  String get achievedLabel;

  /// No description provided for @remainingLabel.
  ///
  /// In en, this message translates to:
  /// **'Remaining'**
  String get remainingLabel;

  /// No description provided for @activeTargets.
  ///
  /// In en, this message translates to:
  /// **'Active targets'**
  String get activeTargets;

  /// No description provided for @bonusEligible.
  ///
  /// In en, this message translates to:
  /// **'Bonus earned'**
  String get bonusEligible;

  /// No description provided for @bonusIncluded.
  ///
  /// In en, this message translates to:
  /// **'In earnings'**
  String get bonusIncluded;

  /// No description provided for @viewTargetDetails.
  ///
  /// In en, this message translates to:
  /// **'View target details'**
  String get viewTargetDetails;

  /// No description provided for @myBonuses.
  ///
  /// In en, this message translates to:
  /// **'My Bonuses'**
  String get myBonuses;

  /// No description provided for @myTargets.
  ///
  /// In en, this message translates to:
  /// **'My Targets'**
  String get myTargets;

  /// No description provided for @performanceSummary.
  ///
  /// In en, this message translates to:
  /// **'Performance Summary'**
  String get performanceSummary;

  /// No description provided for @targetProgressTrend.
  ///
  /// In en, this message translates to:
  /// **'Target Progress Trend'**
  String get targetProgressTrend;

  /// No description provided for @targetRulesPolicy.
  ///
  /// In en, this message translates to:
  /// **'Target Rules & Bonus Policy'**
  String get targetRulesPolicy;

  /// No description provided for @noTargetThisMonth.
  ///
  /// In en, this message translates to:
  /// **'No target this month'**
  String get noTargetThisMonth;

  /// No description provided for @noTargetThisMonthMessage.
  ///
  /// In en, this message translates to:
  /// **'Your manager sets your targets. They appear here once set.'**
  String get noTargetThisMonthMessage;

  /// No description provided for @targetDetails.
  ///
  /// In en, this message translates to:
  /// **'Target Details'**
  String get targetDetails;

  /// No description provided for @targetInformation.
  ///
  /// In en, this message translates to:
  /// **'Target information'**
  String get targetInformation;

  /// No description provided for @targetAmount.
  ///
  /// In en, this message translates to:
  /// **'Target'**
  String get targetAmount;

  /// No description provided for @achievedSales.
  ///
  /// In en, this message translates to:
  /// **'Achieved'**
  String get achievedSales;

  /// No description provided for @progressLabel.
  ///
  /// In en, this message translates to:
  /// **'Progress'**
  String get progressLabel;

  /// No description provided for @periodLabel.
  ///
  /// In en, this message translates to:
  /// **'Period'**
  String get periodLabel;

  /// No description provided for @targetProduct.
  ///
  /// In en, this message translates to:
  /// **'Product'**
  String get targetProduct;

  /// No description provided for @rewardLabel.
  ///
  /// In en, this message translates to:
  /// **'Reward'**
  String get rewardLabel;

  /// No description provided for @bonusOnTarget.
  ///
  /// In en, this message translates to:
  /// **'Bonus on target'**
  String get bonusOnTarget;

  /// No description provided for @reachTargetHint.
  ///
  /// In en, this message translates to:
  /// **'Reach 100% of the target to earn the bonus.'**
  String get reachTargetHint;

  /// No description provided for @targetStateActive.
  ///
  /// In en, this message translates to:
  /// **'Active'**
  String get targetStateActive;

  /// No description provided for @targetStateUpcoming.
  ///
  /// In en, this message translates to:
  /// **'Upcoming'**
  String get targetStateUpcoming;

  /// No description provided for @targetStateCompleted.
  ///
  /// In en, this message translates to:
  /// **'Completed'**
  String get targetStateCompleted;

  /// No description provided for @noTargetsYet.
  ///
  /// In en, this message translates to:
  /// **'No targets yet'**
  String get noTargetsYet;

  /// No description provided for @bonusEligibility.
  ///
  /// In en, this message translates to:
  /// **'Bonus Eligibility'**
  String get bonusEligibility;

  /// No description provided for @eligibilityStatus.
  ///
  /// In en, this message translates to:
  /// **'Eligibility status'**
  String get eligibilityStatus;

  /// No description provided for @eligibleTitle.
  ///
  /// In en, this message translates to:
  /// **'You are eligible for this bonus!'**
  String get eligibleTitle;

  /// No description provided for @notEligibleYet.
  ///
  /// In en, this message translates to:
  /// **'Not eligible yet'**
  String get notEligibleYet;

  /// No description provided for @conditionReachTarget.
  ///
  /// In en, this message translates to:
  /// **'Target reached (100% or more)'**
  String get conditionReachTarget;

  /// No description provided for @currentStatus.
  ///
  /// In en, this message translates to:
  /// **'Current status'**
  String get currentStatus;

  /// No description provided for @statusReached.
  ///
  /// In en, this message translates to:
  /// **'Target reached'**
  String get statusReached;

  /// No description provided for @statusOnTrack.
  ///
  /// In en, this message translates to:
  /// **'On track'**
  String get statusOnTrack;

  /// No description provided for @statusBehind.
  ///
  /// In en, this message translates to:
  /// **'Behind'**
  String get statusBehind;

  /// No description provided for @statusNotReached.
  ///
  /// In en, this message translates to:
  /// **'Not reached'**
  String get statusNotReached;

  /// No description provided for @statusNotStarted.
  ///
  /// In en, this message translates to:
  /// **'Not started'**
  String get statusNotStarted;

  /// No description provided for @bonusDetails.
  ///
  /// In en, this message translates to:
  /// **'Bonus Details'**
  String get bonusDetails;

  /// No description provided for @bonusInformation.
  ///
  /// In en, this message translates to:
  /// **'Bonus information'**
  String get bonusInformation;

  /// No description provided for @bonusTypeLabel.
  ///
  /// In en, this message translates to:
  /// **'Bonus type'**
  String get bonusTypeLabel;

  /// No description provided for @bonusRateLabel.
  ///
  /// In en, this message translates to:
  /// **'Bonus rate'**
  String get bonusRateLabel;

  /// No description provided for @bonusAmountLabel.
  ///
  /// In en, this message translates to:
  /// **'Bonus amount'**
  String get bonusAmountLabel;

  /// No description provided for @expectedPayout.
  ///
  /// In en, this message translates to:
  /// **'Expected payout'**
  String get expectedPayout;

  /// No description provided for @bonusFixed.
  ///
  /// In en, this message translates to:
  /// **'Fixed'**
  String get bonusFixed;

  /// No description provided for @bonusPerUnit.
  ///
  /// In en, this message translates to:
  /// **'Per unit sold'**
  String get bonusPerUnit;

  /// No description provided for @bonusStatePending.
  ///
  /// In en, this message translates to:
  /// **'Pending'**
  String get bonusStatePending;

  /// No description provided for @bonusStateIncluded.
  ///
  /// In en, this message translates to:
  /// **'In earnings'**
  String get bonusStateIncluded;

  /// No description provided for @bonusStateReversed.
  ///
  /// In en, this message translates to:
  /// **'Reversed'**
  String get bonusStateReversed;

  /// No description provided for @bonusStateInProgress.
  ///
  /// In en, this message translates to:
  /// **'In progress'**
  String get bonusStateInProgress;

  /// No description provided for @bonusStateNotReached.
  ///
  /// In en, this message translates to:
  /// **'Not reached'**
  String get bonusStateNotReached;

  /// No description provided for @bonusPendingNote.
  ///
  /// In en, this message translates to:
  /// **'Reached. It joins your earnings when the month is closed by the admin team, and is paid out with them.'**
  String get bonusPendingNote;

  /// No description provided for @bonusIncludedNote.
  ///
  /// In en, this message translates to:
  /// **'This bonus is part of your finalised earnings.'**
  String get bonusIncludedNote;

  /// No description provided for @bonusReversedNote.
  ///
  /// In en, this message translates to:
  /// **'A refund meant the target was no longer reached, so the bonus was taken back.'**
  String get bonusReversedNote;

  /// No description provided for @bonusInProgressNote.
  ///
  /// In en, this message translates to:
  /// **'Keep selling: the bonus is earned once the target is reached.'**
  String get bonusInProgressNote;

  /// No description provided for @bonusNotReachedNote.
  ///
  /// In en, this message translates to:
  /// **'The target period ended without reaching the target, so there is no bonus.'**
  String get bonusNotReachedNote;

  /// No description provided for @bonusHistory.
  ///
  /// In en, this message translates to:
  /// **'Bonus History'**
  String get bonusHistory;

  /// No description provided for @noBonusesYet.
  ///
  /// In en, this message translates to:
  /// **'No bonuses yet'**
  String get noBonusesYet;

  /// No description provided for @noBonusesMessage.
  ///
  /// In en, this message translates to:
  /// **'Bonuses you earn appear here.'**
  String get noBonusesMessage;

  /// No description provided for @totalSalesLabel.
  ///
  /// In en, this message translates to:
  /// **'Total sales'**
  String get totalSalesLabel;

  /// No description provided for @targetsReachedLabel.
  ///
  /// In en, this message translates to:
  /// **'Targets reached'**
  String get targetsReachedLabel;

  /// No description provided for @bonusesEarnedLabel.
  ///
  /// In en, this message translates to:
  /// **'Bonuses earned'**
  String get bonusesEarnedLabel;

  /// No description provided for @monthlyPerformance.
  ///
  /// In en, this message translates to:
  /// **'Monthly performance'**
  String get monthlyPerformance;

  /// No description provided for @noPerformanceYet.
  ///
  /// In en, this message translates to:
  /// **'No targets ended in this range yet.'**
  String get noPerformanceYet;

  /// No description provided for @avgAchievement.
  ///
  /// In en, this message translates to:
  /// **'Average achievement'**
  String get avgAchievement;

  /// No description provided for @vsPreviousRange.
  ///
  /// In en, this message translates to:
  /// **'vs previous period'**
  String get vsPreviousRange;

  /// No description provided for @achievementLegend.
  ///
  /// In en, this message translates to:
  /// **'Achievement (%)'**
  String get achievementLegend;

  /// No description provided for @salesLegend.
  ///
  /// In en, this message translates to:
  /// **'Sales'**
  String get salesLegend;

  /// No description provided for @rulesTargetsTitle.
  ///
  /// In en, this message translates to:
  /// **'Target rules'**
  String get rulesTargetsTitle;

  /// No description provided for @rulesBonusTitle.
  ///
  /// In en, this message translates to:
  /// **'Bonus policy'**
  String get rulesBonusTitle;

  /// No description provided for @ruleT1.
  ///
  /// In en, this message translates to:
  /// **'Your manager sets your targets: an amount of sales or a number of units, for a day, a week, a month or a chosen period.'**
  String get ruleT1;

  /// No description provided for @ruleT2.
  ///
  /// In en, this message translates to:
  /// **'Progress counts your completed, paid sales, reduced by any refund.'**
  String get ruleT2;

  /// No description provided for @ruleB1.
  ///
  /// In en, this message translates to:
  /// **'A bonus is earned when you reach 100% of the target.'**
  String get ruleB1;

  /// No description provided for @ruleB2.
  ///
  /// In en, this message translates to:
  /// **'A bonus is either a fixed amount or an amount for each unit sold in the period.'**
  String get ruleB2;

  /// No description provided for @ruleB3.
  ///
  /// In en, this message translates to:
  /// **'Earned bonuses join your earnings when the month is closed, and are paid out with them.'**
  String get ruleB3;

  /// No description provided for @ruleB4.
  ///
  /// In en, this message translates to:
  /// **'A refund that brings you back under the target takes the bonus back.'**
  String get ruleB4;

  /// No description provided for @targetPeriodDaily.
  ///
  /// In en, this message translates to:
  /// **'Daily'**
  String get targetPeriodDaily;

  /// No description provided for @targetPeriodWeekly.
  ///
  /// In en, this message translates to:
  /// **'Weekly'**
  String get targetPeriodWeekly;

  /// No description provided for @targetPeriodMonthly.
  ///
  /// In en, this message translates to:
  /// **'Monthly'**
  String get targetPeriodMonthly;

  /// No description provided for @targetPeriodCustom.
  ///
  /// In en, this message translates to:
  /// **'Custom period'**
  String get targetPeriodCustom;

  /// No description provided for @unitsValue.
  ///
  /// In en, this message translates to:
  /// **'{count} units'**
  String unitsValue(int count);

  /// No description provided for @percentValue.
  ///
  /// In en, this message translates to:
  /// **'{percent}%'**
  String percentValue(String percent);

  /// No description provided for @awayFromBonus.
  ///
  /// In en, this message translates to:
  /// **'Keep going! You are {percent} away from reaching the target.'**
  String awayFromBonus(String percent);

  /// No description provided for @bonusPaidIn.
  ///
  /// In en, this message translates to:
  /// **'Included in the earnings of {month}'**
  String bonusPaidIn(String month);

  /// No description provided for @afterMonthClosed.
  ///
  /// In en, this message translates to:
  /// **'After {month} is closed'**
  String afterMonthClosed(String month);

  /// No description provided for @targetsReachedValue.
  ///
  /// In en, this message translates to:
  /// **'{reached} / {total}'**
  String targetsReachedValue(int reached, int total);

  /// No description provided for @perUnitRate.
  ///
  /// In en, this message translates to:
  /// **'{amount} per unit sold'**
  String perUnitRate(String amount);

  /// No description provided for @fixedRate.
  ///
  /// In en, this message translates to:
  /// **'{amount} (fixed)'**
  String fixedRate(String amount);

  /// No description provided for @notifications.
  ///
  /// In en, this message translates to:
  /// **'Notifications'**
  String get notifications;

  /// No description provided for @notificationsSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Stay updated on your operations'**
  String get notificationsSubtitle;

  /// No description provided for @notifAll.
  ///
  /// In en, this message translates to:
  /// **'All'**
  String get notifAll;

  /// No description provided for @notifUnread.
  ///
  /// In en, this message translates to:
  /// **'Unread'**
  String get notifUnread;

  /// No description provided for @notifImportant.
  ///
  /// In en, this message translates to:
  /// **'Important'**
  String get notifImportant;

  /// No description provided for @markAllAsRead.
  ///
  /// In en, this message translates to:
  /// **'Mark all as read'**
  String get markAllAsRead;

  /// No description provided for @markAsRead.
  ///
  /// In en, this message translates to:
  /// **'Mark as read'**
  String get markAsRead;

  /// No description provided for @clearRead.
  ///
  /// In en, this message translates to:
  /// **'Clear read'**
  String get clearRead;

  /// No description provided for @clearSelected.
  ///
  /// In en, this message translates to:
  /// **'Clear'**
  String get clearSelected;

  /// No description provided for @managePreferences.
  ///
  /// In en, this message translates to:
  /// **'Manage preferences'**
  String get managePreferences;

  /// No description provided for @notificationCategories.
  ///
  /// In en, this message translates to:
  /// **'Notification Categories'**
  String get notificationCategories;

  /// No description provided for @notificationHistory.
  ///
  /// In en, this message translates to:
  /// **'Notification History'**
  String get notificationHistory;

  /// No description provided for @notificationPreferences.
  ///
  /// In en, this message translates to:
  /// **'Notification Preferences'**
  String get notificationPreferences;

  /// No description provided for @notificationDetails.
  ///
  /// In en, this message translates to:
  /// **'Notification Details'**
  String get notificationDetails;

  /// No description provided for @noNotifications.
  ///
  /// In en, this message translates to:
  /// **'No notifications'**
  String get noNotifications;

  /// No description provided for @noNotificationsMessage.
  ///
  /// In en, this message translates to:
  /// **'Alerts about your stock, requests, earnings and targets appear here.'**
  String get noNotificationsMessage;

  /// No description provided for @catInventory.
  ///
  /// In en, this message translates to:
  /// **'Stock & Inventory'**
  String get catInventory;

  /// No description provided for @catRequests.
  ///
  /// In en, this message translates to:
  /// **'Stock Requests'**
  String get catRequests;

  /// No description provided for @catSales.
  ///
  /// In en, this message translates to:
  /// **'Sales & Payments'**
  String get catSales;

  /// No description provided for @catEarnings.
  ///
  /// In en, this message translates to:
  /// **'Earnings & Payouts'**
  String get catEarnings;

  /// No description provided for @catTargets.
  ///
  /// In en, this message translates to:
  /// **'Targets & Bonuses'**
  String get catTargets;

  /// No description provided for @catOperations.
  ///
  /// In en, this message translates to:
  /// **'Operations & Workflow'**
  String get catOperations;

  /// No description provided for @catSystem.
  ///
  /// In en, this message translates to:
  /// **'System & Announcements'**
  String get catSystem;

  /// No description provided for @catInventorySub.
  ///
  /// In en, this message translates to:
  /// **'Low stock, stock updates'**
  String get catInventorySub;

  /// No description provided for @catRequestsSub.
  ///
  /// In en, this message translates to:
  /// **'Approval, rejection, fulfilment'**
  String get catRequestsSub;

  /// No description provided for @catSalesSub.
  ///
  /// In en, this message translates to:
  /// **'Sales summary, payment alerts'**
  String get catSalesSub;

  /// No description provided for @catEarningsSub.
  ///
  /// In en, this message translates to:
  /// **'Commissions, payouts'**
  String get catEarningsSub;

  /// No description provided for @catTargetsSub.
  ///
  /// In en, this message translates to:
  /// **'Progress, achievements, bonuses'**
  String get catTargetsSub;

  /// No description provided for @catOperationsSub.
  ///
  /// In en, this message translates to:
  /// **'Handover, reconciliation, shifts'**
  String get catOperationsSub;

  /// No description provided for @catSystemSub.
  ///
  /// In en, this message translates to:
  /// **'App updates, important notices'**
  String get catSystemSub;

  /// No description provided for @alwaysOn.
  ///
  /// In en, this message translates to:
  /// **'Always on'**
  String get alwaysOn;

  /// No description provided for @preferencesIntro.
  ///
  /// In en, this message translates to:
  /// **'Choose which notifications you want to receive in the app.'**
  String get preferencesIntro;

  /// No description provided for @pushNotAvailable.
  ///
  /// In en, this message translates to:
  /// **'Notifications on your phone (push) are not available yet.'**
  String get pushNotAvailable;

  /// No description provided for @allTime.
  ///
  /// In en, this message translates to:
  /// **'All time'**
  String get allTime;

  /// No description provided for @yesterdayLabel.
  ///
  /// In en, this message translates to:
  /// **'Yesterday'**
  String get yesterdayLabel;

  /// No description provided for @agoNow.
  ///
  /// In en, this message translates to:
  /// **'Now'**
  String get agoNow;

  /// No description provided for @notifView.
  ///
  /// In en, this message translates to:
  /// **'View'**
  String get notifView;

  /// No description provided for @ntLowStock.
  ///
  /// In en, this message translates to:
  /// **'Low Stock Alert'**
  String get ntLowStock;

  /// No description provided for @ntOutOfStock.
  ///
  /// In en, this message translates to:
  /// **'Out of Stock'**
  String get ntOutOfStock;

  /// No description provided for @ntStockOnItsWay.
  ///
  /// In en, this message translates to:
  /// **'Stock on its way'**
  String get ntStockOnItsWay;

  /// No description provided for @ntRequestApproved.
  ///
  /// In en, this message translates to:
  /// **'Stock Request Approved'**
  String get ntRequestApproved;

  /// No description provided for @ntRequestRejected.
  ///
  /// In en, this message translates to:
  /// **'Stock Request Rejected'**
  String get ntRequestRejected;

  /// No description provided for @ntPayoutApproved.
  ///
  /// In en, this message translates to:
  /// **'Payout Approved'**
  String get ntPayoutApproved;

  /// No description provided for @ntPayoutPaid.
  ///
  /// In en, this message translates to:
  /// **'Payout Paid'**
  String get ntPayoutPaid;

  /// No description provided for @ntPayoutReversed.
  ///
  /// In en, this message translates to:
  /// **'Payout Reversed'**
  String get ntPayoutReversed;

  /// No description provided for @ntNumberVerified.
  ///
  /// In en, this message translates to:
  /// **'Payout Number Confirmed'**
  String get ntNumberVerified;

  /// No description provided for @ntNumberRejected.
  ///
  /// In en, this message translates to:
  /// **'Payout Number Rejected'**
  String get ntNumberRejected;

  /// No description provided for @ntEarningsFinal.
  ///
  /// In en, this message translates to:
  /// **'Monthly Earnings Final'**
  String get ntEarningsFinal;

  /// No description provided for @ntTargetReached.
  ///
  /// In en, this message translates to:
  /// **'Target Reached'**
  String get ntTargetReached;

  /// No description provided for @ntTargetMilestone.
  ///
  /// In en, this message translates to:
  /// **'Target Progress'**
  String get ntTargetMilestone;

  /// No description provided for @ntHandover.
  ///
  /// In en, this message translates to:
  /// **'Cash Handover'**
  String get ntHandover;

  /// No description provided for @ntShiftOpen.
  ///
  /// In en, this message translates to:
  /// **'Shift Still Open'**
  String get ntShiftOpen;

  /// No description provided for @ntAnnouncement.
  ///
  /// In en, this message translates to:
  /// **'Announcement'**
  String get ntAnnouncement;

  /// No description provided for @ntGeneric.
  ///
  /// In en, this message translates to:
  /// **'Notification'**
  String get ntGeneric;

  /// No description provided for @notifFilterLabel.
  ///
  /// In en, this message translates to:
  /// **'{label} ({count})'**
  String notifFilterLabel(String label, int count);

  /// No description provided for @newCount.
  ///
  /// In en, this message translates to:
  /// **'{count} new'**
  String newCount(int count);

  /// No description provided for @selectedCount.
  ///
  /// In en, this message translates to:
  /// **'{count} selected'**
  String selectedCount(int count);

  /// No description provided for @agoMinutes.
  ///
  /// In en, this message translates to:
  /// **'{count}m'**
  String agoMinutes(int count);

  /// No description provided for @agoHours.
  ///
  /// In en, this message translates to:
  /// **'{count}h'**
  String agoHours(int count);

  /// No description provided for @agoDays.
  ///
  /// In en, this message translates to:
  /// **'{count}d'**
  String agoDays(int count);

  /// No description provided for @pushOnPhone.
  ///
  /// In en, this message translates to:
  /// **'Push notifications on this phone'**
  String get pushOnPhone;

  /// No description provided for @editProfile.
  ///
  /// In en, this message translates to:
  /// **'Edit Profile'**
  String get editProfile;

  /// No description provided for @fullNameLabel.
  ///
  /// In en, this message translates to:
  /// **'Full name'**
  String get fullNameLabel;

  /// No description provided for @changePhone.
  ///
  /// In en, this message translates to:
  /// **'Change phone number'**
  String get changePhone;

  /// No description provided for @newPhoneLabel.
  ///
  /// In en, this message translates to:
  /// **'New phone number'**
  String get newPhoneLabel;

  /// No description provided for @confirmationCode.
  ///
  /// In en, this message translates to:
  /// **'Confirmation code'**
  String get confirmationCode;

  /// No description provided for @saveChanges.
  ///
  /// In en, this message translates to:
  /// **'Save changes'**
  String get saveChanges;

  /// No description provided for @profileSaved.
  ///
  /// In en, this message translates to:
  /// **'Profile updated'**
  String get profileSaved;

  /// No description provided for @phoneChanged.
  ///
  /// In en, this message translates to:
  /// **'Phone number changed'**
  String get phoneChanged;

  /// No description provided for @changePhoto.
  ///
  /// In en, this message translates to:
  /// **'Change photo'**
  String get changePhoto;

  /// No description provided for @photoUpdated.
  ///
  /// In en, this message translates to:
  /// **'Photo updated'**
  String get photoUpdated;

  /// No description provided for @enterYourName.
  ///
  /// In en, this message translates to:
  /// **'Enter your name.'**
  String get enterYourName;

  /// No description provided for @emailReadOnly.
  ///
  /// In en, this message translates to:
  /// **'Your email can only be changed by an administrator.'**
  String get emailReadOnly;

  /// No description provided for @fileTooBig.
  ///
  /// In en, this message translates to:
  /// **'The image is larger than 10 MB.'**
  String get fileTooBig;

  /// No description provided for @securityIntro.
  ///
  /// In en, this message translates to:
  /// **'Keep your account secure: your password, your sign-ins and your devices.'**
  String get securityIntro;

  /// No description provided for @twoFactor.
  ///
  /// In en, this message translates to:
  /// **'Two-factor authentication'**
  String get twoFactor;

  /// No description provided for @twoFactorAdminsOnly.
  ///
  /// In en, this message translates to:
  /// **'Required for administrators only'**
  String get twoFactorAdminsOnly;

  /// No description provided for @loginActivity.
  ///
  /// In en, this message translates to:
  /// **'Login Activity'**
  String get loginActivity;

  /// No description provided for @loginActivitySub.
  ///
  /// In en, this message translates to:
  /// **'See recent sign-ins'**
  String get loginActivitySub;

  /// No description provided for @signOutEverywhere.
  ///
  /// In en, this message translates to:
  /// **'Sign out of all other devices'**
  String get signOutEverywhere;

  /// No description provided for @signOutEverywhereSub.
  ///
  /// In en, this message translates to:
  /// **'Every other phone must sign in again'**
  String get signOutEverywhereSub;

  /// No description provided for @signOutEverywhereConfirm.
  ///
  /// In en, this message translates to:
  /// **'Every other phone will have to sign in again. This phone stays signed in.'**
  String get signOutEverywhereConfirm;

  /// No description provided for @signedOutEverywhere.
  ///
  /// In en, this message translates to:
  /// **'Signed out of all other devices'**
  String get signedOutEverywhere;

  /// No description provided for @activitySignedIn.
  ///
  /// In en, this message translates to:
  /// **'Signed in'**
  String get activitySignedIn;

  /// No description provided for @activityPasswordChanged.
  ///
  /// In en, this message translates to:
  /// **'Password changed'**
  String get activityPasswordChanged;

  /// No description provided for @activitySessionsRevoked.
  ///
  /// In en, this message translates to:
  /// **'Signed out of other devices'**
  String get activitySessionsRevoked;

  /// No description provided for @noActivity.
  ///
  /// In en, this message translates to:
  /// **'No activity yet'**
  String get noActivity;

  /// No description provided for @appSettings.
  ///
  /// In en, this message translates to:
  /// **'App Settings'**
  String get appSettings;

  /// No description provided for @appSettingsSub.
  ///
  /// In en, this message translates to:
  /// **'Language, receipt, notifications'**
  String get appSettingsSub;

  /// No description provided for @general.
  ///
  /// In en, this message translates to:
  /// **'General'**
  String get general;

  /// No description provided for @languageLabel.
  ///
  /// In en, this message translates to:
  /// **'Language'**
  String get languageLabel;

  /// No description provided for @followPhone.
  ///
  /// In en, this message translates to:
  /// **'Follow my phone'**
  String get followPhone;

  /// No description provided for @operational.
  ///
  /// In en, this message translates to:
  /// **'Operational'**
  String get operational;

  /// No description provided for @defaultOperation.
  ///
  /// In en, this message translates to:
  /// **'Default operation'**
  String get defaultOperation;

  /// No description provided for @defaultOperationSub.
  ///
  /// In en, this message translates to:
  /// **'Opens first when you sign in'**
  String get defaultOperationSub;

  /// No description provided for @showReceiptAfterSale.
  ///
  /// In en, this message translates to:
  /// **'Show receipt after sale'**
  String get showReceiptAfterSale;

  /// No description provided for @receiptOptionsSub.
  ///
  /// In en, this message translates to:
  /// **'Offer the receipt on the sale confirmation'**
  String get receiptOptionsSub;

  /// No description provided for @faq.
  ///
  /// In en, this message translates to:
  /// **'Frequently Asked Questions'**
  String get faq;

  /// No description provided for @faqSub.
  ///
  /// In en, this message translates to:
  /// **'Quick answers to common questions'**
  String get faqSub;

  /// No description provided for @contactSupport.
  ///
  /// In en, this message translates to:
  /// **'Contact Support'**
  String get contactSupport;

  /// No description provided for @contactSupportSub.
  ///
  /// In en, this message translates to:
  /// **'Get help from the LUTIANO team'**
  String get contactSupportSub;

  /// No description provided for @reportIssue.
  ///
  /// In en, this message translates to:
  /// **'Report an Issue'**
  String get reportIssue;

  /// No description provided for @reportIssueSub.
  ///
  /// In en, this message translates to:
  /// **'Something not working as expected?'**
  String get reportIssueSub;

  /// No description provided for @suggestImprovement.
  ///
  /// In en, this message translates to:
  /// **'Suggest an Improvement'**
  String get suggestImprovement;

  /// No description provided for @suggestImprovementSub.
  ///
  /// In en, this message translates to:
  /// **'Share your ideas with us'**
  String get suggestImprovementSub;

  /// No description provided for @mySupportRequests.
  ///
  /// In en, this message translates to:
  /// **'My support requests'**
  String get mySupportRequests;

  /// No description provided for @supportRequest.
  ///
  /// In en, this message translates to:
  /// **'Support Request'**
  String get supportRequest;

  /// No description provided for @requestType.
  ///
  /// In en, this message translates to:
  /// **'Request type'**
  String get requestType;

  /// No description provided for @supportSubject.
  ///
  /// In en, this message translates to:
  /// **'Subject'**
  String get supportSubject;

  /// No description provided for @supportSubjectHint.
  ///
  /// In en, this message translates to:
  /// **'e.g. Cannot record a sale'**
  String get supportSubjectHint;

  /// No description provided for @supportDescription.
  ///
  /// In en, this message translates to:
  /// **'Description'**
  String get supportDescription;

  /// No description provided for @supportDescriptionHint.
  ///
  /// In en, this message translates to:
  /// **'Describe your issue in detail…'**
  String get supportDescriptionHint;

  /// No description provided for @supportFillIn.
  ///
  /// In en, this message translates to:
  /// **'Fill in the subject and the description.'**
  String get supportFillIn;

  /// No description provided for @attachImage.
  ///
  /// In en, this message translates to:
  /// **'Attach an image (optional)'**
  String get attachImage;

  /// No description provided for @removeAttachment.
  ///
  /// In en, this message translates to:
  /// **'Remove the image'**
  String get removeAttachment;

  /// No description provided for @maxTenMb.
  ///
  /// In en, this message translates to:
  /// **'Image, up to 10 MB'**
  String get maxTenMb;

  /// No description provided for @supportTypeQuestion.
  ///
  /// In en, this message translates to:
  /// **'Question'**
  String get supportTypeQuestion;

  /// No description provided for @supportTypeIssue.
  ///
  /// In en, this message translates to:
  /// **'Issue'**
  String get supportTypeIssue;

  /// No description provided for @supportTypeImprovement.
  ///
  /// In en, this message translates to:
  /// **'Improvement'**
  String get supportTypeImprovement;

  /// No description provided for @supportTypeAccount.
  ///
  /// In en, this message translates to:
  /// **'Account'**
  String get supportTypeAccount;

  /// No description provided for @supportOpen.
  ///
  /// In en, this message translates to:
  /// **'Open'**
  String get supportOpen;

  /// No description provided for @supportInProgress.
  ///
  /// In en, this message translates to:
  /// **'In progress'**
  String get supportInProgress;

  /// No description provided for @supportResolved.
  ///
  /// In en, this message translates to:
  /// **'Resolved'**
  String get supportResolved;

  /// No description provided for @noSupportRequests.
  ///
  /// In en, this message translates to:
  /// **'No support requests yet'**
  String get noSupportRequests;

  /// No description provided for @teamReply.
  ///
  /// In en, this message translates to:
  /// **'Reply from the team'**
  String get teamReply;

  /// No description provided for @noReplyYet.
  ///
  /// In en, this message translates to:
  /// **'No reply yet. You will be notified when the team answers.'**
  String get noReplyYet;

  /// No description provided for @ntSupportReply.
  ///
  /// In en, this message translates to:
  /// **'Support Reply'**
  String get ntSupportReply;

  /// No description provided for @faqQ1.
  ///
  /// In en, this message translates to:
  /// **'Why can\'t I record a sale?'**
  String get faqQ1;

  /// No description provided for @faqA1.
  ///
  /// In en, this message translates to:
  /// **'You need an open cash shift. Open one from the Cash shift card on Home, with your opening float.'**
  String get faqA1;

  /// No description provided for @faqQ2.
  ///
  /// In en, this message translates to:
  /// **'How do I receive stock?'**
  String get faqQ2;

  /// No description provided for @faqA2.
  ///
  /// In en, this message translates to:
  /// **'Go to Inventory, then Receive Stock. Count what arrived and confirm the quantity. A shortfall needs a reason.'**
  String get faqA2;

  /// No description provided for @faqQ3.
  ///
  /// In en, this message translates to:
  /// **'How do I ask for more stock?'**
  String get faqQ3;

  /// No description provided for @faqA3.
  ///
  /// In en, this message translates to:
  /// **'Inventory, then Stock Requests, then Request Stock (Mobile Barista and Market Sales Agent). A manager approves or rejects the whole request.'**
  String get faqA3;

  /// No description provided for @faqQ4.
  ///
  /// In en, this message translates to:
  /// **'When are my earnings final?'**
  String get faqQ4;

  /// No description provided for @faqA4.
  ///
  /// In en, this message translates to:
  /// **'A month shows as provisional until the admin team closes it. Bonuses you earned join your earnings at that time.'**
  String get faqA4;

  /// No description provided for @faqQ5.
  ///
  /// In en, this message translates to:
  /// **'I forgot my password'**
  String get faqQ5;

  /// No description provided for @faqA5.
  ///
  /// In en, this message translates to:
  /// **'On the sign-in screen tap Forgot password. A code is sent by SMS to the phone number on file.'**
  String get faqA5;

  /// No description provided for @faqQ6.
  ///
  /// In en, this message translates to:
  /// **'How do I change my phone number?'**
  String get faqQ6;

  /// No description provided for @faqA6.
  ///
  /// In en, this message translates to:
  /// **'Profile, then Edit Profile, then Change phone number. A code is sent to the new number to confirm it.'**
  String get faqA6;

  /// No description provided for @codeSentTo.
  ///
  /// In en, this message translates to:
  /// **'We sent a code to {phone}.'**
  String codeSentTo(String phone);

  /// No description provided for @requestSentRef.
  ///
  /// In en, this message translates to:
  /// **'Request {reference} sent'**
  String requestSentRef(String reference);

  /// No description provided for @sendPhoneCode.
  ///
  /// In en, this message translates to:
  /// **'Send code'**
  String get sendPhoneCode;

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

  /// No description provided for @legalDraftNotice.
  ///
  /// In en, this message translates to:
  /// **'Draft text, to be reviewed by LUTIANO\'s legal adviser before the app is released.'**
  String get legalDraftNotice;

  /// No description provided for @termsBody.
  ///
  /// In en, this message translates to:
  /// **'## Who these terms are for\nThis app is for LUTIANO sellers. Your account is created by LUTIANO and is for your use only.\n\n## Your account\nKeep your password private and never share your account. Everything done with your account is recorded under your name. If you think someone else knows your password, change it or sign out of all other devices from Security.\n\n## Recording sales and stock\nRecord each sale, cash movement and stock movement accurately and on time. Do not record sales that did not happen. Corrections go through a refund or a request, never through a second sale.\n\n## Money and stock\nYou are responsible for the cash and stock you handle during your shift. Differences found when closing a shift are recorded and reviewed by the team.\n\n## Earnings\nCommissions, bonuses and payouts follow the rules set by LUTIANO and shown in the Earnings tab. Figures are provisional until the month is finalised.\n\n## Acceptable use\nUse the app only for LUTIANO work. Do not try to access data you are not allowed to see, and do not interfere with the app or its servers.\n\n## Ending access\nLUTIANO can suspend or close an account when a seller leaves or when these terms are not respected.\n\n## Changes and contact\nThese terms may change; the app will show the new version. For any question, use Help & Support in your profile.'**
  String get termsBody;

  /// No description provided for @privacyBody.
  ///
  /// In en, this message translates to:
  /// **'## What we keep about you\nYour name, phone number, email address, profile photo, language, the locations you work at, and your sign-in history. If the app crashes, a report with the error and your phone model is sent to our technical team; it contains no personal details.\n\n## What we record while you work\nThe sales, cash movements, stock requests, shifts and support requests you make, with the time and place they were made, so that the business can count its money and stock and calculate your earnings.\n\n## Notifications\nTo send you messages we keep the token of your phone for push notifications, and your notification choices. You can switch non-essential notifications off in Notification Settings.\n\n## Why we use it\nTo run the business, pay you correctly, keep accounts secure and answer your support requests. We do not sell your data and we do not use it for advertising.\n\n## Who can see it\nAdministrators and managers at LUTIANO, within what their role needs. Service providers we use to send SMS and push notifications receive only what is needed to deliver the message.\n\n## How long we keep it\nBusiness records are kept as long as the law and the accounts require. Read notifications are removed after 90 days. When you leave, your account is closed and personal details not needed for the records can be removed on request.\n\n## Your choices\nYou can change your name, phone and photo in your profile, change the app language, and ask us through Help & Support to see, correct or delete your personal data.\n\n## Security\nYour session is kept in your phone\'s secure storage and ends when you sign out. Sign-in codes and passwords are never shown in the app.\n\n## Contact\nUse Help & Support in your profile for any privacy question.'**
  String get privacyBody;

  /// No description provided for @salesCampaigns.
  ///
  /// In en, this message translates to:
  /// **'Sales Campaigns'**
  String get salesCampaigns;

  /// No description provided for @salesCampaignsSub.
  ///
  /// In en, this message translates to:
  /// **'Extra bonuses when your sales reach a tier'**
  String get salesCampaignsSub;

  /// No description provided for @noCampaigns.
  ///
  /// In en, this message translates to:
  /// **'No campaigns'**
  String get noCampaigns;

  /// No description provided for @noCampaignsMessage.
  ///
  /// In en, this message translates to:
  /// **'When LUTIANO runs a sales campaign for you, it will show here.'**
  String get noCampaignsMessage;

  /// No description provided for @campaignReward.
  ///
  /// In en, this message translates to:
  /// **'Bonus now'**
  String get campaignReward;

  /// No description provided for @yourProgress.
  ///
  /// In en, this message translates to:
  /// **'Your progress'**
  String get yourProgress;

  /// No description provided for @campaignNetSales.
  ///
  /// In en, this message translates to:
  /// **'Your net sales'**
  String get campaignNetSales;

  /// No description provided for @nextTierHint.
  ///
  /// In en, this message translates to:
  /// **'Reach {amount} in net sales to earn a bonus of {bonus}.'**
  String nextTierHint(String amount, String bonus);

  /// No description provided for @allTiersReached.
  ///
  /// In en, this message translates to:
  /// **'You have reached every tier. Well done!'**
  String get allTiersReached;

  /// No description provided for @campaignTiersTitle.
  ///
  /// In en, this message translates to:
  /// **'Tiers'**
  String get campaignTiersTitle;

  /// No description provided for @tierBonus.
  ///
  /// In en, this message translates to:
  /// **'Bonus {bonus}'**
  String tierBonus(String bonus);

  /// No description provided for @campaignBonusPaid.
  ///
  /// In en, this message translates to:
  /// **'Bonus paid: {bonus}'**
  String campaignBonusPaid(String bonus);

  /// No description provided for @campaignBonusNote.
  ///
  /// In en, this message translates to:
  /// **'The bonus of the highest tier you reach is added to your earnings after the campaign ends. Refunds reduce your net sales.'**
  String get campaignBonusNote;

  /// No description provided for @campaignOperation.
  ///
  /// In en, this message translates to:
  /// **'Applies to'**
  String get campaignOperation;

  /// No description provided for @allSellers.
  ///
  /// In en, this message translates to:
  /// **'All sellers'**
  String get allSellers;

  /// No description provided for @customerOrders.
  ///
  /// In en, this message translates to:
  /// **'Customer Orders'**
  String get customerOrders;

  /// No description provided for @customerOrder.
  ///
  /// In en, this message translates to:
  /// **'Customer Order'**
  String get customerOrder;

  /// No description provided for @noCustomerOrders.
  ///
  /// In en, this message translates to:
  /// **'No customer orders'**
  String get noCustomerOrders;

  /// No description provided for @noCustomerOrdersMessage.
  ///
  /// In en, this message translates to:
  /// **'Orders customers place at your sales point appear here.'**
  String get noCustomerOrdersMessage;

  /// No description provided for @ordersTabActive.
  ///
  /// In en, this message translates to:
  /// **'Active'**
  String get ordersTabActive;

  /// No description provided for @ordersTabCompleted.
  ///
  /// In en, this message translates to:
  /// **'Completed'**
  String get ordersTabCompleted;

  /// No description provided for @ordersTabCancelled.
  ///
  /// In en, this message translates to:
  /// **'Cancelled'**
  String get ordersTabCancelled;

  /// No description provided for @orderStatusPlaced.
  ///
  /// In en, this message translates to:
  /// **'New'**
  String get orderStatusPlaced;

  /// No description provided for @orderStatusAccepted.
  ///
  /// In en, this message translates to:
  /// **'Preparing'**
  String get orderStatusAccepted;

  /// No description provided for @orderStatusReady.
  ///
  /// In en, this message translates to:
  /// **'Ready'**
  String get orderStatusReady;

  /// No description provided for @orderStatusCollected.
  ///
  /// In en, this message translates to:
  /// **'Completed'**
  String get orderStatusCollected;

  /// No description provided for @orderStatusRejected.
  ///
  /// In en, this message translates to:
  /// **'Declined'**
  String get orderStatusRejected;

  /// No description provided for @orderStatusCancelled.
  ///
  /// In en, this message translates to:
  /// **'Cancelled'**
  String get orderStatusCancelled;

  /// No description provided for @orderStatusExpired.
  ///
  /// In en, this message translates to:
  /// **'Expired'**
  String get orderStatusExpired;

  /// No description provided for @acceptOrder.
  ///
  /// In en, this message translates to:
  /// **'Accept Order'**
  String get acceptOrder;

  /// No description provided for @declineOrder.
  ///
  /// In en, this message translates to:
  /// **'Decline Order'**
  String get declineOrder;

  /// No description provided for @declineReasonHint.
  ///
  /// In en, this message translates to:
  /// **'Why? (the customer sees this)'**
  String get declineReasonHint;

  /// No description provided for @markReady.
  ///
  /// In en, this message translates to:
  /// **'Mark as Ready'**
  String get markReady;

  /// No description provided for @handOver.
  ///
  /// In en, this message translates to:
  /// **'Hand Over'**
  String get handOver;

  /// No description provided for @handOverMessage.
  ///
  /// In en, this message translates to:
  /// **'Collect {total} in cash and hand the order over. This records the sale.'**
  String handOverMessage(String total);

  /// No description provided for @cashReceivedLabel.
  ///
  /// In en, this message translates to:
  /// **'Cash received'**
  String get cashReceivedLabel;

  /// No description provided for @confirmHandOver.
  ///
  /// In en, this message translates to:
  /// **'Confirm Hand Over'**
  String get confirmHandOver;

  /// No description provided for @orderHandedOver.
  ///
  /// In en, this message translates to:
  /// **'Order handed over and recorded as a sale.'**
  String get orderHandedOver;

  /// No description provided for @customerNote.
  ///
  /// In en, this message translates to:
  /// **'Customer note'**
  String get customerNote;

  /// No description provided for @orderItems.
  ///
  /// In en, this message translates to:
  /// **'Items'**
  String get orderItems;

  /// No description provided for @orderProgress.
  ///
  /// In en, this message translates to:
  /// **'Progress'**
  String get orderProgress;

  /// No description provided for @ntNewOrder.
  ///
  /// In en, this message translates to:
  /// **'New Order'**
  String get ntNewOrder;

  /// No description provided for @scanReward.
  ///
  /// In en, this message translates to:
  /// **'Scan Reward'**
  String get scanReward;

  /// No description provided for @rewardFor.
  ///
  /// In en, this message translates to:
  /// **'Reward for {name}'**
  String rewardFor(String name);

  /// No description provided for @takeOffPercent.
  ///
  /// In en, this message translates to:
  /// **'Take {percent}% off the customer\'s bill.'**
  String takeOffPercent(int percent);

  /// No description provided for @fromThisStock.
  ///
  /// In en, this message translates to:
  /// **'Comes from this sales point\'s stock.'**
  String get fromThisStock;

  /// No description provided for @rewardAlreadyUsed.
  ///
  /// In en, this message translates to:
  /// **'This reward was already used.'**
  String get rewardAlreadyUsed;

  /// No description provided for @rewardExpiredCode.
  ///
  /// In en, this message translates to:
  /// **'This reward has expired.'**
  String get rewardExpiredCode;

  /// No description provided for @giveReward.
  ///
  /// In en, this message translates to:
  /// **'Give Reward'**
  String get giveReward;

  /// No description provided for @rewardGiven.
  ///
  /// In en, this message translates to:
  /// **'Reward given.'**
  String get rewardGiven;
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

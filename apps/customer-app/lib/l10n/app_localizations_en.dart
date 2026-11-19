// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appTitle => 'LUTIANO Coffee';

  @override
  String get coffee => 'Coffee';

  @override
  String get tagline => 'Premium coffee, closer to you.';

  @override
  String get welcomeHeadline => 'Great Coffee, Closer to You';

  @override
  String get welcomeSubtitle =>
      'Premium coffee from our points of sale, mobile baristas and your favourite markets.';

  @override
  String get getStarted => 'Get Started';

  @override
  String get alreadyHaveAccount => 'Already have an account?';

  @override
  String get logIn => 'Log In';

  @override
  String get signUp => 'Sign Up';

  @override
  String get noAccount => 'Don\'t have an account?';

  @override
  String get logInToYourAccount => 'Log In to Your Account';

  @override
  String get accessYourOrders => 'Access your orders, rewards and more.';

  @override
  String get sessionEnded => 'Your session ended. Please log in again.';

  @override
  String get phoneNumber => 'Phone number';

  @override
  String get enterValidPhone =>
      'Enter a Cameroonian mobile number (6XX XX XX XX).';

  @override
  String get password => 'Password';

  @override
  String get enterYourPassword => 'Enter your password.';

  @override
  String get passwordOptional => 'Password (optional, 10+ characters)';

  @override
  String get passwordTooShort => 'Use at least 10 characters.';

  @override
  String get passwordsDoNotMatch => 'The passwords do not match.';

  @override
  String get confirmPassword => 'Confirm password';

  @override
  String get showPassword => 'Show password';

  @override
  String get hidePassword => 'Hide password';

  @override
  String get signingIn => 'Logging in…';

  @override
  String get sendMeACode => 'Send me a code';

  @override
  String get forgotPassword => 'Forgot password?';

  @override
  String get or => 'or';

  @override
  String get continueWithGoogle => 'Continue with Google';

  @override
  String get continueWithApple => 'Continue with Apple';

  @override
  String get createYourAccount => 'Create Your Account';

  @override
  String get joinLutiano =>
      'Join LUTIANO Coffee and enjoy great coffee and rewards.';

  @override
  String get fullName => 'Full name';

  @override
  String get enterYourName => 'Enter your name.';

  @override
  String get emailOptional => 'Email (optional)';

  @override
  String get enterValidEmail => 'Enter a valid email address.';

  @override
  String get iAgreeTo => 'I agree to the';

  @override
  String get and => 'and';

  @override
  String get iAgreeToTermsAndPrivacy =>
      'I agree to the Terms of Use and the Privacy Policy';

  @override
  String get acceptTermsToContinue => 'Accept the terms to continue.';

  @override
  String get createAccount => 'Create Account';

  @override
  String get pleaseWait => 'Please wait…';

  @override
  String get verifyAccount => 'Verify Account';

  @override
  String get verifyYourPhone => 'Verify Your Phone';

  @override
  String enterCodeSentTo(String phone) {
    return 'Enter the 6-digit code we sent to $phone.';
  }

  @override
  String get verify => 'Verify';

  @override
  String get verifying => 'Verifying…';

  @override
  String get resendCode => 'Resend code';

  @override
  String resendIn(int seconds) {
    return 'Resend code in ${seconds}s';
  }

  @override
  String get codeSent => 'A new code was sent.';

  @override
  String get resetPassword => 'Reset Password';

  @override
  String get weWillSendCode => 'We will send a code to your phone.';

  @override
  String get enterCodeAndNewPassword =>
      'Enter the code we sent and choose a new password.';

  @override
  String get sendCode => 'Send code';

  @override
  String get sixDigitCode => '6-digit code';

  @override
  String get enterTheCode => 'Enter the 6-digit code.';

  @override
  String get newPassword => 'New password';

  @override
  String get passwordChanged => 'Your password was changed.';

  @override
  String get oneLastStep => 'One Last Step';

  @override
  String get addYourPhoneWhy =>
      'Orders and rewards go with your phone number. We will confirm it by SMS.';

  @override
  String get home => 'Home';

  @override
  String get menu => 'Menu';

  @override
  String get orders => 'Orders';

  @override
  String get rewards => 'Rewards';

  @override
  String get profile => 'Profile';

  @override
  String hello(String name) {
    return 'Hello, $name';
  }

  @override
  String get coffeeLover => 'Coffee lover';

  @override
  String get scanToOrder => 'Scan to Order';

  @override
  String get scanToOrderHint => 'Scan the QR code at a LUTIANO sales point.';

  @override
  String get findASalesPoint => 'Find a sales point';

  @override
  String get recentSalesPoints => 'Recent sales points';

  @override
  String get categories => 'Categories';

  @override
  String get popularDrinks => 'Popular Drinks';

  @override
  String get seeAll => 'See all';

  @override
  String get cart => 'Cart';

  @override
  String get searchMenu => 'Search the menu…';

  @override
  String get noProductsFound => 'Nothing found';

  @override
  String get tryAnotherSearch => 'Try another search or category.';

  @override
  String get chooseSalesPointHint =>
      'Choose a sales point to see what is available and order.';

  @override
  String orderingFrom(String name) {
    return 'Ordering from $name';
  }

  @override
  String get choose => 'Choose';

  @override
  String get change => 'Change';

  @override
  String fromPrice(String price) {
    return 'from $price';
  }

  @override
  String get unavailableHere => 'Not available at this sales point right now.';

  @override
  String get addToFavourites => 'Add to favourites';

  @override
  String get removeFromFavourites => 'Remove from favourites';

  @override
  String get size => 'Size';

  @override
  String get addOnsOptional => 'Add-ons (optional)';

  @override
  String addToCartPrice(String price) {
    return 'Add to Cart ($price)';
  }

  @override
  String get addedToCart => 'Added to cart!';

  @override
  String addedToCartMessage(String name) {
    return '$name was added to your cart.';
  }

  @override
  String get continueShopping => 'Continue shopping';

  @override
  String viewCart(int count) {
    return 'View cart ($count)';
  }

  @override
  String get yourCart => 'Your Cart';

  @override
  String get clear => 'Clear';

  @override
  String get cartEmpty => 'Your cart is empty';

  @override
  String get cartEmptyMessage =>
      'Browse the menu and add your favourite coffee.';

  @override
  String get browseMenu => 'Browse Menu';

  @override
  String get salesPoint => 'Sales point';

  @override
  String get total => 'Total';

  @override
  String get proceedToCheckout => 'Proceed to Checkout';

  @override
  String get favourites => 'Favourites';

  @override
  String get noFavourites => 'No favourites yet';

  @override
  String get noFavouritesMessage =>
      'Tap the heart on a product to keep it here.';

  @override
  String get salesPoints => 'Sales Points';

  @override
  String get searchSalesPoints => 'Search sales points…';

  @override
  String get noSalesPoints => 'No sales points found';

  @override
  String get pointAtQr => 'Point the camera at the sales point\'s QR code.';

  @override
  String get enterCodeManually => 'Enter code manually';

  @override
  String get salesPointCodeHint => 'Code (LUTIANO:OP:…)';

  @override
  String get cancel => 'Cancel';

  @override
  String get continueLabel => 'Continue';

  @override
  String get cameraUnavailable =>
      'The camera is not available. You can enter the code instead.';

  @override
  String get codeNotRecognised => 'This code was not recognised.';

  @override
  String get openNow => 'Open now';

  @override
  String get closedNow => 'Closed now';

  @override
  String get openingHours => 'Opening hours';

  @override
  String get paymentMethods => 'Accepted payments';

  @override
  String get cashAtSalesPoint => 'Cash at the sales point';

  @override
  String get selectThisSalesPoint => 'Select this sales point';

  @override
  String get orderHere => 'Order Here';

  @override
  String get typeFixed => 'Shop';

  @override
  String get typeMobile => 'Mobile barista';

  @override
  String get typeMarket => 'Market stand';

  @override
  String get monday => 'Monday';

  @override
  String get tuesday => 'Tuesday';

  @override
  String get wednesday => 'Wednesday';

  @override
  String get thursday => 'Thursday';

  @override
  String get friday => 'Friday';

  @override
  String get saturday => 'Saturday';

  @override
  String get sunday => 'Sunday';

  @override
  String get accountDetails => 'Account Details';

  @override
  String get securityAndLogin => 'Security & Login';

  @override
  String get language => 'Language';

  @override
  String get followPhone => 'Follow the phone';

  @override
  String get aboutLutiano => 'About LUTIANO';

  @override
  String get termsOfUse => 'Terms of Use';

  @override
  String get privacyPolicy => 'Privacy Policy';

  @override
  String get licenses => 'Licenses';

  @override
  String versionValue(String version) {
    return 'Version $version';
  }

  @override
  String get logOut => 'Log Out';

  @override
  String get logOutQuestion => 'Log out?';

  @override
  String get logOutConfirm =>
      'Are you sure you want to log out of your account?';

  @override
  String get profileSaved => 'Profile saved.';

  @override
  String get saveChanges => 'Save Changes';

  @override
  String get save => 'Save';

  @override
  String get changePhoneNumber => 'Change phone number';

  @override
  String get newPhoneNumber => 'New phone number';

  @override
  String get receiveOffers => 'Receive offers';

  @override
  String get receiveOffersHint => 'News, offers and campaigns by notification.';

  @override
  String get changePassword => 'Change password';

  @override
  String get setPassword => 'Set a password';

  @override
  String get currentPassword => 'Current password';

  @override
  String get newPasswordMin => 'New password (10+ characters)';

  @override
  String get twoStepVerification => 'Two-step verification';

  @override
  String get twoStepHint => 'Ask for an SMS code after your password.';

  @override
  String get twoStepNeedsPassword => 'Set a password first.';

  @override
  String get settingSaved => 'Setting saved.';

  @override
  String get signOutOtherDevices => 'Sign out of other devices';

  @override
  String get signedOutEverywhere => 'Other devices were signed out.';

  @override
  String get deleteAccount => 'Delete Account';

  @override
  String get deleteAccountQuestion => 'Delete your account?';

  @override
  String get deleteAccountWarning =>
      'Your personal details are removed and you are signed out. This cannot be undone.';

  @override
  String get legalDraftNotice =>
      'Draft text, to be reviewed by LUTIANO\'s legal adviser before the app is released.';

  @override
  String get termsBody =>
      '## Who these terms are for\nThese terms apply to customers of LUTIANO Coffee who use this app.\n\n## Your account\nYour account is personal. Keep your password and codes private. You are responsible for what is done with your account.\n\n## Orders\nAn order is a request to a LUTIANO sales point. It is accepted by the seller and paid at the sales point. Prices and availability are those shown at the sales point when you order.\n\n## Rewards\nPoints and rewards follow the rules shown in the app. They have no cash value and can expire.\n\n## Acceptable use\nUse the app for your own purchases only. Do not try to reach data that is not yours or disturb the service.\n\n## Closing your account\nYou can delete your account from the app at any time. LUTIANO may suspend an account that does not respect these terms.\n\n## Changes and contact\nThese terms may change; the app will show the new version.';

  @override
  String get privacyBody =>
      '## What we keep about you\nYour name, phone number, email address if you give one, language, the orders you place and the points you earn. If you sign in with Google or Apple we keep the identifier they give us.\n\n## Why we use it\nTo prepare your orders, give you your rewards, keep your account safe and answer your requests. We do not sell your data.\n\n## Notifications\nWe keep your phone\'s notification token to tell you about your orders and rewards. Offers are sent only if you agreed.\n\n## Who can see it\nLUTIANO staff, within what their role needs, and the services we use to send SMS and notifications.\n\n## How long we keep it\nOrder records are kept as long as the accounts require. When you delete your account your personal details are removed.\n\n## Your choices\nYou can change your details, language and consent in the app, and delete your account.\n\n## Security\nYour session is kept in your phone\'s secure storage and ends when you log out.';

  @override
  String get checkout => 'Checkout';

  @override
  String get orderReview => 'Order review';

  @override
  String get totalToPay => 'Total to pay';

  @override
  String get paymentMethod => 'Payment method';

  @override
  String get payAtTheCounter => 'Pay when you collect your order';

  @override
  String get comingSoon => 'Coming soon';

  @override
  String get noteForTheSeller => 'Note for the seller';

  @override
  String get noteHint => 'e.g. less sugar, extra hot';

  @override
  String get confirmedBySeller =>
      'Your order is confirmed once the seller accepts it. You pay at the sales point.';

  @override
  String get placeOrder => 'Place Order';

  @override
  String get placingOrder => 'Sending…';

  @override
  String get orderSent => 'Order Placed!';

  @override
  String get orderSentMessage =>
      'Your order was sent to the sales point. You will be notified when it changes.';

  @override
  String get orderNumber => 'Order number';

  @override
  String get viewOrderDetails => 'View Order Details';

  @override
  String get myOrders => 'My Orders';

  @override
  String get filterPending => 'Pending';

  @override
  String get filterCompleted => 'Completed';

  @override
  String get filterCancelled => 'Cancelled';

  @override
  String get noOrdersYet => 'No orders yet';

  @override
  String get noOrdersMessage =>
      'You haven\'t placed any orders yet. Browse the menu and order your favourite coffee.';

  @override
  String get orderDetails => 'Order Details';

  @override
  String get items => 'Items';

  @override
  String get orderTracking => 'Order Tracking';

  @override
  String get viewReceipt => 'View Receipt';

  @override
  String get preparingReceipt => 'Preparing your receipt…';

  @override
  String get buyAgain => 'Buy Again';

  @override
  String get cancelOrder => 'Cancel Order';

  @override
  String get cancelOrderQuestion => 'Cancel this order?';

  @override
  String get cancelOrderMessage =>
      'The sales point has not accepted it yet, so you can cancel it now.';

  @override
  String get keepOrder => 'Keep Order';

  @override
  String get nothingToReorder =>
      'None of these items can be ordered right now.';

  @override
  String get someItemsUnavailable =>
      'Some items are no longer available and were left out.';

  @override
  String get orderPlaced => 'Order placed';

  @override
  String get orderBeingPrepared => 'Being prepared';

  @override
  String get orderReadyStatus => 'Ready';

  @override
  String get orderCompleted => 'Completed';

  @override
  String get orderDeclined => 'Declined';

  @override
  String get orderCancelledStatus => 'Cancelled';

  @override
  String get orderExpired => 'Expired';

  @override
  String get stepPlaced => 'Your order has been received.';

  @override
  String get stepAccepted => 'The sales point accepted it and is preparing it.';

  @override
  String get stepReady => 'Your order is ready to collect.';

  @override
  String get stepCompleted => 'Handed over and paid. Thank you!';

  @override
  String get stepDeclined => 'The sales point could not accept this order.';

  @override
  String get stepCancelled => 'This order was cancelled.';

  @override
  String get stepExpired => 'Nobody handled this order in time.';

  @override
  String get yourPointsBalance => 'Your points balance';

  @override
  String pointsValue(int points) {
    return '$points pts';
  }

  @override
  String get pts => 'pts';

  @override
  String get noPointsYet => 'No points yet';

  @override
  String get noPointsYetMessage =>
      'Place an order or show your loyalty QR at a sales point to start earning points.';

  @override
  String pointsExpiringSoon(int points, int days) {
    return '$points pts expire within $days days';
  }

  @override
  String membershipTier(String tier) {
    return '$tier Member';
  }

  @override
  String spendToReachTier(String amount, String tier) {
    return 'Spend $amount more this year to reach $tier';
  }

  @override
  String get topTierReached => 'You have reached the top tier. Thank you!';

  @override
  String get showMyLoyaltyQr => 'Show my loyalty QR';

  @override
  String get myLoyaltyQr => 'My Loyalty QR';

  @override
  String get loyaltyQrHint =>
      'Show this code to the seller when you pay in store, so the purchase earns points.';

  @override
  String get rewardsCatalogue => 'Rewards Catalogue';

  @override
  String get myRewards => 'My Rewards';

  @override
  String rewardsReady(int count) {
    return '$count ready to use';
  }

  @override
  String get pointsHistory => 'Points History';

  @override
  String get membershipTiers => 'Membership Tiers';

  @override
  String get howPointsWork => 'How Points Work';

  @override
  String get noRewards => 'No rewards yet';

  @override
  String get noRewardsMessage => 'New rewards will appear here.';

  @override
  String get blockedMinPoints => 'You need more points to redeem.';

  @override
  String get blockedNotEnough => 'Not enough points yet.';

  @override
  String blockedPurchases(int count) {
    return 'Needs at least $count purchases.';
  }

  @override
  String get blockedSpend => 'Needs more total spending.';

  @override
  String redeemCost(int points) {
    return 'Costs $points points';
  }

  @override
  String get redeemHint =>
      'The reward goes to your wallet. Show its QR code to the seller to get it.';

  @override
  String get confirmRedemption => 'Redeem';

  @override
  String get rewardRedeemed => 'Reward Redeemed!';

  @override
  String get myReward => 'My Reward';

  @override
  String get rewardQr => 'Reward QR code';

  @override
  String get showThisToSeller => 'Show this code to the seller.';

  @override
  String validUntilDate(String date) {
    return 'Valid until $date';
  }

  @override
  String rewardUsedOn(String date) {
    return 'Used $date';
  }

  @override
  String get rewardExpiredMessage =>
      'This reward has expired or is no longer valid.';

  @override
  String get stateAvailable => 'Available';

  @override
  String get stateUsed => 'Used';

  @override
  String get stateExpired => 'Expired';

  @override
  String get noRewardsHere => 'Nothing here';

  @override
  String get noRewardsHereMessage => 'Rewards you redeem appear here.';

  @override
  String get typeEarned => 'Earned';

  @override
  String get typeUsed => 'Used';

  @override
  String get pointsEarned => 'Points earned';

  @override
  String get pointsUsed => 'Points used';

  @override
  String get pointsExpired => 'Points expired';

  @override
  String get pointsReturned => 'Points returned';

  @override
  String get pointsAdjusted => 'Points adjusted';

  @override
  String get tiersHint =>
      'Your tier follows what you spent over the last 12 months.';

  @override
  String get currentTier => 'Current';

  @override
  String get startingTier => 'Where everyone starts';

  @override
  String tierNeeds(String amount) {
    return 'Spend $amount a year';
  }

  @override
  String tierEarns(int percent) {
    return 'Earn $percent% of the base points';
  }

  @override
  String get earnPoints => 'Earn points';

  @override
  String earnRule(int points, String amount) {
    return 'Get $points points for every $amount spent.';
  }

  @override
  String get usePoints => 'Use points';

  @override
  String useRule(int points) {
    return 'Redeem rewards from $points points.';
  }

  @override
  String get pointsValidity => 'Validity';

  @override
  String validityRule(int months) {
    return 'Points are valid for $months months from the day they are earned.';
  }

  @override
  String get currentPromotions => 'Current promotions';

  @override
  String get pointsEstimate => 'Points estimate';

  @override
  String get purchaseAmount => 'Purchase amount';

  @override
  String get calculate => 'Calculate';

  @override
  String get estimatedPoints => 'Estimated points';

  @override
  String get estimateNote =>
      'The points you actually earn follow the rules and promotions at the time of the purchase.';

  @override
  String get notifications => 'Notifications';

  @override
  String get markAllRead => 'Mark all as read';

  @override
  String get notificationSettings => 'Notification Settings';

  @override
  String get noNotifications => 'No notifications';

  @override
  String get noNotificationsMessage =>
      'Updates about your orders, rewards and offers appear here.';

  @override
  String get close => 'Close';

  @override
  String get catOrders => 'Orders';

  @override
  String get catLoyalty => 'Loyalty & Rewards';

  @override
  String get catPromotions => 'Promotions & Offers';

  @override
  String get catSystem => 'System & News';

  @override
  String get alwaysOn => 'Always on';

  @override
  String get pushOnPhone => 'Push on this phone';

  @override
  String get pushNotOnThisPhone =>
      'Push notifications are not set up on this phone, so messages arrive in the app only.';

  @override
  String get hintLoyalty => 'Points, tier updates, rewards';

  @override
  String get hintPromotions => 'New products, discounts, campaigns';

  @override
  String get hintSystem => 'App updates and announcements';

  @override
  String get ntOrderAccepted => 'Order accepted';

  @override
  String get ntOrderDeclined => 'Order declined';

  @override
  String get ntOrderReady => 'Order ready';

  @override
  String get ntOrderComplete => 'Order complete';

  @override
  String get ntOrderCancelled => 'Order cancelled';

  @override
  String get ntOrderExpired => 'Order expired';

  @override
  String get ntRewardRedeemed => 'Reward redeemed';

  @override
  String get ntRewardUsed => 'Reward used';

  @override
  String get ntRewardExpired => 'Reward expired';

  @override
  String get ntSupportReply => 'Support reply';

  @override
  String get ntLutiano => 'LUTIANO Coffee';

  @override
  String get helpAndSupport => 'Help & Support';

  @override
  String get contactSupport => 'Contact Support';

  @override
  String get myRequests => 'My Requests';

  @override
  String get faq => 'Frequently Asked Questions';

  @override
  String get supportRequest => 'Support Request';

  @override
  String get teamReply => 'Reply from the team';

  @override
  String get noReplyYet =>
      'The team has not replied yet. You will be notified.';

  @override
  String get noRequests => 'No requests';

  @override
  String get noRequestsMessage => 'Your requests to the team appear here.';

  @override
  String get requestSent => 'Request sent.';

  @override
  String get requestCategory => 'What is it about?';

  @override
  String get requestTitle => 'Title';

  @override
  String get requestDescription => 'Describe the problem or question';

  @override
  String get enterATitle => 'Enter a short title.';

  @override
  String get describeTheProblem => 'Tell us a little more.';

  @override
  String get addPhotoOptional => 'Add a photo (optional)';

  @override
  String get submitRequest => 'Send Request';

  @override
  String get reqOrder => 'Order & payment';

  @override
  String get reqRewards => 'Points & rewards';

  @override
  String get reqAccount => 'My account';

  @override
  String get reqIssue => 'Report an issue';

  @override
  String get reqQuestion => 'A question';

  @override
  String get reqIdea => 'Suggest an improvement';

  @override
  String get reqOpen => 'Open';

  @override
  String get reqInProgress => 'In progress';

  @override
  String get reqResolved => 'Resolved';

  @override
  String get faqQ1 => 'How do I order?';

  @override
  String get faqA1 =>
      'Choose a sales point (scan its QR code or pick it from the list), add products to your cart and place your order. A seller accepts it and you pay in cash when you collect it.';

  @override
  String get faqQ2 => 'How do I earn points?';

  @override
  String get faqA2 =>
      'Show your loyalty QR code to the seller when you pay, or order in the app. Points are added when the order is handed over.';

  @override
  String get faqQ3 => 'How do I use a reward?';

  @override
  String get faqA3 =>
      'Redeem it from the Rewards tab, then show its QR code to the seller at the sales point.';

  @override
  String get faqQ4 => 'Can I cancel an order?';

  @override
  String get faqA4 =>
      'Yes, while the sales point has not accepted it yet. After that, contact the sales point or write to support.';

  @override
  String get faqQ5 => 'I forgot my password.';

  @override
  String get faqA5 =>
      'On the log in screen choose Forgot password, or log in with a code sent to your phone.';
}

import 'package:material_ui/material_ui.dart';

/// Colours from the LUTIANO Seller design pack.
class AppColors {
  AppColors._();
  static const navy = Color(0xFF0B1F3A);
  static const navyDeep = Color(0xFF071A35);
  static const gold = Color(0xFFF5B800);
  static const goldSoft = Color(0xFFFFF3CC);
  static const background = Color(0xFFF5F7FA);
  static const ink = Color(0xFF10213D);
  static const muted = Color(0xFF78869A);
  static const line = Color(0xFFE6EAF0);
  static const green = Color(0xFF12805C);
  static const greenSoft = Color(0xFFDDF5EA);
  static const red = Color(0xFFC0392B);
  static const redSoft = Color(0xFFFDE8E5);
  static const amber = Color(0xFFB26B00);
  static const amberSoft = Color(0xFFFFF1D6);
  static const blue = Color(0xFF1F4FD1);
  static const blueSoft = Color(0xFFE3EBFF);
  static const purple = Color(0xFF6B2FA3);
  static const purpleSoft = Color(0xFFF0E6FB);
  static const white70 = Color(0xB3FFFFFF);
}

class AppTheme {
  AppTheme._();

  static ThemeData light() {
    final scheme = ColorScheme.fromSeed(
        seedColor: AppColors.gold,
        primary: AppColors.navy,
        secondary: AppColors.gold,
        surface: Colors.white);
    final radius = BorderRadius.circular(12);
    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: AppColors.background,
      appBarTheme: const AppBarTheme(
        backgroundColor: Colors.white,
        foregroundColor: AppColors.ink,
        elevation: 0,
        centerTitle: true,
        titleTextStyle: TextStyle(
            color: AppColors.ink, fontSize: 18, fontWeight: FontWeight.w800),
      ),
      cardTheme: CardThemeData(
        color: Colors.white,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
            borderRadius: radius,
            side: const BorderSide(color: AppColors.line)),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: AppColors.gold,
          foregroundColor: AppColors.navy,
          minimumSize: const Size.fromHeight(52),
          textStyle: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16),
          shape: RoundedRectangleBorder(borderRadius: radius),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: AppColors.navy,
          minimumSize: const Size.fromHeight(50),
          side: const BorderSide(color: AppColors.line),
          textStyle: const TextStyle(fontWeight: FontWeight.w700),
          shape: RoundedRectangleBorder(borderRadius: radius),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: Colors.white,
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
        border: OutlineInputBorder(
            borderRadius: radius,
            borderSide: const BorderSide(color: AppColors.line)),
        enabledBorder: OutlineInputBorder(
            borderRadius: radius,
            borderSide: const BorderSide(color: AppColors.line)),
        focusedBorder: OutlineInputBorder(
            borderRadius: radius,
            borderSide: const BorderSide(color: AppColors.gold, width: 2)),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: Colors.white,
        indicatorColor: AppColors.goldSoft,
        labelTextStyle: WidgetStateProperty.resolveWith((s) => TextStyle(
            fontSize: 12,
            fontWeight: s.contains(WidgetState.selected)
                ? FontWeight.w700
                : FontWeight.w500,
            color: s.contains(WidgetState.selected)
                ? AppColors.navy
                : AppColors.muted)),
      ),
      snackBarTheme: const SnackBarThemeData(
          behavior: SnackBarBehavior.floating, backgroundColor: AppColors.navy),
    );
  }
}

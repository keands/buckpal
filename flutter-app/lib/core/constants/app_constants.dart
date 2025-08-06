import 'package:flutter/material.dart';

class AppConstants {
  static const String appName = 'BuckPal';
  static const String baseUrl = 'http://localhost:8080/api';
  
  // Colors
  static const Color primaryColor = Color(0xFF2E7D32);
  static const Color secondaryColor = Color(0xFF4CAF50);
  static const Color accentColor = Color(0xFF81C784);
  static const Color errorColor = Color(0xFFE57373);
  static const Color warningColor = Color(0xFFFFB74D);
  static const Color infoColor = Color(0xFF64B5F6);
  static const Color successColor = Color(0xFF81C784);
  
  // Text Styles
  static const TextStyle headingStyle = TextStyle(
    fontSize: 24,
    fontWeight: FontWeight.bold,
    color: primaryColor,
  );
  
  static const TextStyle subHeadingStyle = TextStyle(
    fontSize: 18,
    fontWeight: FontWeight.w600,
    color: Colors.black87,
  );
  
  static const TextStyle bodyStyle = TextStyle(
    fontSize: 14,
    color: Colors.black87,
  );
  
  // Spacing
  static const double defaultPadding = 16.0;
  static const double largePadding = 24.0;
  static const double smallPadding = 8.0;
  
  // Border Radius
  static const double defaultRadius = 8.0;
  static const double largeRadius = 12.0;
  
  // Animation Durations
  static const Duration shortDuration = Duration(milliseconds: 200);
  static const Duration mediumDuration = Duration(milliseconds: 300);
  static const Duration longDuration = Duration(milliseconds: 500);
  
  // API Endpoints
  static const String loginEndpoint = '/auth/signin';
  static const String registerEndpoint = '/auth/signup';
  static const String accountsEndpoint = '/accounts';
  static const String transactionsEndpoint = '/transactions';
  static const String csvImportEndpoint = '/transactions/import-csv';
  
  // Storage Keys
  static const String tokenKey = 'auth_token';
  static const String userKey = 'user_data';
  static const String themeKey = 'theme_mode';
  
  // Transaction Types
  static const String incomeType = 'INCOME';
  static const String expenseType = 'EXPENSE';
  static const String transferType = 'TRANSFER';
  
  // Account Types
  static const String checkingType = 'CHECKING';
  static const String savingsType = 'SAVINGS';
  static const String creditCardType = 'CREDIT_CARD';
  static const String investmentType = 'INVESTMENT';
  static const String loanType = 'LOAN';
  static const String otherType = 'OTHER';
}
import '../repositories/auth_repository.dart';

class RegisterUseCase {
  final AuthRepository repository;

  RegisterUseCase(this.repository);

  Future<void> call({
    required String firstName,
    required String lastName,
    required String email,
    required String password,
    required String confirmPassword,
  }) async {
    // Validation
    if (firstName.isEmpty || lastName.isEmpty || email.isEmpty || password.isEmpty) {
      throw Exception('All fields are required');
    }

    if (firstName.length < 2 || lastName.length < 2) {
      throw Exception('First name and last name must be at least 2 characters long');
    }

    if (!_isValidEmail(email)) {
      throw Exception('Please enter a valid email address');
    }

    if (password.length < 6) {
      throw Exception('Password must be at least 6 characters long');
    }

    if (password != confirmPassword) {
      throw Exception('Passwords do not match');
    }

    try {
      await repository.register(firstName, lastName, email, password);
    } catch (e) {
      throw Exception('Registration failed: ${e.toString()}');
    }
  }

  bool _isValidEmail(String email) {
    return RegExp(r'^[^@]+@[^@]+\.[^@]+$').hasMatch(email);
  }
}
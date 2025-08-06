import '../entities/user.dart';

abstract class AuthRepository {
  Future<User> login(String email, String password);
  Future<void> register(String firstName, String lastName, String email, String password);
  Future<void> logout();
  Future<bool> isLoggedIn();
  Future<User?> getCurrentUser();
  Future<void> saveUser(User user);
}
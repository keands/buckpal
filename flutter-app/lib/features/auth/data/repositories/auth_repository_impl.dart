import '../../../../core/constants/app_constants.dart';
import '../../../../core/network/dio_client.dart';
import '../../../../core/storage/secure_storage.dart';
import '../../domain/entities/user.dart';
import '../../domain/repositories/auth_repository.dart';
import '../models/jwt_response.dart';
import '../models/login_request.dart';
import '../models/register_request.dart';
import '../models/user_model.dart';

class AuthRepositoryImpl implements AuthRepository {
  final DioClient dioClient;
  final SecureStorage secureStorage;

  AuthRepositoryImpl({
    required this.dioClient,
    required this.secureStorage,
  });

  @override
  Future<User> login(String email, String password) async {
    try {
      final loginRequest = LoginRequest(email: email, password: password);
      
      final response = await dioClient.post(
        AppConstants.loginEndpoint,
        data: loginRequest.toJson(),
      );

      final jwtResponse = JwtResponse.fromJson(response.data);
      
      final user = User(
        email: jwtResponse.email,
        firstName: jwtResponse.firstName,
        lastName: jwtResponse.lastName,
        token: jwtResponse.token,
      );

      // Save token to secure storage
      await secureStorage.saveToken(jwtResponse.token);

      return user;
    } catch (e) {
      throw Exception('Login failed: ${e.toString()}');
    }
  }

  @override
  Future<void> register(String firstName, String lastName, String email, String password) async {
    try {
      final registerRequest = RegisterRequest(
        firstName: firstName,
        lastName: lastName,
        email: email,
        password: password,
      );

      await dioClient.post(
        AppConstants.registerEndpoint,
        data: registerRequest.toJson(),
      );
    } catch (e) {
      throw Exception('Registration failed: ${e.toString()}');
    }
  }

  @override
  Future<void> logout() async {
    await secureStorage.clearAll();
  }

  @override
  Future<bool> isLoggedIn() async {
    return await secureStorage.hasToken();
  }

  @override
  Future<User?> getCurrentUser() async {
    try {
      final userData = await secureStorage.getUserData();
      if (userData != null) {
        return UserModel.fromJson(userData).toEntity();
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  @override
  Future<void> saveUser(User user) async {
    final userModel = UserModel.fromEntity(user);
    await secureStorage.saveUserData(userModel.toJson());
  }
}
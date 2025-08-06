import 'package:equatable/equatable.dart';

class User extends Equatable {
  final String email;
  final String firstName;
  final String lastName;
  final String? token;

  const User({
    required this.email,
    required this.firstName,
    required this.lastName,
    this.token,
  });

  String get fullName => '$firstName $lastName';

  User copyWith({
    String? email,
    String? firstName,
    String? lastName,
    String? token,
  }) {
    return User(
      email: email ?? this.email,
      firstName: firstName ?? this.firstName,
      lastName: lastName ?? this.lastName,
      token: token ?? this.token,
    );
  }

  @override
  List<Object?> get props => [email, firstName, lastName, token];

  @override
  String toString() {
    return 'User(email: $email, firstName: $firstName, lastName: $lastName)';
  }
}
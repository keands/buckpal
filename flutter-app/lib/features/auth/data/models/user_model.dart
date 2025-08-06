import 'package:json_annotation/json_annotation.dart';
import '../../domain/entities/user.dart';

part 'user_model.g.dart';

@JsonSerializable()
class UserModel extends User {
  const UserModel({
    required super.email,
    required super.firstName,
    required super.lastName,
    super.token,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) => _$UserModelFromJson(json);

  Map<String, dynamic> toJson() => _$UserModelToJson(this);

  factory UserModel.fromEntity(User user) {
    return UserModel(
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      token: user.token,
    );
  }

  User toEntity() {
    return User(
      email: email,
      firstName: firstName,
      lastName: lastName,
      token: token,
    );
  }
}
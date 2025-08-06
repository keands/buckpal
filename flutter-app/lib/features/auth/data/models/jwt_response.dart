import 'package:json_annotation/json_annotation.dart';

part 'jwt_response.g.dart';

@JsonSerializable()
class JwtResponse {
  @JsonKey(name: 'accessToken')
  final String token;
  final String tokenType;
  final String email;
  final String firstName;
  final String lastName;

  const JwtResponse({
    required this.token,
    required this.tokenType,
    required this.email,
    required this.firstName,
    required this.lastName,
  });

  factory JwtResponse.fromJson(Map<String, dynamic> json) => _$JwtResponseFromJson(json);

  Map<String, dynamic> toJson() => _$JwtResponseToJson(this);
}
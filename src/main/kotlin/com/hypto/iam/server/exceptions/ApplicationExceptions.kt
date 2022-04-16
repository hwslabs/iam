package com.hypto.iam.server.exceptions

class InternalException(s: String, ex: Exception? = null) : Exception(s, ex)

class UnknownException(s: String, ex: Exception? = null) : Exception(s, ex)

class EntityAlreadyExistsException(s: String, ex: Exception? = null) : Exception(s, ex)

class EntityNotFoundException(s: String, ex: Exception? = null) : Exception(s, ex)

class JwtExpiredException(s: String, ex: Exception? = null) : Exception(s, ex)

class InvalidJwtException(s: String, ex: Exception? = null) : Exception(s, ex)

class PolicyFormatException(s: String, ex: Exception? = null) : Exception(s, ex)

class PublicKeyExpiredException(s: String, ex: Exception? = null) : Exception(s, ex)

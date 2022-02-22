package com.hypto.iam.server.exceptions

class InternalException(s: String) : Exception(s)

class EntityAlreadyExistsException(s: String) : Exception(s)

class EntityNotFoundException(s: String) : Exception(s)

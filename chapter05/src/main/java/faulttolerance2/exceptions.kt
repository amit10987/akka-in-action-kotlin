package faulttolerance2

import java.io.File
import java.io.Serializable

class UnexpectedColumnsException(msg: String) : Exception(msg), Serializable

class ParseException(msg: String, val file: File) : Exception(msg), Serializable

class ClosedWatchServiceException(msg: String) : Exception(msg), Serializable

class DbBrokenConnectionException(msg: String) : Exception(msg), Serializable

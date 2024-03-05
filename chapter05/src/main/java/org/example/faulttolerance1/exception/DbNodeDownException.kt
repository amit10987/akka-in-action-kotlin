package org.example.faulttolerance1.exception

import java.io.Serializable

class DbNodeDownException(msg: String) : Exception(msg), Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

package org.example.faulttolerance1.exception

import java.io.Serializable

class DiskError(msg: String) : Error(msg), Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

package repository

import java.sql.Connection
import java.sql.PreparedStatement

interface CargosPerContainerRepository {
    fun addCargo(containerId: String, session: ScalikeJdbcSession)
}

class CargosPerContainerRepositoryImpl : CargosPerContainerRepository {

    override fun addCargo(containerId: String, session: ScalikeJdbcSession) {
        val sql = """
            INSERT INTO cargos_per_container (containerId, cargos) 
            VALUES (?, 1) 
            ON CONFLICT (containerId) DO 
            UPDATE SET cargos = cargos_per_container.cargos + 1
        """
        session.withConnection {
            val ps = it.prepareStatement(sql)
            ps.setString(1, containerId)
            ps.executeUpdate()
        }
    }

}
package projection.db_connection

import akka.japi.function.Function
import akka.projection.jdbc.JdbcSession
import scalikejdbc.ConnectionPool
import scalikejdbc.DB
import scalikejdbc.SettingsProvider
import java.sql.Connection

class ScalikeJdbcSession : JdbcSession {
    private val connectionPool: ConnectionPool = ConnectionPool.get("read-side-connection-pool")
    private val connection = connectionPool.borrow()
    val db: DB = DB.connect(connection, SettingsProvider.default())

    inline fun <R> withSession(block: (ScalikeJdbcSession) -> R): R {
        val session = ScalikeJdbcSession()
        return try {
            block(session)
        } finally {
            session.close()
        }
    }
    override fun rollback() {
        db.rollback()
    }

    override fun commit() {
        db.commit()
    }

    override fun <Result : Any?> withConnection(func: Function<Connection, Result>): Result {
        db.begin()
        return db.withinTxWithConnection { func.apply(it) }
    }

    override fun close() {
        db.close()
    }


}
package repository

import akka.japi.function.Function
import akka.projection.jdbc.JdbcSession
import scalikejdbc.ConnectionPool
import java.sql.Connection

class ScalikeJdbcSession : JdbcSession {

    //private val db: DB = DB.connect(ScalikeJdbcSetup.dataSource().connection, SettingsProvider.default())

    private val connectionPool: ConnectionPool = ConnectionPool.get("read-side-connection-pool")
    private val connection = connectionPool.borrow()
   // private val connection: Connection = ScalikeJdbcSetup.dataSource().connection


    init {
        connection.autoCommit = false;
    }

    override fun rollback() {
        connection.rollback()
    }

    override fun commit() {
        connection.commit()
    }

    override fun <Result> withConnection(func: Function<Connection, Result>): Result {
//        db.begin()
//        return db.withinTxWithConnection { func.apply(db.conn()) }
        return func.apply(connection)
    }

    override fun close() {
        connection.close()
    }
}
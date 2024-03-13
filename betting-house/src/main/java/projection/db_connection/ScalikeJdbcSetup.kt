package projection.db_connection

import akka.actor.typed.ActorSystem
import com.typesafe.config.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import scalikejdbc.ConnectionPool
import scalikejdbc.DataSourceConnectionPool
import scalikejdbc.DataSourceConnectionPoolSettings
import scalikejdbc.DataSourceCloser
import java.sql.Connection

object ScalikeJdbcSetup {

    private var dataSource: HikariDataSource? = null

    fun init(system: ActorSystem<*>) {
        initFromConfig(system.settings().config())
    }

    private fun initFromConfig(config: Config) {
        val jdbcConfig = config.getConfig("jdbc-connection-settings")
        dataSource = buildDataSource(jdbcConfig)
        ConnectionPool.singleton(
            DataSourceConnectionPool(
                dataSource!!,
                DataSourceConnectionPoolSettings(jdbcConfig.getString("driver")),
                HikariCloser(dataSource!!)
            )
        )
    }

    fun getConnection(): Connection {
        return dataSource!!.connection
    }

    private fun buildDataSource(config: Config): HikariDataSource {
        val hikariConfig = HikariConfig()
        hikariConfig.poolName = "read-side-bet-connection-pool"
        hikariConfig.maximumPoolSize = config.getInt("connection-pool.max-pool-size")
        hikariConfig.connectionTimeout = config.getDuration("connection-pool.timeout").toMillis()
        hikariConfig.driverClassName = config.getString("driver")
        hikariConfig.jdbcUrl = config.getString("url")
        hikariConfig.username = config.getString("user")
        hikariConfig.password = config.getString("password")
        return HikariDataSource(hikariConfig)
    }
}

private class HikariCloser(private val dataSource: HikariDataSource) : DataSourceCloser {
    override fun close() = dataSource.close()
}

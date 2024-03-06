package repository

import akka.actor.typed.ActorSystem
import com.typesafe.config.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import scalikejdbc.ConnectionPool
import scalikejdbc.DataSourceCloser
import scalikejdbc.DataSourceConnectionPool
import scalikejdbc.DataSourceConnectionPoolSettings

object ScalikeJdbcSetup {
    private lateinit var config: Config


    fun init(system: ActorSystem<*>) {
        config = system.settings().config().getConfig("jdbc-connection-settings")
        val dataSource = dataSource()

        ConnectionPool.add("read-side-connection-pool", DataSourceConnectionPool(
            dataSource,
            DataSourceConnectionPoolSettings.apply(config.getString("driver")),
            HikariCloser(dataSource)
        ))
//        ConnectionPool.singleton(
//            DataSourceConnectionPool(
//                dataSource,
//                DataSourceConnectionPoolSettings.apply(config.getString("driver")),
//                HikariCloser(dataSource)
//            )
//        )
    }


    fun dataSource(): HikariDataSource {
        val dataSourceConfig = HikariConfig()
        dataSourceConfig.poolName = "read-side-frasua-connection-pool"
        dataSourceConfig.maximumPoolSize = config.getInt("connection-pool.max-pool-size")
        dataSourceConfig.connectionTimeout = config.getDuration("connection-pool.timeout").toMillis()
        dataSourceConfig.driverClassName = config.getString("driver")
        dataSourceConfig.jdbcUrl = config.getString("url")
        dataSourceConfig.username = config.getString("user")
        dataSourceConfig.password = config.getString("password")

        return HikariDataSource(dataSourceConfig)
    }

    class HikariCloser(private val dataSource: HikariDataSource) : DataSourceCloser {
        override fun close() {
            dataSource.close()
        }
    }

}
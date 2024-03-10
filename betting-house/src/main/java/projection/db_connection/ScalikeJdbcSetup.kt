package projection.db_connection

import akka.actor.typed.ActorSystem
import com.typesafe.config.Config
import com.zaxxer.hikari.HikariDataSource
import scala.Option
import scala.collection.immutable.List
import scalikejdbc.ConnectionPool
import scalikejdbc.DataSourceCloser
import scalikejdbc.DataSourceConnectionPool
import scalikejdbc.DataSourceConnectionPoolSettings
import scalikejdbc.Log
import scalikejdbc.config.DBs
import scalikejdbc.config.NoEnvPrefix
import scalikejdbc.config.TypesafeConfig
import scalikejdbc.config.TypesafeConfigReader

class ScalikeJdbcSetup(val system: ActorSystem<*>) {

    init {
        initFromConfig(system.settings().config())
    }

    private fun initFromConfig(config: Config) {
//        val dbs = DBsFromConfig(config)
//        dbs.loadGlobalSettings()
        val dataSource = buildDataSource(
            config.getConfig("jdbc-connection-settings")
        )

        ConnectionPool.singleton(
            DataSourceConnectionPool(
                dataSource,
                DataSourceConnectionPoolSettings("driver"),
                HikariCloser(dataSource)
            )
        )
    }

    private fun buildDataSource(config: Config): HikariDataSource {
        val dataSource = HikariDataSource()
        dataSource.poolName = "read-side-bet-connection-pool"
        dataSource.maximumPoolSize = config.getInt("connection-pool.max-pool-size")

        val timeout = config.getDuration("connection-pool.timeout").toMillis()
        dataSource.connectionTimeout = timeout
        dataSource.driverClassName = config.getString("driver")
        dataSource.jdbcUrl = config.getString("url")
        dataSource.username = config.getString("user")
        dataSource.password = config.getString("password")

        return dataSource
    }
}

private class HikariCloser(private val dataSource: HikariDataSource) : DataSourceCloser {
    override fun close() = dataSource.close()

}

class DBsFromConfig(val config: Config) : DBs, TypesafeConfigReader, TypesafeConfig, NoEnvPrefix {
    override fun env(): Option<String> {
        TODO("Not yet implemented")
    }

    override fun log(): Log {
        TODO("Not yet implemented")
    }

    override fun dbNames(): List<String> {
        TODO("Not yet implemented")
    }

    override fun config(): Config {
        TODO("Not yet implemented")
    }


}

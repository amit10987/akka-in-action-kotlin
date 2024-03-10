package projection.to.db

import projection.db_connection.ScalikeJdbcSession

data class StakePerResult(val sum: Double, val result: Int)

interface BetRepository {

    fun addBet(
        betId: String,
        walletId: String,
        marketId: String,
        odds: Double,
        stake: Int,
        result: Int,
        session: ScalikeJdbcSession
    )

    fun getBetPerMarketTotalStake(
        marketId: String,
        session: ScalikeJdbcSession
    ): List<StakePerResult>
}

class BetRepositoryImpl : BetRepository {

    override fun addBet(
        betId: String,
        walletId: String,
        marketId: String,
        odds: Double,
        stake: Int,
        result: Int,
        session: ScalikeJdbcSession
    ) {
        session.db.withinTx { dbSession ->
            val sql = """
                INSERT INTO
                    bet_wallet_market (betId, walletId, marketId, odds, stake, result)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (betId) DO NOTHING
            """
            dbSession.conn().prepareStatement(sql).use { stmt ->
                stmt.setString(1, betId)
                stmt.setString(2, walletId)
                stmt.setString(3, marketId)
                stmt.setDouble(4, odds)
                stmt.setInt(5, stake)
                stmt.setInt(6, result)
                stmt.executeUpdate()
            }
        }
    }

    override fun getBetPerMarketTotalStake(
        marketId: String,
        session: ScalikeJdbcSession
    ): List<StakePerResult> {
        return session.db.readOnly { dbSession ->
            val sql = """
                SELECT sum(stake * odds) AS sum, result FROM bet_wallet_market WHERE marketId = ? GROUP BY marketId, result
            """
            dbSession.conn().prepareStatement(sql).use { stmt ->
                stmt.setString(1, marketId)
                stmt.executeQuery().use { rs ->
                    val result = mutableListOf<StakePerResult>()
                    while (rs.next()) {
                        val sum = rs.getDouble("sum")
                        val resultValue = rs.getInt("result")
                        result.add(StakePerResult(sum, resultValue))
                    }
                    result
                }
            }
        }
    }
}
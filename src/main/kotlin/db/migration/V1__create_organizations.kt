package db.migration

import com.hypto.iam.server.db.models.Organizations
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class V1__create_organizations: BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            addLogger(StdOutSqlLogger)

            SchemaUtils.create(Organizations)

            Organizations.insert {
                it[name] = "organization one"
                it[dateUpdated] = LocalDateTime.now()
            }
            Organizations.insert {
                it[name] = "organization two"
                it[dateUpdated] = LocalDateTime.now()
            }
        }
    }
}
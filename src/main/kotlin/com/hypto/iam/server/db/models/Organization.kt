package com.hypto.iam.server.db.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object Organizations : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val adminUserId = varchar("admin_user", 50).nullable()
    val dateCreated = datetime("date_created").defaultExpression(CurrentDateTime())
    val dateUpdated = datetime("date_updated")
    override val primaryKey = PrimaryKey(id)
}

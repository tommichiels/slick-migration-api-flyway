package scala.slick.migration.flyway

import java.sql.Connection

import slick.dbio.DBIO
import slick.migration.api.{Migration, SqlMigration, TableMigration}

import org.flywaydb.core.api.resolver.{MigrationExecutor, ResolvedMigration}
import org.flywaydb.core.api.{MigrationType, MigrationVersion}


object VersionedMigration {
  def apply(version: Int, migrations: Migration*): VersionedMigration =
    VersionedMigration(version.toString, migrations: _*)
}

/** Wraps one or more [[slick.migration.api.Migration]] objects with a version string. */
case class VersionedMigration(version: String, migrations: Migration*) extends ResolvedMigration {

  def getDescription: String = {
    migrations.map {
      case m: TableMigration[_] => s"TableMigration(${m.tableInfo.tableName})"
      case other                => other.getClass.getName
    }.mkString("\n")
  }

  def getExecutor: MigrationExecutor = new MigrationExecutor {
    def executeInTransaction = true

    def execute(c: Connection) = {
      val db = new UnmanagedDatabase(c)
      db.run(DBIO.sequence(migrations.map(_.apply())))
    }
  }

  def getScript: String = migrations.map {
    case m: TableMigration[_] => m.toString
    case m: SqlMigration      => s"SqlMigration{${m.sql.mkString(";")};}"
    case _                    => "<< slick-migration-api >>"
  }.mkString("\n")

  def getChecksum = null

  def getPhysicalLocation: String = "Custom code"

  def getType: MigrationType = MigrationType.CUSTOM

  def getVersion: MigrationVersion = MigrationVersion fromVersion version
}

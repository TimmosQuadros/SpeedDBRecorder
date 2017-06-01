package app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.jetbrains.anko.db.*

class DB(var context: Context = App.instance) : ManagedSQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    lateinit var db:SQLiteDatabase

    override fun onCreate(datab: SQLiteDatabase) {
        //db.execSQL("CREATE TABLE if not exists Decibel (dataID int NOT NULL AUTO_INCREMENT, db int, speed int, PRIMARY KEY (dataID);")
    }
    override fun onUpgrade(datab: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }

    /**Setup the db with tables, but only create the table if the db doesn't already exists so it doesn't override
     * or wipe out the entire db if it already exists.
     *
     *
     */
    fun setup() {

        val dbExists = context.getDatabasePath("decibelDB").exists()

        db = writableDatabase

        if(!dbExists) {
            db.createTable(
                    DecibelTable.tableName, true,
                    DecibelTable.dbID to INTEGER + PRIMARY_KEY,
                    DecibelTable.db to INTEGER,
                    DecibelTable.speed to INTEGER
            )
        }

        db.close()
    }

    /**CRUD (Create) function to create a row.
     * @param decibel
     * @param speed
     */
    fun insertToDatabase(decibel:Int,speed:Int){

        db = writableDatabase

        db.insert(
                DecibelTable.tableName,
                DecibelTable.db to decibel,
                DecibelTable.speed to speed
        )

        db.close()
        //db.execSQL("INSERT INTO Decibel (decib,speed) VALUES ("+decib+","+speed+")")
    }

    /**CRUD (retreive) function to retreive alll from the specified table
     *
     */
    fun readDatabase(): List<Map<String, Any?>> {

        db = readableDatabase

        return db.select("Decibel").exec() {
            parseList(
                    object : MapRowParser<Map<String, Any?>> {
                        override fun parseRow(columns: Map<String, Any?>): Map<String, Any?> {
                            return columns
                        }
                    })
        }

        db.close()
    }

//    fun deleteWhereDecib(something:Int) {
//
//        db = writableDatabase
//        val TABLE_NAME = "Decibel"
//        val selectQuery = "DELETE FROM " + TABLE_NAME + " WHERE decib =" + something
//        db.execSQL(selectQuery)
//    }

    /**CRUD (delete) function to delete from selected db.
     * @param dataBaseID
     */
    fun deleteEntry(dataBaseID: Int) {

        db = writableDatabase

        db.delete("Decibel", "${DecibelTable.dbID} = ${dataBaseID}")

        db.close()
    }

    /**The companion object makes the members look static so we can call it like this:
     * Class_Name.Companion.instance. But at runtime they act like real instances of objects and can
     * for example implement interfaces.
     *
     */
    companion object {
        private val DB_NAME = "decibelDB"
        private val DB_VERSION = 1
        val instance by lazy { DB() }
    }
}
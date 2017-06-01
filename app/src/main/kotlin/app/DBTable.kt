package app

import java.io.Serializable

/**
 * Mapper class to map the data to the database.
 */
data class Decibel(
        val dbID: Int,
        var db: Int,
        var speed: Int
) : Serializable

object DecibelTable {
    val tableName = "Decibel"
    val dbID = "dbID"
    val db = "db"
    val speed = "speed"
}

/**
 * Created by TimmosQuadros on 28-05-2017.
 */

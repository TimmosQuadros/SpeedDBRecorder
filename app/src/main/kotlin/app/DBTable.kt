package app

import java.io.Serializable

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

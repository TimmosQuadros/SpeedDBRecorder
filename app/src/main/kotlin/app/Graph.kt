package app

import android.graphics.Color
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.util.*


/**
 * Created by TimmosQuadros on 28-05-2017.
 */
class Graph(graph: GraphView) {

    lateinit internal var velocityLine: LineGraphSeries<DataPoint>
    lateinit internal var soundline: LineGraphSeries<DataPoint>
    internal var graph = graph
    val speeds = mutableListOf<Point>()
    val dbs = mutableListOf<Point>()

    var x = 0.0

    fun initialize(){
        velocityLine = LineGraphSeries<DataPoint>()
        soundline = LineGraphSeries<DataPoint>()
    }

    fun reset(){
        x = 0.0
        speeds.clear()
        dbs.clear()
    }

    fun appendData(db:Double, speed:Double){
        if(x>20){
            reset()
        }

        //Unfortunately the LineGraphSeries object doesn't have a clear method so we have to use new instances and trust the garbage collector
        initialize()

        speeds.add(Point(x,speed))
        dbs.add(Point(x,db))

        for(p_speed: Point in speeds){
            velocityLine.appendData(DataPoint(p_speed.x, p_speed.y), true, 100)
        }

        for(p_db: Point in dbs){
            soundline.appendData(DataPoint(p_db.x, p_db.y), true, 100)
        }
        drawGraph()
        x++
    }

    fun drawGraph(){
        velocityLine.title = "Hastighed"
        soundline.title = "DB"
        velocityLine.color = Color.BLUE
        soundline.color = Color.GREEN

        graph.removeAllSeries()

        graph.viewport.isScalable = true
        graph.viewport.setScalableY(true)
        graph.viewport.isScrollable = true
        graph.viewport.setScrollableY(true)

        graph.viewport.isYAxisBoundsManual = true
        graph.viewport.setMaxY(200.0)
        graph.viewport.setMinY(0.0)

        graph.viewport.isXAxisBoundsManual = true
        graph.viewport.setMaxX(20.0)
        graph.viewport.setMinX(0.0)

        graph.addSeries(velocityLine)
        graph.addSeries(soundline)

        graph.legendRenderer.isVisible = true
        graph.legendRenderer.setFixedPosition(0,0)

    }

}
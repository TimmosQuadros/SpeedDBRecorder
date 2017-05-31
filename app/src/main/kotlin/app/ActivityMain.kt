package app

import android.Manifest
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Handler
import java.io.IOException
import android.media.AudioRecord


class ActivityMain : AppCompatActivity(), LocationListener
{
    private var isRecording = false

    //This is changed in the onRequestPermissionsResult()
    private var permissionToRecordAccepted = false
    private var permissionToUseLocation = false

    //This is standard permission integers
    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    private val ASK_MULTIPLE_PERMISSION_REQUEST_CODE = 123

    //The mediarecorder is used instead of the AudioRecorder which is deprecated!
    val mRecorder = MediaRecorder()
    lateinit var aRecorder: AudioRecord

    private val  LOG_TAG: String = "Record"
    var REFERENCE = 0.00002
    private val mSampleRates = intArrayOf(8000, 11025, 22050, 44100, 48000, 16000)

    private lateinit var timer: Timer
    private lateinit  var timerTask: TimerTask

    private var bufferSize = 0;

    private val permissions = arrayOf<String>(Manifest.permission.RECORD_AUDIO,Manifest.permission.ACCESS_FINE_LOCATION)
    private lateinit var mFileName: String

    private var locations: MutableList<Location> = mutableListOf<Location>()
    private var speeds: MutableList<Float> = mutableListOf<Float>()
    protected lateinit var locationManager: LocationManager
    var isGPSEnabled = false
    // The minimum distance to change Updates in meters
    private val MIN_DISTANCE_CHANGE_FOR_UPDATES: Float = 1f // 1 meter
    // The minimum time between updates in milliseconds
    private val MIN_TIME_BW_UPDATES = (500).toLong() // 0.5 sec

    val handler = Handler()

    //EditText (fragment) from layout activity_main.xml id=threshold
    //When the db or speed is higher than threshold it records data to database and plot data to the graphview
    private var thresholdVal = -1

    //SQLLitedatabase
    val ourDataBase: DB = DB()
    //Decibel value
    var db = 0
    //Velocity value
    var v = 0

    //Graph object that we use to append data and plot it to the graph
    private lateinit var graphController: Graph

    //When the activity is created do
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*Initialize an address for the MediaReorder in case we want to save the audio to a file.
        In our case we just use it as an audiometer to measure the sound pressure (decibel)*/
        mFileName = externalCacheDir.absolutePath
        mFileName += "/audiorecordtest.3gp"

        heading.text = ""

        /*This produces a popup that asks the users for permission to use audio recording and location.
        * It also triggers the onRequestPermissionsResult() function that we override below.*/
        ActivityCompat.requestPermissions(this, permissions,ASK_MULTIPLE_PERMISSION_REQUEST_CODE)

        button.setOnClickListener {
            if(!isRecording){
                Log.d("button","click")
                startRecording()
                startTimer()
                thresholdVal = threshold.text.toString().toInt()
                isRecording = true
                button.text = "Stop"
            }else{
                stopRecording()
                stopTimer()
                isRecording = false
                button.text = "Start"
            }
        }

        ourDataBase.setup()//DB_controller
        graphController = Graph(graph) //We just pass a reference of our graph_view to the Graph class...
        graphController.initialize()

    }

    /**
     *
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode==ASK_MULTIPLE_PERMISSION_REQUEST_CODE) {
            permissionToRecordAccepted = grantResults[0] === PackageManager.PERMISSION_GRANTED
            permissionToUseLocation = grantResults[1] === PackageManager.PERMISSION_GRANTED
        }

        //Alternative way of doing it in android using the when switch
        /*when (requestCode) {
            ASK_MULTIPLE_PERMISSION_REQUEST_CODE -> permissionToRecordAccepted = grantResults[0] === PackageManager.PERMISSION_GRANTED
        }*/

        //Log.d("bjornson",""+permissionToRecordAccepted+" "+permissionToUseLocation)
        if (!permissionToRecordAccepted) finish()
        if (!permissionToUseLocation) finish()

        if(permissionToUseLocation) getLocation()
    }

    //Implemented from LocationListener
    override fun onLocationChanged(location: Location) {
        if(isRecording) {
            locations.add(location)
            if (locations.size == 4) {
                for (i in locations.indices) {
                    if (i <= locations.size - 4) {
                        var time = 0f
                        var distance = 0f
                        var location_1: Location = locations[i]
                        var location_2: Location = locations[i + 1]
                        distance = location_1.distanceTo(location_2)
                        time = Math.abs((location_2.time / 1000) - (location_1.time / 1000)).toFloat()
                        speeds.add((distance / time) * 3.6f)
                    }
                }
                var sumSpeed = 0f
                for (speed in speeds) {
                    sumSpeed += speed
                }
                v = (sumSpeed / speeds.size).toInt()
                speed.text = "" + v + " km/t"

                if (v >= thresholdVal && checkBoxSpeed.isChecked && thresholdVal != -1) {
                    //call db controller and store values in database
                    //Log.d("bjornson", ""+db+" "+v)
                    ourDataBase.insertToDatabase(db,v)
                    graphController.appendData(db.toDouble(),v.toDouble())
                }
                locations.clear();
                speeds.clear();
            }
        }

        //gps.text = "long: "+location.longitude+" lat: "+location.latitude
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProviderEnabled(provider: String?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProviderDisabled(provider: String?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun startTimer() {
        timer = Timer()
        initializeTimerTask()

        timer.schedule(timerTask, 500,500) //Run every 500 ms.
    }

    fun stopTimer() {
        timer.cancel()
    }


    /**The Media Recorder has a fsm (final state machine) i.e. you have to initialize and then call the prepare() method.
     *Documentation: https://developer.android.com/reference/android/media/MediaRecorder.html
     */
    private fun startRecording() {
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mRecorder.setOutputFile(mFileName)

        try {
            mRecorder.prepare()
        } catch (e: IOException) {
            Log.e(LOG_TAG, "prepare() failed"+e.stackTrace)
        }catch (e: Exception){
            Log.e(LOG_TAG, "prepare() failed"+e.stackTrace
            )

        }

        mRecorder.start()
    }

    private fun stopRecording() {
        mRecorder.stop()
        mRecorder.reset()
    }

    private fun getLocation(){
        locationManager = applicationContext
                .getSystemService(Context.LOCATION_SERVICE) as LocationManager

        isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (isGPSEnabled) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
            if (locationManager != null) {
                locations.add(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
            }
        }
    }

    private fun threshold(th_speed: Boolean, th_db: Boolean, speed: Int, db: Int){
        if(th_db){

        }else if(th_speed){

        }
    }

    private fun initializeTimerTask() {
        timerTask = object : TimerTask() {
            override fun run() {
                handler.post {
                    //aRecorder.startRecording()
                    db = ((10.0 * Math.log10(mRecorder.maxAmplitude / REFERENCE))-55).toInt()
                    if(db>=thresholdVal && checkBoxDB.isChecked && thresholdVal>0){
                        //call db controller and store values in database
                        Log.d("bjornson", ourDataBase.readDatabase().toString())
                        ourDataBase.insertToDatabase(db,v)
                        graphController.appendData(db.toDouble(),v.toDouble())
                    }
                    decibel.text = ""+db+" db"
                }
            }
        }
    }
}

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
    private var permissionToRecordAccepted = false
    private var permissionToUseLocation = false
    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    private val ASK_MULTIPLE_PERMISSION_REQUEST_CODE = 123


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

    private var thresholdVal = -1

    val ourDataBase: DB = DB()
    var db = 0
    var v = 0

    private lateinit var graphController: Graph

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFileName = externalCacheDir.absolutePath
        mFileName += "/audiorecordtest.3gp"

        heading.text = "AndroidApp"

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
        graphController = Graph(graph)
        graphController.initialize()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode==ASK_MULTIPLE_PERMISSION_REQUEST_CODE) {
            permissionToRecordAccepted = grantResults[0] === PackageManager.PERMISSION_GRANTED
            permissionToUseLocation = grantResults[1] === PackageManager.PERMISSION_GRANTED
        }

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
    //

    fun startTimer() {
        timer = Timer()
        initializeTimerTask()

        timer.schedule(timerTask, 500,500) //Run every 5 sec.
    }

    fun stopTimer() {
        timer.cancel()
    }

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

        aRecorder=findAudioRecord()
    }

    private fun stopRecording() {
        mRecorder.stop()
        mRecorder.reset()
    }


    fun findAudioRecord(): AudioRecord {
        for (rate in mSampleRates) {
            for (audioFormat in shortArrayOf(AudioFormat.ENCODING_PCM_8BIT.toShort(), AudioFormat.ENCODING_PCM_16BIT.toShort())) {
                for (channelConfig in shortArrayOf(AudioFormat.CHANNEL_IN_MONO.toShort(), AudioFormat.CHANNEL_IN_STEREO.toShort())) {
                    try {
                        Log.d("Record", "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                                + channelConfig)
                        val bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig.toInt(), audioFormat.toInt())
                        this.bufferSize=bufferSize
                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, rate, channelConfig.toInt(), audioFormat.toInt(), bufferSize)

                            if (recorder.state == AudioRecord.STATE_INITIALIZED)
                                return recorder
                        }
                    } catch (e: Exception) {
                        Log.e("Record", ""+rate + "Exception, keep trying.", e)
                    }

                }
            }
        }
        return null!!
    }

    //Not used but it's better to measure decibel with the RMS power instead of the peak amplitude
    fun calculateDB(): Double {
        val buffer = ShortArray(this.bufferSize)
        //aRecorder.startRecording();
        val bufferReadResult = aRecorder.read(buffer, 0, this.bufferSize)

        /*
             * Noise level meter begins here
             */
        // Compute the RMS value. (Note that this does not remove DC).
        var rms = (0..buffer.size - 1).sumByDouble { (buffer[it] * buffer[it]).toDouble() }
        rms = Math.sqrt(rms / buffer.size)
        val mAlpha = 0.9
        val mGain = 0.0044
        /*Compute a smoothed version for less flickering of the
            // display.*/
        val mRmsSmoothed = mAlpha + (1 - mAlpha) * rms
        return 20.0 * Math.log10(mGain * mRmsSmoothed)
    }

    fun calc(): Double {
        val data = ShortArray(bufferSize)

        var average = 0.0

        //aRecorder.startRecording()

        aRecorder.read(data, 0, bufferSize)

        //aRecorder.stop()

        for (s in data) {
            if (s > 0) {
                average += Math.abs(s.toInt())
            } else {
                bufferSize--
            }
        }

        val x = average / bufferSize

        //aRecorder.release()

        var db = 0.0

        val pressure = x / 51805.5336

        db = (20 * Math.log10(pressure/REFERENCE))

        if(db>0)
        {
            return db
        }
        return db
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
                    db = ((10.0 * Math.log10(mRecorder.maxAmplitude / REFERENCE))-60).toInt()
                    if(db>=thresholdVal && checkBoxDB.isChecked && thresholdVal!=-1){
                        //call db controller and store values in database
                        //Log.d("bjornson", ourDataBase.readDatabase().toString())
                        ourDataBase.insertToDatabase(db,v)
                        graphController.appendData(db.toDouble(),v.toDouble())
                    }
                    decibel.text = ""+db+" db"
                }
            }
        }
    }
}

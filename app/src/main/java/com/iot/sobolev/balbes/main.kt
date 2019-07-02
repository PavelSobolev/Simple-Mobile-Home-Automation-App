package com.iot.sobolev.balbes

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Debug
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import com.google.firebase.database.*

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.util.*

class main : AppCompatActivity(), Observer {

    fun EnableControls(value:Boolean)  {
        Hue.isEnabled = value
        Brightness.isEnabled = value
        ApplyHue.isEnabled = value
    }

    override fun update(o: Observable?, arg: Any?)  {
        Hue.progress = BulbData.Hue
        Brightness.progress = BulbData.Brightness
        ApplyHue.isChecked = if (BulbData.ParamToApply==2) true else false

        if (BulbData.IsOn==1)
            EnableControls(true)
        else
            EnableControls(false)

        //BulbData.deleteObserver(this)

        brightnessText.text = "Brightness"
        Brightness.visibility = View.VISIBLE
        ApplyHue.visibility = View.VISIBLE
        Hue.visibility = View.VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?)  {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)


        BulbData.addObserver(this)

        
        fab.setOnClickListener () { view ->
            if (!isConnected()!!)
            {
                Toast.makeText(this, "Internet connection is unavailable!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            BulbData.ParamToApply = 0
            BulbData.IsOn = if (BulbData.IsOn == 0) 1 else 0

            if (BulbData.IsOn == 1)
                EnableControls(true)
            else
                EnableControls(false)
        }


        ApplyHue.setOnClickListener {
            if (ApplyHue.isChecked)
                controlHueAndBrightness()
            else
                controlBrightness()
        }

        Hue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                controlHueAndBrightness()
            }
        })

        Brightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (!ApplyHue.isChecked)
                    controlBrightness()
                else
                    controlHueAndBrightness()
            }
        })
    }


    fun isConnected() : Boolean?  {
        val netService: ConnectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val inetInfo: NetworkInfo? = netService.activeNetworkInfo
        return inetInfo?.isConnected
    }

    fun controlHueAndBrightness() {
        if (!isConnected()!!)
        {
            Toast.makeText(this, "Internet connection is unavailable!", Toast.LENGTH_LONG).show()
            return
        }
        BulbData.ParamToApply = 2
        BulbData.Hue = Hue.progress
        BulbData.Brightness = Brightness.progress
    }

    fun controlBrightness() {
        if (!isConnected()!!)
        {
            Toast.makeText(this, "Internet connection is unavailable!", Toast.LENGTH_LONG).show()
            return
        }
        BulbData.ParamToApply = 1
        BulbData.Brightness = Brightness.progress
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean  {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}

// =======================================================================================

object BulbData : Observable() {
    private var paramToApply: Int = -1
    private var isOn : Int = 0
    private var hue : Int = 0
    private var brightness : Int = 0

    //var HueColors = mutableListOf<Int>()
    //var HueRange = 0..359

    // reference to Google Online Realtime Database (Firebase NoSQL database)
    var dbBulbRefernce: DatabaseReference


    init {
        dbBulbRefernce = FirebaseDatabase.getInstance().getReference()
        getStartData()
    }


    fun getStartData() {
        dbBulbRefernce.addValueEventListener(
        //dbBulbRefernce.addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot)
                {
                    isOn = dataSnapshot.child("IsOn").value.toString().toInt()
                    hue = dataSnapshot.child("Hue").value.toString().toInt()
                    brightness = dataSnapshot.child("LightBrightness").value.toString().toInt()
                    paramToApply = dataSnapshot.child("ParamToApply").value.toString().toInt()
                    setChanged()
                    notifyObservers()
                }

                override fun onCancelled(p0: DatabaseError)
                {
                    /*not used*/
                }
            })


    }

    var Hue  // Color bulb hue (from 0 to 360)
        get() = hue
        set(value)
        {
            if (value in 0..360)
            {
                hue = value
                dbBulbRefernce.child("Hue").setValue(value)
            }
        }

    var IsOn: Int // Is bulbs should be on (0) or off (1)
        get() = isOn
        set(value)
        {
            if (value in 0..1)
            {
                dbBulbRefernce.child("IsOn").setValue(value)
                isOn = value
            }
        }


     var Brightness // level of luminosity of the bulb
        get() = brightness
        set(value)
        {
            if (value in 0..100)
            {
                brightness = value
                dbBulbRefernce.child("LightBrightness").setValue(value)
            }
        }

    var ParamToApply: Int
        get() = paramToApply
        set(value)
        {
            if (value in -1..3)
            {
                dbBulbRefernce.child("ParamToApply").setValue(value)
                paramToApply = value
            }
        }
}
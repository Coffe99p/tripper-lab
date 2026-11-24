package com.joaofabijack.tripper

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

/** Each visible screen holds a binding, so moving between app screens keeps discovery available. */
abstract class TripperActivity : Activity() {
    protected var tripper: TripperService? = null
        private set
    private var bound = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            if (!bound) return
            val service = (binder as TripperService.LocalBinder).service
            tripper = service
            service.screenOpened()
            onTripperAvailable(service)
        }

        override fun onServiceDisconnected(name: ComponentName) { tripper = null }
    }

    protected open fun onTripperAvailable(service: TripperService) {}

    override fun onStart() {
        super.onStart()
        bound = bindService(Intent(this, TripperService::class.java), connection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        tripper?.screenClosed()
        if (bound) unbindService(connection)
        bound = false
        tripper = null
        super.onStop()
    }
}

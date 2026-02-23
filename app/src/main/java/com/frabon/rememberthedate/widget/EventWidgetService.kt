package com.frabon.rememberthedate.widget

import android.content.Intent
import android.widget.RemoteViewsService

class EventWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return EventWidgetFactory(this.applicationContext)
    }
}
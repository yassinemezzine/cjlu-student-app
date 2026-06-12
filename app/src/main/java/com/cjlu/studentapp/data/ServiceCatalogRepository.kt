package com.cjlu.studentapp.data

import com.cjlu.studentapp.network.RetrofitClient
import com.cjlu.studentapp.ui.screens.ServiceItem
import com.cjlu.studentapp.ui.screens.toServiceItem

object ServiceCatalogRepository {
    suspend fun loadServices(): List<ServiceItem> =
        RetrofitClient.instance.services().map { it.toServiceItem() }
}

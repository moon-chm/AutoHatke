package io.github.autocoupons

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

class AutoCouponsApplication : Application() {
    val container: AppContainer by lazy { AppContainer() }
}

/** Resolves the application container from ViewModel creation extras. */
val CreationExtras.appContainer: AppContainer
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AutoCouponsApplication).container

package net.randomhacks.retroshopper

import android.app.Application
import android.content.Context
import androidx.room.Room
import net.randomhacks.retroshopper.data.ShoppingDatabase
import net.randomhacks.retroshopper.data.ShoppingRepository

class RetroShopperApplication : Application() {
  lateinit var container: AppContainer

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
  }
}

/** The app's entire object graph, wired by hand — small enough not to need a DI framework. */
class AppContainer(context: Context) {
  private val database =
      Room.databaseBuilder(context, ShoppingDatabase::class.java, "shopping.db").build()

  val repository = ShoppingRepository(database.shoppingDao())
}

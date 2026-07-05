package net.randomhacks.retroshopper.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Item::class, Store::class, StoreItem::class], version = 1)
abstract class ShoppingDatabase : RoomDatabase() {
  abstract fun shoppingDao(): ShoppingDao
}

package net.randomhacks.retroshopper.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A thing you sometimes buy. `needed` is the home-screen flag; `inCart` marks
 * items picked up during the current shopping trip (see
 * [ShoppingDao.checkOut]). An item stays `needed` while it is in the cart so
 * an accidental cart tap is trivially reversible.
 */
@Entity(indices = [Index(value = ["name"], unique = true)])
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val needed: Boolean = false,
    val inCart: Boolean = false,
)

@Entity(indices = [Index(value = ["name"], unique = true)])
data class Store(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
)

/**
 * The existence of a row means "this item is available at this store".
 * Deleting the row means the item is gone from that store (not merely out of
 * stock), so losing its aisle is fine.
 */
@Entity(
    primaryKeys = ["itemId", "storeId"],
    foreignKeys =
        [
            ForeignKey(
                entity = Item::class,
                parentColumns = ["id"],
                childColumns = ["itemId"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(
                entity = Store::class,
                parentColumns = ["id"],
                childColumns = ["storeId"],
                onDelete = ForeignKey.CASCADE,
            ),
        ],
    indices = [Index("storeId")],
)
data class StoreItem(
    val itemId: Long,
    val storeId: Long,
    val aisle: String = "",
)

/** One needed item as seen from a particular store's shopping list. */
data class ShoppingListRow(
    @Embedded val item: Item,
    /** Null when the item has no [StoreItem] record at this store. */
    val aisle: String?,
    val available: Boolean,
)

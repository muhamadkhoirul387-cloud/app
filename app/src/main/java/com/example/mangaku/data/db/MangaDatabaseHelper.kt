package com.example.mangaku.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.mangaku.data.model.Manga

class MangaDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "mangaku.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_MANGA = "manga"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_AUTHOR = "author"
        const val COLUMN_GENRE = "genre"
        const val COLUMN_YEAR = "release_year"
        const val COLUMN_STATUS = "status"
        const val COLUMN_RATING = "rating"
        const val COLUMN_SYNOPSIS = "synopsis"
        const val COLUMN_BORROWED_BY = "borrowed_by"
        const val COLUMN_IS_FAVORITE = "is_favorite"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_MANGA (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_AUTHOR TEXT NOT NULL,
                $COLUMN_GENRE TEXT NOT NULL,
                $COLUMN_YEAR TEXT,
                $COLUMN_STATUS TEXT DEFAULT 'Tersedia',
                $COLUMN_RATING REAL DEFAULT 4.5,
                $COLUMN_SYNOPSIS TEXT,
                $COLUMN_BORROWED_BY TEXT,
                $COLUMN_IS_FAVORITE INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
        seedInitialManga(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MANGA")
        onCreate(db)
    }

    private fun seedInitialManga(db: SQLiteDatabase) {
        val sampleList = listOf(
            Manga(
                title = "One Piece",
                author = "Eiichiro Oda",
                genre = "Shounen",
                releaseYear = "1997",
                status = "Tersedia",
                rating = 4.9,
                synopsis = "Monkey D. Luffy memulai petualangan di laut lepas untuk menemukan harta karun legendaris 'One Piece' dan menjadi Raja Bajak Laut bersama kru Topi Jerami."
            ),
            Manga(
                title = "Frieren: Beyond Journey's End",
                author = "Kanehito Yamada",
                genre = "Fantasy",
                releaseYear = "2020",
                status = "Tersedia",
                rating = 4.9,
                synopsis = "Penyihir elf Frieren memulai perjalanan panjang untuk memahami makna kehidupan dan manusia setelah kelompok pahlawannya berhasil mengalahkan Raja Iblis."
            ),
            Manga(
                title = "Jujutsu Kaisen",
                author = "Gege Akutami",
                genre = "Action",
                releaseYear = "2018",
                status = "Dipinjam",
                rating = 4.8,
                synopsis = "Yuji Itadori menelan jari kutukan Sukuna untuk menyelamatkan temannya dan bergabung dengan SMA Jujutsu Tokyo untuk melawan roh-roh kutukan berbahaya.",
                borrowedBy = "Adit"
            ),
            Manga(
                title = "Spy x Family",
                author = "Tatsuya Endo",
                genre = "Comedy",
                releaseYear = "2019",
                status = "Tersedia",
                rating = 4.7,
                synopsis = "Mata-mata handal 'Twilight' harus membentuk keluarga palsu demi misi perdamaian dunia, tanpa tahu istrinya seorang pembunuh bayaran dan putrinya telepati."
            ),
            Manga(
                title = "Detective Conan",
                author = "Gosho Aoyama",
                genre = "Mystery",
                releaseYear = "1994",
                status = "Tersedia",
                rating = 4.7,
                synopsis = "Detektif SMA Shinichi Kudo diracuni oleh Organisasi Hitam hingga tubuhnya mengecil menjadi anak SD. Ia menyamar sebagai Conan Edogawa untuk memecahkan berbagai kasus."
            ),
            Manga(
                title = "Blue Box",
                author = "Kouji Miura",
                genre = "Romance",
                releaseYear = "2021",
                status = "Tersedia",
                rating = 4.6,
                synopsis = "Taiki Inomata, atlet bulu tangkis, mengagumi Chinatsu Kano, bintang tim basket. Tak disangka, Chinatsu tinggal serumah dengannya karena urusan keluarga."
            )
        )

        for (m in sampleList) {
            val values = ContentValues().apply {
                put(COLUMN_TITLE, m.title)
                put(COLUMN_AUTHOR, m.author)
                put(COLUMN_GENRE, m.genre)
                put(COLUMN_YEAR, m.releaseYear)
                put(COLUMN_STATUS, m.status)
                put(COLUMN_RATING, m.rating)
                put(COLUMN_SYNOPSIS, m.synopsis)
                put(COLUMN_BORROWED_BY, m.borrowedBy)
                put(COLUMN_IS_FAVORITE, if (m.isFavorite) 1 else 0)
            }
            db.insert(TABLE_MANGA, null, values)
        }
    }

    fun getAllManga(): List<Manga> {
        val list = mutableListOf<Manga>()
        val db = readableDatabase
        val cursor: Cursor = db.query(TABLE_MANGA, null, null, null, null, null, "$COLUMN_ID DESC")
        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToManga(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun searchManga(query: String, genreFilter: String?): List<Manga> {
        val list = mutableListOf<Manga>()
        val db = readableDatabase
        val selectionParts = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        if (query.isNotBlank()) {
            selectionParts.add("($COLUMN_TITLE LIKE ? OR $COLUMN_AUTHOR LIKE ?)")
            selectionArgs.add("%$query%")
            selectionArgs.add("%$query%")
        }

        if (!genreFilter.isNullOrBlank() && genreFilter != "Semua") {
            selectionParts.add("$COLUMN_GENRE = ?")
            selectionArgs.add(genreFilter)
        }

        val selection = if (selectionParts.isNotEmpty()) selectionParts.joinToString(" AND ") else null
        val args = if (selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null

        val cursor = db.query(TABLE_MANGA, null, selection, args, null, null, "$COLUMN_ID DESC")
        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToManga(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun insertManga(manga: Manga): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, manga.title)
            put(COLUMN_AUTHOR, manga.author)
            put(COLUMN_GENRE, manga.genre)
            put(COLUMN_YEAR, manga.releaseYear)
            put(COLUMN_STATUS, manga.status)
            put(COLUMN_RATING, manga.rating)
            put(COLUMN_SYNOPSIS, manga.synopsis)
            put(COLUMN_BORROWED_BY, manga.borrowedBy)
            put(COLUMN_IS_FAVORITE, if (manga.isFavorite) 1 else 0)
        }
        return db.insert(TABLE_MANGA, null, values)
    }

    fun updateManga(manga: Manga): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, manga.title)
            put(COLUMN_AUTHOR, manga.author)
            put(COLUMN_GENRE, manga.genre)
            put(COLUMN_YEAR, manga.releaseYear)
            put(COLUMN_STATUS, manga.status)
            put(COLUMN_RATING, manga.rating)
            put(COLUMN_SYNOPSIS, manga.synopsis)
            put(COLUMN_BORROWED_BY, manga.borrowedBy)
            put(COLUMN_IS_FAVORITE, if (manga.isFavorite) 1 else 0)
        }
        return db.update(TABLE_MANGA, values, "$COLUMN_ID = ?", arrayOf(manga.id.toString()))
    }

    fun deleteManga(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_MANGA, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun updateBorrowStatus(id: Long, newStatus: String, borrower: String?): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_STATUS, newStatus)
            put(COLUMN_BORROWED_BY, borrower)
        }
        return db.update(TABLE_MANGA, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun toggleFavorite(id: Long, isFav: Boolean): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_FAVORITE, if (isFav) 1 else 0)
        }
        return db.update(TABLE_MANGA, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    private fun cursorToManga(cursor: Cursor): Manga {
        return Manga(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
            author = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUTHOR)),
            genre = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENRE)),
            releaseYear = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_YEAR)) ?: "",
            status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS)) ?: "Tersedia",
            rating = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_RATING)),
            synopsis = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SYNOPSIS)) ?: "",
            borrowedBy = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BORROWED_BY)),
            isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_FAVORITE)) == 1
        )
    }
}

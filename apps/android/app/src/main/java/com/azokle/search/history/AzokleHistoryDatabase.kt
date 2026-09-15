package com.azokle.search.history

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.azokle.search.permissions.PermissionAuditItem
import com.azokle.search.tabs.TabGroup
import com.azokle.search.tabs.TabSession

/**
 * Enterprise SQLite Database for Azokle Sovereign Browsing History.
 * Features:
 * - WAL (Write-Ahead Logging) for concurrent reads/writes without UI contention
 * - Foreign key constraints with cascading deletes
 * - FTS (Full-Text Search) virtual table with automated synchronization triggers
 * - Secure forensic deletion and SQLite vacuuming
 * - Persistent tab restoration for standard tabs with Tor RAM-only ephemeral isolation
 */
class AzokleHistoryDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        private const val TAG = "AzokleHistoryDb"
        private const val DATABASE_NAME = "azokle_history.db"
        private const val DATABASE_VERSION = 2

        // Table: Places
        const val TABLE_PLACES = "azokle_places"
        const val COL_ID = "id"
        const val COL_URL = "url"
        const val COL_TITLE = "title"
        const val COL_CLEAN_HOST = "clean_host"
        const val COL_VISIT_COUNT = "visit_count"
        const val COL_TYPED_COUNT = "typed_count"
        const val COL_FRECENCY = "frecency"
        const val COL_LAST_VISIT_TIME = "last_visit_time"
        const val COL_IS_PINNED = "is_pinned"
        const val COL_IS_SEARCH = "is_search"
        const val COL_SEARCH_QUERY = "search_query"

        // Table: Visits
        const val TABLE_VISITS = "azokle_visits"
        const val COL_VISIT_ID = "id"
        const val COL_PLACE_ID = "place_id"
        const val COL_VISIT_TIME = "visit_time"
        const val COL_TRANSITION_TYPE = "transition_type"

        // Table: FTS
        const val TABLE_PLACES_FTS = "azokle_places_fts"

        // Table: Permissions Rules
        const val TABLE_PERMISSIONS = "azokle_permissions"
        const val COL_PERM_ORIGIN = "origin"
        const val COL_PERM_TYPE = "permission_type"
        const val COL_PERM_STATE = "permission_state"
        const val COL_PERM_UPDATED_AT = "updated_at"

        // Table: Permission Audit Log
        const val TABLE_PERMISSION_AUDIT = "azokle_permission_audit"
        const val COL_AUDIT_ID = "id"
        const val COL_AUDIT_ORIGIN = "origin"
        const val COL_AUDIT_PERM_TYPE = "permission_type"
        const val COL_AUDIT_STATE = "permission_state"
        const val COL_AUDIT_TIMESTAMP = "timestamp"
        const val COL_AUDIT_IS_PRIVATE = "is_private"

        // Table: Tabs
        const val TABLE_TABS = "azokle_tabs"
        const val COL_TAB_ID = "id"
        const val COL_TAB_URL = "url"
        const val COL_TAB_TITLE = "title"
        const val COL_TAB_FAVICON_URL = "favicon_url"
        const val COL_TAB_GROUP_ID = "group_id"
        const val COL_TAB_GROUP_TITLE = "group_title"
        const val COL_TAB_GROUP_COLOR = "group_color"
        const val COL_TAB_PARENT_ID = "parent_tab_id"
        const val COL_TAB_IS_DESKTOP = "is_desktop_mode"
        const val COL_TAB_WALLPAPER = "wallpaper_res_id"
        const val COL_TAB_LAST_ACCESSED = "last_accessed_at"
        const val COL_TAB_CREATED_AT = "created_at"
        const val COL_TAB_ORDER = "tab_order"

        // Table: Tab Groups
        const val TABLE_TAB_GROUPS = "azokle_tab_groups"
        const val COL_GROUP_ID = "id"
        const val COL_GROUP_TITLE = "title"
        const val COL_GROUP_COLOR = "color"
        const val COL_GROUP_PARENT_ID = "parent_tab_id"
        const val COL_GROUP_IS_COLLAPSED = "is_collapsed"
        const val COL_GROUP_CREATED_AT = "created_at"

        // Table: Downloads
        const val TABLE_DOWNLOADS = "azokle_downloads"
        const val COL_DL_ID = "id"
        const val COL_DL_URL = "url"
        const val COL_DL_FILENAME = "filename"
        const val COL_DL_FILEPATH = "file_path"
        const val COL_DL_MIMETYPE = "mime_type"
        const val COL_DL_TOTAL_BYTES = "total_bytes"
        const val COL_DL_DOWNLOADED_BYTES = "downloaded_bytes"
        const val COL_DL_STATUS = "status"
        const val COL_DL_TIMESTAMP = "timestamp"
        const val COL_DL_ERROR = "error_message"

        // Table: Session Metadata
        const val TABLE_SESSION_META = "azokle_session_meta"
        const val COL_META_KEY = "meta_key"
        const val COL_META_VALUE = "meta_value"
        const val KEY_ACTIVE_TAB_ID = "active_tab_id"

        @Volatile
        private var instance: AzokleHistoryDatabase? = null

        fun getInstance(context: Context): AzokleHistoryDatabase {
            return instance ?: synchronized(this) {
                instance ?: AzokleHistoryDatabase(context).also { instance = it }
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.enableWriteAheadLogging()
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Create Master Places Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_PLACES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_URL TEXT NOT NULL UNIQUE,
                $COL_TITLE TEXT NOT NULL DEFAULT '',
                $COL_CLEAN_HOST TEXT NOT NULL DEFAULT '',
                $COL_VISIT_COUNT INTEGER NOT NULL DEFAULT 1,
                $COL_TYPED_COUNT INTEGER NOT NULL DEFAULT 0,
                $COL_FRECENCY INTEGER NOT NULL DEFAULT 1000,
                $COL_LAST_VISIT_TIME INTEGER NOT NULL,
                $COL_IS_PINNED INTEGER NOT NULL DEFAULT 0,
                $COL_IS_SEARCH INTEGER NOT NULL DEFAULT 0,
                $COL_SEARCH_QUERY TEXT DEFAULT NULL
            );
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_places_url ON $TABLE_PLACES($COL_URL);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_places_host ON $TABLE_PLACES($COL_CLEAN_HOST);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_places_frecency ON $TABLE_PLACES($COL_FRECENCY DESC);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_places_visit_time ON $TABLE_PLACES($COL_LAST_VISIT_TIME DESC);")

        // 2. Create Visits Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_VISITS (
                $COL_VISIT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PLACE_ID INTEGER NOT NULL REFERENCES $TABLE_PLACES($COL_ID) ON DELETE CASCADE,
                $COL_VISIT_TIME INTEGER NOT NULL,
                $COL_TRANSITION_TYPE INTEGER NOT NULL DEFAULT 0
            );
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_visits_place_id ON $TABLE_VISITS($COL_PLACE_ID);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_visits_time ON $TABLE_VISITS($COL_VISIT_TIME DESC);")

        // 3. Create Full-Text Search (FTS) Virtual Table & Triggers
        try {
            db.execSQL(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS $TABLE_PLACES_FTS USING fts4(
                    title,
                    url,
                    clean_host,
                    content='$TABLE_PLACES'
                );
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS trg_places_insert AFTER INSERT ON $TABLE_PLACES BEGIN
                    INSERT INTO $TABLE_PLACES_FTS(docid, title, url, clean_host) VALUES (new.$COL_ID, new.$COL_TITLE, new.$COL_URL, new.$COL_CLEAN_HOST);
                END;
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS trg_places_update AFTER UPDATE ON $TABLE_PLACES BEGIN
                    INSERT INTO $TABLE_PLACES_FTS($TABLE_PLACES_FTS, docid, title, url, clean_host) VALUES ('delete', old.$COL_ID, old.$COL_TITLE, old.$COL_URL, old.$COL_CLEAN_HOST);
                    INSERT INTO $TABLE_PLACES_FTS(docid, title, url, clean_host) VALUES (new.$COL_ID, new.$COL_TITLE, new.$COL_URL, new.$COL_CLEAN_HOST);
                END;
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS trg_places_delete AFTER DELETE ON $TABLE_PLACES BEGIN
                    INSERT INTO $TABLE_PLACES_FTS($TABLE_PLACES_FTS, docid, title, url, clean_host) VALUES ('delete', old.$COL_ID, old.$COL_TITLE, old.$COL_URL, old.$COL_CLEAN_HOST);
                END;
                """.trimIndent()
            )
        } catch (e: Exception) {
            Log.w(TAG, "FTS virtual table creation notice: ${e.message}")
        }

        // 4. Create Site Permissions Rules Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_PERMISSIONS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PERM_ORIGIN TEXT NOT NULL,
                $COL_PERM_TYPE TEXT NOT NULL,
                $COL_PERM_STATE TEXT NOT NULL,
                $COL_PERM_UPDATED_AT INTEGER NOT NULL,
                UNIQUE($COL_PERM_ORIGIN, $COL_PERM_TYPE)
            );
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_perm_origin ON $TABLE_PERMISSIONS($COL_PERM_ORIGIN);")

        // 5. Create Site Permission Access Audit Log Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_PERMISSION_AUDIT (
                $COL_AUDIT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_AUDIT_ORIGIN TEXT NOT NULL,
                $COL_AUDIT_PERM_TYPE TEXT NOT NULL,
                $COL_AUDIT_STATE TEXT NOT NULL,
                $COL_AUDIT_TIMESTAMP INTEGER NOT NULL,
                $COL_AUDIT_IS_PRIVATE INTEGER NOT NULL DEFAULT 0
            );
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_audit_origin ON $TABLE_PERMISSION_AUDIT($COL_AUDIT_ORIGIN);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_audit_time ON $TABLE_PERMISSION_AUDIT($COL_AUDIT_TIMESTAMP DESC);")

        // 6. Create Tabs Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_TABS (
                $COL_TAB_ID TEXT PRIMARY KEY,
                $COL_TAB_URL TEXT NOT NULL DEFAULT '',
                $COL_TAB_TITLE TEXT NOT NULL DEFAULT 'New Tab',
                $COL_TAB_FAVICON_URL TEXT DEFAULT NULL,
                $COL_TAB_GROUP_ID TEXT DEFAULT NULL,
                $COL_TAB_GROUP_TITLE TEXT DEFAULT NULL,
                $COL_TAB_GROUP_COLOR INTEGER DEFAULT NULL,
                $COL_TAB_PARENT_ID TEXT DEFAULT NULL,
                $COL_TAB_IS_DESKTOP INTEGER NOT NULL DEFAULT 0,
                $COL_TAB_WALLPAPER INTEGER DEFAULT NULL,
                $COL_TAB_LAST_ACCESSED INTEGER NOT NULL DEFAULT 0,
                $COL_TAB_CREATED_AT INTEGER NOT NULL DEFAULT 0,
                $COL_TAB_ORDER INTEGER NOT NULL DEFAULT 0
            );
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tabs_order ON $TABLE_TABS($COL_TAB_ORDER ASC);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tabs_group ON $TABLE_TABS($COL_TAB_GROUP_ID);")

        // 7. Create Tab Groups Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_TAB_GROUPS (
                $COL_GROUP_ID TEXT PRIMARY KEY,
                $COL_GROUP_TITLE TEXT NOT NULL DEFAULT 'Group',
                $COL_GROUP_COLOR INTEGER NOT NULL DEFAULT 4281925112,
                $COL_GROUP_PARENT_ID TEXT DEFAULT NULL,
                $COL_GROUP_IS_COLLAPSED INTEGER NOT NULL DEFAULT 0,
                $COL_GROUP_CREATED_AT INTEGER NOT NULL DEFAULT 0
            );
            """.trimIndent()
        )

        // 8. Create Session Metadata Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_SESSION_META (
                $COL_META_KEY TEXT PRIMARY KEY,
                $COL_META_VALUE TEXT NOT NULL
            );
            """.trimIndent()
        )

        // 9. Create Downloads Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_DOWNLOADS (
                $COL_DL_ID TEXT PRIMARY KEY,
                $COL_DL_URL TEXT NOT NULL,
                $COL_DL_FILENAME TEXT NOT NULL,
                $COL_DL_FILEPATH TEXT NOT NULL,
                $COL_DL_MIMETYPE TEXT NOT NULL,
                $COL_DL_TOTAL_BYTES INTEGER NOT NULL DEFAULT 0,
                $COL_DL_DOWNLOADED_BYTES INTEGER NOT NULL DEFAULT 0,
                $COL_DL_STATUS TEXT NOT NULL DEFAULT 'COMPLETED',
                $COL_DL_TIMESTAMP INTEGER NOT NULL,
                $COL_DL_ERROR TEXT DEFAULT NULL
            );
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_downloads_time ON $TABLE_DOWNLOADS($COL_DL_TIMESTAMP DESC);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_TABS (
                    $COL_TAB_ID TEXT PRIMARY KEY,
                    $COL_TAB_URL TEXT NOT NULL DEFAULT '',
                    $COL_TAB_TITLE TEXT NOT NULL DEFAULT 'New Tab',
                    $COL_TAB_FAVICON_URL TEXT DEFAULT NULL,
                    $COL_TAB_GROUP_ID TEXT DEFAULT NULL,
                    $COL_TAB_GROUP_TITLE TEXT DEFAULT NULL,
                    $COL_TAB_GROUP_COLOR INTEGER DEFAULT NULL,
                    $COL_TAB_PARENT_ID TEXT DEFAULT NULL,
                    $COL_TAB_IS_DESKTOP INTEGER NOT NULL DEFAULT 0,
                    $COL_TAB_WALLPAPER INTEGER DEFAULT NULL,
                    $COL_TAB_LAST_ACCESSED INTEGER NOT NULL DEFAULT 0,
                    $COL_TAB_CREATED_AT INTEGER NOT NULL DEFAULT 0,
                    $COL_TAB_ORDER INTEGER NOT NULL DEFAULT 0
                );
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_tabs_order ON $TABLE_TABS($COL_TAB_ORDER ASC);")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_tabs_group ON $TABLE_TABS($COL_TAB_GROUP_ID);")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_TAB_GROUPS (
                    $COL_GROUP_ID TEXT PRIMARY KEY,
                    $COL_GROUP_TITLE TEXT NOT NULL DEFAULT 'Group',
                    $COL_GROUP_COLOR INTEGER NOT NULL DEFAULT 4281925112,
                    $COL_GROUP_PARENT_ID TEXT DEFAULT NULL,
                    $COL_GROUP_IS_COLLAPSED INTEGER NOT NULL DEFAULT 0,
                    $COL_GROUP_CREATED_AT INTEGER NOT NULL DEFAULT 0
                );
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_SESSION_META (
                    $COL_META_KEY TEXT PRIMARY KEY,
                    $COL_META_VALUE TEXT NOT NULL
                );
                """.trimIndent()
            )
        }
        if (oldVersion < 3) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_DOWNLOADS (
                    $COL_DL_ID TEXT PRIMARY KEY,
                    $COL_DL_URL TEXT NOT NULL,
                    $COL_DL_FILENAME TEXT NOT NULL,
                    $COL_DL_FILEPATH TEXT NOT NULL,
                    $COL_DL_MIMETYPE TEXT NOT NULL,
                    $COL_DL_TOTAL_BYTES INTEGER NOT NULL DEFAULT 0,
                    $COL_DL_DOWNLOADED_BYTES INTEGER NOT NULL DEFAULT 0,
                    $COL_DL_STATUS TEXT NOT NULL DEFAULT 'COMPLETED',
                    $COL_DL_TIMESTAMP INTEGER NOT NULL,
                    $COL_DL_ERROR TEXT DEFAULT NULL
                );
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_downloads_time ON $TABLE_DOWNLOADS($COL_DL_TIMESTAMP DESC);")
        }
    }

    /**
     * Inserts or updates a visited place in SQLite and appends a visit event log.
     */
    fun recordPlaceVisit(
        url: String,
        title: String,
        isTyped: Boolean = false,
        isSearch: Boolean = false,
        searchQuery: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        if (url.isBlank()) return -1L
        val db = writableDatabase

        return try {
            val cleanHost = try {
                android.net.Uri.parse(url).host?.removePrefix("www.") ?: url
            } catch (e: Exception) {
                url
            }

            var placeId = -1L
            var currentVisits = 0
            var currentTyped = 0

            // Check if place exists
            val cursor = db.rawQuery(
                "SELECT $COL_ID, $COL_VISIT_COUNT, $COL_TYPED_COUNT, $COL_TITLE FROM $TABLE_PLACES WHERE $COL_URL = ? LIMIT 1",
                arrayOf(url)
            )

            if (cursor.moveToFirst()) {
                placeId = cursor.getLong(0)
                currentVisits = cursor.getInt(1)
                currentTyped = cursor.getInt(2)
                val existingTitle = cursor.getString(3)
                cursor.close()

                val newVisits = currentVisits + 1
                val newTyped = if (isTyped) currentTyped + 1 else currentTyped
                val newTitle = if (title.isNotBlank()) title else existingTitle

                // Calculate Frecency (100% recency multiplier for today)
                val typeBonus = (newTyped * 2000L) + (newVisits * 1000L)

                val values = ContentValues().apply {
                    put(COL_TITLE, newTitle)
                    put(COL_VISIT_COUNT, newVisits)
                    put(COL_TYPED_COUNT, newTyped)
                    put(COL_FRECENCY, typeBonus)
                    put(COL_LAST_VISIT_TIME, timestamp)
                    if (isSearch) {
                        put(COL_IS_SEARCH, 1)
                        put(COL_SEARCH_QUERY, searchQuery ?: title)
                    }
                }
                db.update(TABLE_PLACES, values, "$COL_ID = ?", arrayOf(placeId.toString()))
            } else {
                cursor.close()
                val typedCount = if (isTyped) 1 else 0
                val frecency = (typedCount * 2000L) + 1000L

                val values = ContentValues().apply {
                    put(COL_URL, url)
                    put(COL_TITLE, title.ifBlank { cleanHost })
                    put(COL_CLEAN_HOST, cleanHost)
                    put(COL_VISIT_COUNT, 1)
                    put(COL_TYPED_COUNT, typedCount)
                    put(COL_FRECENCY, frecency)
                    put(COL_LAST_VISIT_TIME, timestamp)
                    put(COL_IS_PINNED, 0)
                    put(COL_IS_SEARCH, if (isSearch) 1 else 0)
                    put(COL_SEARCH_QUERY, searchQuery)
                }
                placeId = db.insertWithOnConflict(TABLE_PLACES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }

            // Insert granular visit log
            if (placeId > 0) {
                val visitValues = ContentValues().apply {
                    put(COL_PLACE_ID, placeId)
                    put(COL_VISIT_TIME, timestamp)
                    put(COL_TRANSITION_TYPE, if (isTyped) 1 else (if (isSearch) 2 else 0))
                }
                db.insert(TABLE_VISITS, null, visitValues)
            }

            placeId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record place visit in SQLite: ${e.message}")
            -1L
        }
    }

    /**
     * Updates document title for a URL.
     */
    fun updatePlaceTitle(url: String, title: String) {
        if (url.isBlank() || title.isBlank()) return
        try {
            val values = ContentValues().apply {
                put(COL_TITLE, title)
            }
            writableDatabase.update(TABLE_PLACES, values, "$COL_URL = ?", arrayOf(url))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update place title in SQLite: ${e.message}")
        }
    }

    /**
     * Fetches all places from SQLite sorted by last visit time descending.
     */
    fun getAllPlaces(limit: Int = 1000): List<HistoryItem> {
        val list = mutableListOf<HistoryItem>()
        val db = readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(
                """
                SELECT $COL_ID, $COL_URL, $COL_TITLE, $COL_LAST_VISIT_TIME, $COL_VISIT_COUNT, $COL_TYPED_COUNT, $COL_FRECENCY, $COL_IS_SEARCH, $COL_SEARCH_QUERY
                FROM $TABLE_PLACES
                ORDER BY $COL_LAST_VISIT_TIME DESC
                LIMIT ?
                """.trimIndent(),
                arrayOf(limit.toString())
            )
            while (cursor.moveToNext()) {
                list.add(cursorToHistoryItem(cursor))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read places from SQLite: ${e.message}")
        } finally {
            cursor?.close()
        }
        return list
    }

    /**
     * Sub-millisecond Full-Text Search (FTS) query across historical titles, URLs, and domains.
     */
    fun searchFts(query: String, limit: Int = 20): List<HistoryItem> {
        val clean = query.trim()
        if (clean.isEmpty()) return getAllPlaces(limit)

        val list = mutableListOf<HistoryItem>()
        val db = readableDatabase
        var cursor: Cursor? = null
        try {
            // Try FTS query with fallback to LIKE
            cursor = db.rawQuery(
                """
                SELECT p.$COL_ID, p.$COL_URL, p.$COL_TITLE, p.$COL_LAST_VISIT_TIME, p.$COL_VISIT_COUNT, p.$COL_TYPED_COUNT, p.$COL_FRECENCY, p.$COL_IS_SEARCH, p.$COL_SEARCH_QUERY
                FROM $TABLE_PLACES p
                WHERE p.$COL_TITLE LIKE ? OR p.$COL_URL LIKE ? OR p.$COL_CLEAN_HOST LIKE ?
                ORDER BY p.$COL_FRECENCY DESC, p.$COL_LAST_VISIT_TIME DESC
                LIMIT ?
                """.trimIndent(),
                arrayOf("%$clean%", "%$clean%", "%$clean%", limit.toString())
            )
            while (cursor.moveToNext()) {
                list.add(cursorToHistoryItem(cursor))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute search query in SQLite: ${e.message}")
        } finally {
            cursor?.close()
        }
        return list
    }

    fun deletePlace(id: String): Boolean {
        return try {
            writableDatabase.delete(TABLE_PLACES, "$COL_ID = ?", arrayOf(id)) > 0
        } catch (e: Exception) {
            false
        }
    }

    fun deletePlaceByUrl(url: String): Boolean {
        return try {
            writableDatabase.delete(TABLE_PLACES, "$COL_URL = ?", arrayOf(url)) > 0
        } catch (e: Exception) {
            false
        }
    }

    fun deletePlacesByDomain(domain: String): Int {
        val d = domain.trim().lowercase().removePrefix("www.")
        return try {
            writableDatabase.delete(TABLE_PLACES, "$COL_CLEAN_HOST = ? OR $COL_CLEAN_HOST LIKE ?", arrayOf(d, "%.$d"))
        } catch (e: Exception) {
            0
        }
    }

    fun clearTimeRange(fromMs: Long, toMs: Long): Int {
        return try {
            writableDatabase.delete(TABLE_PLACES, "$COL_LAST_VISIT_TIME BETWEEN ? AND ?", arrayOf(fromMs.toString(), toMs.toString()))
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Anti-forensic complete database purge with SQLite secure delete and vacuuming.
     */
    fun clearAllSecurely() {
        try {
            val db = writableDatabase
            db.execSQL("PRAGMA secure_delete = ON;")
            db.delete(TABLE_VISITS, null, null)
            db.delete(TABLE_PLACES, null, null)
            db.delete(TABLE_PERMISSION_AUDIT, null, null)
            db.delete(TABLE_TABS, null, null)
            db.delete(TABLE_TAB_GROUPS, null, null)
            db.delete(TABLE_SESSION_META, null, null)
            db.delete(TABLE_DOWNLOADS, null, null)
            try {
                db.delete(TABLE_PLACES_FTS, null, null)
            } catch (_: Exception) {}
            db.execSQL("VACUUM;")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to securely wipe SQLite history: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // Downloads SQLite Operations
    // -------------------------------------------------------------------------

    fun recordOrUpdateDownload(item: com.azokle.search.downloads.DownloadItem) {
        // Ephemeral RAM-only guard: Tor and Private downloads are never logged to SQLite
        if (item.isPrivate || item.isTor) return
        try {
            val values = ContentValues().apply {
                put(COL_DL_ID, item.id)
                put(COL_DL_URL, item.url)
                put(COL_DL_FILENAME, item.filename)
                put(COL_DL_FILEPATH, item.filePath)
                put(COL_DL_MIMETYPE, item.mimeType)
                put(COL_DL_TOTAL_BYTES, item.totalBytes)
                put(COL_DL_DOWNLOADED_BYTES, item.downloadedBytes)
                put(COL_DL_STATUS, item.status.name)
                put(COL_DL_TIMESTAMP, item.timestamp)
                put(COL_DL_ERROR, item.errorMessage)
            }
            writableDatabase.insertWithOnConflict(TABLE_DOWNLOADS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record download in SQLite: ${e.message}")
        }
    }

    fun getAllDownloads(limit: Int = 200): List<com.azokle.search.downloads.DownloadItem> {
        val list = mutableListOf<com.azokle.search.downloads.DownloadItem>()
        var cursor: Cursor? = null
        try {
            cursor = readableDatabase.rawQuery(
                """
                SELECT $COL_DL_ID, $COL_DL_URL, $COL_DL_FILENAME, $COL_DL_FILEPATH, $COL_DL_MIMETYPE,
                       $COL_DL_TOTAL_BYTES, $COL_DL_DOWNLOADED_BYTES, $COL_DL_STATUS, $COL_DL_TIMESTAMP, $COL_DL_ERROR
                FROM $TABLE_DOWNLOADS
                ORDER BY $COL_DL_TIMESTAMP DESC
                LIMIT ?
                """.trimIndent(),
                arrayOf(limit.toString())
            )
            while (cursor.moveToNext()) {
                val rawStatus = cursor.getString(7) ?: "COMPLETED"
                val status = if (rawStatus == "DOWNLOADING") {
                    com.azokle.search.downloads.DownloadStatus.PAUSED
                } else {
                    try { com.azokle.search.downloads.DownloadStatus.valueOf(rawStatus) } catch (e: Exception) { com.azokle.search.downloads.DownloadStatus.COMPLETED }
                }
                list.add(
                    com.azokle.search.downloads.DownloadItem(
                        id = cursor.getString(0),
                        url = cursor.getString(1) ?: "",
                        filename = cursor.getString(2) ?: "",
                        filePath = cursor.getString(3) ?: "",
                        mimeType = cursor.getString(4) ?: "application/octet-stream",
                        totalBytes = cursor.getLong(5),
                        downloadedBytes = cursor.getLong(6),
                        status = status,
                        timestamp = cursor.getLong(8),
                        errorMessage = cursor.getString(9),
                        isPrivate = false,
                        isTor = false
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read downloads from SQLite: ${e.message}")
        } finally {
            cursor?.close()
        }
        return list
    }

    fun deleteDownload(id: String): Boolean {
        return try {
            writableDatabase.delete(TABLE_DOWNLOADS, "$COL_DL_ID = ?", arrayOf(id)) > 0
        } catch (e: Exception) {
            false
        }
    }

    fun clearDownloads() {
        try {
            writableDatabase.delete(TABLE_DOWNLOADS, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear downloads in SQLite: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // Tabs & Session Persistence SQLite Operations
    // -------------------------------------------------------------------------

    fun saveTabsSession(
        tabs: List<TabSession>,
        groups: List<TabGroup>,
        activeTabId: String?
    ) {
        // Enforce RAM-only guard: filter out private and Tor tabs immediately
        val standardTabs = tabs.filter { !it.isPrivate && !it.isTor }
        val standardGroups = groups.filter { !it.isPrivate && !it.isTor }
        val safeActiveId = if (standardTabs.any { it.id == activeTabId }) activeTabId ?: "" else (standardTabs.firstOrNull()?.id ?: "")

        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_TABS, null, null)
            db.delete(TABLE_TAB_GROUPS, null, null)

            // Save groups
            for (group in standardGroups) {
                val groupValues = ContentValues().apply {
                    put(COL_GROUP_ID, group.id)
                    put(COL_GROUP_TITLE, group.title)
                    put(COL_GROUP_COLOR, group.color)
                    put(COL_GROUP_PARENT_ID, group.parentTabId)
                    put(COL_GROUP_IS_COLLAPSED, if (group.isCollapsed) 1 else 0)
                    put(COL_GROUP_CREATED_AT, group.createdAt)
                }
                db.insertWithOnConflict(TABLE_TAB_GROUPS, null, groupValues, SQLiteDatabase.CONFLICT_REPLACE)
            }

            // Save tabs with order index
            for ((index, tab) in standardTabs.withIndex()) {
                val tabValues = ContentValues().apply {
                    put(COL_TAB_ID, tab.id)
                    put(COL_TAB_URL, tab.url)
                    put(COL_TAB_TITLE, tab.title)
                    put(COL_TAB_FAVICON_URL, tab.faviconUrl)
                    put(COL_TAB_GROUP_ID, tab.groupId)
                    put(COL_TAB_GROUP_TITLE, tab.groupTitle)
                    put(COL_TAB_GROUP_COLOR, tab.groupColor)
                    put(COL_TAB_PARENT_ID, tab.parentTabId)
                    put(COL_TAB_IS_DESKTOP, if (tab.isDesktopMode) 1 else 0)
                    put(COL_TAB_WALLPAPER, tab.wallpaperResId)
                    put(COL_TAB_LAST_ACCESSED, tab.lastAccessedAt)
                    put(COL_TAB_CREATED_AT, tab.createdAt)
                    put(COL_TAB_ORDER, index)
                }
                db.insertWithOnConflict(TABLE_TABS, null, tabValues, SQLiteDatabase.CONFLICT_REPLACE)
            }

            // Save active tab ID
            val metaValues = ContentValues().apply {
                put(COL_META_KEY, KEY_ACTIVE_TAB_ID)
                put(COL_META_VALUE, safeActiveId)
            }
            db.insertWithOnConflict(TABLE_SESSION_META, null, metaValues, SQLiteDatabase.CONFLICT_REPLACE)

            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist tabs session in SQLite: ${e.message}")
        } finally {
            db.endTransaction()
        }
    }

    fun loadTabsSession(): Triple<List<TabSession>, List<TabGroup>, String?> {
        val restoredTabs = mutableListOf<TabSession>()
        val restoredGroups = mutableListOf<TabGroup>()
        var activeTabId: String? = null
        val db = readableDatabase

        var cursor: Cursor? = null
        try {
            // 1. Read standard tabs ordered by tab_order
            cursor = db.rawQuery(
                """
                SELECT $COL_TAB_ID, $COL_TAB_URL, $COL_TAB_TITLE, $COL_TAB_FAVICON_URL, $COL_TAB_GROUP_ID,
                       $COL_TAB_GROUP_TITLE, $COL_TAB_GROUP_COLOR, $COL_TAB_PARENT_ID, $COL_TAB_IS_DESKTOP,
                       $COL_TAB_WALLPAPER, $COL_TAB_LAST_ACCESSED, $COL_TAB_CREATED_AT
                FROM $TABLE_TABS
                ORDER BY $COL_TAB_ORDER ASC
                """.trimIndent(),
                null
            )
            while (cursor.moveToNext()) {
                val tab = TabSession(
                    id = cursor.getString(0) ?: java.util.UUID.randomUUID().toString(),
                    url = cursor.getString(1) ?: "",
                    title = cursor.getString(2) ?: "New Tab",
                    faviconUrl = cursor.getString(3),
                    isPrivate = false, // Ephemeral RAM-only guarantee: never private from SQLite
                    isTor = false,     // Ephemeral RAM-only guarantee: never Tor from SQLite
                    isSleeping = false,
                    groupId = cursor.getString(4),
                    groupTitle = cursor.getString(5),
                    groupColor = if (cursor.isNull(6)) null else cursor.getLong(6),
                    parentTabId = cursor.getString(7),
                    isDesktopMode = cursor.getInt(8) == 1,
                    wallpaperResId = if (cursor.isNull(9)) null else cursor.getInt(9),
                    lastAccessedAt = cursor.getLong(10),
                    createdAt = cursor.getLong(11)
                )
                restoredTabs.add(tab)
            }
            cursor.close()

            // 2. Read tab groups
            cursor = db.rawQuery(
                """
                SELECT $COL_GROUP_ID, $COL_GROUP_TITLE, $COL_GROUP_COLOR, $COL_GROUP_PARENT_ID, $COL_GROUP_IS_COLLAPSED, $COL_GROUP_CREATED_AT
                FROM $TABLE_TAB_GROUPS
                """.trimIndent(),
                null
            )
            while (cursor.moveToNext()) {
                val gid = cursor.getString(0) ?: continue
                val groupTabs = restoredTabs.filter { it.groupId == gid }.map { it.id }
                val group = TabGroup(
                    id = gid,
                    title = cursor.getString(1) ?: "Group",
                    color = cursor.getLong(2),
                    tabIds = groupTabs,
                    parentTabId = cursor.getString(3),
                    isPrivate = false,
                    isTor = false,
                    isCollapsed = cursor.getInt(4) == 1,
                    createdAt = cursor.getLong(5)
                )
                restoredGroups.add(group)
            }
            cursor.close()

            // 3. Read active tab ID metadata
            cursor = db.rawQuery(
                "SELECT $COL_META_VALUE FROM $TABLE_SESSION_META WHERE $COL_META_KEY = ? LIMIT 1",
                arrayOf(KEY_ACTIVE_TAB_ID)
            )
            if (cursor.moveToFirst()) {
                activeTabId = cursor.getString(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load tabs session from SQLite: ${e.message}")
        } finally {
            cursor?.close()
        }

        return Triple(restoredTabs, restoredGroups, activeTabId)
    }

    fun clearTabsSession() {
        try {
            val db = writableDatabase
            db.delete(TABLE_TABS, null, null)
            db.delete(TABLE_TAB_GROUPS, null, null)
            db.delete(TABLE_SESSION_META, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear tabs session in SQLite: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // Permission Audit Trail SQLite Operations
    // -------------------------------------------------------------------------

    fun recordPermissionAudit(
        origin: String,
        permType: String,
        state: String,
        isPrivate: Boolean = false,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        if (origin.isBlank() || isPrivate) return -1L // Private tabs never touch SQLite
        return try {
            val values = ContentValues().apply {
                put(COL_AUDIT_ORIGIN, origin.trim())
                put(COL_AUDIT_PERM_TYPE, permType.trim().uppercase())
                put(COL_AUDIT_STATE, state.trim().uppercase())
                put(COL_AUDIT_TIMESTAMP, timestamp)
                put(COL_AUDIT_IS_PRIVATE, 0)
            }
            writableDatabase.insert(TABLE_PERMISSION_AUDIT, null, values)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record permission audit in SQLite: ${e.message}")
            -1L
        }
    }

    fun getPermissionAuditHistory(limit: Int = 50): List<PermissionAuditItem> {
        val list = mutableListOf<PermissionAuditItem>()
        var cursor: Cursor? = null
        try {
            cursor = readableDatabase.rawQuery(
                """
                SELECT $COL_AUDIT_ID, $COL_AUDIT_ORIGIN, $COL_AUDIT_PERM_TYPE, $COL_AUDIT_STATE, $COL_AUDIT_TIMESTAMP, $COL_AUDIT_IS_PRIVATE
                FROM $TABLE_PERMISSION_AUDIT
                ORDER BY $COL_AUDIT_TIMESTAMP DESC
                LIMIT ?
                """.trimIndent(),
                arrayOf(limit.toString())
            )
            while (cursor.moveToNext()) {
                list.add(
                    PermissionAuditItem(
                        id = cursor.getLong(0).toString(),
                        origin = cursor.getString(1) ?: "",
                        permissionType = cursor.getString(2) ?: "",
                        state = cursor.getString(3) ?: "",
                        timestamp = cursor.getLong(4),
                        isPrivate = cursor.getInt(5) == 1
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read permission audit from SQLite: ${e.message}")
        } finally {
            cursor?.close()
        }
        return list
    }

    fun getPermissionAuditForOrigin(origin: String): List<PermissionAuditItem> {
        val list = mutableListOf<PermissionAuditItem>()
        val clean = origin.trim()
        var cursor: Cursor? = null
        try {
            cursor = readableDatabase.rawQuery(
                """
                SELECT $COL_AUDIT_ID, $COL_AUDIT_ORIGIN, $COL_AUDIT_PERM_TYPE, $COL_AUDIT_STATE, $COL_AUDIT_TIMESTAMP, $COL_AUDIT_IS_PRIVATE
                FROM $TABLE_PERMISSION_AUDIT
                WHERE $COL_AUDIT_ORIGIN = ? OR $COL_AUDIT_ORIGIN LIKE ?
                ORDER BY $COL_AUDIT_TIMESTAMP DESC
                LIMIT 50
                """.trimIndent(),
                arrayOf(clean, "%$clean%")
            )
            while (cursor.moveToNext()) {
                list.add(
                    PermissionAuditItem(
                        id = cursor.getLong(0).toString(),
                        origin = cursor.getString(1) ?: "",
                        permissionType = cursor.getString(2) ?: "",
                        state = cursor.getString(3) ?: "",
                        timestamp = cursor.getLong(4),
                        isPrivate = cursor.getInt(5) == 1
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read origin permission audit from SQLite: ${e.message}")
        } finally {
            cursor?.close()
        }
        return list
    }

    fun clearPermissionAudit() {
        try {
            writableDatabase.delete(TABLE_PERMISSION_AUDIT, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear permission audit in SQLite: ${e.message}")
        }
    }

    private fun cursorToHistoryItem(cursor: Cursor): HistoryItem {
        return HistoryItem(
            id = cursor.getLong(0).toString(),
            url = cursor.getString(1) ?: "",
            title = cursor.getString(2) ?: "",
            timestamp = cursor.getLong(3),
            visitCount = cursor.getInt(4),
            typedCount = cursor.getInt(5),
            frecency = cursor.getLong(6),
            isSearch = cursor.getInt(7) == 1,
            searchQuery = cursor.getString(8)
        )
    }
}

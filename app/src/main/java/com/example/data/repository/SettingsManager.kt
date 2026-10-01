package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import com.example.BuildConfig
import java.io.File

data class AboutInfo(
    val developerName: String,
    val appName: String,
    val appVersion: String,
    val updatedDate: String,
    val githubAccount: String,
    val commitHash: String = ""
)

class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("muesic_settings_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BACKUP_FOLDER_PATH = "key_backup_folder_path"
        private const val KEY_BACKUP_FOLDER_URI = "key_backup_folder_uri"
        private const val KEY_DEVELOPER_NAME = "key_developer_name"
        private const val KEY_APP_NAME = "key_app_name"
        private const val KEY_APP_VERSION = "key_app_version"
        private const val KEY_UPDATED_DATE = "key_updated_date"
        private const val KEY_GITHUB_ACCOUNT = "key_github_account"

        const val DEFAULT_DEVELOPER = "Hemu1104"
        const val DEFAULT_APP_NAME = "Muesic"
        const val DEFAULT_APP_VERSION = "1.2.0"
        const val DEFAULT_UPDATED_DATE = "September 2026"
        const val DEFAULT_GITHUB = "https://github.com/Hemu1104"
    }

    /**
     * Returns the designated folder for offline song downloads and backups.
     * Uses user-selected folder if configured, or default app storage folder.
     */
    fun getBackupFolder(): File {
        val customPath = prefs.getString(KEY_BACKUP_FOLDER_PATH, null)
        if (!customPath.isNullOrBlank()) {
            try {
                val customDir = File(customPath)
                if (!customDir.exists()) {
                    customDir.mkdirs()
                }
                if (customDir.exists() && customDir.canWrite()) {
                    return customDir
                }
            } catch (e: Exception) {
                // Fall back to safe app internal/external storage
            }
        }

        // Default: external files directory under Music/MuesicDownloads
        return try {
            val defaultDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir,
                "MuesicDownloads"
            ).apply { if (!exists()) mkdirs() }
            defaultDir
        } catch (e: Exception) {
            context.filesDir
        }
    }

    fun getBackupFolderPath(): String = getBackupFolder().absolutePath

    fun setBackupFolderPath(path: String) {
        try {
            val dir = File(path)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            prefs.edit().putString(KEY_BACKUP_FOLDER_PATH, dir.absolutePath).apply()
        } catch (e: Exception) {
            // Ignore failure to write invalid path
        }
    }

    fun getBackupFolderUri(): String? = prefs.getString(KEY_BACKUP_FOLDER_URI, null)

    fun setBackupFolderUri(uriString: String?) {
        prefs.edit().putString(KEY_BACKUP_FOLDER_URI, uriString).apply()
    }

    fun getGitVersion(): String = BuildConfig.GIT_TAG_OR_VERSION.ifBlank { DEFAULT_APP_VERSION }
    fun getGitDate(): String = BuildConfig.GIT_COMMIT_DATE.ifBlank { DEFAULT_UPDATED_DATE }
    fun getGitRepo(): String = BuildConfig.GIT_REPO_URL.ifBlank { DEFAULT_GITHUB }
    fun getGitCommitHash(): String = BuildConfig.GIT_COMMIT_HASH

    fun getAboutInfo(): AboutInfo {
        val gitVersion = getGitVersion()
        val gitDate = getGitDate()
        val gitRepo = getGitRepo()
        val commit = getGitCommitHash()

        return AboutInfo(
            developerName = prefs.getString(KEY_DEVELOPER_NAME, DEFAULT_DEVELOPER) ?: DEFAULT_DEVELOPER,
            appName = prefs.getString(KEY_APP_NAME, DEFAULT_APP_NAME) ?: DEFAULT_APP_NAME,
            appVersion = prefs.getString(KEY_APP_VERSION, gitVersion) ?: gitVersion,
            updatedDate = prefs.getString(KEY_UPDATED_DATE, gitDate) ?: gitDate,
            githubAccount = prefs.getString(KEY_GITHUB_ACCOUNT, gitRepo) ?: gitRepo,
            commitHash = commit
        )
    }

    fun resetAboutInfoToGit(): AboutInfo {
        prefs.edit()
            .remove(KEY_APP_VERSION)
            .remove(KEY_UPDATED_DATE)
            .remove(KEY_GITHUB_ACCOUNT)
            .apply()
        return getAboutInfo()
    }

    fun saveAboutInfo(
        developerName: String,
        updatedDate: String,
        githubAccount: String
    ) {
        prefs.edit()
            .putString(KEY_DEVELOPER_NAME, developerName.trim().ifEmpty { DEFAULT_DEVELOPER })
            .putString(KEY_UPDATED_DATE, updatedDate.trim().ifEmpty { getGitDate() })
            .putString(KEY_GITHUB_ACCOUNT, githubAccount.trim().ifEmpty { getGitRepo() })
            .apply()
    }
}

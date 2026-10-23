package vn.blindsupport.app

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import kotlinx.coroutines.launch
import vn.blindsupport.app.core.AudioState
import vn.blindsupport.app.core.HealthCheck
import vn.blindsupport.app.core.Prefs
import vn.blindsupport.app.core.RingVolumeGuard
import vn.blindsupport.app.notify.BlindNotificationListener
import vn.blindsupport.app.notify.MessageAnnouncer
import java.util.Calendar

/**
 * "Người gác": hệ thống chạy khoảng 15 phút một lần (kể cả sau khi khởi động lại máy) để
 *  - xin kết nối lại dịch vụ đọc thông báo nếu bị Samsung ngắt;
 *  - trả lại âm lượng chuông nếu app từng bị tắt giữa chừng;
 *  - đọc to cảnh báo (tối đa 6 tiếng một lần) nếu app bị mất quyền quan trọng.
 */
class WatchdogJob : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        Watchdog.check(applicationContext)
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean = true
}

object Watchdog {
    private const val JOB_ID = 4242
    private const val WARN_INTERVAL = 6 * 3600_000L

    fun schedule(context: Context) {
        val js = context.getSystemService(JobScheduler::class.java) ?: return
        if (js.getPendingJob(JOB_ID) != null) return
        val job = JobInfo.Builder(JOB_ID, ComponentName(context, WatchdogJob::class.java))
            .setPeriodic(15 * 60_000L)
            .setPersisted(true)
            .build()
        runCatching { js.schedule(job) }
    }

    fun check(context: Context) {
        Prefs.init(context)
        RingVolumeGuard.restoreIfStale(context)
        if (HealthCheck.isNotificationListenerEnabled(context) && !BlindNotificationListener.connected) {
            runCatching {
                NotificationListenerService.requestRebind(ComponentName(context, BlindNotificationListener::class.java))
            }
        }
        val problems = HealthCheck.spokenProblems(context) ?: return
        val now = System.currentTimeMillis()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (now - lastWarn(context) < WARN_INTERVAL) return
        if (Prefs.isQuietNow(hour) || AudioState.inAnyCall(context)) return
        setLastWarn(context, now)
        App.appScope.launch { MessageAnnouncer.enqueueText(problems) }
    }

    private fun lastWarn(c: Context) =
        c.getSharedPreferences("watchdog", Context.MODE_PRIVATE).getLong("last_warn", 0L)

    private fun setLastWarn(c: Context, v: Long) =
        c.getSharedPreferences("watchdog", Context.MODE_PRIVATE).edit().putLong("last_warn", v).apply()
}

/** Chạy lại "người gác" sau khi bật máy hoặc sau khi cập nhật app. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Watchdog.schedule(context)
        Watchdog.check(context)
    }
}

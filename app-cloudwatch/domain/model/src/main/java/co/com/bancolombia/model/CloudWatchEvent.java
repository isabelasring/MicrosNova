package co.com.bancolombia.model;

public sealed interface CloudWatchEvent permits CloudWatchAlarm, BackupAlarm {
}

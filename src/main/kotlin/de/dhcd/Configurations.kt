package de.dhcd

data class Configurations(val values: List<Configuration>)

data class Configuration(
    val source: SourceConfiguration,
    val target: TargetConfiguration
)

data class SourceConfiguration(
    val hostname: String,
    val hostFingerprint: String,
    val port: Int,
    val path: String,
    val username: String,
    val privateKey: String,
) {
    override fun toString(): String {
        return "SourceConfiguration(hostname='$hostname', hostFingerprint='$hostFingerprint', port=$port, path='$path', username='$username', privateKey=<not shown>)"
    }
}

data class TargetConfiguration(
    val s3Bucket: String,
    val s3Key: String,
)
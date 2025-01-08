package de.dhcd

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.hierynomus.sshj.userauth.keyprovider.OpenSSHKeyV1KeyFile
import jakarta.enterprise.context.ApplicationScoped
import net.schmizz.sshj.SSHClient
import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm
import software.amazon.awssdk.services.s3.model.ChecksumMode
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.security.MessageDigest

@ApplicationScoped
class RequestHandler(
    private val s3Client: S3Client,
    private val configurations: Configurations,
) : RequestStreamHandler {

    companion object {
        val log = LoggerFactory.getLogger(RequestHandler::class.java)
    }

    override fun handleRequest(input: InputStream, output: OutputStream, context: Context) {
        configurations.values.forEach {
            val downloadedFile = download(it.source)
            upload(it.target, downloadedFile)
        }
    }

    fun download(config: SourceConfiguration): ByteArray {
        SSHClient().use { sshClient ->

            sshClient.addHostKeyVerifier(config.hostFingerprint)
            sshClient.connect(InetAddress.getByName(config.hostname), config.port)
            sshClient.authPublickey(config.username, OpenSSHKeyV1KeyFile().apply { init(config.privateKey, null) })

            val byteArrayInMemoryDestFile = ByteArrayInMemoryDestFile()
            sshClient.newSCPFileTransfer().download(config.path, byteArrayInMemoryDestFile)

            return byteArrayInMemoryDestFile.getByteArray()
        }
    }

    fun upload(config: TargetConfiguration, file: ByteArray) {
        val sha1Hash = file.toSHA1String()

        val headObject = runCatching {
            s3Client.headObject(
                HeadObjectRequest.builder()
                    .bucket(config.s3Bucket)
                    .key(config.s3Key)
                    .checksumMode(ChecksumMode.ENABLED)
                    .build()
            )}
            .recoverCatching { exception -> if (exception is NoSuchKeyException) null else throw exception }
            .getOrThrow()

        log.info("source file hash: $sha1Hash")
        log.info("target file hash: ${headObject?.checksumSHA1()}")
        if(headObject?.eTag() == sha1Hash) {
            log.info("Skipping upload of ${config.s3Key} as the contents are the same (SHA1: $sha1Hash)")
            return
        }

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(config.s3Bucket)
                .key(config.s3Key)
                .checksumAlgorithm(ChecksumAlgorithm.SHA1)
                .build(),
            RequestBody.fromBytes(file)
        )
    }
}

fun ByteArray.toSHA1String(): String {
    return MessageDigest.getInstance("SHA-1").digest(this).joinToString("") { "%02x".format(it) }
}
package de.dhcd

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import jakarta.enterprise.context.ApplicationScoped
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress

@ApplicationScoped
class RequestHandler(
    private val s3Client: S3Client,
    private val configurations: Configurations,
) : RequestStreamHandler {

    override fun handleRequest(input: InputStream, output: OutputStream, context: Context) {
        configurations.values.forEach {
            val downloadedFile = download(it.source)
            upload(it.target, downloadedFile)
        }
    }

    fun download(config: SourceConfiguration): ByteArray {
        SSHClient().use { sshClient ->

            // sshClient.addHostKeyVerifier() // TODO add fingerprint of server.

            sshClient.connect(InetAddress.getByName(config.hostname), config.port)
            sshClient.authPublickey(config.username, OpenSSHKeyFile().apply { init(config.privateKey, null) })

            val byteArrayInMemoryDestFile = ByteArrayInMemoryDestFile()
            sshClient.newSCPFileTransfer().download(config.path, byteArrayInMemoryDestFile)

            return byteArrayInMemoryDestFile.getByteArray()
        }
    }

    fun upload(config: TargetConfiguration, file: ByteArray) {
        s3Client.putObject(PutObjectRequest.builder()
            .bucket(config.s3Bucket)
            .key(config.s3Key)
            .build(),
            RequestBody.fromBytes(file)
        )
    }
}
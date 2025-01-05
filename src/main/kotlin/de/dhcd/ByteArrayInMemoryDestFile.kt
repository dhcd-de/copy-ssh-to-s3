package de.dhcd

import net.schmizz.sshj.xfer.InMemoryDestFile
import java.io.ByteArrayOutputStream
import java.io.OutputStream

class ByteArrayInMemoryDestFile : InMemoryDestFile() {

    private val outputStream = ByteArrayOutputStream()

    fun getByteArray(): ByteArray {
        return outputStream.toByteArray()
    }

    override fun getLength(): Long {
        // actual written bytes not necessarily the size of the bytearray.
        return outputStream.size().toLong() // upcast always safe! :)
    }

    override fun getOutputStream(): OutputStream {
        return outputStream
    }

    override fun getOutputStream(append: Boolean): OutputStream {
        if(append) {
            throw NotImplementedError()
        }
        return outputStream
    }

}
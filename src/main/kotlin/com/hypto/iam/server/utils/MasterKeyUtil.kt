package com.hypto.iam.server.utils

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files

object MasterKeyUtil {

    private const val PRIVATE_KEY = "/tmp/private_key"
    private const val PUBLIC_KEY = "/tmp/public_key"

    /**
     * Generates new EC public & private key pair (both pem and der) in `/tmp`
     */
    fun generateKeyPair() {
        try {
            val scriptStream = this::class.java.getResourceAsStream("/generate_key_pair.sh")
            requireNotNull(scriptStream) {
                "Script not found"
            }
            val file = File("/tmp/generate_key_pair.sh")
            Files.copy(scriptStream, file.toPath())

            val process = Runtime.getRuntime().exec("/bin/bash ${file.absolutePath}", null, null)
            val output = StringBuilder()
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String? = reader.readLine()
            while (line != null) {
                output.append(line.trimIndent())
                line = reader.readLine()
            }

            if (process.waitFor() == 0) {
                println(output)
            } else {
                throw IOException()
            }
        } catch (e: IOException) {
            println(e.message)
        } catch (e: InterruptedException) {
            println(e.message)
        }
    }

    fun loadPrivateKeyDer(): ByteArray {
        return loadKeyDer("$PRIVATE_KEY.der")
    }

    fun loadPublicKeyDer(): ByteArray {
        return loadKeyDer("$PUBLIC_KEY.der")
    }

    fun loadPublicKeyPem(): ByteArray {
        return loadKeyPem("$PUBLIC_KEY.pem")
    }

    fun loadPrivateKeyPem(): ByteArray {
        return loadKeyPem("$PRIVATE_KEY.pem")
    }

    private fun loadKeyDer(path: String): ByteArray {
        return File(path).readBytes()
    }

    private fun loadKeyPem(path: String): ByteArray {
        return File(path).readBytes()
    }
}

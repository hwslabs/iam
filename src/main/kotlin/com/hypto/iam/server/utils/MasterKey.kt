package com.hypto.iam.server.utils

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

object MasterKey {

    private const val PRIVATE_KEY = "/tmp/private_key.der"
    private const val PUBLIC_KEY = "/tmp/public_key.der"

    /**
     * Generates new EC public & private key pair (both pem and der) in `/tmp`
     */
    fun generateKeyPair() {
        try {
            val scriptPath = this::class.java.classLoader.getResource("generate_key_pair.sh")?.path
            println(scriptPath)

            val process = Runtime.getRuntime().exec("/bin/bash $scriptPath", null, null)
            val output = StringBuilder()
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String? = reader.readLine()
            while (line != null) {
                output.append(line.trimIndent())
                line = reader.readLine()
            }

            if (process.waitFor() == 0) { println(output) } else { throw IOException() }
        } catch (e: IOException) {
            println(e.message)
        } catch (e: InterruptedException) {
            println(e.message)
        }
    }

    fun loadPrivateKeyDer(): ByteArray {
        return loadKeyDer(PRIVATE_KEY)
    }

    fun loadPublicKeyDer(): ByteArray {
        return loadKeyDer(PUBLIC_KEY)
    }

    private fun loadKeyDer(path: String): ByteArray {
        return File(path).readBytes()
    }
}

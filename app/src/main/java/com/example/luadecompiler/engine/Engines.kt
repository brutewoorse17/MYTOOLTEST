package com.example.luadecompiler.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

enum class LuaBytecodeVersion { LUA_51, LUA_52, LUA_53, LUAJIT, UNKNOWN }

data class LuaChunkInfo(
  val isBytecode: Boolean,
  val version: LuaBytecodeVersion,
  val details: String
)

object LuaDetector {
  fun detect(bytes: ByteArray): LuaChunkInfo {
    if (bytes.size < 4) return LuaChunkInfo(false, LuaBytecodeVersion.UNKNOWN, "Too small")
    if (bytes[0] == 0x1B.toByte()) {
      // Bytecode; check magic
      if (bytes.size >= 4 && bytes[1] == 'L'.code.toByte()) {
        val third = bytes[2]
        return if (third == 'u'.code.toByte() && bytes[3] == 'a'.code.toByte()) {
          // Standard Lua 5.x: next byte is version
          val ver = if (bytes.size >= 5) bytes[4].toInt() and 0xFF else -1
          val version = when (ver) {
            0x51 -> LuaBytecodeVersion.LUA_51
            0x52 -> LuaBytecodeVersion.LUA_52
            0x53 -> LuaBytecodeVersion.LUA_53
            else -> LuaBytecodeVersion.UNKNOWN
          }
          LuaChunkInfo(true, version, "Lua bytecode 0x%02X".format(ver))
        } else if (third == 'J'.code.toByte()) {
          // LuaJIT: 1B 4C 4A 01 02 ...
          LuaChunkInfo(true, LuaBytecodeVersion.LUAJIT, "LuaJIT bytecode")
        } else {
          LuaChunkInfo(true, LuaBytecodeVersion.UNKNOWN, "Unknown 1B 4C ${third}")
        }
      }
      return LuaChunkInfo(true, LuaBytecodeVersion.UNKNOWN, "Starts with ESC but not Lua/LJ")
    }
    return LuaChunkInfo(false, LuaBytecodeVersion.UNKNOWN, "Not bytecode (likely text)")
  }
}

interface DecompilerEngine {
  val name: String
  fun supports(version: LuaBytecodeVersion): Boolean
  suspend fun decompile(bytes: ByteArray): Result<String>
}

class UnluacEngine : DecompilerEngine {
  override val name: String = "unluac (Lua 5.1)"
  override fun supports(version: LuaBytecodeVersion): Boolean = version == LuaBytecodeVersion.LUA_51

  override suspend fun decompile(bytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
    try {
      val clazz = try { Class.forName("unluac.Main") } catch (e: Throwable) { null }
      if (clazz == null) {
        return@withContext Result.failure(IllegalStateException("unluac.jar not found. Place unluac.jar into app/libs and rebuild."))
      }

      // Write input bytes to a temp file
      val tempIn: File = File.createTempFile("chunk", ".luac")
      tempIn.writeBytes(bytes)

      val originalOut = System.out
      val originalErr = System.err
      val baos = ByteArrayOutputStream()
      val eaos = ByteArrayOutputStream()
      val psOut = PrintStream(baos)
      val psErr = PrintStream(eaos)
      try {
        System.setOut(psOut)
        System.setErr(psErr)
        // Invoke unluac.Main.main(String[] args)
        val method = clazz.getMethod("main", Array<String>::class.java)
        method.invoke(null, arrayOf(tempIn.absolutePath))
      } finally {
        System.setOut(originalOut)
        System.setErr(originalErr)
        psOut.flush()
        psErr.flush()
        tempIn.delete()
      }

      val stdout = baos.toString(Charsets.UTF_8.name())
      val stderr = eaos.toString(Charsets.UTF_8.name())

      if (stdout.isNotBlank()) Result.success(stdout)
      else Result.failure(IllegalStateException("unluac produced no output${if (stderr.isNotBlank()) ": $stderr" else ""}"))
    } catch (t: Throwable) {
      Result.failure(t)
    }
  }
}

class LuadecEngine : DecompilerEngine {
  override val name: String = "luadec (Lua 5.1–5.3 via NDK)"
  override fun supports(version: LuaBytecodeVersion): Boolean =
    version == LuaBytecodeVersion.LUA_51 || version == LuaBytecodeVersion.LUA_52 || version == LuaBytecodeVersion.LUA_53

  override suspend fun decompile(bytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
    Result.failure(UnsupportedOperationException("Native luadec not integrated yet"))
  }
}

object EngineRouter {
  private val unluac by lazy { UnluacEngine() }
  private val luadec by lazy { LuadecEngine() }

  fun pick(version: LuaBytecodeVersion): DecompilerEngine = when (version) {
    LuaBytecodeVersion.LUA_51 -> unluac
    LuaBytecodeVersion.LUA_52, LuaBytecodeVersion.LUA_53 -> luadec
    else -> object : DecompilerEngine {
      override val name: String = "unsupported"
      override fun supports(version: LuaBytecodeVersion): Boolean = false
      override suspend fun decompile(bytes: ByteArray): Result<String> = Result.failure(UnsupportedOperationException("Unsupported Lua version"))
    }
  }
}
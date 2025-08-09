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
      // Prefer direct API if present in the dependency
      try {
        val BHeader = Class.forName("unluac.parse.BHeader")
        val LFunction = Class.forName("unluac.decompile.LFunction")
        val Decompiler = Class.forName("unluac.decompile.Decompiler")
        val DecompilerState = Class.forName("unluac.decompile.DecompilerState")

        val headerCtor = BHeader.getConstructor(java.io.InputStream::class.java)
        val header = headerCtor.newInstance(bytes.inputStream())
        val lmainField = BHeader.getDeclaredField("lmain").apply { isAccessible = true }
        val lmain = lmainField.get(header)

        val state = DecompilerState.getDeclaredConstructor(BHeader).newInstance(header)
        val decomp = Decompiler.getDeclaredConstructor(DecompilerState, LFunction).newInstance(state, lmain)
        val out = java.io.ByteArrayOutputStream()
        val pout = java.io.PrintStream(out)
        val printMethod = Decompiler.getMethod("print", java.io.PrintStream::class.java)
        printMethod.invoke(decomp, pout)
        return@withContext Result.success(out.toString(Charsets.UTF_8.name()))
      } catch (_: Throwable) {
        // Fallback to invoking Main and capturing stdout
      }

      val mainClazz = try { Class.forName("unluac.Main") } catch (e: Throwable) { null }
      if (mainClazz == null) {
        return@withContext Result.failure(IllegalStateException("unluac not found. Ensure JitPack dep or libs/unluac.jar is present."))
      }

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
        val method = mainClazz.getMethod("main", Array<String>::class.java)
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

object LuadecNative {
  init {
    for (lib in arrayOf("luadec51_bridge","luadec52_bridge","luadec53_bridge")) {
      try { System.loadLibrary(lib) } catch (_: Throwable) {}
    }
  }
  external fun decompile(bytes: ByteArray, version: Int): String?
}

class LuadecEngine : DecompilerEngine {
  override val name: String = "luadec (Lua 5.1–5.3 via NDK)"
  override fun supports(version: LuaBytecodeVersion): Boolean =
    version == LuaBytecodeVersion.LUA_51 || version == LuaBytecodeVersion.LUA_52 || version == LuaBytecodeVersion.LUA_53

  override suspend fun decompile(bytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
    try {
      val detected = LuaDetector.detect(bytes)
      val verCode = when (detected.version) {
        LuaBytecodeVersion.LUA_51 -> 51
        LuaBytecodeVersion.LUA_52 -> 52
        LuaBytecodeVersion.LUA_53 -> 53
        else -> return@withContext Result.failure(IllegalArgumentException("Unsupported Lua version for luadec"))
      }
      val out = LuadecNative.decompile(bytes, verCode)
      if (out != null && !out.startsWith("[luadec stub]")) Result.success(out) else Result.failure(UnsupportedOperationException(out ?: "luadec returned null"))
    } catch (t: Throwable) {
      Result.failure(t)
    }
  }
}

object EngineRouter {
  private val unluac by lazy { UnluacEngine() }
  private val luadec by lazy { LuadecEngine() }

  fun enginesFor(version: LuaBytecodeVersion): List<DecompilerEngine> = when (version) {
    LuaBytecodeVersion.LUA_51 -> listOf(unluac, luadec)
    LuaBytecodeVersion.LUA_52, LuaBytecodeVersion.LUA_53 -> listOf(luadec)
    else -> emptyList()
  }

  fun unsupported(): DecompilerEngine = object : DecompilerEngine {
    override val name: String = "unsupported"
    override fun supports(version: LuaBytecodeVersion): Boolean = false
    override suspend fun decompile(bytes: ByteArray): Result<String> =
      Result.failure(UnsupportedOperationException("Unsupported Lua version"))
  }
}
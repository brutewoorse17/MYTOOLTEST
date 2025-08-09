#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_luadecompiler_engine_LuadecNative_decompile(
    JNIEnv* env,
    jobject /* thisObj */,
    jbyteArray /*byteArray*/,
    jint version)
{
#if defined(LUADEC_VER)
  int libver = LUADEC_VER;
#else
  int libver = 0;
#endif
  std::string msg = std::string("[luadec stub] bridge ") + std::to_string(libver) + " received version " + std::to_string(version);
  return env->NewStringUTF(msg.c_str());
}
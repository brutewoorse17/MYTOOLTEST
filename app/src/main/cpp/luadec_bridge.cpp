#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_luadecompiler_engine_LuadecNative_decompile(
    JNIEnv* env,
    jobject /* thisObj */,
    jstring jpath,
    jint version)
{
#if defined(LUADEC_VER)
  int libver = LUADEC_VER;
#else
  int libver = 0;
#endif
  const char* cpath = env->GetStringUTFChars(jpath, nullptr);
  std::string path = cpath ? cpath : "";
  if (cpath) env->ReleaseStringUTFChars(jpath, cpath);

  std::string msg = std::string("[luadec stub] bridge ") + std::to_string(libver) + " would decompile '" + path + "' for Lua " + std::to_string(version);
  return env->NewStringUTF(msg.c_str());
}
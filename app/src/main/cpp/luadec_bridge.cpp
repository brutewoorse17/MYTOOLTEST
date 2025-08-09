#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_luadecompiler_engine_LuadecNative_decompile(
    JNIEnv* env,
    jobject /* thisObj */,
    jbyteArray byteArray,
    jint version)
{
  // TODO: integrate luadec C sources and route by version
  const char* ver = "unknown";
  switch (version) {
    case 51: ver = "5.1"; break;
    case 52: ver = "5.2"; break;
    case 53: ver = "5.3"; break;
  }
  std::string msg = std::string("[luadec stub] Native support not yet integrated for Lua ") + ver;
  return env->NewStringUTF(msg.c_str());
}
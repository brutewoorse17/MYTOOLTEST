#include <jni.h>
#include <string>

static jstring make_stub(JNIEnv* env, int libver, const char* path) {
  std::string msg = std::string("[luadec stub] bridge ") + std::to_string(libver) + " would decompile '" + (path ? path : "") + "'";
  return env->NewStringUTF(msg.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_luadecompiler_engine_LuadecNative_00024Companion_decompile51(
    JNIEnv* env,
    jobject /* thisObj */,
    jstring jpath)
{
  const char* cpath = env->GetStringUTFChars(jpath, nullptr);
  jstring out = make_stub(env, 51, cpath);
  if (cpath) env->ReleaseStringUTFChars(jpath, cpath);
  return out;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_luadecompiler_engine_LuadecNative_00024Companion_decompile52(
    JNIEnv* env,
    jobject /* thisObj */,
    jstring jpath)
{
  const char* cpath = env->GetStringUTFChars(jpath, nullptr);
  jstring out = make_stub(env, 52, cpath);
  if (cpath) env->ReleaseStringUTFChars(jpath, cpath);
  return out;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_luadecompiler_engine_LuadecNative_00024Companion_decompile53(
    JNIEnv* env,
    jobject /* thisObj */,
    jstring jpath)
{
  const char* cpath = env->GetStringUTFChars(jpath, nullptr);
  jstring out = make_stub(env, 53, cpath);
  if (cpath) env->ReleaseStringUTFChars(jpath, cpath);
  return out;
}
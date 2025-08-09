#include <jni.h>
#include <string>
#include <vector>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>
#include <stdio.h>

static jstring make_stub(JNIEnv* env, int libver, const char* path) {
  std::string msg = std::string("[luadec stub] bridge ") + std::to_string(libver) + " would decompile '" + (path ? path : "") + "'";
  return env->NewStringUTF(msg.c_str());
}

static std::string run_luadec_and_capture(int libver, const char* path) {
#ifndef LUADEC_HAVE_SOURCES
  (void)libver; (void)path;
  return std::string();
#else
  if (!path) return std::string();
  // Prepare argv: luadec <path>
  std::vector<const char*> argv;
  argv.push_back("luadec");
  argv.push_back(path);
  int argc = (int)argv.size();

  // Create a pipe to capture stdout
  int pipefd[2];
  if (pipe(pipefd) != 0) {
    return std::string();
  }
  int stdout_fd = dup(fileno(stdout));
  fflush(stdout);
  dup2(pipefd[1], fileno(stdout));
  close(pipefd[1]);

  // Call versioned luadec main
  int rc = 0;
  #if LUADEC_VER == 51
    extern "C" int luadec51_main(int, const char**);
    rc = luadec51_main(argc, argv.data());
  #elif LUADEC_VER == 52
    extern "C" int luadec52_main(int, const char**);
    rc = luadec52_main(argc, argv.data());
  #elif LUADEC_VER == 53
    extern "C" int luadec53_main(int, const char**);
    rc = luadec53_main(argc, argv.data());
  #else
    rc = -1;
  #endif

  // Restore stdout
  fflush(stdout);
  dup2(stdout_fd, fileno(stdout));
  close(stdout_fd);

  // Read from pipe
  std::string output;
  char buffer[4096];
  ssize_t n;
  while ((n = read(pipefd[0], buffer, sizeof(buffer))) > 0) {
    output.append(buffer, buffer + n);
  }
  close(pipefd[0]);

  if (rc != 0 && output.empty()) {
    return std::string();
  }
  return output;
#endif
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_luadecompiler_engine_LuadecNative_decompile51(
    JNIEnv* env,
    jobject /* thisObj */,
    jstring jpath)
{
  const char* cpath = env->GetStringUTFChars(jpath, nullptr);
#ifdef LUADEC_HAVE_SOURCES
  std::string out = run_luadec_and_capture(51, cpath);
  if (cpath) env->ReleaseStringUTFChars(jpath, cpath);
  if (out.empty()) return make_stub(env, 51, nullptr);
  return env->NewStringUTF(out.c_str());
#else
  jstring out = make_stub(env, 51, cpath);
  if (cpath) env->ReleaseStringUTFChars(jpath, cpath);
  return out;
#endif
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_luadecompiler_engine_LuadecNative_decompile52(
    JNIEnv* env,
    jobject /* thisObj */,
    jstring jpath)
{
  const char* cpath = env->GetStringUTFChars(jpath, nullptr);
#ifdef LUADEC_HAVE_SOURCES
  std::string out = run_luadec_and_capture(52, cpath);
  if (cpath) env->ReleaseStringUTFChars(jpath, cpath);
  if (out.empty()) return make_stub(env, 52, nullptr);
  return env->NewStringUTF(out.c_str());
#else
  jstring out = make_stub(env, 52, cpath);
  if (cpath) env->ReleaseStringUTFChars(jpath, cpath);
  return out;
#endif
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_luadecompiler_engine_LuadecNative_decompile53(
    JNIEnv* env,
    jobject /* thisObj */,
    jstring jpath)
{
  const char* cpath = env->GetStringUTFChars(jpath, nullptr);
#ifdef LUADEC_HAVE_SOURCES
  std::string out = run_luadec_and_capture(53, cpath);
  if (cpath) env->ReleaseStringUTFChars(jpath, cpath);
  if (out.empty()) return make_stub(env, 53, nullptr);
  return env->NewStringUTF(out.c_str());
#else
  jstring out = make_stub(env, 53, cpath);
  if (cpath) env->ReleaseStringUTFChars(jpath, cpath);
  return out;
#endif
}
@ECHO OFF

SET DIR=%~dp0
SET APP_BASE_NAME=%~n0

set DEFAULT_JVM_OPTS=-Xmx64m -Xms64m

SET CLASSPATH=%DIR%\gradle\wrapper\gradle-wrapper.jar

SET JAVA_EXE=java.exe

"%JAVA_EXE%" %DEFAULT_JVM_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
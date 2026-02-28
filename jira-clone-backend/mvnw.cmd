@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one or more
@REM contributor license agreements.  See the NOTICE file distributed with
@REM this work for additional information regarding copyright ownership.
@REM The ASF licenses this file to You under the Apache License, Version 2.0
@REM (the "License"); you may not use this file except in compliance with
@REM the License.  You may obtain a copy of the License at
@REM
@REM      https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.2.0
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "__MVNW_ARG0_NAME__=%~nx0")
@SET __ MVNW_CMD__=
@SET "__MVNW_ERROR__="
@SET "__MVNW_PWSH_RUN_CMD__=powershell.exe"

@SETLOCAL
@SET MAVEN_WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
@SET MAVEN_WRAPPER_PROPERTIES="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties"
@SET DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

@FOR /F "usebackq tokens=1,2 delims==" %%A IN (%MAVEN_WRAPPER_PROPERTIES%) DO (
    @IF "%%A"=="wrapperUrl" SET DOWNLOAD_URL=%%B
)

@SET WRAPPER_JAR="%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6-bin\apache-maven-3.9.6-bin.zip"
@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

@SET DOWNLOAD_JAR_URL=%DOWNLOAD_URL%

@IF EXIST %MAVEN_WRAPPER_JAR% (
    goto RunMavenWrapper
)

@ECHO Downloading Maven Wrapper from: %DOWNLOAD_URL%
%__MVNW_PWSH_RUN_CMD__% -Command "&{ try { $webclient = new-object System.Net.WebClient; $webclient.DownloadFile('%DOWNLOAD_URL%', $MAVEN_WRAPPER_JAR) } catch [Exception] { throw $_ } }"

:RunMavenWrapper
@SET JAVA_HOME
@IF NOT "%JAVA_HOME%"=="" (
    @SET JAVACMD=%JAVA_HOME%/bin/java.exe
    @IF NOT EXIST "%JAVACMD%" (
        @echo The JAVA_HOME environment variable is not defined correctly, so mvnw cannot run.
        @echo JAVA_HOME is set to "%JAVA_HOME%", but "%JAVACMD%" does not exist.
        @EXIT /B 1
    )
) ELSE (
    @SET JAVACMD=java.exe
)

@SET MAVEN_JAVA_EXE="%JAVACMD%"

%MAVEN_JAVA_EXE% ^
  %MAVEN_OPTS% ^
  %MAVEN_DEBUG_OPTS% ^
  -classpath %MAVEN_WRAPPER_JAR% ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  %WRAPPER_LAUNCHER% %MAVEN_CONFIG% %*

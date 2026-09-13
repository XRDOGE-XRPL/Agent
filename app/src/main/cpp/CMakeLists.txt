cmake_minimum_required(VERSION 3.22.1)
project(agentkern LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(AGENT_NATIVE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../native)

add_library(agentkern SHARED
    jni_bruecke.cpp
    ${AGENT_NATIVE_DIR}/src/protokollierung.cpp
    ${AGENT_NATIVE_DIR}/src/json_werkzeuge.cpp
    ${AGENT_NATIVE_DIR}/src/dateisystem.cpp
    ${AGENT_NATIVE_DIR}/src/http_klient.cpp
    ${AGENT_NATIVE_DIR}/src/protokoll.cpp
    ${AGENT_NATIVE_DIR}/src/git.cpp
    ${AGENT_NATIVE_DIR}/src/build.cpp
    ${AGENT_NATIVE_DIR}/src/ollama.cpp
    ${AGENT_NATIVE_DIR}/src/schleife.cpp
)

target_include_directories(agentkern PRIVATE ${AGENT_NATIVE_DIR}/include)
find_library(log-lib log)
target_link_libraries(agentkern ${log-lib})

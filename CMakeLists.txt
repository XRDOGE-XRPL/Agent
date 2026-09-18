cmake_minimum_required(VERSION 3.16)
project(AgentStandaloneNative LANGUAGES CXX)

option(AGENT_BUILD_TESTS "Build the native test suite" OFF)
option(AGENT_BUILD_HOST_TOOLS "Build optional host-only utilities" OFF)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

include(cmake/standalone_android_deps.cmake)

if(AGENT_BUILD_TESTS)
    include(CTest)
    enable_testing()
endif()

# Keep the project self-contained and independent from the external AOSP Soong/Make
# graph. Only the local native runtime and its immediate static dependencies are built.
add_subdirectory(native)

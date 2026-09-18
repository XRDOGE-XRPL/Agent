cmake_minimum_required(VERSION 3.16)
cmake_policy(SET CMP0002 NEW)
project(AgentStandaloneNative LANGUAGES CXX)

option(AGENT_BUILD_TESTS "Build the native test suite" OFF)
option(AGENT_BUILD_HOST_TOOLS "Build optional host-only utilities" OFF)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set_property(GLOBAL PROPERTY USE_FOLDERS OFF)

# Keep the project fully self-contained and independent from any external AOSP/Soong
# tree or vendored submodule checkout. The dependency shims live in-tree and are valid
# static libraries with explicit local sources.
include(cmake/standalone_android_deps.cmake)

if(AGENT_BUILD_TESTS)
    include(CTest)
    enable_testing()
endif()

add_subdirectory(native)

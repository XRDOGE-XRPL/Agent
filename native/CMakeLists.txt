cmake_minimum_required(VERSION 3.16)
project(AgentNative LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(AGENT_CORE_SOURCES
    src/protokollierung.cpp
    src/json_werkzeuge.cpp
    src/dateisystem.cpp
    src/http_klient.cpp
    src/protokoll.cpp
    src/git.cpp
    src/build.cpp
    src/ollama.cpp
    src/schleife.cpp
)

add_library(agentkern_static STATIC ${AGENT_CORE_SOURCES})
target_include_directories(agentkern_static
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_compile_definitions(agentkern_static PUBLIC AGENT_STANDALONE_BUILD=1)

# Avoid AOSP/Soong globals, missing submodule paths, and target-name collisions.
# The runtime core is self-contained and only links the platform libraries that are
# actually available in a native standalone toolchain.
if(WIN32)
    target_link_libraries(agentkern_static PUBLIC ws2_32)
endif()

if(MINGW)
    target_link_options(agentkern_static PUBLIC -static-libgcc -static-libstdc++ -static)
    target_link_libraries(agentkern_static PUBLIC winpthread)
endif()

if(UNIX AND NOT APPLE AND NOT ANDROID)
    target_link_libraries(agentkern_static PUBLIC pthread)
endif()

# Only keep the single production entry point and any directly related unit tests.
add_executable(agentenlauf cli/haupteinstieg.cpp)
target_link_libraries(agentenlauf PRIVATE agentkern_static)

if(AGENT_BUILD_TESTS)
    function(agent_test name)
        add_executable(${name} tests/${name}.cpp)
        target_link_libraries(${name} PRIVATE agentkern_static)
        add_test(NAME ${name} COMMAND ${name})
    endfunction()

    agent_test(dateisystem_test)
    agent_test(json_protokoll_test)
    agent_test(git_befehl_test)
    agent_test(http_url_test)
    agent_test(schleife_mock_test)
endif()

if(AGENT_BUILD_HOST_TOOLS)
    message(STATUS "Host-only utility builds are enabled; keep this disabled for Android/NDK standalone builds.")
endif()

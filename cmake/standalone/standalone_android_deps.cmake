# Self-contained dependency declarations for Android/NDK standalone builds.
# These libraries intentionally own no external AOSP source tree and use a tiny local
# placeholder object so CMake never sees an empty target definition.

function(agent_add_stub_library target_name)
    set(_stub_source "${CMAKE_CURRENT_SOURCE_DIR}/cmake/standalone/empty.cpp")
    if(NOT EXISTS "${_stub_source}")
        message(FATAL_ERROR "Missing standalone stub source: ${_stub_source}")
    endif()

    add_library(${target_name} STATIC "${_stub_source}")
    target_include_directories(${target_name} INTERFACE ${CMAKE_CURRENT_SOURCE_DIR})
endfunction()

agent_add_stub_library(libbase)
agent_add_stub_library(libcutils)
agent_add_stub_library(libutils)
agent_add_stub_library(libziparchive)
agent_add_stub_library(expat)

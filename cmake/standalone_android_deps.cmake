# Canonical self-contained dependency declarations for Android/NDK standalone builds.
# These are intentionally local shims: each target is created from a real source file in
# this repository so CMake never sees an empty target definition or missing sources.

function(agent_add_stub_library target_name)
    set(_stub_dir "${CMAKE_CURRENT_LIST_DIR}/standalone")
    set(_stub_source "${_stub_dir}/empty.cpp")

    if(NOT EXISTS "${_stub_source}")
        message(FATAL_ERROR "Missing standalone stub source: ${_stub_source}")
    endif()

    if(TARGET ${target_name})
        return()
    endif()

    add_library(${target_name} STATIC "${_stub_source}")
    target_include_directories(${target_name} INTERFACE "${_stub_dir}")
    set_target_properties(${target_name} PROPERTIES EXPORT_NAME ${target_name})
endfunction()

set(_agent_standalone_libs
    libbase
    libcutils
    libutils
    libziparchive
    expat
)

foreach(_agent_dep IN LISTS _agent_standalone_libs)
    agent_add_stub_library(${_agent_dep})
endforeach()

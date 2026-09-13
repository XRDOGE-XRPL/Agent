cmake_minimum_required(VERSION 3.16)
project(AutonomerEntwicklungsagent LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
enable_testing()

set(AGENT_SOURCES
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

add_library(agentkern_static STATIC ${AGENT_SOURCES})
target_include_directories(agentkern_static PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)
if(WIN32)
    target_link_libraries(agentkern_static PUBLIC ws2_32)
endif()
if(MINGW)
    target_link_options(agentkern_static PUBLIC -static-libgcc -static-libstdc++ -static)
    target_link_libraries(agentkern_static PUBLIC winpthread)
endif()

add_executable(agentenlauf cli/haupteinstieg.cpp)
target_link_libraries(agentenlauf PRIVATE agentkern_static)

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

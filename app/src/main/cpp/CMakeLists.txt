cmake_minimum_required(VERSION 3.22.1)
project(luadec_bridges)

set(BRIDGE_SRC ${CMAKE_CURRENT_LIST_DIR}/luadec_bridge.cpp)

add_library(luadec51_bridge SHARED ${BRIDGE_SRC})
target_compile_definitions(luadec51_bridge PRIVATE LUADEC_VER=51)

add_library(luadec52_bridge SHARED ${BRIDGE_SRC})
target_compile_definitions(luadec52_bridge PRIVATE LUADEC_VER=52)

add_library(luadec53_bridge SHARED ${BRIDGE_SRC})
target_compile_definitions(luadec53_bridge PRIVATE LUADEC_VER=53)

find_library(log-lib log)

target_link_libraries(luadec51_bridge ${log-lib})
target_link_libraries(luadec52_bridge ${log-lib})
target_link_libraries(luadec53_bridge ${log-lib})
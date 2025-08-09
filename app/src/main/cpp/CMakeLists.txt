cmake_minimum_required(VERSION 3.22.1)
project(luadec_bridges)

set(BRIDGE_SRC ${CMAKE_CURRENT_LIST_DIR}/luadec_bridge.cpp)
set(LUADEC_ROOT ${CMAKE_CURRENT_LIST_DIR}/third_party/luadec)

function(configure_bridge target ver)
  add_library(${target} SHARED ${BRIDGE_SRC})
  target_compile_definitions(${target} PRIVATE LUADEC_VER=${ver})
  target_compile_features(${target} PRIVATE cxx_std_17)
  # Attempt to include luadec sources if vendored
  if(EXISTS ${LUADEC_ROOT})
    message(STATUS "Configuring ${target} with luadec sources (version ${ver})")
    # Common luadec sources
    file(GLOB LUADEC_COMMON
      ${LUADEC_ROOT}/luadec/*.c
    )
    # Lua core sources per version
    if(${ver} EQUAL 51)
      set(LUA_CORE_DIR ${LUADEC_ROOT}/lua-5.1/src)
    elseif(${ver} EQUAL 52)
      set(LUA_CORE_DIR ${LUADEC_ROOT}/lua-5.2/src)
    else()
      set(LUA_CORE_DIR ${LUADEC_ROOT}/lua-5.3/src)
    endif()
    if(EXISTS ${LUA_CORE_DIR})
      file(GLOB LUA_CORE
        ${LUA_CORE_DIR}/*.c
      )
      # Exclude standalone binaries if present
      list(FILTER LUA_CORE EXCLUDE REGEX ".*/lua(\\.c)?$")
      list(FILTER LUA_CORE EXCLUDE REGEX ".*/luac(\\.c)?$")
      target_sources(${target} PRIVATE ${LUADEC_COMMON} ${LUA_CORE})
      target_compile_definitions(${target} PRIVATE LUADEC_HAVE_SOURCES=1)
      # Expose luadec main() under a versioned name for JNI to call
      target_compile_definitions(${target} PRIVATE main=luadec${ver}_main)
      if(${ver} EQUAL 51)
        target_compile_definitions(${target} PRIVATE LUA_VERSION_NUM=501)
      elseif(${ver} EQUAL 52)
        target_compile_definitions(${target} PRIVATE LUA_VERSION_NUM=502)
      else()
        target_compile_definitions(${target} PRIVATE LUA_VERSION_NUM=503)
      endif()
      # Include paths
      target_include_directories(${target} PRIVATE ${LUADEC_ROOT}/luadec ${LUA_CORE_DIR})
      # Quiet some warnings
      target_compile_options(${target} PRIVATE -Wno-unused-parameter -Wno-sign-compare)
    else()
      message(WARNING "Lua core not found for version ${ver} under ${LUA_CORE_DIR}. Building stub for ${target}.")
    endif()
  else()
    message(STATUS "luadec not vendored; building stub for ${target}")
  endif()
  find_library(log-lib log)
  target_link_libraries(${target} ${log-lib})
endfunction()

configure_bridge(luadec51_bridge 51)
configure_bridge(luadec52_bridge 52)
configure_bridge(luadec53_bridge 53)
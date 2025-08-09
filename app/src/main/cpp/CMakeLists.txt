cmake_minimum_required(VERSION 3.22.1)
project(luadec_bridge)

add_library(luadec_bridge SHARED
  luadec_bridge.cpp
)

target_compile_features(luadec_bridge PRIVATE cxx_std_17)

find_library(log-lib log)

target_link_libraries(luadec_bridge
  ${log-lib}
)
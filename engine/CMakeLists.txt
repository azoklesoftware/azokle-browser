cmake_minimum_required(VERSION 3.20)
project(azokle_engine LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(azokle_engine STATIC
    gecko/bridge/GeckoEngineBridge.cpp
)

target_include_directories(azokle_engine PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/gecko/bridge
    ${CMAKE_CURRENT_SOURCE_DIR}/gecko/integration
)

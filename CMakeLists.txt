cmake_minimum_required(VERSION 3.20)
project(azokle_browser_workspace LANGUAGES CXX C)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE "RelWithDebInfo" CACHE STRING "Choose the type of build." FORCE)
endif()

# Global security and performance compiler flags
if(CMAKE_CXX_COMPILER_ID MATCHES "GNU|Clang")
    add_compile_options(
        -Wall
        -Wextra
        -Wpedantic
        -Wno-unused-parameter
        -fPIC
    )
endif()

# 1. Cross-Platform Engine (Gecko Quantum, Mock, Desktop Controller)
add_subdirectory(engine)

# 2. Native Applications
if(UNIX AND NOT APPLE)
    add_subdirectory(apps/linux)
endif()

option(BUILD_WINDOWS "Build Windows presentation shell" OFF)
if(WIN32 OR BUILD_WINDOWS)
    add_subdirectory(apps/windows)
endif()

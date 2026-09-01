cmake_minimum_required(VERSION 3.20)
project(azokle_browser_workspace LANGUAGES CXX C)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 1. Cross-Platform Engine
add_subdirectory(engine)

# 2. Linux Native Application
add_subdirectory(apps/linux)

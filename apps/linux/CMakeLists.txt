cmake_minimum_required(VERSION 3.20)
project(azokle_browser_linux LANGUAGES CXX C)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(RUST_LIB_DIR "${CMAKE_SOURCE_DIR}/target/debug")

add_executable(azokle-browser-linux
    src/main.cpp
    src/window/AzokleBrowserWindow.cpp
    src/widget/AzokleGeckoWidget.cpp
    src/tabs/AzokleTabManager.cpp
    src/ui/AzokleDrawUtils.cpp
)

target_include_directories(azokle-browser-linux PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/src
    ${CMAKE_SOURCE_DIR}/bindings/c/include
    ${CMAKE_SOURCE_DIR}/engine/include
    ${CMAKE_SOURCE_DIR}/engine/gecko/bridge
)

target_link_directories(azokle-browser-linux PRIVATE
    ${RUST_LIB_DIR}
)

target_link_libraries(azokle-browser-linux PRIVATE
    azokle_engine
    azokle_c
    X11
    pthread
    dl
    m
)

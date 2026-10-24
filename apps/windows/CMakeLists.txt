cmake_minimum_required(VERSION 3.20)
project(azokle_browser_windows LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    set(RUST_LIB_DIR "${CMAKE_SOURCE_DIR}/target/release")
else()
    set(RUST_LIB_DIR "${CMAKE_SOURCE_DIR}/target/debug")
endif()

add_executable(azokle-browser-windows
    src/main.cpp
    src/window/AzokleWin32Window.cpp
)

target_include_directories(azokle-browser-windows PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/src
    ${CMAKE_SOURCE_DIR}/engine/include
    ${CMAKE_SOURCE_DIR}/engine/desktop/include
    ${CMAKE_SOURCE_DIR}/engine/gecko/bridge
    ${CMAKE_SOURCE_DIR}/bindings/c/include
)

target_link_directories(azokle-browser-windows PRIVATE
    ${RUST_LIB_DIR}
    ${CMAKE_SOURCE_DIR}/target/release
    ${CMAKE_SOURCE_DIR}/target/debug
)

if(WIN32)
    target_link_libraries(azokle-browser-windows PRIVATE
        azokle_engine
        azokle_c
        ws2_32
        userenv
        bcrypt
        ntdll
        secur32
        crypt32
        ncrypt
    )
else()
    target_link_libraries(azokle-browser-windows PRIVATE
        azokle_engine
        azokle_c
        pthread
        dl
        m
    )
endif()


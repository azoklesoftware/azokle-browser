cmake_minimum_required(VERSION 3.20)
project(azokle_browser_linux LANGUAGES CXX C)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimization Flags
if(CMAKE_BUILD_TYPE STREQUAL "Release" OR CMAKE_BUILD_TYPE STREQUAL "RelWithDebInfo")
    add_compile_options(-O3 -pipe -fno-plt)
    if(NOT MSVC)
        add_compile_options(-flto)
    endif()
endif()

# Determine Rust artifact directory
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    set(RUST_LIB_DIR "${CMAKE_SOURCE_DIR}/target/release")
else()
    set(RUST_LIB_DIR "${CMAKE_SOURCE_DIR}/target/debug")
endif()

add_executable(azokle-browser-linux
    src/main.cpp
    src/window/AzokleBrowserWindow.cpp
    src/widget/AzokleGeckoWidget.cpp
    src/tabs/AzokleTabManager.cpp
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

# Portable runtime RPATH configuration
set_target_properties(azokle-browser-linux PROPERTIES
    BUILD_RPATH "${RUST_LIB_DIR}"
    INSTALL_RPATH "$ORIGIN/../lib:$ORIGIN"
)

# Standard installation targets
install(TARGETS azokle-browser-linux DESTINATION bin)
install(FILES "${RUST_LIB_DIR}/libazokle_c.so" DESTINATION lib OPTIONAL)

use cxx_build;
use pkg_config;
use std::{env, path::PathBuf};

fn main() {
    let manifest_dir = PathBuf::from(env::var("CARGO_MANIFEST_DIR").unwrap());

    let verilated_cpp = manifest_dir.join("verilated_cpp");
    println!("cargo:rerun-if-changed={}", verilated_cpp.display());

    let verilator =
        pkg_config::probe_library("verilator").expect("Failed to find verilator via pkg-config");

    let cmake_out_dir = cmake::Config::new(&verilated_cpp).build();

    // Not really do compile actually, as we are using cmake to build the verilated_cpp.
    // Here we just "compile" the libverilated_cpp.a to libverilated.a
    cxx_build::bridge("src/core/sim/verilated.rs")
        .include(&cmake_out_dir)
        .include(&cmake_out_dir.join("VFutureCore.dir"))
        .includes(verilator.include_paths)
        .cpp(true)
        .std("c++20")
        .warnings(false)
        .compile("verilated");

    println!("cargo:rustc-link-search=native={}", cmake_out_dir.display());
    println!("cargo:rustc-link-lib=static=verilated_cpp");
    println!("cargo:rustc-link-lib=z");
}

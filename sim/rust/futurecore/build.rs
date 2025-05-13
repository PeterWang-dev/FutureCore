use cxx_build;
use pkg_config;
use std::{env, path::PathBuf};

fn main() {
    let manifest_dir = PathBuf::from(env::var("CARGO_MANIFEST_DIR").unwrap());

    let verilated_cpp_dir = manifest_dir.join("verilated_cpp");
    let vsrc_path = verilated_cpp_dir.join("vsrc");
    println!("cargo:rerun-if-changed={}", vsrc_path.display());

    let cmake_out_dir = cmake::build(&verilated_cpp_dir);
    let verilator =
        pkg_config::probe_library("verilator").expect("Failed to find verilator via pkg-config");
    // Not really do compile actually, as we are using cmake to build the verilated_cpp.
    // Here we just "compile" the libverilated_cpp.a to libverilated.a
    cxx_build::bridge("src/verilated.rs")
        .include(&cmake_out_dir)
        .include(&cmake_out_dir.join("VFutureCore.dir"))
        .includes(verilator.include_paths)
        .compile("verilated");

    println!("cargo:rustc-link-search=native={}", cmake_out_dir.display());
    println!("{}", cmake_out_dir.display());
    println!("cargo:rustc-link-lib=static=verilated_cpp");
    println!("cargo:rustc-link-lib=z");
}

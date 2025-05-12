use cxx_build;
use pkg_config;
use std::{env, path::PathBuf};

fn main() {
    let manifest_dir = PathBuf::from(env::var("CARGO_MANIFEST_DIR").unwrap());

    let verilated_dir = cmake::build(manifest_dir.join("verilated_cpp"));
    let vsrc_path = verilated_dir.join("vsrc");
    println!("cargo:rerun-if-changed={}", vsrc_path.display());

    let verilator =
        pkg_config::probe_library("verilator").expect("Failed to find verilator via pkg-config");

    cxx_build::bridge("src/verilated.rs")
        .include(&verilated_dir)
        .include(&verilated_dir.join("VFutureCore.dir"))
        .includes(verilator.include_paths)
        .compile("verilated");

    println!("cargo:rustc-link-search=native={}", verilated_dir.display());
    println!("{}", verilated_dir.display());
    println!("cargo:rustc-link-lib=static=verilated_cpp");
    println!("cargo:rustc-link-lib=z");
}

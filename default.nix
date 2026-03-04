{
  stdenv,
  mill_0_11,
  jdk,
  scala_2_13,
  python3,
  which,
  rustc,
  cargo,
  llvmPackages,
  cmake,
  pkg-config,
  verilator,
  zlib,
}:
stdenv.mkDerivation {
  pname = "npc";
  version = "0.1.0";
  src = ./.;
  nativeBuildInputs = [
    # For build Future Core
    mill_0_11
    jdk
    scala_2_13
    python3
    which
    # For build simulation platform
    rustc
    cargo
    llvmPackages.bintools
    cmake
    pkg-config
  ];
  buildInputs = [
    verilator
    zlib
  ];
}

{
  stdenv,
  which,
  mill,
  scala_2_13,
  verilator,
  rustc,
  cargo,
  llvmPackages,
  cmake,
  pkg-config,
  python3,
  zlib,
}:
stdenv.mkDerivation {
  pname = "npc";
  version = "0.1.0";
  src = ./.;
  nativeBuildInputs = [
    # For build Future Core
    mill
    scala_2_13
    which
    python3
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

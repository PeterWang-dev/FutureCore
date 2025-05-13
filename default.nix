{
  stdenv,
  mill,
  scala_2_13,
  verilator,
  python3,
  yosys,
  zlib,
  SDL2_classic,
  SDL2_ttf,
  SDL2_image,
}:
stdenv.mkDerivation {
  pname = "futurecore";
  version = "0.1.0";
  buildInputs = [
    mill
    scala_2_13
    verilator
    python3
    yosys
    zlib
  ];
}

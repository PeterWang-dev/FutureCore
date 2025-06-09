{
  pkgs ? (import <nixpkgs> { }),
  inputsFrom ? [ ],
  additionalPackages ? [ ],
  shellHook ? "",
}:
let
  concatLines = pkgs.lib.strings.concatLines;
  makeLibraryPath = pkgs.lib.makeLibraryPath;

  thisPackage = pkgs.callPackage ./default.nix { };
  toolPackages = with pkgs; [
    llvmPackages.bintools
    clang-tools
    gtkwave
    ieda
    lolcat
    metals
  ];

  fenix = pkgs.callPackage (pkgs.fetchFromGitHub {
    owner = "nix-community";
    repo = "fenix";
    rev = "monthly";
    hash = "sha256-MJNhEBsAbxRp/53qsXv6/eaWkGS8zMGX9LuCz1BLeck==";
  }) { };

  rustToolchain = [
    (fenix.fromToolchainFile {
      file = ./sim/rust/rust-toolchain.toml;
    })
    pkgs.pkg-config
  ];
in
pkgs.mkShell {
  inputsFrom = inputsFrom ++ [ thisPackage ];
  packages = additionalPackages ++ toolPackages ++ rustToolchain;
  shellHook = concatLines [
    ''echo "This is NPC." | lolcat''
    shellHook
  ];

  LD_LIBRARY_PATH = makeLibraryPath [
    (with pkgs; [
      llvmPackages.libclang
      zlib
    ])
  ];
}

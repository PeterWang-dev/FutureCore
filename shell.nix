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
    # llvmPackages.bintools
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
    hash = "sha256-dJj8TUoZGj55Ttro37vvFGF2L+xlYNfspQ9u4BfqTFw=";
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

  LD_LIBRARY_PATH = makeLibraryPath (
    with pkgs;
    [
      llvmPackages.libclang.lib
      zlib
    ]
  );
}

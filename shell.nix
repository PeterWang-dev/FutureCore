let
  nixpkgs =
    # "https://github.com/NixOS/nixpkgs/archive/release-24.11.tar.gz"
    fetchTarball "https://mirrors.cernet.edu.cn/nix-channels/nixos-24.11/nixexprs.tar.xz";

  overlaysBaseUri = ../nix/overlays; # points to folder of overlays in ysyx-project
  # "http://forgejo.pwdevstudio.internal/ysyx-project/ysyx-project/raw/branch/main/nix/overlays/"
  verilator = import (overlaysBaseUri + "/verilator.nix");
  mill = import (overlaysBaseUri + "/mill.nix");
  ieda = import (overlaysBaseUri + "/ieda.nix");
  python3 = import (overlaysBaseUri + "/python3.nix");
in
{
  pkgs ? (
    import nixpkgs {
      config = { };
      overlays = [
        verilator
        mill
        ieda
        python3
      ];
    }
  ),
  inputsFrom ? [ ],
  packages ? [ ],
  shellHook ? "",
}:
let
  lib = pkgs.lib;

  thisPackage = pkgs.callPackage ./default.nix { };
  toolPackages =
    with pkgs;
    [
      bear
      universal-ctags
      clang-tools
      git
      gtkwave
      lolcat
      metals
      verible
      xdot
    ]
    ++ [ pkgs.ieda ];
  # teroshdlDeps = [
  #   (pkgs.python3.withPackages (
  #     python-pkgs: with python-pkgs; [
  #       yowasp-yosys
  #       vunit-hdl
  #       edalize
  #       cocotb
  #     ]
  #   ))
  # ];
  fenix = pkgs.callPackage (pkgs.fetchFromGitHub {
    owner = "nix-community";
    repo = "fenix";
    rev = "main";
    hash = "sha256-oLrH70QIWB/KpaI+nztyP1hG4zAEEpMiNk6sA8QLQ/8=";
  }) { };
  rustToolchain = [
    fenix.stable.toolchain
    pkgs.pkg-config
  ];
in
pkgs.mkShell {
  inputsFrom = inputsFrom ++ [ thisPackage ];
  # packages = packages ++ toolPackages ++ teroshdlDeps;
  packages = packages ++ toolPackages ++ rustToolchain;
  shellHook = lib.strings.concatLines [
    ''echo "This is NPC." | lolcat''
    shellHook
  ];
}

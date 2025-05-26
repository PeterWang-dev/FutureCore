{
  pkgs ? (import <nixpkgs> { }),
  inputsFrom ? [ ],
  additionalPackages ? [ ],
  shellHook ? "",
}:
let
  concatLines = pkgs.lib.strings.concatLines;

  thisPackage = pkgs.callPackage ./default.nix { };
  toolPackages =
    with pkgs;
    [
      clang-tools
      gtkwave
      ieda
      lolcat
      metals
    ];

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
  packages = additionalPackages ++ toolPackages ++ rustToolchain;
  shellHook = concatLines [
    ''echo "This is NPC." | lolcat''
    shellHook
  ];
}

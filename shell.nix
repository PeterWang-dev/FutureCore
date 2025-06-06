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
    hash = "sha256-gILKNw2g7eGXt1aVJM0pUdeJQX0z6kXZNoiAJPjXHTo=";
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

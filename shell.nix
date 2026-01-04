{
  pkgs ? import (fetchTarball "https://nixos.org/channels/nixos-25.11/nixexprs.tar.xz") {
    overlays = [
      (import "${fetchTarball "https://github.com/nix-community/fenix/archive/main.tar.gz"}/overlay.nix")
    ];
  },

}:
let
  # use mill 0.11.12 instead of the latest version with breaking changes
  mill_0_11 = pkgs.callPackage ./nix/mill-0.11.12.nix { };
  rustToolchain = pkgs.fenix.fromToolchainFile {
    file = ./sim/rust/rust-toolchain.toml;
    sha256 = "sha256-sqSWJDUxc+zaz1nBWMAJKTAGBuGWP25GCftIOlCEAtA=";
  };
  npc = pkgs.callPackage ./default.nix { inherit mill_0_11; };
in
pkgs.mkShell rec {
  name = "npc-env";
  inputsFrom = [ npc ];
  packages = with pkgs; [
    rustToolchain
    metals
    gtkwave
    ieda
  ];
  shellHook = ''
    export NPC_HOME=${NPC_HOME}
  '';
  NPC_HOME = toString ./.;
}

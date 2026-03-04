{
  pkgs ? import (fetchTarball "https://nixos.org/channels/nixos-25.11/nixexprs.tar.xz") { },
}:
let
  # Append overlays after nixpkgs import to avoid issues with multi-file interactions
  pkgsOverlay = pkgs.appendOverlays [
    # fenix overlay for Rust toolchain management
    (import "${fetchTarball "https://github.com/nix-community/fenix/archive/main.tar.gz"}/overlay.nix")
  ];

  # Use mill 0.11.12 instead of the latest version with breaking changes
  mill_0_11 = pkgsOverlay.callPackage ./nix/mill-0.11.12.nix { };
  rustToolchain = pkgsOverlay.fenix.fromToolchainFile {
    file = ./sim/rust/rust-toolchain.toml;
    sha256 = "sha256-sqSWJDUxc+zaz1nBWMAJKTAGBuGWP25GCftIOlCEAtA=";
  };
  npc = pkgsOverlay.callPackage ./default.nix { inherit mill_0_11; };
in
pkgsOverlay.mkShell rec {
  name = "npc-env";
  inputsFrom = [ npc ];
  packages = with pkgsOverlay; [
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

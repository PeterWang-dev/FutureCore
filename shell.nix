{
  pkgs ? import (fetchTarball "https://nixos.org/channels/nixos-25.11/nixexprs.tar.xz") { },
}:
let
  # Append overlays after nixpkgs import to avoid issues with multi-file interactions
  pkgsOverlay = pkgs.appendOverlays [
    # rust-overlay for Rust toolchain management
    (import (fetchTarball "https://github.com/oxalica/rust-overlay/archive/master.tar.gz"))
  ];

  # Use mill 0.11.12 instead of the latest version with breaking changes
  mill_0_11 = pkgsOverlay.callPackage ./nix/mill-0.11.12.nix { };
  rustToolchain = pkgsOverlay.rust-bin.fromRustupToolchainFile ./sim/rust/rust-toolchain.toml;
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

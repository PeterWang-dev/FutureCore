{
  pkgs ? import (fetchTarball "https://nixos.org/channels/nixos-25.11/nixexprs.tar.xz") { },
}:
let
  # use mill 0.11.12 instead of the latest version with breaking changes
  mill_0_11 = pkgs.callPackage ./nix/mill-0.11.12.nix { };
  npc = pkgs.callPackage ./default.nix { inherit mill_0_11; };
in
pkgs.mkShell rec {
  inputsFrom = [ npc ];
  packages = with pkgs; [
    gtkwave
    ieda
    metals
  ];
  shellHook = ''
    export NPC_HOME=${NPC_HOME}
  '';
  NPC_HOME = toString ./.;
}

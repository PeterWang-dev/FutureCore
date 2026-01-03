{
  pkgs ? import <nixpkgs> { },
}:
let
  npc = pkgs.callPackage ./default.nix { };
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

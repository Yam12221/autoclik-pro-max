# To learn more about how to use Nix to configure your environment
# see: https://developers.google.com/idx/guides/customize-idx-env
{ pkgs, ... }: {
  # Which nixpkgs channel to use.
  channel = "stable-24.05"; # or "unstable"
  
  # Use https://search.nixos.org/packages to find packages
  packages = [
    pkgs.jdk17
    pkgs.gradle
  ];
  
  # Sets environment variables in the workspace
  env = {};
  idx = {
    # Search for the extensions you want on https://open-vsx.org/ and use "publisher.id"
    extensions = [
      "redhat.java"
      "vscjava.vscode-java-debug"
      "vscjava.vscode-java-dependency"
      "vscjava.vscode-maven"
      "vscjava.vscode-gradle"
    ];
    
    # Enable previews and customize configuration
    previews = {
      enable = true;
      previews = {
        android = {
          command = [
            "gradle"
            "assembleDebug"
            "--no-daemon"
          ];
          manager = "android";
        };
      };
    };
  };
}

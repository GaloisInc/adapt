# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  config.vm.box = "ubuntu/trusty64"
  config.vm.box_url = "https://atlas.hashicorp.com/ubuntu/boxes/trusty64/versions/20160107.1.0/providers/virtualbox.box"

  # Forward the Dashboard port for the user to get system status.
  config.vm.network "forwarded_port", guest: 8080, host: 8081

  config.vm.provider "virtualbox" do |vb|
  #   # Display the VirtualBox GUI when booting the machine
  #   vb.gui = true
  #
  #   # Customize the amount of memory on the VM:
     vb.memory = "4096"
     vb.cpus   = 2
  end

  config.vm.provision "shell", privileged: false, path: "install/bootstrap.sh"
  config.vm.provision "shell", run: "always", privileged: false, path: "start_daemons.sh"
end

